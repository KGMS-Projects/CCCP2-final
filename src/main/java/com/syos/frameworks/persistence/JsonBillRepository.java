package com.syos.frameworks.persistence;

import com.google.gson.reflect.TypeToken;
import com.syos.entities.Bill;
import com.syos.usecases.repositories.BillRepository;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JSON-based implementation of BillRepository.
 */
public class JsonBillRepository implements BillRepository {
    private static final String FILENAME = "bills.json";
    private final JsonDataStore dataStore;
    private final Type listType = new TypeToken<ArrayList<BillData>>() {
    }.getType();

    public JsonBillRepository() {
        this.dataStore = JsonDataStore.getInstance();
    }

    @Override
    public void save(Bill bill) {
        List<BillData> bills = loadAll();
        bills.add(toData(bill));
        dataStore.save(FILENAME, bills);
    }

    @Override
    public Optional<Bill> findBySerialNumber(int serialNumber) {
        return loadAll().stream()
                .filter(b -> b.serialNumber == serialNumber)
                .map(this::toEntity)
                .findFirst();
    }

    @Override
    public List<Bill> findAll() {
        return loadAll().stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<Bill> findByDate(LocalDate date) {
        return loadAll().stream()
                .filter(b -> LocalDateTime.parse(b.billDate).toLocalDate().equals(date))
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<Bill> findByTransactionType(Bill.TransactionType type) {
        return loadAll().stream()
                .filter(b -> b.transactionType.equals(type.name()))
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<Bill> findByDateAndType(LocalDate date, Bill.TransactionType type) {
        return loadAll().stream()
                .filter(b -> LocalDateTime.parse(b.billDate).toLocalDate().equals(date))
                .filter(b -> b.transactionType.equals(type.name()))
                .map(this::toEntity)
                .toList();
    }

    @Override
    public int getNextSerialNumber() {
        List<BillData> bills = loadAll();
        return bills.stream()
                .mapToInt(b -> b.serialNumber)
                .max()
                .orElse(0) + 1;
    }

    private List<BillData> loadAll() {
        return dataStore.load(FILENAME, listType);
    }

    private BillData toData(Bill bill) {
        BillData data = new BillData();
        data.serialNumber = bill.getSerialNumber();
        data.billDate = bill.getBillDate().toString();
        data.items = bill.getItems().stream().map(this::toItemData).toList();
        data.subtotal = bill.getSubtotal();
        data.discount = bill.getDiscount();
        data.total = bill.getTotal();
        data.cashTendered = bill.getCashTendered();
        data.change = bill.getChange();
        data.transactionType = bill.getTransactionType().name();
        data.customerId = bill.getCustomerId();
        return data;
    }

    private Bill toEntity(BillData data) {
        Bill.Builder builder = new Bill.Builder()
                .serialNumber(data.serialNumber)
                .billDate(LocalDateTime.parse(data.billDate))
                .cashTendered(data.cashTendered)
                .transactionType(Bill.TransactionType.valueOf(data.transactionType))
                .customerId(data.customerId);

        for (BillItemData itemData : data.items) {
            builder.addItem(new Bill.BillItem(
                    itemData.productCode,
                    itemData.productName,
                    itemData.unit,
                    itemData.quantity,
                    itemData.price,
                    itemData.discountPercentage));
        }

        return builder.build();
    }

    private BillItemData toItemData(Bill.BillItem item) {
        BillItemData data = new BillItemData();
        data.productCode = item.getProductCode();
        data.productName = item.getProductName();
        data.unit = item.getUnit();
        data.quantity = item.getQuantity();
        data.price = item.getPrice();
        data.discountPercentage = item.getDiscountPercentage();
        return data;
    }

    private static class BillData {
        int serialNumber;
        String billDate;
        List<BillItemData> items;
        double subtotal;
        double discount;
        double total;
        double cashTendered;
        double change;
        String transactionType;
        String customerId;
    }

    private static class BillItemData {
        String productCode;
        String productName;
        String unit;
        int quantity;
        double price;
        double discountPercentage;
    }
}
