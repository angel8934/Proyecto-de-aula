package com.registro.usuarios.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioControlador {

    // Muestra la página principal cuando el usuario entra al inicio del sitio
    @GetMapping("/")
    public String mostrarPaginaPrincipal() {
        return "paginaPrincipal"; 
    }
}
