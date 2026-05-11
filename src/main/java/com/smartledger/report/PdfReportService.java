package com.smartledger.report;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.smartledger.auth.User;
import com.smartledger.auth.UserRepository;
import com.smartledger.transaction.Transaction;
import com.smartledger.transaction.TransactionRepository;
import com.smartledger.transaction.TransactionType;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public PdfReportService(TransactionRepository transactionRepository,
                             UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public byte[] generateReport(String email) {
        User user = getUser(email);

        BigDecimal income = transactionRepository
                .sumByUserIdAndType(user.getId(), TransactionType.INCOME);
        BigDecimal expense = transactionRepository
                .sumByUserIdAndType(user.getId(), TransactionType.EXPENSE);
        BigDecimal balance = income.subtract(expense);

        List<Transaction> transactions = transactionRepository
                .findByUserIdOrderByTransactionDateDesc(user.getId());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Title
        Paragraph title = new Paragraph("SmartLedger AI — Financial Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY);
        document.add(title);

        // Generated date
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        document.add(new Paragraph("Generated: " + date)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY));

        document.add(new Paragraph("\n"));

        // User info
        document.add(new Paragraph("Account Holder: " + user.getName())
                .setFontSize(12).setBold());
        document.add(new Paragraph("Email: " + user.getEmail())
                .setFontSize(11));

        document.add(new Paragraph("\n"));

        // Summary box
        document.add(new Paragraph("Financial Summary")
                .setFontSize(14).setBold()
                .setFontColor(ColorConstants.DARK_GRAY));

        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth();

        summaryTable.addCell(createCell("Total Income", true));
        summaryTable.addCell(createCell("₹" + income, false));
        summaryTable.addCell(createCell("Total Expenses", true));
        summaryTable.addCell(createCell("₹" + expense, false));
        summaryTable.addCell(createCell("Net Balance", true));
        summaryTable.addCell(createCell("₹" + balance, false));

        document.add(summaryTable);
        document.add(new Paragraph("\n"));

        // Transactions table
        document.add(new Paragraph("Transaction History")
                .setFontSize(14).setBold()
                .setFontColor(ColorConstants.DARK_GRAY));

        Table txTable = new Table(UnitValue.createPercentArray(
                new float[]{25, 15, 15, 20, 25}))
                .useAllAvailableWidth();

        // Headers
        txTable.addHeaderCell(createHeaderCell("Title"));
        txTable.addHeaderCell(createHeaderCell("Amount"));
        txTable.addHeaderCell(createHeaderCell("Type"));
        txTable.addHeaderCell(createHeaderCell("Category"));
        txTable.addHeaderCell(createHeaderCell("Date"));

        // Rows
        for (Transaction t : transactions) {
            txTable.addCell(createCell(t.getTitle(), false));
            txTable.addCell(createCell("₹" + t.getAmount(), false));
            txTable.addCell(createCell(t.getType().name(), false));
            txTable.addCell(createCell(t.getCategory() != null ? t.getCategory() : "-", false));
            txTable.addCell(createCell(t.getTransactionDate().toString(), false));
        }

        document.add(txTable);
        document.close();

        return baos.toByteArray();
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5);
    }

    private Cell createCell(String text, boolean isBold) {
        Paragraph p = new Paragraph(text).setFontSize(10);
        if (isBold) p.setBold();
        return new Cell().add(p).setPadding(4);
    }
}