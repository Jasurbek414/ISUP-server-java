package com.isup.report;

import com.isup.entity.EventLog;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ExcelReportGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public byte[] generate(String title, List<EventLog> events) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Davomat");

            // Styles
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Font boldFont = wb.createFont();
            boldFont.setBold(true);

            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(boldFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dataStyle = wb.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            // Header row
            String[] headers = {"#", "Xodim No", "Ism", "Kirish vaqti", "Chiqish vaqti", "Ishlagan soat", "Qurilma"};
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Group events by employee (in/out pairs)
            Map<String, List<EventLog>> byEmployee = new LinkedHashMap<>();
            for (EventLog e : events) {
                byEmployee.computeIfAbsent(
                    e.getEmployeeNo() != null ? e.getEmployeeNo() : "unknown",
                    k -> new ArrayList<>()
                ).add(e);
            }

            int rowNum = 2;
            int seq = 1;
            for (Map.Entry<String, List<EventLog>> entry : byEmployee.entrySet()) {
                List<EventLog> empEvents = entry.getValue();
                EventLog checkIn = empEvents.stream()
                        .filter(e -> "in".equals(e.getDirection())).findFirst().orElse(null);
                EventLog checkOut = empEvents.stream()
                        .filter(e -> "out".equals(e.getDirection())).findFirst().orElse(null);

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(seq++);
                row.createCell(1).setCellValue(entry.getKey());
                row.createCell(2).setCellValue(checkIn != null && checkIn.getEmployeeName() != null ? checkIn.getEmployeeName() : "-");
                row.createCell(3).setCellValue(checkIn != null && checkIn.getEventTime() != null ? FMT.format(checkIn.getEventTime()) : "-");
                row.createCell(4).setCellValue(checkOut != null && checkOut.getEventTime() != null ? FMT.format(checkOut.getEventTime()) : "-");

                // Calculate hours worked
                String hours = "-";
                if (checkIn != null && checkOut != null && checkIn.getEventTime() != null && checkOut.getEventTime() != null) {
                    long secs = checkOut.getEventTime().getEpochSecond() - checkIn.getEventTime().getEpochSecond();
                    if (secs > 0) hours = String.format("%d:%02d", secs / 3600, (secs % 3600) / 60);
                }
                row.createCell(5).setCellValue(hours);
                row.createCell(6).setCellValue(checkIn != null ? checkIn.getDeviceId() : "-");

                for (int c = 0; c < 7; c++) {
                    row.getCell(c).setCellStyle(dataStyle);
                }
            }

            // Auto size columns
            for (int i = 0; i < 7; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
