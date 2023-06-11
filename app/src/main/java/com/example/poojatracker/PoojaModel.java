package com.example.poojatracker;

public class PoojaModel {
    String img,title,desc,contentURL;
    boolean status;

    public PoojaModel(String img,String title,String desc,String contentURL,boolean status)
    {
        this.img=img;
        this.title=title;
        this.desc=desc;
        this.contentURL=contentURL;
        this.status=status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
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

    public boolean isStatus() {
        return status;
    }

    public String getContentURL() {
        return contentURL;
    }

}
