#!/usr/bin/env python3
"""
ISUP Server Documentation PDF Generator
Uses fpdf2 library with Unicode font support
"""
from fpdf import FPDF
from fpdf.enums import XPos, YPos
import os, re

# Font paths (Windows system fonts)
FONT_REGULAR = "C:/Windows/Fonts/arial.ttf"
FONT_BOLD    = "C:/Windows/Fonts/arialbd.ttf"
FONT_ITALIC  = "C:/Windows/Fonts/ariali.ttf"
FONT_MONO    = "C:/Windows/Fonts/cour.ttf"
FONT_MONO_B  = "C:/Windows/Fonts/courbd.ttf"

class ISUPDocPDF(FPDF):
    def __init__(self):
        super().__init__('P', 'mm', 'A4')
        self.set_auto_page_break(auto=True, margin=20)
        self.set_margins(20, 20, 20)
        self._load_fonts()

    def _load_fonts(self):
        self.add_font('Arial', style='',  fname=FONT_REGULAR)
        self.add_font('Arial', style='B', fname=FONT_BOLD)
        self.add_font('Arial', style='I', fname=FONT_ITALIC)
        self.add_font('Courier2', style='',  fname=FONT_MONO)
        self.add_font('Courier2', style='B', fname=FONT_MONO_B)

    def header(self):
        if self.page_no() > 1:
            self.set_font('Arial', 'I', 8)
            self.set_text_color(128, 128, 128)
            self.cell(0, 8, "ISUP Server - To'liq Texnik Hujjat v2.0",
                      new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            self.set_draw_color(200, 200, 200)
            self.line(20, self.get_y(), 190, self.get_y())
            self.ln(3)

    def footer(self):
        self.set_y(-15)
        self.set_font('Arial', 'I', 8)
        self.set_text_color(128, 128, 128)
        self.cell(0, 8, f'Sahifa {self.page_no()}', align='C')

    def cover_page(self):
        self.add_page()
        # Background feel
        self.set_fill_color(15, 23, 42)
        self.rect(0, 0, 210, 297, 'F')

        # Accent line
        self.set_fill_color(99, 102, 241)
        self.rect(0, 100, 210, 3, 'F')

        # Title
        self.set_y(120)
        self.set_font('Arial', 'B', 32)
        self.set_text_color(255, 255, 255)
        self.cell(0, 15, 'ISUP SERVER', align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)

        self.set_font('Arial', '', 18)
        self.set_text_color(165, 180, 252)
        self.cell(0, 12, "To'liq Texnik Hujjat", align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)

        self.ln(10)
        self.set_font('Arial', '', 12)
        self.set_text_color(148, 163, 184)
        self.cell(0, 8, 'Hikvision qurilmalarni universal boshqaruv tizimi',
                  align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)

        # Info box
        self.set_y(220)
        self.set_fill_color(30, 41, 59)
        self.set_draw_color(99, 102, 241)
        self.set_line_width(0.5)
        self.rect(40, 215, 130, 50, 'FD')

        self.set_y(222)
        self.set_font('Arial', '', 10)
        self.set_text_color(203, 213, 225)
        items = [
            ('Versiya:', 'v2.0'),
            ('Sana:', 'Mart 2026'),
            ('Platforma:', 'Java 17 + Spring Boot 3.2'),
            ('URL:', 'https://fake-faceid.uzinc.uz'),
        ]
        for label, value in items:
            self.set_x(50)
            self.set_font('Arial', 'B', 10)
            self.cell(35, 9, label)
            self.set_font('Arial', '', 10)
            self.cell(0, 9, value, new_x=XPos.LMARGIN, new_y=YPos.NEXT)

        # Bottom
        self.set_y(275)
        self.set_font('Arial', 'I', 9)
        self.set_text_color(100, 116, 139)
        self.cell(0, 8, 'Hikvision ISUP Protocol | ISAPI | WebSocket | Docker', align='C')

    def toc_entry(self, num, title, page_hint=''):
        self.set_font('Arial', '', 10)
        self.set_text_color(30, 41, 59)
        x = self.get_x()
        self.set_x(20)
        self.cell(10, 7, f'{num}.')
        self.cell(0, 7, title, new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    def chapter_title(self, title):
        self.add_page()
        # Header bar
        self.set_fill_color(99, 102, 241)
        self.rect(0, 0, 210, 2, 'F')
        self.ln(5)

        self.set_font('Arial', 'B', 18)
        self.set_text_color(30, 41, 59)
        self.cell(0, 12, title, new_x=XPos.LMARGIN, new_y=YPos.NEXT)

        # Underline
        self.set_fill_color(99, 102, 241)
        self.rect(20, self.get_y(), 40, 2, 'F')
        self.ln(6)

    def section_title(self, title, level=2):
        self.ln(4)
        if level == 2:
            self.set_font('Arial', 'B', 13)
            self.set_text_color(79, 70, 229)
        else:
            self.set_font('Arial', 'B', 11)
            self.set_text_color(55, 65, 81)
        self.multi_cell(0, 7, title, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(1)

    def body_text(self, text):
        self.set_font('Arial', '', 10)
        self.set_text_color(55, 65, 81)
        self.multi_cell(0, 6, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(1)

    def code_block(self, code):
        self.set_fill_color(248, 250, 252)
        self.set_draw_color(226, 232, 240)
        self.set_line_width(0.3)

        lines = code.strip().split('\n')
        block_h = len(lines) * 5.5 + 6

        # Draw background
        self.rect(20, self.get_y(), 170, block_h, 'FD')

        self.set_x(24)
        y_start = self.get_y() + 3
        self.set_y(y_start)

        self.set_font('Courier2', '', 8.5)
        self.set_text_color(30, 41, 59)
        for line in lines:
            self.set_x(24)
            # Color keywords
            if line.strip().startswith('#'):
                self.set_text_color(100, 116, 139)
            elif any(line.strip().startswith(m) for m in ['GET ', 'POST ', 'PUT ', 'DELETE ']):
                self.set_text_color(79, 70, 229)
            elif line.strip().startswith('{') or line.strip().startswith('}'):
                self.set_text_color(30, 41, 59)
            else:
                self.set_text_color(30, 41, 59)
            self.cell(166, 5.5, line, new_x=XPos.LMARGIN, new_y=YPos.NEXT)

        self.set_text_color(55, 65, 81)
        self.ln(4)

    def info_box(self, text, color='blue'):
        colors = {
            'blue': (219, 234, 254, 37, 99, 235),
            'green': (220, 252, 231, 22, 163, 74),
            'yellow': (254, 249, 195, 161, 98, 7),
            'red': (254, 226, 226, 185, 28, 28),
        }
        bg = colors.get(color, colors['blue'])
        self.set_fill_color(bg[0], bg[1], bg[2])
        self.set_text_color(bg[3], bg[4], bg[5])
        self.set_font('Arial', '', 9)
        h = max(10, len(text) // 80 * 6 + 10)
        self.set_x(20)
        self.multi_cell(170, 6, text, fill=True, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(55, 65, 81)
        self.ln(3)

    def simple_table(self, headers, rows, col_widths=None):
        if col_widths is None:
            w = 170 // len(headers)
            col_widths = [w] * len(headers)

        # Header
        self.set_fill_color(99, 102, 241)
        self.set_text_color(255, 255, 255)
        self.set_font('Arial', 'B', 9)
        for i, h in enumerate(headers):
            self.cell(col_widths[i], 8, h, border=0, fill=True)
        self.ln()

        # Rows
        self.set_font('Arial', '', 9)
        for idx, row in enumerate(rows):
            if idx % 2 == 0:
                self.set_fill_color(248, 250, 252)
            else:
                self.set_fill_color(255, 255, 255)
            self.set_text_color(55, 65, 81)
            for i, cell in enumerate(row):
                self.cell(col_widths[i], 7, str(cell), border=0, fill=True)
            self.ln()

        self.set_text_color(55, 65, 81)
        self.ln(4)

    def bullet(self, text, level=0):
        self.set_font('Arial', '', 10)
        self.set_text_color(55, 65, 81)
        indent = 20 + level * 8
        self.set_x(indent)
        bullet_char = '•' if level == 0 else '-'
        self.cell(6, 6, bullet_char)
        self.multi_cell(170 - level * 8 - 6, 6, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)


def generate_pdf():
    pdf = ISUPDocPDF()
    pdf.set_title("ISUP Server - To'liq Texnik Hujjat")
    pdf.set_author("ISUP Server Team")

    # ═══════════════════════════════════════
    # MUQOVA
    # ═══════════════════════════════════════
    pdf.cover_page()

    # ═══════════════════════════════════════
    # 1. TIZIM HAQIDA
    # ═══════════════════════════════════════
    pdf.chapter_title("1. Tizim haqida")

    pdf.body_text(
        "ISUP Server — Hikvision kompaniyasining Face ID terminallari, IP kameralar, "
        "NVR/DVR qurilmalar va kirish nazorat tizimlari bilan ishlash uchun yaratilgan "
        "universal boshqaruv serveri. Tizim HikCentral dasturining asosiy funksiyalarini "
        "o'z ichiga oladi va ochiq API orqali istalgan backend tizimiga integratsiya qilish mumkin."
    )

    pdf.section_title("Asosiy imkoniyatlar")
    pdf.simple_table(
        ['Imkoniyat', 'Tavsif'],
        [
            ['ISUP protokoli', 'Hikvision qurilmalar bilan TCP ulanish (v1 va v5)'],
            ['Face ID', 'Xodimlarni ro\'yxatdan o\'tkazish, o\'chirish, qidirish'],
            ['Kirish nazorati', 'Eshik ochish/yopish, vaqtli ochish, blacklist/whitelist'],
            ['Kamera boshqaruvi', 'PTZ, yozib olish, tasvir sozlamalari'],
            ['Davomat', 'Real-time hodisalar, Excel/PDF hisobotlar'],
            ['Webhook', 'Har qanday backend tizimiga event yuborish'],
            ['WebSocket', 'Real-time monitoring va live eventlar'],
            ['SADP Discovery', 'Tarmoqdagi Hikvision qurilmalarni avtomatik topish'],
            ['Cloudflare Tunnel', 'Xavfsiz internet ulanish (HTTPS)'],
        ],
        [60, 110]
    )

    pdf.section_title("Texnologiyalar")
    pdf.simple_table(
        ['Qism', 'Texnologiya'],
        [
            ['Backend', 'Java 17, Spring Boot 3.2, Netty 4.1'],
            ['Ma\'lumotlar bazasi', 'PostgreSQL 16 + Flyway migration'],
            ['TCP Server', 'Netty (ISUP protokoli)'],
            ['HTTP Client', 'OkHttp 4.12 (ISAPI)'],
            ['Hisobotlar', 'Apache POI (Excel), iText (PDF)'],
            ['Frontend', 'React 18, TypeScript, Tailwind CSS v3'],
            ['State', 'Zustand, React Query v5'],
            ['Real-time', 'SockJS + STOMP (WebSocket)'],
            ['Infratuzilma', 'Docker, Nginx, Cloudflare Tunnel'],
        ],
        [60, 110]
    )

    # ═══════════════════════════════════════
    # 2. ARXITEKTURA
    # ═══════════════════════════════════════
    pdf.chapter_title("2. Arxitektura")

    pdf.section_title("Tizim sxemasi")
    pdf.code_block("""Internet (HTTPS)
    |
Cloudflare (fake-faceid.uzinc.uz)
    |
    +-- cloudflared (Docker) -----> isup-server:8090
    |                                  |-- REST API
    |                                  |-- WebSocket (STOMP)
    |                                  |-- React SPA serve
    |                                  |
    |                               postgres:5432
    |
    +-- frontend:80 (nginx) [port 8888 - local]

Hikvision qurilmalar:
    Face ID Terminal  ----TCP:7660----> ISUP TCP Server
    IP Kamera         ----TCP:7660----> ISUP TCP Server
    NVR               ----TCP:7660----> ISUP TCP Server

                        UDP:37020
    Hikvision qurilma <-----------> SADP Discovery""")

    pdf.section_title("Portlar")
    pdf.simple_table(
        ['Port', 'Protokol', 'Maqsad'],
        [
            ['7660', 'TCP', 'ISUP qurilma ulanishi'],
            ['8090', 'HTTP', 'REST API + React UI'],
            ['8888', 'HTTP', 'Frontend nginx (local test)'],
            ['37020', 'UDP', 'SADP qurilma topish'],
            ['4040', 'HTTP', 'Ngrok boshqaruv paneli'],
        ],
        [30, 35, 105]
    )

    # ═══════════════════════════════════════
    # 3. O'RNATISH
    # ═══════════════════════════════════════
    pdf.chapter_title("3. O'rnatish va ishga tushirish")

    pdf.section_title("Talablar")
    pdf.bullet("Docker Desktop 4.x yoki yuqori versiya")
    pdf.bullet("2 GB RAM (minimum), 4 GB tavsiya etiladi")
    pdf.bullet("Internet ulanish (Cloudflare tunnel uchun)")
    pdf.bullet("Port 7660 (TCP) va 37020 (UDP) ochiq bo'lishi")

    pdf.section_title("Ishga tushirish")
    pdf.code_block("""# 1. Papkaga o'ting
cd "D:/ISUP server java"

# 2. Barcha containerlarni ishga tushirish
docker compose up -d

# 3. Holat tekshirish
docker compose ps

# 4. Server sog'lig'ini tekshirish
curl http://localhost:8090/health""")

    pdf.section_title("Kutilgan javob")
    pdf.code_block("""{
  "status": "UP",
  "isup_server": {"status": "UP", "port": 7660},
  "database": {"status": "UP"},
  "uptime_seconds": 30,
  "memory_mb": 47
}""")

    pdf.section_title("Muhim buyruqlar")
    pdf.code_block("""# Loglarni ko'rish
docker logs isupserverjava-isup-server-1 --tail 100 -f

# Faqat server qayta ishga tushirish
docker compose restart isup-server

# To'liq qayta qurilish
docker compose build --no-cache isup-server
docker compose up -d isup-server

# DB ga kirish
docker exec -it isupserverjava-postgres-1 psql -U isup -d isup_db

# Containerlar holati
docker compose ps""")

    # ═══════════════════════════════════════
    # 4. KIRISH
    # ═══════════════════════════════════════
    pdf.chapter_title("4. Kirish va autentifikatsiya")

    pdf.section_title("Web interfeys orqali")
    pdf.body_text("Brauzerda oching: https://fake-faceid.uzinc.uz")
    pdf.simple_table(
        ['Maydon', 'Qiymat'],
        [
            ['Login', 'admin'],
            ['Parol', 'admin123'],
        ],
        [50, 120]
    )

    pdf.section_title("API orqali login")
    pdf.code_block("""POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

# Javob:
{
  "token": "changeme123",
  "username": "admin"
}

# Har bir so'rovda:
Authorization: Bearer changeme123""")

    pdf.section_title("Parolni o'zgartirish")
    pdf.body_text(".env faylini tahrirlang:")
    pdf.code_block("""ADMIN_USERNAME=admin
ADMIN_PASSWORD=YangiMustahkamParol123!
ADMIN_SECRET=YangiSecretKey456!""")
    pdf.body_text("Keyin serverni qayta ishga tushiring: docker compose up -d isup-server")

    # ═══════════════════════════════════════
    # 5. QURILMALAR
    # ═══════════════════════════════════════
    pdf.chapter_title("5. Qurilmalar boshqaruvi")

    pdf.section_title("Hikvision qurilmasida ISUP sozlash")
    pdf.body_text("Qurilma boshqaruv paneliga kiring (brauzerda qurilma IP si), so'ng:")
    pdf.code_block("""Configuration → Network → Advanced Settings → Platform Access
  Platform Access Mode: Other
    Server Address: [server IP manzili, masalan: 192.168.1.35]
    Port: 7660
    Device ID: admin  (yoki qurilma serial raqami)
    Key: (bo'sh qoldiring)""")

    pdf.section_title("Qurilma qo'shish (API)")
    pdf.code_block("""POST /api/devices
Authorization: Bearer changeme123
Content-Type: application/json

{
  "deviceId": "device001",
  "name": "1-qavat kirish eshigi",
  "location": "1-bino, 1-qavat",
  "deviceIp": "192.168.1.100",
  "deviceUsername": "admin",
  "devicePassword": "admin12345",
  "projectId": 1
}""")

    pdf.section_title("Qurilma imkoniyatlari")
    pdf.simple_table(
        ['Imkoniyat', 'Tavsif'],
        [
            ['face', 'Yuz tanish va ro\'yxatga olish'],
            ['door', 'Eshik ochish/yopish'],
            ['attendance', 'Davomat hisobi'],
            ['rtsp', 'Video oqim (RTSP)'],
            ['ptz', 'Pan/Tilt/Zoom kamera boshqaruvi'],
            ['nvr', 'Network Video Recorder'],
            ['dvr', 'Digital Video Recorder'],
            ['recording', 'Yozib olish funksiyasi'],
            ['motion', 'Harakat aniqlash'],
            ['line_crossing', 'Chiziq kesib o\'tish aniqlash'],
            ['card', 'Karta orqali kirish'],
            ['alarm', 'Signal kirish/chiqish'],
            ['thermal', 'Termal tasvirlash'],
            ['temperature', 'Harorat o\'lchash'],
        ],
        [40, 130]
    )

    # ═══════════════════════════════════════
    # 6. LOYIHALAR
    # ═══════════════════════════════════════
    pdf.chapter_title("6. Loyihalar (Projects)")

    pdf.body_text(
        "Loyihalar — qurilmalarni guruhlash va webhook integratsiyasi uchun asosiy tushuncha. "
        "Har bir loyiha alohida webhook URL va secret key ga ega."
    )

    pdf.section_title("Loyiha yaratish")
    pdf.code_block("""POST /api/projects
Authorization: Bearer changeme123

{
  "name": "Ofis binosi",
  "webhookUrl": "https://sizning-saytingiz.uz/webhook",
  "retryCount": 3
}""")

    pdf.section_title("Loyiha tuzilmasi")
    pdf.code_block("""Loyiha: "Ofis binosi"
  |-- Qurilma: 1-qavat kirish (DS-K1T343)
  |-- Qurilma: 2-qavat kirish (DS-K1T343)
  |-- Qurilma: Xavfsizlik kamerasi (DS-2CD)
  +-- Qurilma: Kutubxona eshigi (DS-K1T343)""")

    # ═══════════════════════════════════════
    # 7. YUZ BOSHQARUVI
    # ═══════════════════════════════════════
    pdf.chapter_title("7. Yuz boshqaruvi (Face ID)")

    pdf.section_title("Xodim qo'shish — API")
    pdf.code_block("""# Bitta qurilmaga
POST /api/faces/enroll
{
  "deviceId": "admin",
  "employeeNo": "EMP001",
  "name": "Jasurbek Toshmatov",
  "gender": "male",
  "photoBase64": "/9j/4AAQSkZJRgAB..."
}

# Barcha online qurilmalarga bir vaqtda
POST /api/faces/enroll-all
{
  "employeeNo": "EMP001",
  "name": "Jasurbek Toshmatov",
  "gender": "male",
  "photoBase64": "/9j/4AAQSkZJRgAB..."
}

# O'chirish (barcha qurilmalardan)
DELETE /api/faces/EMP001

# Qurilmadagi yuzlar ro'yxati
GET /api/faces?deviceId=admin""")

    pdf.section_title("Foto talablari")
    pdf.simple_table(
        ['Parametr', 'Talab'],
        [
            ['Format', 'JPEG/JPG'],
            ['Hajm', '200KB dan kichik (tavsiya)'],
            ["O'lcham", '240x320 px yoki kattaroq'],
            ['Holat', 'Yuzning 80% ko\'rinib turishi'],
            ['Kodlash', 'Base64 (UTF-8)'],
        ],
        [50, 120]
    )

    # ═══════════════════════════════════════
    # 8. KIRISH NAZORATI
    # ═══════════════════════════════════════
    pdf.chapter_title("8. Kirish nazorati (Access Control)")

    pdf.section_title("Eshik boshqaruvi")
    pdf.code_block("""# Eshikni ochish
POST /api/access/door/open
{"deviceId": "admin", "doorNo": 1}

# Eshikni yopish
POST /api/access/door/close
{"deviceId": "admin", "doorNo": 1}

# Vaqtli ochish (10 soniya)
POST /api/access/door/open-timed
{"deviceId": "admin", "doorNo": 1, "seconds": 10}""")

    pdf.section_title("Blacklist / Whitelist")
    pdf.code_block("""# Blacklistga qo'shish (kirish taqiqlanadi)
POST /api/access/blacklist
{
  "employeeNo": "EMP999",
  "deviceIds": null,
  "reason": "Shartnoma tugadi"
}

# Blacklistdan o'chirish
DELETE /api/access/blacklist/EMP999

# Whitelist (vaqtli ruxsat)
POST /api/access/whitelist
{
  "employeeNo": "EMP100",
  "deviceIds": ["admin", "device002"],
  "validFrom": "2024-01-15T08:00:00Z",
  "validTo": "2024-01-15T18:00:00Z"
}""")

    # ═══════════════════════════════════════
    # 9. KAMERA & PTZ
    # ═══════════════════════════════════════
    pdf.chapter_title("9. Kamera va PTZ boshqaruvi")

    pdf.section_title("Tasvir sozlamalari")
    pdf.code_block("""# Kamera sozlamalarini olish
GET /api/camera/{deviceId}/image/{channelId}

# Sozlamalarni yangilash
PUT /api/camera/{deviceId}/image/{channelId}
{"brightness": 50, "contrast": 50, "saturation": 50}

# Video kanallar ro'yxati
GET /api/camera/{deviceId}/streams""")

    pdf.section_title("PTZ boshqaruvi (Pan/Tilt/Zoom)")
    pdf.code_block("""# Kamerani burish
POST /api/camera/{deviceId}/ptz/{channelId}/move
{"direction": "RIGHT", "speed": 50}

# Harakatni to'xtatish
POST /api/camera/{deviceId}/ptz/{channelId}/stop

# Presetga o'tish
POST /api/camera/{deviceId}/ptz/{channelId}/preset/{presetId}/goto

# Presetlar ro'yxati
GET /api/camera/{deviceId}/ptz/{channelId}/presets""")

    pdf.simple_table(
        ['Direction', 'Ma\'no'],
        [
            ['LEFT', 'Chapga burish'],
            ['RIGHT', "O'ngga burish"],
            ['UP', 'Yuqoriga'],
            ['DOWN', 'Pastga'],
            ['ZOOM_IN', 'Yaqinlashtirish'],
            ['ZOOM_OUT', 'Uzoqlashtirish'],
        ],
        [50, 120]
    )

    pdf.section_title("RTSP Video oqim")
    pdf.code_block("""# RTSP URL olish
GET /api/stream/{deviceId}

# Javob:
{
  "rtspUrl": "rtsp://admin:admin12345@192.168.1.100:554/Streaming/Channels/101",
  "rtspSubUrl": "rtsp://admin:admin12345@192.168.1.100:554/Streaming/Channels/102"
}""")

    # ═══════════════════════════════════════
    # 10. QURILMA KONFIGURATSIYASI
    # ═══════════════════════════════════════
    pdf.chapter_title("10. Qurilma sozlamalari va monitoring")

    pdf.code_block("""# Tizim ma'lumotlari
GET /api/device-config/{deviceId}/info

# CPU, xotira, harorat
GET /api/device-config/{deviceId}/status

# Tarmoq sozlamalari
GET /api/device-config/{deviceId}/network

# Disk ma'lumotlari
GET /api/device-config/{deviceId}/storage

# Qayta ishga tushirish (ehtiyot bilan!)
POST /api/device-config/{deviceId}/reboot

# Signal (alarm) portlari
GET /api/device-config/{deviceId}/alarm/inputs
POST /api/device-config/{deviceId}/alarm/outputs/{id}/trigger

# Yozilgan videolar qidirish
GET /api/device-config/{deviceId}/recordings?channelId=101&startTime=...&endTime=...

# Qurilmadagi foydalanuvchilar
GET /api/device-config/{deviceId}/users
POST /api/device-config/{deviceId}/users
DELETE /api/device-config/{deviceId}/users/{employeeNo}""")

    # ═══════════════════════════════════════
    # 11. EVENTLAR
    # ═══════════════════════════════════════
    pdf.chapter_title("11. Eventlar va davomat")

    pdf.section_title("Event turlari")
    pdf.simple_table(
        ['Event turi', 'Tavsif'],
        [
            ['attendance', 'Xodim kirish/chiqish'],
            ['door', 'Eshik ochildi/yopildi'],
            ['alarm', 'Signal berdi'],
            ['motion', 'Harakat aniqlandi'],
            ['line_crossing', 'Chiziq kesib o\'tildi'],
            ['face_detection', 'Yuz aniqlandi'],
            ['intrusion', 'Ruxsatsiz kirish'],
        ],
        [50, 120]
    )

    pdf.section_title("Eventlarni so'rash")
    pdf.code_block("""# Barcha eventlar (sahifalab)
GET /api/events?page=0&size=20

# Vaqt oralig'i bo'yicha
GET /api/events?startTime=2024-01-15T00:00:00Z&endTime=2024-01-15T23:59:59Z

# Qurilma bo'yicha
GET /api/events?deviceId=admin

# Xodim bo'yicha
GET /api/events?employeeNo=EMP001

# Event turi bo'yicha
GET /api/events?eventType=attendance""")

    # ═══════════════════════════════════════
    # 12. HISOBOTLAR
    # ═══════════════════════════════════════
    pdf.chapter_title("12. Hisobotlar (Excel / PDF)")

    pdf.code_block("""# Kunlik davomat - Excel
GET /api/reports/attendance/daily?date=2024-01-15

# Kunlik davomat - PDF
GET /api/reports/attendance/daily?date=2024-01-15&format=pdf

# Oylik davomat
GET /api/reports/attendance/monthly?month=2024-01

# Xodim tarixi
GET /api/reports/attendance/employee/EMP001?from=2024-01-01&to=2024-01-31""")

    pdf.section_title("Excel hisobot ustunlari")
    pdf.simple_table(
        ['Ustun', "Ma'no"],
        [
            ['Xodim ID', 'Xodim identifikatori'],
            ['Ism', "To'liq ism"],
            ['Kirish', 'Birinchi kirish vaqti'],
            ['Chiqish', 'Oxirgi chiqish vaqti'],
            ['Ishlagan soat', 'Jami ish soati'],
            ['Qurilma', 'Qaysi qurilmadan kirgan'],
            ['Tasdiqlash usuli', 'Face / Card / Fingerprint'],
        ],
        [45, 125]
    )

    # ═══════════════════════════════════════
    # 13. WEBHOOK
    # ═══════════════════════════════════════
    pdf.chapter_title("13. Webhook integratsiya")

    pdf.body_text(
        "Webhook — qurilmadan event kelganda sizning serveringizga avtomatik "
        "POST so'rovi yuborilishi. Django, Laravel, Node.js yoki boshqa har qanday "
        "framework bilan ishlash mumkin."
    )

    pdf.section_title("Webhook payload")
    pdf.code_block("""{
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
}""")

    pdf.section_title("HMAC imzo tekshirish (Django)")
    pdf.code_block("""import hmac, hashlib, json
from django.views.decorators.csrf import csrf_exempt

def verify_hmac(body, signature, secret):
    expected = hmac.new(
        secret.encode(), body.encode(), hashlib.sha256
    ).hexdigest()
    return signature == f"sha256={expected}"

@csrf_exempt
def isup_webhook(request):
    sig = request.headers.get('X-ISUP-Signature', '')
    body = request.body.decode('utf-8')

    if not verify_hmac(body, sig, 'sizning-secret-key'):
        return JsonResponse({'error': 'Invalid signature'}, status=401)

    data = json.loads(body)
    emp_no = data.get('employee_no')
    direction = data.get('direction')  # 'in' yoki 'out'
    photo = data.get('photo_base64')

    # ... hodisani qayta ishlash
    return JsonResponse({'status': 'ok'})""")

    # ═══════════════════════════════════════
    # 14. QOLLAB QURILMALAR
    # ═══════════════════════════════════════
    pdf.chapter_title("14. Qo'llab-quvvatlanadigan qurilmalar")

    pdf.section_title("Face ID terminallar")
    pdf.simple_table(
        ['Model', 'Imkoniyatlar'],
        [
            ['DS-K1T343EWX', 'face, door, attendance, rtsp'],
            ['DS-K1T343MWX', 'face, door, attendance, rtsp'],
            ['DS-K1T671MWX', 'face, door, attendance, temperature'],
            ['DS-K1T804MWX', 'face, door, attendance, card'],
            ['DS-K1TA70MIW', 'face, door, attendance, temperature, alarm'],
        ],
        [65, 105]
    )

    pdf.section_title("IP Kameralar")
    pdf.simple_table(
        ['Seriya', 'Imkoniyatlar'],
        [
            ['DS-2CD1xxx', 'rtsp, motion'],
            ['DS-2CD2xxx', 'rtsp, motion, line_crossing'],
            ['DS-2CD3xxx', 'rtsp, motion, face, line_crossing'],
            ['DS-2DE3xx (PTZ)', 'rtsp, ptz, motion'],
            ['DS-2DF8xx (PTZ)', 'rtsp, ptz, motion, line_crossing'],
            ['DS-2TD2xxx (Thermal)', 'rtsp, thermal, motion'],
            ['iDS-2CD7xx (Smart)', 'rtsp, face, motion, line_crossing'],
        ],
        [65, 105]
    )

    pdf.section_title("NVR / DVR")
    pdf.simple_table(
        ['Model', 'Imkoniyatlar'],
        [
            ['DS-7608/7616/7632NI', 'nvr, recording, playback, rtsp'],
            ['DS-9632NI-M8', 'nvr, recording, playback, rtsp'],
            ['DS-8616SHI-ST', 'dvr, recording, playback, rtsp'],
        ],
        [65, 105]
    )

    pdf.section_title("Kirish nazorat panellari")
    pdf.simple_table(
        ['Model', 'Imkoniyatlar'],
        [
            ['DS-K2604T', 'door, card, alarm'],
            ['DS-K2602T', 'door, card, alarm'],
            ['DS-K2M061', 'door, card, alarm'],
        ],
        [65, 105]
    )

    # ═══════════════════════════════════════
    # 15. XATOLIKLARNI BARTARAF ETISH
    # ═══════════════════════════════════════
    pdf.chapter_title("15. Xatoliklarni bartaraf etish")

    pdf.section_title("Qurilma ulanmayapti")
    pdf.info_box(
        "Belgilari: Server logida qurilma ko'rinmaydi, qurilma offline ko'rsatiladi",
        'yellow'
    )
    pdf.code_block("""# 1. Server loglarini ko'rish
docker logs isupserverjava-isup-server-1 --tail 50

# 2. TCP portni tekshirish
netstat -an | grep 7660

# 3. Qurilmadan server IP ni ping qilish
ping [server_ip]  # qurilma terminalidan""")
    pdf.body_text("Qurilma sozlamalarida Server Address to'g'ri ekanligini va port 7660 ochiq ekanligini tekshiring.")

    pdf.section_title("ISAPI xatosi (Face/Door API)")
    pdf.info_box("Belgilari: 'Device has no IP address' xatosi", 'red')
    pdf.body_text(
        "Yechim: Qurilmani tahrirlang va deviceIp maydonini to'ldiring. "
        "IP manzil server bilan bir tarmoqda bo'lishi kerak."
    )

    pdf.section_title("Webhook kelmayapti")
    pdf.code_block("""# Failed webhooks tekshirish
GET /api/events?webhookStatus=failed

# Webhook test
curl -X POST [sizning_webhook_url] -H "Content-Type: application/json" -d '{}'""")

    pdf.section_title("Konteyner ishlamayapti")
    pdf.code_block("""# Barcha loglar
docker compose logs --tail 30

# Qayta ishga tushirish
docker compose restart

# To'liq o'chirib yoqish
docker compose down && docker compose up -d""")

    # ═══════════════════════════════════════
    # 16. API TO'LIQ RO'YXATI
    # ═══════════════════════════════════════
    pdf.chapter_title("16. API to'liq ro'yxati")

    pdf.section_title("Autentifikatsiya")
    pdf.simple_table(
        ['Metod', 'URL', 'Tavsif'],
        [['POST', '/api/auth/login', 'Kirish (username + password)']],
        [15, 80, 75]
    )

    pdf.section_title("Qurilmalar")
    pdf.simple_table(
        ['Metod', 'URL', 'Tavsif'],
        [
            ['GET', '/api/devices', 'Barcha qurilmalar'],
            ['GET', '/api/devices/online', 'Online qurilmalar'],
            ['GET', '/api/devices/{id}', 'Bitta qurilma'],
            ['POST', '/api/devices', 'Qurilma qo\'shish'],
            ['PUT', '/api/devices/{id}', 'Yangilash'],
            ['DELETE', '/api/devices/{id}', "O'chirish"],
        ],
        [15, 80, 75]
    )

    pdf.section_title("Loyihalar")
    pdf.simple_table(
        ['Metod', 'URL', 'Tavsif'],
        [
            ['GET', '/api/projects', 'Barcha loyihalar'],
            ['POST', '/api/projects', 'Loyiha yaratish'],
            ['PUT', '/api/projects/{id}', 'Yangilash'],
            ['DELETE', '/api/projects/{id}', "O'chirish"],
            ['POST', '/api/projects/{id}/regenerate-secret', 'Secret yangilash'],
        ],
        [15, 90, 65]
    )

    pdf.section_title("Yuz boshqaruvi")
    pdf.simple_table(
        ['Metod', 'URL', 'Tavsif'],
        [
            ['POST', '/api/faces/enroll', 'Bitta qurilmaga yuz yuklash'],
            ['POST', '/api/faces/enroll-all', 'Barcha qurilmalarga'],
            ['DELETE', '/api/faces/{empNo}', "Barcha qurilmalardan o'chirish"],
            ['GET', '/api/faces?deviceId=x', 'Qurilmadagi yuzlar'],
        ],
        [15, 85, 70]
    )

    pdf.section_title("Kirish nazorati")
    pdf.simple_table(
        ['Metod', 'URL', 'Tavsif'],
        [
            ['POST', '/api/access/door/open', 'Eshik ochish'],
            ['POST', '/api/access/door/close', 'Eshik yopish'],
            ['POST', '/api/access/door/open-timed', 'Vaqtli ochish'],
            ['POST', '/api/access/blacklist', 'Blacklistga qo\'shish'],
            ['DELETE', '/api/access/blacklist/{no}', "Blacklistdan o'chirish"],
            ['GET', '/api/access/blacklist', 'Blacklist ro\'yxati'],
            ['POST', '/api/access/whitelist', 'Whitelist qo\'shish'],
        ],
        [15, 90, 65]
    )

    pdf.section_title("Kamera va PTZ")
    pdf.simple_table(
        ['Metod', 'URL', 'Tavsif'],
        [
            ['GET', '/api/camera/{did}/image/{ch}', 'Tasvir sozlamalari'],
            ['PUT', '/api/camera/{did}/image/{ch}', 'Sozlamalarni yangilash'],
            ['GET', '/api/camera/{did}/streams', 'Video kanallar'],
            ['POST', '/api/camera/{did}/ptz/{ch}/move', 'PTZ harakatlantirish'],
            ['POST', '/api/camera/{did}/ptz/{ch}/stop', 'PTZ to\'xtatish'],
            ['POST', '/api/camera/{did}/ptz/{ch}/preset/{n}/goto', 'Presetga o\'tish'],
            ['GET', '/api/camera/{did}/ptz/{ch}/presets', 'Presetlar'],
            ['GET', '/api/stream/{deviceId}', 'RTSP URL'],
        ],
        [15, 95, 60]
    )

    pdf.section_title("Qurilma konfiguratsiyasi")
    pdf.simple_table(
        ['Metod', 'URL', 'Tavsif'],
        [
            ['GET', '/api/device-config/{did}/info', 'Tizim ma\'lumotlari'],
            ['GET', '/api/device-config/{did}/status', 'Holat (CPU, xotira)'],
            ['GET', '/api/device-config/{did}/network', 'Tarmoq sozlamalari'],
            ['GET', '/api/device-config/{did}/storage', 'Disk ma\'lumotlari'],
            ['POST', '/api/device-config/{did}/reboot', 'Qayta ishga tushirish'],
            ['GET', '/api/device-config/{did}/alarm/inputs', 'Signal kirish'],
            ['POST', '/api/device-config/{did}/alarm/outputs/{n}/trigger', 'Signal'],
            ['GET', '/api/device-config/{did}/motion/{ch}', 'Harakat aniqlash'],
            ['GET', '/api/device-config/{did}/recordings', 'Yozilgan videolar'],
            ['GET', '/api/device-config/{did}/users', 'Foydalanuvchilar'],
            ['POST', '/api/device-config/{did}/users', 'Foydalanuvchi qo\'shish'],
            ['DELETE', '/api/device-config/{did}/users/{empNo}', "O'chirish"],
        ],
        [15, 100, 55]
    )

    pdf.section_title("Hisobotlar va boshqa")
    pdf.simple_table(
        ['Metod', 'URL', 'Tavsif'],
        [
            ['GET', '/api/events', 'Eventlar (filter + pagination)'],
            ['GET', '/api/reports/attendance/daily', 'Kunlik hisobot'],
            ['GET', '/api/reports/attendance/monthly', 'Oylik hisobot'],
            ['GET', '/api/reports/attendance/employee/{no}', 'Xodim tarixi'],
            ['POST', '/api/discovery/scan', 'Qurilma qidirish'],
            ['GET', '/api/discovery/devices', 'Topilgan qurilmalar'],
            ['GET', '/health', 'Server holati'],
            ['GET', '/metrics', 'Prometheus metrikalar'],
        ],
        [15, 95, 60]
    )

    # Save
    output_path = "D:/ISUP server java/docs/ISUP_Server_Documentation.pdf"
    pdf.output(output_path)
    print(f"PDF muvaffaqiyatli yaratildi: {output_path}")
    print(f"Sahifalar soni: {pdf.page_no()}")

if __name__ == '__main__':
    generate_pdf()
