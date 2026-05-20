package com.registro.usuarios.controlador;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.registro.usuarios.modelo.Usuario;
import com.registro.usuarios.servicio.DoctorChatServicio;
import com.registro.usuarios.servicio.OllamaServicio;
import com.registro.usuarios.servicio.OpenAIService;
import com.registro.usuarios.servicio.UsuarioServicio;

@RestController
@RequestMapping("/chat")
public class ChatControlador {

    private final OllamaServicio ollamaServicio;
    private final OpenAIService openAIService;
    private final DoctorChatServicio doctorChatServicio;
    private final UsuarioServicio usuarioServicio;

    // Constructor con todas las IAs
    public ChatControlador(OllamaServicio ollamaServicio, OpenAIService openAIService,
                           DoctorChatServicio doctorChatServicio, UsuarioServicio usuarioServicio) {
        this.ollamaServicio = ollamaServicio;
        this.openAIService = openAIService;
        this.doctorChatServicio = doctorChatServicio;
        this.usuarioServicio = usuarioServicio;
    }

    //  Ollama
    @PostMapping("/ollama")
    public String chatOllama(@RequestBody String mensaje) {
        return ollamaServicio.obtenerRespuesta(mensaje);
    }

    //  OpenAI 
    @PostMapping("/openai")
    public String chatOpenAI(@RequestBody String mensaje) {
        return openAIService.obtenerRespuesta(mensaje);
    }

    // Chat MCP exclusivo para doctores
    @PostMapping("/doctor")
    public String chatDoctor(@RequestBody String mensaje) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailDoctor = auth.getName();
        Usuario medico = usuarioServicio.buscarPorUsername(emailDoctor);

        if (medico == null) {
            return "Error: no se pudo identificar al médico autenticado.";
        }

        return doctorChatServicio.procesarMensajeDoctor(mensaje, medico.getId());
    }

    // Multimodal
    @PostMapping("/{modelo}")
    public String elegirIA(@PathVariable String modelo, @RequestBody String mensaje) {

        if (modelo.equalsIgnoreCase("openai")) {
            return openAIService.obtenerRespuesta(mensaje);
        } else if (modelo.equalsIgnoreCase("ollama")) {
            return ollamaServicio.obtenerRespuesta(mensaje);
        } else {
            return "Modelo no válido";
        }
    }
}