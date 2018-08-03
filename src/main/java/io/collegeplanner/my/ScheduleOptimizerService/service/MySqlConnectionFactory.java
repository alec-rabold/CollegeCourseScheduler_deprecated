package io.collegeplanner.my.ScheduleOptimizerService.service;

import io.collegeplanner.my.ScheduleOptimizerService.service.RegistrationDataDaoImpl;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;

import static io.collegeplanner.my.ScheduleOptimizerService.Constants.DATABASE_JDBC_CONNECTION_URL;

@Log4j2
@Service
public class MySqlConnectionFactory {

    private static Connection dbConnection;

    public static void createConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            dbConnection = (Connection)DriverManager.getConnection(DATABASE_JDBC_CONNECTION_URL);
            log.info("Connection to database successful.");
        } catch(Exception e) {
            log.fatal("Connection to database failed.", e);
        }
    }

    public static RegistrationDataDaoImpl getRegistrationDataDao() {
        return new RegistrationDataDaoImpl(dbConnection);
    }
}
