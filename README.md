# websocket通讯实现：
## 主要文件：
- com.example.cooking.handler.WsHandler
- com.example.cooking.service.impl.CookingServiceImpl

## 测试流程：
方式：浏览器
示例代码与示例流程：ws_test/test.js

## 接口简要示例：

### 统一json格式：
{type, message, data}

### 连接WS
message返回sid
- connectWs()
    - {type: 'CONNECT', message: '4e4eb993-ab79-c374-409e-6d5fd9a2b786', data: null}

### 创建菜单：
message返回sid， 

data返回这两个菜的主要信息(TODO)
- createSession(["简易红烧肉","白灼虾"])
    - {type: 'CREATE_SESSION', message: '4e4eb993-ab79-c374-409e-6d5fd9a2b786', data: null}

### 拉取下一步：
- 正常情况：data 返回菜当前步信息
    - requestNext()
      - {type: 'REQUEST_NEXT', message: null, data: {…}}

- 如果当前步骤可阻塞，需要先发送确认阻塞开始： message返回提示，data 返回菜当前步信息
    - requestNext()
      - {type: 'NO_NEXT_STEP', message: 'Current step is blockable but not started, need call START_BLOCKABLE !', data: {…}}
- 发送确认阻塞开始： message返回sid
  - startBlockable()
    - {type: 'START_BLOCKABLE', message: '4e4eb993-ab79-c374-409e-6d5fd9a2b786', data: null}
- 如果上一步发送了阻塞开始，下一次拉取下一步是下一个菜： data 返回下一个菜当前步信息
  - requestNext()
    - {type: 'REQUEST_NEXT', message: null, data: {… (下个菜的信息)}}
- 阻塞时间到，服务端主动发信息，并回到前面的步骤继续： message返回提示，data 返回菜当前步信息
  - 前端不用发信息
    - {type: ' BLOCK_FINISHED', message: 'Block finishedd ! Please go to deal the prev dish', data: {…}}
- 如果当前没有下一步可做了，但还有任务在等待，返回等待任务信息：message返回提示，data 等待中的菜当前步信息
  - requestNext()
    - {type: 'NO_NEXT_STEP', message: 'dish waiting ... 23seconds left !', data: {…}}
- 如果当前没有下一步可做了，且没有等待，即做完了： message返回提示
  - requestNext()
    - {type: 'NO_NEXT_STEP', message: 'All dishes done !', data: null}

