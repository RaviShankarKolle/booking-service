package com.library.fine.schedule;

import com.library.fine.fine.FineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically syncs overdue fines with the borrow service (same logic as {@code POST /api/v1/fines/recalculate}).
 */
@Component
public class FineRecalculationScheduler {

    private static final Logger log = LoggerFactory.getLogger(FineRecalculationScheduler.class);

    /**
     * Spring 6-field cron: second, minute, hour, day of month, month, day of week.
     * Runs at the top of every hour.
     */
    private static final String RECALCULATE_CRON = "0 0 * * * *";

    private final FineService fineService;

    public FineRecalculationScheduler(FineService fineService) {
        this.fineService = fineService;
    }

    @Scheduled(cron = RECALCULATE_CRON)
    public void recalculateOverdueFines() {
        try {
            int updated = fineService.recalculateOverdueFines();
            log.info("Scheduled overdue fine recalculation completed; {} fine(s) touched", updated);
        } catch (Exception ex) {
            log.error("Scheduled overdue fine recalculation failed", ex);
        }
    }
}
