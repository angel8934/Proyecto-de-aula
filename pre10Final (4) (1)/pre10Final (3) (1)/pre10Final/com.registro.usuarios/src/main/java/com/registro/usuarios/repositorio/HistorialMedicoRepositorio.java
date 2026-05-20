package com.registro.usuarios.repositorio;

import com.registro.usuarios.modelo.HistorialMedico;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialMedicoRepositorio extends JpaRepository<HistorialMedico, Long> {
	
	 List<HistorialMedico> findByUsuario_Id(Long usuarioId);
}
