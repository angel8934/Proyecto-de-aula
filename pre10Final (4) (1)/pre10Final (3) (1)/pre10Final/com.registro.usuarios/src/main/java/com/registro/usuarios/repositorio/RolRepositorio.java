package com.registro.usuarios.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.registro.usuarios.modelo.Rol;

@Repository
public interface RolRepositorio extends JpaRepository<Rol, Long> {
    
	
	public Rol findByNombre(String nombre);
}