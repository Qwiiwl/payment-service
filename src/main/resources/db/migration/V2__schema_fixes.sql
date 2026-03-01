-- Добавляем зарезервированный баланс на случай если операция пойдет не по плану
ALTER TABLE cards
    ADD COLUMN IF NOT EXISTS reserved_balance NUMERIC(19,2) NOT NULL DEFAULT 0.00
    CHECK (reserved_balance >= 0);


-- Не внедрил мыло на первой версии, поэтому добавляем тут
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- Заполняем мыла для уже существующих юзеров
UPDATE users
SET email = COALESCE(email, 'attentiondor@gmail.com')
WHERE email IS NULL;

-- Делаем email обязательным
ALTER TABLE users
    ALTER COLUMN email SET NOT NULL;

-- Добавляем UNIQUE на email не создаём повторно, если constraint уже существует
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   pg_constraint
        WHERE  conname = 'uk_users_email'
    ) THEN
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
END IF;
END$$;