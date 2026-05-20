package com.syos.client.gui;

import com.syos.client.controller.ClientController;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Dashboard panel showing system overview with summary cards
 * and key metrics for the SYOS POS system.
 */
public class DashboardPanel extends JPanel {

    private final ClientController controller;
    private JLabel totalProductsLabel;
    private JLabel lowStockLabel;
    private JLabel totalStockLabel;
    private JLabel batchesLabel;
    private JPanel alertsPanel;

    public DashboardPanel(ClientController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(15, 15));
        setBackground(MainFrame.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUI();
        refreshData();
    }

    private void buildUI() {
        // Title
        JLabel title = new JLabel("Dashboard Overview");
        title.setFont(MainFrame.FONT_TITLE);
        title.setForeground(MainFrame.TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setOpaque(false);

        // Summary cards row
        JPanel cardsRow = new JPanel(new GridLayout(1, 4, 15, 0));
        cardsRow.setOpaque(false);

        cardsRow.add(createSummaryCard("Total Products", "0", MainFrame.PRIMARY,
                c -> totalProductsLabel = c));
        cardsRow.add(createSummaryCard("Total Stock", "0", MainFrame.SUCCESS,
                c -> totalStockLabel = c));
        cardsRow.add(createSummaryCard("Low Stock Items", "0", MainFrame.WARNING,
                c -> lowStockLabel = c));
        cardsRow.add(createSummaryCard("Stock Batches", "0", new Color(156, 39, 176),
                c -> batchesLabel = c));

        content.add(cardsRow, BorderLayout.NORTH);

        // Alerts panel
        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setBackground(MainFrame.BG_CARD);
        alertsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel alertsTitle = new JLabel("⚠  Stock Alerts & Notifications");
        alertsTitle.setFont(MainFrame.FONT_HEADING);
        alertsTitle.setForeground(MainFrame.TEXT_PRIMARY);
        alertsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        alertsPanel.add(alertsTitle);
        alertsPanel.add(Box.createVerticalStrut(10));

        JScrollPane alertsScroll = new JScrollPane(alertsPanel);
        alertsScroll.setBorder(BorderFactory.createEmptyBorder());
        alertsScroll.getVerticalScrollBar().setUnitIncrement(16);

        content.add(alertsScroll, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel createSummaryCard(String title, String value, Color accentColor,
                                      java.util.function.Consumer<JLabel> labelSetter) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(MainFrame.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));

        // Color accent bar at top
        JPanel accent = new JPanel();
        accent.setPreferredSize(new Dimension(0, 4));
        accent.setBackground(accentColor);
        card.add(accent, BorderLayout.NORTH);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(MainFrame.FONT_SMALL);
        titleLabel.setForeground(MainFrame.TEXT_SECONDARY);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setForeground(accentColor);
        labelSetter.accept(valueLabel);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(valueLabel, BorderLayout.CENTER);
        card.add(textPanel, BorderLayout.CENTER);

        return card;
    }

    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int productCount = 0, totalStock = 0, lowStock = 0, batchCount = 0;
            List<Map<String, Object>> inventoryList = null;

            @Override
            protected Void doInBackground() {
                try {
                    List<Map<String, Object>> products = controller.getProducts();
                    productCount = products.size();

                    inventoryList = controller.getInventoryAll();
                    for (Map<String, Object> inv : inventoryList) {
                        int total = ((Number) inv.get("totalQuantity")).intValue();
                        totalStock += total;
                        if ((Boolean) inv.get("belowReorderLevel")) lowStock++;
                    }

                    List<Map<String, Object>> batches = controller.getStockBatchesAll();
                    batchCount = batches.size();
                } catch (Exception e) {
                    System.err.println("Dashboard refresh error: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                totalProductsLabel.setText(String.valueOf(productCount));
                totalStockLabel.setText(String.valueOf(totalStock));
                lowStockLabel.setText(String.valueOf(lowStock));
                batchesLabel.setText(String.valueOf(batchCount));

                // Update alerts
                alertsPanel.removeAll();
                JLabel alertsTitle = new JLabel("⚠  Stock Alerts & Notifications");
                alertsTitle.setFont(MainFrame.FONT_HEADING);
                alertsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
                alertsPanel.add(alertsTitle);
                alertsPanel.add(Box.createVerticalStrut(10));

                if (inventoryList != null) {
                    boolean hasAlerts = false;
                    for (Map<String, Object> inv : inventoryList) {
                        if ((Boolean) inv.get("belowReorderLevel")) {
                            hasAlerts = true;
                            String name = inv.get("productName") != null ?
                                    inv.get("productName").toString() : inv.get("productCode").toString();
                            int total = ((Number) inv.get("totalQuantity")).intValue();
                            JLabel alert = new JLabel("  ⚠  LOW STOCK: " + name +
                                    " — Only " + total + " units remaining (reorder level: 50)");
                            alert.setFont(MainFrame.FONT_BODY);
                            alert.setForeground(MainFrame.ERROR);
                            alert.setAlignmentX(Component.LEFT_ALIGNMENT);
                            alert.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
                            alertsPanel.add(alert);
                        }
                    }
                    if (!hasAlerts) {
                        JLabel noAlerts = new JLabel("  ✓  All stock levels are healthy");
                        noAlerts.setFont(MainFrame.FONT_BODY);
                        noAlerts.setForeground(MainFrame.SUCCESS);
                        noAlerts.setAlignmentX(Component.LEFT_ALIGNMENT);
                        alertsPanel.add(noAlerts);
                    }
                }
                alertsPanel.revalidate();
                alertsPanel.repaint();
            }
        };
        worker.execute();
    }
}
