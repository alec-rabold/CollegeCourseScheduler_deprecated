package io.collegeplanner.my.ScheduleOptimizerService.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class ScheduleDto implements Comparable<ScheduleDto> {
    private final List<CourseSectionDto> coursesInSchedule;
    private final int numGoodProfessors;
    private final int numBadProfessors;
    private final int gapTimeTightness;
    private final int numClassDaysPerWeek;
    private final long[] layout;
    private int algorithmValue;

    // TODO: more logical ranking algorithm..
    public void setAlgorithmValue(UserOptionsDto userOptions) {
        this.algorithmValue = userOptions.getScheduleSpreadPreference()
                * (2  * this.gapTimeTightness)
                - (45 * this.numGoodProfessors)
                + (55 * this.numBadProfessors)
                + (this.numClassDaysPerWeek * 30)
                * (userOptions.getDaysPerWeekPreference());
    }

    @Override
    public int compareTo(ScheduleDto otherSchedule) {
        return ((Integer)otherSchedule.algorithmValue).compareTo(this.algorithmValue);
    }
}
