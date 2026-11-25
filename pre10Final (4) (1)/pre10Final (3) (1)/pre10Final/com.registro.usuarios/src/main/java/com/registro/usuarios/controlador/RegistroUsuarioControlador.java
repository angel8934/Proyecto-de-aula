
package com.registro.usuarios.controlador;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.registro.usuarios.dto.UsuarioRegistroDTO;
import com.registro.usuarios.modelo.Usuario;
import com.registro.usuarios.servicio.UsuarioServicio;
import com.registro.usuarios.servicio.VerificacionEmailServicio;

@Controller
@RequestMapping("/registro")
public class RegistroUsuarioControlador {

    private final UsuarioServicio usuarioServicio;
    private final VerificacionEmailServicio verificacionEmail;

    public RegistroUsuarioControlador(
            UsuarioServicio usuarioServicio,
            VerificacionEmailServicio verificacionEmail) {

        this.usuarioServicio = usuarioServicio;
        this.verificacionEmail = verificacionEmail;
    }

    @ModelAttribute("usuario")
    public UsuarioRegistroDTO nuevoUsuario() {
        return new UsuarioRegistroDTO();
    }

    @GetMapping
    public String mostrarFormularioDeRegistro() {
        return "registro";
    }

    @PostMapping
    public String registrarCuentaDeUsuario(
            @ModelAttribute("usuario") UsuarioRegistroDTO registroDTO,
            RedirectAttributes redirectAttributes) {

        if (!verificacionEmail.estaConfigurado()) {
            redirectAttributes.addFlashAttribute(
                "registroError",
                "El correo de activacion no esta configurado."
            );
            return "redirect:/registro";
        }

        try {
            Usuario usuario = usuarioServicio.guardar(
                registroDTO
            );

            try {
                verificacionEmail.enviarConLimite(usuario);

                redirectAttributes.addFlashAttribute(
                    "registroExitoso",
                    "Cuenta creada. Revisa tu correo para activarla."
                );

            } catch (RestClientException e) {
                redirectAttributes.addFlashAttribute(
                    "registroError",
                    "Tu cuenta quedo pendiente, pero no se pudo "
                    + "enviar el correo. Solicita un nuevo enlace."
                );
            }

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                "registroError",
                e.getMessage()
            );

        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute(
                "registroError",
                "No se pudo completar el registro. "
                + "Comprueba que el correo no exista."
            );
        }

        return "redirect:/registro";
    }

    @PostMapping("/reenviar")
    public String reenviarCorreo(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        try {
            verificacionEmail.reenviar(email);

            redirectAttributes.addFlashAttribute(
                "registroExitoso",
                "Si existe una cuenta pendiente y puede recibir "
                + "otro enlace, revisa ese correo."
            );

        } catch (RestClientException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                "registroError",
                "No fue posible procesar el reenvio. "
                + "Intentalo mas tarde."
            );
        }

        return "redirect:/registro";
    }
}
