**Примечание:** Подключение к Kafka временно не работает
Все блоки с вызовами Kafka закомментированы, чтобы проект компилировался и запускался без ошибок
Работа с Kafka будет восстановлена потом



**Curl-проверки:**

*Регистрация*
curl -i -X POST http://localhost:8080/auth/register \
-H "Content-Type: application/json" \
-d '{
"fullName": "your_name",
"phoneNumber": "your_number",
"password": "your_password"
}'
*Логин*
curl -i -X POST http://localhost:8080/auth/login \
-H "Content-Type: application/json" \
-d '{
"phoneNumber": "your_number",
"password": "your_password"
}'

*Создание карты*
curl -X POST "http://localhost:8080/transfer/cards/test?number=8600123412344321&balance=1000"


*Получить список всех карт*
curl http://localhost:8080/transfer/cards/test


*Перевод*
curl -i -X POST http://localhost:8080/transfer \
-H "Content-Type: application/json" \
-d '{
"fromCard": "your_card",
"toCard": "receiver_card",
"amount": 10
}'

*Недостаточно средств*
curl -i -X POST http://localhost:8080/transfer \
-H "Content-Type: application/json" \
-d '{
"fromCard": "your_card",
"toCard": "receiver_card",
"amount": 999999
}'


*Оплата штрафа*
curl -i -X POST http://localhost:8080/payments/fine \
-H "Content-Type: application/json" \
-d '{
"cardNumber": "your_card",
"fineId": "FINE-number",
"amount": your_amount
}'


*Пополнение телефона*
curl -i -X POST http://localhost:8080/payments/phone \
-H "Content-Type: application/json" \
-d '{
"cardNumber": "your_card",
"phoneNumber": "phone_number",
"amount": your_amount
}'


*История по карте*
curl -s "http://localhost:8080/transactions/history?cardNumber=8600123412341234"


*Пагинация*
curl -s "http://localhost:8080/transactions/history?cardNumber=8600123412341234&page=0&size=5"


*Фильтр по типу*
curl -s "http://localhost:8080/transactions/history?cardNumber=8600123412341234&type=YOUR_TYPE" // Type - PHONE_TOPUP | TRANSFER | FINE_PAYMENT


*Сортировка по дате*
curl -s "http://localhost:8080/transactions/history?cardNumber=8600123412341234&sort=createdAt,asc"


*Фильтр + пагинация + сортировка*
curl -s "http://localhost:8080/transactions/history?cardNumber=8600123412341234&type=TRANSFER&page=0&size=3&sort=createdAt,desc"


*Проверка параллельности*
bash -c '
curl -s -X POST http://localhost:8080/transfer -H "Content-Type: application/json" -d "{\"fromCard\":\"your_card\",\"toCard\":\"receiver_card\",\"amount\":600}" &
curl -s -X POST http://localhost:8080/transfer -H "Content-Type: application/json" -d "{\"fromCard\":\"your_card\",\"toCard\":\"receiver_card\",\"amount\":600}" &
wait
echo
'



**Проверки в БД**
SELECT id, transaction_id, status, error_message, amount, created_at
FROM transactions
ORDER BY id DESC
LIMIT 10;