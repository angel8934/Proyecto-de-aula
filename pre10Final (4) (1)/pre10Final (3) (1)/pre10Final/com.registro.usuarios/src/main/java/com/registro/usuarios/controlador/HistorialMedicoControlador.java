package com.registro.usuarios.controlador;

import com.registro.usuarios.modelo.HistorialMedico;
import com.registro.usuarios.modelo.Usuario;
import com.registro.usuarios.servicio.HistorialMedicoServicio;
import com.registro.usuarios.servicio.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@Controller

public class HistorialMedicoControlador {

    @Autowired
    private HistorialMedicoServicio historialMedicoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    /**
     * Muestra formulario de historial.
     * Ya no carga 40k+ usuarios para el dropdown.
     * El dropdown se reemplaza por un campo de búsqueda AJAX.
     */
    @GetMapping("/historial")
    public String mostrarFormularioHistorial(Model model) {
        // Ya no se carga listarUsuarios() - se usa AJAX con búsqueda
        model.addAttribute("historialMedico", new HistorialMedico());
        return "historial";
    }


    @PostMapping("/historial/guardar")
    public String guardarHistorial(@ModelAttribute HistorialMedico historialMedico) {
        historialMedicoServicio.guardarHistorial(historialMedico);
        return "redirect:/historial";
    }
    
 
        /**
         * Muestra formulario para seleccionar usuario.
         * Ya no carga 40k+ usuarios para el dropdown.
         */
        @GetMapping("/consulta")
        public String mostrarFormularioConsulta(Model model) {
            // Ya no se carga listarUsuarios() - se usa AJAX con búsqueda
            return "buscarHistorial";
        }

        // Procesar formulario para mostrar historiales de usuario seleccionado
        @PostMapping("/consulta")
        public String procesarConsulta(@RequestParam("usuarioId") Long usuarioId, Model model) {
            List<HistorialMedico> historiales = historialMedicoServicio.buscarPorUsuarioId(usuarioId);
            model.addAttribute("historiales", historiales);

            // Pasar el ID del usuario seleccionado para que el frontend lo muestre
            model.addAttribute("usuarioSeleccionado", usuarioId);

            return "buscarHistorial";
        }
    

}
