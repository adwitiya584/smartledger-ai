package com.smartledger.report;

import com.itextpdf.kernel.colors.*;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.smartledger.auth.User;
import com.smartledger.auth.UserRepository;
import com.smartledger.investment.InvestmentRepository;
import com.smartledger.loan.LoanRepository;
import com.smartledger.transaction.Transaction;
import com.smartledger.transaction.TransactionRepository;
import com.smartledger.transaction.TransactionType;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final InvestmentRepository investmentRepository;
    private final LoanRepository loanRepository;

    public PdfReportService(TransactionRepository transactionRepository,
                             UserRepository userRepository,
                             InvestmentRepository investmentRepository,
                             LoanRepository loanRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.investmentRepository = investmentRepository;
        this.loanRepository = loanRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public byte[] generateReport(String email) {
        return generateReport(email, -1);
    }

    public byte[] generateReport(String email, int year) {
        User user = getUser(email);

        BigDecimal income = transactionRepository
                .sumByUserIdAndType(user.getId(), TransactionType.INCOME);
        BigDecimal expense = transactionRepository
                .sumByUserIdAndType(user.getId(), TransactionType.EXPENSE);
        BigDecimal balance = income.subtract(expense);

        BigDecimal totalInvested = investmentRepository.totalInvested(user.getId());
        BigDecimal totalCurrentValue = investmentRepository.totalCurrentValue(user.getId());
        BigDecimal totalOutstanding = loanRepository.totalOutstanding(user.getId());
        BigDecimal totalEmi = loanRepository.totalMonthlyEmi(user.getId());
        BigDecimal netSavings = balance.subtract(totalInvested);

        List<Transaction> transactions = transactionRepository
                .findByUserIdOrderByTransactionDateDesc(user.getId());

        // Filter by year if specified
        if (year > 0) {
            transactions = transactions.stream()
                    .filter(t -> t.getTransactionDate().getYear() == year)
                    .collect(Collectors.toList());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        // ── TITLE PAGE ──
        doc.add(new Paragraph("SmartLedger AI")
                .setFontSize(28).setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(99, 102, 241)));

        doc.add(new Paragraph(year > 0 ? "Annual Financial Report " + year : "Complete Financial Report")
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY));

        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        doc.add(new Paragraph("Generated: " + date)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY));

        doc.add(new Paragraph("\n"));

        // ── USER INFO ──
        doc.add(new Paragraph("Account: " + user.getName() + " | " + user.getEmail())
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY));

        doc.add(new Paragraph("\n"));

        // ── FINANCIAL SUMMARY ──
        addSectionTitle(doc, "Financial Summary");

        Table summary = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth();

        addSummaryRow(summary, "Total Income", "₹" + income, new DeviceRgb(16, 185, 129));
        addSummaryRow(summary, "Total Expenses", "₹" + expense, new DeviceRgb(239, 68, 68));
        addSummaryRow(summary, "Balance", "₹" + balance, new DeviceRgb(99, 102, 241));
        addSummaryRow(summary, "Total Invested", "₹" + totalInvested, new DeviceRgb(245, 158, 11));
        addSummaryRow(summary, "Investment Value", "₹" + totalCurrentValue, new DeviceRgb(6, 182, 212));
        addSummaryRow(summary, "Net Savings", "₹" + netSavings, new DeviceRgb(16, 185, 129));
        addSummaryRow(summary, "Loan Outstanding", "₹" + totalOutstanding, new DeviceRgb(239, 68, 68));
        addSummaryRow(summary, "Monthly EMI", "₹" + totalEmi, new DeviceRgb(245, 158, 11));

        doc.add(summary);
        doc.add(new Paragraph("\n"));

        // ── TAX SUMMARY ──
        addSectionTitle(doc, "Tax Computation Summary (Indicative)");

        double grossIncome = income.doubleValue();
        double stdDeduction = 75000;
        double taxableIncome = Math.max(grossIncome - stdDeduction, 0);
        double tax = computeNewRegimeTax(taxableIncome);
        double cess = tax * 0.04;
        double totalTax = tax + cess;

        Table taxTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth();

        addTaxRow(taxTable, "Gross Total Income", "₹" + String.format("%.2f", grossIncome));
        addTaxRow(taxTable, "Standard Deduction (New Regime)", "₹75,000");
        addTaxRow(taxTable, "Taxable Income", "₹" + String.format("%.2f", taxableIncome));
        addTaxRow(taxTable, "Income Tax (New Regime)", "₹" + String.format("%.2f", tax));
        addTaxRow(taxTable, "Health & Education Cess (4%)", "₹" + String.format("%.2f", cess));
        addTaxRow(taxTable, "Total Tax Payable", "₹" + String.format("%.2f", totalTax));
        addTaxRow(taxTable, "Monthly TDS", "₹" + String.format("%.2f", totalTax / 12));

        doc.add(taxTable);
        doc.add(new Paragraph("*This is an indicative calculation. Consult a CA for accurate tax filing.")
                .setFontSize(9).setFontColor(ColorConstants.GRAY));
        doc.add(new Paragraph("\n"));

        // ── TRANSACTION HISTORY ──
        addSectionTitle(doc, "Transaction History");

        Table txTable = new Table(UnitValue.createPercentArray(
                new float[]{25, 12, 18, 20, 25}))
                .useAllAvailableWidth();

        txTable.addHeaderCell(createHeaderCell("Title"));
        txTable.addHeaderCell(createHeaderCell("Amount"));
        txTable.addHeaderCell(createHeaderCell("Type"));
        txTable.addHeaderCell(createHeaderCell("Category"));
        txTable.addHeaderCell(createHeaderCell("Date"));

        for (Transaction t : transactions) {
            txTable.addCell(createCell(t.getTitle(), false));
            txTable.addCell(createCell("₹" + t.getAmount(), false));

            Cell typeCell = createCell(t.getType().name(), false);
            if (t.getType() == TransactionType.INCOME) {
                typeCell.setFontColor(new DeviceRgb(16, 185, 129));
            } else {
                typeCell.setFontColor(new DeviceRgb(239, 68, 68));
            }
            txTable.addCell(typeCell);

            txTable.addCell(createCell(t.getCategory() != null ? t.getCategory() : "—", false));
            txTable.addCell(createCell(t.getTransactionDate().toString(), false));
        }

        doc.add(txTable);
        doc.close();

        return baos.toByteArray();
    }

    private double computeNewRegimeTax(double income) {
        if (income <= 700000) return 0; // Rebate 87A
        double tax = 0;
        double[][] slabs = {
            {0, 300000, 0},
            {300000, 600000, 5},
            {600000, 900000, 10},
            {900000, 1200000, 15},
            {1200000, 1500000, 20},
            {1500000, Double.MAX_VALUE, 30}
        };
        for (double[] slab : slabs) {
            if (income > slab[0]) {
                double taxable = Math.min(income, slab[1]) - slab[0];
                tax += taxable * slab[2] / 100;
            }
        }
        return tax;
    }

    private void addSectionTitle(Document doc, String title) {
        doc.add(new Paragraph(title)
                .setFontSize(14).setBold()
                .setFontColor(new DeviceRgb(99, 102, 241))
                .setMarginBottom(8));
    }

    private void addSummaryRow(Table table, String label, String value, Color color) {
        table.addCell(new Cell().add(new Paragraph(label).setFontSize(11))
                .setPadding(6).setBackgroundColor(new DeviceRgb(248, 250, 252)));
        table.addCell(new Cell().add(new Paragraph(value).setFontSize(11).setBold()
                .setFontColor(color)).setPadding(6));
    }

    private void addTaxRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setFontSize(10))
                .setPadding(5).setBackgroundColor(new DeviceRgb(248, 250, 252)));
        table.addCell(new Cell().add(new Paragraph(value).setFontSize(10).setBold())
                .setPadding(5));
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(10))
                .setBackgroundColor(new DeviceRgb(99, 102, 241))
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6);
    }

    private Cell createCell(String text, boolean isBold) {
        Paragraph p = new Paragraph(text).setFontSize(9);
        if (isBold) p.setBold();
        return new Cell().add(p).setPadding(4);
    }
}