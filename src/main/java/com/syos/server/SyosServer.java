package com.syos.server;

import com.syos.common.Response;
import com.syos.frameworks.database.*;
import com.syos.server.web.WebAppServer;
import com.syos.usecases.*;
import com.syos.usecases.observers.InventorySubject;
import com.syos.usecases.observers.StockAlertObserver;
import com.syos.usecases.repositories.*;
import com.syos.usecases.strategies.ExpiryPriorityStockSelectionStrategy;
import com.syos.usecases.strategies.StockSelectionStrategy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SYOS Server - Main entry point for the server tier.
 * 
 * This is a standalone application that:
 * 1. Initializes all repositories and use cases (business logic layer)
 * 2. Connects to the MySQL database (data layer)
 * 3. Accepts TCP socket connections from GUI clients
 * 4. Processes requests using a thread pool + blocking queue architecture
 * 
 * Architecture (Clean Architecture on Server Side):
 * - Entities: Product, Bill, Inventory, StockBatch, User (core domain)
 * - Use Cases: ProcessSale, AddStockBatch, TransferStock, etc. (business rules)
 * - Interface Adapters: RequestDispatcher (converts network requests to use case calls)
 * - Frameworks: MySQL repositories, Socket server (external interfaces)
 * 
 * Concurrency Architecture:
 * ┌──────────┐   ┌──────────┐          ┌──────────────────┐     ┌────────────┐
 * │ Client 1 │──►│ Handler  │──enqueue─►│  BlockingQueue   │────►│  Worker    │
 * └──────────┘   │ Thread 1 │          │  (RequestQueue)  │     │  Thread    │
 * ┌──────────┐   ├──────────┤          │                  │     │  Pool      │
 * │ Client 2 │──►│ Handler  │──enqueue─►│  Queued requests │────►│  (dispatch │
 * └──────────┘   │ Thread 2 │          │  processed FIFO  │     │   + lock)  │
 * ┌──────────┐   ├──────────┤          └──────────────────┘     └────────────┘
 * │ Client N │──►│ Handler  │
 * └──────────┘   │ Thread N │
 *                └──────────┘
 *   Connection       IO                    Request                Processing
 *   Thread Pool    Threads                  Queue                  Workers
 */
public class SyosServer {

    private static final int DEFAULT_PORT = 5000;
    private static final int WEB_PORT = 8080;            // HTTP port for Online Store
    private static final int CONNECTION_POOL_SIZE = 20;  // Max concurrent client connections
    private static final int WORKER_POOL_SIZE = 5;       // Worker threads for processing requests

    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService connectionPool;  // Thread pool for client connections
    private ExecutorService workerPool;      // Thread pool for processing requests
    private RequestQueue requestQueue;
    private ClientRegistry clientRegistry;
    private RequestDispatcher dispatcher;
    private WebAppServer webAppServer;
    private volatile boolean running = true;

    public SyosServer(int port) {
        this.port = port;
    }

    /**
     * Initializes all server components:
     * - Database repositories
     * - Business logic (use cases)
     * - Concurrency infrastructure (thread pools, queue, registry)
     */
    private void initialize() {
        System.out.println("=== SYOS Server Starting ===");
        System.out.println("Initializing database connection...");

        // Initialize repositories (Database/Framework layer)
        ProductRepository productRepository = new MySQLProductRepository();
        BillRepository billRepository = new MySQLBillRepository();
        InventoryRepository inventoryRepository = new MySQLInventoryRepository();
        StockBatchRepository stockBatchRepository = new MySQLStockBatchRepository();
        UserRepository userRepository = new MySQLUserRepository();

        // Initialize observers
        InventorySubject inventorySubject = new InventorySubject();
        inventorySubject.attach(new StockAlertObserver());

        // Initialize strategies
        StockSelectionStrategy stockSelectionStrategy = new ExpiryPriorityStockSelectionStrategy();

        // Initialize use cases (Business Logic layer)
        ProcessSaleUseCase processSaleUseCase = new ProcessSaleUseCase(
                productRepository, billRepository, inventoryRepository,
                stockBatchRepository, stockSelectionStrategy, inventorySubject);

        AddStockBatchUseCase addStockBatchUseCase = new AddStockBatchUseCase(
                productRepository, stockBatchRepository, inventoryRepository, inventorySubject);

        TransferStockUseCase transferStockUseCase = new TransferStockUseCase(
                inventoryRepository, stockBatchRepository, stockSelectionStrategy, inventorySubject);

        RegisterUserUseCase registerUserUseCase = new RegisterUserUseCase(userRepository);
        AuthenticateUserUseCase authenticateUserUseCase = new AuthenticateUserUseCase(userRepository);

        // Initialize concurrency infrastructure
        clientRegistry = new ClientRegistry();
        requestQueue = new RequestQueue(500);

        // Initialize request dispatcher (Interface Adapter layer)
        dispatcher = new RequestDispatcher(
                productRepository, billRepository, inventoryRepository,
                stockBatchRepository, userRepository,
                processSaleUseCase, addStockBatchUseCase, transferStockUseCase,
                registerUserUseCase, authenticateUserUseCase,
                inventorySubject, clientRegistry);

        // Create thread pools
        connectionPool = Executors.newFixedThreadPool(CONNECTION_POOL_SIZE);
        workerPool = Executors.newFixedThreadPool(WORKER_POOL_SIZE);

        System.out.println("✓ Repositories initialized");
        System.out.println("✓ Use cases initialized");
        System.out.println("✓ Thread pools created (connections: " + CONNECTION_POOL_SIZE +
                ", workers: " + WORKER_POOL_SIZE + ")");
        System.out.println("✓ Request queue initialized (capacity: 500)");
    }

    /**
     * Starts the worker threads that consume from the request queue.
     * These threads take requests from the BlockingQueue and dispatch them
     * to the appropriate use case via the RequestDispatcher.
     */
    private void startWorkers() {
        for (int i = 0; i < WORKER_POOL_SIZE; i++) {
            final int workerId = i;
            workerPool.submit(() -> {
                Thread.currentThread().setName("Worker-" + workerId);
                System.out.println("[WORKER-" + workerId + "] Started");

                while (running) {
                    try {
                        // Block until a request is available
                        RequestQueue.QueuedRequest queuedRequest = requestQueue.take();

                        long waitTimeMs = (System.nanoTime() - queuedRequest.getEnqueueTimeNanos()) / 1_000_000;
                        System.out.println("[WORKER-" + workerId + "] Processing: "
                                + queuedRequest.getRequest().getMethod()
                                + " (waited " + waitTimeMs + "ms in queue)");

                        // Dispatch to use case
                        Response response = dispatcher.dispatch(
                                queuedRequest.getRequest(),
                                queuedRequest.getClientHandler());

                        // Send response back to client
                        queuedRequest.getClientHandler().sendResponse(response);

                        // Complete the future (for any waiting code)
                        queuedRequest.getResponseFuture().complete(response);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("[WORKER-" + workerId + "] Error: " + e.getMessage());
                    }
                }
                System.out.println("[WORKER-" + workerId + "] Stopped");
            });
        }
    }

    /**
     * Starts accepting client connections.
     * Each connection is handled by a ClientHandler running in the connection pool.
     */
    private void acceptConnections() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("\n══════════════════════════════════════════");
            System.out.println("  SYOS Server listening on port " + port);
            System.out.println("  Waiting for client connections...");
            System.out.println("══════════════════════════════════════════\n");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] New connection from: " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(
                        clientSocket, requestQueue, clientRegistry, dispatcher);
                connectionPool.submit(handler);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[SERVER] Error accepting connections: " + e.getMessage());
            }
        }
    }

    /** Gracefully shut down the server */
    public void shutdown() {
        System.out.println("\n[SERVER] Shutting down...");
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) { /* ignore */ }
        if (webAppServer != null) webAppServer.stop();
        if (connectionPool != null) connectionPool.shutdownNow();
        if (workerPool != null) workerPool.shutdownNow();
        DatabaseManager.getInstance().shutdown();
        System.out.println("[SERVER] Shutdown complete");
    }

    /**
     * Main entry point for the SYOS Server application.
     * Usage: java com.syos.server.SyosServer [port]
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: " + DEFAULT_PORT);
            }
        }

        SyosServer server = new SyosServer(port);

        // Shutdown hook for clean exit
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        try {
            server.initialize();
            server.startWorkers();

            // Start embedded Tomcat for Online Store (web browser)
            try {
                server.webAppServer = new WebAppServer(WEB_PORT, server.dispatcher, server.clientRegistry);
                server.webAppServer.start();
            } catch (Exception e) {
                System.err.println("[WARNING] Failed to start Online Store web server: " + e.getMessage());
                System.err.println("Online Store will not be available. POS system continues working.");
            }

            server.acceptConnections();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            server.shutdown();
        }
    }
}
