package com.example.sqlite.entity;

public class Message {
    public String title;
    public String content;
    public String time;

    public Message(String title, String content, String time) {
        this.title = title;
        this.content = content;
        this.time = time;
    }
}
