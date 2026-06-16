package com.example.sqlite.dto;

public class ReceiptResultDto {
    public final String title;
    public final double amount;
    public final String category;
    public final String note;
    public final String rawJson;

    public ReceiptResultDto(String title, double amount, String category, String note) {
        this(title, amount, category, note, "");
    }

    public ReceiptResultDto(String title, double amount, String category, String note, String rawJson) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.rawJson = rawJson;
    }
}
