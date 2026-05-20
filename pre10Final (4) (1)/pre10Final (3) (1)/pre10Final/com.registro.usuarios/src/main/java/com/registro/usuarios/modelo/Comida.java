package com.registro.usuarios.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "alimento")
public class Comida {
    @Id
    private String id;
    private String nombre;
    private double calorias;
    private double proteinas;
    private double grasas;
    private double carbohidratos;

    public Comida() {
    }

    public Comida(String nombre, double calorias, double proteinas, double grasas, double carbohidratos) {
        this.nombre = nombre;
        this.calorias = calorias;
        this.proteinas = proteinas;
        this.grasas = grasas;
        this.carbohidratos = carbohidratos;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getCalorias() {
        return calorias;
    }

    public double getProteinas() {
        return proteinas;
    }

    public double getGrasas() {
        return grasas;
    }

    public double getCarbohidratos() {
        return carbohidratos;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCalorias(double calorias) {
        this.calorias = calorias;
    }

    public void setProteinas(double proteinas) {
        this.proteinas = proteinas;
    }

    public void setGrasas(double grasas) {
        this.grasas = grasas;
    }

    public void setCarbohidratos(double carbohidratos) {
        this.carbohidratos = carbohidratos;
    }
}