# Universal ISUP Server

A production-grade Spring Boot + Netty server implementing the Hikvision ISUP v5.0 protocol for face terminals and access control devices. Receives attendance events, stores them in PostgreSQL, and dispatches webhook notifications to configured projects.

---

## Features

- **ISUP v5.0 TCP Protocol** - Netty-based server accepting connections from Hikvision face terminals
- **SADP Discovery** - UDP broadcast discovery of devices on the local network
- **Multi-project Webhooks** - Route events to multiple project endpoints with HMAC-SHA256 signatures
- **REST API** - Manage devices, projects, and query events
- **WebSocket** - Live event feed at `/ws/events`
- **Security** - IP ban manager, login timeout, rate limiting, CORS configuration
- **Circuit Breaker** - Per-project circuit breaker for webhook reliability
- **Event Buffering** - In-memory buffer when DB is temporarily unavailable
- **Metrics** - Prometheus-compatible `/metrics` endpoint
- **Health Check** - `/health` endpoint with pool stats and uptime
- **Telegram Alerts** - Optional alerting for critical events
- **Structured Logging** - JSON logging in prod profile, human-readable in dev
- **Graceful Shutdown** - Sends LOGOUT to all devices before stopping
- **Auto DB Backup** - Daily PostgreSQL dumps with 7-day retention

---

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Ports available: `7660` (TCP), `37020/udp` (SADP), `8090` (API)

### 1. Configure environment

```bash
cp .env.example .env
# Edit .env and set:
#   ADMIN_SECRET=your-strong-secret
#   POSTGRES_PASSWORD=your-db-password
```

### 2. Start with Docker

```bash
docker compose up -d
```

### 3. Verify

```bash
curl http://localhost:8090/health
curl http://localhost:8090/metrics
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `POSTGRES_DB` | `isup_db` | PostgreSQL database name |
| `POSTGRES_USER` | `isup` | PostgreSQL username |
| `POSTGRES_PASSWORD` | `isup_pass` | PostgreSQL password |
| `ISUP_TCP_PORT` | `7660` | TCP port for device connections |
| `SADP_UDP_PORT` | `37020` | UDP port for SADP discovery |
| `API_PORT` | `8090` | REST API port |
| `ALLOW_UNKNOWN_DEVICES` | `true` | Accept devices not in DB |
| `ADMIN_SECRET` | `changeme123` | Admin API secret key (change in production!) |
| `CORS_ALLOWED_ORIGINS` | `*` | Allowed CORS origins (comma-separated or `*`) |
| `ALERT_TELEGRAM_TOKEN` | *(empty)* | Telegram bot token for alerts |
| `ALERT_CHAT_ID` | *(empty)* | Telegram chat ID for alerts |
| `EVENT_RETENTION_DAYS` | `90` | Days to keep events before cleanup |
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring profile (`prod` for JSON logs) |

---

## API Reference

### Health & Monitoring

| Method | Path | Description |
|---|---|---|
| GET | `/health` | Health status with DB pool and device stats |
| GET | `/metrics` | Prometheus text format metrics |

### Devices

| Method | Path | Description |
|---|---|---|
| GET | `/api/devices` | List all devices |
| POST | `/api/devices` | Register a new device |
| PUT | `/api/devices/{id}` | Update device |
| DELETE | `/api/devices/{id}` | Delete device |
| GET | `/api/devices/online` | List online devices |

### Events

| Method | Path | Description |
|---|---|---|
| GET | `/api/events` | List events (paginated) |
| GET | `/api/events?deviceId=X` | Filter by device |
| GET | `/api/events/recent` | Last 10 events |
| GET | `/api/events/today/count` | Event count today |

### Projects

| Method | Path | Description |
|---|---|---|
| GET | `/api/projects` | List all projects |
| POST | `/api/projects` | Create project |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project |

### Discovery

| Method | Path | Description |
|---|---|---|
| GET | `/api/discovery` | List discovered devices |
| POST | `/api/discovery/scan` | Trigger SADP scan |

---

## Architecture

```
Hikvision Device
      │ TCP :7660
      ▼
 Netty Pipeline
  ├── BanCheck (IpBanManager)
  ├── LoginTimeoutHandler (10s)
  ├── IdleStateHandler (120s)
  ├── IsupFrameDecoder
  └── IsupMessageHandler
         │
         ├── SessionRegistry (online devices)
         ├── DeviceService (DB device management)
         └── EventService
                ├── EventLogRepository (PostgreSQL)
                ├── EventBuffer (in-memory fallback)
                ├── WebSocket push (/topic/events)
                └── WebhookDispatcher
                       ├── CircuitBreaker (per project)
                       └── HTTP POST to project webhook URL
```

---

## Security

### IP Ban Manager

- 5 failed login attempts within 60 seconds → auto-ban for 1 hour
- Banned IPs are rejected immediately at connection time
- Manual ban/unban available via `IpBanManager` bean
- Expired bans cleaned every 10 minutes

### Login Timeout

- Devices that don't send `LOGIN_REQUEST` within 10 seconds are disconnected

### Rate Limiting

- 100 requests per IP per 60 seconds on all API endpoints
- Returns HTTP 429 when exceeded

### Internal Endpoints

- `/internal/**` accessible only from `127.0.0.1` and `::1`

---

## Webhook Payload

Each event is delivered as a JSON POST with an HMAC-SHA256 signature in the `X-ISUP-Signature` header:

```json
{
  "event_id": "abc123",
  "event_type": "attendance",
  "device_id": "K8500000001",
  "device_name": "Front Door",
  "employee_no": "12345",
  "employee_name": "John Doe",
  "verify_mode": "face",
  "direction": "in",
  "event_time": "2024-01-15T08:30:00Z",
  "timestamp": "2024-01-15T08:30:01Z"
}
```

Verify signature: `sha256=HMAC-SHA256(secret_key, request_body)`

### Circuit Breaker

- 5 consecutive failures → circuit OPEN (webhooks skip for 5 minutes)
- After 5 minutes → HALF_OPEN (one test request)
- Success → CLOSED; failure → OPEN again

---

## Deployment

### VPS Deploy Script

```bash
chmod +x deploy.sh

# Deploy to server
./deploy.sh root@YOUR_SERVER_IP

# Deploy with custom .env
./deploy.sh root@YOUR_SERVER_IP --env /path/to/production.env
```

The script:
1. Checks SSH connectivity
2. Installs Docker if not present (Ubuntu 22.04)
3. Creates `/opt/isup-server` on remote
4. Syncs files via rsync (excluding build artifacts)
5. Sets up `.env` on remote
6. Runs `docker compose up -d`
7. Waits for health check to pass
8. Shows final status

### Manual Docker Deploy

```bash
# Build and start
docker compose up -d --build

# View logs
docker compose logs -f isup-server

# Stop
docker compose down

# Update
git pull && docker compose up -d --build
```

---

## Database Backups

Daily automated backups run at 03:00 via the `db-backup` container:

- Stored in `./backups/backup_YYYYMMDD_HHMMSS.sql`
- Backups older than 7 days are automatically deleted

Manual backup:

```bash
docker compose exec postgres pg_dump -U isup isup_db > backups/manual_$(date +%Y%m%d).sql
```

Restore:

```bash
docker compose exec -T postgres psql -U isup isup_db < backups/backup_20240115_030000.sql
```

---

## Logging

- **Development** (`!prod`): Human-readable colored console output
- **Production** (`prod`): Structured JSON per line for log aggregation (Loki, ELK, etc.)

```bash
# Change profile in .env:
SPRING_PROFILES_ACTIVE=prod    # JSON logs
SPRING_PROFILES_ACTIVE=default # Human-readable logs
```

---

## Database Schema

- `projects` - Webhook destinations with secret keys
- `devices` - Known devices with password hashes and project assignment
- `event_logs` - All received attendance/alarm events with webhook delivery status
- `discovered_devices` - SADP-discovered devices not yet registered

---

## Telegram Alerts

Set `ALERT_TELEGRAM_TOKEN` and `ALERT_CHAT_ID` in `.env` to enable alerts for:

- Server started/stopped
- Device offline
- Webhook failure (exhausted retries)
- Database disconnection
- High memory usage

---

## License

MIT License
