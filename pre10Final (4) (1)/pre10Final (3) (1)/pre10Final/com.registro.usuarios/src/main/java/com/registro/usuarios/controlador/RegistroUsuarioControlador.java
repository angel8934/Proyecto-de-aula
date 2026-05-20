package com.registro.usuarios.controlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.registro.usuarios.dto.UsuarioRegistroDTO;
import com.registro.usuarios.modelo.Rol; // Asegúrate de importar la clase Rol
import com.registro.usuarios.servicio.UsuarioServicio;
import com.registro.usuarios.servicio.UsuarioServicioImpl;

import java.util.Collections; // Para usar Collections.singletonList

@Controller
@RequestMapping("/registro")
public class RegistroUsuarioControlador {

	//Con el Autowired el codigo busca lo necesario para que funciones esta parte
    @Autowired
    private UsuarioServicioImpl usuarioServicioImpl;
    private UsuarioServicio usuarioServicio;

    public RegistroUsuarioControlador(UsuarioServicio usuarioServicio) {
        super();
        this.usuarioServicio = usuarioServicio;
    }
    //facilita el paso de informacion entre la vista y el controlador 
    @ModelAttribute("usuario")
    public UsuarioRegistroDTO retornarNuevoUsuarioRegistroDTO() {
        return new UsuarioRegistroDTO();
    }
    //definir un metodo que responde a una solicitud
    @GetMapping
    public String mostrarFormularioDeRegistro() {
        return "registro";
    }
    
    //se asegura de que el rol del registrado sea 1 
    @PostMapping
    public String registrarCuentaDeUsuario(@ModelAttribute("usuario") UsuarioRegistroDTO registroDTO) {
        // Crear el rol con ID 1
        Rol rolUsuario = new Rol();
        rolUsuario.setId(1); // seguridad para que el rol sea 1 para los registrados

      
        // Guardar el usuario
        usuarioServicio.guardar(registroDTO);
        return "redirect:/registro?exito";
    }
}
