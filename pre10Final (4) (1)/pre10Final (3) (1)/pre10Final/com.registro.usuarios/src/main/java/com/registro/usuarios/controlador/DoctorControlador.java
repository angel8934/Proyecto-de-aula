package com.registro.usuarios.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DoctorControlador {

    /**
     * Ya no carga los 40k+ pacientes en el servidor.
     * La tabla de pacientes se carga vía AJAX desde /api/usuarios/rol/1 con paginación.
     */
    @GetMapping("/doctor")
    public String paginaDoctor(Model model) {
        // Los usuarios se cargan por AJAX con paginación en el frontend
        return "doctor";
    }
}