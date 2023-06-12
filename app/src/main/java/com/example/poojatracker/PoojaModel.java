package com.example.poojatracker;

public class PoojaModel {
    int id;
    String title,desc,contentURL,VideoId;
    int status;

    public PoojaModel() {

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
        return VideoId;
    }
    public PoojaModel(int id,String title,String desc,String contentURL,int status)
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

    public void setStatus(int status) {
        this.status = status;
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

    public int isStatus() {
        return status;
    }

    public String getContentURL() {
        return contentURL;
    }

}
