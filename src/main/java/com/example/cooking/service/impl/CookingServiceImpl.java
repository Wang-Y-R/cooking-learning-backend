package com.example.cooking.service.impl;

import com.example.cooking.common.web.WsHandler;
import com.example.cooking.dao.entity.Recipe;
import com.example.cooking.dao.entity.Step;
import com.example.cooking.dao.repository.RecipeRepository;
import com.example.cooking.dto.CookingRuntime;
import com.example.cooking.service.CookingService;
import com.example.cooking.utils.TimeParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class CookingServiceImpl implements CookingService {

    private final Map<String, CookingRuntime> cookingMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    private final RecipeRepository recipeRepository;  // to change name

    // create session from dishNames JSON array node (from WsHandler)
    public Boolean createSessionWithDishNames(String sid, com.fasterxml.jackson.databind.JsonNode dishNamesNode) {
        List<Recipe> recipes = new ArrayList<>();
        // 从数据库找并加入recipes
        for (com.fasterxml.jackson.databind.JsonNode n : dishNamesNode) {
            String name = n.asText();
            Recipe r = recipeRepository.findByName(name);
            if(r!=null){
                recipes.add(r);
            }else{
                System.err.println("no such dish: " + name);
                return false;
            }
        }

        Map<Integer, Integer> stepMap = new HashMap<>();
        for(int i=0;i<recipes.size();i++){
            stepMap.put(i,0);
        }
        CookingRuntime cookingRuntime = CookingRuntime.builder()
                .sid(sid)
                .recipes(recipes)
                .stepMap(stepMap)
                .currentRecipeIndex(0)
                .build();

        cookingMap.put(sid, cookingRuntime);

        return true;
    }


    // optional: bind by userId
//    public void bindUserWs(String userId, String wsSessionId) {
//        // 简单实现：把 userId 存到所有匹配 session 上（或维护 user->session 映射）
//        // 此处省略复杂逻辑
//    }

    // unbind wsSessionId（连接断开）
    public void unbindWsSession(String sid) {
        cookingMap.remove(sid);
    }


    // 客户端主动拉取下一步（并消费）
    public String pollNextStepAndConsume(String sid) {
        CookingRuntime cookingRuntime = cookingMap.get(sid);
        List<Recipe> recipes = cookingRuntime.getRecipes();
        int curRecipeIdx = cookingRuntime.getCurrentRecipeIndex();

        if(curRecipeIdx >= recipes.size()){
            // 当前没有下一步了
            return null;
        }
        Recipe curRecipe = recipes.get(curRecipeIdx);

        Map<Integer,Integer> stepMap = cookingRuntime.getStepMap();
        // 取当前一步
        int curStepIdx = stepMap.get(curRecipeIdx);
        if(curStepIdx >= recipes.get(curRecipeIdx).getSteps().size()){
            // 当前没有下一步了
            return null;
        }
        Step step = curRecipe.getSteps().get(curStepIdx);
        if(step.getIsBlockable()){
            System.out.println("当前步isblockable，直接返回让用户确认");
            return step.toString();
        }

        stepMap.put(curRecipeIdx, stepMap.get(curRecipeIdx) + 1);

        while (curRecipeIdx <= recipes.size() && recipes.get(curRecipeIdx).getSteps().size() <= curStepIdx){
            curRecipeIdx++;
            curStepIdx = stepMap.get(curRecipeIdx);
        }
        cookingRuntime.setCurrentRecipeIndex(curRecipeIdx);
        return step.toString();
    }

    public Boolean startBlockabled(String sid){
        CookingRuntime cookingRuntime = cookingMap.get(sid);
        List<Recipe> recipes = cookingRuntime.getRecipes();
        int curRecipeIdx = cookingRuntime.getCurrentRecipeIndex();
        if(curRecipeIdx >= recipes.size()){
            // 当前没有下一步了
            return false;
        }

        Recipe curRecipe = recipes.get(curRecipeIdx);
        int curStepIdx = cookingRuntime.getStepMap().get(curRecipeIdx);
        if(curStepIdx >= recipes.get(curRecipeIdx).getSteps().size()){
            // 当前没有下一步了
            return false;
        }
        Step step = curRecipe.getSteps().get(curStepIdx);

        if(!step.getIsBlockable()){
            return false;
        }

        int blockSeconds = TimeParser.parseMinutes(step.getTimeRequirement().getDuration());


        // set second for testing
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            // 时间到后自动执行
            this.finishBlockable(sid, curRecipeIdx);
        }, blockSeconds, TimeUnit.SECONDS);

//        ScheduledFuture<?> future = scheduler.schedule(() -> {
//            // 时间到后自动执行
//            this.finishBlockable(sid, curRecipeIdx);
//        }, blockSeconds, TimeUnit.MINUTES);

        // 可以开始做下一道菜
        cookingRuntime.setCurrentRecipeIndex(curRecipeIdx+1);

        return true;

    }

    public void finishBlockable(String sid, int curRecipeIdx){
        System.out.println("finishBlockable sid: "+ sid + "curReciprIdx:" + curRecipeIdx);
        CookingRuntime cookingRuntime = cookingMap.get(sid);
        if(cookingRuntime == null) return;



        cookingRuntime.setCurrentRecipeIndex(curRecipeIdx);

        List<Recipe> recipes = cookingRuntime.getRecipes();
        if(curRecipeIdx >= recipes.size()){
            // 当前没有下一步了
            WsHandler.sendToWsSession(sid, "BLOCK_FINISHED, NEXT STEP: NONE\n");
        }
        Recipe curRecipe = recipes.get(curRecipeIdx);

        Map<Integer,Integer> stepMap = cookingRuntime.getStepMap();
        // 取当前一步
        int curStepIdx = stepMap.get(curRecipeIdx);
        if(curStepIdx >= recipes.get(curRecipeIdx).getSteps().size()){
            // 当前没有下一步了
            WsHandler.sendToWsSession(sid, "BLOCK_FINISHED, NEXT STEP: NONE\n");
        }
        Step step = curRecipe.getSteps().get(curStepIdx);

        stepMap.put(curRecipeIdx, stepMap.get(curRecipeIdx) + 1);

        while (curRecipeIdx <= recipes.size() && recipes.get(curRecipeIdx).getSteps().size() <= curStepIdx){
            curRecipeIdx++;
            curStepIdx = stepMap.get(curRecipeIdx);
        }
        cookingRuntime.setCurrentRecipeIndex(curRecipeIdx);

        WsHandler.sendToWsSession(sid, "BLOCK_FINISHED, NEXT STEP: \n" + step.toString());
    }
}
