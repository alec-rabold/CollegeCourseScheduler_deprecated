package io.collegeplanner.my.ScheduleOptimizerService.configuration;

import io.collegeplanner.my.ScheduleOptimizerService.service.MySqlConnectionFactory;
import io.collegeplanner.my.ScheduleOptimizerService.service.RegistrationDataDao;
import org.jdbi.v3.core.Jdbi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.collegeplanner.my.ScheduleOptimizerService.Constants.DATABASE_JDBC_CONNECTION_URL;

@Configuration
public class BeanConfig {

    /** JDBI Beans */
    @Bean
    public Jdbi createJdbi() {
        return Jdbi.create(DATABASE_JDBC_CONNECTION_URL);
    }
    @Bean(name = "registrationData")
    public RegistrationDataDao registrationDataDao(Jdbi jdbi) {
        return jdbi.onDemand(RegistrationDataDao.class);
    }


    @Bean
    public UserDao userDao(Jdbi jdbi) {
        return jdbi.onDemand(UserDao.class);
    }
}
