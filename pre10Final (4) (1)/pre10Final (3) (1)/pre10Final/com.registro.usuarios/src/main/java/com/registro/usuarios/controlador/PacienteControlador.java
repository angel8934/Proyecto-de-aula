package com.registro.usuarios.controlador;

import com.registro.usuarios.modelo.ImcRegistro;
import com.registro.usuarios.modelo.Usuario;
import com.registro.usuarios.repositorio.ImcRegistroRepositorio;
import com.registro.usuarios.servicio.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Controller
public class PacienteControlador {

	@Autowired
	private UsuarioServicio usuarioServicio;

	@Autowired
	private ImcRegistroRepositorio imcRegistroRepositorio;

	//definir un metodo que responde a una solicitud de enviar la usuarios a su apartado 
	@GetMapping("/index")
    public String paginaPaciente(Model model, Authentication authentication) {
		// Siempre inicializar con lista vacía para evitar errores en Thymeleaf
		model.addAttribute("historialImc", Collections.emptyList());

		// Cargar el historial de IMC del usuario autenticado
		if (authentication != null) {
			Usuario usuario = usuarioServicio.buscarPorUsername(authentication.getName());
			if (usuario != null) {
				List<ImcRegistro> historial = imcRegistroRepositorio
						.findByUsuarioIdOrderByFechaRegistroDesc(usuario.getId());
				model.addAttribute("historialImc", historial);
			}
		}
        return "index"; 
    }

	@PostMapping("/paciente/guardar-imc")
	public String guardarImc(@RequestParam("imc") double imc,
							 @RequestParam("categoria") String categoria,
							 Authentication authentication) {
		if (authentication != null) {
			Usuario usuario = usuarioServicio.buscarPorUsername(authentication.getName());
			if (usuario != null) {
				ImcRegistro registro = new ImcRegistro(imc, categoria, LocalDateTime.now(), usuario);
				imcRegistroRepositorio.save(registro);
			}
		}
		return "redirect:/index";
	}
}
