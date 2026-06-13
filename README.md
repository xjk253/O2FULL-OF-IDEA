# O2FULL-OF-IDEA

让灵感不再落空 — AI 宠物伴侣「泡泡」

## 项目结构

```
├── agent/          AI 大脑 (Node.js + TypeScript)
│   ├── character/  角色配置 + emoMap
│   ├── petstate/   情绪状态机
│   ├── prompts/    动态 System Prompt 工厂
│   ├── postprocess/ 四级管道 (分句/表情/显示/TTS)
│   ├── tools/      工具定义 (灵感捕获/搜索/Bash)
│   ├── memory/     会话记忆 + 温记忆
│   ├── triggers/   主动触发器
│   ├── preprocess/ 6 步编排器
│   └── loop/       SDK 调用 + prompt mode tool calling
├── gateway/        WebSocket 服务器 (端口 8080)
├── phone/          Android 客户端 (Kotlin + Compose)
```

## 快速开始

### Agent 开发

```bash
cd agent
npm install
npm test          # 运行测试
npm run dev       # 开发模式
```

### Gateway

```bash
cd gateway
npm install
DEEPSEEK_API_KEY=your-key npm run dev
```

### 手机连接

```bash
adb reverse tcp:8080 tcp:8080
```

## 架构

```
用户消息 → phone → WebSocket → gateway → agent
                                              ↓
                                        preprocess (状态更新/记忆检索/Prompt构建)
                                              ↓
                                        loop (LLM推理 + Tool调用)
                                              ↓
                                        postprocess (分句/表情提取/显示/TTS净化)
                                              ↓
                                        ← sentence/done → phone
```

## 测试

```bash
cd agent && npm test
# 35 tests passing
```
