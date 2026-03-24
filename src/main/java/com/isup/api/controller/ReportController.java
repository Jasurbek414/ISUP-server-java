package com.isup.api.controller;

import com.isup.report.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** GET /api/reports/attendance/daily?date=2024-01-15&format=excel|pdf */
    @GetMapping("/attendance/daily")
    public ResponseEntity<byte[]> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "excel") String format) throws Exception {

        if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = reportService.dailyPdf(date);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"attendance_" + date + ".pdf\"")
                    .body(pdf);
        }
        byte[] excel = reportService.dailyExcel(date);
        return excelResponse(excel, "attendance_" + date + ".xlsx");
    }

    /** GET /api/reports/attendance/monthly?month=2024-01 */
    @GetMapping("/attendance/monthly")
    public ResponseEntity<byte[]> monthly(@RequestParam String month) throws Exception {
        String[] parts = month.split("-");
        byte[] excel = reportService.monthlyExcel(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        return excelResponse(excel, "attendance_" + month + ".xlsx");
    }

    /** GET /api/reports/attendance/employee/{empNo}?from=2024-01-01&to=2024-01-31 */
    @GetMapping("/attendance/employee/{employeeNo}")
    public ResponseEntity<byte[]> employee(
            @PathVariable String employeeNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws Exception {

        byte[] excel = reportService.employeeExcel(employeeNo, from, to);
        return excelResponse(excel, "employee_" + employeeNo + ".xlsx");
    }

    private ResponseEntity<byte[]> excelResponse(byte[] data, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data);
    }
}
