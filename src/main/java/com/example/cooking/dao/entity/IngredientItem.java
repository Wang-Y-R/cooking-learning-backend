package com.example.cooking.dao.entity;

import lombok.Data;
import jakarta.persistence.Embeddable;

@Data
@Embeddable
public class IngredientItem {
    private String name;    // 名字
    private String amount;  // 用量 100g / 2个 / 适量
    private String note;    // 备注 （可以为空）
}