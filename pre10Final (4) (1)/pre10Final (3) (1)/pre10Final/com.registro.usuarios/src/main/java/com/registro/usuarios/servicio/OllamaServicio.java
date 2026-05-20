package com.registro.usuarios.servicio;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class OllamaServicio {

    private final String OLLAMA_URL = "http://localhost:11434/api/generate";

    public String obtenerRespuesta(String mensaje) {

        try {
            RestTemplate restTemplate = new RestTemplate();

            String prompt = """
            Eres un asistente médico.

            Reglas:
            - No des diagnósticos definitivos
            - Responde claro y breve
            - Recomienda acudir a un profesional si es grave

            Paciente:
            """ + mensaje;

            Map<String, Object> body = new HashMap<>();
            body.put("model", "llama3");
            body.put("prompt", prompt);
            body.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(OLLAMA_URL, request, Map.class);

            return response.getBody().get("response").toString();

        } catch (RestClientException e) {
            return "Error con IA local: " + e.getMessage();
        }
    }
}