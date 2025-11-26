package com.example.cooking.dao.repository;

import com.example.cooking.dao.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findByDishName(String dishName);

    List<Recipe> findByDishNameContainingIgnoreCase(String keyword);

    List<Recipe> findByCategoryIgnoreCase(String category);
}
