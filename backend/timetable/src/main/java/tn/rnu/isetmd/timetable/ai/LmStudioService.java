package tn.rnu.isetmd.timetable.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tn.rnu.isetmd.timetable.ai.dto.AiTimetableEntry;
import tn.rnu.isetmd.timetable.ai.dto.AiTimetableResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LmStudioService implements AiService {

    private final LmStudioProperties properties;
    private final ObjectMapper objectMapper;

    private final RestClient restClient = RestClient.create();

    @Override
    public AiTimetableResponse extractTimetable(byte[] image) {

        try {
            String prompt = loadPrompt();

            String base64Image = Base64.getEncoder()
                    .encodeToString(image);

            Map<String, Object> request = Map.of(
                    "model", properties.model(),
                    "temperature", properties.temperature(),
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of(
                                                    "type", "text",
                                                    "text", prompt
                                            ),
                                            Map.of(
                                                    "type", "image_url",
                                                    "image_url", Map.of(
                                                            "url",
                                                            "data:image/jpeg;base64," + base64Image
                                                    )
                                            )
                                    )
                            )
                    )
            );

            Map<?, ?> response = restClient
                    .post()
                    .uri(properties.baseUrl() + "/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            String content = extractContent(response);

            return parseTimetableResponse(content);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to extract timetable using LM Studio",
                    e
            );
        }
    }

    private String loadPrompt() throws IOException {

        ClassPathResource resource =
                new ClassPathResource("prompts/timetable-extraction.txt");

        return new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    private String extractContent(Map<?, ?> response) {

        Object choicesObject = response.get("choices");

        if (!(choicesObject instanceof List<?> choices)
                || choices.isEmpty()) {

            throw new RuntimeException(
                    "LM Studio returned no choices"
            );
        }

        Object firstChoice = choices.get(0);

        if (!(firstChoice instanceof Map<?, ?> choice)) {
            throw new RuntimeException(
                    "Invalid LM Studio response"
            );
        }

        Object messageObject = choice.get("message");

        if (!(messageObject instanceof Map<?, ?> message)) {
            throw new RuntimeException(
                    "LM Studio response contains no message"
            );
        }

        Object content = message.get("content");

        if (!(content instanceof String text)) {
            throw new RuntimeException(
                    "LM Studio response contains no text content"
            );
        }

        return text;
    }

    private String cleanJson(String content) {

        String cleaned = content.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(
                    0,
                    cleaned.length() - 3
            );
        }

        return cleaned.trim();
    }

    private AiTimetableResponse parseTimetableResponse(
            String content
    ) throws Exception {

        String json = cleanJson(content);

        if (json.trim().startsWith("[")) {

            List<AiTimetableEntry> entries =
                    objectMapper.readValue(
                            json,
                            objectMapper.getTypeFactory()
                                    .constructCollectionType(
                                            List.class,
                                            AiTimetableEntry.class
                                    )
                    );

            return new AiTimetableResponse(
                    entries,
                    List.of()
            );
        }

        return objectMapper.readValue(
                json,
                AiTimetableResponse.class
        );
    }
}