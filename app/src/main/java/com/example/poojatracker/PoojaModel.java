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

    public String VideoIdFromUrl(String url)
    {
        String VideoId = "gXWXKjR-qII"; //default video
        for(int i=0;i<url.length()-1;i++)
        {
            if(url.charAt(i)=='v' && url.charAt(i+1)=='=')
            {
                VideoId=url.substring(i+2,i+13);
                break;
            }
        }
        if(VideoId.equals("gXWXKjR-qII"))
        {
            if(url.length()>=11)
                VideoId=url.substring(url.length()-11,url.length());
        }
        return VideoId;
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
