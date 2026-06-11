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
    battery_capacity DECIMAL(5,2) COMMENT '电池容量kWh',
    is_verified TINYINT DEFAULT 0 COMMENT '0-未实名, 1-已实名',
    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL-正常, ABNORMAL-异常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    INDEX idx_user_id (user_id),
    INDEX idx_plate_number (plate_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 车棚表
CREATE TABLE IF NOT EXISTS charging_shed (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shed_name VARCHAR(100) NOT NULL,
    location VARCHAR(255) NOT NULL,
    total_ports INT DEFAULT 0,
    available_ports INT DEFAULT 0,
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
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING-待确认, CONFIRMED-已确认, IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消, EXPIRED-已过期',
    cancel_reason VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (port_id) REFERENCES charging_port(id),
    FOREIGN KEY (shed_id) REFERENCES charging_shed(id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_time (reserve_start_time, reserve_end_time)
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
    price_per_kwh DECIMAL(8,4) NOT NULL COMMENT '电价元/kWh',
    service_fee DECIMAL(8,4) DEFAULT 0.00 COMMENT '服务费元/kWh',
    peak_start_time TIME,
    peak_end_time TIME,
    peak_price_multiplier DECIMAL(4,2) DEFAULT 1.5,
    valley_start_time TIME,
    valley_end_time TIME,
    valley_price_multiplier DECIMAL(4,2) DEFAULT 0.5,
    is_default TINYINT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (shed_id) REFERENCES charging_shed(id),
    INDEX idx_shed_id (shed_id)
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
    status VARCHAR(20) DEFAULT 'UNPAID' COMMENT 'UNPAID-未支付, PAID-已支付, OVERDUE-已逾期',
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
