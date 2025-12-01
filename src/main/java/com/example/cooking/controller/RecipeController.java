package com.example.cooking.controller;

import com.example.cooking.dao.entity.Recipe;
import com.example.cooking.dao.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {
    private final RecipeRepository recipeRepository;

    @GetMapping
    public ResponseEntity<List<Recipe>> getAllRecipes(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category
    ) {
        if (keyword != null && !keyword.isBlank()) {
            return ResponseEntity.ok(recipeRepository.findByDishNameContainingIgnoreCase(keyword.trim()));
        }
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(recipeRepository.findByCategoryIgnoreCase(category.trim()));
        }
        return ResponseEntity.ok(recipeRepository.findAll());
    }

    @GetMapping("/{dishName}")
    public ResponseEntity<Recipe> getRecipeByName(@PathVariable String dishName) {
        return recipeRepository.findByDishName(dishName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
