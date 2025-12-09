package com.example.cooking.service.impl;

import com.example.cooking.common.convention.wsmessage.WSMessage;
import com.example.cooking.common.web.WebSocketManager;
import com.example.cooking.handler.WsHandler;
import com.example.cooking.dao.entity.Recipe;
import com.example.cooking.dao.entity.Step;
import com.example.cooking.dao.repository.RecipeRepository;
import com.example.cooking.dao.entity.CookingRuntime;
import com.example.cooking.service.CookingService;
import com.example.cooking.utils.TimeParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

import static com.example.cooking.common.constant.WSMessageType.*;

@Service
@RequiredArgsConstructor
public class CookingServiceImpl implements CookingService {

    private final WebSocketManager webSocketManager;
    private final Map<String, CookingRuntime> cookingMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    private final RecipeRepository recipeRepository;

    private static final ObjectMapper M = new ObjectMapper();

    // create session from dishNames JSON array node (from WsHandler)
    public Boolean createSessionWithDishNames(String sid, com.fasterxml.jackson.databind.JsonNode dishNamesNode) {
        List<Recipe> recipes = new ArrayList<>();
        // 从数据库找并加入recipes
        for (com.fasterxml.jackson.databind.JsonNode n : dishNamesNode) {
            String name = n.asText();
            Optional<Recipe> recipeOpt = recipeRepository.findByDishName(name);
            if(recipeOpt.isPresent()){
                recipes.add(recipeOpt.get());
            }else{
                System.err.println("no such dish: " + name);
                return false;
            }
        }

        Map<Integer, Integer> stepMap = new HashMap<>();
        for(int i=0;i<recipes.size();i++){
            stepMap.put(i,-1);
        }
        CookingRuntime cookingRuntime = CookingRuntime.builder()
                .sid(sid)
                .recipes(recipes)
                .stepMap(stepMap)
                .currentRecipeIndex(0)
                .taskMap(new HashMap<>())
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
    public Boolean pollNextStepAndConsume(String sid) {
        if(!currentRecipeExist(sid)){
            if(!checkTasksExist(sid)) {
                // 没有下一步且没有菜在等待
                webSocketManager.send(sid,
                        WSMessage.buildSuccess(NO_NEXT_STEP, "All dishes done !", null)
                );
            }
            return false;
        }


        // 处理：目前指向blockable步但还没开始
        if(currentStepIsBlockableButNotStart(sid)) {
            System.out.println("currentStepIsBlockableButNotStart, need call block start");
            CookingRuntime cookingRuntime = cookingMap.get(sid);
            int curRecipeIdx = cookingRuntime.getCurrentRecipeIndex();
            List<Recipe> recipes = cookingRuntime.getRecipes();
            Recipe curRecipe = recipes.get(curRecipeIdx);
            String dishName = curRecipe.getDishName();

            int curStepIdx = cookingRuntime.getStepMap().get(curRecipeIdx);
            Step step = curRecipe.getSteps().get(curStepIdx);

            webSocketManager.send(sid,
                    WSMessage.buildSuccess(NO_NEXT_STEP,
                            "Current step is blockable but not started, need call START_BLOCKABLE !",
                            toJsonStep(dishName,step))
            );
            return false;
        }

        if(gotoNextStepifPresent(sid)){
            CookingRuntime cookingRuntime = cookingMap.get(sid);
            int curRecipeIdx = cookingRuntime.getCurrentRecipeIndex();
            List<Recipe> recipes = cookingRuntime.getRecipes();
            Recipe curRecipe = recipes.get(curRecipeIdx);
            String dishName = curRecipe.getDishName();

            int curStepIdx = cookingRuntime.getStepMap().get(curRecipeIdx);
            Step step = curRecipe.getSteps().get(curStepIdx);

            if(step.getIsBlockable()){
                System.out.println("current step is blockable");
            }

            webSocketManager.send(sid,
                    WSMessage.buildSuccess(REQUEST_NEXT, null, toJsonStep(dishName, step))
            );
            return true;
        } else if(!checkTasksExist(sid)) {
            // 没有下一步且没有菜在等待
            webSocketManager.send(sid,
                    WSMessage.buildSuccess(NO_NEXT_STEP, "All dishes done !", null)
            );
            return false;
        }
        return true;

    }

    private Boolean checkTasksExist(String sid){
        CookingRuntime cookingRuntime = cookingMap.get(sid);
        for(Map.Entry<String, ScheduledFuture<?>> e : cookingRuntime.getTaskMap().entrySet()){
            String key = e.getKey();
            ScheduledFuture<?> task = e.getValue();
//                Long delay = task.getDelay(TimeUnit.MINUTES);
            Long delay = task.getDelay(TimeUnit.SECONDS);
            int separator = key.indexOf('+');
            int recipeIdx = Integer.parseInt(key.substring(0, separator));
            int stepIdx = Integer.parseInt(key.substring(separator + 1));
            Recipe recipe = cookingRuntime.getRecipes().get(recipeIdx);
            String dishName = recipe.getDishName();
            Step step = recipe.getSteps().get(stepIdx);

            // 已经完成了的任务，但还没处理
            // https://fcnd3knzrt0y.feishu.cn/wiki/KnJEwg96SiTzeDkLWeicRFFdnJb issue1
            if(delay<0) {
                cookingRuntime.setCurrentRecipeIndex(recipeIdx);
                pollNextStepAndConsume(sid);
                return true;
            }

            webSocketManager.send(sid,
                    WSMessage.buildSuccess(NO_NEXT_STEP,
                            "dish waiting ... " + delay + "seconds left !",
                            toJsonStep(dishName, step)
                    )
            );
//            webSocketManager.send(sid,
//                    WSMessage.buildSuccess(NO_NEXT_STEP,
//                            "dish waiting ... " + delay + "minutes left !",
//                            toJsonStep(dishName, step)
//                    )
//            );
            return true;
        }
        return false;
    }
    public Boolean startBlockabled(String sid){
        if(!currentRecipeExist(sid)){
            return false;  // 理论上不能到这里
        }
        CookingRuntime cookingRuntime = cookingMap.get(sid);

        List<Recipe> recipes = cookingRuntime.getRecipes();
        int curRecipeIdx = cookingRuntime.getCurrentRecipeIndex();
        Recipe curRecipe = recipes.get(curRecipeIdx);

        int curStepIdx = cookingRuntime.getStepMap().get(curRecipeIdx);
        Step step = curRecipe.getSteps().get(curStepIdx);

        // 如果blockable，设定定时任务
        if(step.getIsBlockable()){
            int blockMinutes = TimeParser.parseMinutes(step.getTimeRequirement().getDuration());

            // set second for testing
            ScheduledFuture<?> future = scheduler.schedule(() -> {
                // 时间到后自动执行
                this.finishBlockable(sid, curRecipeIdx);
            }, blockMinutes, TimeUnit.SECONDS);

//                ScheduledFuture<?> future = scheduler.schedule(() -> {
//                    // 时间到后自动执行
//                    this.finishBlockable(sid, curRecipeIdx);
//                }, blockMinutes, TimeUnit.MINUTES);
            String curRecipeIdxStr = String.valueOf(curRecipeIdx);
            String curStepIdxStr =  String.valueOf(curStepIdx);
            cookingRuntime.getTaskMap().put(curRecipeIdxStr + "+" + curStepIdxStr, future);
        } else {
            return false;
        }

        // 可以开始做下一道菜
        cookingRuntime.setCurrentRecipeIndex(curRecipeIdx + 1);

        return true;
    }

    public void finishBlockable(String sid, int curRecipeIdx){
        System.out.println("finishBlockable sid: "+ sid + "curReciprIdx:" + curRecipeIdx);
        CookingRuntime cookingRuntime = cookingMap.get(sid);
        if(cookingRuntime == null) return;

        cookingRuntime.setCurrentRecipeIndex(curRecipeIdx);
        List<Recipe> recipes = cookingRuntime.getRecipes();

        Recipe curRecipe = recipes.get(curRecipeIdx);
        String dishName = curRecipe.getDishName();

        Map<Integer,Integer> stepMap = cookingRuntime.getStepMap();
        // 取当前一步
        int curStepIdx = stepMap.get(curRecipeIdx);

        Step step = curRecipe.getSteps().get(curStepIdx);

        webSocketManager.send(sid,
                 WSMessage.buildSuccess(BLOCK_FINISHED,
                         "Block finishedd ! Please go to deal the prev dish",
                         toJsonStep(dishName, step)
                 )
        );
    }


    private Boolean currentRecipeExist(String sid) {
        CookingRuntime cookingRuntime = cookingMap.get(sid);
        List<Recipe> recipes = cookingRuntime.getRecipes();
        int curRecipeIdx = cookingRuntime.getCurrentRecipeIndex();

        if(curRecipeIdx >= recipes.size()){
            return false;
        }
        return true;
    }

    /*
    如果有下一步，指针指向下一步，并返回true
    否则直接返回false
     */
    private Boolean gotoNextStepifPresent (String sid){
        CookingRuntime cookingRuntime = cookingMap.get(sid);
        List<Recipe> recipes = cookingRuntime.getRecipes();
        int curRecipeIdx = cookingRuntime.getCurrentRecipeIndex();

        if(curRecipeIdx >= recipes.size()){
            // 当前没有下一步了
            return false;
        }

        Map<Integer,Integer> stepMap = cookingRuntime.getStepMap();
        // 取下一步
        int curStepIdx = stepMap.get(curRecipeIdx) + 1;
        // 当前的菜做完了，进入下一道
        while (curRecipeIdx < recipes.size() &&  curStepIdx  >= recipes.get(curRecipeIdx).getSteps().size()){
            curRecipeIdx++;
            if(curRecipeIdx < recipes.size()) {
                curStepIdx = stepMap.get(curRecipeIdx) + 1;
            }
        }
        // 遍历完了要做的菜也没找到下一步
        if(curRecipeIdx >= recipes.size()){
            return false;
        }
        // 找到了,设置当前步的信息
        cookingRuntime.setCurrentRecipeIndex(curRecipeIdx);
        stepMap.put(curRecipeIdx, curStepIdx);
        return true;
    }

    private JsonNode toJsonStep(String recipeName, Step step){
        String imagePrefix = "/generated_images/" + recipeName + "/step_";
        String imageName = String.format("%02d.png", step.getStepNumber()); // 补零到两位
        step.setImageUrl(imagePrefix + imageName);
        ObjectNode node = M.createObjectNode().put("dishName", recipeName);
        JsonNode stepNode = M.valueToTree(step);
        node.setAll((ObjectNode) stepNode);
        return node;
    }


    private Boolean currentStepIsBlockableButNotStart(String sid){
        CookingRuntime cookingRuntime = cookingMap.get(sid);
        List<Recipe> recipes = cookingRuntime.getRecipes();
        int curRecipeIdx = cookingRuntime.getCurrentRecipeIndex();
        Recipe recipe = recipes.get(curRecipeIdx);

        int curStepIdx = cookingRuntime.getStepMap().get(curRecipeIdx);
        // 还没开始
        if(curStepIdx==-1){
            return false;
        }

        String curRecipeIdxStr = String.valueOf(curRecipeIdx);
        String curStepIdxStr =  String.valueOf(curStepIdx);
        if(recipe.getSteps().get(curStepIdx).getIsBlockable() &&
                (!cookingRuntime.getTaskMap().containsKey(curRecipeIdxStr + "+" + curStepIdxStr))){
            return true;
        }
        // 如果是blockable且已经做过，删掉记录
        if(recipe.getSteps().get(curStepIdx).getIsBlockable()){
            cookingRuntime.getTaskMap().remove(curRecipeIdxStr + "+" + curStepIdxStr);
        }
        return false;
    }

    @Override
    public Optional<CookingRuntime> getRuntime(String sid) {
        return Optional.ofNullable(cookingMap.get(sid));
    }

}
