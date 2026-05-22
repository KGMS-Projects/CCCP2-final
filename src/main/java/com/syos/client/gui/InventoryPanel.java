package com.syos.client.gui;

import com.syos.client.controller.ClientController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;


public class InventoryPanel extends JPanel {

    private final ClientController controller;
    private JTable productTable;
    private DefaultTableModel productModel;
    private JTable inventoryTable;
    private DefaultTableModel inventoryModel;

    public InventoryPanel(ClientController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(15, 15));
        setBackground(MainFrame.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUI();
        refreshData();
    }

    private void buildUI() {
        // Title + Actions
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Inventory Management");
        title.setFont(MainFrame.FONT_TITLE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        JButton addBtn = MainFrame.createStyledButton("+ Add Product", MainFrame.PRIMARY);
        addBtn.addActionListener(e -> showAddProductDialog());
        actions.add(addBtn);

        JButton refreshBtn = MainFrame.createStyledButton("Refresh", MainFrame.TEXT_SECONDARY);
        refreshBtn.addActionListener(e -> refreshData());
        actions.add(refreshBtn);

        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Split: Products table (top) | Inventory levels (bottom)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(280);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        // Products table
        JPanel productsPanel = MainFrame.createCardPanel();
        productsPanel.setLayout(new BorderLayout(5, 5));
        JLabel prodLabel = new JLabel("Products Catalog");
        prodLabel.setFont(MainFrame.FONT_HEADING);
        productsPanel.add(prodLabel, BorderLayout.NORTH);

        productModel = new DefaultTableModel(
                new String[]{"Code", "Name", "Unit", "Price (Rs.)", "Discount %", "Discounted Price"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        productTable = new JTable(productModel);
        MainFrame.styleTable(productTable);
        productsPanel.add(new JScrollPane(productTable), BorderLayout.CENTER);
        splitPane.setTopComponent(productsPanel);

        // Inventory table
        JPanel invPanel = MainFrame.createCardPanel();
        invPanel.setLayout(new BorderLayout(5, 5));
        JLabel invLabel = new JLabel("Inventory Levels");
        invLabel.setFont(MainFrame.FONT_HEADING);
        invPanel.add(invLabel, BorderLayout.NORTH);

        inventoryModel = new DefaultTableModel(
                new String[]{"Code", "Product", "Shelf", "Store", "Online", "Total", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        inventoryTable = new JTable(inventoryModel);
        MainFrame.styleTable(inventoryTable);

        // Custom renderer for status column coloring
        inventoryTable.getColumnModel().getColumn(6).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                        if (!isSelected) {
                            if ("LOW".equals(value)) {
                                setForeground(MainFrame.ERROR);
                                setBackground(new Color(255, 235, 235));
                                setFont(getFont().deriveFont(Font.BOLD));
                            } else {
                                setForeground(MainFrame.SUCCESS);
                                setBackground(row % 2 == 0 ? MainFrame.BG_CARD : MainFrame.TABLE_ALT_ROW);
                                setFont(getFont().deriveFont(Font.PLAIN));
                            }
                        }
                        return this;
                    }
                });

        invPanel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        splitPane.setBottomComponent(invPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void showAddProductDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add New Product",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 400);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        form.setBackground(MainFrame.BG_CARD);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField codeF = MainFrame.createStyledField(15);
        JTextField nameF = MainFrame.createStyledField(15);
        JTextField unitF = MainFrame.createStyledField(15); unitF.setText("pcs");
        JTextField priceF = MainFrame.createStyledField(15);
        JTextField discountF = MainFrame.createStyledField(15); discountF.setText("0");
        JTextField stockF = MainFrame.createStyledField(15); stockF.setText("0");
        JTextField expiryF = MainFrame.createStyledField(15);
        expiryF.setToolTipText("yyyy-MM-dd");

        String[] labels = {"Code:", "Name:", "Unit:", "Price (Rs.):", "Discount %:",
                "Initial Stock:", "Expiry Date:"};
        JTextField[] fields = {codeF, nameF, unitF, priceF, discountF, stockF, expiryF};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0.3;
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(MainFrame.FONT_BODY);
            form.add(lbl, gbc);
            gbc.gridx = 1; gbc.weightx = 0.7;
            form.add(fields[i], gbc);
        }

        gbc.gridx = 0; gbc.gridy = labels.length; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        JButton saveBtn = MainFrame.createStyledButton("Save Product", MainFrame.SUCCESS);
        saveBtn.setPreferredSize(new Dimension(0, 40));
        saveBtn.addActionListener(e -> {
            try {
                controller.addProduct(
                        codeF.getText().trim(), nameF.getText().trim(), unitF.getText().trim(),
                        Double.parseDouble(priceF.getText().trim()),
                        Double.parseDouble(discountF.getText().trim()),
                        Integer.parseInt(stockF.getText().trim()),
                        expiryF.getText().trim()
                );
                JOptionPane.showMessageDialog(dialog, "Product added successfully!");
                dialog.dispose();
                refreshData();
            } catch (Exception ex) {
                String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                JOptionPane.showMessageDialog(dialog, "Error: " + msg,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        form.add(saveBtn, gbc);

        dialog.add(form);
        dialog.setVisible(true);
    }

    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<Map<String, Object>> products;
            List<Map<String, Object>> inventory;

            @Override
            protected Void doInBackground() throws Exception {
                products = controller.getProducts();
                inventory = controller.getInventoryAll();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    productModel.setRowCount(0);
                    for (Map<String, Object> p : products) {
                        productModel.addRow(new Object[]{
                                p.get("code"),
                                p.get("name"),
                                p.get("unit"),
                                String.format("%.2f", ((Number) p.get("price")).doubleValue()),
                                String.format("%.1f", ((Number) p.get("discountPercentage")).doubleValue()),
                                String.format("%.2f", ((Number) p.get("discountedPrice")).doubleValue())
                        });
                    }

                    inventoryModel.setRowCount(0);
                    for (Map<String, Object> inv : inventory) {
                        String name = inv.get("productName") != null ?
                                inv.get("productName").toString() : "-";
                        boolean low = (Boolean) inv.get("belowReorderLevel");
                        inventoryModel.addRow(new Object[]{
                                inv.get("productCode"),
                                name,
                                ((Number) inv.get("shelfQuantity")).intValue(),
                                ((Number) inv.get("storeQuantity")).intValue(),
                                ((Number) inv.get("onlineQuantity")).intValue(),
                                ((Number) inv.get("totalQuantity")).intValue(),
                                low ? "LOW" : "OK"
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Inventory refresh error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}
