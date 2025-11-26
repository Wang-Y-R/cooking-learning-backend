package com.example.cooking.dao.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "recipes")
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String dishName;           // 菜名

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recipe_images", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "image_url")
    private List<String> images;       // 图片列表

    @Column(length = 1000)
    private String description;        // 描述

    private Integer difficulty;        // 难度 1-5

    private String servings;           // 份量

    private String category;           // 分类（enum）

    @Embedded
    private Ingredients ingredients;   // 配料

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "recipe_id")
    @OrderBy("stepNumber ASC")
    private List<Step> steps;          // 步骤
}
