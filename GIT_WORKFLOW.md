# Git 分支协议

## 分支结构

```
main          稳定版本，可直接部署
  └── dev     开发主线，日常提交在这里
       └── feature/xxx   具体功能开发
```

## 分支规则

### main — 稳定版本
- 只接受从 `dev` 合入
- 合入前必须测试通过、功能完整
- 合并方式：`git merge dev`（由项目负责人操作）
- **禁止直接在 main 上开发**

### dev — 开发主线
- 日常开发的基础分支
- 从 main 创建，保持同步
- feature 开发完且无冲突后合入
- 提交前确保 `npm test` 通过

### feature/xxx — 功能分支
- 命名规范：
  - `feature/voice-chat` — 语音对话
  - `feature/web-capture` — 网页内容捕获
  - `feature/new-tools` — 新工具集
  - `fix/reconnect-bug` — Bug 修复
- 从 `dev` 创建，开发完后合并回 `dev`

## 工作流程

### 开发新功能

```bash
# 1. 从 dev 创建 feature 分支
git checkout dev
git pull origin dev
git checkout -b feature/xxx

# 2. 开发 + 提交
git add ...
git commit -m "feat(xxx): 具体改动描述"

# 3. 开发完成，切回 dev 合并
git checkout dev
git pull origin dev
git merge feature/xxx

# 4. 检查无冲突，推送 dev
git push origin dev

# 5. 删除 feature 分支（可选）
git branch -d feature/xxx
```

### 发版到 main

```bash
# dev 测试通过后
git checkout main
git pull origin main
git merge dev
git push origin main
```

## Commit 消息规范

```
feat(模块): 简短描述       — 新功能
fix(模块): 简短描述        — Bug 修复
refactor(模块): 简短描述   — 重构
docs: 简短描述             — 文档
chore: 简短描述            — 构建/配置
```

模块示例：`agent`, `gateway`, `phone`, `tools`, `prompts`

## 冲突处理

合并 feature 到 dev 时如果冲突：
1. 优先保留双方的有效改动
2. 不确定时联系对方确认
3. 合并后跑一遍测试确保没坏
