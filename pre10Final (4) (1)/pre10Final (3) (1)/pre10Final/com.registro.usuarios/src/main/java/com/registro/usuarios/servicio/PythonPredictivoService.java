
package com.registro.usuarios.servicio;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PythonPredictivoService {

    private final RestClient cliente;

    public PythonPredictivoService(
            @Value("${predictor.api.url:http://localhost:8000}")
            String url) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));

        this.cliente = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    public Map<String, Object> predecir(
            Map<String, Object> datosPaciente) {

        Map<String, Object> respuesta = cliente.post()
                .uri("/predecir")
                .contentType(MediaType.APPLICATION_JSON)
                .body(datosPaciente)
                .retrieve()
                .body(
                    new ParameterizedTypeReference<
                        Map<String, Object>
                    >() {}
                );

        if (respuesta == null) {
            throw new IllegalStateException(
                "FastAPI devolvió una respuesta vacía."
            );
        }

        return respuesta;
    }
}
