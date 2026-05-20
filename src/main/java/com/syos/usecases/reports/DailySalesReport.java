package com.syos.usecases.reports;

import com.syos.entities.Bill;
import com.syos.usecases.repositories.BillRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily sales report implementation.
 * Requirement: "The total sale for a given day. This report should display the
 * items sold
 * (with name and code), the total quantity, and the total revenue for the given
 * day."
 */
public class DailySalesReport extends ReportTemplate {
    private final BillRepository billRepository;
    private final LocalDate date;
    private final Bill.TransactionType transactionType;

    public DailySalesReport(BillRepository billRepository, LocalDate date, Bill.TransactionType transactionType) {
        this.billRepository = billRepository;
        this.date = date;
        this.transactionType = transactionType;
    }

    @Override
    protected String getReportHeader() {
        return String.format("=== DAILY SALES REPORT ===\nDate: %s\nTransaction Type: %s\n",
                date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                transactionType == null ? "ALL" : transactionType);
    }

    @Override
    protected String getReportBody() {
        List<Bill> bills = transactionType == null
                ? billRepository.findByDate(date)
                : billRepository.findByDateAndType(date, transactionType);

        if (bills.isEmpty()) {
            return "No sales recorded for this date.";
        }

        // Aggregate items
        Map<String, SalesItem> salesItems = new HashMap<>();
        double totalRevenue = 0.0;

        for (Bill bill : bills) {
            totalRevenue += bill.getTotal();

            for (Bill.BillItem item : bill.getItems()) {
                String key = item.getProductCode();
                salesItems.computeIfAbsent(key, k -> new SalesItem(item.getProductCode(), item.getProductName()))
                        .addQuantity(item.getQuantity())
                        .addRevenue(item.getFinalPrice());
            }
        }

        // Build report
        StringBuilder body = new StringBuilder();
        body.append(String.format("%-10s %-30s %-10s %-15s\n", "Code", "Name", "Quantity", "Revenue (Rs.)"));
        body.append("-".repeat(70)).append("\n");

        for (SalesItem item : salesItems.values()) {
            body.append(String.format("%-10s %-30s %-10d %-15.2f\n",
                    item.code, item.name, item.quantity, item.revenue));
        }

        body.append("-".repeat(70)).append("\n");
        body.append(String.format("Total Revenue: Rs. %.2f\n", totalRevenue));
        body.append(String.format("Total Transactions: %d\n", bills.size()));

        return body.toString();
    }

    private static class SalesItem {
        String code;
        String name;
        int quantity;
        double revenue;

        SalesItem(String code, String name) {
            this.code = code;
            this.name = name;
            this.quantity = 0;
            this.revenue = 0.0;
        }

        SalesItem addQuantity(int qty) {
            this.quantity += qty;
            return this;
        }

        SalesItem addRevenue(double rev) {
            this.revenue += rev;
            return this;
        }
    }
}
