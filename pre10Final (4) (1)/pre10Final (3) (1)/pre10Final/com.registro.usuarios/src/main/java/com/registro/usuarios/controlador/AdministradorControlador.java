package com.registro.usuarios.controlador;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.registro.usuarios.modelo.Usuario;
import com.registro.usuarios.servicio.UsuarioServicio;

@Controller
public class AdministradorControlador {
	   
	
	@Autowired
	private UsuarioServicio usuarioServicio;
	
	 @GetMapping("/administrador")
	    public String paginaAdministrador(Model model) {
	        // Doctores y admins son listas pequeñas, se cargan normalmente
	        List<Usuario> doctores = usuarioServicio.listarDoctoresPorRol(2L);
	        List<Usuario> administradores = usuarioServicio.listarAdministradoresPorRol(3L);

	        model.addAttribute("doctores", doctores);
	        model.addAttribute("administradores", administradores);
	        // Los pacientes (40k+) se cargan vía AJAX con paginación en el frontend
	        model.addAttribute("totalPacientes", usuarioServicio.contarUsuariosPorRol(1L));

	        return "administrador";
	    }
}
