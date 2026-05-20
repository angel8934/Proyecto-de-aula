package com.registro.usuarios.servicio;

import com.registro.usuarios.modelo.Comida;
import com.registro.usuarios.repositorio.ComidaRepositorio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ComidaServicio {

	@Autowired
	private ComidaRepositorio comidaRepositorio;

	public List<Comida> obtenerTodasLasComidas() {
		return comidaRepositorio.findAll();
	}
}
