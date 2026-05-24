# 餐厅管理系统

这是基于现有 JavaEE 课设骨架改造的餐厅管理系统，沿用原有的 Spring Boot + MyBatis-Plus + Vue 3 + Vite 技术栈，当前首版已覆盖：

- 员工登录与注册
- 桌台管理
- 菜品分类、菜品与配料绑定
- 原料分类与库存管理
- 游客/服务员/前台下单
- 后厨制作队列与状态流转
- 催单、订单提权、前台收款、订单结单
- 光盘记录与经营分析
- 独立 Vue 3 + Vite 前端工作台（位于 `frontend/`）

## 环境要求

- JDK 17
- Maven 3.9+
- MySQL 8.0
- Node.js 20+

## 数据库初始化

1. 创建数据库并导入脚本：

```sql
source sql/restaurant_management_mysql8.sql;
```

2. 修改 [application.yml](/mnt/host/c/Users/xiaoy/Desktop/javaee%20sx/src/main/resources/application.yml) 中的数据库账号密码。

## 启动方式

```bash
mvn spring-boot:run
```

或：

```bash
mvn clean package
java -jar target/restaurant-management-system-0.0.1-SNAPSHOT.jar
```

## 前端构建与托管方式

前端工程位于 `frontend/`，使用 Vue 3 + Vite 开发。当前方案改为构建后直接输出到 Spring Boot 的 `src/main/resources/static/`，由后端统一托管，不再依赖单独运行 Vite 开发服务器。

```bash
cd frontend
npm install
npm run build
```

构建完成后，直接在 IDEA 中启动后端项目，然后访问：

- 页面地址：`http://localhost:8080/`
- 接口地址：`http://localhost:8080/api/...`

如果你仍然想单独调试前端，也可以继续使用：

```bash
cd frontend
npm run dev
```

当前前端已实现：

- 登录与注册
- 总览经营卡片
- 桌台管理
- 原料分类与库存维护
- 菜品分类、菜品、配料维护
- 创建订单、收款、提权、结单
- 后厨制作队列与状态更新
- 服务员确认上桌与清桌记录
- 光盘情况记录
- 店长/经理经营分析总览

## 主要接口

- `POST /api/users/register`
- `POST /api/users/login`
- `GET /api/dashboard/overview`
- `GET /api/tables`
- `POST /api/tables`
- `GET /api/material-categories`
- `POST /api/material-categories`
- `GET /api/materials`
- `POST /api/materials`
- `PUT /api/materials/{id}`
- `POST /api/materials/refresh-status`
- `GET /api/dish-categories`
- `POST /api/dish-categories`
- `GET /api/dishes`
- `POST /api/dishes`
- `PUT /api/dishes/{id}`
- `POST /api/dishes/{id}/materials`
- `GET /api/orders`
- `POST /api/orders`
- `POST /api/orders/{id}/pay`
- `POST /api/orders/{id}/priority`
- `POST /api/orders/{id}/complete`
- `POST /api/orders/{id}/waste-records`
- `GET /api/kitchen/queue`
- `POST /api/kitchen/items/{id}/start`
- `POST /api/kitchen/items/{id}/ready`
- `POST /api/kitchen/items/{id}/serve`
- `POST /api/kitchen/items/{id}/urge`
- `GET /api/analytics/summary`

## 说明

- 当前前端可构建到 Spring Boot `static` 目录，由后端直接托管。
- 推荐优先导入 `sql/restaurant_management_mysql8.sql` 后再启动。
- 如果你之前已经导入过旧版本餐厅脚本，请重新执行一次 SQL，以补齐 `plate_waste_record` 表。
- 当前前端已按角色裁剪导航：服务员、前台、后厨、经理、店长看到的工作区入口不同。
