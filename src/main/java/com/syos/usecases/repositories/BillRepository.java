package com.syos.usecases.repositories;

import com.syos.entities.Bill;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Bill data access.
 * Follows Dependency Inversion and Interface Segregation Principles.
 */
public interface BillRepository {
    void save(Bill bill);

    Optional<Bill> findBySerialNumber(int serialNumber);

    List<Bill> findAll();

    List<Bill> findByDate(LocalDate date);

    List<Bill> findByTransactionType(Bill.TransactionType type);

    List<Bill> findByDateAndType(LocalDate date, Bill.TransactionType type);

    int getNextSerialNumber();
}
