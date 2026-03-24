package com.isup.report;

import com.isup.entity.EventLog;
import com.isup.repository.EventLogRepository;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.List;

@Service
public class ReportService {

    private final EventLogRepository eventRepo;
    private final ExcelReportGenerator excelGen;
    private final PdfReportGenerator   pdfGen;

    public ReportService(EventLogRepository eventRepo,
                          ExcelReportGenerator excelGen,
                          PdfReportGenerator pdfGen) {
        this.eventRepo = eventRepo;
        this.excelGen  = excelGen;
        this.pdfGen    = pdfGen;
    }

    /** Daily attendance report as Excel bytes. */
    public byte[] dailyExcel(LocalDate date) throws Exception {
        List<EventLog> events = getDayEvents(date);
        String title = "Kunlik davomat: " + date;
        return excelGen.generate(title, events);
    }

    /** Daily attendance report as PDF bytes. */
    public byte[] dailyPdf(LocalDate date) throws Exception {
        List<EventLog> events = getDayEvents(date);
        String title = "Kunlik davomat: " + date;
        return pdfGen.generate(title, events);
    }

    /** Monthly report as Excel. */
    public byte[] monthlyExcel(int year, int month) throws Exception {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
        List<EventLog> events = getDateRangeEvents(start, end);
        String title = "Oylik davomat: " + year + "-" + String.format("%02d", month);
        return excelGen.generate(title, events);
    }

    /** Employee history report as Excel. */
    public byte[] employeeExcel(String employeeNo, LocalDate from, LocalDate to) throws Exception {
        Instant start = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end   = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<EventLog> events = eventRepo.findByEmployeeNoAndTimeRange(employeeNo, start, end);
        return excelGen.generate("Xodim tarixi: " + employeeNo + " (" + from + " - " + to + ")", events);
    }

    private List<EventLog> getDayEvents(LocalDate date) {
        Instant start = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end   = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return eventRepo.findByTimeRange(start, end);
    }

    private List<EventLog> getDateRangeEvents(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end   = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return eventRepo.findByTimeRange(start, end);
    }
}
