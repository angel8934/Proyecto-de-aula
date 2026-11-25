
package com.registro.usuarios.controlador;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccesoBiometricoController {

    @GetMapping("/inicio-passkey")
    public String inicio(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean admin = authentication.getAuthorities()
            .stream()
            .anyMatch(a -> a.getAuthority()
                .equals("ROLE_ADMINISTRADOR"));

        boolean doctor = authentication.getAuthorities()
            .stream()
            .anyMatch(a -> a.getAuthority()
                .equals("ROLE_DOCTOR"));

        boolean paciente = authentication.getAuthorities()
            .stream()
            .anyMatch(a -> a.getAuthority()
                .equals("ROLE_PACIENTE"));

        if (admin) {
            return "redirect:/administrador";
        }

        if (doctor) {
            return "redirect:/doctor";
        }

        if (paciente) {
            return "redirect:/index";
        }

        return "redirect:/paginaPrincipal";
    }
}
