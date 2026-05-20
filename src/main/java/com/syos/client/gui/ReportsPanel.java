package com.syos.client.gui;

import com.syos.client.controller.ClientController;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Reports panel for generating and viewing business reports.
 * Supports: Daily Sales, Reshelve, Reorder, Stock, and Bill reports.
 */
public class ReportsPanel extends JPanel {

    private final ClientController controller;
    private JComboBox<String> reportTypeCombo;
    private JSpinner dateSpinner;
    private JComboBox<String> txTypeCombo;
    private JTextArea reportArea;

    public ReportsPanel(ClientController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(15, 15));
        setBackground(MainFrame.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("Reports");
        title.setFont(MainFrame.FONT_TITLE);
        add(title, BorderLayout.NORTH);

        // Controls
        JPanel controls = MainFrame.createCardPanel();
        controls.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 5));

        controls.add(createLabel("Report Type:"));
        reportTypeCombo = new JComboBox<>(new String[]{
                "Daily Sales", "Reshelve", "Reorder Levels", "Stock", "Bill"
        });
        reportTypeCombo.setFont(MainFrame.FONT_BODY);
        reportTypeCombo.setPreferredSize(new Dimension(180, 34));
        controls.add(reportTypeCombo);

        controls.add(createLabel("Date:"));
        SpinnerDateModel dateModel = new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH);
        dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setFont(MainFrame.FONT_BODY);
        dateSpinner.setPreferredSize(new Dimension(120, 34));
        controls.add(dateSpinner);

        controls.add(createLabel("Transaction:"));
        txTypeCombo = new JComboBox<>(new String[]{"ALL", "COUNTER", "ONLINE"});
        txTypeCombo.setFont(MainFrame.FONT_BODY);
        txTypeCombo.setPreferredSize(new Dimension(130, 34));
        controls.add(txTypeCombo);

        JButton generateBtn = MainFrame.createStyledButton("Generate Report", MainFrame.PRIMARY);
        generateBtn.addActionListener(e -> generateReport());
        controls.add(generateBtn);

        // Report display
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setOpaque(false);
        contentPanel.add(controls, BorderLayout.NORTH);

        reportArea = new JTextArea();
        reportArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        reportArea.setEditable(false);
        reportArea.setBackground(new Color(250, 250, 252));
        reportArea.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        reportArea.setText("  Select a report type and click 'Generate Report'...");

        JScrollPane scroll = new JScrollPane(reportArea);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        contentPanel.add(scroll, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(MainFrame.FONT_BODY);
        return lbl;
    }

    private void generateReport() {
        String selected = (String) reportTypeCombo.getSelectedItem();
        String reportType;
        switch (selected) {
            case "Daily Sales": reportType = "DAILY_SALES"; break;
            case "Reshelve": reportType = "RESHELVE"; break;
            case "Reorder Levels": reportType = "REORDER"; break;
            case "Stock": reportType = "STOCK"; break;
            case "Bill": reportType = "BILL"; break;
            default: reportType = "DAILY_SALES";
        }

        Date selectedDate = (Date) dateSpinner.getValue();
        String date = new SimpleDateFormat("yyyy-MM-dd").format(selectedDate);

        String txType = (String) txTypeCombo.getSelectedItem();

        reportArea.setText("  Generating report...");

        final String fReportType = reportType;
        final String fDate = date;
        final String fTxType = txType;

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return controller.generateReport(fReportType, fDate, fTxType);
            }

            @Override
            protected void done() {
                try {
                    String report = get();
                    reportArea.setText(report);
                    reportArea.setCaretPosition(0);
                } catch (Exception e) {
                    String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    reportArea.setText("  Error generating report: " + msg);
                }
            }
        };
        worker.execute();
    }
}
