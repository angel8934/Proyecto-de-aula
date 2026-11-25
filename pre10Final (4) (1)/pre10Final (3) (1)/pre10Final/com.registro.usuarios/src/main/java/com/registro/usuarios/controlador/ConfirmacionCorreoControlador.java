
package com.registro.usuarios.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.registro.usuarios.servicio.VerificacionEmailServicio;

@Controller
public class ConfirmacionCorreoControlador {

    private final VerificacionEmailServicio servicio;

    public ConfirmacionCorreoControlador(
            VerificacionEmailServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/verificar-cuenta")
    public String verificarCuenta(
            @RequestParam(required = false) String token,
            RedirectAttributes redirectAttributes) {

        boolean confirmado = servicio.confirmar(token);

        if (confirmado) {
            redirectAttributes.addFlashAttribute(
                "registroExitoso",
                "Tu cuenta fue activada correctamente. "
                + "Ya puedes iniciar sesion."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                "registroError",
                "El enlace es invalido, ya fue utilizado "
                + "o ha vencido."
            );
        }

        return "redirect:/registro";
    }
}
