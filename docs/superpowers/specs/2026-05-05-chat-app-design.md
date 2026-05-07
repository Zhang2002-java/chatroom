# 网上聊天应用设计文档

**日期:** 2026-05-05
**定位:** 求职作品
**技术栈:** Spring Boot 3.2 + Vue 3.4 + MySQL 8.0

---

## 一、需求概述

群聊+私聊的实时通讯 Web 应用。用户注册登录后搜索添加好友，进行一对一私聊或群组聊天。支持文本、图片、文件、表情四种消息类型，消息持久保存。

### 功能清单

| 模块 | 功能 |
|------|------|
| 认证 | 用户名+密码注册、登录、JWT 鉴权 |
| 用户 | 头像、昵称、个性签名，搜索用户 |
| 好友 | 搜索添加、好友申请与验证、好友列表、拉黑 |
| 私聊 | 一对一实时聊天，消息状态（已发送/已送达/已读），消息撤回 |
| 群聊 | 创建群、拉人、发言、退群 |
| 消息 | 文本/图片/文件/表情，历史记录分页加载，消息搜索，撤回 |
| 部署 | 前后端分离部署（Nginx 静态资源 + Spring Boot 服务） |

---

## 二、架构设计

### 整体架构

标准分层单体架构（Controller → Service → Repository）。

- **前端:** Vue 3 SPA，Nginx 提供静态资源，端口 80/443
- **后端:** Spring Boot REST API + WebSocket，端口 8080
- **数据库:** MySQL 8.0
- **文件存储:** 本地磁盘
- **认证:** JWT 令牌，HTTP Header 传递，WebSocket 连接时传递 token 验证

### 技术选型

| 层 | 技术 | 选型理由 |
|------|------|---------|
| 后端框架 | Spring Boot 3.2 | Java 生态标准，求职市场认可 |
| 安全 | Spring Security + JWT | 无状态认证，天然支持前后端分离 |
| 实时通信 | Spring WebSocket | Spring Boot 原生支持，原始 WebSocket + JSON 协议 |
| ORM | MyBatis-Plus 3.x | SQL 可控，学习价值高，国内主流 |
| 数据库 | MySQL 8.0 | Java 生态事实标准 |
| 构建 | Maven | Spring Boot 标配 |
| 前端框架 | Vue 3.4 + TypeScript | Composition API，类型安全 |
| UI 组件 | Element Plus | 国内 Vue 3 最主流，中文文档友好 |
| 状态管理 | Pinia | Vue 3 官方推荐，替代 Vuex |
| HTTP 客户端 | Axios | 拦截器机制方便统一处理 JWT |
| 构建 | Vite | 替代 Webpack，开发体验好 |

---

## 三、数据库设计

### 表结构

**user** — 用户表
| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) UNIQUE | 用户名 |
| password | VARCHAR(255) | BCrypt 加密后的密码 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像文件路径 |
| signature | VARCHAR(200) | 个性签名 |
| created_at | DATETIME | 注册时间 |

**friend_relation** — 好友关系表
| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| user_id | BIGINT | 发起方用户 ID |
| friend_id | BIGINT | 目标用户 ID |
| status | TINYINT | 0=待验证 1=已通过 2=已拉黑 |
| created_at | DATETIME | 创建时间 |

**message** — 消息表
| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| sender_id | BIGINT | 发送者 ID |
| receiver_id | BIGINT | 接收者 ID（用户或群组） |
| chat_type | VARCHAR(10) | 'private' 或 'group' |
| content_type | VARCHAR(10) | 'text'/'image'/'file'/'emoji' |
| content | TEXT | 消息正文或文件路径 |
| status | TINYINT | 1=已发送 2=已送达 3=已读 |
| is_recalled | TINYINT DEFAULT 0 | 0=正常 1=已撤回 |
| created_at | DATETIME | 发送时间 |

**group_info** — 群组表
| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| name | VARCHAR(50) | 群名称 |
| avatar | VARCHAR(255) | 群头像路径 |
| owner_id | BIGINT | 群主用户 ID |
| created_at | DATETIME | 创建时间 |

**group_member** — 群成员表
| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| group_id | BIGINT | 群组 ID |
| user_id | BIGINT | 用户 ID |
| role | VARCHAR(10) | 'owner' 或 'member' |
| joined_at | DATETIME | 加入时间 |

**file_record** — 文件记录表
| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| uploader_id | BIGINT | 上传者 ID |
| file_name | VARCHAR(255) | 原始文件名 |
| file_path | VARCHAR(500) | 存储路径 |
| file_size | BIGINT | 文件大小（字节） |
| file_type | VARCHAR(50) | MIME 类型 |
| created_at | DATETIME | 上传时间 |

### 索引设计

| 表 | 索引 | 用途 |
|------|------|------|
| message | `(receiver_id, chat_type, created_at)` | 加载聊天记录 |
| message | `(sender_id, created_at)` | 搜索"我发的消息" |
| friend_relation | `(user_id, status)` | 查询好友列表 |
| friend_relation | `(user_id, friend_id)` UNIQUE | 防止重复添加 |
| user | `username` UNIQUE | 搜索用户 |
| group_member | `(user_id)` | 查某用户加入的群 |

---

## 四、API 设计

### 规范

- URL 前缀: `/api/v1`
- 统一响应格式: `{ "code": 200, "message": "success", "data": {} }`
- 认证方式: `Authorization: Bearer <jwt_token>`
- REST 规范: 资源用名词复数，HTTP 方法表达动作

### 接口清单

**认证**
```
POST   /api/v1/auth/register        注册
POST   /api/v1/auth/login           登录
```

**用户**
```
GET    /api/v1/users/search         搜索用户 (?keyword=)
GET    /api/v1/users/{id}           查看用户资料
PUT    /api/v1/users/me             修改个人信息
PUT    /api/v1/users/me/avatar      上传头像
```

**好友**
```
POST   /api/v1/friends/request      发送好友申请
PUT    /api/v1/friends/{id}/accept  同意申请
PUT    /api/v1/friends/{id}/reject  拒绝申请
DELETE /api/v1/friends/{id}         删除好友
GET    /api/v1/friends              好友列表
PUT    /api/v1/friends/{id}/block   拉黑好友
```

**消息**
```
GET    /api/v1/messages             聊天记录 (?targetId=&chatType=&page=&size=)
POST   /api/v1/messages/search      消息搜索 (?keyword=)
PUT    /api/v1/messages/{id}/recall 撤回消息（限 2 分钟内）
POST   /api/v1/upload               上传文件/图片
```

**群组**
```
POST   /api/v1/groups               创建群组
GET    /api/v1/groups               我的群组列表
GET    /api/v1/groups/{id}/members  群成员列表
POST   /api/v1/groups/{id}/members  拉人进群
DELETE /api/v1/groups/{id}/members/{userId}  踢人/退群
```

---

## 五、WebSocket 设计

### 消息协议

统一 JSON 格式：
```json
{
  "type": "CHAT",
  "senderId": 1,
  "receiverId": 2,
  "chatType": "private",
  "contentType": "text",
  "content": "你好",
  "timestamp": 1714809600000
}
```

### 消息类型

| type | 含义 |
|------|------|
| CHAT | 聊天消息 |
| RECALL | 消息撤回通知 |
| STATUS | 消息状态变更（已送达/已读） |
| HEARTBEAT | 心跳检测 |

### 消息状态流转

发送中 → 已发送 → 已送达 → 已读

- **已发送:** 服务端成功存储
- **已送达:** 通过 WebSocket 推送到接收方客户端
- **已读:** 接收方打开聊天窗口后确认

### 心跳机制

- 客户端每 30 秒发送 HEARTBEAT
- 服务端 60 秒内未收到任何消息则断开连接

---

## 六、前端设计

### 路由结构

```
/login             登录页
/register          注册页
/home              主页面布局（含左侧导航）
  /home/chat             聊天面板（默认）
  /home/chat/:id         与某用户/群的聊天
  /home/friends          好友列表 + 申请管理
  /home/groups           群组列表
  /home/profile          个人资料
  /home/search           用户搜索 + 消息搜索
```

### 组件树

```
App.vue
├─ LoginView.vue
├─ RegisterView.vue
└─ HomeView.vue              (WebSocket 连接管理)
    ├─ NavSidebar.vue        (导航 + 未读消息数)
    ├─ ChatPanel.vue         (当前会话)
    │   ├─ ChatHeader.vue
    │   ├─ MessageList.vue   (消息渲染 + 自动滚底)
    │   │   └─ MessageItem.vue  (单条消息气泡)
    │   └─ MessageInput.vue  (输入框 + 表情 + 上传)
    │       ├─ EmojiPicker.vue
    │       └─ FileUpload.vue
    ├─ FriendList.vue
    ├─ GroupList.vue
    ├─ SearchView.vue
    ├─ ProfileView.vue
    └─ AddFriendDialog.vue
```

### 状态管理 (Pinia)

| Store | 内容 |
|-------|------|
| userStore | 当前登录用户信息、JWT token |
| chatStore | WebSocket 连接实例、未读消息计数 |

组件本地状态自行管理，仅跨组件共享状态放入 Pinia。

---

## 七、后端包结构

```
src/main/java/com/chatroom/
├── ChatApplication.java
├── config/
│   ├── WebSocketConfig.java       (WebSocket 端点配置)
│   ├── SecurityConfig.java        (Spring Security + JWT 过滤器)
│   └── UploadConfig.java          (文件上传路径配置)
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── FriendController.java
│   ├── MessageController.java
│   ├── GroupController.java
│   └── UploadController.java
├── service/
│   ├── impl/                      (Service 实现类)
│   ├── AuthService.java
│   ├── UserService.java
│   ├── FriendService.java
│   ├── MessageService.java
│   ├── GroupService.java
│   └── FileService.java
├── mapper/                        (MyBatis-Plus Mapper 接口)
├── entity/
│   ├── User.java
│   ├── FriendRelation.java
│   ├── Message.java
│   ├── GroupInfo.java
│   ├── GroupMember.java
│   └── FileRecord.java
├── dto/                           (Data Transfer Object)
├── websocket/
│   └── ChatWebSocketHandler.java  (WebSocket 消息分发)
├── security/
│   ├── JwtTokenProvider.java
│   └── JwtAuthenticationFilter.java
└── common/
    ├── Result.java                (统一响应体)
    └── ApiException.java          (统一异常)
```

---

## 八、部署方案

### 开发环境

| 组件 | 地址 | 说明 |
|------|------|------|
| 前端 | localhost:5173 | Vite 开发服务器，自动代理 `/api` 到后端 |
| 后端 | localhost:8080 | Spring Boot DevTools 热重载 |
| 数据库 | localhost:3306 | MySQL 本地实例 |

### 生产部署

```
Nginx (80/443)
├── /                   → 前端静态文件 (dist/)
├── /api                → proxy_pass localhost:8080
└── /ws                 → WebSocket 代理
Spring Boot (8080)
├── chatroom.jar        → 后端服务
└── /data/uploads/      → 文件存储目录
MySQL (3306)
└── chatroom_db         → 数据库
```

前端 `npm run build` 生成纯静态文件，Nginx 直接提供。后端 `mvn package -DskipTests` 打包成 jar，`java -jar` 运行。

---

## 九、实现优先级

| 优先级 | 功能 | 说明 |
|--------|------|------|
| P0 | 项目搭建、数据库建表 | 基础设施，最先做 |
| P0 | 注册登录、JWT 认证 | 没有认证其他都做不了 |
| P0 | WebSocket 连接 + 文本私聊 | 核心功能，做完了项目就"活"了 |
| P1 | 好友搜索、添加、列表 | 私聊的前置条件 |
| P1 | 图片/文件/表情消息 | 丰富消息类型 |
| P1 | 消息状态（已送达/已读） | 提升体验 |
| P2 | 群聊功能 | 私聊稳定后再做 |
| P2 | 消息搜索 | 非实时核心 |
| P2 | 消息撤回 | 辅助功能 |
| P2 | 拉黑处理 | 辅助功能 |
| P3 | 个人资料编辑 | 锦上添花 |
| P3 | 前后端打包部署 | 收官阶段 |
