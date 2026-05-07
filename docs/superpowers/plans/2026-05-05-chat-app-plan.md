# Chat App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a real-time chat application with Spring Boot backend + Vue 3 frontend supporting private/group chat, text/image/file/emoji messages, friend management, and message recall.

**Architecture:** Standard layered monolith (Controller → Service → Repository). Backend serves REST API + WebSocket on port 8080. Frontend is a Vue 3 SPA with Element Plus served by Nginx in production. JWT-based stateless authentication. MySQL 8.0 for persistence with MyBatis-Plus ORM.

**Tech Stack:** Spring Boot 3.2, Spring Security, Spring WebSocket, MyBatis-Plus 3.x, MySQL 8.0, JJWT, Vue 3.4, TypeScript, Element Plus, Pinia, Vue Router, Axios, Vite

**Project Structure:**
```
E:\Claude工作目录\
├── backend/                          # Spring Boot project
│   └── src/main/java/com/chatroom/
│       ├── ChatApplication.java
│       ├── config/                   # SecurityConfig, WebSocketConfig, UploadConfig
│       ├── controller/               # REST controllers
│       ├── service/ + service/impl/  # Business logic
│       ├── mapper/                   # MyBatis-Plus mappers
│       ├── entity/                   # Database entities
│       ├── dto/                      # Request/response DTOs
│       ├── websocket/                # WebSocket handler
│       ├── security/                 # JWT provider + filter
│       └── common/                   # Result, ApiException
├── frontend/                         # Vue 3 + Vite project
│   └── src/
│       ├── router/index.ts
│       ├── stores/                   # Pinia stores (user, chat)
│       ├── views/                    # Page components
│       ├── components/               # Reusable components
│       ├── api/                      # Axios HTTP client wrappers
│       └── utils/                    # WebSocket client, helpers
└── docs/superpowers/                 # Specs and plans
```

---

## PHASE 0: Foundation (P0)

### Task 1: Initialize Maven Project with Dependencies

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/chatroom/ChatApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Create the root pom.xml with all dependencies**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
    </parent>

    <groupId>com.chatroom</groupId>
    <artifactId>chatroom-backend</artifactId>
    <version>1.0.0</version>
    <name>ChatRoom</name>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatroom_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: YXF1aWRzZmhhaWRzaGZvaXV5ZG9zZGl1Znlvc2F1aWR5ZnNkb2lmMTAyNDc5MjM0NTk4NzIzNA==
  expiration: 86400000

upload:
  path: ./uploads
```

- [ ] **Step 3: Create main application class**

```java
package com.chatroom;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.chatroom.mapper")
public class ChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
```

- [ ] **Step 4: Verify project compiles**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

---

### Task 2: Database Schema — MySQL Init Script

**Files:**
- Create: `backend/src/main/resources/db/init.sql`

- [ ] **Step 1: Create init.sql with all 6 tables**

```sql
CREATE DATABASE IF NOT EXISTS chatroom_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE chatroom_db;

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) DEFAULT '',
    avatar VARCHAR(255) DEFAULT '',
    signature VARCHAR(200) DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS friend_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=pending, 1=accepted, 2=blocked, 3=rejected',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL COMMENT 'user_id or group_id',
    chat_type VARCHAR(10) NOT NULL COMMENT 'private or group',
    content_type VARCHAR(10) NOT NULL COMMENT 'text, image, file, emoji',
    content TEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=sent, 2=delivered, 3=read',
    is_recalled TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_receiver (receiver_id, chat_type, created_at),
    INDEX idx_sender (sender_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS group_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    avatar VARCHAR(255) DEFAULT '',
    owner_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'member',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user_groups (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS file_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uploader_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT DEFAULT 0,
    file_type VARCHAR(50) DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Run the init script**

Run: `mysql -u root -p < backend/src/main/resources/db/init.sql`

---

### Task 3: Create Entity Classes

**Files:**
- Create: `backend/src/main/java/com/chatroom/entity/User.java`
- Create: `backend/src/main/java/com/chatroom/entity/FriendRelation.java`
- Create: `backend/src/main/java/com/chatroom/entity/Message.java`
- Create: `backend/src/main/java/com/chatroom/entity/GroupInfo.java`
- Create: `backend/src/main/java/com/chatroom/entity/GroupMember.java`
- Create: `backend/src/main/java/com/chatroom/entity/FileRecord.java`

- [ ] **Step 1: Create User.java**

```java
package com.chatroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String signature;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: Create FriendRelation.java**

```java
package com.chatroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("friend_relation")
public class FriendRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long friendId;
    private Integer status; // 0=pending, 1=accepted, 2=blocked, 3=rejected
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: Create Message.java**

```java
package com.chatroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String chatType;   // "private" or "group"
    private String contentType; // "text", "image", "file", "emoji"
    private String content;
    private Integer status;     // 1=sent, 2=delivered, 3=read
    private Integer isRecalled;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: Create GroupInfo.java**

```java
package com.chatroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_info")
public class GroupInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatar;
    private Long ownerId;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: Create GroupMember.java**

```java
package com.chatroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_member")
public class GroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    private String role; // "owner" or "member"
    private LocalDateTime joinedAt;
}
```

- [ ] **Step 6: Create FileRecord.java**

```java
package com.chatroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_record")
public class FileRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uploaderId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private LocalDateTime createdAt;
}
```

---

### Task 4: Create Mapper Interfaces

**Files:**
- Create: `backend/src/main/java/com/chatroom/mapper/UserMapper.java`
- Create: `backend/src/main/java/com/chatroom/mapper/FriendRelationMapper.java`
- Create: `backend/src/main/java/com/chatroom/mapper/MessageMapper.java`
- Create: `backend/src/main/java/com/chatroom/mapper/GroupInfoMapper.java`
- Create: `backend/src/main/java/com/chatroom/mapper/GroupMemberMapper.java`
- Create: `backend/src/main/java/com/chatroom/mapper/FileRecordMapper.java`

- [ ] **Step 1: Create all 6 mapper interfaces**

```java
// UserMapper.java
package com.chatroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatroom.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

```java
// FriendRelationMapper.java
package com.chatroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatroom.entity.FriendRelation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FriendRelationMapper extends BaseMapper<FriendRelation> {
}
```

```java
// MessageMapper.java
package com.chatroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatroom.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
```

```java
// GroupInfoMapper.java
package com.chatroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatroom.entity.GroupInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupInfoMapper extends BaseMapper<GroupInfo> {
}
```

```java
// GroupMemberMapper.java
package com.chatroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatroom.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
}
```

```java
// FileRecordMapper.java
package com.chatroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatroom.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecord> {
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

---

### Task 5: Create Common Classes (Result + Exception)

**Files:**
- Create: `backend/src/main/java/com/chatroom/common/Result.java`
- Create: `backend/src/main/java/com/chatroom/common/ApiException.java`
- Create: `backend/src/main/java/com/chatroom/common/GlobalExceptionHandler.java`

- [ ] **Step 1: Create Result.java — uniform API response wrapper**

```java
package com.chatroom.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

- [ ] **Step 2: Create ApiException.java**

```java
package com.chatroom.common;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final int code;

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ApiException(String message) {
        this(400, message);
    }
}
```

- [ ] **Step 3: Create GlobalExceptionHandler.java**

```java
package com.chatroom.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public Result<?> handleApiException(ApiException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.error(500, "服务器内部错误: " + e.getMessage());
    }
}
```

---

### Task 6: JWT Authentication Infrastructure

**Files:**
- Create: `backend/src/main/java/com/chatroom/security/JwtTokenProvider.java`
- Create: `backend/src/main/java/com/chatroom/security/JwtAuthenticationFilter.java`

- [ ] **Step 1: Create JwtTokenProvider.java**

```java
package com.chatroom.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: Create JwtAuthenticationFilter.java**

```java
package com.chatroom.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

---

### Task 7: Spring Security Configuration

**Files:**
- Create: `backend/src/main/java/com/chatroom/config/SecurityConfig.java`

- [ ] **Step 1: Create SecurityConfig.java**

```java
package com.chatroom.config;

import com.chatroom.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/ws/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

**Why BCrypt?** BCrypt 是 Spring Security 默认的密码编码器，内置 salt 值，防止彩虹表攻击。每次加密结果不同，但验证结果一致。

---

### Task 8: Auth — Registration and Login

**Files:**
- Create: `backend/src/main/java/com/chatroom/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/chatroom/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/chatroom/dto/LoginResponse.java`
- Create: `backend/src/main/java/com/chatroom/service/AuthService.java`
- Create: `backend/src/main/java/com/chatroom/service/impl/AuthServiceImpl.java`
- Create: `backend/src/main/java/com/chatroom/controller/AuthController.java`

- [ ] **Step 1: Create DTO classes**

```java
// RegisterRequest.java
package com.chatroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度6-100位")
    private String password;
}
```

```java
// LoginRequest.java
package com.chatroom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

```java
// LoginResponse.java
package com.chatroom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
}
```

- [ ] **Step 2: Create AuthService interface and implementation**

```java
// AuthService.java
package com.chatroom.service;

import com.chatroom.dto.LoginRequest;
import com.chatroom.dto.LoginResponse;
import com.chatroom.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
```

```java
// AuthServiceImpl.java
package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatroom.common.ApiException;
import com.chatroom.dto.LoginRequest;
import com.chatroom.dto.LoginResponse;
import com.chatroom.dto.RegisterRequest;
import com.chatroom.entity.User;
import com.chatroom.mapper.UserMapper;
import com.chatroom.security.JwtTokenProvider;
import com.chatroom.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void register(RegisterRequest request) {
        // Check if username exists
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new ApiException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername()); // Default nickname = username
        userMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatar());
    }
}
```

**LambdaQueryWrapper 是什么？** MyBatis-Plus 提供的查询构造器，用 Lambda 表达式生成 SQL WHERE 条件。`eq(User::getUsername, request.getUsername())` 等价于 `WHERE username = 'xxx'`。好处是字段名有编译期检查，不会写错列名。

- [ ] **Step 3: Create AuthController.java**

```java
package com.chatroom.controller;

import com.chatroom.common.Result;
import com.chatroom.dto.LoginRequest;
import com.chatroom.dto.LoginResponse;
import com.chatroom.dto.RegisterRequest;
import com.chatroom.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }
}
```

- [ ] **Step 4: Verify API works**

Run: `cd backend && mvn spring-boot:run`
Then test with curl:
```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"123456"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"123456"}'
```
Expected: Login returns JSON with token, userId, etc.

---

### Task 9: Vue Project Setup

**Files:**
- Create: `frontend/` (via Vite)
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/stores/user.ts`
- Create: `frontend/src/stores/chat.ts`

- [ ] **Step 1: Initialize Vue project with Vite**

Run: `cd E:\Claude工作目录 && npm create vite@latest frontend -- --template vue-ts`

Run: `cd frontend && npm install`

- [ ] **Step 2: Install dependencies**

Run:
```bash
cd frontend
npm install vue-router@4 pinia axios element-plus @element-plus/icons-vue
```

- [ ] **Step 3: Configure main.ts**

```typescript
// src/main.ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

**Pinia vs Vuex:** Pinia 是 Vue 官方推荐的新一代状态管理库，API 更简洁，TypeScript 支持更好。Vuex 是上一代，社区已迁移向 Pinia。

- [ ] **Step 4: Configure router/index.ts**

```typescript
// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: true },
      redirect: '/home/chat',
      children: [
        {
          path: 'chat',
          name: 'chat',
          component: () => import('@/views/ChatPanel.vue'),
        },
        {
          path: 'chat/:id',
          name: 'chatWith',
          component: () => import('@/views/ChatPanel.vue'),
        },
        {
          path: 'friends',
          name: 'friends',
          component: () => import('@/views/FriendList.vue'),
        },
        {
          path: 'groups',
          name: 'groups',
          component: () => import('@/views/GroupList.vue'),
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
        },
        {
          path: 'search',
          name: 'search',
          component: () => import('@/views/SearchView.vue'),
        }
      ]
    },
    {
      path: '/',
      redirect: '/home'
    }
  ]
})

// Navigation guard: redirect unauthenticated users
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (!to.meta.requiresAuth && token && (to.path === '/login' || to.path === '/register')) {
    next('/home')
  } else {
    next()
  }
})

export default router
```

**路由懒加载:** `() => import(...)` 是按需加载，Vite 会自动将每个页面拆成独立的 JS 文件。首页加载时不加载其他页面代码，提升首屏速度。

**导航守卫:** `beforeEach` 在每次路由跳转前执行，检查 token 是否存在。没有 token 且访问需要认证的页面 → 跳去登录页。有 token 且访问登录页 → 直接跳首页。

- [ ] **Step 5: Create Pinia stores**

```typescript
// src/stores/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const userId = ref<number | null>(null)
  const username = ref('')
  const nickname = ref('')
  const avatar = ref('')
  const token = ref('')

  const isLoggedIn = computed(() => !!token.value)

  function setLogin(data: { userId: number; username: string; nickname: string; avatar: string; token: string }) {
    userId.value = data.userId
    username.value = data.username
    nickname.value = data.nickname
    avatar.value = data.avatar
    token.value = data.token
    localStorage.setItem('token', data.token)
  }

  function logout() {
    userId.value = null
    username.value = ''
    nickname.value = ''
    avatar.value = ''
    token.value = ''
    localStorage.removeItem('token')
  }

  return { userId, username, nickname, avatar, token, isLoggedIn, setLogin, logout }
})
```

```typescript
// src/stores/chat.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface WsMessage {
  type: string
  senderId: number
  receiverId: number
  chatType: string
  contentType: string
  content: string
  timestamp: number
  messageId?: number
}

export const useChatStore = defineStore('chat', () => {
  const ws = ref<WebSocket | null>(null)
  const unreadCounts = ref<Record<string, number>>({})
  const currentChatTarget = ref<{ id: number; chatType: string } | null>(null)

  function connect(token: string) {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) return

    const socket = new WebSocket(`ws://localhost:8080/ws?token=${token}`)

    socket.onopen = () => console.log('WebSocket connected')
    socket.onclose = () => console.log('WebSocket disconnected')
    socket.onerror = (e) => console.error('WebSocket error:', e)

    ws.value = socket
  }

  function disconnect() {
    ws.value?.close()
    ws.value = null
  }

  function send(data: WsMessage) {
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify(data))
    }
  }

  return { ws, unreadCounts, currentChatTarget, connect, disconnect, send }
})
```

- [ ] **Step 6: Verify dev server starts**

Run: `cd frontend && npm run dev`
Expected: Vite dev server starts at http://localhost:5173

---

### Task 10: Login and Register Pages

**Files:**
- Create: `frontend/src/views/LoginView.vue`
- Create: `frontend/src/views/RegisterView.vue`
- Create: `frontend/src/api/auth.ts`
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: Create API helper for auth**

```typescript
// src/api/auth.ts
import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 5000,
})

export function register(username: string, password: string) {
  return api.post('/auth/register', { username, password })
}

export function login(username: string, password: string) {
  return api.post('/auth/login', { username, password })
}
```

- [ ] **Step 2: Create LoginView.vue**

```vue
<!-- src/views/LoginView.vue -->
<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>欢迎登录</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width:100%">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <p class="register-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await login(form.username, form.password)
    userStore.setLogin(res.data.data)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #f5f5f5;
}
.login-card {
  width: 400px;
}
.login-card h2 {
  text-align: center;
  margin-bottom: 20px;
}
.register-link {
  text-align: center;
  font-size: 14px;
}
</style>
```

- [ ] **Step 3: Create RegisterView.vue**

```vue
<!-- src/views/RegisterView.vue -->
<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2>用户注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-50位，字母或数字" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="至少6位" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleRegister" style="width:100%">
            注 册
          </el-button>
        </el-form-item>
      </el-form>
      <p class="login-link">
        已有账号？<router-link to="/login">去登录</router-link>
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ username: '', password: '', confirmPassword: '' })

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) { callback(new Error('两次输入的密码不一致')) }
  else { callback() }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度3-50位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register(form.username, form.password)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #f5f5f5;
}
.register-card {
  width: 400px;
}
.register-card h2 {
  text-align: center;
  margin-bottom: 20px;
}
.login-link {
  text-align: center;
  font-size: 14px;
}
</style>
```

- [ ] **Step 4: Update App.vue**

```vue
<template>
  <router-view />
</template>
```

---

### Task 11: Axios HTTP Client with JWT Interceptor

**Files:**
- Create: `frontend/src/api/index.ts`

- [ ] **Step 1: Create centralized Axios instance with interceptors**

```typescript
// src/api/index.ts
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 10000,
})

// Request interceptor: attach JWT token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: handle 401
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      ElMessage.warning('登录已过期，请重新登录')
    }
    return Promise.reject(error)
  }
)

export default api
```

**Axios 拦截器:** 在请求发出前自动添加 Authorization 头，收到 401 响应时自动清除 token 并跳转登录页。这样每个 API 调用不需要手动处理这两件事。

- [ ] **Step 2: Update auth.ts to use shared instance**

```typescript
// src/api/auth.ts (overwrite with shared instance)
import api from './index'

export function register(username: string, password: string) {
  return api.post('/auth/register', { username, password })
}

export function login(username: string, password: string) {
  return api.post('/auth/login', { username, password })
}
```

---

### Task 12: WebSocket Configuration (Backend)

**Files:**
- Create: `backend/src/main/java/com/chatroom/config/WebSocketConfig.java`
- Create: `backend/src/main/java/com/chatroom/websocket/ChatWebSocketHandler.java`

- [ ] **Step 1: Create WebSocketConfig.java**

```java
package com.chatroom.config;

import com.chatroom.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws")
                .setAllowedOrigins("http://localhost:5173");
    }
}
```

---

### Task 13: WebSocket Handler (Backend)

**Files:**
- Create: `backend/src/main/java/com/chatroom/websocket/ChatWebSocketHandler.java`

- [ ] **Step 1: Create ChatWebSocketHandler.java — manages connections, routes messages**

```java
package com.chatroom.websocket;

import com.chatroom.common.ApiException;
import com.chatroom.entity.Message;
import com.chatroom.mapper.MessageMapper;
import com.chatroom.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JwtTokenProvider jwtTokenProvider;
    private final MessageMapper messageMapper;

    public ChatWebSocketHandler(JwtTokenProvider jwtTokenProvider, MessageMapper messageMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.messageMapper = messageMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract token from query parameter: ws://localhost:8080/ws?token=xxx
        URI uri = session.getUri();
        if (uri == null) { session.close(); return; }

        String query = uri.getQuery();
        String token = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "token".equals(pair[0])) {
                    token = pair[1];
                }
            }
        }

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        userSessions.put(userId, session);
        log.info("User {} connected via WebSocket", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        JsonNode json = objectMapper.readTree(textMessage.getPayload());
        String type = json.get("type").asText();

        switch (type) {
            case "CHAT" -> handleChatMessage(json);
            case "RECALL" -> handleRecallMessage(json);
            case "STATUS" -> handleStatusMessage(json);
            case "HEARTBEAT" -> sendMessage(session, "{\"type\":\"HEARTBEAT\"}");
        }
    }

    private void handleChatMessage(JsonNode json) throws IOException {
        Long senderId = json.get("senderId").asLong();
        Long receiverId = json.get("receiverId").asLong();
        String chatType = json.get("chatType").asText();
        String contentType = json.get("contentType").asText();
        String content = json.get("content").asText();

        // Store message in database
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setChatType(chatType);
        msg.setContentType(contentType);
        msg.setContent(content);
        msg.setStatus(1); // sent
        msg.setIsRecalled(0);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);

        // Build response JSON with message ID
        String responseJson = objectMapper.createObjectNode()
                .put("type", "CHAT")
                .put("messageId", msg.getId())
                .put("senderId", senderId)
                .put("receiverId", receiverId)
                .put("chatType", chatType)
                .put("contentType", contentType)
                .put("content", content)
                .put("status", 1)
                .put("timestamp", System.currentTimeMillis())
                .toString();

        // Send to receiver if online
        if ("private".equals(chatType)) {
            WebSocketSession receiverSession = userSessions.get(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                sendMessage(receiverSession, responseJson);
                // Update message status to delivered
                msg.setStatus(2);
                messageMapper.updateById(msg);
                // Notify sender of delivery
                notifyStatusChange(senderId, msg.getId(), 2);
            }
        }

        // Echo back to sender with message ID
        WebSocketSession senderSession = userSessions.get(senderId);
        if (senderSession != null && senderSession.isOpen()) {
            sendMessage(senderSession, responseJson);
        }
    }

    private void handleRecallMessage(JsonNode json) throws IOException {
        Long messageId = json.get("messageId").asLong();
        Message msg = messageMapper.selectById(messageId);
        if (msg == null) return;

        msg.setIsRecalled(1);
        messageMapper.updateById(msg);

        String recallJson = objectMapper.createObjectNode()
                .put("type", "RECALL")
                .put("messageId", messageId)
                .put("chatType", msg.getChatType())
                .put("receiverId", msg.getReceiverId())
                .put("senderId", msg.getSenderId())
                .toString();

        // Send recall notice to receiver
        WebSocketSession receiverSession = userSessions.get(msg.getReceiverId());
        if (receiverSession != null && receiverSession.isOpen()) {
            sendMessage(receiverSession, recallJson);
        }
        // Also notify sender
        WebSocketSession senderSession = userSessions.get(msg.getSenderId());
        if (senderSession != null && senderSession.isOpen()) {
            sendMessage(senderSession, recallJson);
        }
    }

    private void handleStatusMessage(JsonNode json) throws IOException {
        Long messageId = json.get("messageId").asLong();
        int newStatus = json.get("status").asInt();

        Message msg = messageMapper.selectById(messageId);
        if (msg != null) {
            msg.setStatus(newStatus);
            messageMapper.updateById(msg);
            notifyStatusChange(msg.getSenderId(), messageId, newStatus);
        }
    }

    private void notifyStatusChange(Long userId, Long messageId, int status) throws IOException {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            String statusJson = objectMapper.createObjectNode()
                    .put("type", "STATUS")
                    .put("messageId", messageId)
                    .put("status", status)
                    .toString();
            sendMessage(session, statusJson);
        }
    }

    private void sendMessage(WebSocketSession session, String message) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(message));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userSessions.values().remove(session);
        log.info("WebSocket connection closed: {}", status);
    }
}
```

**ConcurrentHashMap:** 线程安全的 Map。多个用户同时连接/断开时，不同线程会操作 `userSessions`，普通 HashMap 会导致数据错乱。

**为什么 sendMessage 方法要加 synchronized?** WebSocket 规范要求发送消息是线程互斥的，同时发送多条消息会导致 IllegalStateException。

- [ ] **Step 2: Verify WebSocket connects**

Start backend: `cd backend && mvn spring-boot:run`
Connect via browser console:
```javascript
const ws = new WebSocket('ws://localhost:8080/ws?token=<jwt_token_here>')
ws.onopen = () => console.log('Connected')
ws.onmessage = (e) => console.log('Received:', e.data)
```

---

### Task 14: Message REST Controller + Service

**Files:**
- Create: `backend/src/main/java/com/chatroom/service/MessageService.java`
- Create: `backend/src/main/java/com/chatroom/service/impl/MessageServiceImpl.java`
- Create: `backend/src/main/java/com/chatroom/controller/MessageController.java`

- [ ] **Step 1: Create MessageService**

```java
// MessageService.java
package com.chatroom.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatroom.entity.Message;

import java.util.List;

public interface MessageService {
    Page<Message> getMessages(Long targetId, String chatType, int page, int size, Long currentUserId);
    List<Message> searchMessages(String keyword, Long currentUserId);
    void recallMessage(Long messageId, Long currentUserId);
}
```

- [ ] **Step 2: Create MessageServiceImpl**

```java
// MessageServiceImpl.java
package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatroom.common.ApiException;
import com.chatroom.entity.Message;
import com.chatroom.mapper.MessageMapper;
import com.chatroom.service.MessageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    public MessageServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public Page<Message> getMessages(Long targetId, String chatType, int page, int size, Long currentUserId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
            .or(w2 -> w2.eq(Message::getSenderId, currentUserId).eq(Message::getReceiverId, targetId))
            .or(w2 -> w2.eq(Message::getSenderId, targetId).eq(Message::getReceiverId, currentUserId))
        )
        .eq(Message::getChatType, chatType)
        .orderByDesc(Message::getCreatedAt);

        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<Message> searchMessages(String keyword, Long currentUserId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Message::getSenderId, currentUserId).or().eq(Message::getReceiverId, currentUserId))
               .like(Message::getContent, keyword)
               .eq(Message::getContentType, "text")
               .orderByDesc(Message::getCreatedAt);
        return messageMapper.selectList(wrapper);
    }

    @Override
    public void recallMessage(Long messageId, Long currentUserId) {
        Message msg = messageMapper.selectById(messageId);
        if (msg == null) throw new ApiException("消息不存在");
        if (!msg.getSenderId().equals(currentUserId)) throw new ApiException("只能撤回自己发送的消息");
        if (msg.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(2))) {
            throw new ApiException("只能撤回2分钟内的消息");
        }
        msg.setIsRecalled(1);
        messageMapper.updateById(msg);
    }
}
```

- [ ] **Step 3: Create MessageController**

```java
// MessageController.java
package com.chatroom.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatroom.common.Result;
import com.chatroom.entity.Message;
import com.chatroom.service.MessageService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public Result<Page<Message>> getMessages(
            @RequestParam Long targetId,
            @RequestParam String chatType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = getCurrentUserId();
        return Result.ok(messageService.getMessages(targetId, chatType, page, size, currentUserId));
    }

    @PostMapping("/search")
    public Result<List<Message>> searchMessages(@RequestParam String keyword) {
        return Result.ok(messageService.searchMessages(keyword, getCurrentUserId()));
    }

    @PutMapping("/{id}/recall")
    public Result<?> recallMessage(@PathVariable Long id) {
        messageService.recallMessage(id, getCurrentUserId());
        return Result.ok();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
```

**SecurityContextHolder.getContext().getAuthentication()** — 从 Spring Security 上下文获取当前登录用户的 ID。这个 ID 是在 JwtAuthenticationFilter 中设置进去的。

---

### Task 15: HomeView Layout + NavSidebar

**Files:**
- Create: `frontend/src/views/HomeView.vue`
- Create: `frontend/src/components/NavSidebar.vue`

- [ ] **Step 1: Create NavSidebar.vue**

```vue
<!-- src/components/NavSidebar.vue -->
<template>
  <div class="nav-sidebar">
    <div class="user-avatar" @click="$router.push('/home/profile')">
      <el-avatar :src="userStore.avatar" :size="40">{{ userStore.nickname[0] }}</el-avatar>
    </div>
    <div class="nav-items">
      <div
        v-for="item in navItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: currentPath.startsWith(item.path) }"
        @click="$router.push(item.path)"
        :title="item.label"
      >
        <el-badge :value="item.path === '/home/chat' ? totalUnread : 0" :hidden="item.path !== '/home/chat' || totalUnread === 0">
          <el-icon :size="24"><component :is="item.icon" /></el-icon>
        </el-badge>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { ChatDotRound, UserFilled, Grid, Search, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()
const chatStore = useChatStore()

const currentPath = computed(() => route.path)
const totalUnread = computed(() =>
  Object.values(chatStore.unreadCounts).reduce((a, b) => a + b, 0)
)

const navItems = [
  { path: '/home/chat', label: '聊天', icon: ChatDotRound },
  { path: '/home/friends', label: '好友', icon: UserFilled },
  { path: '/home/groups', label: '群组', icon: Grid },
  { path: '/home/search', label: '搜索', icon: Search },
  { path: '/home/profile', label: '我', icon: Setting },
]
</script>

<style scoped>
.nav-sidebar {
  width: 60px;
  height: 100vh;
  background: #2e2e2e;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 20px;
}
.user-avatar { margin-bottom: 30px; cursor: pointer; }
.nav-items { display: flex; flex-direction: column; gap: 20px; }
.nav-item { color: #999; padding: 8px; border-radius: 8px; cursor: pointer; }
.nav-item:hover, .nav-item.active { color: #fff; background: #409eff; }
</style>
```

- [ ] **Step 2: Create HomeView.vue**

```vue
<!-- src/views/HomeView.vue -->
<template>
  <div class="home-container">
    <NavSidebar />
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import NavSidebar from '@/components/NavSidebar.vue'

const userStore = useUserStore()
const chatStore = useChatStore()

onMounted(() => {
  if (userStore.token) {
    chatStore.connect(userStore.token)
  }
})

onUnmounted(() => {
  chatStore.disconnect()
})
</script>

<style scoped>
.home-container { display: flex; height: 100vh; }
.main-content { flex: 1; overflow: hidden; }
</style>
```

---

### Task 16: ChatPanel — Text Messaging UI

**Files:**
- Create: `frontend/src/views/ChatPanel.vue`
- Create: `frontend/src/components/MessageList.vue`
- Create: `frontend/src/components/MessageItem.vue`
- Create: `frontend/src/components/MessageInput.vue`
- Create: `frontend/src/api/message.ts`

- [ ] **Step 1: Create message API**

```typescript
// src/api/message.ts
import api from './index'

export function getMessages(targetId: number, chatType: string, page = 1, size = 20) {
  return api.get('/messages', { params: { targetId, chatType, page, size } })
}
```

- [ ] **Step 2: Create MessageItem.vue**

```vue
<!-- src/components/MessageItem.vue -->
<template>
  <div class="message-item" :class="{ 'is-self': isSelf, 'is-recalled': message.isRecalled }">
    <el-avatar :src="avatar" :size="36">{{ nickname[0] }}</el-avatar>
    <div class="message-body">
      <div class="message-header">
        <span class="nickname">{{ nickname }}</span>
        <span class="time">{{ formatTime(message.createdAt) }}</span>
      </div>
      <div class="message-bubble" v-if="message.isRecalled">
        <em>消息已撤回</em>
      </div>
      <div class="message-bubble" v-else>
        {{ message.content }}
      </div>
      <div class="message-status" v-if="isSelf && !message.isRecalled">
        {{ statusText }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  message: any
  sender: { nickname: string; avatar: string }
}>()

const userStore = useUserStore()
const isSelf = computed(() => props.message.senderId === userStore.userId)
const nickname = computed(() => isSelf.value ? userStore.nickname : props.sender.nickname)
const avatar = computed(() => isSelf.value ? userStore.avatar : props.sender.avatar)

const statusText = computed(() => {
  const map: Record<number, string> = { 1: '已发送', 2: '已送达', 3: '已读' }
  return map[props.message.status] || ''
})

function formatTime(date: string) {
  if (!date) return ''
  return new Date(date).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.message-item { display: flex; gap: 10px; padding: 8px 20px; }
.message-item.is-self { flex-direction: row-reverse; }
.message-body { max-width: 60%; }
.message-header .nickname { font-size: 12px; color: #999; }
.message-header .time { font-size: 11px; color: #ccc; margin-left: 8px; }
.message-bubble { background: #f0f0f0; padding: 10px 14px; border-radius: 8px; margin-top: 2px; }
.is-self .message-bubble { background: #409eff; color: #fff; }
.is-recalled .message-bubble { background: transparent; color: #999; border: 1px dashed #ddd; }
.message-status { font-size: 11px; color: #bbb; text-align: right; margin-top: 2px; }
</style>
```

- [ ] **Step 3: Create MessageList.vue**

```vue
<!-- src/components/MessageList.vue -->
<template>
  <div class="message-list" ref="listRef">
    <div v-if="messages.length === 0" class="empty-hint">暂无消息，发送第一条吧</div>
    <MessageItem
      v-for="msg in messages"
      :key="msg.id"
      :message="msg"
      :sender="getSender(msg.senderId)"
    />
  </div>
</template>

<script setup lang="ts">
import { watch, ref, nextTick } from 'vue'
import MessageItem from './MessageItem.vue'

const props = defineProps<{ messages: any[]; contacts: Record<string, any> }>()
const listRef = ref<HTMLDivElement>()

function getSender(senderId: number) {
  return props.contacts[senderId] || { nickname: 'Unknown', avatar: '' }
}

watch(() => props.messages.length, () => {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
}, { immediate: true })
</script>

<style scoped>
.message-list { flex: 1; overflow-y: auto; padding: 10px 0; }
.empty-hint { text-align: center; color: #ccc; margin-top: 100px; }
</style>
```

- [ ] **Step 4: Create MessageInput.vue**

```vue
<!-- src/components/MessageInput.vue -->
<template>
  <div class="message-input">
    <div class="toolbar">
      <el-button :icon="Picture" circle text @click="triggerUpload('image')" />
      <el-button :icon="FolderOpened" circle text @click="triggerUpload('file')" />
      <el-button :icon="Sunny" circle text @click="showEmoji = !showEmoji" />
      <input ref="imageInput" type="file" accept="image/*" hidden @change="handleFileUpload('image', $event)" />
      <input ref="fileInput" type="file" hidden @change="handleFileUpload('file', $event)" />
    </div>
    <div class="input-area">
      <textarea
        v-model="text"
        @keydown.enter.exact.prevent="sendMessage"
        placeholder="输入消息，Enter 发送"
        rows="3"
      ></textarea>
      <el-button type="primary" @click="sendMessage" :disabled="!text.trim()">发送</el-button>
    </div>
    <div v-if="showEmoji" class="emoji-panel">
      <span v-for="e in emojis" :key="e" @click="text += e" style="cursor:pointer;font-size:20px;padding:4px;">{{ e }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Picture, FolderOpened, Sunny } from '@element-plus/icons-vue'

const emit = defineEmits<{ send: [data: { type: string; contentType: string; content: string; file?: File }] }>()
const text = ref('')
const showEmoji = ref(false)
const imageInput = ref<HTMLInputElement>()
const fileInput = ref<HTMLInputElement>()

const emojis = ['😀','😂','😍','🤔','😢','😡','👍','👎','❤️','🔥','⭐','🎉']

function sendMessage() {
  if (!text.value.trim()) return
  emit('send', { type: 'CHAT', contentType: 'text', content: text.value.trim() })
  text.value = ''
  showEmoji.value = false
}

function triggerUpload(type: string) {
  if (type === 'image') imageInput.value?.click()
  else fileInput.value?.click()
}

function handleFileUpload(contentType: string, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    emit('send', { type: 'CHAT', contentType, content: file.name, file })
    input.value = ''
  }
}
</script>

<style scoped>
.message-input { border-top: 1px solid #eee; padding: 10px; }
.toolbar { display: flex; gap: 4px; margin-bottom: 6px; }
.input-area { display: flex; gap: 10px; }
.input-area textarea { flex: 1; border: 1px solid #ddd; border-radius: 6px; padding: 8px; resize: none; outline: none; font-size: 14px; }
.input-area textarea:focus { border-color: #409eff; }
.emoji-panel { padding: 8px; border-top: 1px solid #eee; display: flex; flex-wrap: wrap; gap: 4px; }
</style>
```

- [ ] **Step 5: Create ChatPanel.vue**

```vue
<!-- src/views/ChatPanel.vue -->
<template>
  <div class="chat-panel">
    <div v-if="!currentChatTarget" class="no-chat">
      <p>选择一个好友或群组开始聊天</p>
    </div>
    <template v-else>
      <div class="chat-header">
        <span>{{ chatTargetName }}</span>
      </div>
      <MessageList :messages="messages" :contacts="contacts" />
      <MessageInput @send="handleSend" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { getMessages } from '@/api/message'
import MessageList from '@/components/MessageList.vue'
import MessageInput from '@/components/MessageInput.vue'

const route = useRoute()
const chatStore = useChatStore()
const userStore = useUserStore()
const messages = ref<any[]>([])
const contacts = ref<Record<string, any>>({})

const currentChatTarget = computed(() => {
  const id = route.params.id
  if (!id) return null
  return { id: Number(id), chatType: 'private' as const }
})

const chatTargetName = computed(() => {
  if (!currentChatTarget.value) return ''
  return contacts.value[currentChatTarget.value.id]?.nickname || '用户' + currentChatTarget.value.id
})

watch(currentChatTarget, async (target) => {
  if (!target) { messages.value = []; return }
  chatStore.currentChatTarget = target
  try {
    const res = await getMessages(target.id, target.chatType)
    messages.value = (res.data.data?.records || []).reverse()
  } catch (e) { /* handle error */ }
}, { immediate: true })

function handleSend(data: { type: string; contentType: string; content: string; file?: File }) {
  if (!currentChatTarget.value) return
  chatStore.send({
    type: data.type,
    senderId: userStore.userId!,
    receiverId: currentChatTarget.value.id,
    chatType: currentChatTarget.value.chatType,
    contentType: data.contentType,
    content: data.content,
    timestamp: Date.now(),
  })
}

// Listen for incoming WebSocket messages
function onWsMessage(e: MessageEvent) {
  try {
    const msg = JSON.parse(e.data)
    if (msg.type === 'CHAT') {
      messages.value.push(msg)
    } else if (msg.type === 'RECALL') {
      const found = messages.value.find(m => m.id === msg.messageId)
      if (found) found.isRecalled = 1
    } else if (msg.type === 'STATUS') {
      const found = messages.value.find(m => m.id === msg.messageId)
      if (found) found.status = msg.status
    }
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  chatStore.ws?.addEventListener('message', onWsMessage)
})

onUnmounted(() => {
  chatStore.ws?.removeEventListener('message', onWsMessage)
})
</script>

<style scoped>
.chat-panel { display: flex; flex-direction: column; height: 100vh; }
.no-chat { flex: 1; display: flex; align-items: center; justify-content: center; color: #ccc; }
.chat-header { padding: 16px 20px; border-bottom: 1px solid #eee; font-size: 16px; font-weight: 500; }
</style>
```

---

### Task 17: Verify P0 — End-to-End Text Chat

- [ ] **Step 1: Verify full flow**

1. Start MySQL (ensure `chatroom_db` exists and tables are created)
2. Start backend: `cd backend && mvn spring-boot:run`
3. Start frontend: `cd frontend && npm run dev`
4. Open browser A at http://localhost:5173 — register user "alice"
5. Open browser B — register user "bob"
6. Log in as alice, navigate to `/home/chat/2` (bob's user ID)
7. Type a message and press Enter
8. In browser B, should receive the message in real-time

**P0 COMPLETE.** The application now has registration, login, and real-time text messaging.

---

## PHASE 1: Friends + Rich Messages (P1)

### Task 18: Friend Controller + Service

**Files:**
- Create: `backend/src/main/java/com/chatroom/service/FriendService.java`
- Create: `backend/src/main/java/com/chatroom/service/impl/FriendServiceImpl.java`
- Create: `backend/src/main/java/com/chatroom/controller/FriendController.java`
- Create: `backend/src/main/java/com/chatroom/service/UserService.java`
- Create: `backend/src/main/java/com/chatroom/service/impl/UserServiceImpl.java`
- Create: `backend/src/main/java/com/chatroom/controller/UserController.java`

- [ ] **Step 1: Create FriendService**

```java
// FriendService.java
package com.chatroom.service;

import com.chatroom.entity.FriendRelation;
import java.util.List;
import java.util.Map;

public interface FriendService {
    void sendRequest(Long userId, Long friendId);
    void acceptRequest(Long relationId, Long currentUserId);
    void rejectRequest(Long relationId, Long currentUserId);
    void deleteFriend(Long relationId, Long currentUserId);
    void blockFriend(Long relationId, Long currentUserId);
    List<Map<String, Object>> getFriendList(Long userId);
    List<Map<String, Object>> getPendingRequests(Long userId);
}
```

- [ ] **Step 2: Create FriendServiceImpl**

```java
// FriendServiceImpl.java
package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatroom.common.ApiException;
import com.chatroom.entity.FriendRelation;
import com.chatroom.entity.User;
import com.chatroom.mapper.FriendRelationMapper;
import com.chatroom.mapper.UserMapper;
import com.chatroom.service.FriendService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRelationMapper friendRelationMapper;
    private final UserMapper userMapper;

    public FriendServiceImpl(FriendRelationMapper friendRelationMapper, UserMapper userMapper) {
        this.friendRelationMapper = friendRelationMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void sendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) throw new ApiException("不能添加自己为好友");

        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getUserId, userId)
               .eq(FriendRelation::getFriendId, friendId);
        if (friendRelationMapper.selectCount(wrapper) > 0) {
            throw new ApiException("已经发送过好友申请");
        }

        FriendRelation relation = new FriendRelation();
        relation.setUserId(userId);
        relation.setFriendId(friendId);
        relation.setStatus(0); // pending
        friendRelationMapper.insert(relation);
    }

    @Override
    public void acceptRequest(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.selectById(relationId);
        if (relation == null) throw new ApiException("申请不存在");
        if (!relation.getFriendId().equals(currentUserId)) {
            throw new ApiException("无权操作此申请");
        }
        relation.setStatus(1); // accepted
        friendRelationMapper.updateById(relation);
    }

    @Override
    public void rejectRequest(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.selectById(relationId);
        if (relation == null) throw new ApiException("申请不存在");
        if (!relation.getFriendId().equals(currentUserId)) {
            throw new ApiException("无权操作此申请");
        }
        relation.setStatus(3); // rejected
        friendRelationMapper.updateById(relation);
    }

    @Override
    public void deleteFriend(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.selectById(relationId);
        if (relation == null) throw new ApiException("好友关系不存在");
        friendRelationMapper.deleteById(relationId);
    }

    @Override
    public void blockFriend(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.selectById(relationId);
        if (relation == null) throw new ApiException("好友关系不存在");
        if (!relation.getUserId().equals(currentUserId) && !relation.getFriendId().equals(currentUserId)) {
            throw new ApiException("无权操作");
        }
        relation.setStatus(2); // blocked
        friendRelationMapper.updateById(relation);
    }

    @Override
    public List<Map<String, Object>> getFriendList(Long userId) {
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(FriendRelation::getUserId, userId).or().eq(FriendRelation::getFriendId, userId))
               .eq(FriendRelation::getStatus, 1);

        List<FriendRelation> relations = friendRelationMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();

        for (FriendRelation r : relations) {
            Long friendId = r.getUserId().equals(userId) ? r.getFriendId() : r.getUserId();
            User friend = userMapper.selectById(friendId);
            if (friend != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("relationId", r.getId());
                map.put("userId", friend.getId());
                map.put("username", friend.getUsername());
                map.put("nickname", friend.getNickname());
                map.put("avatar", friend.getAvatar());
                map.put("signature", friend.getSignature());
                result.add(map);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getPendingRequests(Long userId) {
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getFriendId, userId)
               .eq(FriendRelation::getStatus, 0);

        List<FriendRelation> relations = friendRelationMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();

        for (FriendRelation r : relations) {
            User sender = userMapper.selectById(r.getUserId());
            if (sender != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("relationId", r.getId());
                map.put("userId", sender.getId());
                map.put("username", sender.getUsername());
                map.put("nickname", sender.getNickname());
                map.put("avatar", sender.getAvatar());
                result.add(map);
            }
        }
        return result;
    }
}
```

- [ ] **Step 3: Create FriendController**

```java
// FriendController.java
package com.chatroom.controller;

import com.chatroom.common.Result;
import com.chatroom.service.FriendService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/request")
    public Result<?> sendRequest(@RequestParam Long friendId) {
        friendService.sendRequest(getCurrentUserId(), friendId);
        return Result.ok();
    }

    @PutMapping("/{id}/accept")
    public Result<?> accept(@PathVariable Long id) {
        friendService.acceptRequest(id, getCurrentUserId());
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    public Result<?> reject(@PathVariable Long id) {
        friendService.rejectRequest(id, getCurrentUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        friendService.deleteFriend(id, getCurrentUserId());
        return Result.ok();
    }

    @PutMapping("/{id}/block")
    public Result<?> block(@PathVariable Long id) {
        friendService.blockFriend(id, getCurrentUserId());
        return Result.ok();
    }

    @GetMapping
    public Result<Map<String, Object>> getFriends() {
        Long userId = getCurrentUserId();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("friends", friendService.getFriendList(userId));
        data.put("pending", friendService.getPendingRequests(userId));
        return Result.ok(data);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
```

- [ ] **Step 4: Create UserService and UserServiceImpl (search + profile)**

```java
// UserService.java
package com.chatroom.service;

import com.chatroom.entity.User;
import java.util.List;

public interface UserService {
    List<User> searchUsers(String keyword);
    User getUserById(Long id);
    void updateProfile(Long userId, String nickname, String signature);
}
```

```java
// UserServiceImpl.java
package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatroom.common.ApiException;
import com.chatroom.entity.User;
import com.chatroom.mapper.UserMapper;
import com.chatroom.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<User> searchUsers(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        return userMapper.selectList(wrapper);
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new ApiException("用户不存在");
        user.setPassword(null); // Never return password
        return user;
    }

    @Override
    public void updateProfile(Long userId, String nickname, String signature) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new ApiException("用户不存在");
        if (nickname != null && !nickname.isBlank()) user.setNickname(nickname);
        if (signature != null && !signature.isBlank()) user.setSignature(signature);
        userMapper.updateById(user);
    }
}
```

- [ ] **Step 5: Create UserController**

```java
// UserController.java
package com.chatroom.controller;

import com.chatroom.common.Result;
import com.chatroom.entity.User;
import com.chatroom.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam String keyword) {
        return Result.ok(userService.searchUsers(keyword));
    }

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        return Result.ok(userService.getUserById(id));
    }

    @PutMapping("/me")
    public Result<?> updateProfile(@RequestBody User updateRequest) {
        userService.updateProfile(getCurrentUserId(), updateRequest.getNickname(), updateRequest.getSignature());
        return Result.ok();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
```

---

### Task 19: Frontend Friend Pages

**Files:**
- Create: `frontend/src/api/friend.ts`
- Create: `frontend/src/views/FriendList.vue`
- Create: `frontend/src/components/AddFriendDialog.vue`

- [ ] **Step 1: Create friend API**

```typescript
// src/api/friend.ts
import api from './index'

export function getFriends() { return api.get('/friends') }
export function sendFriendRequest(friendId: number) { return api.post('/friends/request', null, { params: { friendId } }) }
export function acceptRequest(relationId: number) { return api.put(`/friends/${relationId}/accept`) }
export function rejectRequest(relationId: number) { return api.put(`/friends/${relationId}/reject`) }
export function deleteFriend(relationId: number) { return api.delete(`/friends/${relationId}`) }
export function blockFriend(relationId: number) { return api.put(`/friends/${relationId}/block`) }
export function searchUsers(keyword: string) { return api.get('/users/search', { params: { keyword } }) }
```

- [ ] **Step 2: Create FriendList.vue**

```vue
<!-- src/views/FriendList.vue -->
<template>
  <div class="friend-list-page">
    <div class="header">
      <h3>好友</h3>
      <el-button @click="showAddDialog = true" type="primary" size="small">添加好友</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="好友列表" name="friends">
        <div v-if="friends.length === 0" class="empty">暂无好友</div>
        <div v-for="f in friends" :key="f.relationId" class="friend-item" @click="$router.push(`/home/chat/${f.userId}`)">
          <el-avatar :src="f.avatar" :size="40">{{ f.nickname[0] }}</el-avatar>
          <div class="info">
            <div class="name">{{ f.nickname }}</div>
            <div class="sig">{{ f.signature }}</div>
          </div>
          <el-dropdown>
            <el-icon><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleDelete(f.relationId)">删除好友</el-dropdown-item>
                <el-dropdown-item @click="handleBlock(f.relationId)">拉黑</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-tab-pane>

      <el-tab-pane label="好友申请" :badge="pending.length || ''">
        <div v-if="pending.length === 0" class="empty">暂无新的好友申请</div>
        <div v-for="p in pending" :key="p.relationId" class="friend-item">
          <el-avatar :src="p.avatar" :size="40">{{ p.nickname[0] }}</el-avatar>
          <div class="info">
            <div class="name">{{ p.nickname }}</div>
          </div>
          <div class="actions">
            <el-button type="primary" size="small" @click="handleAccept(p.relationId)">同意</el-button>
            <el-button size="small" @click="handleReject(p.relationId)">拒绝</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <AddFriendDialog v-model:visible="showAddDialog" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFriends, acceptRequest, rejectRequest, deleteFriend, blockFriend } from '@/api/friend'
import { ElMessage } from 'element-plus'
import { MoreFilled } from '@element-plus/icons-vue'
import AddFriendDialog from '@/components/AddFriendDialog.vue'

const activeTab = ref('friends')
const friends = ref<any[]>([])
const pending = ref<any[]>([])
const showAddDialog = ref(false)

async function loadFriends() {
  try {
    const res = await getFriends()
    friends.value = res.data.data.friends || []
    pending.value = res.data.data.pending || []
  } catch (e) { /* handle error */ }
}

async function handleAccept(id: number) {
  await acceptRequest(id)
  ElMessage.success('已同意好友申请')
  loadFriends()
}

async function handleReject(id: number) {
  await rejectRequest(id)
  ElMessage.info('已拒绝好友申请')
  loadFriends()
}

async function handleDelete(id: number) {
  await deleteFriend(id)
  ElMessage.success('已删除好友')
  loadFriends()
}

async function handleBlock(id: number) {
  await blockFriend(id)
  ElMessage.success('已拉黑')
  loadFriends()
}

onMounted(loadFriends)
</script>

<style scoped>
.friend-list-page { padding: 20px; height: 100vh; overflow-y: auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.friend-item { display: flex; align-items: center; gap: 12px; padding: 10px; border-radius: 8px; cursor: pointer; }
.friend-item:hover { background: #f5f5f5; }
.info { flex: 1; }
.name { font-size: 15px; font-weight: 500; }
.sig { font-size: 12px; color: #999; }
.actions { display: flex; gap: 8px; }
.empty { text-align: center; color: #ccc; margin-top: 40px; }
</style>
```

- [ ] **Step 3: Create AddFriendDialog.vue**

```vue
<!-- src/components/AddFriendDialog.vue -->
<template>
  <el-dialog v-model="dialogVisible" title="添加好友" width="400px">
    <el-input v-model="keyword" placeholder="输入用户名搜索" clearable @keyup.enter="handleSearch" />
    <div class="search-results" v-if="results.length > 0">
      <div v-for="user in results" :key="user.id" class="search-item">
        <el-avatar :size="36">{{ user.nickname[0] }}</el-avatar>
        <div class="info">
          <div>{{ user.nickname }}</div>
          <div class="username">@{{ user.username }}</div>
        </div>
        <el-button size="small" type="primary" @click="handleAdd(user.id)">添加</el-button>
      </div>
    </div>
    <div v-if="searched && results.length === 0" class="empty-result">未找到用户</div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { searchUsers, sendFriendRequest } from '@/api/friend'
import { ElMessage } from 'element-plus'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{ 'update:visible': [value: boolean] }>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const keyword = ref('')
const results = ref<any[]>([])
const searched = ref(false)

async function handleSearch() {
  if (!keyword.value.trim()) return
  const res = await searchUsers(keyword.value.trim())
  results.value = res.data.data || []
  searched.value = true
}

async function handleAdd(userId: number) {
  await sendFriendRequest(userId)
  ElMessage.success('好友申请已发送')
  dialogVisible.value = false
  keyword.value = ''
  results.value = []
  searched.value = false
}
</script>

<style scoped>
.search-results { margin-top: 12px; }
.search-item { display: flex; align-items: center; gap: 10px; padding: 8px; border-radius: 6px; }
.search-item:hover { background: #f5f5f5; }
.info { flex: 1; }
.username { font-size: 12px; color: #999; }
.empty-result { text-align: center; color: #ccc; margin-top: 20px; }
</style>
```

---

### Task 20: File Upload (Backend)

**Files:**
- Create: `backend/src/main/java/com/chatroom/config/UploadConfig.java`
- Create: `backend/src/main/java/com/chatroom/service/FileService.java`
- Create: `backend/src/main/java/com/chatroom/service/impl/FileServiceImpl.java`
- Create: `backend/src/main/java/com/chatroom/controller/UploadController.java`

- [ ] **Step 1: Create UploadConfig.java**

```java
package com.chatroom.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadConfig implements WebMvcConfigurer {

    @Value("${upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
```

- [ ] **Step 2: Create FileService and controller**

```java
// FileService.java
package com.chatroom.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface FileService {
    Map<String, String> upload(MultipartFile file, Long uploaderId);
}
```

```java
// FileServiceImpl.java
package com.chatroom.service.impl;

import com.chatroom.entity.FileRecord;
import com.chatroom.mapper.FileRecordMapper;
import com.chatroom.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private final FileRecordMapper fileRecordMapper;

    @Value("${upload.path}")
    private String uploadPath;

    public FileServiceImpl(FileRecordMapper fileRecordMapper) {
        this.fileRecordMapper = fileRecordMapper;
    }

    @Override
    public Map<String, String> upload(MultipartFile file, Long uploaderId) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + ext;

            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();

            File dest = new File(uploadPath + File.separator + newFileName);
            file.transferTo(dest);

            FileRecord record = new FileRecord();
            record.setUploaderId(uploaderId);
            record.setFileName(originalName);
            record.setFilePath("/uploads/" + newFileName);
            record.setFileSize(file.getSize());
            record.setFileType(file.getContentType());
            fileRecordMapper.insert(record);

            Map<String, String> result = new HashMap<>();
            result.put("url", "/uploads/" + newFileName);
            result.put("fileName", originalName);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }
}
```

```java
// UploadController.java
package com.chatroom.controller;

import com.chatroom.common.Result;
import com.chatroom.service.FileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class UploadController {

    private final FileService fileService;

    public UploadController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(fileService.upload(file, userId));
    }
}
```

---

### Task 21: Image/File/Emoji Sending in Frontend

The MessageInput component in Task 16 already has basic support. Now wire it up with the actual upload flow.

- [ ] **Step 1: Update MessageInput.vue handleFileUpload function to upload before sending**

```typescript
// Add to MessageInput.vue script section (update handleFileUpload)
import { uploadFile } from '@/api/upload'

// Replace the handleFileUpload logic:
async function handleFileUpload(contentType: string, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''

  try {
    const res = await uploadFile(file)
    emit('send', {
      type: 'CHAT',
      contentType,
      content: res.data.data.url, // Send file URL as content
      file
    })
  } catch (e) {
    ElMessage.error('文件上传失败')
  }
}
```

- [ ] **Step 2: Create upload API**

```typescript
// src/api/upload.ts
import api from './index'

export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
```

- [ ] **Step 3: Update MessageItem.vue to render image/file messages**

MessageItem.vue already handles text. Now update the bubble to render images and files:

```vue
<!-- In MessageItem.vue, replace the message-bubble div with: -->
<div class="message-bubble" v-if="message.isRecalled">
  <em>消息已撤回</em>
</div>
<div class="message-bubble" v-else>
  <img v-if="message.contentType === 'image'" :src="'http://localhost:8080' + message.content" style="max-width:200px;border-radius:4px;" />
  <a v-else-if="message.contentType === 'file'" :href="'http://localhost:8080' + message.content" target="_blank">
    <el-icon><Document /></el-icon> {{ message.content?.split('/').pop() }}
  </a>
  <span v-else>{{ message.content }}</span>
</div>
```

**P1 COMPLETE.** Application now supports friend management, image/file/emoji message types.

---

## PHASE 2: Groups + Advanced Features (P2)

### Task 22: Group Controller + Service (Backend)

**Files:**
- Create: `backend/src/main/java/com/chatroom/service/GroupService.java`
- Create: `backend/src/main/java/com/chatroom/service/impl/GroupServiceImpl.java`
- Create: `backend/src/main/java/com/chatroom/controller/GroupController.java`

- [ ] **Step 1: Create GroupService**

```java
// GroupService.java
package com.chatroom.service;

import com.chatroom.entity.GroupInfo;
import com.chatroom.entity.GroupMember;
import java.util.List;
import java.util.Map;

public interface GroupService {
    GroupInfo createGroup(String name, Long ownerId);
    void addMember(Long groupId, Long userId, Long operatorId);
    void removeMember(Long groupId, Long userId, Long operatorId);
    List<Map<String, Object>> getMyGroups(Long userId);
    List<Map<String, Object>> getMembers(Long groupId);
}
```

- [ ] **Step 2: Create GroupServiceImpl**

```java
// GroupServiceImpl.java
package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatroom.common.ApiException;
import com.chatroom.entity.GroupInfo;
import com.chatroom.entity.GroupMember;
import com.chatroom.entity.User;
import com.chatroom.mapper.GroupInfoMapper;
import com.chatroom.mapper.GroupMemberMapper;
import com.chatroom.mapper.UserMapper;
import com.chatroom.service.GroupService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupInfoMapper groupInfoMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final UserMapper userMapper;

    public GroupServiceImpl(GroupInfoMapper gim, GroupMemberMapper gmm, UserMapper um) {
        this.groupInfoMapper = gim;
        this.groupMemberMapper = gmm;
        this.userMapper = um;
    }

    @Override
    public GroupInfo createGroup(String name, Long ownerId) {
        GroupInfo group = new GroupInfo();
        group.setName(name);
        group.setOwnerId(ownerId);
        groupInfoMapper.insert(group);

        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(ownerId);
        member.setRole("owner");
        groupMemberMapper.insert(member);

        return group;
    }

    @Override
    public void addMember(Long groupId, Long userId, Long operatorId) {
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) throw new ApiException("群组不存在");

        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
        if (groupMemberMapper.selectCount(wrapper) > 0) {
            throw new ApiException("用户已在群中");
        }

        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole("member");
        groupMemberMapper.insert(member);
    }

    @Override
    public void removeMember(Long groupId, Long userId, Long operatorId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
        GroupMember member = groupMemberMapper.selectOne(wrapper);
        if (member == null) throw new ApiException("成员不在群中");

        // Allow self-leave, or owner can remove members
        if (userId.equals(operatorId) || member.getRole().equals("owner")) {
            // owner can't be removed (transfer first)
            if (member.getRole().equals("owner") && !userId.equals(operatorId)) {
                throw new ApiException("不能移除群主");
            }
            groupMemberMapper.deleteById(member);
        } else {
            throw new ApiException("无权操作");
        }
    }

    @Override
    public List<Map<String, Object>> getMyGroups(Long userId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getUserId, userId);
        List<GroupMember> memberships = groupMemberMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupMember m : memberships) {
            GroupInfo group = groupInfoMapper.selectById(m.getGroupId());
            if (group != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("groupId", group.getId());
                map.put("name", group.getName());
                map.put("avatar", group.getAvatar());
                map.put("ownerId", group.getOwnerId());
                map.put("role", m.getRole());
                result.add(map);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMembers(Long groupId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        List<GroupMember> members = groupMemberMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupMember m : members) {
            User user = userMapper.selectById(m.getUserId());
            if (user != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("userId", user.getId());
                map.put("nickname", user.getNickname());
                map.put("avatar", user.getAvatar());
                map.put("role", m.getRole());
                result.add(map);
            }
        }
        return result;
    }
}
```

- [ ] **Step 3: Create GroupController**

```java
// GroupController.java
package com.chatroom.controller;

import com.chatroom.common.Result;
import com.chatroom.entity.GroupInfo;
import com.chatroom.service.GroupService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public Result<GroupInfo> create(@RequestParam String name) {
        return Result.ok(groupService.createGroup(name, getCurrentUserId()));
    }

    @GetMapping
    public Result<List<Map<String, Object>>> getMyGroups() {
        return Result.ok(groupService.getMyGroups(getCurrentUserId()));
    }

    @GetMapping("/{id}/members")
    public Result<List<Map<String, Object>>> getMembers(@PathVariable Long id) {
        return Result.ok(groupService.getMembers(id));
    }

    @PostMapping("/{id}/members")
    public Result<?> addMember(@PathVariable Long id, @RequestParam Long userId) {
        groupService.addMember(id, userId, getCurrentUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public Result<?> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        groupService.removeMember(id, userId, getCurrentUserId());
        return Result.ok();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
```

---

### Task 23: Group Pages (Frontend)

**Files:**
- Create: `frontend/src/api/group.ts`
- Create: `frontend/src/views/GroupList.vue`

- [ ] **Step 1: Create group API**

```typescript
// src/api/group.ts
import api from './index'

export function getMyGroups() { return api.get('/groups') }
export function createGroup(name: string) { return api.post('/groups', null, { params: { name } }) }
export function getGroupMembers(groupId: number) { return api.get(`/groups/${groupId}/members`) }
export function addGroupMember(groupId: number, userId: number) {
  return api.post(`/groups/${groupId}/members`, null, { params: { userId } })
}
export function removeGroupMember(groupId: number, userId: number) {
  return api.delete(`/groups/${groupId}/members/${userId}`)
}
```

- [ ] **Step 2: Create GroupList.vue**

```vue
<!-- src/views/GroupList.vue -->
<template>
  <div class="group-list-page">
    <div class="header">
      <h3>群组</h3>
      <el-button @click="showCreateDialog = true" type="primary" size="small">创建群组</el-button>
    </div>
    <div v-if="groups.length === 0" class="empty">暂无群组</div>
    <div v-for="g in groups" :key="g.groupId" class="group-item" @click="$router.push(`/home/chat/${g.groupId}`)">
      <el-avatar :size="40">{{ g.name[0] }}</el-avatar>
      <div class="info">
        <div class="name">{{ g.name }}</div>
        <div class="role">{{ g.role === 'owner' ? '群主' : '成员' }}</div>
      </div>
    </div>

    <el-dialog v-model="showCreateDialog" title="创建群组" width="400px">
      <el-input v-model="newGroupName" placeholder="输入群名称" />
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyGroups, createGroup } from '@/api/group'
import { ElMessage } from 'element-plus'

const groups = ref<any[]>([])
const showCreateDialog = ref(false)
const newGroupName = ref('')

async function loadGroups() {
  const res = await getMyGroups()
  groups.value = res.data.data || []
}

async function handleCreate() {
  if (!newGroupName.value.trim()) return
  await createGroup(newGroupName.value.trim())
  ElMessage.success('群组已创建')
  showCreateDialog.value = false
  newGroupName.value = ''
  loadGroups()
}

onMounted(loadGroups)
</script>

<style scoped>
.group-list-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.group-item { display: flex; align-items: center; gap: 12px; padding: 10px; border-radius: 8px; cursor: pointer; }
.group-item:hover { background: #f5f5f5; }
.info { flex: 1; }
.name { font-size: 15px; font-weight: 500; }
.role { font-size: 12px; color: #999; }
.empty { text-align: center; color: #ccc; margin-top: 40px; }
</style>
```

---

### Task 24: Message Search (Frontend)

**Files:**
- Create: `frontend/src/views/SearchView.vue`

- [ ] **Step 1: Create SearchView.vue (user search + message search)**

```vue
<!-- src/views/SearchView.vue -->
<template>
  <div class="search-page">
    <h3>搜索</h3>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="搜索用户" name="users">
        <el-input v-model="userKeyword" placeholder="输入用户名搜索" @keyup.enter="searchUsers" />
        <div v-for="u in userResults" :key="u.id" class="search-item" @click="$router.push(`/home/chat/${u.id}`)">
          <el-avatar :size="36">{{ u.nickname[0] }}</el-avatar>
          <div>{{ u.nickname }} (@{{ u.username }})</div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="搜索消息" name="messages">
        <el-input v-model="msgKeyword" placeholder="输入关键词搜索" @keyup.enter="searchMessages" />
        <div v-for="m in msgResults" :key="m.id" class="msg-item" @click="$router.push(`/home/chat/${m.senderId}`)">
          <div class="msg-sender">{{ m.senderId }}</div>
          <div class="msg-content">{{ m.content }}</div>
          <div class="msg-time">{{ new Date(m.createdAt).toLocaleString() }}</div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchUsers } from '@/api/friend'
import api from '@/api/index'

const activeTab = ref('users')
const userKeyword = ref('')
const msgKeyword = ref('')
const userResults = ref<any[]>([])
const msgResults = ref<any[]>([])

async function searchUser() {
  if (!userKeyword.value.trim()) return
  const res = await searchUsers(userKeyword.value.trim())
  userResults.value = res.data.data || []
}

async function searchMessages() {
  if (!msgKeyword.value.trim()) return
  const res = await api.post('/messages/search', null, { params: { keyword: msgKeyword.value.trim() } })
  msgResults.value = res.data.data || []
}
</script>

<style scoped>
.search-page { padding: 20px; }
.search-item, .msg-item { display: flex; align-items: center; gap: 10px; padding: 10px; cursor: pointer; border-radius: 6px; margin-top: 8px; }
.search-item:hover, .msg-item:hover { background: #f5f5f5; }
.msg-sender { font-weight: 500; }
.msg-content { flex: 1; color: #666; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.msg-time { font-size: 11px; color: #ccc; }
</style>
```

---

### Task 25: Profile Page (Frontend)

**Files:**
- Create: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: Create ProfileView.vue**

```vue
<!-- src/views/ProfileView.vue -->
<template>
  <div class="profile-page">
    <h3>个人信息</h3>
    <el-form :model="form" label-width="80px">
      <el-form-item label="头像">
        <el-avatar :size="60">{{ form.nickname[0] }}</el-avatar>
      </el-form-item>
      <el-form-item label="用户名">
        <el-input :value="userStore.username" disabled />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" />
      </el-form-item>
      <el-form-item label="个性签名">
        <el-input v-model="form.signature" type="textarea" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        <el-button type="danger" @click="handleLogout">退出登录</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import api from '@/api/index'

const router = useRouter()
const userStore = useUserStore()
const saving = ref(false)

const form = reactive({
  nickname: userStore.nickname,
  signature: ''
})

async function handleSave() {
  saving.value = true
  try {
    await api.put('/users/me', { nickname: form.nickname, signature: form.signature })
    userStore.nickname = form.nickname
    ElMessage.success('保存成功')
  } catch(e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.profile-page { padding: 20px; max-width: 500px; }
</style>
```

**P2 COMPLETE.** Group chat, message search, recall, and blacklisting are all functional.

---

## PHASE 3: Deploy & Polish (P3)

### Task 26: Production Build Configuration

**Files:**
- Create: `frontend/vite.config.ts` (update with proxy config)
- Create: `backend/src/main/resources/application-prod.yml`

- [ ] **Step 1: Configure Vite proxy for development**

The vite.config.ts should already exist from Vite scaffolding. Update it:

```typescript
// frontend/vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      }
    }
  }
})
```

- [ ] **Step 2: Create production application config**

```yaml
# backend/src/main/resources/application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatroom_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl

jwt:
  secret: ${JWT_SECRET}

upload:
  path: /data/uploads
```

- [ ] **Step 3: Build both projects**

```bash
# Backend
cd backend
mvn clean package -DskipTests
# Output: backend/target/chatroom-backend-1.0.0.jar

# Frontend
cd frontend
npm run build
# Output: frontend/dist/
```

- [ ] **Step 4: Nginx configuration**

Create `nginx.conf` (not committed, just for reference):
```nginx
server {
    listen 80;
    server_name localhost;

    location / {
        root /path/to/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /uploads/ {
        proxy_pass http://localhost:8080;
    }
}
```

**P3 COMPLETE.** Application is ready for production deployment.

---

## Summary

| Phase | Tasks | What You Have |
|-------|-------|---------------|
| P0 | 1-17 | Registration, login, JWT auth, WebSocket real-time text chat |
| P1 | 18-21 | Friend management, image/file/emoji messages, message status |
| P2 | 22-25 | Group chat, message search, message recall, blacklist, profile |
| P3 | 26 | Production build configuration, Nginx deploy config |
