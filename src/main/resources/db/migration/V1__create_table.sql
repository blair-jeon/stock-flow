CREATE TABLE users(
    id BIGINT NOT NULL,
    login_id VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('USER', 'SELLER', 'ADMIN') NOT NULL,
    status ENUM('ACTIVE', 'SUSPENDED') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_login_id UNIQUE (login_id)
) ENGINE=InnoDB;

CREATE TABLE users_seq(
    next_val BIGINT
) ENGINE=InnoDB;

CREATE TABLE user_profiles(
    user_id BIGINT NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    PRIMARY KEY(user_id)
) ENGINE=InnoDB;

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    status ENUM('ACTIVE', 'HIDDEN', 'SUSPENDED') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE inventories (
    product_id BIGINT NOT NULL,
    stock_quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (product_id)
) ENGINE=InnoDB;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status ENUM(
        'CREATED', 'CONFIRMED', 'SHIPPED', 'COMPLETED', 'CANCELLED'
        ) NOT NULL,
        amount DECIMAL(38, 2) NOT NULL,
        name VARCHAR(255) NOT NULL,
        line_count INT NOT NULL,
        created_at DATETIME(6) NOT NULL,
        updated_at DATETIME(6) NOT NULL,
        PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(38, 2) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_order_items_order_id (order_id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;