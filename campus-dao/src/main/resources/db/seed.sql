-- 演示数据（可重复执行：INSERT OR IGNORE）
-- 默认账号：admin / Admin@123（管理员），linxiaolv / Student@123，chenxy / Student@123

INSERT OR IGNORE INTO users (id, username, password_hash, salt, nickname, role, credit_score, enabled, created_at) VALUES
    (1, 'admin',     'd7012ca1b3bfac30043a5399dafe3c855bba3d7da684f606939b93d13c25eff4', 'n2K7xQ1p', '系统管理员', 'ADMIN',   100.0, 1, '2026-09-01 09:00:00'),
    (2, 'linxiaolv', 'ea83d4d3facb3497da170569503cf6b5eba6119c42bc3e3d51bd01d572dda003', 'a9Rm3Tz0', '林小绿',     'STUDENT', 100.0, 1, '2026-09-02 10:20:00'),
    (3, 'chenxy',    '84c34e5407ad6bc3db4329ecde8f289e51f8e05ffbd2f1842aa43e5bde6827dd', 'k4Lp8Wv2', '陈同学',     'STUDENT', 100.0, 1, '2026-09-03 14:05:00');

INSERT OR IGNORE INTO categories (id, name, parent_id, sort_order, enabled) VALUES
    (1, '教材书籍', NULL, 10, 1),
    (2, '数码配件', NULL, 20, 1),
    (3, '文体用品', NULL, 30, 1),
    (4, '生活物资', NULL, 40, 1),
    (5, '宿舍家电', NULL, 50, 1);

INSERT OR IGNORE INTO carbon_factors (category_id, kg_co2_saved) VALUES
    (1, 2.5),
    (2, 8.0),
    (3, 1.5),
    (4, 1.0),
    (5, 12.0);

INSERT OR IGNORE INTO items (id, owner_id, category_id, title, description, condition_level, trade_type, price, status, tags, image_path, view_count, created_at, updated_at) VALUES
    (1, 2, 1, '高等数学（第七版）上下册', '大一用过的教材，笔记完整，无缺页。', 'GOOD',     'SELL',     25.0, 'ON_SALE', '教材,数学', NULL, 12, '2026-10-28 09:30:00', '2026-10-28 09:30:00'),
    (2, 3, 2, '罗技 K380 蓝牙键盘',        '三模连接，成色很好，因换机械键盘闲置。', 'LIKE_NEW', 'EXCHANGE', 0.0,  'ON_SALE', '数码,键盘', NULL, 8,  '2026-10-29 15:10:00', '2026-10-29 15:10:00'),
    (3, 2, 3, '羽毛球拍一对',              '入门级球拍，适合课余锻炼，附球。',       'GOOD',     'SELL',     40.0, 'ON_SALE', '运动,羽毛球', NULL, 5, '2026-10-30 18:40:00', '2026-10-30 18:40:00');
