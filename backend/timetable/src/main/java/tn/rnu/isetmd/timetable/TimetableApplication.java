package tn.rnu.isetmd.timetable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import tn.rnu.isetmd.timetable.ai.LmStudioProperties;

@SpringBootApplication
@EnableConfigurationProperties(LmStudioProperties.class)
public class TimetableApplication {
    public static void main(String[] args) {
        SpringApplication.run(
                TimetableApplication.class,
                args
        );
    }
}
