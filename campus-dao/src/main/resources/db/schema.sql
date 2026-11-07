-- 低碳校园资源循环流转系统 数据库结构
-- 数据库：SQLite（文件 data/campus.db，首次启动自动执行本脚本）

CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    salt          TEXT    NOT NULL,
    nickname      TEXT    NOT NULL,
    role          TEXT    NOT NULL DEFAULT 'STUDENT',
    credit_score  REAL    NOT NULL DEFAULT 100.0,
    enabled       INTEGER NOT NULL DEFAULT 1,
    created_at    TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT    NOT NULL UNIQUE,
    parent_id  INTEGER REFERENCES categories(id) ON DELETE SET NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    enabled    INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS items (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id        INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id     INTEGER NOT NULL REFERENCES categories(id),
    title           TEXT    NOT NULL,
    description     TEXT    NOT NULL,
    condition_level TEXT    NOT NULL DEFAULT 'GOOD',
    trade_type      TEXT    NOT NULL DEFAULT 'SELL',
    price           REAL    NOT NULL DEFAULT 0,
    status          TEXT    NOT NULL DEFAULT 'ON_SALE',
    tags            TEXT    NOT NULL DEFAULT '',
    image_path      TEXT,
    view_count      INTEGER NOT NULL DEFAULT 0,
    created_at      TEXT    NOT NULL,
    updated_at      TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_items_owner    ON items(owner_id);
CREATE INDEX IF NOT EXISTS idx_items_category ON items(category_id);
CREATE INDEX IF NOT EXISTS idx_items_status   ON items(status);
CREATE INDEX IF NOT EXISTS idx_items_created  ON items(created_at DESC);
-- 状态与分类组合查询（分类筛选 + 默认在售过滤）
CREATE INDEX IF NOT EXISTS idx_items_status_category ON items(status, category_id);

CREATE TABLE IF NOT EXISTS favorites (
    user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_id    INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    created_at TEXT    NOT NULL,
    PRIMARY KEY (user_id, item_id)
);

CREATE TABLE IF NOT EXISTS deals (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id      INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    buyer_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         TEXT    NOT NULL,
    message      TEXT,
    status       TEXT    NOT NULL DEFAULT 'PENDING',
    created_at   TEXT    NOT NULL,
    updated_at   TEXT    NOT NULL,
    completed_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_deals_buyer  ON deals(buyer_id);
CREATE INDEX IF NOT EXISTS idx_deals_seller ON deals(seller_id);
CREATE INDEX IF NOT EXISTS idx_deals_item   ON deals(item_id);
-- 按状态计数与已完成口径统计
CREATE INDEX IF NOT EXISTS idx_deals_status ON deals(status);

CREATE TABLE IF NOT EXISTS reviews (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    deal_id     INTEGER NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    reviewer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewee_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TEXT    NOT NULL,
    UNIQUE (deal_id, reviewer_id)
);

CREATE INDEX IF NOT EXISTS idx_reviews_reviewee ON reviews(reviewee_id);

CREATE TABLE IF NOT EXISTS operation_logs (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action     TEXT    NOT NULL,
    detail     TEXT,
    created_at TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_logs_created ON operation_logs(created_at DESC);

CREATE TABLE IF NOT EXISTS carbon_factors (
    category_id     INTEGER PRIMARY KEY REFERENCES categories(id) ON DELETE CASCADE,
    kg_co2_saved    REAL NOT NULL DEFAULT 0
);
