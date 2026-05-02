# Taxi Microservices

Backend demo for a taxi ordering platform. The project is split into three Spring Boot services:

- `user-service` on port `8081`: passengers, drivers, driver status, Redis cache for available drivers.
- `trip-service` on port `8082`: trip creation, atomic driver assignment through User Service, pricing, rating, statistics.
- `notification-service` on port `8083`: DB-backed notification queue processed by a worker pool.

Infrastructure is started by Docker Compose:

- PostgreSQL stores passengers, drivers, trips and notification tasks.
- Redis caches the set of available driver ids.

## Run

```bash
docker compose up --build
```

All services use the same JWT secret from `docker-compose.yml`. Get a token from User Service:

```bash
curl -X POST http://localhost:8081/auth/token ^
  -H "Content-Type: application/json" ^
  -d "{\"subject\":\"demo\",\"role\":\"USER\"}"
```

Use the returned value as `Authorization: Bearer <token>` for protected endpoints.

## Example Flow

Create a passenger:

```bash
curl -X POST http://localhost:8081/passengers ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Alice\",\"email\":\"alice@example.com\",\"phone\":\"+70000000001\"}"
```

Create a driver:

```bash
curl -X POST http://localhost:8081/drivers ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Bob\",\"email\":\"bob@example.com\",\"phone\":\"+70000000002\",\"licenseNumber\":\"A123BC\"}"
```

Create a trip:

```bash
curl -X POST http://localhost:8082/trips ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer <token>" ^
  -d "{\"passengerId\":1,\"origin\":\"Lenina 1\",\"destination\":\"Airport\",\"distanceKm\":12.5}"
```

Update status:

```bash
curl -X PATCH http://localhost:8082/trips/1/status ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer <token>" ^
  -d "{\"status\":\"COMPLETED\"}"
```

Rate the completed trip:

```bash
curl -X PATCH http://localhost:8082/trips/1/rating ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer <token>" ^
  -d "{\"rating\":5}"
```

Get notifications for a trip:

```bash
curl "http://localhost:8083/notifications?trip_id=1" ^
  -H "Authorization: Bearer <token>"
```

Get daily statistics:

```bash
curl "http://localhost:8082/trips/statistics?date=2026-05-02" ^
  -H "Authorization: Bearer <token>"
```

## Concurrency Notes

Driver allocation is atomic in User Service:

```sql
SELECT id FROM drivers
WHERE status = 'AVAILABLE'
ORDER BY id
FOR UPDATE SKIP LOCKED
LIMIT 1
```

Notification workers use the same idea when claiming `PENDING` tasks. Each worker updates one task to `PROCESSING` before sending, so two threads cannot process the same row concurrently. On shutdown the pool stops accepting new work, waits up to 10 seconds, then interrupts the remaining workers.
