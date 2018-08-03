package io.collegeplanner.my.ScheduleOptimizerService.service;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface RegistrationDataDao {
    ResultSet getAllProfessors(final String university) throws SQLException;
    ResultSet getAllCoursesAndIDs(final String university) throws SQLException;
}
