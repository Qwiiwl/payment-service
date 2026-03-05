# Payment Service (Spring Boot)

> **Примечание:** подключение к **Kafka** временно не работает.  
> Блоки с вызовами Kafka закомментированы, чтобы проект **компилировался и запускался** без ошибок.  
> Работа с Kafka будет восстановлена позже.

## Base URL

В проекте задан `server.servlet.context-path: /api/v1`, поэтому все запросы идут через:

- `http://localhost:8080/api/v1`

## Быстрый старт

1) Подними PostgreSQL и создай БД `payment_db`  
2) docker exec -it payment-postgres psql -U arvi -d payment_db
3) Проверь настройки в `src/main/resources/application.yml` (url/username/password)  
4) Запуск:

```bash
./gradlew bootRun
```

> Если Flyway ругается на **checksum mismatch**, значит миграция была изменена после применения.  
> Для локальной разработки проще всего пересоздать схему/БД или выполнить `flywayRepair`.

---

## Curl-проверки

Ниже примеры запросов **с корректными путями** согласно контроллерам проекта.

### 1) Регистрация (отправка OTP)

`POST /auth/register`

```bash
curl -i -X POST "http://localhost:8080/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test_User",
    "phoneNumber": "+998911234567",
    "password": "qwerty123",
    "email": "dq2qdasdasfefdgfdv@gmail.com"
  }'
```

### 2) Подтверждение OTP (для пользователя)

`POST /auth/confirm`

```bash
curl -i -X POST "http://localhost:8080/api/v1/auth/confirm" \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+998901234567",
    "code": "1234"
  }'
```

### 3) Логин (отправка OTP)

`POST /auth/login`

```bash
curl -i -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+998901234567",
    "password": "qwerty123",
    "email": "attentiondor@gmail.com"
  }'
```

> Обычно после login нужно снова подтвердить OTP через `/auth/confirm`.

---

### 4) Инициировать добавление карты (отправка OTP)

`POST /cards/add`

```bash
curl -i -X POST "http://localhost:8080/api/v1/cards/add" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cardNumber": "8600123412344321"
  }'
```

### 5) Подтвердить добавление карты (OTP + финальное создание карты)

`POST /cards/confirm`

```bash
curl -i -X POST "http://localhost:8080/api/v1/cards/confirm" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cardNumber": "8600123412344321",
    "otpCode": "1234"
  }'
```

---

### 6) Перевод между картами

`POST /transfer`

```bash
curl -i -X POST "http://localhost:8080/api/v1/transfer" \
  -H "Content-Type: application/json" \
  -d '{
    "fromCard": "8600123412344321",
    "toCard": "8600123412349999",
    "amount": 10
  }'
```

**Недостаточно средств (пример):**

```bash
curl -i -X POST "http://localhost:8080/api/v1/transfer" \
  -H "Content-Type: application/json" \
  -d '{
    "fromCard": "8600123412344321",
    "toCard": "8600123412349999",
    "amount": 999999
  }'
```

---

### 7) Оплата штрафа

`POST /fine-payment`

```bash
curl -i -X POST "http://localhost:8080/api/v1/fine-payment" \
  -H "Content-Type: application/json" \
  -d '{
    "fromCard": "8600123412344321",
    "fineId": 15,
    "amount": 50000
  }'
```

---

### 8) Пополнение телефона

`POST /phone-topup`

```bash
curl -i -X POST "http://localhost:8080/api/v1/phone-topup" \
  -H "Content-Type: application/json" \
  -d '{
    "fromCard": "8600123412344321",
    "phoneNumber": "+998901234567",
    "amount": 20000
  }'
```

---

### 9) История по карте

`GET /transactions/history`

Минимально:

```bash
curl -s "http://localhost:8080/api/v1/transactions/history?cardNumber=8600123412344321"
```

**Пагинация:**

```bash
curl -s "http://localhost:8080/api/v1/transactions/history?cardNumber=8600123412344321&page=0&size=5"
```

**Фильтр по типу:**

Типы: `PHONE_TOPUP | TRANSFER | FINE_PAYMENT`

```bash
curl -s "http://localhost:8080/api/v1/transactions/history?cardNumber=8600123412344321&type=TRANSFER"
```

**Сортировка по дате:**

```bash
curl -s "http://localhost:8080/api/v1/transactions/history?cardNumber=8600123412344321&sort=createdAt,asc"
```

**Фильтр + пагинация + сортировка:**

```bash
curl -s "http://localhost:8080/api/v1/transactions/history?cardNumber=8600123412344321&type=TRANSFER&page=0&size=3&sort=createdAt,desc"
```

---

### 10) Проверка параллельности (2 запроса одновременно)

```bash
bash -c '
curl -s -X POST "http://localhost:8080/api/v1/transfer" -H "Content-Type: application/json" -d "{\"fromCard\":\"8600123412344321\",\"toCard\":\"8600123412349999\",\"amount\":600}" &
curl -s -X POST "http://localhost:8080/api/v1/transfer" -H "Content-Type: application/json" -d "{\"fromCard\":\"8600123412344321\",\"toCard\":\"8600123412349999\",\"amount\":600}" &
wait
echo
'
```

---

## Проверки в БД

Последние транзакции:

```sql
SELECT id, transaction_id, status, error_message, amount, created_at
FROM transactions
ORDER BY id DESC
LIMIT 10;
```
