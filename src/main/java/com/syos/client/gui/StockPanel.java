package com.syos.client.gui;

import com.syos.client.controller.ClientController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;


public class StockPanel extends JPanel {

    private final ClientController controller;
    private JTable batchTable;
    private DefaultTableModel batchModel;

    public StockPanel(ClientController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(15, 15));
        setBackground(MainFrame.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUI();
        refreshData();
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Stock & Batch Management");
        title.setFont(MainFrame.FONT_TITLE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton addBatchBtn = MainFrame.createStyledButton("+ Add Batch", MainFrame.PRIMARY);
        addBatchBtn.addActionListener(e -> showAddBatchDialog());
        actions.add(addBatchBtn);

        JButton toShelfBtn = MainFrame.createStyledButton("Transfer → Shelf", new Color(0, 137, 123));
        toShelfBtn.addActionListener(e -> showTransferDialog("STORE_TO_SHELF", "Store → Shelf"));
        actions.add(toShelfBtn);

        JButton toOnlineBtn = MainFrame.createStyledButton("Transfer → Online", new Color(156, 39, 176));
        toOnlineBtn.addActionListener(e -> showTransferDialog("STORE_TO_ONLINE", "Store → Online"));
        actions.add(toOnlineBtn);

        JButton refreshBtn = MainFrame.createStyledButton("Refresh", MainFrame.TEXT_SECONDARY);
        refreshBtn.addActionListener(e -> refreshData());
        actions.add(refreshBtn);

        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Batch table
        JPanel tablePanel = MainFrame.createCardPanel();
        tablePanel.setLayout(new BorderLayout(5, 5));

        batchModel = new DefaultTableModel(
                new String[]{"Batch ID", "Product Code", "Purchase Date", "Qty",
                        "Expiry Date", "Days Left", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        batchTable = new JTable(batchModel);
        MainFrame.styleTable(batchTable);

        // Status column coloring
        batchTable.getColumnModel().getColumn(6).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                        if (!isSelected) {
                            setFont(getFont().deriveFont(Font.BOLD));
                            if ("EXPIRED".equals(value)) {
                                setForeground(MainFrame.ERROR);
                                setBackground(new Color(255, 235, 235));
                            } else if ("EXPIRING".equals(value)) {
                                setForeground(MainFrame.WARNING);
                                setBackground(new Color(255, 248, 225));
                            } else {
                                setForeground(MainFrame.SUCCESS);
                                setBackground(row % 2 == 0 ? MainFrame.BG_CARD : MainFrame.TABLE_ALT_ROW);
                            }
                        }
                        return this;
                    }
                });

        tablePanel.add(new JScrollPane(batchTable), BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
    }

    private void showAddBatchDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Add Stock Batch", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(380, 280);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        form.setBackground(MainFrame.BG_CARD);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 5, 6, 5);

        JTextField codeF = MainFrame.createStyledField(15);
        JTextField qtyF = MainFrame.createStyledField(15);
        JTextField expiryF = MainFrame.createStyledField(15);
        expiryF.setToolTipText("yyyy-MM-dd");

        String[] labels = {"Product Code:", "Quantity:", "Expiry Date:"};
        JTextField[] fields = {codeF, qtyF, expiryF};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0.3;
            form.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1; gbc.weightx = 0.7;
            form.add(fields[i], gbc);
        }

        gbc.gridx = 0; gbc.gridy = labels.length; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        JButton saveBtn = MainFrame.createStyledButton("Add Batch", MainFrame.SUCCESS);
        saveBtn.addActionListener(e -> {
            try {
                controller.addStockBatch(
                        codeF.getText().trim(),
                        Integer.parseInt(qtyF.getText().trim()),
                        expiryF.getText().trim()
                );
                JOptionPane.showMessageDialog(dialog, "Stock batch added!");
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

    private void showTransferDialog(String transferType, String label) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Transfer Stock: " + label, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(380, 230);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        form.setBackground(MainFrame.BG_CARD);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 5, 6, 5);

        JTextField codeF = MainFrame.createStyledField(15);
        JTextField qtyF = MainFrame.createStyledField(15);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        form.add(new JLabel("Product Code:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(codeF, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        form.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(qtyF, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        JButton transferBtn = MainFrame.createStyledButton("Transfer", new Color(0, 137, 123));
        transferBtn.addActionListener(e -> {
            try {
                controller.transferStock(
                        codeF.getText().trim(),
                        Integer.parseInt(qtyF.getText().trim()),
                        transferType
                );
                JOptionPane.showMessageDialog(dialog, "Stock transferred!");
                dialog.dispose();
                refreshData();
            } catch (Exception ex) {
                String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                JOptionPane.showMessageDialog(dialog, "Error: " + msg,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        form.add(transferBtn, gbc);
        dialog.add(form);
        dialog.setVisible(true);
    }

    public void refreshData() {
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                return controller.getStockBatchesAll();
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> batches = get();
                    batchModel.setRowCount(0);
                    for (Map<String, Object> b : batches) {
                        batchModel.addRow(new Object[]{
                                b.get("batchId"),
                                b.get("productCode"),
                                b.get("purchaseDate"),
                                ((Number) b.get("quantity")).intValue(),
                                b.get("expiryDate"),
                                ((Number) b.get("daysUntilExpiry")).intValue(),
                                b.get("status")
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Stock refresh error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}
