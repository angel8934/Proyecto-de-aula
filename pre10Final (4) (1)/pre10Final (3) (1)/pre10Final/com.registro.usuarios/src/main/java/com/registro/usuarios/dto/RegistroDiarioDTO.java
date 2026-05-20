package com.registro.usuarios.dto;

import java.util.List;

public class RegistroDiarioDTO {
    private String fecha;
    private double calorias;
    private double proteinas;
    private double grasas;
    private double carbohidratos;
    private List<String> alimentos;

    

    public String getFecha() {
		return fecha;
	}



	public void setFecha(String fecha) {
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



	public RegistroDiarioDTO() {}
}
