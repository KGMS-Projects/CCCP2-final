package com.syos.client.gui;

import com.syos.client.ServerConnection;
import com.syos.client.controller.ClientController;
import com.syos.client.gui.components.SidebarPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;


public class MainFrame extends JFrame {

    // Color scheme
    public static final Color PRIMARY = new Color(41, 98, 255);
    public static final Color PRIMARY_DARK = new Color(25, 70, 200);
    public static final Color BG_MAIN = new Color(240, 242, 247);
    public static final Color BG_CARD = Color.WHITE;
    public static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    public static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    public static final Color SUCCESS = new Color(46, 125, 50);
    public static final Color WARNING = new Color(255, 152, 0);
    public static final Color ERROR = new Color(211, 47, 47);
    public static final Color BORDER = new Color(224, 224, 224);
    public static final Color TABLE_HEADER_BG = new Color(236, 239, 244);
    public static final Color TABLE_ALT_ROW = new Color(248, 249, 252);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 13);

    private static final String[] NAV_NAMES = {
            "Dashboard", "Point of Sale", "Inventory", "Stock", "Online Store", "Reports"
    };
    private static final String[] NAV_ICONS = {
            "📊", "🛒", "📦", "🏪", "🌐", "📋"
    };
    private static final String[] CARD_KEYS = {
            "DASHBOARD", "POS", "INVENTORY", "STOCK", "ONLINE", "REPORTS"
    };

    private final ClientController controller;
    private final ServerConnection connection;
    
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private SidebarPanel sidebarPanel;
    private JLabel statusLabel;
    private JLabel connectionLabel;

    private POSPanel posPanel;
    private InventoryPanel inventoryPanel;
    private StockPanel stockPanel;
    private ReportsPanel reportsPanel;
    private OnlineStorePanel onlineStorePanel;
    private DashboardPanel dashboardPanel;

    public MainFrame(ClientController controller, ServerConnection connection) {
        this.controller = controller;
        this.connection = connection;
        initializeFrame();
        createComponents();
        setupNotificationListener();
    }

    private void initializeFrame() {
        setTitle("SYOS - Synex Outlet Store Management System");
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_MAIN);
        setLayout(new BorderLayout());
    }

    private void createComponents() {
        // Sidebar Navigation
        sidebarPanel = new SidebarPanel(NAV_NAMES, NAV_ICONS);
        sidebarPanel.setNavClickListener(index -> {
            cardLayout.show(contentPanel, CARD_KEYS[index]);
        });
        add(sidebarPanel, BorderLayout.WEST);

        // Right side (Header + Content + Footer)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(BG_MAIN);

        // Header panel
        rightPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // Content Area (CardLayout)
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG_MAIN);
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create inner panels
        dashboardPanel = new DashboardPanel(controller);
        posPanel = new POSPanel(controller, this);
        inventoryPanel = new InventoryPanel(controller);
        stockPanel = new StockPanel(controller);
        onlineStorePanel = new OnlineStorePanel(controller, this);
        reportsPanel = new ReportsPanel(controller);

        contentPanel.add(dashboardPanel, "DASHBOARD");
        contentPanel.add(posPanel, "POS");
        contentPanel.add(inventoryPanel, "INVENTORY");
        contentPanel.add(stockPanel, "STOCK");
        contentPanel.add(onlineStorePanel, "ONLINE");
        contentPanel.add(reportsPanel, "REPORTS");

        rightPanel.add(contentPanel, BorderLayout.CENTER);

        // Status bar
        rightPanel.add(createStatusBar(), BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(15, 20, 15, 20)));

        JLabel title = new JLabel("Welcome to Synex Outlet Store");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        // Connection status
        connectionLabel = new JLabel("● Connected to " + connection.getHost() + ":" + connection.getPort());
        connectionLabel.setFont(FONT_SMALL);
        connectionLabel.setForeground(new Color(76, 175, 80));
        header.add(connectionLabel, BorderLayout.EAST);

        return header;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(250, 250, 252));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(6, 15, 6, 15)
        ));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_SECONDARY);
        statusBar.add(statusLabel, BorderLayout.WEST);

        JLabel versionLabel = new JLabel("Developed by Mihilayan Sachinthana");
        versionLabel.setFont(FONT_SMALL);
        versionLabel.setForeground(TEXT_SECONDARY);
        statusBar.add(versionLabel, BorderLayout.EAST);

        return statusBar;
    }

    /**
     * Listen for server push notifications to refresh displayed data.
     */
    private void setupNotificationListener() {
        connection.addNotificationListener(response -> {
            String type = response.getNotificationType();
            if (type == null) return;

            // Update UI on the Event Dispatch Thread (Swing thread safety)
            SwingUtilities.invokeLater(() -> {
                switch (type) {
                    case "INVENTORY_CHANGED":
                        statusLabel.setText("Inventory updated by another client");
                        refreshAllPanels();
                        break;
                    case "SALE_COMPLETED":
                        statusLabel.setText("A sale was completed");
                        refreshAllPanels();
                        break;
                    case "PRODUCT_CHANGED":
                        statusLabel.setText("Product catalog updated");
                        refreshAllPanels();
                        break;
                    default:
                        statusLabel.setText("Notification: " + type);
                }
            });
        });
    }

    /** Refresh all panels to show latest data */
    public void refreshAllPanels() {
        if (dashboardPanel != null) dashboardPanel.refreshData();
        if (inventoryPanel != null) inventoryPanel.refreshData();
        if (stockPanel != null) stockPanel.refreshData();
        if (posPanel != null) posPanel.refreshData();
    }

    /** Update the status bar text */
    public void setStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    // ==================== Utility Methods for Panels ====================

    /** Creates a styled button with consistent appearance */
    public static JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(FONT_BUTTON);
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(160, 38));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color original = bgColor;
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(original.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(original);
            }
        });
        return button;
    }

    /** Creates a styled text field */
    public static JTextField createStyledField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return field;
    }

    /** Creates a card panel with border and shadow effect */
    public static JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        return card;
    }

    /** Configures a JTable with consistent styling */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(32);
        table.setGridColor(BORDER);
        table.setShowGrid(true);
        table.setSelectionBackground(new Color(200, 220, 255));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(TABLE_HEADER_BG);
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY));

        // Alternating row colors
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? BG_CARD : TABLE_ALT_ROW);
                }
                return this;
            }
        });
    }
}
