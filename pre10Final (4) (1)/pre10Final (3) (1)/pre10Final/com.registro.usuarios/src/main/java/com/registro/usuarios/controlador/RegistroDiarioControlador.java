package com.registro.usuarios.controlador;

import com.registro.usuarios.dto.RegistroDiarioDTO;
import com.registro.usuarios.modelo.RegistroDiario;
import com.registro.usuarios.servicio.RegistroDiarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registro")
@CrossOrigin(origins = "*") // Asegúrate de permitir acceso desde frontend
public class RegistroDiarioControlador {

    @Autowired
    private RegistroDiarioServicio servicio;

    @PostMapping("/guardar")
    public RegistroDiario guardar(@RequestBody RegistroDiarioDTO dto) {
        return servicio.guardarRegistro(dto);
    }
}
