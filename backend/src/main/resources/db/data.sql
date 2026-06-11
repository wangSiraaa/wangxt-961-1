-- 初始化测试用户数据
INSERT IGNORE INTO sys_user (id, username, password, real_name, phone, id_card, role, balance, is_verified) VALUES
(1, 'resident1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张三', '13800138001', '110101199001010001', 'RESIDENT', 100.00, 1),
(2, 'resident2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李四', '13800138002', '110101199002020002', 'RESIDENT', -50.00, 1),
(3, 'resident3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王五', '13800138003', '110101199003030003', 'RESIDENT', 200.00, 0),
(4, 'property1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '赵物业', '13800138004', '110101198501010004', 'PROPERTY', 0.00, 1),
(5, 'safety1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '钱安全', '13800138005', '110101198502020005', 'SAFETY_OFFICER', 0.00, 1);

-- 初始化车辆数据
INSERT IGNORE INTO vehicle (id, user_id, plate_number, vehicle_type, battery_capacity, is_verified, status) VALUES
(1, 1, '京A12345', '比亚迪汉EV', 76.90, 1, 'NORMAL'),
(2, 1, '京A67890', '特斯拉Model 3', 60.00, 1, 'NORMAL'),
(3, 2, '京B11111', '蔚来ES6', 70.00, 1, 'NORMAL'),
(4, 3, '京C22222', '小鹏P7', 80.90, 0, 'NORMAL');

-- 初始化车棚数据
INSERT IGNORE INTO charging_shed (id, shed_name, location, total_ports, available_ports, status, manager_id) VALUES
(1, '1号车棚', '小区北区1号楼北侧', 10, 8, 'OPEN', 4),
(2, '2号车棚', '小区南区5号楼南侧', 8, 6, 'OPEN', 4),
(3, '3号车棚', '小区东区停车场', 12, 12, 'MAINTENANCE', 4);

-- 初始化充电位数据
INSERT IGNORE INTO charging_port (id, shed_id, port_code, port_type, power_rating, status, current_temperature, is_power_on) VALUES
(1, 1, 'A01', 'NORMAL', 7.0, 'AVAILABLE', 25.0, 1),
(2, 1, 'A02', 'NORMAL', 7.0, 'AVAILABLE', 25.0, 1),
(3, 1, 'A03', 'NORMAL', 7.0, 'OCCUPIED', 25.0, 1),
(4, 1, 'A04', 'NORMAL', 7.0, 'AVAILABLE', 25.0, 1),
(5, 1, 'A05', 'FAST', 60.0, 'AVAILABLE', 25.0, 1),
(6, 1, 'A06', 'FAST', 60.0, 'FAULT', 25.0, 0),
(7, 1, 'A07', 'NORMAL', 7.0, 'AVAILABLE', 25.0, 1),
(8, 1, 'A08', 'NORMAL', 7.0, 'MAINTENANCE', 25.0, 0),
(9, 1, 'A09', 'NORMAL', 7.0, 'AVAILABLE', 25.0, 1),
(10, 1, 'A10', 'NORMAL', 7.0, 'AVAILABLE', 25.0, 1),
(11, 2, 'B01', 'NORMAL', 7.0, 'AVAILABLE', 25.0, 1),
(12, 2, 'B02', 'NORMAL', 7.0, 'AVAILABLE', 25.0, 1),
(13, 2, 'B03', 'NORMAL', 7.0, 'OCCUPIED', 58.0, 1),
(14, 2, 'B04', 'NORMAL', 7.0, 'AVAILABLE', 25.0, 1),
(15, 2, 'B05', 'FAST', 60.0, 'AVAILABLE', 25.0, 1),
(16, 2, 'B06', 'FAST', 60.0, 'AVAILABLE', 25.0, 1),
(17, 2, 'B07', 'NORMAL', 7.0, 'AVAILABLE', 25.0, 1),
(18, 2, 'B08', 'NORMAL', 7.0, 'AVAILABLE', 25.0, 1);

-- 初始化计费规则
INSERT IGNORE INTO pricing_rule (id, rule_name, shed_id, price_per_kwh, service_fee, peak_start_time, peak_end_time, peak_price_multiplier, valley_start_time, valley_end_time, valley_price_multiplier, is_default, status) VALUES
(1, '默认计费规则', NULL, 1.20, 0.50, '08:00:00', '22:00:00', 1.5, '22:00:00', '08:00:00', 0.5, 1, 'ACTIVE'),
(2, '1号车棚高峰计费', 1, 1.00, 0.60, '10:00:00', '20:00:00', 1.3, '00:00:00', '06:00:00', 0.6, 0, 'ACTIVE');

-- 初始化预约数据
INSERT IGNORE INTO reservation (id, user_id, vehicle_id, port_id, shed_id, reserve_start_time, reserve_end_time, actual_start_time, actual_end_time, status) VALUES
(1, 1, 1, 3, 1, NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), NOW(), NULL, 'IN_PROGRESS'),
(2, 2, 3, 13, 2, NOW(), DATE_ADD(NOW(), INTERVAL 3 HOUR), NOW(), NULL, 'IN_PROGRESS'),
(3, 1, 2, 1, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY + 2 HOUR), NULL, NULL, 'CONFIRMED');

-- 初始化充电记录
INSERT IGNORE INTO charging_record (id, reservation_id, port_id, user_id, vehicle_id, start_time, start_soc, energy_consumed, max_temperature, avg_temperature, status) VALUES
(1, 1, 3, 1, 1, NOW(), 30.0, 15.5, 42.0, 38.0, 'CHARGING'),
(2, 2, 13, 2, 3, NOW(), 20.0, 22.3, 58.0, 45.0, 'CHARGING');

-- 初始化温度告警
INSERT IGNORE INTO temperature_alert (id, port_id, reservation_id, vehicle_id, temperature, threshold, alert_level, status, auto_power_off) VALUES
(1, 13, 2, 3, 58.0, 55.0, 'DANGER', 'PENDING', 0);

-- 初始化账单数据
INSERT IGNORE INTO billing (id, user_id, reservation_id, charging_record_id, bill_type, amount, energy_consumed, price_per_kwh, service_fee, status, due_date) VALUES
(1, 1, NULL, NULL, 'CHARGING', 85.50, 45.0, 1.20, 22.50, 'UNPAID', DATE_ADD(CURDATE(), INTERVAL 7 DAY)),
(2, 2, NULL, NULL, 'CHARGING', 120.00, 60.0, 1.20, 48.00, 'UNPAID', DATE_ADD(CURDATE(), INTERVAL 7 DAY));
