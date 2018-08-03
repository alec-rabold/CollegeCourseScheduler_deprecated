package io.collegeplanner.my.ScheduleOptimizerService.service;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.List;

import static io.collegeplanner.my.ScheduleOptimizerService.Constants.COURSE_AND_IDS_TABLES;
import static io.collegeplanner.my.ScheduleOptimizerService.Constants.PROFESSORS_TABLES;
import static io.collegeplanner.my.ScheduleOptimizerService.Constants.SELECT_ALL_FROM_PLACEHOLDER_TABLE_QUERY;

@Log4j2
@Service
@Repository("registrationDataDao")
@RequiredArgsConstructor
public class RegistrationDataDaoImpl implements RegistrationDataDao {

    Jdbi jdbi = new Jdbi()

    private final Connection dbConnection;

    public ResultSet getAllProfessors(final String university) throws SQLException {
        return this.createPreparedStatementWithParameters(SELECT_ALL_FROM_PLACEHOLDER_TABLE_QUERY,
                ImmutableList.of(PROFESSORS_TABLES.get(university)))
                .executeQuery();
    }

    public ResultSet getAllCoursesAndIDs(final String university) throws SQLException {
        return this.createPreparedStatementWithParameters(SELECT_ALL_FROM_PLACEHOLDER_TABLE_QUERY,
                ImmutableList.of(COURSE_AND_IDS_TABLES.get(university)))
                .executeQuery();
    }

    private PreparedStatement createPreparedStatementWithParameters(final String query, final List<Object> parameters) throws SQLException{
        final PreparedStatement preparedStatement = this.dbConnection.prepareStatement(query);
        int parameterNumber = 1; // these are not zero-indexed
        for(final Object param : parameters)
            preparedStatement.setObject(parameterNumber++, param);
        return preparedStatement;
    }
}
