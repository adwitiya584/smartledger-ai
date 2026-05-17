package com.smartledger.report;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final PdfReportService pdfReportService;

    public ReportController(PdfReportService pdfReportService) {
        this.pdfReportService = pdfReportService;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(Authentication auth) {
        byte[] pdf = pdfReportService.generateReport(auth.getName());
        return buildResponse(pdf, "smartledger-report.pdf");
    }

    @GetMapping("/pdf/{year}")
    public ResponseEntity<byte[]> downloadYearlyPdf(
            Authentication auth,
            @PathVariable int year) {
        byte[] pdf = pdfReportService.generateReport(auth.getName(), year);
        return buildResponse(pdf, "smartledger-report-" + year + ".pdf");
    }

    private ResponseEntity<byte[]> buildResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}