package com.syos.client;

import com.syos.client.controller.ClientController;
import com.syos.client.gui.MainFrame;

import javax.swing.*;
import java.awt.*;

/**
 * SYOS Client - Main entry point for the GUI client tier.
 * 
 * This is a SEPARATE application from the server. It:
 * 1. Connects to the SYOS server via TCP socket
 * 2. Launches the Swing GUI
 * 3. All data operations go through the server (no direct DB access)
 * 
 * Usage: java com.syos.client.SyosClient [host] [port]
 * Default: localhost 5000
 * 
 * Clean Architecture:
 * - This client contains only the Frameworks & Drivers layer (GUI)
 * - The Interface Adapter (ClientController) translates GUI events to network requests
 * - Entities, Use Cases, and Database are all on the server side
 */
public class SyosClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try { port = Integer.parseInt(args[1]); }
            catch (NumberFormatException e) { /* use default */ }
        }

        final String serverHost = host;
        final int serverPort = port;

        // Set system look and feel for a native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Customize UI defaults for modern appearance
            UIManager.put("TabbedPane.selectedBackground", new Color(240, 242, 247));
            UIManager.put("TabbedPane.background", new Color(245, 245, 250));
            UIManager.put("TabbedPane.contentAreaColor", new Color(240, 242, 247));
            UIManager.put("Panel.background", new Color(240, 242, 247));
            UIManager.put("OptionPane.background", Color.WHITE);
            UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.BOLD, 13));
            UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("ComboBox.font", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));
        } catch (Exception e) {
            // Fallback to default L&F
        }

        // Start GUI on the Event Dispatch Thread (Swing thread safety requirement)
        SwingUtilities.invokeLater(() -> {
            // Show connection dialog
            JDialog connectDialog = new JDialog((Frame) null, "SYOS - Connect to Server", true);
            connectDialog.setSize(420, 220);
            connectDialog.setLocationRelativeTo(null);
            connectDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(6, 5, 6, 5);

            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            JLabel title = new JLabel("Connect to SYOS Server");
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            panel.add(title, gbc);

            gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.3;
            panel.add(new JLabel("Host:"), gbc);
            gbc.gridx = 1; gbc.weightx = 0.7;
            JTextField hostField = new JTextField(serverHost, 15);
            hostField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            panel.add(hostField, gbc);

            gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
            panel.add(new JLabel("Port:"), gbc);
            gbc.gridx = 1; gbc.weightx = 0.7;
            JTextField portField = new JTextField(String.valueOf(serverPort), 15);
            portField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            panel.add(portField, gbc);

            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            gbc.insets = new Insets(15, 5, 5, 5);
            JButton connectBtn = MainFrame.createStyledButton("Connect", MainFrame.PRIMARY);
            connectBtn.setPreferredSize(new Dimension(0, 40));

            final ServerConnection[] connRef = new ServerConnection[1];

            connectBtn.addActionListener(e -> {
                connectBtn.setEnabled(false);
                connectBtn.setText("Connecting...");

                String h = hostField.getText().trim();
                int p;
                try { p = Integer.parseInt(portField.getText().trim()); }
                catch (NumberFormatException ex) { p = DEFAULT_PORT; }

                ServerConnection conn = new ServerConnection(h, p);
                try {
                    conn.connect();
                    connRef[0] = conn;
                    connectDialog.dispose();
                } catch (Exception ex) {
                    connectBtn.setEnabled(true);
                    connectBtn.setText("Connect");
                    JOptionPane.showMessageDialog(connectDialog,
                            "Cannot connect to server at " + h + ":" + p + "\n" + ex.getMessage(),
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            panel.add(connectBtn, gbc);

            connectDialog.add(panel);
            connectDialog.setVisible(true);

            // After dialog closes, check if connected
            if (connRef[0] != null && connRef[0].isConnected()) {
                ClientController controller = new ClientController(connRef[0]);
                MainFrame mainFrame = new MainFrame(controller, connRef[0]);
                mainFrame.setVisible(true);
            } else {
                System.out.println("Connection cancelled. Exiting.");
                System.exit(0);
            }
        });
    }
}
