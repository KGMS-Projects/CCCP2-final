# SYOS - Synex Outlet Store Management System

## Project Overview
A comprehensive Point of Sale (POS) and Inventory Management System built with Clean Architecture principles, SOLID design, and extensive design patterns.

## Architecture

### Clean Architecture Layers
1. **Entities** - Core business objects (Product, Bill, Stock, User)
2. **Use Cases** - Business logic (ProcessSale, ManageInventory, GenerateReports)
3. **Interface Adapters** - Controllers, Presenters, Gateways
4. **Frameworks & Drivers** - UI, Database, External interfaces

### SOLID Principles Applied
- **S**ingle Responsibility Principle - Each class has one reason to change
- **O**pen/Closed Principle - Open for extension, closed for modification
- **L**iskov Substitution Principle - Subtypes are substitutable for base types
- **I**nterface Segregation Principle - Many specific interfaces over one general
- **D**ependency Inversion Principle - Depend on abstractions, not concretions

### Design Patterns Implemented (11 patterns)
1. **Singleton** - Database connection, Configuration manager
2. **Factory Method** - Bill factory, Report factory
3. **Abstract Factory** - Transaction factory (Counter/Online)
4. **Builder** - Bill builder, Product builder
5. **Strategy** - Stock selection strategy (FIFO, Expiry-based)
6. **Observer** - Inventory observers for stock alerts
7. **Command** - Transaction commands (Sale, Refund)
8. **Repository** - Data access abstraction
9. **Facade** - Simplified POS interface
10. **Decorator** - Discount decorators
11. **Template Method** - Report generation template

## Project Structure
```
pos-system/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── syos/
│   │               ├── entities/          # Core business entities
│   │               ├── usecases/          # Business logic
│   │               ├── interfaces/        # Interface adapters
│   │               ├── frameworks/        # External frameworks
│   │               └── Main.java          # Application entry point
│   └── test/
│       └── java/
│           └── com/
│               └── syos/                  # Comprehensive test suite
├── docs/                                  # Design diagrams
├── data/                                  # JSON data storage
└── pom.xml                                # Maven configuration
```

## Features
1. **Point of Sale (Counter)**
   - Item scanning by code
   - Quantity management
   - Automatic calculation with discounts
   - Bill generation with serial numbers
   - Cash payment processing

2. **Inventory Management**
   - Product CRUD operations
   - Stock tracking (shelf, store, online)
   - Batch management with expiry dates
   - Automatic stock reduction on sales

3. **Online Store**
   - User registration and authentication
   - Separate online inventory
   - Online order processing
   - Transaction type identification

4. **Stock Management**
   - Batch-wise stock tracking
   - FIFO with expiry priority
   - Stock transfer (store to shelf)
   - Reorder level alerts

5. **Reporting**
   - Daily sales report (by transaction type)
   - Reshelving requirements
   - Reorder levels (< 50 items)
   - Stock report (batch-wise)
   - Bill report (all transactions)

## How to Run
```bash
# Compile
javac -d bin -sourcepath src/main/java src/main/java/com/syos/Main.java

# Run
java -cp bin com.syos.Main

# Run tests (with JUnit)
mvn test
```

## Testing
- Unit tests for all entities
- Integration tests for use cases
- Mock objects for external dependencies
- Test coverage > 90%

## Design Documentation
See `/docs` folder for:
- Class diagrams
- Use case diagrams
- Sequence diagrams
- Architecture analysis
