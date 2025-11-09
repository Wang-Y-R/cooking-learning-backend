// ---------- 配置 ----------
const WS_URL = "ws://localhost:8080/ws"; // 改成你的地址（或内网IP）

// ---------- 连接与通用处理 ----------
window.connectWs = function () {
  if (window.ws && window.ws.readyState === WebSocket.OPEN) {
    console.log("已有连接");
    return;
  }
  window.ws = new WebSocket(WS_URL);

  ws.onopen = () => {
    console.log("[ws] open");
  };

  ws.onmessage = (ev) => {
    const raw = ev.data;
    // 尝试解析成 JSON，否则直接打印
    let parsed = null;
    try {
      parsed = JSON.parse(raw);
    } catch (e) {
      // 非 JSON（比如后端 step.toString() 这种），直接打印
    }
    if (parsed) {
      console.log("[ws] recv (json):", parsed);
      // 如果是 CONNECTED 消息，保存 sid
      if (parsed.type === "CONNECTED" && parsed.wsSessionId) {
        window.sid = parsed.wsSessionId;
        console.log("[ws] saved sid =", window.sid);
      }
    } else {
      console.log("[ws] recv (raw):", raw);
    }
  };

  ws.onclose = () => {
    console.log("[ws] closed");
    window.sid = undefined;
  };

  ws.onerror = (e) => {
    console.error("[ws] error", e);
  };
};

// ---------- 辅助：安全发送 ----------
window.wsSend = function (objOrStr) {
  if (!window.ws || window.ws.readyState !== WebSocket.OPEN) {
    console.warn("ws not open");
    return;
  }
  const payload =
    typeof objOrStr === "string" ? objOrStr : JSON.stringify(objOrStr);
  console.log("[ws] send:", payload);
  ws.send(payload);
};

// ---------- 操作函数：创建会话 ----------
// dishes: array of dish names, e.g. ["简易红烧肉","白灼虾"]
window.createSession = function (dishes) {
  if (!Array.isArray(dishes)) {
    console.error("dishes 必须是数组");
    return;
  }
  wsSend({
    type: "CREATE_SESSION",
    dishNames: dishes,
  });
};

// ---------- 操作函数：请求下一步 ----------
window.requestNext = function () {
  if (!window.sid) {
    console.warn("尚未得到 sid（等待 CONNECTED）");
  }
  wsSend({ type: "REQUEST_NEXT" }); // 你后端用 session.getId() 作为 key，服务端知道是谁发的
};

// ---------- 操作函数：启动当前阻塞步骤计时 ----------
window.startBlockable = function () {
  if (!window.sid) {
    console.warn("尚未得到 sid（等待 CONNECTED）");
  }
  wsSend({ type: "START_BLOCKABLE" });
};

// ---------- 心跳（可选） ----------
window.heartbeat = function () {
  wsSend({ type: "HEARTBEAT" });
};

// ---------- 关闭连接 ----------
window.closeWs = function () {
  if (window.ws) window.ws.close();
};

// ---------- 快速测试脚本（按顺序执行） ----------
// 1) connectWs()
// 2) 等 console 收到 CONNECTED 并有 sid（会自动保存到 window.sid）
// 3) createSession(["简易红烧肉","白灼虾"])   // 请使用你 repo 中存在的菜名
// 4) requestNext()   // 拉取第一步（后端会 advance 并返回 step，注意你后端 pollNext 返回可能是 step.toString）
// 5) 如果该 step 是可阻塞的（isBlockable），调用 startBlockable() 启动计时（服务端会 schedule）
// 6) 等服务端在定时结束时推送 "BLOCK_FINISHED, NEXT STEP: ..."（你在 onmessage 会看到）
// 7) 重复 requestNext / startBlockable 流程直到 SESSION_END 或 NO_STEP


// ---------- 测试示例 ----------
// connectWs()
// undefined
// VM44:12 [ws] open
// VM44:25 [ws] recv (json): {type: 'CONNECTED', wsSessionId: 'd1905279-809b-ae71-ac3e-6382c9596fd6'}
// VM44:29 [ws] saved sid = d1905279-809b-ae71-ac3e-6382c9596fd6
// createSession(["简易红烧肉","白灼虾"])
// VM44:54 [ws] send: {"type":"CREATE_SESSION","dishNames":["简易红烧肉","白灼虾"]}
// undefined
// VM44:25 [ws] recv (json): {type: 'CREATE_SESSION', message: 'success'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 1, description: '猪五花肉切大块（约4.5cm，冷冻半小时至一小时更好切）', timeRequirement: null, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 2, description: '冷水锅中放入切好的猪五花肉，加入料酒与葱姜，煮15分钟去掉血腥', timeRequirement: {…}, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): currentStepIsBlockableButNotStart, need call block start
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'd1905279-809b-ae71-ac3e-6382c9596fd6'}
// startBlockable()
// VM44:54 [ws] send: {"type":"START_BLOCKABLE"}
// undefined
// VM44:25 [ws] recv (json): {type: 'START_BLOCKABLE', message: 'success'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '白灼虾', stepNumber: 1, description: '洋葱切小块，姜切片，平铺平底锅', timeRequirement: null, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '白灼虾', stepNumber: 2, description: '活虾冲洗一下（去除虾线、剪刀减掉虾腿虾须子都是可选操作），控水，铺在平底锅的洋葱、姜片之上', timeRequirement: null, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '白灼虾', stepNumber: 3, description: '锅内倒入料酒，盖上锅盖，中火1分钟，小火5分钟，关火5分钟', timeRequirement: {…}, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): currentStepIsBlockableButNotStart, need call block start
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'd1905279-809b-ae71-ac3e-6382c9596fd6'}
// startBlockable()
// VM44:54 [ws] send: {"type":"START_BLOCKABLE"}
// undefined
// VM44:25 [ws] recv (json): {type: 'START_BLOCKABLE', message: 'success'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 1 StepIdx: 2wait... left: 657seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'd1905279-809b-ae71-ac3e-6382c9596fd6'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 1 StepIdx: 2wait... left: 646seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'd1905279-809b-ae71-ac3e-6382c9596fd6'}
// VM44:37 [ws] closed
// connectWs()
// undefined
// VM44:12 [ws] open
// VM44:25 [ws] recv (json): {type: 'CONNECTED', wsSessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// VM44:29 [ws] saved sid = acbaa801-fbae-668f-5f56-a2de956d52d9
// createSession(["简易红烧肉","白灼虾"])
// VM44:54 [ws] send: {"type":"CREATE_SESSION","dishNames":["简易红烧肉","白灼虾"]}
// undefined
// VM44:25 [ws] recv (json): {type: 'CREATE_SESSION', message: 'success'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 1, description: '猪五花肉切大块（约4.5cm，冷冻半小时至一小时更好切）', timeRequirement: null, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 2, description: '冷水锅中放入切好的猪五花肉，加入料酒与葱姜，煮15分钟去掉血腥', timeRequirement: {…}, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): currentStepIsBlockableButNotStart, need call block start
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// startBlockable()
// VM44:54 [ws] send: {"type":"START_BLOCKABLE"}
// undefined
// VM44:25 [ws] recv (json): {type: 'START_BLOCKABLE', message: 'success'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '白灼虾', stepNumber: 1, description: '洋葱切小块，姜切片，平铺平底锅', timeRequirement: null, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '白灼虾', stepNumber: 2, description: '活虾冲洗一下（去除虾线、剪刀减掉虾腿虾须子都是可选操作），控水，铺在平底锅的洋葱、姜片之上', timeRequirement: null, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '白灼虾', stepNumber: 3, description: '锅内倒入料酒，盖上锅盖，中火1分钟，小火5分钟，关火5分钟', timeRequirement: {…}, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): currentStepIsBlockableButNotStart, need call block start
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// VM44:32 [ws] recv (raw): BLOCK_FINISHED: {"dishName":"简易红烧肉","stepNumber":2,"description":"冷水锅中放入切好的猪五花肉，加入料酒与葱姜，煮15分钟去掉血腥","timeRequirement":{"duration":"15分钟","type":"exact"},"targetCondition":null,"isBlockable":true,"heatLevel":null}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 3, description: '锅中放入两片生姜提味', timeRequirement: null, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 4, description: '开中小火后直接加入五花肉，不需要放入食用油，每块五花肉六个面都煎一下', timeRequirement: null, targetCondition: '煎至出油', …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 5, description: '将煎出的油倒出备用，并将五花肉推至一边，加入15g冰糖，翻炒', timeRequirement: null, targetCondition: '冰糖融化', …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 6, description: '融化后将五花肉与冰糖炒至融合上色，加入生抽10ml、老抽15ml、料酒5ml，翻炒至上色', timeRequirement: null, targetCondition: '上色', …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 7, description: '加入烧好的开水炖煮40分钟，并放入生姜2片、香叶3片、八角2个', timeRequirement: {…}, targetCondition: null, …}
// startBlockable()
// VM44:54 [ws] send: {"type":"START_BLOCKABLE"}
// undefined
// VM44:25 [ws] recv (json): {type: 'START_BLOCKABLE', message: 'success'}
// startBlockable()
// VM44:54 [ws] send: {"type":"START_BLOCKABLE"}
// undefined
// VM44:25 [ws] recv (json): {type: 'START_BLOCKABLE', message: 'success'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 1 StepIdx: 2wait... left: 8seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 1 StepIdx: 2wait... left: 2seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// VM44:32 [ws] recv (raw): BLOCK_FINISHED: {"dishName":"白灼虾","stepNumber":3,"description":"锅内倒入料酒，盖上锅盖，中火1分钟，小火5分钟，关火5分钟","timeRequirement":{"duration":"11分钟","type":"exact"},"targetCondition":null,"isBlockable":true,"heatLevel":"中火转小火转关火"}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '白灼虾', stepNumber: 4, description: '和上一步并行操作，制作蘸料：葱切成葱花、蒜切碎、倒入酱油、芝麻、香醋，搅拌之', timeRequirement: null, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '白灼虾', stepNumber: 5, description: '油烧热，淋入蘸料', timeRequirement: null, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '白灼虾', stepNumber: 6, description: '虾出锅，用干净的盘子装好', timeRequirement: null, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 0 StepIdx: 6wait... left: 19seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 0 StepIdx: 6wait... left: 9seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// VM44:32 [ws] recv (raw): BLOCK_FINISHED: {"dishName":"简易红烧肉","stepNumber":7,"description":"加入烧好的开水炖煮40分钟，并放入生姜2片、香叶3片、八角2个","timeRequirement":{"duration":"40分钟","type":"exact"},"targetCondition":null,"isBlockable":true,"heatLevel":null}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 8, description: '盖上锅盖煮至沸腾后，加入煮好扎好孔的鹌鹑蛋和豆皮，开中小火，等待40分钟', timeRequirement: {…}, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): currentStepIsBlockableButNotStart, need call block start
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// startBlockable()
// VM44:54 [ws] send: {"type":"START_BLOCKABLE"}
// undefined
// VM44:25 [ws] recv (json): {type: 'START_BLOCKABLE', message: 'success'}
// startBlockable()
// VM44:54 [ws] send: {"type":"START_BLOCKABLE"}
// undefined
// VM44:25 [ws] recv (json): {type: 'ERROR', message: 'Not Blockable!'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 0 StepIdx: 7wait... left: 37seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 0 StepIdx: 7wait... left: 36seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 0 StepIdx: 7wait... left: 35seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 0 StepIdx: 7wait... left: 34seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 0 StepIdx: 7wait... left: 34seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 0 StepIdx: 7wait... left: 27seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): RecipeIdx: 0 StepIdx: 7wait... left: 7seconds
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
// VM44:32 [ws] recv (raw): BLOCK_FINISHED: {"dishName":"简易红烧肉","stepNumber":8,"description":"盖上锅盖煮至沸腾后，加入煮好扎好孔的鹌鹑蛋和豆皮，开中小火，等待40分钟","timeRequirement":{"duration":"40分钟","type":"exact"},"targetCondition":null,"isBlockable":true,"heatLevel":"中小火"}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 9, description: '打开锅盖，待汤汁快没有的时候开大火收汁（切记不可收干）', timeRequirement: null, targetCondition: '汤汁收至合适程度', …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 10, description: '加入2-3g盐，翻炒一下，就可以出锅了', timeRequirement: null, targetCondition: null, …}
// requestNext()
// VM44:54 [ws] send: {"type":"REQUEST_NEXT"}
// undefined
// VM44:32 [ws] recv (raw): all dishes done
// VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}