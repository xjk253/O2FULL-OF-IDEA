# BubblePetApp - 项目规范

## 项目概述
Android 悬浮宠物应用，包含可拖拽气泡宠物、聊天界面，后期接入 FastAPI AI 后端。

## 技术栈
- Language: Java 11
- Min SDK: 24 (Android 7.0)
- Target SDK: 36
- Build: Gradle Kotlin DSL
- UI: 传统 View 系统（非 Compose）
- 网络: HttpURLConnection（后期可换 Retrofit）

## 项目结构
```
app/src/main/java/com/example/bubblepet/
  MainActivity.java           # 首页，权限申请，启动服务
  OverlayPetService.java      # 悬浮窗前台服务
  AiChatClient.java           # AI 聊天 HTTP 客户端
app/src/main/res/layout/
  activity_main.xml           # 首页布局
app/src/main/AndroidManifest.xml
```

## 代码规范
- 不使用 Compose，全部用传统 View/XML
- 中文注释
- 不使用第三方网络库（保持轻量）
- 权限检查必须完整（悬浮窗、前台服务）
- 服务必须用前台通知

## 测试
- 用真机测试（虚拟机不支持模拟器）
- 设备：Xiaomi

## 后端对接
- FastAPI 地址：待定
- 接口格式：待定
- AiChatClient 预留可替换接口
