# Agent 架构设计（V1）

## 目标

实现一个极简 Agent 框架：

- 前端聊天界面
- SSE 实时返显
- OpenAI Tool Calling
- Planner（慢思考）
- Executor（执行循环）
- Docker Sandbox
- AI 自主查阅系统文档
- AI 自主维护长期记忆

## 整体架构

```text
Browser
  |
  v
Ktor SSE Endpoint
  |
  v
Planner Loop
  |
  v
Executor Loop
  |
  |-- Business Tools
  |
  `-- Docker Sandbox
        |
        |-- rg
        |-- cat
        |-- ls
        `-- memory/docs
```

## 前端流程

用户输入：

```text
我中午吃了费大厨辣椒炒肉和一碗米饭
```

发送：

```http
POST /chat/stream
```

请求：

```json
{
  "conversationId": "xxx",
  "message": "我中午吃了费大厨辣椒炒肉和一碗米饭"
}
```

## SSE 连接

后端返回：

```http
Content-Type: text/event-stream
Cache-Control: no-cache
```

保持连接。

## SSE 消息格式

状态：

```text
data: {"type":"status","text":"正在分析请求"}
```

文本增量：

```text
data: {"type":"delta","text":"好的"}
```

工具执行：

```text
data: {"type":"tool","text":"正在查询食物"}
```

完成：

```text
data: {"type":"done"}
```

## Planner 阶段（慢思考）

目标：

分析任务并制定计划。

允许使用：

```text
Docker Sandbox
```

允许执行：

```text
rg
cat
ls
```

Planner 可以：

```text
查系统文档
查业务规则
查长期记忆
生成执行计划
```

Planner 输出：

```json
{
  "intent": "create_diet_record",
  "summary": "识别食物并记录热量"
}
```

## Executor 阶段

Executor 负责：

```text
生成用户回复
调用业务工具
流式输出
```

核心循环：

```kotlin
while (true) {
    callAI()
    streamDelta()

    if (noToolCall) {
        break
    }

    executeTool()
    appendToolResult()
}
```

## Tool Calling 流程

```text
AI
  |
  v
findFoodItem
  |
  v
Tool Result
  |
  v
AI
  |
  v
createDietRecord
  |
  v
Tool Result
  |
  v
AI
  |
  v
Done
```

所有输出都通过当前 SSE 连接返回前端。

## Docker Sandbox

容器名称：

```text
ai-sandbox
```

目录结构：

```text
/workspace
  |-- docs
  |-- memory
  `-- tmp
```

## Docs

挂载：

```bash
-v /srv/docs:/workspace/docs:ro
```

特点：

```text
只读
存放系统说明文档
业务规则
能力文档
```

## Memory

挂载：

```bash
-v /srv/memory:/workspace/memory
```

特点：

```text
可读写
长期记忆
摘要
知识记录
```

## 网络

禁止联网：

```bash
--network none
```

## 资源限制

```bash
--cpus=1 --memory=512m
```

## Sandbox 工具

允许：

```bash
rg
cat
ls
```

后续可增加：

```bash
find
head
tail
jq
```

## Sandbox 调用

后端执行：

```bash
docker exec ai-sandbox rg "饮食" /workspace/docs
```

读取文件：

```bash
docker exec ai-sandbox cat /workspace/docs/diet.md
```

列目录：

```bash
docker exec ai-sandbox ls /workspace/docs
```

## OpenAI 调用结构

一次用户消息：

```text
User Message
  |
  v
Planner
  |
  v
Executor Loop
  |
  v
AI
  |
  v
Tool
  |
  v
AI
  |
  v
Tool
  |
  v
AI
  |
  v
Done
```

## Conversation

前端传：

```json
{
  "conversationId": "xxx"
}
```

后端负责：

```text
查历史消息
构建上下文
存储消息
维护会话
```

## 第一版核心能力

必须实现：

```text
SSE
Planner
Executor Loop
Tool Calling
Docker Sandbox
Conversation
```

暂不实现：

```text
向量库
RAG
复杂工作流框架
多 Agent
```

目标：

先实现一个稳定、可扩展、极简的 Agent 核心。
