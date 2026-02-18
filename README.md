# Currency MT Parser (Spring Boot, Java)

Итоговое ДЗ по дисциплине «Теория и практика многопоточности».

Приложение собирает курсы валют с сайта ЦБ РФ, сохраняет данные в H2 и отдает результаты через REST API.

## Что реализовано по требованиям

- MVC backend на Spring Boot (Java 17).
- Многопоточность:
- `ExecutorService` (кастомный пул `currency-worker-*`) для конкурентных задач.
- `ScheduledExecutorService` (демон-поток `currency-scheduler-*`) для регулярного запуска сбора.
- Использованы `Thread`/`Runnable` (демон-потоки: consumer очереди и логгер очереди).
- Использованы потокобезопасные структуры: `BlockingQueue`, `ConcurrentHashMap`.
- Использованы примитивы синхронизации: `CountDownLatch`, `ReentrantLock`.
- Межсервисное взаимодействие: `WebClient` (webflux) + `RestTemplate`.
- Хранилище: встроенная БД H2.
- Получение данных через endpoint `/api/v1/answer`.
- Endpoint на конкретную дату и валюту: `/api/v1/answer/by-date`.
- Сортировка результатов из H2 через `parallelStream()`.
- Unit-тесты добавлены.

## Запуск (вариант 1: локально)

1. Открыть проект в IntelliJ IDEA Community.
2. Запустить команду в терминале проекта:

```bash
./gradlew bootRun
```

3. Дождаться старта приложения на `http://localhost:8080`.

## Запуск (вариант 2: Docker)

```bash
docker compose up --build
```

После старта сервис доступен на `http://localhost:8080`.

## Проверка логики

1. Запустить сбор вручную (можно без даты, тогда берется текущая):

```bash
curl -X POST "http://localhost:8080/api/v1/collect?date=2025-11-11"
```

2. Получить список курсов за дату:

```bash
curl "http://localhost:8080/api/v1/answer?date=2025-11-11&sortBy=rateToRub&direction=desc"
```

3. Получить курс конкретной валюты за дату:

```bash
curl "http://localhost:8080/api/v1/answer/by-date?date=2025-11-11&currency=USD"
```

4. Проверить статус последнего сбора:

```bash
curl "http://localhost:8080/api/v1/status"
```

## Тесты

```bash
./gradlew test
```

## H2-консоль

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:ratesdb`
- user: `sa`
- password: *(пусто)*
