package com.example.cooking.dao.entity;

import lombok.Data;

import java.util.List;

@Data
public class Ingredients {
    private List<IngredientItem> required;   // 必要
    private List<IngredientItem> optional;   // 可选
}