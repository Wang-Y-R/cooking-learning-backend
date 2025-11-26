package com.example.cooking.controller;

import com.example.cooking.dao.entity.CookingRuntime;
import com.example.cooking.dao.entity.IngredientItem;
import com.example.cooking.dao.entity.Recipe;
import com.example.cooking.dao.repository.RecipeRepository;
import com.example.cooking.service.CookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SessionController {

    private final CookingService cookingService;
    private final RecipeRepository recipeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, String>> createSession(@RequestBody CreateSessionRequest req) {
        String sid = UUID.randomUUID().toString();
        ArrayNode arr = objectMapper.createArrayNode();
        req.getDishes().forEach(arr::add);
        boolean ok = cookingService.createSession(sid, arr);
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("error", "创建会话失败，菜名不存在"));
        }
        return ResponseEntity.ok(Map.of("sessionId", sid));
    }

    @PostMapping("/sessions/{sid}/next")
    public ResponseEntity<Map<String, Object>> next(@PathVariable String sid) {
        boolean ok = cookingService.pollNextStepAndConsume(sid);
        return ResponseEntity.ok(Map.of("success", ok));
    }

    @PostMapping("/sessions/{sid}/blockable")
    public ResponseEntity<Map<String, Object>> startBlockable(@PathVariable String sid) {
        boolean ok = cookingService.startBlockabled(sid);
        return ResponseEntity.ok(Map.of("success", ok));
    }

    @GetMapping("/sessions/{sid}")
    public ResponseEntity<?> sessionState(@PathVariable String sid) {
        Optional<CookingRuntime> runtimeOpt = cookingService.getRuntime(sid);
        return runtimeOpt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/shopping-list")
    public ResponseEntity<Map<String, Object>> shoppingList(@RequestParam List<Long> recipeIds) {
        List<Recipe> recipes = recipeRepository.findAllById(recipeIds);
        List<IngredientItem> all = new ArrayList<>();
        for (Recipe r : recipes) {
            if (r.getIngredients() != null && r.getIngredients().getRequired() != null) {
                all.addAll(r.getIngredients().getRequired());
            }
        }
        // 简单合并同名食材
        Map<String, List<String>> merged = all.stream()
                .collect(Collectors.groupingBy(IngredientItem::getName,
                        Collectors.mapping(IngredientItem::getAmount, Collectors.toList())));

        List<Map<String, String>> items = merged.entrySet().stream().map(e -> {
            String amount = String.join(" / ", e.getValue());
            return Map.of("name", e.getKey(), "amount", amount);
        }).toList();

        return ResponseEntity.ok(Map.of(
                "count", items.size(),
                "items", items
        ));
    }

    @Data
    public static class CreateSessionRequest {
        // 支持两种格式：直接传菜名数组，或 { dishes: ["红烧肉"] }
        private List<String> dishes = new ArrayList<>();
    }
}
