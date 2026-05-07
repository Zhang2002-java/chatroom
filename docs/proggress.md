# 聊天应用项目 — 当前进度

> 最后更新：2026-05-07

## 项目概况

Spring Boot 3.2.5 + Vue 3.4 + MySQL 8.0 网上聊天应用，求职作品项目。

## 已完成的功能

### P0 - 核心基础
- [x] 项目脚手架搭建（Maven + Vite + MySQL）
- [x] 用户注册 / 登录（JWT + BCrypt）
- [x] Spring Security + CORS 配置
- [x] 好友搜索、添加、同意/拒绝、删除、拉黑
- [x] 私聊（文本消息，WebSocket 实时推送）
- [x] 消息持久化（MySQL）

### P1 - 增强功能
- [x] 消息状态（已发送 / 已送达 / 已读）
- [x] WebSocket 实时消息 + 通知音（Web Audio API）
- [x] 消息撤回（2 分钟内，仅发送者）
- [x] 文件上传（图片、文件，UUID 重命名本地存储）
- [x] 群聊（创建群、加成员、移除成员）
- [x] 消息搜索

### P2 - 前端体验
- [x] 登录 / 注册页面
- [x] 好友列表页面（含申请管理）
- [x] 聊天面板（消息列表 + 输入框）
- [x] 对话列表侧边栏（好友 + 群组，搜索过滤）
- [x] 导航侧边栏（未读徽标）
- [x] Emoji 选择器
- [x] 个人资料编辑

### P3 - 生产化
- [x] 生产环境配置（application-prod.yml）
- [x] 全局异常处理
- [x] 前端打包部署配置（nginx.conf + vite build）
- [x] Docker 化（Dockerfile + docker-compose.yml）

## 已修复的 Bug

1. **好友申请不通知对方** — 在 FriendServiceImpl.sendRequest() 中添加 WebSocket 推送
2. **好友同意后列表不刷新** — 添加 FRIEND_ACCEPTED 消息类型，前端实时监听刷新
3. **重复添加好友** — 修复双向校验，检查所有状态（pending/accepted/blocked/rejected）
4. **已读不回馈** — 添加 READ_CONVERSATION WebSocket 消息类型，发送方实时更新消息状态
5. **删除好友按钮不工作** — 改用内联按钮 + @click.stop 防止事件冒泡
6. **消息顺序错乱** — 添加 MybatisPlusConfig 分页拦截器，区分私聊/群聊查询逻辑
7. **聊天头像显示数字 ID** — 打开对话时主动请求用户信息填充 contacts
8. **缺少对话列表** — 新建 ConversationList.vue 组件，集成到 ChatPanel 左侧

## 项目结构

```
E:\Claude工作目录\
├── docker-compose.yml           # 一键部署编排
├── backend/                     # Spring Boot 后端
│   ├── Dockerfile               # 后端容器构建
│   ├── pom.xml
│   └── src/main/java/com/chatroom/
│       ├── ChatroomApplication.java
│       ├── config/              # Security, WebSocket, Upload, MybatisPlus 配置
│       ├── security/            # JWT 令牌 + 过滤器
│       ├── entity/              # 6 个实体类
│       ├── mapper/              # 6 个 MyBatis-Plus Mapper
│       ├── service/impl/        # Auth, Message, Friend, User, Group, File 实现
│       ├── controller/          # 6 个 REST 控制器
│       ├── websocket/           # ChatWebSocketHandler
│       ├── common/              # Result, ApiException, GlobalExceptionHandler
│       └── dto/                 # RegisterRequest, LoginRequest, LoginResponse
├── frontend/                    # Vue 3 前端
│   ├── Dockerfile               # 前端容器构建（Node + Nginx）
│   ├── nginx.conf               # Nginx 部署配置
│   ├── src/
│   │   ├── views/              # Login, Register, Home, ChatPanel, FriendList, GroupList, Search, Profile
│   │   ├── components/         # NavSidebar, ConversationList, MessageList, MessageItem, MessageInput, AddFriendDialog
│   │   ├── stores/             # user, chat (Pinia)
│   │   ├── api/                # Axios 封装 + 各模块 API
│   │   └── router/             # 路由配置 + 导航守卫
│   └── vite.config.ts
└── docs/
    ├── proggress.md             # 项目进度文档
    ├── superpowers/specs/       # 设计规范文档
    └── superpowers/plans/       # 实现计划文档
```

## 如何运行

### 后端（IntelliJ IDEA）
1. 用 IDEA 打开 `backend/` 目录
2. 确保 MySQL 已启动，执行 `backend/src/main/resources/db/init.sql` 建表
3. 修改 `application.yml` 中数据库密码
4. 运行 `ChatroomApplication.java`

### 前端（VS Code 或命令行）
```bash
cd frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`

## 下次继续时需要了解的事项

1. **全部功能已完成** — P0-P3 所有功能均已实现
2. **代码未提交 Git** — 如果要做版本管理，需要先 `git init`
3. **Docker 部署** — 项目根目录 `docker-compose up` 一键启动（MySQL + 后端 + 前端）
4. **裸机部署** — 手动部署时先启动 MySQL → 后端 `mvn spring-boot:run -Dspring-boot.run.profiles=prod` → Nginx 配置见 `frontend/nginx.conf`
5. **重启注意** — 修改后端 Java 代码需要重启应用，前端 Vite 会自动热重载
