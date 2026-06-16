package com.example.sqlite.entity;

public class Transaction {
    public int id;
    public String title;
    public String time;
    public String category;
    public double amount;
    public boolean isExpense;
    public long timestamp;
    public boolean isHeader = false;
    public String daySummary = "";

    public Transaction(int id, String title, String time, double amount, boolean isExpense, long timestamp) {
        this(id, title, time, amount, isExpense, timestamp, title);
    }

    public Transaction(int id, String title, String time, double amount, boolean isExpense, long timestamp, String category) {
        this.id = id;
        this.title = title;
        this.time = time;
        this.amount = amount;
        this.isExpense = isExpense;
        this.timestamp = timestamp;
        this.category = category;
    }

    public Transaction(String date, String summary) {
        this.title = date;
        this.daySummary = summary;
        this.isHeader = true;
    }
}
