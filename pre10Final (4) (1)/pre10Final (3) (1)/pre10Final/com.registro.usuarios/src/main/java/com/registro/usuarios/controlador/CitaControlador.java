package com.registro.usuarios.controlador;

import com.registro.usuarios.dto.CitaDTO;
import com.registro.usuarios.modelo.Cita;
import com.registro.usuarios.modelo.Usuario;
import com.registro.usuarios.servicio.CitaServicio;
import com.registro.usuarios.servicio.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/citas")
public class CitaControlador {

    @Autowired
    private CitaServicio citaServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    // ========== CITAS DE PACIENTE ==========

    @GetMapping("/apartado")
    public String mostrarFormularioCita(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuario = auth.getName();
        Usuario usuario = usuarioServicio.buscarPorUsername(emailUsuario);

        CitaDTO citaDTO = new CitaDTO();
        // Pre-llenar el email del usuario logueado
        if (usuario != null) {
            citaDTO.setEmail(usuario.getEmail());
        }

        model.addAttribute("citaDTO", citaDTO);
        List<Usuario> doctores = usuarioServicio.listarDoctoresPorRol(2);
        model.addAttribute("doctores", doctores);
        model.addAttribute("usuarioEmail", emailUsuario);
        return "apartarcita";
    }

    @PostMapping("/guardar")
    public String guardarCita(@ModelAttribute("citaDTO") CitaDTO citaDTO, Model model, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuario = auth.getName();

        Usuario usuario = usuarioServicio.buscarPorUsername(emailUsuario);
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "No se pudo identificar al usuario. Inicie sesión nuevamente.");
            return "redirect:/login";
        }

        // Asegurar que el email sea el del usuario logueado
        citaDTO.setEmail(usuario.getEmail());

        try {
            citaServicio.crearCita(citaDTO, usuario.getId());
            redirectAttributes.addFlashAttribute("exito", "¡Cita agendada exitosamente!");
            return "redirect:/citas/mis";
        } catch (IllegalArgumentException e) {
            model.addAttribute("citaDTO", citaDTO);
            List<Usuario> doctores = usuarioServicio.listarDoctoresPorRol(2);
            model.addAttribute("doctores", doctores);
            model.addAttribute("usuarioEmail", emailUsuario);
            model.addAttribute("error", e.getMessage());
            return "apartarcita";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("citaDTO", citaDTO);
            List<Usuario> doctores = usuarioServicio.listarDoctoresPorRol(2);
            model.addAttribute("doctores", doctores);
            model.addAttribute("usuarioEmail", emailUsuario);
            model.addAttribute("error", "Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return "apartarcita";
        }
    }

    @GetMapping("/mis")
    public String listarCitasUsuario(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuario = auth.getName();

        Usuario usuario = usuarioServicio.buscarPorUsername(emailUsuario);
        if (usuario == null) {
            return "redirect:/index";
        }

        List<Cita> citas = citaServicio.obtenerCitasPorUsuario(usuario.getId());
        model.addAttribute("citas", citas);

        return "miscitas";
    }

    // ========== CITAS DE DOCTOR ==========

    @GetMapping("/verCitas")
    public String verCitas(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuario = auth.getName();

        Usuario medico = usuarioServicio.buscarPorUsername(emailUsuario);
        if (medico == null) {
            return "redirect:/login";
        }

        List<Cita> citas = citaServicio.obtenerCitasPorMedico(medico.getId());
        model.addAttribute("citas", citas);

        return "doctorcitas";
    }

    @GetMapping("/todas")
    public String listarTodasLasCitas(Model model) {
        List<Cita> citas = citaServicio.obtenerTodasLasCitas();
        model.addAttribute("citas", citas);
        return "doctorcitas";
    }

    // ========== APARTADO DE CITA POR DOCTOR A PACIENTE ==========

    @GetMapping("/doctor/apartar")
    public String mostrarFormularioCitaDoctor() {
        // Redirigir a la vista de citas del doctor (doctorapartarcita fue eliminado)
        return "redirect:/citas/verCitas";
    }

    @PostMapping("/doctor/guardar")
    public String guardarCitaDoctor(@ModelAttribute("citaDTO") CitaDTO citaDTO,
                                     @RequestParam("pacienteEmail") String pacienteEmail,
                                     Model model, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailDoctor = auth.getName();
        Usuario medico = usuarioServicio.buscarPorUsername(emailDoctor);

        if (medico == null) {
            return "redirect:/login";
        }

        // Buscar paciente por email
        Usuario paciente = usuarioServicio.buscarPorUsername(pacienteEmail.trim());
        if (paciente == null) {
            redirectAttributes.addFlashAttribute("error", "No se encontró ningún paciente con el email: " + pacienteEmail);
            return "redirect:/citas/verCitas";
        }

        // Asegurar que el médico asignado sea el doctor logueado
        citaDTO.setMedico(medico.getId());
        citaDTO.setEmail(paciente.getEmail());

        try {
            citaServicio.crearCita(citaDTO, paciente.getId());
            redirectAttributes.addFlashAttribute("exito", "¡Cita agendada exitosamente para " + paciente.getNombre() + " " + paciente.getApellido() + "!");
            return "redirect:/citas/verCitas";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/citas/verCitas";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return "redirect:/citas/verCitas";
        }
    }

    // API para buscar paciente por email (autocompletado para doctor)
    @GetMapping("/api/buscar-paciente")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarPaciente(@RequestParam("email") String email) {
        Map<String, Object> response = new HashMap<>();
        Usuario paciente = usuarioServicio.buscarPorUsername(email.trim());

        if (paciente != null) {
            response.put("encontrado", true);
            response.put("nombre", paciente.getNombre() + " " + paciente.getApellido());
            response.put("email", paciente.getEmail());
            response.put("celular", paciente.getCelular());
        } else {
            response.put("encontrado", false);
        }

        return ResponseEntity.ok(response);
    }
}
