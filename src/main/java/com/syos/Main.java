package com.syos;

import com.syos.entities.*;
import com.syos.frameworks.database.*;
import com.syos.usecases.*;
import com.syos.usecases.observers.InventorySubject;
import com.syos.usecases.observers.StockAlertObserver;
import com.syos.usecases.reports.*;
import com.syos.usecases.repositories.*;
import com.syos.usecases.strategies.ExpiryPriorityStockSelectionStrategy;
import com.syos.usecases.strategies.StockSelectionStrategy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static ProductRepository productRepository;
    private static BillRepository billRepository;
    private static InventoryRepository inventoryRepository;
    private static StockBatchRepository stockBatchRepository;
    private static UserRepository userRepository;

    private static ProcessSaleUseCase processSaleUseCase;
    private static AddStockBatchUseCase addStockBatchUseCase;
    private static TransferStockUseCase transferStockUseCase;
    private static RegisterUserUseCase registerUserUseCase;
    private static AuthenticateUserUseCase authenticateUserUseCase;

    private static InventorySubject inventorySubject;
    private static StockSelectionStrategy stockSelectionStrategy;
    private static Scanner scanner;
    private static User currentUser = null;

    public static void main(String[] args) {
        System.out.println("\n=== SYOS - Synex Outlet Store Management System ===\n");
        initializeSystem();
        runMainMenu();
    }

    private static void initializeSystem() {
        productRepository = new MySQLProductRepository();
        billRepository = new MySQLBillRepository();
        inventoryRepository = new MySQLInventoryRepository();
        stockBatchRepository = new MySQLStockBatchRepository();
        userRepository = new MySQLUserRepository();

        inventorySubject = new InventorySubject();
        inventorySubject.attach(new StockAlertObserver());

        stockSelectionStrategy = new ExpiryPriorityStockSelectionStrategy();

        processSaleUseCase = new ProcessSaleUseCase(productRepository, billRepository,
                inventoryRepository, stockBatchRepository, stockSelectionStrategy, inventorySubject);
        addStockBatchUseCase = new AddStockBatchUseCase(productRepository, stockBatchRepository,
                inventoryRepository, inventorySubject);
        transferStockUseCase = new TransferStockUseCase(inventoryRepository, stockBatchRepository,
                stockSelectionStrategy, inventorySubject);
        registerUserUseCase = new RegisterUserUseCase(userRepository);
        authenticateUserUseCase = new AuthenticateUserUseCase(userRepository);

        scanner = new Scanner(System.in);
    }

    private static void runMainMenu() {
        while (true) {
            System.out.println("\n=== MAIN MENU ===");
            System.out.println("1. Point of Sale (Counter)");
            System.out.println("2. Inventory Management");
            System.out.println("3. Stock Management");
            System.out.println("4. Online Store");
            System.out.println("5. Reports");
            System.out.println("6. Exit");
            System.out.print("Select option: ");

            switch (getIntInput()) {
                case 1:
                    pointOfSaleMenu();
                    break;
                case 2:
                    inventoryMenu();
                    break;
                case 3:
                    stockManagementMenu();
                    break;
                case 4:
                    onlineStoreMenu();
                    break;
                case 5:
                    reportsMenu();
                    break;
                case 6:
                    System.out.println("\nThank you for using SYOS!");
                    System.exit(0);
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void pointOfSaleMenu() {
        System.out.println("\n=== POINT OF SALE ===");
        List<ProcessSaleUseCase.SaleRequest.SaleItem> items = new ArrayList<>();

        while (true) {
            System.out.print("\nEnter product code (or 'done' to checkout): ");
            String code = scanner.nextLine().trim();
            if (code.equalsIgnoreCase("done"))
                break;

            if (!productRepository.exists(code)) {
                System.out.println("Product not found!");
                continue;
            }

            System.out.print("Enter quantity: ");
            int quantity = getIntInput();
            if (quantity <= 0) {
                System.out.println("Quantity must be positive!");
                continue;
            }

            items.add(new ProcessSaleUseCase.SaleRequest.SaleItem(code, quantity));
            System.out.println("Item added to cart");
        }

        if (items.isEmpty()) {
            System.out.println("No items to checkout.");
            return;
        }

        double total = 0.0;
        System.out.println("\n--- Cart Summary ---");
        for (var item : items) {
            Product product = productRepository.findByCode(item.getProductCode()).get();
            double itemTotal = product.getDiscountedPrice() * item.getQuantity();
            total += itemTotal;
            System.out.printf("%s x %d = Rs. %.2f%n", product.getName(), item.getQuantity(), itemTotal);
        }
        System.out.printf("Total: Rs. %.2f%n", total);

        double cash;
        while (true) {
            System.out.print("\nEnter cash tendered: Rs. ");
            cash = getDoubleInput();
            if (cash >= total)
                break;
            System.out.println("Insufficient cash! Please enter at least Rs. " + String.format("%.2f", total));
        }

        try {
            Bill bill = processSaleUseCase.execute(new ProcessSaleUseCase.SaleRequest(
                    items, cash, Bill.TransactionType.COUNTER, null));
            System.out.println("\nSale completed successfully!");
            printBill(bill);
        } catch (ProcessSaleUseCase.SaleException e) {
            System.out.println("Sale failed: " + e.getMessage());
        }
    }

    private static void printBill(Bill bill) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        System.out.println("\n");
        System.out.println("        ╔═════════════════════════════════════════╗");
        System.out.println("        ║          S Y O S   S T O R E            ║");
        System.out.println("        ║     Synex Outlet Store (Pvt) Ltd        ║");
        System.out.println("        ╠═════════════════════════════════════════╣");
        System.out.printf("        ║ BILL NO: %-8d      DATE: %-10s ║%n",
                bill.getSerialNumber(), bill.getBillDate().format(dateFmt));
        System.out.printf("        ║ TYPE: %-12s     TIME: %-10s ║%n",
                bill.getTransactionType(), bill.getBillDate().format(timeFmt));
        if (bill.getCustomerId() != null) {
            System.out.printf("        ║ CUSTOMER: %-29s ║%n", bill.getCustomerId());
        }
        System.out.println("        ╠═════════════════════════════════════════╣");
        System.out.printf("        ║ %-15s %3s %7s %9s   ║%n", "ITEM", "QTY", "PRICE", "AMOUNT");
        System.out.println("        ╠═════════════════════════════════════════╣");

        for (var item : bill.getItems()) {
            System.out.printf("        ║ %-15s %3d %7.2f %9.2f   ║%n",
                    item.getProductName().length() > 15 ? item.getProductName().substring(0, 15)
                            : item.getProductName(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getItemTotal());
            if (item.getDiscountPercentage() > 0) {
                System.out.printf("        ║   Discount (%.0f%%)            -%7.2f     ║%n",
                        item.getDiscountPercentage(), item.getDiscountAmount());
            }
        }

        System.out.println("        ╠═════════════════════════════════════════╣");
        System.out.printf("        ║ SUBTOTAL:                    %10.2f ║%n", bill.getSubtotal());
        if (bill.getDiscount() > 0) {
            System.out.printf("        ║ DISCOUNT:                    %10.2f ║%n", bill.getDiscount());
        }
        System.out.println("        ╠═════════════════════════════════════════╣");
        System.out.printf("        ║ GRAND TOTAL:                 %10.2f ║%n", bill.getTotal());
        System.out.println("        ╠═════════════════════════════════════════╣");
        System.out.printf("        ║ CASH:                        %10.2f ║%n", bill.getCashTendered());
        System.out.printf("        ║ CHANGE:                      %10.2f ║%n", bill.getChange());
        System.out.println("        ╠═════════════════════════════════════════╣");
        System.out.println("        ║     Thank you for shopping at SYOS!     ║");
        System.out.println("        ╚═════════════════════════════════════════╝\n");
    }

    private static void inventoryMenu() {
        while (true) {
            System.out.println("\n=== INVENTORY MANAGEMENT ===");
            System.out.println("1. View All Products");
            System.out.println("2. Add New Product");
            System.out.println("3. View Inventory Levels");
            System.out.println("4. Back");
            System.out.print("Select option: ");

            switch (getIntInput()) {
                case 1:
                    viewAllProducts();
                    break;
                case 2:
                    addNewProduct();
                    break;
                case 3:
                    viewInventoryLevels();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void viewAllProducts() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            System.out.println("No products available.");
            return;
        }
        System.out.printf("%n%-10s %-20s %-10s %-10s%n", "Code", "Name", "Price", "Discount");
        for (Product p : products) {
            System.out.printf("%-10s %-20s Rs.%-7.2f %.1f%%%n", p.getCode(), p.getName(), p.getPrice(),
                    p.getDiscountPercentage());
        }
    }

    private static void addNewProduct() {
        System.out.print("Product Code: ");
        String code = scanner.nextLine().trim();
        if (productRepository.exists(code)) {
            System.out.println("Product code already exists!");
            return;
        }

        System.out.print("Product Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Unit (kg/L/pcs): ");
        String unit = scanner.nextLine().trim();
        if (unit.isEmpty())
            unit = "pcs";

        System.out.print("Price (Rs.): ");
        double price = getDoubleInput();

        System.out.print("Discount (%): ");
        double discount = getDoubleInput();

        System.out.print("Initial Stock: ");
        int qty = getIntInput();

        System.out.print("Expiry Date (YYYY-MM-DD): ");
        LocalDate expiryDate;
        try {
            expiryDate = LocalDate.parse(scanner.nextLine().trim());
        } catch (Exception e) {
            expiryDate = LocalDate.now().plusYears(1);
        }

        try {
            productRepository.save(new Product.Builder().code(code).name(name).unit(unit)
                    .price(price).discountPercentage(discount).build());
            inventoryRepository.save(new Inventory(code));
            if (qty > 0)
                addStockBatchUseCase.execute(code, qty, expiryDate);
            System.out.println("Product added successfully!");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewInventoryLevels() {
        List<Inventory> inventories = inventoryRepository.findAll();
        if (inventories.isEmpty()) {
            System.out.println("No inventory data.");
            return;
        }
        System.out.printf("%n%-10s %-20s %-8s %-8s %-8s %-8s%n", "Code", "Product", "Shelf", "Store", "Online",
                "Total");
        for (Inventory inv : inventories) {
            String name = productRepository.findByCode(inv.getProductCode()).map(Product::getName).orElse("Unknown");
            System.out.printf("%-10s %-20s %-8d %-8d %-8d %-8d %s%n", inv.getProductCode(), name,
                    inv.getShelfQuantity(), inv.getStoreQuantity(), inv.getOnlineQuantity(),
                    inv.getTotalQuantity(), inv.isBelowReorderLevel() ? "LOW" : "");
        }
    }

    private static void stockManagementMenu() {
        while (true) {
            System.out.println("\n=== STOCK MANAGEMENT ===");
            System.out.println("1. Add Stock Batch");
            System.out.println("2. View Stock Batches");
            System.out.println("3. Transfer to Shelf");
            System.out.println("4. Transfer to Online");
            System.out.println("5. Back");
            System.out.print("Select option: ");

            switch (getIntInput()) {
                case 1:
                    addStockBatch();
                    break;
                case 2:
                    viewStockBatches();
                    break;
                case 3:
                    transferStock(TransferStockUseCase.TransferType.STORE_TO_SHELF);
                    break;
                case 4:
                    transferStock(TransferStockUseCase.TransferType.STORE_TO_ONLINE);
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void addStockBatch() {
        System.out.print("Product Code: ");
        String code = scanner.nextLine().trim();
        if (!productRepository.exists(code)) {
            System.out.println("Product not found!");
            return;
        }
        System.out.print("Batch Code (or Enter to auto-generate): ");
        String batchCode = scanner.nextLine().trim();
        System.out.print("Quantity: ");
        int qty = getIntInput();
        System.out.print("Expiry Date (yyyy-MM-dd): ");
        try {
            LocalDate expiryDate = LocalDate.parse(scanner.nextLine().trim());
            if (batchCode.isEmpty()) {
                addStockBatchUseCase.execute(code, qty, expiryDate);
            } else {
                StockBatch batch = new StockBatch(batchCode, code, LocalDate.now(), qty, expiryDate);
                stockBatchRepository.save(batch);
                inventoryRepository.findByProductCode(code).ifPresent(inv -> {
                    inv.addToStore(qty);
                    inventoryRepository.update(inv);
                    inventorySubject.notifyObservers(inv);
                });
            }
            System.out
                    .println("Stock batch added! Batch Code: " + (batchCode.isEmpty() ? "Auto-generated" : batchCode));
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format!");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewStockBatches() {
        List<StockBatch> batches = stockBatchRepository.findAll();
        if (batches.isEmpty()) {
            System.out.println("No stock batches.");
            return;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        System.out.printf("%n%-30s %-10s %-12s %-6s %-12s %-8s%n", "Batch Code", "Product", "Purchased", "Qty",
                "Expiry", "Status");
        System.out.println("─".repeat(80));
        for (StockBatch b : batches) {
            String status = b.isExpired() ? "EXPIRED" : (b.getDaysUntilExpiry() < 30 ? "EXPIRING" : "OK");
            System.out.printf("%-30s %-10s %-12s %-6d %-12s %-8s%n", b.getBatchId(), b.getProductCode(),
                    b.getPurchaseDate().format(fmt), b.getQuantity(), b.getExpiryDate().format(fmt), status);
        }
    }

    private static void transferStock(TransferStockUseCase.TransferType type) {
        System.out.print("Product Code: ");
        String code = scanner.nextLine().trim();
        System.out.print("Quantity: ");
        int qty = getIntInput();
        try {
            transferStockUseCase.execute(code, qty, type);
            System.out.println("Stock transferred!");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void onlineStoreMenu() {
        if (currentUser == null)
            onlineAuthMenu();
        else
            onlineShoppingMenu();
    }

    private static void onlineAuthMenu() {
        while (true) {
            System.out.println("\n=== ONLINE STORE ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Back");
            System.out.print("Select option: ");

            switch (getIntInput()) {
                case 1:
                    if (loginUser()) {
                        onlineShoppingMenu();
                        return;
                    }
                    break;
                case 2:
                    registerUser();
                    break;
                case 3:
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static boolean loginUser() {
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        try {
            currentUser = authenticateUserUseCase.execute(email, password);
            System.out.println("Login successful! Welcome, " + currentUser.getName());
            return true;
        } catch (AuthenticateUserUseCase.AuthenticationException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private static void registerUser() {
        System.out.print("Full Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        System.out.print("Address: ");
        String address = scanner.nextLine().trim();
        try {
            registerUserUseCase.execute(name, email, password, address);
            System.out.println("Registration successful! You can now login.");
        } catch (RegisterUserUseCase.RegistrationException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void onlineShoppingMenu() {
        System.out.println("\n=== ONLINE SHOPPING === (Logged in: " + currentUser.getName() + ")");
        List<ProcessSaleUseCase.SaleRequest.SaleItem> cart = new ArrayList<>();

        while (true) {
            System.out.println("\n1. Browse Products");
            System.out.println("2. View Cart");
            System.out.println("3. Checkout");
            System.out.println("4. Logout");
            System.out.print("Select option: ");

            switch (getIntInput()) {
                case 1:
                    browseOnlineProducts(cart);
                    break;
                case 2:
                    viewCart(cart);
                    break;
                case 3:
                    if (checkoutOnline(cart))
                        return;
                    break;
                case 4:
                    currentUser = null;
                    System.out.println("Logged out!");
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void browseOnlineProducts(List<ProcessSaleUseCase.SaleRequest.SaleItem> cart) {
        System.out.printf("%n%-10s %-20s %-12s %-10s%n", "Code", "Name", "Price", "Available");
        for (Product p : productRepository.findAll()) {
            int avail = inventoryRepository.findByProductCode(p.getCode()).map(Inventory::getOnlineQuantity).orElse(0);
            System.out.printf("%-10s %-20s Rs.%-8.2f %-10d%n", p.getCode(), p.getName(), p.getDiscountedPrice(), avail);
        }
        System.out.print("\nEnter product code (or 'back'): ");
        String code = scanner.nextLine().trim();
        if (code.equalsIgnoreCase("back") || !productRepository.exists(code))
            return;
        System.out.print("Quantity: ");
        cart.add(new ProcessSaleUseCase.SaleRequest.SaleItem(code, getIntInput()));
        System.out.println("Added to cart!");
    }

    private static void viewCart(List<ProcessSaleUseCase.SaleRequest.SaleItem> cart) {
        if (cart.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }
        double total = 0.0;
        for (var item : cart) {
            Product p = productRepository.findByCode(item.getProductCode()).get();
            double itemTotal = p.getDiscountedPrice() * item.getQuantity();
            total += itemTotal;
            System.out.printf("%s x %d = Rs. %.2f%n", p.getName(), item.getQuantity(), itemTotal);
        }
        System.out.printf("Total: Rs. %.2f%n", total);
    }

    private static boolean checkoutOnline(List<ProcessSaleUseCase.SaleRequest.SaleItem> cart) {
        if (cart.isEmpty()) {
            System.out.println("Cart is empty!");
            return false;
        }
        viewCart(cart);
        System.out.print("\nConfirm order? (yes/no): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("yes"))
            return false;

        try {
            double total = cart.stream()
                    .mapToDouble(item -> productRepository.findByCode(item.getProductCode()).get().getDiscountedPrice()
                            * item.getQuantity())
                    .sum();
            Bill bill = processSaleUseCase.execute(new ProcessSaleUseCase.SaleRequest(
                    cart, total, Bill.TransactionType.ONLINE, currentUser.getUserId()));
            System.out.println("\nOrder placed successfully!");
            printBill(bill);
            cart.clear();
            return true;
        } catch (ProcessSaleUseCase.SaleException e) {
            System.out.println("Order failed: " + e.getMessage());
            return false;
        }
    }

    private static void reportsMenu() {
        while (true) {
            System.out.println("\n=== REPORTS ===");
            System.out.println("1. Daily Sales Report");
            System.out.println("2. Reshelve Report");
            System.out.println("3. Reorder Levels Report");
            System.out.println("4. Stock Report");
            System.out.println("5. Bill Report");
            System.out.println("6. Back");
            System.out.print("Select option: ");

            switch (getIntInput()) {
                case 1:
                    generateDailySalesReport();
                    break;
                case 2:
                    System.out.println(
                            "\n" + new ReshelveReport(inventoryRepository, productRepository).generateReport());
                    break;
                case 3:
                    System.out.println(
                            "\n" + new ReorderLevelsReport(inventoryRepository, productRepository).generateReport());
                    break;
                case 4:
                    System.out
                            .println("\n" + new StockReport(stockBatchRepository, productRepository).generateReport());
                    break;
                case 5:
                    generateBillReport();
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void generateDailySalesReport() {
        System.out.print("Enter date (yyyy-MM-dd) or Enter for today: ");
        String dateStr = scanner.nextLine().trim();
        LocalDate date = dateStr.isEmpty() ? LocalDate.now() : LocalDate.parse(dateStr);
        Bill.TransactionType type = getTransactionTypeChoice();
        System.out.println("\n" + new DailySalesReport(billRepository, date, type).generateReport());
    }

    private static void generateBillReport() {
        Bill.TransactionType type = getTransactionTypeChoice();
        System.out.println("\n" + new BillReport(billRepository, type).generateReport());
    }

    private static Bill.TransactionType getTransactionTypeChoice() {
        System.out.println("1. All  2. Counter  3. Online");
        System.out.print("Select type: ");
        switch (getIntInput()) {
            case 2:
                return Bill.TransactionType.COUNTER;
            case 3:
                return Bill.TransactionType.ONLINE;
            default:
                return null;
        }
    }

    private static int getIntInput() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static double getDoubleInput() {
        try {
            return Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
