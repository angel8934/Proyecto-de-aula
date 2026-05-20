package com.registro.usuarios.servicio;

import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.*;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class ModeloPredictivoService {

    private Classifier modelo;
    private Instances estructura;

    public ModeloPredictivoService() {
        try {
            // Cargar el modelo WEKA
            InputStream inputStream = getClass().getResourceAsStream("/modelos/modeloPlanNutricional.model");
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            modelo = (Classifier) ois.readObject();
            ois.close();

            // Cargar estructura ARFF
            InputStream arffStream = getClass().getResourceAsStream("/modelos/plan_nutricional_360_balanceado.arff");
            estructura = new DataSource(arffStream).getDataSet();
            estructura.setClassIndex(estructura.numAttributes() - 1);

            System.out.println("✅ Modelo y estructura cargados correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error cargando el modelo o estructura: " + e.getMessage());
        }
    }

    /**
     * Predice la clase usando WEKA y calcula un porcentaje intuitivo basado en los atributos.
     */
    public Map<String, Object> predecirConPorcentaje(
            int edad,
            String genero,
            String motivacion,
            int intentosPrevios,
            String sistemaApoyo,
            String actividad,
            String complejidad,
            String estres,
            String afinidad
    ) {
        Map<String, Object> resultadoMap = new HashMap<>();
        try {
            // --- Crear instancia WEKA ---
            DenseInstance instancia = new DenseInstance(estructura.numAttributes());
            instancia.setDataset(estructura);

            instancia.setValue(estructura.attribute("edad"), edad);
            instancia.setValue(estructura.attribute("genero"), genero);
            instancia.setValue(estructura.attribute("motivacion_inicial"), motivacion);
            instancia.setValue(estructura.attribute("intentos_previos"), intentosPrevios);
            instancia.setValue(estructura.attribute("sistema_apoyo"), sistemaApoyo);
            instancia.setValue(estructura.attribute("actividad_fisica"), actividad);
            instancia.setValue(estructura.attribute("complejidad_plan"), complejidad);
            instancia.setValue(estructura.attribute("estres"), estres);
            instancia.setValue(estructura.attribute("afinidad_plan"), afinidad);

            // --- Predicción de clase con WEKA ---
            double resultadoIndex = modelo.classifyInstance(instancia);
            String clase = estructura.classAttribute().value((int) resultadoIndex);

            // --- Cálculo de porcentaje intuitivo según los atributos ---
            double porcentaje = calcularPorcentajeAdherencia(motivacion, sistemaApoyo, actividad, complejidad, afinidad);

            System.out.println("🔮 Predicción del modelo: " + clase + " | Porcentaje calculado: " + porcentaje + "%");

            resultadoMap.put("clase", clase);
            resultadoMap.put("porcentaje", porcentaje);

        } catch (Exception e) {
            e.printStackTrace();
            resultadoMap.put("clase", "Error");
            resultadoMap.put("porcentaje", 0.0);
        }

        return resultadoMap;
    }

    /**
     * Calcula un porcentaje intuitivo de adherencia basado en los atributos.
     */
    private double calcularPorcentajeAdherencia(String motivacion, String sistemaApoyo, String actividad, String complejidad, String afinidad) {
    double score = 0;

    // Motivación (peso alto)
    switch (motivacion) {
        case "Alto": score += 30; break;
        case "Medio": score += 10; break;
        case "Bajo": score += 0; break;
    }

    // Sistema de apoyo (peso alto)
    switch (sistemaApoyo) {
        case "Fuerte": score += 30; break;
        case "Moderado": score += 10; break;
        case "Debil": score += 0; break;
    }

    // Actividad física (peso medio)
    switch (actividad) {
        case "Alto": score += 15; break;
        case "Moderado": score += 7; break;
        case "Bajo": score += 0; break;
    }

    // Complejidad del plan (peso bajo)
    switch (complejidad) {
        case "Simple": score += 15; break;
        case "Moderada": score += 7; break;
        case "Compleja": score += 0; break;
    }

    // Afinidad con el plan (peso alto)
    switch (afinidad) {
        case "Alta": score += 30; break;
        case "Moderada": score += 10; break;
        case "Baja": score += 0; break;
    }

    // Limitar a 100%
    return Math.min(score, 100);
}

}