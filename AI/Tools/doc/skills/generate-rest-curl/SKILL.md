---
name: generate-rest-curl
description: Builds copy-pastable curl commands for REST endpoints in this repo from Controller or Feign API definitions, request DTOs, and path annotations. Use when the user asks for curl、接口curl、HTTP调试命令、本地调用示例, or wants to hit a specified Java interface/endpoint.
---

# 指定接口生成 curl

## 目标

根据用户点名的 **Feign 接口**（`api/.../client/*Api.java`）、**Controller**（`interfaces/.../rest/*Controller.java`）或 **方法名/路径片段**，输出可直接粘贴执行的 `curl`；路径与方法必须与源码注解一致。

## 解析顺序

1. **定位契约**：优先读对应的 `@FeignClient` 接口（`path` + 方法上 `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` / `@PatchMapping`）。Controller 若实现该 Api，以 **Feign 接口上的映射为准**（与运行时对外路径一致）。
2. **拼完整路径**：`path`（类级）与 method 上的 value/path **按 Spring 规则拼接**（注意首尾 `/`，避免出现 `//`）。
3. **方法**：与注解一致；`@RequestBody` → JSON body；`@RequestParam` → query string；`@PathVariable` → 替换 URL 占位符。
4. **请求体**：打开入参类型（`*Request` / DTO），按字段生成 **最小合法示例 JSON**（必填字段给占位值；枚举用文档或 `*Enum` 中的合法 `code`；日期字符串用 ISO 或项目常用格式）。
5. **响应**：本工程统一 `HttpResult<T>`，curl 输出中可一句说明成功时外层结构，不必展开。

## 本仓库约定（写入 curl 注释或说明）

- 服务默认端口见 `start/src/main/resources/bootstrap.yml` 的 `server.port`（当前常见为 **9190**）；实际环境可能被 Nacos 覆盖，占位写 `http://localhost:9190` 并注明可改。
- Feign 对外前缀习惯为 **`/zta/scm/order/...`**，与 `module-rules/java-api-contract.mdc` 中路径约定一致；不要用其它系统网关路径冒充本服务。
- `Content-Type: application/json` 用于带 body 的请求。
- **认证**：若代码或用户场景需要（网关 Header、Token、开放平台 `AppKey`/`Sign`/`Timestamp` 等），用占位符或注释说明，勿臆造密钥；可参考 `start/src/test/java/com/zta/scm/order/api/erp/ApiErpTest.java` 的签名思路仅作说明，不把真实 secret 写进 skill 输出。

## 输出格式

- 每条接口一段，含：简短说明、**一条多行 curl**（`\` 续行）、可选「最小 JSON」单独代码块便于编辑。
- Windows 用户可附注：将 `\` 改为 `^` 或合成一行。

## 示例（对齐本仓库）

输入：采购单推送 `ErpOpenPlatformOrderApi#pushPurchaseOrder`。

输出应包含：

- `POST http://localhost:9190/zta/scm/order/open/erp/purchase/push`
- `-H 'Content-Type: application/json'`
- `-d '{ ... }'` 中与 `PurchaseOrderPushRequest` 及子对象字段一致的示例结构

（具体字段以当时源码为准，skill 不要求背字段列表。）

## 校验

生成后自检：**URL 与方法**是否与 `*Api.java` 或 Controller 注解一致；**JSON 字段名**是否与 Java 属性（Jackson 命名）一致。
