package com.dimvxs.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.awt.Desktop;
import java.net.URI;
import java.util.*;

@SpringBootApplication
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}

@Component
class BrowserLauncher {

    // После запуска приложения автоматически открывает браузер
    @EventListener(ApplicationReadyEvent.class)
    public void launchBrowser() {
        System.setProperty("java.awt.headless", "false");
        try {
            Desktop.getDesktop().browse(new URI("http://localhost:8080"));
        } catch (Exception ignored) {}
    }
}

@Service
class OpenAiService {

    // Ключ взят с https://openrouter.ai/settings/keys
    // Если возникает ошибка AI: 401 Unauthorized on POST request,
    // необходимо перегенерировать ключ
    @Value("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")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // Полезные ссылки:
    // https://x.ai/api
    // https://platform.openai.com/api-keys
    // https://developer.puter.com/tutorials/free-unlimited-openai-api/ — БЕСПЛАТНО

    // private static final String API_URL = "https://api.x.ai/v1/chat/completions"; // Grok API
    // private static final String API_URL = "https://api.openai.com/v1/chat/completions"; // ChatGPT
      private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    public String getJoke(String topic) {
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", "http://localhost:8080");
            headers.set("X-Title", "Генератор шуток");

            Map<String, Object> message = Map.of(
                "role", "user",
                "content", "Расскажи короткую смешную шутку на русском языке на тему: " + topic
            );

            Map<String, Object> requestBody = Map.of(
                // Лимиты: 50 запросов в день, 20 в минуту
                //"model", "x-ai/grok-4.1-fast:free",
                  "model", "openai/gpt-4o-mini",
                // "model", "mistralai/mistral-7b-instruct:free",
                "messages", List.of(message),
                "temperature", 0.9, // Чем выше значение, тем больше «бреда»
                "max_tokens", 300 // (1 токен ≈ 3–4 символа или 3/4 слова)
            );

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response =
                    restTemplate.postForObject(API_URL, request, Map.class);

            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices =
                        (List<Map<String, Object>>) response.get("choices");

                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> msg =
                            (Map<String, Object>) choices.get(0).get("message");

                    return (String) msg.get("content");
                }
            }
            return "Не удалось получить шутку";
        } catch (Exception e) {
            return "Ошибка AI: " + e.getMessage();
        }
    }
}

@Controller
class JokeController {

    private final OpenAiService openAiService;

    public JokeController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/joke")
    public String getJoke(@RequestParam("topic") String topic, Model model) {

        if (topic == null || topic.trim().isEmpty()) {
            model.addAttribute("error", "Пожалуйста, введите тему!");
            return "index";
        }

        String joke = openAiService.getJoke(topic);
        model.addAttribute("topic", topic);
        model.addAttribute("joke", joke);

        return "index";
    }
}
