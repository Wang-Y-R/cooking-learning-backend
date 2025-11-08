package com.example.cooking.dto;

import com.example.cooking.dao.entity.Recipe;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Data
@Builder
public class CookingRuntime {
    private String sid;

    private List<Recipe> recipes;     // 用户这次选的所有菜
    private int currentRecipeIndex;   // 当前做到第几个菜

    private Map<Integer, Integer> stepMap;
}