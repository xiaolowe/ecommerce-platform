# 项目协作约定

## 技术栈

- 后端使用 Java 21、Spring Boot 3.3.5、Spring Cloud Gateway、MyBatis 和 Flyway。
- 前端统一使用 Vue 3、TypeScript、Vite、Vue Router、Pinia；运营后台使用 Element Plus。
- 业务服务包括认证、商品、库存、订单、媒体、支付和购物车服务；每个服务拥有独立数据库。

## 本地开发

- 用户本地开发统一使用全局 JDK 21、Maven、Node.js 和 pnpm，不要擅自切换到项目 `.tools` 中的工具链。
- 后端在 IntelliJ IDEA 中运行共享配置，前端源码位于 `frontend`，在 VS Code 中运行。
- 后端开发端口：网关 8080、认证 8081、商品 8082、库存 8083、订单 8084、媒体 8085、支付 8086、购物车 8087。
- 前端开发端口：运营后台 5173、用户商城 5174。
- 默认本地管理员为 `admin@example.com`；认证服务通过共享 IDEA 运行配置初始化账号。
- 本地业务数据使用 H2；媒体图片和媒体元数据保存在项目 `.data` 中，`.data` 不进入 Git。
- 支付服务本地使用 Mock 渠道；支付服务与订单服务之间的内部确认请求使用 `INTERNAL_API_KEY`。
- 管理端订单和支付查询使用 `/api/v1/admin/**`，必须在网关和业务服务两层校验 `ROLE_ADMIN`。
- 用户订单列表使用 `/api/v1/orders`，必须按 JWT subject 对应的客户 ID 隔离数据。
- 用户购物车使用 `/api/v1/cart`，必须按 JWT subject 持久化并按客户 ID 隔离。
- 多商品订单必须由服务端重新读取商品价格，并对全部订单明细执行库存预占与失败补偿。
- 收货地址和订单收货信息都属于用户私有数据，必须按 JWT subject 校验归属。
- 待支付订单一律通过订单服务取消；库存只能由订单服务使用内部令牌释放，不允许前端直接调用库存预占接口。

## Git 与 GitHub

- 所有 Git commit 备注必须使用中文。
- 推送到 GitHub 的提交必须保持中文提交备注。
- 提交前不得包含本地上传图片、密钥、工具链缓存或构建产物。
