package com.isup.report;

import com.isup.entity.EventLog;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class PdfReportGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public byte[] generate(String title, List<EventLog> events) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.DARK_GRAY);
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(10);
        doc.add(titlePara);

        // Generated time
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY);
        doc.add(new Paragraph("Hisobot vaqti: " + FMT.format(java.time.Instant.now()), smallFont));
        doc.add(Chunk.NEWLINE);

        // Table
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 1.5f, 2f, 2f, 2f, 1.5f, 2f});

        // Headers
        BaseColor headerBg = new BaseColor(25, 118, 210);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        String[] headers = {"#", "Xodim No", "Ism", "Kirish", "Chiqish", "Soat", "Qurilma"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // Data rows
        Font dataFont = new Font(Font.FontFamily.HELVETICA, 9);
        Map<String, List<EventLog>> byEmployee = new LinkedHashMap<>();
        for (EventLog e : events) {
            byEmployee.computeIfAbsent(
                e.getEmployeeNo() != null ? e.getEmployeeNo() : "unknown",
                k -> new ArrayList<>()
            ).add(e);
        }

        int seq = 1;
        boolean alt = false;
        for (Map.Entry<String, List<EventLog>> entry : byEmployee.entrySet()) {
            List<EventLog> empEvents = entry.getValue();
            EventLog checkIn  = empEvents.stream().filter(e -> "in".equals(e.getDirection())).findFirst().orElse(null);
            EventLog checkOut = empEvents.stream().filter(e -> "out".equals(e.getDirection())).findFirst().orElse(null);

            BaseColor rowBg = alt ? new BaseColor(245, 245, 245) : BaseColor.WHITE;
            alt = !alt;

            String[] row = {
                String.valueOf(seq++),
                entry.getKey(),
                checkIn != null && checkIn.getEmployeeName() != null ? checkIn.getEmployeeName() : "-",
                checkIn != null && checkIn.getEventTime() != null ? FMT.format(checkIn.getEventTime()) : "-",
                checkOut != null && checkOut.getEventTime() != null ? FMT.format(checkOut.getEventTime()) : "-",
                calcHours(checkIn, checkOut),
                checkIn != null ? checkIn.getDeviceId() : "-"
            };
            for (String val : row) {
                PdfPCell cell = new PdfPCell(new Phrase(val, dataFont));
                cell.setBackgroundColor(rowBg);
                cell.setPadding(5);
                table.addCell(cell);
            }
        }
        doc.add(table);

        // Footer
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Jami: " + byEmployee.size() + " xodim", smallFont));

        doc.close();
        return out.toByteArray();
    }

    private String calcHours(EventLog in, EventLog out) {
        if (in == null || out == null || in.getEventTime() == null || out.getEventTime() == null) return "-";
        long secs = out.getEventTime().getEpochSecond() - in.getEventTime().getEpochSecond();
        if (secs <= 0) return "-";
        return String.format("%d:%02d", secs / 3600, (secs % 3600) / 60);
    }
}
