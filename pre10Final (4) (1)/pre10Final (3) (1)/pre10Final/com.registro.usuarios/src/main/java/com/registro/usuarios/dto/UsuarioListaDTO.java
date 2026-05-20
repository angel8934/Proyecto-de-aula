package com.registro.usuarios.dto;

/**
 * DTO para listar usuarios sin exponer datos sensibles como el password.
 * Optimiza el tamaño del JSON enviado al frontend.
 */
public class UsuarioListaDTO {

    private long id;
    private String nombre;
    private String apellido;
    private int edad;
    private String celular;
    private String email;

    public UsuarioListaDTO() {
    }

    public UsuarioListaDTO(long id, String nombre, String apellido, int edad, String celular, String email) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.edad = edad;
        this.celular = celular;
        this.email = email;
    }

    // Getters y Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
