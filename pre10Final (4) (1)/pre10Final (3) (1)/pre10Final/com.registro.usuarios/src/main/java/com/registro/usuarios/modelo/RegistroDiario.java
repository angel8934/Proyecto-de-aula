package com.registro.usuarios.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "registros")
public class RegistroDiario {

    @Id
    private String id;
    private Date fecha;
    private double calorias;
    private double proteinas;
    private double grasas;
    private double carbohidratos;
    private List<String> alimentos; // Nombres de alimentos consumidos

   

    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	

	public Date getFecha() {
		return fecha;
	}

	public void setFecha(Date fecha) {
		this.fecha = fecha;
	}

	public double getCalorias() {
		return calorias;
	}

	public void setCalorias(double calorias) {
		this.calorias = calorias;
	}

	public double getProteinas() {
		return proteinas;
	}

	public void setProteinas(double proteinas) {
		this.proteinas = proteinas;
	}

	public double getGrasas() {
		return grasas;
	}

	public void setGrasas(double grasas) {
		this.grasas = grasas;
	}

	public double getCarbohidratos() {
		return carbohidratos;
	}

	public void setCarbohidratos(double carbohidratos) {
		this.carbohidratos = carbohidratos;
	}

	public List<String> getAlimentos() {
		return alimentos;
	}

	public void setAlimentos(List<String> alimentos) {
		this.alimentos = alimentos;
	}

	// Constructor vacío
    public RegistroDiario() {}

    // Constructor con campos
    public RegistroDiario(Date fecha, double calorias, double proteinas, double grasas, double carbohidratos, List<String> alimentos) {
        this.fecha = fecha;
        this.calorias = calorias;
        this.proteinas = proteinas;
        this.grasas = grasas;
        this.carbohidratos = carbohidratos;
        this.alimentos = alimentos;
    }

    
}
