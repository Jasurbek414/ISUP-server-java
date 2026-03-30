# ISUP Server — To'liq Texnik Hujjat

**Versiya**: 2.0
**Sana**: 2026-yil mart
**Platforma**: Java 17 + Spring Boot 3.2 + Netty + PostgreSQL
**Manzil**: https://fake-faceid.uzinc.uz

---

## MUNDARIJA

1. [Tizim haqida](#1-tizim-haqida)
2. [Arxitektura](#2-arxitektura)
3. [O'rnatish va ishga tushirish](#3-ornatish-va-ishga-tushirish)
4. [Kirish va autentifikatsiya](#4-kirish-va-autentifikatsiya)
5. [Qurilmalar boshqaruvi](#5-qurilmalar-boshqaruvi)
6. [Loyihalar](#6-loyihalar)
7. [Eventlar va davomat](#7-eventlar-va-davomat)
8. [Yuz boshqaruvi (Face ID)](#8-yuz-boshqaruvi-face-id)
9. [Kirish nazorati (Access Control)](#9-kirish-nazorati-access-control)
10. [Kamera boshqaruvi](#10-kamera-boshqaruvi)
11. [PTZ Kamera boshqaruvi](#11-ptz-kamera-boshqaruvi)
12. [Qurilma sozlamalari](#12-qurilma-sozlamalari)
13. [Live Video (RTSP)](#13-live-video-rtsp)
14. [Hisobotlar](#14-hisobotlar)
15. [SADP Qurilma topish](#15-sadp-qurilma-topish)
16. [WebSocket real-time eventlar](#16-websocket-real-time-eventlar)
17. [Webhook integratsiya](#17-webhook-integratsiya)
18. [ISUP protokoli](#18-isup-protokoli)
19. [Xavfsizlik](#19-xavfsizlik)
20. [Docker va deploy](#20-docker-va-deploy)
21. [Konfiguratsiya parametrlari](#21-konfiguratsiya-parametrlari)
22. [Qo'llab-quvvatlanadigan qurilmalar](#22-qollab-quvvatlanadigan-qurilmalar)
23. [Xatoliklarni bartaraf etish](#23-xatoliklarni-bartaraf-etish)
24. [API to'liq ro'yxati](#24-api-toliq-royxati)

---

## 1. Tizim haqida

**ISUP Server** — Hikvision kompaniyasining Face ID terminallari, IP kameralar, NVR/DVR qurilmalar va kirish nazorat tizimlari bilan ishlash uchun yaratilgan universal boshqaruv serveri.

### Asosiy imkoniyatlar

| Imkoniyat | Tavsif |
|-----------|--------|
| **ISUP protokoli** | Hikvision qurilmalar bilan TCP ulanish (v1 va v5) |
| **Face ID boshqaruvi** | Xodimlarni ro'yxatdan o'tkazish, o'chirish, qidirish |
| **Kirish nazorati** | Eshik ochish/yopish, vaqtli ochish, blacklist/whitelist |
| **Kamera boshqaruvi** | PTZ, yozib olish, tasvir sozlamalari |
| **Davomat tizimi** | Real-time hodisalar, hisobotlar (Excel/PDF) |
| **Webhook** | Har qanday backend tizimiga event yuborish |
| **WebSocket** | Real-time monitoring |
| **SADP Discovery** | Tarmoqdagi Hikvision qurilmalarni avtomatik topish |
| **Cloudflare Tunnel** | Xavfsiz internet ulanish |

### Qo'llab-quvvatlanadigan qurilma turlari

- **Face ID terminallar**: DS-K1T343, DS-K1T671, DS-K1T804, DS-K1TA seriyalari
- **IP kameralar**: DS-2CD seriyalari (barcha modellari)
- **PTZ kameralar**: DS-2DE, DS-2DF seriyalari
- **NVR**: DS-7xxx, DS-9xxx seriyalari
- **DVR**: DS-8xxx seriyalari
- **Kirish nazorat panellari**: DS-K2xxx seriyalari
- **Termal kameralar**: DS-2TD seriyalari
- **DeepinView smart kameralar**: iDS-2CD, iDS-2SK seriyalari

---

## 2. Arxitektura

```
┌─────────────────────────────────────────────────────────┐
│                    Internet                              │
└─────────────────────┬───────────────────────────────────┘
                       │ HTTPS
┌──────────────────────▼──────────────────────────────────┐
│           Cloudflare (fake-faceid.uzinc.uz)             │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│          Docker Network (isup-network)                   │
│                                                          │
│  ┌─────────────────┐    ┌────────────────────────────┐  │
│  │   cloudflared   │───▶│   isup-server:8090         │  │
│  │   (tunnel)      │    │   Spring Boot              │  │
│  └─────────────────┘    │   ┌────────────────────┐   │  │
│                          │   │ REST API           │   │  │
│  ┌─────────────────┐    │   │ WebSocket (STOMP)  │   │  │
│  │   frontend:80   │    │   │ React SPA serve    │   │  │
│  │   nginx         │    │   └────────────────────┘   │  │
│  │   (port 8888)   │    └──────────────┬─────────────┘  │
│  └─────────────────┘                   │                 │
│                          ┌─────────────▼─────────────┐  │
│  ┌─────────────────┐    │   postgres:5432            │  │
│  │   ngrok         │    │   PostgreSQL 16            │  │
│  │   (TCP 7660)    │    └────────────────────────────┘  │
│  └─────────────────┘                                     │
│                          ┌─────────────────────────────┐ │
│                          │   ISUP TCP Server: 7660     │ │
│                          │   Netty (qurilma ulanishi)  │ │
│                          └─────────────────────────────┘ │
│                          ┌─────────────────────────────┐ │
│                          │   SADP UDP: 37020           │ │
│                          │   (qurilma topish)          │ │
│                          └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘

Qurilmalar (Hikvision):
  ├── Face ID Terminal  ──TCP:7660──▶ ISUP Server
  ├── IP Kamera        ──TCP:7660──▶ ISUP Server
  └── NVR              ──TCP:7660──▶ ISUP Server
```

### Texnologiyalar to'plami

**Backend:**
- Java 17, Spring Boot 3.2
- Netty 4.1 (TCP server)
- PostgreSQL 16 + Flyway (migration)
- Apache POI (Excel), iText (PDF)
- OkHttp (ISAPI HTTP client)

**Frontend:**
- React 18 + TypeScript
- Tailwind CSS v3 (Glassmorphism dark theme)
- React Query v5, Zustand, Axios
- SockJS + STOMP (WebSocket)
- Recharts (grafik)

**Infratuzilma:**
- Docker + Docker Compose
- Nginx (reverse proxy)
- Cloudflare Tunnel (HTTPS)

---

## 3. O'rnatish va ishga tushirish

### Talablar

- Docker Desktop 4.x+
- 2 GB RAM (minimum)
- Internet ulanish (Cloudflare uchun)

### Ishga tushirish

```bash
# 1. Papkaga o'ting
cd "D:/ISUP server java"

# 2. Barcha containerlarni ishga tushiring
docker compose up -d

# 3. Holat tekshirish
docker compose ps

# 4. Server sog'lig'ini tekshirish
curl http://localhost:8090/health
```

### Kutilgan natija

```json
{
  "status": "UP",
  "isup_server": {"status": "UP", "port": 7660},
  "database": {"status": "UP"},
  "uptime_seconds": 30,
  "memory_mb": 47
}
```

### Portlar

| Port | Protokol | Maqsad |
|------|----------|--------|
| 7660 | TCP | ISUP qurilma ulanishi |
| 8090 | HTTP | REST API + React UI |
| 8888 | HTTP | Frontend (nginx, local test) |
| 37020 | UDP | SADP qurilma topish |
| 4040 | HTTP | Ngrok dashboard |

---

## 4. Kirish va autentifikatsiya

### Web interfeys orqali kirish

Brauzerda oching: **https://fake-faceid.uzinc.uz**

```
Login: admin
Parol: isup1234
```

### API orqali kirish

```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Javob:**
```json
{
  "token": "changeme123",
  "username": "admin"
}
```

Olingan tokenni har bir API so'rovda ishlatish:
```
Authorization: Bearer changeme123
```

### Parolni o'zgartirish

`.env` faylida:
```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=yangi_parol_kiriting
```
Keyin: `docker compose up -d isup-server`

---

## 5. Qurilmalar boshqaruvi

### Qurilma qo'shish (Web UI)

1. **Qurilmalar** menyusiga boring
2. **+ Qo'shish** tugmasini bosing
3. Ma'lumotlarni kiriting:
   - **Device ID**: Qurilma identifikatori (masalan: `admin`)
   - **IP manzil**: Qurilma IP si (masalan: `192.168.1.100`)
   - **Parol**: ISAPI parol (default: `admin12345`)
   - **Loyiha**: Qaysi loyihaga bog'lash

### Qurilma qo'shish (API)

```bash
POST /api/devices
Authorization: Bearer changeme123

{
  "deviceId": "device001",
  "name": "1-qavat kirish eshigi",
  "location": "1-bino, kirish",
  "deviceIp": "192.168.1.100",
  "deviceUsername": "admin",
  "devicePassword": "admin12345",
  "projectId": 1
}
```

### Qurilma sozlamalari

Qurilma sozlamalari sahifasida:
- **Asosiy ma'lumotlar**: Model, serial raqam, firmware versiyasi
- **Tarmoq sozlamalari**: IP manzil, subnet, gateway
- **Disk ma'lumotlari**: HDD holati, sig'im, band joy
- **Tizim holati**: CPU, xotira, harorat
- **Qo'shimcha**: Qayta ishga tushirish (reboot)

### SADP orqali avtomatik topish

Server tarmoqdagi barcha Hikvision qurilmalarni avtomatik topadi:

```bash
POST /api/discovery/scan
# → tarmoqni skanerlaydi

GET /api/discovery/devices
# → topilgan qurilmalar ro'yxati
```

Topilgan qurilmani tizimga qo'shish:
```bash
POST /api/discovery/{id}/claim
```

### Qurilma imkoniyatlari (Capabilities)

Server qurilma ulanganida uning imkoniyatlarini avtomatik aniqlaydi:

| Imkoniyat | Tavsif |
|-----------|--------|
| `face` | Yuz tanish va ro'yxatga olish |
| `door` | Eshik ochish/yopish |
| `attendance` | Davomat hisobi |
| `rtsp` | Video oqim |
| `ptz` | Pan/Tilt/Zoom kamera boshqaruvi |
| `nvr` | Yozib olish va saqlash |
| `dvr` | Raqamli video yozib olish |
| `recording` | Yozib olish funksiyasi |
| `motion` | Harakat aniqlash |
| `line_crossing` | Chiziq kesib o'tish aniqlash |
| `card` | Kartadan foydalanish |
| `alarm` | Signal kirish/chiqish |
| `thermal` | Termal tasvirlash |
| `temperature` | Harorat o'lchash |

---

## 6. Loyihalar

Loyihalar — qurilmalarni guruhlash va webhook integratsiyasi uchun asosiy tushuncha.

### Loyiha yaratish

```bash
POST /api/projects
Authorization: Bearer changeme123

{
  "name": "Ofis binosi",
  "webhookUrl": "https://sizning-saytingiz.uz/webhook",
  "retryCount": 3
}
```

### Loyiha tuzilmasi

```
Loyiha: "Ofis binosi"
  ├── Qurilma: 1-qavat kirish eshigi (DS-K1T343)
  ├── Qurilma: 2-qavat kirish (DS-K1T343)
  ├── Qurilma: Xavfsizlik kamerasi (DS-2CD)
  └── Qurilma: Kutubxona eshigi (DS-K1T343)
```

### Secret key

Har bir loyihada avtomatik `secretKey` yaratiladi. Bu key webhook imzosi uchun ishlatiladi:

```bash
# Secret key yangilash
POST /api/projects/{id}/regenerate-secret
```

---

## 7. Eventlar va davomat

### Event turlari

| Event turi | Tavsif |
|------------|--------|
| `attendance` | Xodim kirish/chiqish |
| `door` | Eshik ochildi/yopildi |
| `alarm` | Signal berdi |
| `motion` | Harakat aniqlandi |
| `line_crossing` | Chiziq kesib o'tildi |
| `face_detection` | Yuz aniqlandi |
| `intrusion` | Ruxsatsiz kirish |

### Eventlarni ko'rish

**Web UI**: Eventlar sahifasida jadval ko'rinishida, fotosurат bilan.

**API:**
```bash
# Barcha eventlar (sahifalab)
GET /api/events?page=0&size=20

# Vaqt oralig'i bo'yicha filter
GET /api/events?startTime=2024-01-15T00:00:00Z&endTime=2024-01-15T23:59:59Z

# Qurilma bo'yicha filter
GET /api/events?deviceId=admin&size=50

# Xodim bo'yicha
GET /api/events?employeeNo=EMP001

# Event turi bo'yicha
GET /api/events?eventType=attendance
```

### Kirish/chiqish yo'nalishlari

| Qiymat | Ma'no |
|--------|-------|
| `in` | Kirish |
| `out` | Chiqish |
| `break_in` | Tanaffusdan qaytish |
| `break_out` | Tanaffusga chiqish |
| `overtime_in` | Ortiqcha vaqt boshlash |
| `overtime_out` | Ortiqcha vaqt tugash |

### Tasdiqlash usullari (Verify Mode)

| Qiymat | Ma'no |
|--------|-------|
| `face` | Yuz orqali |
| `card` | Plastik karta |
| `fp` | Barmoq izi |
| `faceAndCard` | Yuz + karta |
| `faceAndFp` | Yuz + barmoq izi |
| `cardOrFace` | Karta yoki yuz |

---

## 8. Yuz boshqaruvi (Face ID)

### Xodim qo'shish

**Web UI (Drag & Drop):**
1. "Yuz Boshqaruv" sahifasiga boring
2. Foto suratingizni suring yoki bosib yuklang
3. Xodim ID, Ism, Jins kiriting
4. "Barcha qurilmalarga" belgilang
5. "Yuklash" tugmasini bosing

**API:**
```bash
# Bitta qurilmaga yuklash
POST /api/faces/enroll
Authorization: Bearer changeme123
Content-Type: application/json

{
  "deviceId": "admin",
  "employeeNo": "EMP001",
  "name": "Jasurbek Toshmatov",
  "gender": "male",
  "photoBase64": "/9j/4AAQSkZJRgAB..."
}

# Barcha online qurilmalarga yuklash
POST /api/faces/enroll-all
{
  "employeeNo": "EMP001",
  "name": "Jasurbek Toshmatov",
  "gender": "male",
  "photoBase64": "/9j/4AAQSkZJRgAB..."
}
```

### Xodim o'chirish

```bash
# Barcha qurilmalardan o'chirish
DELETE /api/faces/EMP001

# Qurilmadagi yuzlar ro'yxati
GET /api/faces?deviceId=admin
```

### Foto talablari

- Format: JPEG/JPG
- O'lcham: 200KB dan kichik (tavsiya etiladi)
- Ruxsat: 240×320 px yoki yuqori
- Holat: To'g'ri, yuzning 80% ko'rinib turishi kerak

---

## 9. Kirish nazorati (Access Control)

### Eshik boshqaruvi

**Web UI**: "Kirish Nazorati" sahifasi → qurilma kartasidagi tugmalar

**API:**
```bash
# Eshikni ochish
POST /api/access/door/open
{
  "deviceId": "admin",
  "doorNo": 1
}

# Eshikni yopish
POST /api/access/door/close
{
  "deviceId": "admin",
  "doorNo": 1
}

# Vaqtli ochish (10 soniya)
POST /api/access/door/open-timed
{
  "deviceId": "admin",
  "doorNo": 1,
  "seconds": 10
}
```

### Blacklist boshqaruvi

```bash
# Blacklistga qo'shish
POST /api/access/blacklist
{
  "employeeNo": "EMP999",
  "deviceIds": null,
  "reason": "Shartnoma tugadi"
}

# Blacklistdan o'chirish
DELETE /api/access/blacklist/EMP999

# Blacklist ro'yxati
GET /api/access/blacklist
```

### Whitelist (vaqtli ruxsat)

```bash
POST /api/access/whitelist
{
  "employeeNo": "EMP100",
  "deviceIds": ["admin", "device002"],
  "validFrom": "2024-01-15T08:00:00Z",
  "validTo": "2024-01-15T18:00:00Z"
}
```

---

## 10. Kamera boshqaruvi

### Tasvir sozlamalari

```bash
# Kamera tasvir sozlamalarini olish
GET /api/camera/{deviceId}/image/{channelId}

# Sozlamalarni yangilash
PUT /api/camera/{deviceId}/image/{channelId}
{
  "brightness": 50,
  "contrast": 50,
  "saturation": 50,
  "sharpness": 50
}

# Video oqim kanallarini olish
GET /api/camera/{deviceId}/streams
```

### Yozib olish

```bash
# Yozib olishni boshlash
POST /api/device-config/{deviceId}/recordings/{channelId}/start

# Yozib olishni to'xtatish
POST /api/device-config/{deviceId}/recordings/{channelId}/stop

# Yozilgan videolarni qidirish
GET /api/device-config/{deviceId}/recordings?channelId=101&startTime=2024-01-15T00:00:00&endTime=2024-01-15T23:59:59
```

### Harakat aniqlash

```bash
GET /api/device-config/{deviceId}/motion/{channelId}
```

---

## 11. PTZ Kamera boshqaruvi

PTZ kameralar (DS-2DE, DS-2DF seriyalari) uchun.

### Harakatlanish

```bash
# Kamerani burish (direction: LEFT, RIGHT, UP, DOWN, ZOOM_IN, ZOOM_OUT)
POST /api/camera/{deviceId}/ptz/{channelId}/move
{
  "direction": "RIGHT",
  "speed": 50
}

# To'xtatish
POST /api/camera/{deviceId}/ptz/{channelId}/stop
```

### Preset pozitsiyalar

```bash
# Barcha presetlar ro'yxati
GET /api/camera/{deviceId}/ptz/{channelId}/presets

# Presetga o'tish
POST /api/camera/{deviceId}/ptz/{channelId}/preset/{presetId}/goto

# Joriy holat
GET /api/camera/{deviceId}/ptz/{channelId}/status
```

### PTZ tezlik darajalari

| Qiymat | Tavsif |
|--------|--------|
| 1-30 | Sekin |
| 31-70 | O'rta |
| 71-100 | Tez |

---

## 12. Qurilma sozlamalari

### Tizim ma'lumotlari

```bash
# To'liq qurilma ma'lumoti
GET /api/device-config/{deviceId}/info

# CPU, xotira, harorat holati
GET /api/device-config/{deviceId}/status

# Tarmoq sozlamalari
GET /api/device-config/{deviceId}/network

# Disk ma'lumotlari
GET /api/device-config/{deviceId}/storage

# Qurilmani qayta ishga tushirish (EHTIYOT BILAN!)
POST /api/device-config/{deviceId}/reboot
```

### Signal boshqaruvi

```bash
# Signal kirish portlari holati
GET /api/device-config/{deviceId}/alarm/inputs

# Signal chiqish portini ishga tushirish
POST /api/device-config/{deviceId}/alarm/outputs/{outputId}/trigger
```

### Foydalanuvchi boshqaruvi (qurilma ichida)

```bash
# Qurilmadagi foydalanuvchilar
GET /api/device-config/{deviceId}/users

# Foydalanuvchi qo'shish
POST /api/device-config/{deviceId}/users
{
  "employeeNo": "EMP001",
  "name": "Jasurbek",
  "cardNo": "1234567890"
}

# Foydalanuvchi o'chirish
DELETE /api/device-config/{deviceId}/users/{employeeNo}
```

---

## 13. Live Video (RTSP)

### RTSP URL olish

```bash
GET /api/stream/{deviceId}
```

**Javob:**
```json
{
  "rtspUrl": "rtsp://admin:admin12345@192.168.1.100:554/Streaming/Channels/101",
  "rtspSubUrl": "rtsp://admin:admin12345@192.168.1.100:554/Streaming/Channels/102"
}
```

### RTSP URL formatlari

| Kanal | URL | Sifat |
|-------|-----|-------|
| Ana oqim | `.../Streaming/Channels/101` | HD (1080p) |
| Sub oqim | `.../Streaming/Channels/102` | SD (720p) |
| Kanal 2 | `.../Streaming/Channels/201` | 2-kanal |

### Video mijozlar

RTSP URL ni quyidagi dasturlarda ochish mumkin:
- **VLC Media Player** (kompyuter)
- **IINA** (Mac)
- **HiLook** mobil ilova
- **Hik-Connect** mobil ilova
- **Web UI** — "Live Video" sahifasi (HLS.js orqali)

### NVR uchun kanal URL lari

```
rtsp://admin:pass@192.168.1.200:554/Streaming/Channels/101  ← 1-kamera
rtsp://admin:pass@192.168.1.200:554/Streaming/Channels/201  ← 2-kamera
rtsp://admin:pass@192.168.1.200:554/Streaming/Channels/301  ← 3-kamera
```

---

## 14. Hisobotlar

### Kunlik davomat hisoboti

**Web UI**: "Hisobotlar" sahifasi → Sana tanlang → Excel/PDF

**API:**
```bash
# Excel format
GET /api/reports/attendance/daily?date=2024-01-15

# PDF format
GET /api/reports/attendance/daily?date=2024-01-15&format=pdf
```

### Oylik hisobot

```bash
GET /api/reports/attendance/monthly?month=2024-01
```

### Xodim tarixi

```bash
GET /api/reports/attendance/employee/EMP001?from=2024-01-01&to=2024-01-31
```

### Excel ustunlari

| Ustun | Ma'no |
|-------|-------|
| Xodim | Xodim ID |
| Ism | To'liq ism |
| Kirish | Birinchi kirish vaqti |
| Chiqish | Oxirgi chiqish vaqti |
| Ishlagan soat | Jami ish soati |
| Qurilma | Qaysi qurilmadan kirgan |
| Tasdiqlash usuli | Face/Card/FP |

---

## 15. SADP Qurilma topish

SADP (Search Active Device Protocol) — Hikvision'ning UDP-based qurilma topish protokoli.

### Qanday ishlaydi

1. Server UDP port 37020 ni tinglaydi
2. Tarmoqdagi Hikvision qurilmalar o'zlarini e'lon qiladi
3. Server ularni `discovered_devices` jadvaliga yozadi
4. Web UI da "Topilgan qurilmalar" bo'limida ko'rinadi

### Qurilmalarni topish

```bash
# Skanerlashni boshlash
POST /api/discovery/scan

# Topilgan qurilmalar
GET /api/discovery/devices
```

**Javob:**
```json
[
  {
    "ip": "192.168.1.100",
    "mac": "44:19:B6:XX:XX:XX",
    "model": "DS-K1T343EWX",
    "serialNo": "DS-K1T343EWX20221201BBWR...",
    "firmware": "V1.4.5",
    "activated": true
  }
]
```

### Qurilmani tizimga qo'shish

```bash
POST /api/discovery/{id}/claim
```

---

## 16. WebSocket real-time eventlar

### Ulanish

```javascript
const client = new Client({
  webSocketFactory: () => new SockJS('/ws'),
  connectHeaders: { Authorization: 'Bearer changeme123' },
  reconnectDelay: 5000,
});

client.activate();
client.subscribe('/topic/events', (message) => {
  const event = JSON.parse(message.body);
  console.log(event);
});
```

### Event ma'lumotlari

```json
{
  "eventId": "uuid-...",
  "eventType": "attendance",
  "deviceId": "admin",
  "deviceName": "1-qavat kirish",
  "deviceModel": "DS-K1T343EWX",
  "employeeNo": "EMP001",
  "employeeName": "Jasurbek",
  "direction": "in",
  "verifyMode": "face",
  "eventTime": "2024-01-15T09:30:00Z",
  "photoBase64": "...",
  "doorNo": 1
}
```

---

## 17. Webhook integratsiya

### Webhook qanday ishlaydi

```
Qurilma → ISUP Server → [Event saqlanadi] → [Webhook yuboriladi] → Sizning serveringiz
```

### Webhook sozlash

Loyihada `webhookUrl` ko'rsating:
```bash
PUT /api/projects/1
{
  "webhookUrl": "https://sizning-saytingiz.uz/api/isup-events"
}
```

### Kelgan ma'lumot (POST body)

```json
{
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "event_type": "attendance",
  "device_id": "admin",
  "device_model": "DS-K1T343EWX",
  "employee_no": "EMP001",
  "employee_name": "Jasurbek Toshmatov",
  "direction": "in",
  "verify_mode": "face",
  "door_no": 1,
  "event_time": "2024-01-15T09:30:00Z",
  "photo_base64": "...",
  "project_id": 1,
  "timestamp": "2024-01-15T09:30:01Z"
}
```

### HMAC imzo tekshirish

```python
import hmac, hashlib

def verify_webhook(body: str, signature: str, secret: str) -> bool:
    expected = hmac.new(
        secret.encode(),
        body.encode(),
        hashlib.sha256
    ).hexdigest()
    return signature == f"sha256={expected}"

# Django namunasi
def webhook_view(request):
    sig = request.headers.get('X-ISUP-Signature', '')
    body = request.body.decode('utf-8')
    if not verify_webhook(body, sig, 'sizning-secret-keyingiz'):
        return JsonResponse({'error': 'Invalid signature'}, status=401)
    data = json.loads(body)
    # ... hodisani qayta ishlash
```

### Retry siyosati

- Default: 3 ta urinish
- Interval: 5 soniya
- Xato bo'lsa: `webhook_status = "failed"` yoziladi

---

## 18. ISUP protokoli

### v5 Frame tuzilmasi (STX = 0x20)

```
+--------+----------+----------+----------+----------+------------+-----+
| STX    | Length   | MsgType  | SessionID| SeqNo    | Payload    | ETX |
| 1 byte | 4 LE     | 2 LE     | 4 LE     | 2 LE     | N bytes    | 1   |
+--------+----------+----------+----------+----------+------------+-----+
| 0x20   |          |          |          |          |            | 0x0A|
+--------+----------+----------+----------+----------+------------+-----+
```

### v1 Frame tuzilmasi (STX = 0x10, DS-K1T343 seriyasi)

```
+--------+----------+------------+
| STX    | BodyLen  | Body       |
| 0x10   | 1 byte   | BodyLen    |
+--------+----------+------------+
```

### Xabar turlari

| Kod | Tur | Tavsif |
|-----|-----|--------|
| 0x0001 | LOGIN_REQUEST | Qurilma serverga ulanish so'rovi |
| 0x0002 | LOGIN_RESPONSE | Server javob beradi |
| 0x0013 | KEEPALIVE_REQUEST | Hayot signali (har 30 sek) |
| 0x0014 | KEEPALIVE_RESPONSE | Hayot signali javobi |
| 0x0004 | ALARM_EVENT | Qurilmadan event (davomat, signal) |
| 0x0005 | ALARM_RESPONSE | Event qabul qilindi |
| 0x0003 | LOGOUT | Qurilma uzilish |

---

## 19. Xavfsizlik

### API autentifikatsiyasi

Barcha API so'rovlar uchun:
```
Authorization: Bearer changeme123
```

Bundan mustasno:
- `GET /health` — ochiq
- `POST /api/auth/login` — ochiq
- `OPTIONS *` — CORS preflight

### Rate Limiting

- Chegara: 60 soniyada 100 so'rov (bir IP dan)
- Oshib ketsa: `429 Too Many Requests`

### ISUP qurilma autentifikatsiyasi

- HMAC-SHA256 challenge-response
- Nonce: 33 baytlik tasodifiy son
- Parol: `Device.passwordHash` yoki bo'sh satr

### Tavsiyalar

```env
# Kuchli parollar ishlating:
ADMIN_PASSWORD=Uzb3k1st0n#2024!
ADMIN_SECRET=Hikvision_S3rv3r_S3cr3t_K3y!
POSTGRES_PASSWORD=P0stgr3SQL_P@ssw0rd!
```

---

## 20. Docker va deploy

### Barcha xizmatlar

```yaml
services:
  isup-server:    # Spring Boot (8090)
  frontend:       # React nginx (8888)
  postgres:       # PostgreSQL (5432)
  cloudflared:    # Internet tunnel
  ngrok:          # TCP tunnel (7660)
  db-backup:      # Avtomatik zaxira nusxa
```

### Muhim buyruqlar

```bash
# Barcha containerlarni ishga tushirish
docker compose up -d

# Faqat server qayta ishga tushirish
docker compose restart isup-server

# Loglarni ko'rish
docker logs isup-server-1 --tail 100 -f

# To'liq qayta qurilish
docker compose build --no-cache isup-server
docker compose up -d isup-server

# Ma'lumotlar bazasini ko'rish
docker exec -it isupserverjava-postgres-1 psql -U isup -d isup_db

# Zaxira nusxa
docker exec isupserverjava-postgres-1 pg_dump -U isup isup_db > backup.sql
```

### Nginx konfiguratsiyasi

```nginx
server {
  listen 80;
  # / → React SPA
  location / { try_files $uri /index.html; }
  # /api/ → Spring Boot REST
  location /api/ { proxy_pass http://isup-server:8090/api/; }
  # /ws → WebSocket
  location /ws { proxy_pass http://isup-server:8090/ws; }
}
```

---

## 21. Konfiguratsiya parametrlari

### `.env` fayli

```env
# Ma'lumotlar bazasi
POSTGRES_DB=isup_db
POSTGRES_USER=isup
POSTGRES_PASSWORD=isup_pass

# Portlar
ISUP_TCP_PORT=7660
SADP_UDP_PORT=37020
API_PORT=8090

# Admin kirish
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
ADMIN_SECRET=changeme123

# Qurilma sozlamalari
ALLOW_UNKNOWN_DEVICES=true

# Cloudflare
TUNNEL_TOKEN=eyJh...

# Telegram ogohlantirish
ALERT_TELEGRAM_TOKEN=
ALERT_CHAT_ID=

# Eventlar saqlash muddati (kun)
EVENT_RETENTION_DAYS=90
```

### `application.yml` parametrlari

```yaml
isup:
  tcp.port: 7660          # ISUP TCP port
  sadp.port: 37020        # SADP UDP port
  allow-unknown-devices: true  # Noma'lum qurilmalar
  webhook:
    connect-timeout-ms: 5000  # Webhook ulanish vaqt chegarasi
    read-timeout-ms: 10000    # Webhook o'qish vaqt chegarasi
    max-retries: 3             # Qayta urinishlar soni
    retry-delay-ms: 5000      # Qayta urinish orasidagi kutish
  event:
    retention-days: 90         # Eventlar saqlash muddati
    buffer.max-size: 10000    # Xotira bufer hajmi
```

---

## 22. Qo'llab-quvvatlanadigan qurilmalar

### Face ID terminallar

| Model | Imkoniyatlar |
|-------|-------------|
| DS-K1T343EWX | face, door, attendance, rtsp |
| DS-K1T343MWX | face, door, attendance, rtsp |
| DS-K1T671 | face, door, attendance, temperature |
| DS-K1T804 | face, door, attendance, card |
| DS-K1TA70 | face, door, attendance, temperature, alarm |
| DS-K1T672M | face, door, card, attendance |

### IP Kameralar

| Seriya | Imkoniyatlar |
|--------|-------------|
| DS-2CD1xxx | rtsp, motion |
| DS-2CD2xxx | rtsp, motion, line_crossing |
| DS-2CD3xxx | rtsp, motion, line_crossing, face |
| DS-2DE3xx | rtsp, ptz, motion |
| DS-2DF8xx | rtsp, ptz, motion, line_crossing |
| DS-2TD2xxx | rtsp, thermal, motion |
| iDS-2CD7xx | rtsp, face, motion, line_crossing |

### NVR/DVR

| Model | Imkoniyatlar |
|-------|-------------|
| DS-7608/7616/7632NI | nvr, recording, playback, rtsp |
| DS-9632NI-M8 | nvr, recording, playback, rtsp |
| DS-8616SHI-ST | dvr, recording, playback, rtsp |

### Kirish nazorat panellari

| Model | Imkoniyatlar |
|-------|-------------|
| DS-K2604T | door, card, alarm |
| DS-K2602T | door, card, alarm |
| DS-K2M061 | door, card, alarm |

### Qurilma ISUP sozlamalari

Hikvision qurilmasida ISUP sozlash:
```
Configuration → Network → Advanced Settings → Platform Access
  Platform Access Mode: Hik-Connect
  ↓ yoki ↓
  Platform Access Mode: Other
    Server Address: 192.168.1.35 (server IP)
    Port: 7660
    Device ID: admin
    Key: (bo'sh qoldiriladi)
```

---

## 23. Xatoliklarni bartaraf etish

### Qurilma ulanmayapti

**Belgilari**: Server logida qurilma ko'rinmaydi

**Tekshirish:**
```bash
# Server loglarini ko'rish
docker logs isupserverjava-isup-server-1 --tail 50

# TCP portni tekshirish
netstat -an | grep 7660

# Qurilmadan ping
ping 192.168.1.35  # server IP
```

**Yechimlar:**
1. Qurilma sozlamalarida Server Address to'g'ri ekanligini tekshiring
2. Port 7660 ochiq ekanligini tekshiring
3. `ALLOW_UNKNOWN_DEVICES=true` ekanligini tekshiring

### ISAPI ulanmayapti (Face, Door API)

**Belgilari**: "Device has no IP address" xatosi

**Yechim:**
1. Qurilmaning `deviceIp` maydonini to'ldiring
2. IP manzil server bilan bir tarmoqda ekanligini tekshiring
3. Qurilmaning HTTP portini tekshiring (odatda 80)

### Webhook kelmayapti

**Belgilari**: Eventlar DB ga yoziladi, lekin webhook server qabul qilmaydi

**Tekshirish:**
```bash
# Failed webhooks
curl -s "http://localhost:8090/api/events?webhookStatus=failed" \
  -H "Authorization: Bearer changeme123"

# Server health
curl http://localhost:8090/health
```

**Yechimlar:**
1. Webhook URL to'g'ri va erishiladi ekanligini tekshiring
2. Firewall qurilmalarini tekshiring
3. HMAC imzoni to'g'ri tekshirayotganingizni tasdiqlang

### Ma'lumotlar bazasi ulanmayapti

```bash
# Postgres holati
docker compose ps postgres

# Loglar
docker logs isupserverjava-postgres-1 --tail 20
```

### Xotira etishmayapti

```bash
# Joriy foydalanish
curl http://localhost:8090/health | python3 -m json.tool

# Container statistika
docker stats
```

---

## 24. API to'liq ro'yxati

### Autentifikatsiya
```
POST   /api/auth/login          Kirish (username + password)
```

### Qurilmalar
```
GET    /api/devices             Barcha qurilmalar
GET    /api/devices/online      Online qurilmalar
GET    /api/devices/{id}        Bitta qurilma
POST   /api/devices             Qurilma qo'shish
PUT    /api/devices/{id}        Qurilma yangilash
DELETE /api/devices/{id}        Qurilma o'chirish
```

### Loyihalar
```
GET    /api/projects             Barcha loyihalar
GET    /api/projects/{id}        Bitta loyiha
POST   /api/projects             Loyiha yaratish
PUT    /api/projects/{id}        Loyiha yangilash
DELETE /api/projects/{id}        Loyiha o'chirish
POST   /api/projects/{id}/regenerate-secret   Secret yangilash
```

### Eventlar
```
GET    /api/events               Eventlar (sahifalab, filter)
GET    /api/events/recent        So'nggi eventlar
GET    /api/events/stats         Statistika
```

### Yuz boshqaruvi
```
POST   /api/faces/enroll         Bitta qurilmaga yuz yuklash
POST   /api/faces/enroll-all     Barcha qurilmalarga
DELETE /api/faces/{employeeNo}   Barcha qurilmalardan o'chirish
GET    /api/faces?deviceId=xxx   Qurilmadagi yuzlar
```

### Kirish nazorati
```
POST   /api/access/door/open        Eshik ochish
POST   /api/access/door/close       Eshik yopish
POST   /api/access/door/open-timed  Vaqtli ochish
POST   /api/access/blacklist        Blacklistga qo'shish
DELETE /api/access/blacklist/{no}   Blacklistdan o'chirish
GET    /api/access/blacklist        Blacklist ro'yxati
POST   /api/access/whitelist        Whitelist qo'shish
GET    /api/access/whitelist        Whitelist ro'yxati
```

### Kamera
```
GET    /api/camera/{did}/image/{ch}          Tasvir sozlamalari
PUT    /api/camera/{did}/image/{ch}          Sozlamalarni yangilash
GET    /api/camera/{did}/streams             Video kanallar
POST   /api/camera/{did}/ptz/{ch}/move       PTZ harakatlantirish
POST   /api/camera/{did}/ptz/{ch}/stop       PTZ to'xtatish
POST   /api/camera/{did}/ptz/{ch}/preset/{n}/goto   Presetga o'tish
GET    /api/camera/{did}/ptz/{ch}/presets    Presetlar ro'yxati
GET    /api/camera/{did}/ptz/{ch}/status     Joriy holat
```

### Qurilma konfiguratsiyasi
```
GET    /api/device-config/{did}/info                 Tizim ma'lumotlari
GET    /api/device-config/{did}/status               Holat
GET    /api/device-config/{did}/network              Tarmoq sozlamalari
GET    /api/device-config/{did}/storage              Disk ma'lumotlari
POST   /api/device-config/{did}/reboot               Qayta ishga tushirish
GET    /api/device-config/{did}/alarm/inputs         Signal kirish
POST   /api/device-config/{did}/alarm/outputs/{n}/trigger  Signal ishga tushirish
GET    /api/device-config/{did}/motion/{ch}          Harakat aniqlash
GET    /api/device-config/{did}/recordings           Yozilgan videolar
POST   /api/device-config/{did}/recordings/{ch}/start   Yozishni boshlash
POST   /api/device-config/{did}/recordings/{ch}/stop    Yozishni to'xtatish
GET    /api/device-config/{did}/users                Foydalanuvchilar
POST   /api/device-config/{did}/users                Foydalanuvchi qo'shish
DELETE /api/device-config/{did}/users/{empNo}        O'chirish
```

### Video
```
GET    /api/stream/{deviceId}    RTSP URL
```

### Hisobotlar
```
GET    /api/reports/attendance/daily?date=YYYY-MM-DD&format=excel|pdf
GET    /api/reports/attendance/monthly?month=YYYY-MM
GET    /api/reports/attendance/employee/{empNo}?from=...&to=...
```

### Topish (Discovery)
```
POST   /api/discovery/scan       Tarmoq skanerlash
GET    /api/discovery/devices    Topilgan qurilmalar
GET    /api/discovery/all        Barchasi (da'vo qilinganlar ham)
POST   /api/discovery/{id}/claim Tizimga qo'shish
```

### Monitoring
```
GET    /health      Server sog'lig'i (auth talab qilinmaydi)
GET    /metrics     Prometheus metrikalar
```

---

*Hujjat ISUP Server v2.0 uchun tayyorlangan.*
*Savol va takliflar uchun GitHub Issues sahifasiga murojaat qiling.*
