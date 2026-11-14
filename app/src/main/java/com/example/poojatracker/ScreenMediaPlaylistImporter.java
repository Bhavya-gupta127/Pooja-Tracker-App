package com.example.poojatracker;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Play-Protect-safe, zero-permission playlist extractor.
 * Scans either a Screen Recording (.mp4) or Screenshots (.png/.jpg) of the legacy
 * 2023 "Pooja Scheduler" app using offline ML Kit Devanagari+Latin OCR.
 */
public class ScreenMediaPlaylistImporter {

    private static final String TAG = "ScreenMediaImport";
    private static final Pattern COUNT_HEADER_PATTERN = Pattern.compile("^\\d+\\s*/\\s*\\d+$");
    private static final Pattern YT_ID_EXTRACT_PATTERN =
            Pattern.compile("(?:v=|youtu\\.be/|shorts/|live/|embed/)([A-Za-z0-9_-]{11})");

    public interface Callback {
        void onProgress(int currentStep, int totalSteps, int recoveredPoojas);
        void onComplete(int recoveredPoojas, int dayLinksAdded);
        void onError(String errorMessage);
    }

    static class OcrLine {
        final String text;
        final int left;
        final int top;
        final int right;
        final int bottom;

        OcrLine(String text, int left, int top, int right, int bottom) {
            this.text = text != null ? text.trim() : "";
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    static class KnownPooja {
        final String title;
        final String desc;
        final String contentUrl;
        final String videoId;

        KnownPooja(String title, String desc, String contentUrl) {
            this.title = title;
            this.desc = desc;
            this.contentUrl = contentUrl;
            this.videoId = PoojaModel.extractVideoId(contentUrl);
        }
    }

    static class ExtractedCard {
        final String day;
        final String title;
        final String desc;
        final String contentUrl;

        ExtractedCard(String day, String title, String desc, String contentUrl) {
            this.day = day;
            this.title = title;
            this.desc = desc;
            this.contentUrl = contentUrl;
        }
    }

    static class FrameParseResult {
        final String detectedDay;
        final List<ExtractedCard> cards;

        FrameParseResult(String detectedDay, List<ExtractedCard> cards) {
            this.detectedDay = detectedDay;
            this.cards = cards;
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static List<KnownPooja> loadBundledCatalog(Context context) {
        List<KnownPooja> catalog = new ArrayList<>();
        try (InputStream is = context.getAssets().open("default_poojas.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) continue;
                String title = obj.optString("title", "").trim();
                String desc = obj.optString("desc", obj.optString("description", "")).trim();
                String url = obj.optString("contentURL", obj.optString("contentUrl", "")).trim();
                if (!title.isEmpty() && !url.isEmpty()) {
                    catalog.add(new KnownPooja(title, desc, url));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to read bundled default_poojas.json", e);
        }
        return catalog;
    }

    public static JSONArray loadBundledJsonArray(Context context) throws Exception {
        try (InputStream is = context.getAssets().open("default_poojas.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return new JSONArray(sb.toString());
        }
    }

    public void importFromMediaUris(Context context, List<Uri> uris, MyDBHelper dbHelper, Callback callback) {
        final Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            TextRecognizer recognizer = TextRecognition.getClient(
                    new DevanagariTextRecognizerOptions.Builder().build()
            );
            try {
                List<KnownPooja> catalog = loadBundledCatalog(appContext);
                ContentResolver resolver = appContext.getContentResolver();

                Set<Integer> uniqueIds = new HashSet<>();
                Set<String> dayLinks = new HashSet<>();
                String lastDetectedDay = "Monday";

                // Pre-calculate total work units across videos & images
                int totalUnits = 0;
                List<Long> videoDurationsMs = new ArrayList<>();
                List<Boolean> isVideoFlags = new ArrayList<>();
                for (Uri uri : uris) {
                    String mime = resolver.getType(uri);
                    boolean isVideo = (mime != null && mime.startsWith("video/"))
                            || uri.toString().toLowerCase(Locale.US).endsWith(".mp4");
                    isVideoFlags.add(isVideo);
                    if (isVideo) {
                        long durMs = getVideoDurationMs(appContext, uri);
                        videoDurationsMs.add(durMs);
                        int frames = Math.max(1, Math.min(140, (int) (durMs / 750L) + 1));
                        totalUnits += frames;
                    } else {
                        videoDurationsMs.add(0L);
                        totalUnits += 1;
                    }
                }

                int completedUnits = 0;
                for (int i = 0; i < uris.size(); i++) {
                    Uri uri = uris.get(i);
                    if (isVideoFlags.get(i)) {
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        try {
                            retriever.setDataSource(appContext, uri);
                            long durMs = videoDurationsMs.get(i);
                            long stepMs = Math.max(750L, durMs / 140L);
                            for (long tMs = 0; tMs <= durMs; tMs += stepMs) {
                                Bitmap frame = retriever.getFrameAtTime(
                                        tMs * 1000L,
                                        MediaMetadataRetriever.OPTION_CLOSEST
                                );
                                if (frame == null) {
                                    frame = retriever.getFrameAtTime(
                                            tMs * 1000L,
                                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                                    );
                                }
                                if (frame != null) {
                                    lastDetectedDay = processBitmapFrame(
                                            frame,
                                            recognizer,
                                            lastDetectedDay,
                                            catalog,
                                            dbHelper,
                                            uniqueIds,
                                            dayLinks
                                    );
                                    frame.recycle();
                                }
                                completedUnits++;
                                postProgress(callback, completedUnits, Math.max(completedUnits, totalUnits), uniqueIds.size());
                            }
                        } finally {
                            try {
                                retriever.release();
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        Bitmap bitmap = decodeBitmap(resolver, uri);
                        if (bitmap != null) {
                            lastDetectedDay = processBitmapFrame(
                                    bitmap,
                                    recognizer,
                                    lastDetectedDay,
                                    catalog,
                                    dbHelper,
                                    uniqueIds,
                                    dayLinks
                            );
                            bitmap.recycle();
                        }
                        completedUnits++;
                        postProgress(callback, completedUnits, Math.max(completedUnits, totalUnits), uniqueIds.size());
                    }
                }

                final int finalCount = uniqueIds.size();
                final int finalLinks = dayLinks.size();
                mainHandler.post(() -> callback.onComplete(finalCount, finalLinks));
            } catch (Exception e) {
                Log.e(TAG, "Screen media OCR import failed", e);
                final String msg = e.getMessage() != null ? e.getMessage() : "Unable to scan media";
                mainHandler.post(() -> callback.onError(msg));
            } finally {
                recognizer.close();
            }
        });
    }

    private void postProgress(Callback callback, int step, int total, int count) {
        mainHandler.post(() -> callback.onProgress(step, total, count));
    }

    private long getVideoDurationMs(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) {
                return Math.max(500L, Long.parseLong(dur));
            }
        } catch (Exception ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
        return 10000L;
    }

    private Bitmap decodeBitmap(ContentResolver resolver, Uri uri) {
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) return null;
            return BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            Log.w(TAG, "Failed to decode image URI: " + uri, e);
            return null;
        }
    }

    private String processBitmapFrame(
            Bitmap bitmap,
            TextRecognizer recognizer,
            String fallbackDay,
            List<KnownPooja> catalog,
            MyDBHelper dbHelper,
            Set<Integer> uniqueIds,
            Set<String> dayLinks
    ) throws Exception {
        Text visionText = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)));
        List<OcrLine> ocrLines = new ArrayList<>();
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect box = line.getBoundingBox();
                if (box != null) {
                    ocrLines.add(new OcrLine(line.getText(), box.left, box.top, box.right, box.bottom));
                }
            }
        }

        FrameParseResult result = parseExtractedLines(
                ocrLines,
                bitmap.getWidth(),
                bitmap.getHeight(),
                fallbackDay,
                catalog
        );

        for (ExtractedCard card : result.cards) {
            int id = MyDBHelper.deterministicId(card.title, card.contentUrl);
            dbHelper.addPooja(id, card.title, card.desc, card.contentUrl, false);
            dbHelper.addDay(id, card.day);
            uniqueIds.add(id);
            dayLinks.add(id + "|" + card.day);
        }
        return result.detectedDay;
    }

    static FrameParseResult parseExtractedLines(
            List<OcrLine> rawLines,
            int frameW,
            int frameH,
            String fallbackDay,
            List<KnownPooja> catalog
    ) {
        if (rawLines == null || rawLines.isEmpty() || frameW <= 0 || frameH <= 0) {
            return new FrameParseResult(fallbackDay, Collections.emptyList());
        }

        List<OcrLine> filtered = new ArrayList<>();
        Set<String> distinctWeekdayLines = new HashSet<>();

        for (OcrLine line : rawLines) {
            if (line.text.isEmpty()) continue;
            // Ignore Android status bar at the very top
            if (line.bottom < frameH * 0.045f) continue;
            // Ignore text embedded inside the 70x70 left thumbnail artwork
            if (line.right < frameW * 0.265f) continue;

            String maybeDay = matchExactWeekday(line.text);
            if (maybeDay != null) {
                distinctWeekdayLines.add(maybeDay);
            }
            filtered.add(line);
        }

        // If 3+ weekday buttons appear at once, this frame is MainActivity (7-day menu) — skip cards
        if (distinctWeekdayLines.size() >= 3) {
            return new FrameParseResult(fallbackDay, Collections.emptyList());
        }

        Collections.sort(filtered, Comparator.comparingInt(a -> a.top));

        String activeDay = fallbackDay != null ? fallbackDay : "Monday";
        for (OcrLine line : filtered) {
            if (line.top < frameH * 0.30f) {
                String headerDay = matchExactWeekday(line.text);
                if (headerDay != null) {
                    activeDay = headerDay;
                    break;
                }
            }
        }

        List<ExtractedCard> extracted = new ArrayList<>();
        int lastConsumedBottom = (int) (frameH * 0.16f);

        for (int i = 0; i < filtered.size(); i++) {
            OcrLine line = filtered.get(i);
            String compact = line.text.replaceAll("\\s+", "");
            String lower = compact.toLowerCase(Locale.US);

            if (!lower.contains("youtube.com") && !lower.contains("youtu.be")) {
                continue;
            }

            // Merge wrapped 2nd URL line if present directly underneath
            String combinedUrl = compact;
            if (i + 1 < filtered.size()) {
                OcrLine next = filtered.get(i + 1);
                int gap = next.top - line.bottom;
                int lineH = Math.max(12, line.bottom - line.top);
                String nextCompact = next.text.replaceAll("\\s+", "");
                if (gap >= -4 && gap <= lineH * 1.3f
                        && !next.text.contains(" ")
                        && !nextCompact.toLowerCase(Locale.US).contains("youtube")) {
                    combinedUrl = combinedUrl + nextCompact;
                }
            }

            // Collect 1–2 candidate lines right above this URL inside the same card
            List<OcrLine> aboveInCard = new ArrayList<>();
            int maxCardLookback = (int) (frameH * 0.12f);
            for (int j = i - 1; j >= 0; j--) {
                OcrLine prev = filtered.get(j);
                if (prev.bottom <= lastConsumedBottom) break;
                if (line.top - prev.bottom > maxCardLookback) break;
                if (prev.top < frameH * 0.20f) break;
                if (COUNT_HEADER_PATTERN.matcher(prev.text).matches()) continue;
                if (matchExactWeekday(prev.text) != null) continue;
                String prevLower = prev.text.replaceAll("\\s+", "").toLowerCase(Locale.US);
                if (prevLower.contains("youtube.com") || prevLower.contains("youtu.be")) break;
                aboveInCard.add(0, prev);
                if (aboveInCard.size() >= 2) break;
            }

            lastConsumedBottom = line.bottom;
            if (aboveInCard.isEmpty()) {
                continue;
            }

            String rawTitle = aboveInCard.get(0).text.trim();
            String rawDesc = aboveInCard.size() >= 2 ? aboveInCard.get(1).text.trim() : "";

            ExtractedCard resolved = resolveWithCatalog(activeDay, rawTitle, rawDesc, combinedUrl, catalog);
            if (resolved != null) {
                extracted.add(resolved);
            }
        }

        return new FrameParseResult(activeDay, extracted);
    }

    private static ExtractedCard resolveWithCatalog(
            String day,
            String rawTitle,
            String rawDesc,
            String rawUrl,
            List<KnownPooja> catalog
    ) {
        String candidateId = extractCandidateVideoId(rawUrl);

        if (catalog != null && !catalog.isEmpty()) {
            // 1. Exact title match in known catalog
            for (KnownPooja kp : catalog) {
                if (kp.title.equalsIgnoreCase(rawTitle)) {
                    return new ExtractedCard(
                            day,
                            kp.title,
                            rawDesc.isEmpty() ? kp.desc : rawDesc,
                            kp.contentUrl
                    );
                }
            }
            // 2. Exact or 1-char OCR distance match on 11-char YouTube ID (e.g. O vs 0, I vs l)
            if (candidateId != null && candidateId.length() == 11) {
                for (KnownPooja kp : catalog) {
                    if (kp.videoId.length() == 11 && hammingDistance(candidateId, kp.videoId) <= 1) {
                        return new ExtractedCard(
                                day,
                                rawTitle.isEmpty() ? kp.title : rawTitle,
                                rawDesc.isEmpty() ? kp.desc : rawDesc,
                                kp.contentUrl
                        );
                    }
                }
            }
        }

        if (candidateId == null || candidateId.length() != 11 || rawTitle.isEmpty()) {
            return null;
        }
        String normalizedUrl = rawUrl.startsWith("http")
                ? rawUrl
                : ("https://www.youtube.com/watch?v=" + candidateId);
        return new ExtractedCard(day, rawTitle, rawDesc, normalizedUrl);
    }

    private static String extractCandidateVideoId(String url) {
        String direct = PoojaModel.extractVideoId(url);
        if (direct != null && direct.length() == 11 && PoojaModel.hasValidVideoId(direct)) {
            return direct;
        }
        Matcher m = YT_ID_EXTRACT_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static int hammingDistance(String a, String b) {
        if (a.length() != b.length()) return 99;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                diff++;
            }
        }
        return diff;
    }

    static String matchExactWeekday(String raw) {
        if (raw == null) return null;
        String clean = raw.trim().toLowerCase(Locale.US);
        switch (clean) {
            case "monday":
                return "Monday";
            case "tuesday":
                return "Tuesday";
            case "wednesday":
                return "Wednesday";
            case "thursday":
            case "thurday":
                return "Thursday";
            case "friday":
                return "Friday";
            case "saturday":
                return "Saturday";
            case "sunday":
                return "Sunday";
            default:
                return null;
        }
    }
}
