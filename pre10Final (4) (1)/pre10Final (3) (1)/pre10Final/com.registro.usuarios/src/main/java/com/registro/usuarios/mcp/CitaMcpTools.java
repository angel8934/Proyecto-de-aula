package com.registro.usuarios.mcp;

import com.registro.usuarios.modelo.Cita;
import com.registro.usuarios.servicio.CitaServicio;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class CitaMcpTools {

    @Autowired
    private CitaServicio citaServicio;

    @Tool(description = "Obtiene todas las citas médicas registradas en el sistema")
    public List<Cita> listarTodasLasCitas() {
        return citaServicio.obtenerTodasLasCitas();
    }

    @Tool(description = "Obtiene las citas médicas de un paciente o usuario específico dado su ID")
    public List<Cita> obtenerCitasPorUsuario(Long usuarioId) {
        return citaServicio.obtenerCitasPorUsuario(usuarioId);
    }
    
    @Tool(description = "Obtiene las citas médicas asignadas a un médico específico dado su ID")
    public List<Cita> obtenerCitasPorMedico(Long medicoId) {
        return citaServicio.obtenerCitasPorMedico(medicoId);
    }

    @Tool(description = "Crea una nueva cita médica para un paciente usando su email, fecha/hora, lugar y el ID del médico")
    public Cita crearCita(String emailPaciente, LocalDateTime fechaHora, String lugar, Long medicoId) {
        return citaServicio.crearCitaDesdeChat(emailPaciente, fechaHora, lugar, medicoId);
    }

    @Tool(description = "Elimina una cita médica existente dado su ID de cita")
    public boolean eliminarCita(Long citaId) {
        return citaServicio.eliminarCita(citaId);
    }

    @Tool(description = "Actualiza la fecha/hora y/o lugar de una cita existente dado su ID")
    public Cita actualizarCita(Long citaId, LocalDateTime nuevaFechaHora, String nuevoLugar) {
        return citaServicio.actualizarCita(citaId, nuevaFechaHora, nuevoLugar);
    }

    @Tool(description = "Busca una cita médica específica dado su ID")
    public Optional<Cita> buscarCitaPorId(Long citaId) {
        return citaServicio.buscarCitaPorId(citaId);
    }
}
