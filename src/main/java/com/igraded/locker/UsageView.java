package com.igraded.locker;

/** How much of a teacher's Locker space is used. */
public record UsageView(long usedBytes, long quotaBytes) {

    public int percent() {
        if (quotaBytes <= 0) {
            return 100;
        }
        return (int) Math.min(100, usedBytes * 100 / quotaBytes);
    }

    public long remainingBytes() {
        return Math.max(0, quotaBytes - usedBytes);
    }

    /** True once the Locker is nearly full, so the bar can change colour. */
    public boolean high() {
        return percent() >= 90;
    }

    public String usedLabel() {
        return FileRules.humanSize(usedBytes);
    }

    public String quotaLabel() {
        return FileRules.humanSize(quotaBytes);
    }
}
