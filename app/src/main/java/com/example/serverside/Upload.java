package com.example.serverside;

public class Upload {

    public String name;
    public String url;
    public String songscategory;

    public Upload(String name, String url, String songscategory) {
        this.name = name;
        this.url = url;
        this.songscategory = songscategory;
    }

    public Upload() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSongscategory() {
        return songscategory;
    }

    public void setSongscategory(String songscategory) {
        this.songscategory = songscategory;
    }
}
