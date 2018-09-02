package io.collegeplanner.my.ScheduleOptimizerService.service;

import io.collegeplanner.my.ScheduleOptimizerService.model.CourseDto;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

import static io.collegeplanner.my.ScheduleOptimizerService.Constants.SELECT_ALL_FROM_TABLENAME_QUERY;

@Repository
public interface RegistrationDataDao {

    @SqlQuery(SELECT_ALL_FROM_TABLENAME_QUERY)
    List<String> getAllProfessors(final String tableName);

    @SqlQuery(SELECT_ALL_FROM_TABLENAME_QUERY)
    @RegisterBeanMapper(CourseDto.class)
    List<CourseDto> getAllCoursesAndIDs(final String tableName);
}
