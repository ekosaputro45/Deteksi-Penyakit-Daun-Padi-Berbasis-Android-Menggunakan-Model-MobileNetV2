package com.example.deteksipenyakitdaunpadi;

public class NewsItem {
    public final String title;
    public final String source;
    public final String date;
    public final String summary;
    public final String url;
    public final String imageUrl;

    public NewsItem(String title, String source, String date, String summary, String url, String imageUrl) {
        this.title = title;
        this.source = source;
        this.date = date;
        this.summary = summary;
        this.url = url;
        this.imageUrl = imageUrl;
    }
}
