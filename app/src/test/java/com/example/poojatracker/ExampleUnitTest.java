package com.example.poojatracker;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Local unit tests covering YouTube URL parsing/validation and Pooja JSON export aggregation.
 */
public class ExampleUnitTest {
    @Test
    public void extractVideoId_handlesAllYouTubeFormats() {
        assertEquals(
                "Naruxgq6oMU",
                PoojaModel.extractVideoId("https://www.youtube.com/watch?v=Naruxgq6oMU&list=RDNaruxgq6oMU&start_radio=1")
        );
        assertEquals(
                "Naruxgq6oMU",
                PoojaModel.extractVideoId("https://youtu.be/Naruxgq6oMU?si=AbCdEfGhIjKlMnOp")
        );
        assertEquals(
                "4bbwANHNlEQ",
                PoojaModel.extractVideoId("https://www.youtube.com/shorts/4bbwANHNlEQ?feature=share")
        );
        assertEquals(
                "Clc7w9CeLXs",
                PoojaModel.extractVideoId("https://www.youtube.com/live/Clc7w9CeLXs?si=xyz123")
        );
        assertEquals(
                "Naruxgq6oMU",
                PoojaModel.extractVideoId("  Naruxgq6oMU  ")
        );
    }

    @Test
    public void hasValidVideoId_rejectsEmptyOrInvalidLinks() {
        assertTrue(PoojaModel.hasValidVideoId("https://www.youtube.com/watch?v=Naruxgq6oMU&list=RDNaruxgq6oMU&start_radio=1"));
        assertTrue(PoojaModel.hasValidVideoId("Naruxgq6oMU"));
        assertFalse(PoojaModel.hasValidVideoId(""));
        assertFalse(PoojaModel.hasValidVideoId("   "));
        assertFalse(PoojaModel.hasValidVideoId("not-a-youtube-link"));
        assertFalse(PoojaModel.hasValidVideoId(null));
    }

    @Test
    public void aggregatePoojaRecords_groupsDaysPerIdAndHandlesNullDaySafely() {
        List<MyDBHelper.JoinedPoojaRow> rows = Arrays.asList(
                new MyDBHelper.JoinedPoojaRow(101, "Ganesh Mantra", "108 Times", "https://www.youtube.com/watch?v=Naruxgq6oMU", true, "Monday"),
                new MyDBHelper.JoinedPoojaRow(101, "Ganesh Mantra", "108 Times", "https://www.youtube.com/watch?v=Naruxgq6oMU", true, "Wednesday"),
                new MyDBHelper.JoinedPoojaRow(101, "Ganesh Mantra", "108 Times", "https://www.youtube.com/watch?v=Naruxgq6oMU", true, "Sunday"),
                new MyDBHelper.JoinedPoojaRow(202, "Orphan Entry", "No days", "https://www.youtube.com/watch?v=4bbwANHNlEQ", false, null)
        );

        List<MyDBHelper.PoojaExportRecord> aggregated = MyDBHelper.aggregatePoojaRecords(rows);
        assertEquals(2, aggregated.size());

        MyDBHelper.PoojaExportRecord ganesh = aggregated.get(0);
        assertEquals(101, ganesh.id);
        assertArrayEquals(new int[]{1, 0, 1, 0, 0, 0, 1}, ganesh.days);

        MyDBHelper.PoojaExportRecord orphan = aggregated.get(1);
        assertEquals(202, orphan.id);
        assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0, 0}, orphan.days);
    }

    @Test
    public void deterministicId_isStableAcrossDaysDuringLegacyMigration() {
        int idMon = MyDBHelper.deterministicId("Ganesh Mantra", "https://www.youtube.com/watch?v=Naruxgq6oMU");
        int idWed = MyDBHelper.deterministicId("  ganesh mantra ", "https://www.youtube.com/watch?v=Naruxgq6oMU  ");
        int idOther = MyDBHelper.deterministicId("Hanuman Chalisa", "https://www.youtube.com/watch?v=AETFvQonfV8");
        assertEquals(idMon, idWed);
        assertNotEquals(idMon, idOther);
        assertTrue(idMon >= 100000 && idMon < 1000000);
    }

    @Test
    public void parseExtractedLines_extractsCardsIgnoresThumbnailNoiseAndSkipsMainMenu() {
        List<ScreenMediaPlaylistImporter.KnownPooja> catalog = Arrays.asList(
                new ScreenMediaPlaylistImporter.KnownPooja(
                        "Mangal Navgrah Stotra",
                        "मंगल स्तोत्र",
                        "https://www.youtube.com/watch?v=0EZnB_FdC8c"
                )
        );

        List<ScreenMediaPlaylistImporter.OcrLine> mondayFrame = Arrays.asList(
                // Status bar at top (ignored)
                new ScreenMediaPlaylistImporter.OcrLine("12:06  81%", 60, 20, 980, 65),
                // Day header
                new ScreenMediaPlaylistImporter.OcrLine("Monday", 350, 260, 725, 365),
                // Count header (ignored)
                new ScreenMediaPlaylistImporter.OcrLine("4 / 21", 910, 520, 1020, 565),
                // Left thumbnail overlay text inside 70x70 image (ignored because right < 0.265 * 1080)
                new ScreenMediaPlaylistImporter.OcrLine("मंगल स्तोत्र कुंडली", 80, 790, 255, 860),
                // Card 1 (known catalog entry even with 0->O OCR confusion)
                new ScreenMediaPlaylistImporter.OcrLine("Mangal Navgrah Stotra", 310, 765, 890, 820),
                new ScreenMediaPlaylistImporter.OcrLine("मंगल स्तोत्र", 300, 830, 420, 865),
                new ScreenMediaPlaylistImporter.OcrLine("https://www.youtube.com/watch?v=OEZnB_FdC8c", 300, 880, 980, 915),
                // Card 2 (brand-new custom Pooja added by user, not in catalog)
                new ScreenMediaPlaylistImporter.OcrLine("Custom Bhajan", 300, 1110, 620, 1160),
                new ScreenMediaPlaylistImporter.OcrLine("https://www.youtube.com/watch?v=Naruxgq6oMU", 300, 1185, 975, 1220)
        );

        ScreenMediaPlaylistImporter.FrameParseResult res =
                ScreenMediaPlaylistImporter.parseExtractedLines(mondayFrame, 1080, 2400, "Sunday", catalog);
        assertEquals("Monday", res.detectedDay);
        assertEquals(2, res.cards.size());

        assertEquals("Mangal Navgrah Stotra", res.cards.get(0).title);
        assertEquals("https://www.youtube.com/watch?v=0EZnB_FdC8c", res.cards.get(0).contentUrl);
        assertEquals("Monday", res.cards.get(0).day);

        assertEquals("Custom Bhajan", res.cards.get(1).title);
        assertEquals("https://www.youtube.com/watch?v=Naruxgq6oMU", res.cards.get(1).contentUrl);

        // Main menu frame with 7 weekday buttons should return 0 cards
        List<ScreenMediaPlaylistImporter.OcrLine> mainMenu = Arrays.asList(
                new ScreenMediaPlaylistImporter.OcrLine("MONDAY", 350, 430, 730, 500),
                new ScreenMediaPlaylistImporter.OcrLine("TUESDAY", 350, 680, 730, 750),
                new ScreenMediaPlaylistImporter.OcrLine("WEDNESDAY", 350, 930, 730, 1000)
        );
        ScreenMediaPlaylistImporter.FrameParseResult menuRes =
                ScreenMediaPlaylistImporter.parseExtractedLines(mainMenu, 1080, 2400, "Monday", catalog);
        assertEquals(0, menuRes.cards.size());
    }
}