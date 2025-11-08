package com.example.cooking.dao.entity;

import lombok.Data;

import java.util.List;

@Data
public class Recipe {
    private String dishName;           // 菜名
    private List<String> images;       // 图片列表
    private String description;        // 描述
    private Integer difficulty;        // 难度 1-5
    private String servings;           // 份量
    private String category;           // 分类（enum）
    private Ingredients ingredients;   // 配料
    private List<Step> steps;          // 步骤
}