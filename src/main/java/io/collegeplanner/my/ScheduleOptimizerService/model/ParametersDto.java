package io.collegeplanner.my.ScheduleOptimizerService.model;

import lombok.*;
import lombok.experimental.Wither;

import java.util.List;

@Getter
@Setter
@Wither
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametersDto {
    private String year;
    private String season;
    private String spreadPreference;
    private String waitlistOption;
    private String onlineOption;
    private String numDaysOption;
    private String problems;
    private String suggestion;
    private String[] chosenCourses;
    private String[] wantedProfessors;
    private String[] unwantedProfessors;
    private String[] excludeProfessors;
    private String[] unavStartTimes;
    private String[] unavEndTimes;
    private List<String[]> setOfDays;
    private long[] unavTimesBitBlocks;
    private boolean isMobile;
}
