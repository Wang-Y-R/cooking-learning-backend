# websocket通讯实现：
## 主要文件：
- com.example.cooking.common.web.WsHandler
- com.example.cooking.service.impl.CookingServiceImpl

## 测试流程：
方式：浏览器
示例代码与示例流程：ws_test/test.js

## 接口简要示例：
### 连接WS
connectWs()
VM44:25 [ws] recv (json): {type: 'CONNECTED', wsSessionId: 'd1905279-809b-ae71-ac3e-6382c9596fd6'}
VM44:29 [ws] saved sid = d1905279-809b-ae71-ac3e-6382c9596fd6

### 创建菜单：
createSession(["简易红烧肉","白灼虾"])
VM44:25 [ws] recv (json): {type: 'CREATE_SESSION', message: 'success'}

### 拉取下一步：
- 正常情况：
    - requestNext()
VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 1, description: '猪五花肉切大块（约4.5cm，冷冻半小时至一小时更好切）', timeRequirement: null, targetCondition: null, …}

- 如果当前步骤可阻塞，需要先发送确认阻塞开始：
    - requestNext()
VM44:25 [ws] recv (json): {dishName: '简易红烧肉', stepNumber: 2, description: '冷水锅中放入切好的猪五花肉，加入料酒与葱姜，煮15分钟去掉血腥', timeRequirement: {…}, targetCondition: null, …} （可阻塞步骤）
    - requestNext()
VM44:32 [ws] recv (raw): currentStepIsBlockableButNotStart, need call block start
VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'd1905279-809b-ae71-ac3e-6382c9596fd6'}
- 发送确认阻塞开始：
  - startBlockable()
VM44:25 [ws] recv (json): {type: 'START_BLOCKABLE', message: 'success'}
- 如果上一步发送了阻塞开始，下一次拉取下一步是下一个菜：
  - requestNext()
VM44:25 [ws] recv (json): {dishName: '白灼虾', stepNumber: 1, description: '洋葱切小块，姜切片，平铺平底锅', timeRequirement: null, targetCondition: null, …}
- 阻塞时间到，服务端主动发信息，并回到前面的步骤继续：
  - VM44:32 [ws] recv (raw): BLOCK_FINISHED: {"dishName":"简易红烧肉","stepNumber":2,"description":"冷水锅中放入切好的猪五花肉，加入料酒与葱姜，煮15分钟去掉血腥","timeRequirement":{"duration":"15分钟","type":"exact"},"targetCondition":null,"isBlockable":true,"heatLevel":null}
- 如果当前没有下一步可做了，但还有任务在等待，返回等待任务信息：
  - requestNext()
VM44:32 [ws] recv (raw): RecipeIdx: 1 StepIdx: 2wait... left: 2seconds
VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}
- 如果当前没有下一步可做了，且没有等待，即做完了：
  - requestNext()
VM44:32 [ws] recv (raw): all dishes done
VM44:25 [ws] recv (json): {type: 'NO_STEP', sessionId: 'acbaa801-fbae-668f-5f56-a2de956d52d9'}


