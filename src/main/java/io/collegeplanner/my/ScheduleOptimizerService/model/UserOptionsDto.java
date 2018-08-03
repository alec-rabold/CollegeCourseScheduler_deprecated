package io.collegeplanner.my.ScheduleOptimizerService.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserOptionsDto {
    private String[] wantedProfessors;
    private String[] unwantedProfessors;
    private String[] excludeProfessors;
    private int daysPerWeekPreference; // default to 1
    private int scheduleSpreadPreference; // default to 1
    private boolean showOnlineClasses; // default to true
    private boolean showWaitlistedClasses;
    private long[] unavailableTimesBitBlocks;
}
