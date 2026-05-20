package com.syos.client.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A modern, dark-themed sidebar panel for navigation.
 * Replaces the default JTabbedPane to provide a premium web-like UI.
 */
public class SidebarPanel extends JPanel {

    private static final Color BG_COLOR = new Color(30, 41, 59);
    private static final Color TEXT_COLOR = new Color(203, 213, 225);
    private static final Color HOVER_COLOR = new Color(51, 65, 85);
    private static final Color ACTIVE_BG = new Color(15, 23, 42);
    private static final Color ACTIVE_TEXT = Color.WHITE;
    private static final Color ACCENT_COLOR = new Color(56, 189, 248);

    private final String[] navNames;
    private final String[] navIcons;
    private final NavItem[] navItems;

    private int activeIndex = 0;
    private NavClickListener clickListener;

    public interface NavClickListener {
        void onNavClicked(int index);
    }

    public SidebarPanel(String[] navNames, String[] navIcons) {
        this.navNames = navNames;
        this.navIcons = navIcons;
        this.navItems = new NavItem[navNames.length];
        
        setPreferredSize(new Dimension(240, 0));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout());

        buildUI();
    }

    public void setNavClickListener(NavClickListener listener) {
        this.clickListener = listener;
    }

    private void buildUI() {
        // Brand Area (Top)
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBackground(BG_COLOR);
        brandPanel.setBorder(new EmptyBorder(25, 20, 25, 20));

        JLabel brandName = new JLabel("SYOS POS");
        brandName.setFont(new Font("Segoe UI", Font.BOLD, 24));
        brandName.setForeground(Color.WHITE);
        brandName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subBrand = new JLabel("Inventory System");
        subBrand.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subBrand.setForeground(new Color(148, 163, 184));
        subBrand.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandPanel.add(brandName);
        brandPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        brandPanel.add(subBrand);

        add(brandPanel, BorderLayout.NORTH);

        // Navigation Links (Center)
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(BG_COLOR);
        navPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        for (int i = 0; i < navNames.length; i++) {
            NavItem item = new NavItem(navNames[i], navIcons[i], i);
            navItems[i] = item;
            navPanel.add(item);
            navPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        
        // Wrap nav in a panel to align top
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_COLOR);
        wrapper.add(navPanel, BorderLayout.NORTH);

        add(wrapper, BorderLayout.CENTER);

        // Footer / Connection Indicator
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_COLOR);
        footer.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel status = new JLabel("● Online");
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        status.setForeground(new Color(34, 197, 94));
        footer.add(status, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);

        // Set initial active state
        if (navItems.length > 0) {
            navItems[0].setActive(true);
        }
    }

    /**
     * Custom component for each navigation item.
     * Handles hover effects and active state rendering.
     */
    private class NavItem extends JPanel {
        private final int index;
        private boolean isActive = false;
        private boolean isHovered = false;

        public NavItem(String text, String iconStr, int index) {
            this.index = index;
            setLayout(new BorderLayout());
            setBackground(BG_COLOR);
            setMaximumSize(new Dimension(240, 48));
            setPreferredSize(new Dimension(240, 48));
            setBorder(new EmptyBorder(0, 20, 0, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel label = new JLabel(iconStr + "   " + text);
            label.setFont(new Font("Segoe UI", Font.BOLD, 14));
            label.setForeground(TEXT_COLOR);
            add(label, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isActive) {
                        isHovered = true;
                        repaint();
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isActive) {
                        isHovered = false;
                        repaint();
                    }
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    if (clickListener != null && !isActive) {
                        setActiveItem(index);
                        clickListener.onNavClicked(index);
                    }
                }
            });
        }

        public void setActive(boolean active) {
            this.isActive = active;
            this.isHovered = false;
            Component[] comps = getComponents();
            if (comps.length > 0 && comps[0] instanceof JLabel) {
                ((JLabel) comps[0]).setForeground(active ? ACTIVE_TEXT : TEXT_COLOR);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (isActive) {
                g2.setColor(ACTIVE_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Left accent bar
                g2.setColor(ACCENT_COLOR);
                g2.fillRect(0, 0, 4, getHeight());
            } else if (isHovered) {
                g2.setColor(HOVER_COLOR);
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else {
                g2.setColor(BG_COLOR);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void setActiveItem(int index) {
        if (activeIndex >= 0 && activeIndex < navItems.length) {
            navItems[activeIndex].setActive(false);
        }
        activeIndex = index;
        navItems[activeIndex].setActive(true);
    }
}
