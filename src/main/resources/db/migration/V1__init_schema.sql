CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- CARDS
CREATE TABLE IF NOT EXISTS cards (
                                     id BIGSERIAL PRIMARY KEY,
                                     card_number VARCHAR(19) NOT NULL UNIQUE,
                                     balance NUMERIC(19,2) NOT NULL CHECK (balance >= 0),
                                     status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                                         CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED')),
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- FINES

CREATE TABLE IF NOT EXISTS fines (
                                     id BIGSERIAL PRIMARY KEY,
                                     fine_number VARCHAR(50) NOT NULL UNIQUE,
                                     amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
                                     paid BOOLEAN NOT NULL DEFAULT FALSE,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     paid_at TIMESTAMP
);

-- PHONE ACCOUNTS
CREATE TABLE IF NOT EXISTS phone_accounts (
                                              id BIGSERIAL PRIMARY KEY,
                                              phone_number VARCHAR(20) NOT NULL UNIQUE,
                                              balance NUMERIC(19,2) NOT NULL CHECK (balance >= 0),
                                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- TRANSACTIONS
CREATE TABLE IF NOT EXISTS transactions (
                                            id BIGSERIAL PRIMARY KEY,
                                            transaction_id UUID NOT NULL UNIQUE,
                                            type VARCHAR(50) NOT NULL
                                                CHECK (type IN ('TRANSFER', 'FINE_PAYMENT', 'PHONE_TOPUP')),
                                            source_identifier VARCHAR(50) NOT NULL,
                                            destination_identifier VARCHAR(50),
                                            source_card_id BIGINT,
                                            destination_card_id BIGINT,
                                            amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
                                            status VARCHAR(20) NOT NULL
                                                CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING')),
                                            error_message TEXT,
                                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            CONSTRAINT fk_transactions_source_card
                                                FOREIGN KEY (source_card_id)
                                                    REFERENCES cards(id)
                                                    ON DELETE RESTRICT,
                                            CONSTRAINT fk_transactions_destination_card
                                                FOREIGN KEY (destination_card_id)
                                                    REFERENCES cards(id)
                                                    ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_transactions_created_at
    ON transactions(created_at);

CREATE INDEX IF NOT EXISTS idx_transactions_source
    ON transactions(source_identifier);

CREATE INDEX IF NOT EXISTS idx_transactions_type
    ON transactions(type);


-- USERS (для регистрации/авторизации)
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     full_name VARCHAR(255) NOT NULL,
                                     phone_number VARCHAR(50) NOT NULL UNIQUE,
                                     password VARCHAR(255) NOT NULL,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- OTP CODES (и для карт, и для auth)
CREATE TABLE IF NOT EXISTS otp_codes (
                                         id BIGSERIAL PRIMARY KEY,
                                         user_id BIGINT NOT NULL,
                                         card_number VARCHAR(32), -- NULL для регистрации/логина
                                         code VARCHAR(6) NOT NULL,
                                         status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED')),
                                         expire_at TIMESTAMP NOT NULL,
                                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         CONSTRAINT fk_otp_user
                                             FOREIGN KEY (user_id)
                                                 REFERENCES users(id)
                                                 ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_otp_lookup
    ON otp_codes(user_id, card_number, status, created_at DESC);

-- SEED DATA
INSERT INTO cards (card_number, balance, status) VALUES
                                                     ('8600123412341234', 1000.00, 'ACTIVE'),
                                                     ('8600123412345678', 500.00, 'ACTIVE'),
                                                     ('8600123412349999', 0.00, 'BLOCKED')
ON CONFLICT (card_number) DO NOTHING;

INSERT INTO fines (fine_number, amount, paid) VALUES
                                                  ('FINE-001', 150.00, FALSE),
                                                  ('FINE-002', 200.00, TRUE),
                                                  ('FINE-003', 50.00, FALSE)
ON CONFLICT (fine_number) DO NOTHING;

INSERT INTO phone_accounts (phone_number, balance) VALUES
                                                       ('+998901234567', 50.00),
                                                       ('+998901234568', 100.00),
                                                       ('+998901234569', 0.00)
ON CONFLICT (phone_number) DO NOTHING;

INSERT INTO users (full_name, phone_number, password) VALUES
    ('Test User', '+998901234567', 'bcrypt_placeholder')
ON CONFLICT (phone_number) DO NOTHING;

INSERT INTO transactions (transaction_id, type, source_identifier, destination_identifier, source_card_id, destination_card_id, amount, status)
VALUES
    (gen_random_uuid(), 'TRANSFER', '8600123412341234', '8600123412345678', 1, 2, 100.00, 'SUCCESS'),
    (gen_random_uuid(), 'FINE_PAYMENT', '8600123412341234', 'FINE-001', 1, NULL, 150.00, 'SUCCESS'),
    (gen_random_uuid(), 'PHONE_TOPUP', '8600123412345678', '+998901234567', 2, NULL, 20.00, 'SUCCESS')
ON CONFLICT (transaction_id) DO NOTHING;