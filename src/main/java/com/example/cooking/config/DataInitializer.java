package com.example.cooking.config;

import com.example.cooking.dao.entity.Recipe;
import com.example.cooking.dao.repository.RecipeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RecipeRepository recipeRepository;

    private final ObjectMapper objectMapper;
    @Override
    public void run(String... args) throws Exception {
        InputStream is = null;
        try {
            // 1. 优先从 classpath (src/main/resources/data.json) 读取
            is = this.getClass().getClassLoader().getResourceAsStream("data.json");


            if (is == null) {
                System.out.println(">>> DataInitializer: 未找到 data.json（classpath:/data.json 或 /mnt/data/data.json）");
                return;
            }

            // 3. 解析为 List<Recipe>
            List<Recipe> recipes = objectMapper.readValue(is, new TypeReference<List<Recipe>>() {});

            // 4. 检查数据库是否已有数据，避免重复加载
            if (recipeRepository.count() == 0) {
                // 全部保存到数据库
                recipeRepository.saveAll(recipes);
                System.out.println(">>> Recipe data initialized, count = " + recipes.size());
            } else {
                System.out.println(">>> Recipe data already exists, skipping initialization");
            }
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException ignored) {}
            }
        }
    }
}