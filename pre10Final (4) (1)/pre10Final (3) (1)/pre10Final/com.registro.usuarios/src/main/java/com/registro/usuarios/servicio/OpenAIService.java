package com.registro.usuarios.servicio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    public String obtenerRespuesta(String mensaje) {

        String url = "https://api.openai.com/v1/chat/completions";

        RestTemplate restTemplate = new RestTemplate();

        // Body JSON
        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4.1-mini");

        List<Map<String, String>> messages = new ArrayList<>();

// 🧠 CONTEXTO (como el prompt de Ollama)
messages.add(Map.of(
        "role", "system",
        "content", """
Eres un asistente médico.

Reglas:
- No des diagnósticos definitivos
- Responde claro y breve
- Recomienda acudir a un profesional si es grave
"""
));

// 👤 MENSAJE DEL USUARIO
messages.add(Map.of(
        "role", "user",
        "content", mensaje
));

        body.put("messages", messages);

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.getBody().get("choices");

            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");

            return message.get("content").toString();

        } catch (RestClientException e) {
            return "Error con OpenAI: " + e.getMessage();
        }
    }
}