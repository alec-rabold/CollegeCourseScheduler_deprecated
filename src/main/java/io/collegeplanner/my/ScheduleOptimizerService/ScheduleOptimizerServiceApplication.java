package io.collegeplanner.my.ScheduleOptimizerService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ScheduleOptimizerServiceApplication {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(ScheduleOptimizerServiceApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(ScheduleOptimizerServiceApplication.class, args);
	}
}
