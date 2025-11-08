package com.example.cooking.controller;

import com.example.cooking.dao.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HelloController {
    private final RecipeRepository recipeRepository;

    @GetMapping()
    public String sayhello() {
        return "Hello";
    }

    @GetMapping("/repo")
    public String getrepo() {
        return recipeRepository.findAll().toString();
    }

}
