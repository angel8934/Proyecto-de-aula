package com.registro.usuarios.controlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.registro.usuarios.servicio.UsuarioServicio;

@Controller
@RequestMapping("/login")
public class RegistroControlador {

	// Con el Autowired el codigo busca lo necesario para que funciones esta parte
	@Autowired
	private UsuarioServicio servicio;

	// definir un metodo que responde a una solicitud
	@GetMapping
	public String iniciarSesion() {
		return "login"; // Nombre de la vista de login
	}

	// definir un metodo que responde a una solicitud
	@GetMapping("/success")
	public String loginSuccess() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String role = authentication.getAuthorities().stream().findFirst()
				.map(grantedAuthority -> grantedAuthority.getAuthority()).orElse("");

		switch (role) {
		case "ROLE_PACIENTE":
			return "redirect:/index"; // Redirigir a la página del paciente
		case "ROLE_DOCTOR":
			return "redirect:/doctor"; // Redirigir a la página del doctor
		case "ROLE_ADMINISTRADOR":
			return "redirect:/administrador"; // Redirigir a la página del administrador
		default:
			return "redirect:/login?error"; // Redirigir a login si no hay rol
		}
	}

	// definir un metodo que responde a una solicitud
	@GetMapping("/")
	public String verPaginaDeInicio() {
		return "login"; // Nombre de la vista de la página de inicio
	}
}