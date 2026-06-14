# RAG 长期记忆系统设计

> 日期: 2026-06-14
> 状态: 已批准,待实现
> 分支: x (备份点 2df033c)

## 背景

当前系统有三层记忆:

1. **L1 会话内存** (`agent/memory/l1.ts`): Map<sessionId, Message[]>,保存最近对话。已通过持久化 sessionId 实现跨连接保留。
2. **Warm memory** (`agent/memory/warm.ts`): MEMORY.md + USER.md,人工策展的简短事实,2200 字符上限,始终注入 prompt。
3. **缺失**: 跨会话的长期记忆。会话历史随 L1 堆积会撑爆 context window,且无法按相关性检索。

## 目标

- 会话结束时自动总结,保存为可检索的长期记忆
- 用户新消息到达时,按相关性检索历史记忆,注入 prompt
- Phone 端展开聊天只显示当前会话(自启动宠物起),不再加载本地历史档案
- 全部记录由服务器负责保存

## 非目标

- 向量数据库 / embedding(本期用关键词匹配,够用即可)
- 跨设备记忆同步(单机 gateway 即可)
- 用户可编辑记忆(后续可加)

## 架构

```
┌─────────────┐         ┌──────────────────────────────┐
│   phone     │  chat   │          gateway             │
│  当前会话   │ ──────► │  preprocess                  │
│  (内存)     │ ◄────── │   ├─ L1 memory (近 10 条)    │
│             │  reply  │   ├─ warm memory (策展事实)  │
│  不加载     │         │   └─ RAG retrieval ◄─────────┐│
│  本地档案   │         │                              ││
└─────────────┘         │  ws close 触发:             ││
                        │   summarizer(messages) ─────┐││
                        └──────────────────────────────┘││
                                                          ││
                ┌─────────────────────────────────────────┘│
                │                                           │
                │  ┌────────────────────────────────────────▼
                │  │ agent/memory/rag/data/*.json
                │  │ (每会话总结一文件,JSON 格式)
                │  └────────────────────────────────────────▲
                │                                           │
                └───────────────────────────────────────────┘
                   (每条 chat 前 retriever.search)
```

## 组件设计

### 1. Phone 侧 — 当前会话内存

**新文件: `SessionMessages.java`**

```java
public class SessionMessages {
    private static final List<ChatMessage> messages = new ArrayList<>();
    public static List<ChatMessage> get() { return messages; }
    public static void add(ChatMessage m) { messages.add(m); }
    public static void clear() { messages.clear(); }
}
```

**改动:**
- `OverlayPetService.onCreate()`: 调 `SessionMessages.clear()` 重置会话
- `OverlayPetService` sentence listener: 调 `SessionMessages.add()` 替代 `messageStore.append()`
- `OverlayPetService.onSendMessage()`: 调 `SessionMessages.add()` 替代 `messageStore.append()`
- `ChatActivity.onCreate()`: 从 `SessionMessages.get()` 加载,移除 `messageStore.load()` 调用
- `MessageStore`: 保留代码不删除,但停止使用(留作未来备用)

**边界:**
- 启动宠物 = 新会话(内存清空)
- 切换悬浮窗/全屏 = 同一会话(内存共享)
- 关闭宠物 + 重启 = 新会话

### 2. 新建 `agent/memory/rag/` 模块

**`types.ts`**
```typescript
export interface RagEntry {
  sessionId: string;
  startTime: string;    // ISO 8601
  endTime: string;      // ISO 8601
  summary: string;       // LLM 生成的会话总结
  keywords: string[];    // LLM 提取的关键词
  messageCount: number;
}
```

**`store.ts`** — 文件 I/O
- `DATA_DIR = resolve(__dirname, "data")`
- `list(): Promise<RagEntry[]>` — 扫描 data/*.json,按时间倒序
- `save(entry: RagEntry): Promise<void>` — 写 `data/{sessionId}-{endTime}.json`
- 启动时自动 `mkdirSync(DATA_DIR, { recursive: true })`

**`summarizer.ts`** — 调 LLM 生成总结
- `summarize(sessionId, messages: Message[]): Promise<RagEntry | null>`
- 不走 `runLoop`(那是带工具的流式聊天循环),直接用 `@anthropic-ai/sdk` 的
  `client.messages.create` 做一次性非流式调用(参考 loop.ts 的 client 初始化方式)
- 构造 system prompt:"请总结以下对话的关键信息,提取 5-10 个关键词。
  只输出 JSON:{summary: string, keywords: string[]}"
- `JSON.parse` 解析返回值,失败时尝试从响应文本中提取 `{...}` 子串
- 生成 RagEntry(含 sessionId/起止时间/messageCount)
- 失败时记日志返回 null(不抛异常,不阻塞 ws.close)

**`retriever.ts`** — 关键词检索
- `retrieve(query: string, topK = 3): Promise<string>`
- 流程:
  1. 中文分词(简单按标点/空格切分,过滤停用词)
  2. 扫描所有 RagEntry,对每条计算 `keywords` + `summary` 与查询词的命中数
  3. 按命中数降序,取 topK
  4. 拼接为:"## 相关历史记忆\n- [时间] summary\n- ..."
- 无匹配时返回空字符串

### 3. Gateway 集成 (`server.ts`)

**新增依赖:**
- `import { summarize } from "../agent/memory/rag/summarizer"`
- `import { retrieve as ragRetrieve } from "../agent/memory/rag/retriever"`
- `import { save as ragSave } from "../agent/memory/rag/store"`

**ws.on("close") 处理:**
```typescript
ws.on("close", async () => {
  console.log(`[gateway] 断开: ${sessionId}`);
  // 取未总结的消息
  const allMessages = getRecent(sessionId, 9999);
  const unsummarized = allMessages.slice(summarizedCountMap.get(sessionId) ?? 0);
  if (unsummarized.length >= 2) {
    const entry = await summarize(sessionId, unsummarized);
    if (entry) {
      await ragSave(entry);
      summarizedCountMap.set(sessionId, allMessages.length);
      console.log(`[rag] 已保存会话总结: ${entry.summary.slice(0, 50)}...`);
    }
  }
});
```

**handleChat() 修改:**
```typescript
// 在 preprocess 前检索 RAG
const ragContext = await ragRetrieve(text);
// 传给 preprocess 作为新依赖项
```

**preprocess.ts 修改:**
- `PreProcessDeps` 加 `ragMemory: { retrieve(query: string): Promise<string> } | null`
- Step 5 修改:`const ragContext = deps.ragMemory ? await deps.ragMemory.retrieve(input.text) : "";`
- 把 ragContext 拼到 systemPrompt(类似 warmMemory)

### 4. 边界情况

| 情况 | 处理 |
|---|---|
| 总结时 LLM 报错 | 记日志,返回 null,不阻塞 ws.close |
| RAG 目录不存在 | store.ts 启动时 mkdirSync |
| 用户消息为空 | 跳过检索 |
| 无匹配记忆 | 返回空字符串,不注入 |
| 总结后新增 0 条消息 | 跳过总结(避免空总结) |
| 总结后新增仅 1 条 | 跳过(单条不值得总结) |
| 关键词含停用词 | 过滤掉 |

## 测试计划

- `summarizer.test.ts`: mock LLM,验证 JSON 解析、失败处理
- `retriever.test.ts`: 准备 3 条 RagEntry,验证关键词命中排序
- `store.test.ts`: 临时目录,验证读写、目录创建
- phone `SessionMessages`: 单元测试 clear/add/get
- 端到端:聊两轮 → 断开 → 检查 data/ 下有 JSON → 重连 → 问相关问题 → 验证 AI 引用了历史

## 文件清单

**新增:**
- `phone/.../SessionMessages.java`
- `agent/memory/rag/types.ts`
- `agent/memory/rag/store.ts`
- `agent/memory/rag/summarizer.ts`
- `agent/memory/rag/retriever.ts`
- `agent/memory/rag/data/.gitkeep` (空目录占位)

**修改:**
- `phone/.../OverlayPetService.java` — 用 SessionMessages 替代 MessageStore
- `phone/.../ChatActivity.java` — 从 SessionMessages 加载
- `gateway/server.ts` — 集成 summarizer (ws close) + retriever (chat)
- `agent/preprocess/preprocess.ts` — 加 ragMemory 依赖
- `agent/preprocess/types.ts` — 加 ragMemory 类型

## 不变

- L1 memory (`l1.ts`) 不变
- Warm memory (`warm.ts`) 不变
- postprocess 不变
- MessageStore.java 代码保留(停止使用)
