package com.registro.usuarios.dto;

import java.util.Collection;

import com.registro.usuarios.modelo.Rol; // Asegúra de importar la clase Rol

public class UsuarioRegistroDTO {

    private long id;
    private String nombre;
    private String apellido;
    private int edad;
    private String celular;
    private String email;
    private String password;
    

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

 

    public UsuarioRegistroDTO(long id, String nombre, String apellido, int edad, String celular, String email,
			String password) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.apellido = apellido;
		this.edad = edad;
		this.celular = celular;
		this.email = email;
		this.password = password;
	}

	

    public UsuarioRegistroDTO(String nombre, String apellido, int edad, String celular, String email, String password) {
		super();
		this.nombre = nombre;
		this.apellido = apellido;
		this.edad = edad;
		this.celular = celular;
		this.email = email;
		this.password = password;
	}

	

    public UsuarioRegistroDTO(String email) {
        super();
        this.email = email;
    }

    public UsuarioRegistroDTO() {
        super();
    }
}