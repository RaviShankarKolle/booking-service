CREATE TABLE IF NOT EXISTS fines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    borrow_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    last_calculated_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_fines_user_status (user_id, status),
    INDEX idx_fines_borrow (borrow_id)
);
