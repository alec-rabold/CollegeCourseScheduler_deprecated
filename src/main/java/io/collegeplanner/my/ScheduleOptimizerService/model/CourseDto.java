package io.collegeplanner.my.ScheduleOptimizerService.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
@Setter
public class CourseDto {
    private String title;
    private String units;
    private String seats;
    private String courseID;
    private String schedNumber;
    private List<String> days;
    private List<String> times;
    private List<String> locations;
    private List<String> instructors;
    private CourseDto relatedCourse; // So far, only necessary for UCSB

    public boolean isComplete() {
        return StringUtils.isNoneEmpty(courseID, schedNumber, title, units, seats) && !times.isEmpty();
    }
}
