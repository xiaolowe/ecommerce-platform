#!/usr/bin/env python3
import argparse
import concurrent.futures
import json
import os
import random
import sys
import threading
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone


CATALOG = {
    "数码": (
        ["星环手机", "云杉耳机", "曜石平板", "极光键盘", "轻羽充电器", "远山相机"],
        (29900, 899900),
    ),
    "家居": (
        ["原木边几", "亚麻抱枕", "晨光台灯", "陶白餐具", "柔软地毯", "藤编收纳篮"],
        (3900, 129900),
    ),
    "穿搭": (
        ["轻量夹克", "有机棉衬衫", "城市双肩包", "羊毛围巾", "复古贝雷帽", "舒适休闲鞋"],
        (5900, 169900),
    ),
    "运动": (
        ["缓震跑鞋", "户外水壶", "训练哑铃", "防泼水外套", "瑜伽垫", "骑行头盔"],
        (2900, 99900),
    ),
    "食品": (
        ["手冲咖啡豆", "山野蜂蜜", "坚果礼盒", "桂花乌龙", "低糖曲奇", "海盐巧克力"],
        (1900, 39900),
    ),
    "美妆": (
        ["修护面霜", "清透防晒", "香氛护手霜", "柔雾唇膏", "氨基酸洁面", "木质香水"],
        (2900, 89900),
    ),
    "办公": (
        ["阅读台灯", "桌面收纳", "降噪耳麦", "便携显示器", "无针订书机", "人体工学坐垫"],
        (3900, 129900),
    ),
}

SERIES = ["基础款", "轻享版", "城市版", "自然系列", "专业版", "限定色", "经典款"]
COLORS = ["曜石黑", "雾白", "松石绿", "砂岩灰", "海盐蓝", "暖杏色"]
MATERIALS = ["轻盈", "耐用", "舒适", "简约", "随身", "高质感"]


class SeedError(RuntimeError):
    pass


class ApiClient:
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.token = ""

    def request(self, method: str, path: str, payload=None, authenticated=True):
        body = None if payload is None else json.dumps(payload).encode("utf-8")
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "X-Trace-Id": f"seed-{time.time_ns()}",
        }
        if authenticated and self.token:
            headers["Authorization"] = f"Bearer {self.token}"

        request = urllib.request.Request(
            f"{self.base_url}{path}",
            data=body,
            headers=headers,
            method=method,
        )
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                raw = response.read()
                return response.status, json.loads(raw) if raw else None
        except urllib.error.HTTPError as error:
            raw = error.read().decode("utf-8", errors="replace")
            try:
                detail = json.loads(raw).get("detail", raw)
            except json.JSONDecodeError:
                detail = raw
            raise SeedError(f"{method} {path} -> {error.code}: {detail}") from error
        except urllib.error.URLError as error:
            raise SeedError(f"无法连接 {self.base_url}: {error.reason}") from error

    def login(self, email: str, password: str):
        status, data = self.request(
            "POST",
            "/api/v1/auth/login",
            {"email": email, "password": password},
            authenticated=False,
        )
        if status != 200 or not data or not data.get("accessToken"):
            raise SeedError("管理员登录失败")
        if data.get("user", {}).get("role") != "ADMIN":
            raise SeedError("当前账号不是管理员")
        self.token = data["accessToken"]


def build_product(index: int, batch: str):
    category = random.choice(list(CATALOG))
    names, (min_price, max_price) = CATALOG[category]
    base_name = random.choice(names)
    series = random.choice(SERIES)
    material = random.choice(MATERIALS)
    color = random.choice(COLORS)
    model = f"{index:03d}"
    name = f"{material}{base_name} {series} {model}"
    sku = f"DEMO-{batch}-{index:04d}"
    return {
        "sku": sku,
        "name": name,
        "description": f"演示商品 · {category} · {color} · 批次 {batch}",
        "priceCents": random.randint(min_price, max_price),
        "currency": "CNY",
    }, random.randint(20, 300)


def main():
    parser = argparse.ArgumentParser(description="为电商平台生成演示商品和库存")
    parser.add_argument("--base-url", default=os.getenv("BASE_URL", "http://127.0.0.1:8080"))
    parser.add_argument("--count", type=int, default=int(os.getenv("PRODUCT_COUNT", "300")))
    parser.add_argument("--workers", type=int, default=int(os.getenv("SEED_WORKERS", "8")))
    parser.add_argument("--admin-email", default=os.getenv("BOOTSTRAP_ADMIN_EMAIL", "admin@example.com"))
    parser.add_argument(
        "--admin-password",
        default=os.getenv("BOOTSTRAP_ADMIN_PASSWORD", "Admin123!ChangeMe"),
    )
    args = parser.parse_args()

    if args.count < 1 or args.count > 5000:
        parser.error("--count 必须在 1 到 5000 之间")

    random.seed(20261106)
    batch = datetime.now(timezone.utc).strftime("%y%m%d%H%M%S")
    client = ApiClient(args.base_url)
    client.login(args.admin_email, args.admin_password)

    print(f"管理员登录成功，准备生成 {args.count} 个商品，批次 {batch}")
    progress_lock = threading.Lock()
    completed = 0
    failures = []

    def seed_one(index: int):
        product_payload, stock = build_product(index, batch)
        status, product = client.request("POST", "/api/v1/products", product_payload)
        if status != 201 or not product or not product.get("id"):
            raise SeedError(f"商品 {index} 创建失败")
        client.request(
            "POST",
            "/api/v1/inventory/stocks",
            {"productId": product["id"], "availableStock": stock},
        )
        return product

    with concurrent.futures.ThreadPoolExecutor(max_workers=max(args.workers, 1)) as executor:
        futures = {
            executor.submit(seed_one, index): index
            for index in range(1, args.count + 1)
        }
        for future in concurrent.futures.as_completed(futures):
            index = futures[future]
            try:
                future.result()
            except Exception as error:
                failures.append((index, str(error)))
            with progress_lock:
                completed += 1
                if completed % 25 == 0 or completed == args.count:
                    print(f"进度 {completed}/{args.count}")

    if failures:
        print(f"生成完成，但有 {len(failures)} 个失败：", file=sys.stderr)
        for index, message in failures[:10]:
            print(f"  #{index}: {message}", file=sys.stderr)
        return 1

    status, page = client.request("GET", "/api/v1/products?page=0&size=1")
    total = page["page"]["totalElements"] if status == 200 and page else "未知"
    print("测试数据生成完成")
    print(f"  商品批次：{batch}")
    print(f"  当前商品总数：{total}")
    print("  商城：http://localhost:5174")
    print("  后台：http://localhost:5173/products")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except SeedError as error:
        print(f"seed_demo_data: {error}", file=sys.stderr)
        raise SystemExit(1)
