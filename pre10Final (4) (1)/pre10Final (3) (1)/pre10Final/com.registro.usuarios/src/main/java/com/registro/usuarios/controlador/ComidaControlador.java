package com.registro.usuarios.controlador;

import com.registro.usuarios.modelo.Comida;
import com.registro.usuarios.servicio.ComidaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/comidas")
@CrossOrigin(origins = "*")  // Para permitir llamadas desde cualquier origen, ajustar en producción
public class ComidaControlador {
	
	//Con el Autowired el codigo busca lo necesario para que funciones esta parte ya que alli esta este metodo 
    @Autowired
    private ComidaServicio comidaServicio;

    //definir un metodo que responde a una solicitud de ver los valores nutricionales 
    @GetMapping
    public List<Comida> listarComidas() {
        return comidaServicio.obtenerTodasLasComidas();
    }
}
