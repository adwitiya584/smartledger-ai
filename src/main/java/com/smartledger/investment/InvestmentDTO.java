package com.smartledger.investment;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvestmentDTO {
    private String name;
    private String type;
    private BigDecimal investedAmount;
    private BigDecimal currentValue;
    private BigDecimal monthlySip;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private String notes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getInvestedAmount() { return investedAmount; }
    public void setInvestedAmount(BigDecimal investedAmount) { this.investedAmount = investedAmount; }

    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }

    public BigDecimal getMonthlySip() { return monthlySip; }
    public void setMonthlySip(BigDecimal monthlySip) { this.monthlySip = monthlySip; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}