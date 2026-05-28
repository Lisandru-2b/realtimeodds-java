package xyz.realtimeodds.entities;

final class PeriodLabels {
    private PeriodLabels() {}

    static String label(String period) {
        if (period == null || period.equals("full_match")) return "";
        return " (" + period + ")";
    }
}
