
package com.registro.usuarios.controlador;

import com.registro.usuarios.servicio.PythonPredictivoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RestController
@RequestMapping("/api/prediccion")
public class PrediccionController {

    private final PythonPredictivoService predictor;

    public PrediccionController(
            PythonPredictivoService predictor) {
        this.predictor = predictor;
    }

    @PostMapping("/calcular")
    public ResponseEntity<Map<String, Object>> calcularPrediccion(
            @RequestBody Map<String, Object> datos) {

        try {
            Map<String, Object> resultado =
                    predictor.predecir(datos);

            return ResponseEntity.ok(resultado);

        } catch (RestClientException e) {

            return ResponseEntity.status(
                    HttpStatus.SERVICE_UNAVAILABLE
            ).body(Map.of(
                "error",
                "El servicio predictivo no está disponible."
            ));

        } catch (IllegalStateException e) {

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(Map.of(
                "error",
                "No se pudo procesar la predicción."
            ));
        }
    }
}
