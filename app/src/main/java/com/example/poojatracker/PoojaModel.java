package com.example.poojatracker;

public class PoojaModel {
    int id;
    String title,desc,contentURL,VideoId;
    boolean status;

    public PoojaModel() {

    }

    public PoojaModel(int id,String title,String desc,String contentURL, int intstatus) {
        this.id=id;
        this.title=title;
        this.desc=desc;
        this.contentURL=contentURL;
        this.status= intstatus!=0;
        this.VideoId=VideoIdFromUrl(contentURL);
    }

    private static final String DEFAULT_VIDEO_ID = "gXWXKjR-qII";
    private static final java.util.regex.Pattern YOUTUBE_URL_PATTERN = java.util.regex.Pattern.compile(
            "(?:(?:www\\.|m\\.|music\\.)?youtube(?:-nocookie)?\\.com/(?:[^/\\n\\s]+/.+/|(?:v|e(?:mbed)?|shorts|live)/|.*?[?&]v=)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    public static boolean hasValidVideoId(String url) {
        if (url == null) {
            return false;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (trimmed.matches("^[a-zA-Z0-9_-]{11}$")) {
            return true;
        }
        return YOUTUBE_URL_PATTERN.matcher(trimmed).find();
    }

    public static String extractVideoId(String url) {
        if (url == null) {
            return DEFAULT_VIDEO_ID;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_VIDEO_ID;
        }
        if (trimmed.matches("^[a-zA-Z0-9_-]{11}$")) {
            return trimmed;
        }
        java.util.regex.Matcher matcher = YOUTUBE_URL_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return DEFAULT_VIDEO_ID;
    }

    public String VideoIdFromUrl(String url)
    {
        return extractVideoId(url);
    }

    public PoojaModel(int id,String title,String desc,String contentURL,boolean status)
    {
        this.id=id;
        this.title=title;
        this.desc=desc;
        this.contentURL=contentURL;
        this.status= status;
        this.VideoId=VideoIdFromUrl(contentURL);
    }

    public String getVideoId() {
        return VideoId;
    }

    public void setVideoId(String videoId) {
        VideoId = videoId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setContentURL(String contentURL) {
        this.contentURL = contentURL;
    }

    public String getContentURL() {
        return contentURL;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
