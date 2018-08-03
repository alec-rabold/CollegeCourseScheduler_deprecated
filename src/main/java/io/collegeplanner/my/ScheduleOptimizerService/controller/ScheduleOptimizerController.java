package io.collegeplanner.my.ScheduleOptimizerService.controller;

import com.google.common.collect.ImmutableMap;
import io.collegeplanner.my.ScheduleOptimizerService.model.RunScheduleAnalyzer;
import io.collegeplanner.my.ScheduleOptimizerService.service.MySqlConnectionFactory;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.sql.ResultSet;

import static io.collegeplanner.my.ScheduleOptimizerService.Constants.*;

@Log4j2
@Controller
@RequestMapping(value = "/university")
public class ScheduleOptimizerController {

    @RequestMapping(value = "/{collegeName}")
    public String userPreferencesForSchedule(
            @PathVariable("collegeName") final String collegeName, final Model model) {
        try {
            MySqlConnectionFactory.createConnection();
            final ResultSet coursesAndIDs = MySqlConnectionFactory.getRegistrationDataDao()
                    .getAllCoursesAndIDs(collegeName);
            final ResultSet professors = MySqlConnectionFactory.getRegistrationDataDao()
                    .getAllProfessors(collegeName);
            model.addAllAttributes(
                    ImmutableMap.<String, ResultSet>builder()
                        .put(COURSES_AND_ID_RESULTSET_VARIABLE, coursesAndIDs)
                        .put(PROFESSORS_RESULTSET_VARIABLE, professors)
                        .build()
            );
        } catch(Exception e) {
            log.fatal("Unable to read from course registration database.", e);
        }
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
