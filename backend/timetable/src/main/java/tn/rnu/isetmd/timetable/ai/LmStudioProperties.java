package tn.rnu.isetmd.timetable.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.lm-studio")
public record LmStudioProperties(
        String baseUrl,
        String model,
        double temperature
) {
}