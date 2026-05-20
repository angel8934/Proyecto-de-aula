package com.registro.usuarios.controlador;

import com.registro.usuarios.servicio.ModeloPredictivoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/prediccion")
public class PrediccionController {

    @Autowired
    private ModeloPredictivoService modeloPredictivoService;

    @PostMapping("/calcular")
    public Map<String, Object> calcularPrediccion(@RequestBody Map<String, Object> datos) {
        try {
            System.out.println("📩 Datos recibidos: " + datos);

            // --- Traducción de valores al español según el ARFF ---
            String genderWeka = datos.get("gender").toString().equalsIgnoreCase("male") ? "Masculino" : "Femenino";

            String motivation = datos.get("motivation").toString().toLowerCase();
            String motivationWeka = switch (motivation) {
                case "high" -> "Alto";
                case "medium" -> "Medio";
                case "low" -> "Bajo";
                default -> "Medio";
            };

            String supportSystem = datos.get("supportSystem").toString().toLowerCase();
            String supportSystemWeka = switch (supportSystem) {
                case "strong" -> "Fuerte";
                case "moderate" -> "Moderado";
                case "weak" -> "Debil";
                default -> "Moderado";
            };

            String activityLevel = datos.get("activityLevel").toString().toLowerCase();
            String activityLevelWeka = switch (activityLevel) {
                case "high" -> "Alto";
                case "medium" -> "Moderado";
                case "low" -> "Bajo";
                default -> "Moderado";
            };

            String dietComplexity = datos.get("dietComplexity").toString().toLowerCase();
            String dietComplexityWeka = switch (dietComplexity) {
                case "simple" -> "Simple";
                case "medium" -> "Moderada";
                case "complex" -> "Compleja";
                default -> "Simple";
            };

            String stressLevel = datos.get("stressLevel").toString().toLowerCase();
            String stressLevelWeka = switch (stressLevel) {
                case "low" -> "Bajo";
                case "medium" -> "Moderado";
                case "high" -> "Alto";
                default -> "Moderado";
            };

            String foodPreferences = datos.get("foodPreferences").toString().toLowerCase();
            String foodPreferencesWeka = switch (foodPreferences) {
                case "high" -> "Alta";
                case "medium" -> "Moderada";
                case "low" -> "Baja";
                default -> "Moderada";
            };

            // --- Llamada al servicio ---
            Map<String, Object> resultadoModelo = modeloPredictivoService.predecirConPorcentaje(
                    Integer.parseInt(datos.get("age").toString()),
                    genderWeka,
                    motivationWeka,
                    Integer.parseInt(datos.get("previousDiets").toString()),
                    supportSystemWeka,
                    activityLevelWeka,
                    dietComplexityWeka,
                    stressLevelWeka,
                    foodPreferencesWeka
            );

            String clase = (String) resultadoModelo.get("clase");
            double porcentaje = (double) resultadoModelo.get("porcentaje");

            // --- Descripción basada en la clase ---
            String descripcion = switch (clase) {
                case "Alta" -> "Alta probabilidad de adherencia al plan nutricional.";
                case "Media" -> "Probabilidad moderada de adherencia al plan nutricional.";
                default -> "Baja probabilidad de adherencia al plan nutricional.";
            };

            return Map.of(
                    "nivel", clase,
                    "porcentaje", porcentaje,
                    "descripcion", descripcion
            );
            

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error al realizar la predicción: " + e.getMessage());
        }
    }
}