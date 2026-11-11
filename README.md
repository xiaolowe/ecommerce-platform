# Ecommerce Platform

Java 21 + Spring Boot + MyBatis 的多服务电商基础骨架。当前版本跑通“注册登录 → 商品上架 → 初始化库存 → 下单预占库存 → Mock 支付 → 订单已支付”，并支持用户取消、支付超时自动关单和库存幂等释放，提供 Docker Compose 与 Kubernetes 部署入口。

## 服务

| 模块 | 端口 | 职责 |
|---|---:|---|
| `gateway` | 8080 | 统一入口、JWT 鉴权、路由、CORS |
| `services/auth-service` | 8081 | 用户、登录、JWT 签发、管理员初始化 |
| `services/catalog-service` | 8082 | 商品、价格、上下架 |
| `services/inventory-service` | 8083 | 可售库存、预占、释放、幂等预占单 |
| `services/order-service` | 8084 | 订单快照、状态机、库存预占与失败补偿 |
| `services/media-service` | 8085 | 本地图片上传、素材库、公开图片读取 |
| `services/payment-service` | 8086 | 支付单、Mock 渠道、订单支付确认同步 |
| `frontend/admin-web` | 3001 | Vue 3 + Element Plus 运营后台 |
| `frontend/storefront-web` | 3002 | Vue 3 用户商城、购物袋和订单确认 |

每个业务服务拥有独立数据库 schema，通过 Flyway 管理表结构。网关和业务服务使用同一个 JWT 密钥，业务服务也会校验 Token，避免只依赖网关保护。

前端统一使用 Vue 3、TypeScript、Vue Router、Pinia 和 Vite。运营后台使用 Element Plus 按需加载；用户商城使用轻量组件，避免后台依赖进入商城包。

后端数据访问统一使用 MyBatis：Mapper 接口位于各服务业务包，SQL 位于 `src/main/resources/mapper/*.xml`。数据库结构由 Flyway 管理，本地默认 H2，容器和生产环境使用 MySQL。

## 本地运行

没有安装 MySQL、JDK 21、Maven 或 Docker 时，可以直接使用 H2 内存数据库运行整套前后端：

```bash
./scripts/run-local-h2.sh
```

脚本会按需下载 JDK 21、Maven、Node.js 22 和 pnpm，打包后端、启动 6 个 Java 服务，再启动两个 Vue 3 开发服务器。所有工具都放在项目 `.tools` 中，不需要全局安装。首次运行需要联网。

脚本把 JDK、按需下载的 Node.js/pnpm、Maven、Java 依赖和临时下载限制在项目 `.tools` 与 `frontend/node_modules` 内，不会写入 `~/.m2` 或 `~/.npm`。pnpm 的内容仓库使用全局默认位置，不强制写入项目。

如果默认端口已被其他程序占用，脚本会自动向后寻找可用端口，并在启动完成后打印实际访问地址。

运行地址：

- 运营后台：<http://localhost:5173>
- 用户商城：<http://localhost:5174>
- API 网关：<http://localhost:8080>

业务 H2 数据只存在于当前运行进程中，按 `Ctrl+C` 后清空。图片素材及其元数据会保存在项目 `.data/`，重启后仍可继续使用。这种方式适合本地开发和演示，不替代真实 MySQL 验证。

运营后台的“图片素材”页面支持上传、浏览、复制地址和删除图片。图片文件保存在项目 `.data/media`，商品数据库只保存 `/media/...` 相对地址。当前本地存储支持 JPG、PNG 和 GIF，单张最大 10 MB。

上传图片后，可以在“商品管理”中设置或更换商品主图。商城首页和商品详情会自动展示商品主图。

服务启动后可以生成演示商品和库存：

```bash
./scripts/seed_demo_data.py
```

默认生成 300 个商品，每个商品带 20～300 件库存。数量和网关地址可调整：

```bash
./scripts/seed_demo_data.py --count 500 --base-url http://127.0.0.1:8080
```

H2 环境每次重新启动都会清空数据，重启后需要再次执行种子脚本。

如果已安装 Docker 24+ 和 Docker Compose v2，也可以使用 MySQL 8.4 完整环境。首次运行前可复制 `.env.example` 为 `.env` 并修改密码与 JWT 密钥。

```bash
docker compose up --build -d
docker compose ps
BOOTSTRAP_ADMIN_EMAIL=admin@example.com \
BOOTSTRAP_ADMIN_PASSWORD='Admin123!ChangeMe' \
./scripts/smoke-test.sh
```

访问前端：

- 运营后台：<http://localhost:3001>
- 用户商城：<http://localhost:3002>

查看统一入口：

```bash
curl http://localhost:8080/actuator/health
```

停止环境：

```bash
docker compose down
```

如需同时删除本地 MySQL 数据卷，显式执行 `docker compose down -v`。该命令会删除本地开发数据。

## 本机构建

如果电脑没有安装 JDK 21 和 Maven，可以让项目自动准备工具链：

```bash
./scripts/bootstrap-build.sh
```

该脚本会：

- 优先复用已有的 JDK 21。
- 没有 JDK 21 时，从 Adoptium 下载经 SHA-256 校验的 Eclipse Temurin 21。
- 将 JDK 放入项目 `.tools`，不修改系统 Java。
- 使用 Maven Wrapper 下载 Maven 3.9.9。
- 将 Maven 依赖放入 `.tools/m2`，并执行完整 `verify`。

首次执行需要联网。后续离线构建可以显式增加 Maven 离线参数：

```bash
./scripts/bootstrap-build.sh -o verify
```

同时构建并验证 Java 后端和两个 Vue 3 前端：

```bash
./scripts/build-all.sh
```

前端工具链同样可自动准备：优先复用 Node.js 22+ 和 pnpm 11，否则下载 Node.js 22 并在项目内安装 pnpm。

已经安装 JDK 21 时也可以直接使用 Maven Wrapper：

```bash
./mvnw -B verify
```

单独启动某个服务时默认使用内存 H2；生产配置通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 和 `JWT_SECRET` 注入。

## 前端开发

前端使用 pnpm workspace，Node.js 22+：

```bash
cd frontend
pnpm install
pnpm dev:admin
pnpm dev:storefront
```

开发服务器会把 `/api` 代理到本机 `8080` 网关。完整检查命令：

```bash
pnpm typecheck
pnpm test
pnpm build
```

运营后台包含管理员登录、商品创建、库存管理、订单查询和支付记录查询。订单与支付管理接口同时受网关和业务服务两层 `ROLE_ADMIN` 权限保护，运营人员可以按状态和关键字分页检索，并查看订单商品、支付时间及支付同步状态。用户商城包含商品浏览、商品详情、注册登录、购物袋、订单确认、本地 Mock 支付和“我的订单”；用户订单接口按 JWT 中的客户 ID 过滤，只能读取当前用户自己的订单。当前订单服务按单个商品创建订单，多明细购物袋批量下单需要下一阶段扩展订单接口。

支付接口通过网关访问：

```bash
# 创建支付单，同一订单重复请求会返回同一支付单
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <用户 Token>" \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"<订单 ID>","channel":"MOCK"}'

# 模拟支付成功，支付服务随后确认订单并推进为 PAID
curl -X POST http://localhost:8080/api/v1/payments/<支付单号>/mock-pay \
  -H "Authorization: Bearer <用户 Token>"
```

Mock 支付不会产生真实扣款。支付服务以订单服务返回的金额和币种创建支付单，支付成功后通过内部令牌调用订单服务；同步失败时由定时任务重试。真实微信、支付宝渠道应实现 `PaymentProvider`，并补充签名验签、回调日志、主动查单、退款与对账。

订单默认有 15 分钟支付窗口，可通过 `ORDER_PAYMENT_TTL_SECONDS` 调整。待支付订单可以由用户主动取消；超时订单由订单服务定时任务自动关闭。取消成功后订单服务通过内部令牌调用库存服务释放预占库存，释放失败会自动重试。支付晚于订单关闭时会标记为“人工处理”，不会无限重试。

## IDEA 与 VS Code 联合开发

后端在 IntelliJ IDEA 中启动，前端在 VS Code 中启动：

1. 使用 IDEA 打开项目根目录并导入 Maven 项目。
2. 在 `Project Structure → Project SDK` 中选择全局 JDK 21。没有全局 JDK 时，也可以临时使用项目内 JDK：

   ```text
   /Users/xiaolowe/agent/codex/ecommerce-platform/.tools/jdk-21
   ```

3. 将项目语言级别设置为 Java 21。
4. 在运行配置中选择“后端-全部服务”，或单独启动某个服务进行调试。
5. 使用 VS Code 打开同一个项目根目录。
6. 在 VS Code 中选择 `Terminal → Run Task → 前端：启动两个服务`。

VS Code 任务默认使用全局 Node.js 和 pnpm，pnpm 的内容仓库使用全局默认位置。可通过 `pnpm store path` 查看实际路径。前后端联合开发时不要同时运行 `run-local-h2.sh`，否则会争用 8080～8085、5173、5174 端口。

## 云上部署

1. 准备六套 MySQL schema，或者同一 MySQL 实例中的六个逻辑库。
2. 修改 `deploy/k8s/configmap.yaml` 中的数据库地址、域名和 CORS 来源。
3. 替换 `deploy/k8s/app.yaml` 中的镜像地址。
4. 从 `deploy/k8s/secrets.env.example` 创建 Kubernetes Secret。
5. 安装 Nginx Ingress Controller 后应用清单。

```bash
kubectl -n ecommerce create secret generic ecommerce-secrets \
  --from-env-file=deploy/k8s/secrets.env
kubectl apply -k deploy/k8s
kubectl -n ecommerce rollout status deployment --all
```

Secret 必须在 Deployment 启动前创建。生产环境不要把数据库、JWT 密钥或管理员密码提交到 Git。建议使用云厂商 Secret Manager、External Secrets 或 Sealed Secrets。

## 扩展入口

- 新服务复制一个 `services/*-service` 模块，并保持自己的数据库和迁移目录。
- 当前 Mock 支付通过同步内部接口确认订单；真实支付渠道应增加回调日志、签名验签、主动查单和幂等事件消费。
- 支付回调和履约通知应使用消息队列、Outbox、消费幂等和死信重试。
- 生产环境增加服务身份鉴权、mTLS、API 限流、分布式追踪导出和集中日志。
- 数据库增长后按服务独立扩容，不跨服务共享实体模型或表。

## 当前边界

这是可运行的基础骨架，不是完整交易系统。当前支付只覆盖本地 Mock 闭环，尚未接入真实渠道、自动退款、对账、优惠、售后、搜索和消息队列。订单超时和库存释放目前使用定时任务与内部令牌，生产级跨服务恢复仍需要服务身份鉴权、Outbox 和可靠的补偿任务共同保证。
