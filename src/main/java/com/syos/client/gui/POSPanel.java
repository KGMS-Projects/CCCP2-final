package com.syos.client.gui;

import com.syos.client.controller.ClientController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Point of Sale panel for processing counter sales.
 * Features: product scanning, cart management, checkout with bill preview.
 * Uses SwingWorker for non-blocking server communication.
 */
public class POSPanel extends JPanel {

    private final ClientController controller;
    private final MainFrame mainFrame;

    private JTextField codeField;
    private JTextField qtyField;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel totalLabel;
    private JTextField cashField;
    private JTextArea billPreview;

    private final List<Map<String, Object>> cartItems = new ArrayList<>();
    private double cartTotal = 0.0;

    public POSPanel(ClientController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBackground(MainFrame.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUI();
        updateRealtimePreview(); // Show empty bill on start
    }

    private void buildUI() {
        // Title
        JLabel title = new JLabel("Point of Sale — Counter");
        title.setFont(MainFrame.FONT_TITLE);
        add(title, BorderLayout.NORTH);

        // Main split: Left (input + cart) | Right (bill preview)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(650);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        // Left panel: Input + Cart
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setOpaque(false);

        // Input row
        JPanel inputPanel = MainFrame.createCardPanel();
        // Use GridBagLayout to prevent wrapping and ensure buttons fit
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;

        gbc.gridx = 0; gbc.weightx = 0;
        inputPanel.add(new JLabel("Code:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        codeField = MainFrame.createStyledField(8);
        inputPanel.add(codeField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        inputPanel.add(new JLabel("Qty:"), gbc);

        gbc.gridx = 3; gbc.weightx = 0.5;
        qtyField = MainFrame.createStyledField(4);
        qtyField.setText("1");
        inputPanel.add(qtyField, gbc);

        gbc.gridx = 4; gbc.weightx = 0;
        JButton addBtn = MainFrame.createStyledButton("Add", MainFrame.PRIMARY);
        addBtn.setPreferredSize(new Dimension(80, 36));
        addBtn.addActionListener(e -> addToCart());
        inputPanel.add(addBtn, gbc);

        gbc.gridx = 5; gbc.weightx = 0;
        JButton clearBtn = MainFrame.createStyledButton("Clear", new Color(220, 53, 69)); // Bright Red
        clearBtn.setPreferredSize(new Dimension(80, 36));
        clearBtn.addActionListener(e -> clearCart());
        inputPanel.add(clearBtn, gbc);

        leftPanel.add(inputPanel, BorderLayout.NORTH);

        // Cart table
        cartModel = new DefaultTableModel(
                new String[]{"Code", "Name", "Unit Price", "Qty", "Discount%", "Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        cartTable = new JTable(cartModel);
        MainFrame.styleTable(cartTable);
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        leftPanel.add(cartScroll, BorderLayout.CENTER);

        // Bottom: Total + Checkout
        JPanel checkoutPanel = MainFrame.createCardPanel();
        checkoutPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 5));

        totalLabel = new JLabel("Total: Rs. 0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        totalLabel.setForeground(MainFrame.PRIMARY);
        checkoutPanel.add(totalLabel);

        checkoutPanel.add(new JLabel("Cash:"));
        cashField = MainFrame.createStyledField(10);
        // Live update change preview on cash entry
        cashField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateRealtimePreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateRealtimePreview(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateRealtimePreview(); }
        });
        checkoutPanel.add(cashField);

        JButton checkoutBtn = MainFrame.createStyledButton("Checkout", MainFrame.SUCCESS);
        checkoutBtn.setPreferredSize(new Dimension(140, 42));
        checkoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        checkoutBtn.addActionListener(e -> checkout());
        checkoutPanel.add(checkoutBtn);

        leftPanel.add(checkoutPanel, BorderLayout.SOUTH);
        splitPane.setLeftComponent(leftPanel);

        // Right panel: Bill preview
        JPanel rightPanel = MainFrame.createCardPanel();
        rightPanel.setLayout(new BorderLayout(5, 5));
        JLabel previewTitle = new JLabel("Live Bill Preview");
        previewTitle.setFont(MainFrame.FONT_HEADING);
        rightPanel.add(previewTitle, BorderLayout.NORTH);

        billPreview = new JTextArea();
        billPreview.setFont(new Font("Consolas", Font.PLAIN, 12));
        billPreview.setEditable(false);
        billPreview.setBackground(new Color(250, 250, 252));
        
        JScrollPane previewScroll = new JScrollPane(billPreview);
        previewScroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        rightPanel.add(previewScroll, BorderLayout.CENTER);

        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // Enter key triggers add
        codeField.addActionListener(e -> addToCart());
    }

    private void addToCart() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) return;

        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid quantity!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Fetch product info from server asynchronously
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                List<Map<String, Object>> products = controller.getProducts();
                for (Map<String, Object> p : products) {
                    if (code.equalsIgnoreCase(p.get("code").toString())) return p;
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> product = get();
                    if (product == null) {
                        JOptionPane.showMessageDialog(POSPanel.this,
                                "Product not found: " + code, "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    double price = ((Number) product.get("price")).doubleValue();
                    double discount = ((Number) product.get("discountPercentage")).doubleValue();
                    double discountedPrice = ((Number) product.get("discountedPrice")).doubleValue();
                    double itemTotal = discountedPrice * qty;

                    Map<String, Object> cartItem = new HashMap<>();
                    cartItem.put("productCode", code);
                    cartItem.put("quantity", qty);
                    cartItem.put("productName", product.get("name")); // Adjusted key for displayBill compatibility
                    cartItem.put("price", price);
                    cartItem.put("discountPercentage", discount);
                    cartItem.put("itemTotal", itemTotal);
                    cartItems.add(cartItem);

                    cartModel.addRow(new Object[]{
                            code,
                            product.get("name"),
                            String.format("Rs. %.2f", price),
                            qty,
                            String.format("%.1f%%", discount),
                            String.format("Rs. %.2f", itemTotal)
                    });

                    cartTotal += itemTotal;
                    totalLabel.setText(String.format("Total: Rs. %.2f", cartTotal));
                    
                    updateRealtimePreview(); // Live update

                    codeField.setText("");
                    qtyField.setText("1");
                    codeField.requestFocus();
                    mainFrame.setStatus("Added " + product.get("name") + " x" + qty + " to cart");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(POSPanel.this,
                            "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void clearCart() {
        cartItems.clear();
        cartModel.setRowCount(0);
        cartTotal = 0.0;
        totalLabel.setText("Total: Rs. 0.00");
        cashField.setText("");
        updateRealtimePreview();
    }

    private void updateRealtimePreview() {
        Map<String, Object> mockBill = new HashMap<>();
        mockBill.put("serialNumber", "PENDING");
        mockBill.put("transactionType", "COUNTER");
        mockBill.put("billDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(new Date()));
        mockBill.put("items", cartItems);
        mockBill.put("total", cartTotal);

        double cash = 0.0;
        try {
            if (!cashField.getText().trim().isEmpty()) {
                cash = Double.parseDouble(cashField.getText().trim());
            }
        } catch (NumberFormatException ignored) {}
        
        mockBill.put("cashTendered", cash);
        mockBill.put("change", Math.max(0.0, cash - cartTotal));

        displayBill(mockBill);
    }

    private void checkout() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty!", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double cash;
        try {
            cash = Double.parseDouble(cashField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter valid cash amount!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (cash < cartTotal) {
            JOptionPane.showMessageDialog(this,
                    String.format("Insufficient cash! Need at least Rs. %.2f", cartTotal),
                    "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final double finalCash = cash;
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                List<Map<String, Object>> saleItems = new ArrayList<>();
                for (Map<String, Object> ci : cartItems) {
                    Map<String, Object> si = new HashMap<>();
                    si.put("productCode", ci.get("productCode"));
                    si.put("quantity", ci.get("quantity"));
                    saleItems.add(si);
                }
                return controller.processSale(saleItems, finalCash, "COUNTER", null);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> bill = get();
                    displayBill(bill); // Show final official bill
                    mainFrame.setStatus("Sale completed! Bill #" + bill.get("serialNumber"));
                    
                    // Clear cart data without overwriting the final bill preview
                    cartItems.clear();
                    cartModel.setRowCount(0);
                    cartTotal = 0.0;
                    totalLabel.setText("Total: Rs. 0.00");
                    cashField.setText("");
                } catch (Exception e) {
                    String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    JOptionPane.showMessageDialog(POSPanel.this,
                            "Sale failed: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    @SuppressWarnings("unchecked")
    private void displayBill(Map<String, Object> bill) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ╔═══════════════════════════════════════╗\n");
        sb.append("  ║          S Y O S   S T O R E          ║\n");
        sb.append("  ║     Synex Outlet Store (Pvt) Ltd      ║\n");
        sb.append("  ╠═══════════════════════════════════════╣\n");
        sb.append(String.format("  ║ BILL NO: %-8s  TYPE: %-10s   ║%n",
                bill.get("serialNumber"), bill.get("transactionType")));
        sb.append(String.format("  ║ DATE: %-32s║%n",
                bill.get("billDate")));
        sb.append("  ╠═══════════════════════════════════════╣\n");
        sb.append(String.format("  ║ %-14s %4s %8s %9s ║%n", "ITEM", "QTY", "PRICE", "AMOUNT"));
        sb.append("  ╠═══════════════════════════════════════╣\n");

        List<Map<String, Object>> items = (List<Map<String, Object>>) bill.get("items");
        if (items != null && !items.isEmpty()) {
            for (Map<String, Object> item : items) {
                String name = item.get("productName").toString();
                if (name.length() > 14) name = name.substring(0, 14);
                sb.append(String.format("  ║ %-14s %4d %8.2f %9.2f ║%n",
                        name,
                        ((Number) item.get("quantity")).intValue(),
                        ((Number) item.get("price")).doubleValue(),
                        ((Number) item.get("itemTotal")).doubleValue()));
            }
        } else {
            sb.append("  ║                                       ║\n");
            sb.append("  ║        [  CART IS EMPTY  ]            ║\n");
            sb.append("  ║                                       ║\n");
        }

        sb.append("  ╠═══════════════════════════════════════╣\n");
        sb.append(String.format("  ║ TOTAL:                      %9.2f ║%n",
                ((Number) bill.get("total")).doubleValue()));
        sb.append(String.format("  ║ CASH:                       %9.2f ║%n",
                ((Number) bill.get("cashTendered")).doubleValue()));
        sb.append(String.format("  ║ CHANGE:                     %9.2f ║%n",
                ((Number) bill.get("change")).doubleValue()));
        sb.append("  ╠═══════════════════════════════════════╣\n");
        sb.append("  ║    Thank you for shopping at SYOS!    ║\n");
        sb.append("  ╚═══════════════════════════════════════╝\n");

        billPreview.setText(sb.toString());
        billPreview.setCaretPosition(0);
    }

    public void refreshData() {
        // POS panel doesn't need auto-refresh; cart is local
    }
}
