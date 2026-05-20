package com.registro.usuarios.dto;

import java.time.LocalDateTime;

public class CitaDTO {
    private Long id;
    private LocalDateTime fechaHora;
    private String lugar;
    private Long medico; 
    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public Long getMedico() {
        return medico;
    }

    public void setMedico(Long medico) {
        this.medico = medico;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
