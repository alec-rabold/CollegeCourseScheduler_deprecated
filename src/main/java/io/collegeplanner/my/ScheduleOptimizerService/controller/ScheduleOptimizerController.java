package io.collegeplanner.my.ScheduleOptimizerService.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.collegeplanner.my.ScheduleOptimizerService.model.CourseDto;
import io.collegeplanner.my.ScheduleOptimizerService.service.RegistrationDataDao;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static io.collegeplanner.my.ScheduleOptimizerService.Constants.*;

@Log4j2
@Controller
@RequestMapping(value = "/university")
public class ScheduleOptimizerController {

    @Inject
    private RegistrationDataDao registrationDataDao;

    @RequestMapping(value = "/{collegeName}")
    public String userPreferencesForSchedule(@PathVariable("collegeName") final String collegeName,
                                             final Model model) {
        // TODO: combine into one Dto
        final List<String> professors = ImmutableList.copyOf(
                registrationDataDao.getAllProfessors(collegeName));
        final List<CourseDto> courseDtos = ImmutableList.copyOf(
                registrationDataDao.getAllCoursesAndIDs(collegeName));

        model.addAttribute(PROFESSORS_RESULTSET_VARIABLE, professors);
        model.addAttribute(COURSES_AND_ID_RESULTSET_VARIABLE, courseDtos);

        return USER_PREFERENCES_FOR_SCHEDULE_VIEW;
    }

    /**
    @RequestMapping(value = "/{collegeName}/analyzing")
    public String analyzeSchedulePermutations() {
        // RequestDispatcher requestDispatcher;
        // present loading page until CompletableFuture finish

    }

    @RequestMapping(value = "/{collegeName}/customized-schedule")
     */

}
