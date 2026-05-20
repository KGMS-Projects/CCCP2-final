package com.syos.usecases.reports;

/**
 * Abstract base class for report generation.
 * Implements Template Method Pattern - defines skeleton of report generation
 * algorithm.
 * Follows Open/Closed Principle - open for extension through subclasses.
 */
public abstract class ReportTemplate {

    /**
     * Template method defining the report generation process.
     * Final to prevent override of the algorithm structure.
     */
    public final String generateReport() {
        StringBuilder report = new StringBuilder();

        report.append(getReportHeader());
        report.append("\n");
        report.append(getReportBody());
        report.append("\n");
        report.append(getReportFooter());

        return report.toString();
    }

    /**
     * Hook method for report header.
     */
    protected abstract String getReportHeader();

    /**
     * Hook method for report body - must be implemented by subclasses.
     */
    protected abstract String getReportBody();

    /**
     * Hook method for report footer - can be overridden.
     */
    protected String getReportFooter() {
        return "--- End of Report ---";
    }
}
