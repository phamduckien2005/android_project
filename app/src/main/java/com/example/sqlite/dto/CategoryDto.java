package com.example.sqlite.dto;

public class CategoryDto {
    public String name;
    public int iconRes;
    public String color;

    public CategoryDto(String name, int iconRes, String color) {
        this.name = name;
        this.iconRes = iconRes;
        this.color = color;
    }
}
