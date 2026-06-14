-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20) UNIQUE,
    id_card VARCHAR(18),
    role VARCHAR(20) NOT NULL COMMENT 'RESIDENT-居民, PROPERTY-物业, SAFETY_OFFICER-安全员',
    balance DECIMAL(10,2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE-正常, FROZEN-冻结',
    is_verified TINYINT DEFAULT 0 COMMENT '0-未实名认证, 1-已实名认证',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 车辆表
CREATE TABLE IF NOT EXISTS vehicle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plate_number VARCHAR(20) UNIQUE COMMENT '车牌号',
    vehicle_type VARCHAR(50) COMMENT '车辆型号',
    battery_brand VARCHAR(50) COMMENT '电池品牌',
    battery_capacity DECIMAL(5,2) COMMENT '电池容量kWh',
    is_verified TINYINT DEFAULT 0 COMMENT '0-未实名, 1-已实名',
    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL-正常, ABNORMAL-异常, FROZEN-冻结',
    frozen_reason VARCHAR(255) COMMENT '冻结原因',
    frozen_at DATETIME COMMENT '冻结时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    INDEX idx_user_id (user_id),
    INDEX idx_plate_number (plate_number),
    INDEX idx_battery_brand (battery_brand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 车棚表
CREATE TABLE IF NOT EXISTS charging_shed (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shed_name VARCHAR(100) NOT NULL,
    location VARCHAR(255) NOT NULL,
    total_ports INT DEFAULT 0,
    available_ports INT DEFAULT 0,
    max_power_limit DECIMAL(10,2) DEFAULT 100.00 COMMENT '车棚总功率上限kW',
    current_total_power DECIMAL(10,2) DEFAULT 0.00 COMMENT '当前总使用功率kW',
    status VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN-营业中, CLOSED-关闭, MAINTENANCE-维护中',
    manager_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (manager_id) REFERENCES sys_user(id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 充电位表
CREATE TABLE IF NOT EXISTS charging_port (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shed_id BIGINT NOT NULL,
    port_code VARCHAR(50) NOT NULL,
    port_type VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL-普通, FAST-快充',
    power_rating DECIMAL(5,2) DEFAULT 7.0 COMMENT '额定功率kW',
    status VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE-可用, OCCUPIED-占用, MAINTENANCE-维护, FAULT-故障',
    current_temperature DECIMAL(5,2) DEFAULT 25.0,
    is_power_on TINYINT DEFAULT 1 COMMENT '0-断电, 1-通电',
    current_reservation_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (shed_id) REFERENCES charging_shed(id),
    UNIQUE KEY uk_shed_port (shed_id, port_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预约表
CREATE TABLE IF NOT EXISTS reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    port_id BIGINT NOT NULL,
    shed_id BIGINT NOT NULL,
    reserve_start_time DATETIME NOT NULL,
    reserve_end_time DATETIME NOT NULL,
    actual_start_time DATETIME,
    actual_end_time DATETIME,
    queue_position INT COMMENT '排队位置',
    scheduled_start_time DATETIME COMMENT '预计开始时间',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING-待确认, CONFIRMED-已确认, IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消, EXPIRED-已过期, QUEUED-排队中',
    cancel_reason VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (port_id) REFERENCES charging_port(id),
    FOREIGN KEY (shed_id) REFERENCES charging_shed(id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_time (reserve_start_time, reserve_end_time),
    INDEX idx_queue (shed_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 充电记录表
CREATE TABLE IF NOT EXISTS charging_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    port_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    start_soc DECIMAL(5,2) COMMENT '开始电量%',
    end_soc DECIMAL(5,2) COMMENT '结束电量%',
    energy_consumed DECIMAL(8,2) DEFAULT 0.00 COMMENT '耗电量kWh',
    max_temperature DECIMAL(5,2) COMMENT '最高温度',
    avg_temperature DECIMAL(5,2) COMMENT '平均温度',
    status VARCHAR(20) DEFAULT 'CHARGING' COMMENT 'CHARGING-充电中, COMPLETED-完成, INTERRUPTED-中断, EMERGENCY_STOP-紧急停止',
    stop_reason VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (reservation_id) REFERENCES reservation(id),
    FOREIGN KEY (port_id) REFERENCES charging_port(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    INDEX idx_reservation_id (reservation_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 温度告警表
CREATE TABLE IF NOT EXISTS temperature_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    port_id BIGINT NOT NULL,
    reservation_id BIGINT,
    vehicle_id BIGINT,
    temperature DECIMAL(5,2) NOT NULL,
    threshold DECIMAL(5,2) NOT NULL,
    alert_level VARCHAR(20) DEFAULT 'WARNING' COMMENT 'WARNING-警告, DANGER-危险',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING-待处理, PROCESSING-处理中, RESOLVED-已解决',
    handled_by BIGINT,
    handled_at DATETIME,
    handle_result VARCHAR(255),
    auto_power_off TINYINT DEFAULT 0 COMMENT '0-未自动断电, 1-已自动断电',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (port_id) REFERENCES charging_port(id),
    FOREIGN KEY (reservation_id) REFERENCES reservation(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (handled_by) REFERENCES sys_user(id),
    INDEX idx_status (status),
    INDEX idx_port_id (port_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 计费规则表
CREATE TABLE IF NOT EXISTS pricing_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    shed_id BIGINT,
    community_id VARCHAR(50) COMMENT '小区ID',
    price_per_kwh DECIMAL(8,4) NOT NULL COMMENT '电价元/kWh',
    service_fee DECIMAL(8,4) DEFAULT 0.00 COMMENT '服务费元/kWh',
    free_minutes INT DEFAULT 0 COMMENT '免费充电时长分钟',
    peak_start_time TIME,
    peak_end_time TIME,
    peak_price_multiplier DECIMAL(4,2) DEFAULT 1.5,
    valley_start_time TIME,
    valley_end_time TIME,
    valley_price_multiplier DECIMAL(4,2) DEFAULT 0.5,
    flat_price_multiplier DECIMAL(4,2) DEFAULT 1.0 COMMENT '平时段倍率',
    is_default TINYINT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (shed_id) REFERENCES charging_shed(id),
    INDEX idx_shed_id (shed_id),
    INDEX idx_community_id (community_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 黑名单电池品牌表
CREATE TABLE IF NOT EXISTS battery_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_name VARCHAR(50) NOT NULL UNIQUE COMMENT '电池品牌名称',
    ban_reason VARCHAR(255) COMMENT '禁用原因',
    banned_by BIGINT COMMENT '操作人',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE-生效, INACTIVE-失效',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (banned_by) REFERENCES sys_user(id),
    INDEX idx_brand_name (brand_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 安全告警表（扩展温度告警，支持多种告警类型）
CREATE TABLE IF NOT EXISTS safety_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    port_id BIGINT NOT NULL,
    reservation_id BIGINT,
    vehicle_id BIGINT,
    alert_type VARCHAR(30) NOT NULL COMMENT 'TEMPERATURE-温度告警, SMOKE-烟感告警, OCCUPANCY-长时间占位, OVERPOWER-功率过载',
    alert_level VARCHAR(20) DEFAULT 'WARNING' COMMENT 'WARNING-警告, DANGER-危险',
    alert_value DECIMAL(10,2) COMMENT '告警值',
    threshold DECIMAL(10,2) COMMENT '阈值',
    description VARCHAR(255) COMMENT '告警描述',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING-待处理, PROCESSING-处理中, RESOLVED-已解决',
    handled_by BIGINT,
    handled_at DATETIME,
    handle_result VARCHAR(255),
    auto_power_off TINYINT DEFAULT 0 COMMENT '0-未自动断电, 1-已自动断电',
    remark VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (port_id) REFERENCES charging_port(id),
    FOREIGN KEY (reservation_id) REFERENCES reservation(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (handled_by) REFERENCES sys_user(id),
    INDEX idx_status (status),
    INDEX idx_port_id (port_id),
    INDEX idx_alert_type (alert_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 断电记录表
CREATE TABLE IF NOT EXISTS power_off_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    port_id BIGINT NOT NULL,
    reservation_id BIGINT,
    vehicle_id BIGINT,
    user_id BIGINT,
    alert_id BIGINT COMMENT '关联告警ID',
    power_off_type VARCHAR(30) NOT NULL COMMENT 'AUTO-自动断电, MANUAL-人工断电, EMERGENCY-紧急断电',
    reason VARCHAR(255) NOT NULL COMMENT '断电原因',
    operator_id BIGINT COMMENT '操作人ID（人工断电时）',
    power_off_time DATETIME NOT NULL,
    power_on_time DATETIME COMMENT '恢复通电时间',
    status VARCHAR(20) DEFAULT 'POWER_OFF' COMMENT 'POWER_OFF-已断电, POWER_ON-已恢复',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (port_id) REFERENCES charging_port(id),
    FOREIGN KEY (reservation_id) REFERENCES reservation(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (alert_id) REFERENCES safety_alert(id),
    FOREIGN KEY (operator_id) REFERENCES sys_user(id),
    INDEX idx_port_id (port_id),
    INDEX idx_status (status),
    INDEX idx_power_off_time (power_off_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 复核记录表
CREATE TABLE IF NOT EXISTS review_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    alert_id BIGINT COMMENT '关联告警ID',
    power_off_id BIGINT COMMENT '关联断电记录ID',
    review_type VARCHAR(30) NOT NULL COMMENT 'VEHICLE_UNFREEZE-车辆解冻复核, CHARGE_RESUME-恢复充电复核',
    review_result VARCHAR(20) NOT NULL COMMENT 'APPROVED-通过, REJECTED-驳回',
    reviewer_id BIGINT NOT NULL COMMENT '复核人ID',
    review_remark VARCHAR(500) COMMENT '复核意见',
    reviewed_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (alert_id) REFERENCES safety_alert(id),
    FOREIGN KEY (power_off_id) REFERENCES power_off_record(id),
    FOREIGN KEY (reviewer_id) REFERENCES sys_user(id),
    INDEX idx_vehicle_id (vehicle_id),
    INDEX idx_review_result (review_result),
    INDEX idx_reviewed_at (reviewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 烟感设备表
CREATE TABLE IF NOT EXISTS smoke_detector (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    port_id BIGINT NOT NULL,
    device_code VARCHAR(50) NOT NULL UNIQUE COMMENT '设备编号',
    location VARCHAR(100) COMMENT '安装位置',
    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL-正常, FAULT-故障, ALARM-告警',
    smoke_level DECIMAL(5,2) DEFAULT 0.00 COMMENT '烟雾浓度%',
    last_check_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (port_id) REFERENCES charging_port(id),
    INDEX idx_port_id (port_id),
    INDEX idx_device_code (device_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 账单表
CREATE TABLE IF NOT EXISTS billing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reservation_id BIGINT,
    charging_record_id BIGINT,
    bill_type VARCHAR(20) DEFAULT 'CHARGING' COMMENT 'CHARGING-充电费, SERVICE-服务费, OTHER-其他',
    amount DECIMAL(10,2) NOT NULL,
    energy_consumed DECIMAL(8,2),
    price_per_kwh DECIMAL(8,4),
    service_fee DECIMAL(10,2) DEFAULT 0.00,
    peak_energy DECIMAL(8,2) DEFAULT 0.00 COMMENT '峰时电量kWh',
    valley_energy DECIMAL(8,2) DEFAULT 0.00 COMMENT '谷时电量kWh',
    flat_energy DECIMAL(8,2) DEFAULT 0.00 COMMENT '平时电量kWh',
    peak_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '峰时电费',
    valley_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '谷时电费',
    flat_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '平时电费',
    free_minutes INT DEFAULT 0 COMMENT '减免分钟数',
    status VARCHAR(20) DEFAULT 'UNPAID' COMMENT 'UNPAID-未支付, PAID-已支付, OVERDUE-已逾期, CANCELLED-已取消',
    due_date DATE,
    paid_at DATETIME,
    payment_method VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (reservation_id) REFERENCES reservation(id),
    FOREIGN KEY (charging_record_id) REFERENCES charging_record(id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 缴费记录表
CREATE TABLE IF NOT EXISTS payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    billing_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100),
    status VARCHAR(20) DEFAULT 'SUCCESS',
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (billing_id) REFERENCES billing(id),
    INDEX idx_user_id (user_id),
    INDEX idx_billing_id (billing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 温度历史记录表
CREATE TABLE IF NOT EXISTS temperature_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    port_id BIGINT NOT NULL,
    reservation_id BIGINT,
    temperature DECIMAL(5,2) NOT NULL,
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (port_id) REFERENCES charging_port(id),
    FOREIGN KEY (reservation_id) REFERENCES reservation(id),
    INDEX idx_port_time (port_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
