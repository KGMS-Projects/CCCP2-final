package com.syos.client.gui;

import com.syos.client.controller.ClientController;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

/**
 * Online Store panel in the POS Swing GUI.
 * Since the Online Store is now a web application served by embedded Tomcat,
 * this panel shows a link to open it in the browser and displays online
 * sales/inventory info for management purposes.
 */
public class OnlineStorePanel extends JPanel {

    private final ClientController controller;
    private final MainFrame mainFrame;
    private static final String STORE_URL = "http://localhost:8080";

    public OnlineStorePanel(ClientController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBackground(MainFrame.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("Online Store Management");
        title.setFont(MainFrame.FONT_TITLE);
        add(title, BorderLayout.NORTH);

        // Center panel with info and launch button
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Info card
        JPanel card = MainFrame.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(500, 320));

        JLabel icon = new JLabel("🌐");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(icon);
        card.add(Box.createVerticalStrut(15));

        JLabel heading = new JLabel("Online Store — Web Application");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(10));

        JLabel desc = new JLabel("<html><center>The online store runs as a web application in the browser.<br>" +
                "Customers can browse products, add to cart, and checkout.<br>" +
                "All online orders appear here and in the Reports tab.</center></html>");
        desc.setFont(MainFrame.FONT_BODY);
        desc.setForeground(MainFrame.TEXT_SECONDARY);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(desc);
        card.add(Box.createVerticalStrut(20));

        JLabel urlLabel = new JLabel(STORE_URL);
        urlLabel.setFont(new Font("Consolas", Font.PLAIN, 16));
        urlLabel.setForeground(MainFrame.PRIMARY);
        urlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(urlLabel);
        card.add(Box.createVerticalStrut(15));

        JButton openBtn = MainFrame.createStyledButton("Open Online Store in Browser", MainFrame.PRIMARY);
        openBtn.setPreferredSize(new Dimension(300, 45));
        openBtn.setMaximumSize(new Dimension(300, 45));
        openBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        openBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        openBtn.addActionListener(e -> openBrowser());
        card.add(openBtn);
        card.add(Box.createVerticalStrut(10));

        JLabel note = new JLabel("<html><center><i>Online sales use the same inventory pool.<br>" +
                "Changes are synced in real-time across all clients.</i></center></html>");
        note.setFont(MainFrame.FONT_SMALL);
        note.setForeground(MainFrame.TEXT_SECONDARY);
        note.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(note);

        gbc.gridy = 0;
        centerPanel.add(card, gbc);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void openBrowser() {
        try {
            Desktop.getDesktop().browse(new URI(STORE_URL));
            mainFrame.setStatus("Online Store opened in browser");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Could not open browser. Please visit:\n" + STORE_URL,
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
