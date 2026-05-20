package com.registro.usuarios.modelo;

import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = "email"),
		indexes = {
			@Index(name = "idx_usuario_nombre", columnList = "nombre"),
			@Index(name = "idx_usuario_apellido", columnList = "apellido"),
			@Index(name = "idx_usuario_email", columnList = "email")
		})
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "nombre")
	private String nombre;

	@Column(name = "apellido")
	private String apellido;
	
	@Column(name= "edad")
	private int edad;
	
	@Column (name="celular")
	private String celular;

	private String email;

	@JsonIgnore
	private String password;
	
	@ManyToMany(fetch = FetchType.EAGER, cascade= CascadeType.ALL)
	@JoinTable(
			name = "usuarios_roles",
			joinColumns = @JoinColumn(name ="usuarios_id",referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "rol_id", referencedColumnName = "id")
			)
	private Collection<Rol> roles;

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

	public Collection<Rol> getRoles() {
		return roles;
	}

	public void setRoles(Collection<Rol> roles) {
		this.roles = roles;
	}

	
	
	public Usuario(long id, String nombre, String apellido, int edad, String celular, String email, String password,
			Collection<Rol> roles) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.apellido = apellido;
		this.edad = edad;
		this.celular = celular;
		this.email = email;
		this.password = password;
		this.roles = Collections.emptyList(); // Inicializa roles como una lista vacía
	}

	
	public Usuario(String nombre, String apellido, int edad, String celular, String email, String password,
			Collection<Rol> roles) {
		super();
		this.nombre = nombre;
		this.apellido = apellido;
		this.edad = edad;
		this.celular = celular;
		this.email = email;
		this.password = password;
		this.roles = roles;
	}

	

	public Usuario() {
		super();
	}
	
	@Override
    public String toString() {
        return nombre + " " + apellido; // Devuelve el nombre y apellido del usuario
    }

}
