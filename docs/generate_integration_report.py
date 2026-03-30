#!/usr/bin/env python3
from fpdf import FPDF
from fpdf.enums import XPos, YPos
import os

FONT_REGULAR = "C:/Windows/Fonts/arial.ttf"
FONT_BOLD    = "C:/Windows/Fonts/arialbd.ttf"

class IntegrationReportPDF(FPDF):
    def __init__(self):
        super().__init__('P', 'mm', 'A4')
        self.set_auto_page_break(auto=True, margin=20)
        self.set_margins(20, 20, 20)
        self.add_font('Arial', style='',  fname=FONT_REGULAR)
        self.add_font('Arial', style='B', fname=FONT_BOLD)

    def header(self):
        if self.page_no() > 1:
            self.set_font('Arial', '', 8)
            self.set_text_color(100)
            self.cell(0, 10, "Hikvision Face ID Integratsiya Hisoboti | ISUP Server Java", 
                      new_x=XPos.LMARGIN, new_y=YPos.NEXT, align='R')
            self.line(20, self.get_y(), 190, self.get_y())
            self.ln(5)

    def footer(self):
        self.set_y(-15)
        self.set_font('Arial', '', 8)
        self.cell(0, 10, f'Sahifa {self.page_no()}', align='C')

    def cover_page(self):
        self.add_page()
        self.set_fill_color(31, 41, 55) # Dark gray
        self.rect(0, 0, 210, 297, 'F')
        
        self.set_y(100)
        self.set_font('Arial', 'B', 28)
        self.set_text_color(255, 255, 255)
        self.cell(0, 15, 'HIKVISION FACE ID', align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_font('Arial', '', 20)
        self.cell(0, 15, 'INTEGRATSIYA HISOBOTI', align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        
        self.set_y(150)
        self.set_draw_color(255, 255, 255)
        self.line(50, 145, 160, 145)
        
        self.set_font('Arial', '', 12)
        self.cell(0, 10, 'ISUP Server Java v2.0 Loyihasi Analizi', align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        
        self.set_y(250)
        self.set_font('Arial', '', 10)
        self.cell(0, 10, 'Sana: 2026-03-27 | Toshkent, O\'zbekiston', align='C')

    def chapter_title(self, title):
        self.ln(10)
        self.set_font('Arial', 'B', 16)
        self.set_text_color(31, 41, 55)
        self.multi_cell(0, 10, title)
        self.set_draw_color(31, 41, 55)
        self.line(self.get_x(), self.get_y(), self.get_x() + 50, self.get_y())
        self.ln(5)

    def chapter_section(self, title):
        self.ln(5)
        self.set_font('Arial', 'B', 12)
        self.set_text_color(79, 70, 229) # Indigo
        self.cell(0, 10, title, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(2)

    def body_text(self, text):
        self.set_font('Arial', '', 11)
        self.set_text_color(55, 65, 81)
        self.multi_cell(0, 7, text)
        self.ln(2)

    def bullet(self, text):
        self.set_x(25)
        self.set_font('Arial', '', 11)
        self.cell(5, 7, chr(149))
        self.multi_cell(0, 7, text)
        self.ln(1)

def generate():
    pdf = IntegrationReportPDF()
    pdf.set_title("ISUP Integratsiya Hisoboti")
    pdf.cover_page()
    
    # Chapter 1
    pdf.chapter_title("1. Loyiha Haqida Umumiy Ma'lumot")
    pdf.body_text(
        "Mazkur ISUP Server Java loyihasi Hikvision kompaniyasining Face ID terminallarini masofadan "
        "boshqarish va integratsiya qilish uchun mo'ljallangan universal platformadir."
    )
    pdf.chapter_section("Texnik ko'rsatkichlar:")
    pdf.bullet("Java 17 + Spring Boot 3.2 texnologiyalari asosida qurilgan.")
    pdf.bullet("Netty 4.1 framework orqali TCP 7660 portida qurilmalar bilan xavfsiz ulanish (ISUP protocol) o'rnatiladi.")
    pdf.bullet("Real-vaqtda hodisalarni (attendance events) qabul qilish va Webhook orqali tashqi tizimlarga yuborish imkoniyati.")
    pdf.bullet("Xodimlarning yuz ma'lumotlarini (Face templates) qurilmaga yuklash va boshqarish API orqali amalga oshiriladi.")

    # Chapter 2
    pdf.chapter_title("2. Hikvision Face ID Integratsiya Muammolari")
    pdf.body_text(
        "Hikvision qurilmalari bilan ulanishda yuzaga kelayotgan asosiy texnik to'siqlar quyidagilardan iborat:"
    )
    
    pdf.chapter_section("Protokol va Framing Mos kelmasligi:")
    pdf.body_text(
        "Ko'pgina Face ID terminallar (masalan, DS-K1T343) EHome 5.0 (V5) protokolida ishlaydi, "
        "lekin ular ba'zida eski V1 formatidagi headerlar bilan ma'lumot yuboradi. Server bu turli dialektlarni "
        "taniydigan universal dekoderga ega bo'lishi shart."
    )
    
    pdf.chapter_section("Ulanish (Handshake) Murakkabligi:")
    pdf.bullet("One-Step Handshake: Yangi firmwar-lar serverdan darhol XML formatidagi tasdiqni (REG_RESULT) kutadi.")
    pdf.bullet("Two-Step Fallback: Server 401 Challenge (nonce) yuborganda, qurilma uni qabul qilmasligi va ulanishni uzib qo'yishi mumkin.")
    
    pdf.chapter_section("Sid va Session Management:")
    pdf.body_text(
        "Qurilmalar ulanish vaqtida serverdan olingan Session ID (SID) ni keyingi barcha paketlarda kutadi. "
        "Agar server SID ni 0 deb qaytarsa yoki har safar o'zgartirsa, terminal ulanishni barqaror ushlab turolmaydi."
    )

    # Chapter 3
    pdf.chapter_title("3. Ulanmaslikning Asosiy Sabablari (Analiz)")
    pdf.body_text("Reja asosida asosiy sabablar tahlili:")
    
    pdf.chapter_section("I. Tarmoq va Firewall To'siqlari")
    pdf.bullet("Server porti (7660) tarmoq xavfsizlik tizimi (Firewall) tomonidan bloklangan bo'lishi.")
    pdf.bullet("Qurilma va server o'rtasida to'g'ridan-to'g'ri bog'liqlik yo'qligi (IP routing xatoliklari).")
    
    pdf.chapter_section("II. Protokol va Firmware Faktori")
    pdf.bullet("Qurilma proshivkasining o'ta yangiligi va u serverdan maxsus XML taglarini (PPVSPMessage) talab qilishi.")
    pdf.bullet("XML payload o'lchamining V1 framing limitidan (255 bayt) oshib ketishi natijasida paket buzilishi.")

    pdf.chapter_section("III. Identifikatsiya Xatolari")
    pdf.bullet("Device ID noto'g'ri kiritilishi (Qurilmadagi ID server bazasidagi ID bilan mos kelmasligi).")
    pdf.bullet("Qurilmadagi vaqtdagi katta farq (Time Sync) integratsiya xatoligiga sabab bo'lishi mumkin.")

    # Chapter 4
    pdf.chapter_title("4. Muammolarni Bartaraf Etish va Integratsiya Rejasi")
    pdf.body_text("Loyiha barqarorligi uchun quyidagi qadamlar amalga oshirilishi lozim:")
    
    pdf.chapter_section("Qisqa muddatli vazifalar:")
    pdf.bullet("Universal IsupFrameDecoder orqali 0x10 va 0x20 headerlarni barqaror qabul qilish.")
    pdf.bullet("Registration Response Burst: Ulanishda REG_RESULT + SET_TIME + Binary Success paketlarini bir vaqtda yuborish.")
    
    pdf.chapter_section("Uzoq muddatli optimizatsiya:")
    pdf.bullet("Har bir qurilma uchun unique va barqaror Session ID (SID) generatsiyasini joriy etish.")
    pdf.bullet("XML konfiguratsiyalarini o'ta minimal ko'rinishga keltirish (V1 payload limitidan chiqmaslik uchun).")
    
    pdf.add_page()
    pdf.set_y(50)
    pdf.set_font('Arial', 'B', 14)
    pdf.cell(0, 10, "Xulosa", new_x=XPos.LMARGIN, new_y=YPos.NEXT, align='C')
    pdf.ln(5)
    pdf.set_font('Arial', '', 11)
    pdf.multi_cell(0, 8, 
        "Ushbu hisobot Hikvision Face ID terminallarini ISUP server bilan integratsiya qilishda "
        "yuzaga keladigan asosiy texnik to'siqlarni yoritib beradi. Yuqorida ko'rsatilgan choralarni "
        "qo'llash orqali tizim barqarorligini 99% gacha oshirish mumkin."
    )

    output_path = "d:/ISUP server java/docs/ISUP_Integratsiya_Hisoboti.pdf"
    pdf.output(output_path)
    print(f"Hisobot yaratildi: {output_path}")

if __name__ == "__main__":
    generate()
