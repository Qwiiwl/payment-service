
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


CREATE TABLE cards (
                       id BIGSERIAL PRIMARY KEY,
                       card_number VARCHAR(19) NOT NULL UNIQUE,
                       balance NUMERIC(19,2) NOT NULL CHECK (balance >= 0),
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                           CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED')),
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE fines (
                       id BIGSERIAL PRIMARY KEY,
                       fine_number VARCHAR(50) NOT NULL UNIQUE,
                       amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
                       paid BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       paid_at TIMESTAMP
);

CREATE TABLE phone_accounts (
                                id BIGSERIAL PRIMARY KEY,
                                phone_number VARCHAR(20) NOT NULL UNIQUE,
                                balance NUMERIC(19,2) NOT NULL CHECK (balance >= 0),
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions (
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


CREATE INDEX idx_transactions_created_at
    ON transactions(created_at);

CREATE INDEX idx_transactions_source
    ON transactions(source_identifier);

CREATE INDEX idx_transactions_type
    ON transactions(type);


INSERT INTO cards (card_number, balance, status) VALUES
                                                     ('8600123412341234', 1000.00, 'ACTIVE'),
                                                     ('8600123412345678', 500.00, 'ACTIVE'),
                                                     ('8600123412349999', 0.00, 'BLOCKED');

INSERT INTO fines (fine_number, amount, paid) VALUES
                                                  ('FINE-001', 150.00, FALSE),
                                                  ('FINE-002', 200.00, TRUE),
                                                  ('FINE-003', 50.00, FALSE);


INSERT INTO phone_accounts (phone_number, balance) VALUES
                                                       ('+998901234567', 50.00),
                                                       ('+998901234568', 100.00),
                                                       ('+998901234569', 0.00);

INSERT INTO transactions (transaction_id, type, source_identifier, destination_identifier, source_card_id, destination_card_id, amount, status)
VALUES
    (gen_random_uuid(), 'TRANSFER', '8600123412341234', '8600123412345678', 1, 2, 100.00, 'SUCCESS'),
    (gen_random_uuid(), 'FINE_PAYMENT', '8600123412341234', 'FINE-001', 1, NULL, 150.00, 'SUCCESS'),
    (gen_random_uuid(), 'PHONE_TOPUP', '8600123412345678', '+998901234567', 2, NULL, 20.00, 'SUCCESS');
