package com.example.cooking.dao.repository;

import com.example.cooking.dao.entity.Recipe;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class RecipeRepository {
    private final List<Recipe> recipes = new ArrayList<>();

    public List<Recipe> findAll() {
        return recipes;
    }

    public void addRecipe(Recipe recipe) {
        recipes.add(recipe);
    }

    public Recipe findByName(String name){
        for(Recipe r : recipes){
            if(name.equals(r.getDishName())){
                return r;
            }
        }
        return null;
    }
}