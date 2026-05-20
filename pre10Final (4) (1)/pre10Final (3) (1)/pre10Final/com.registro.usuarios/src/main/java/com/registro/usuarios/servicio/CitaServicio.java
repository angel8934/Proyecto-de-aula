package com.registro.usuarios.servicio;

import com.registro.usuarios.dto.CitaDTO;
import com.registro.usuarios.modelo.Cita;
import com.registro.usuarios.modelo.Usuario;
import com.registro.usuarios.repositorio.CitaRepositorio;
import com.registro.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CitaServicio {

    @Autowired
    private CitaRepositorio citaRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    public Cita crearCita(CitaDTO citaDTO, Long usuarioId) {
        // Validar datos de entrada
        if (citaDTO.getFechaHora() == null) {
            throw new IllegalArgumentException("Debe seleccionar una fecha y hora para la cita.");
        }
        if (citaDTO.getMedico() == null) {
            throw new IllegalArgumentException("Debe seleccionar un médico.");
        }
        if (citaDTO.getLugar() == null || citaDTO.getLugar().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un lugar de consulta.");
        }

        Usuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado. Inicie sesión nuevamente."));

        Usuario medico = usuarioRepositorio.findById(citaDTO.getMedico())
                .orElseThrow(() -> new IllegalArgumentException("Médico no encontrado. Seleccione otro médico."));

        LocalDateTime fechaHora = citaDTO.getFechaHora();
        String lugar = citaDTO.getLugar();

        // No se permiten citas en el pasado
        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se pueden agendar citas en el pasado.");
        }

        // Solo entre 6 AM y 10 PM
        int hora = fechaHora.getHour();
        if (hora < 6 || hora >= 22) {
            throw new IllegalArgumentException("Las citas solo se pueden agendar entre las 6:00 AM y las 10:00 PM.");
        }
        
        // Nueva validación de intervalo de 30 minutos entre citas del mismo usuario
        LocalDateTime desde = fechaHora.minusMinutes(30);
        LocalDateTime hasta = fechaHora.plusMinutes(30);

        List<Cita> citasCercanas = citaRepositorio.findByUsuario_IdAndFechaHoraBetween(usuarioId, desde, hasta);
        if (!citasCercanas.isEmpty()) {
            long minutosFaltantes = citasCercanas.stream()
                .mapToLong(citaExistente -> Math.abs(java.time.Duration.between(fechaHora, citaExistente.getFechaHora()).toMinutes()))
                .min()
                .orElse(30);

            throw new IllegalArgumentException("Habrá una cita disponible en " + minutosFaltantes + " minutos. Por favor elija un horario dentro de ese tiempo.");
        }

        // Solo una cita por día
        LocalDateTime inicioDelDia = fechaHora.toLocalDate().atStartOfDay();
        LocalDateTime finDelDia = inicioDelDia.plusDays(1).minusSeconds(1);

        if (citaRepositorio.existsByUsuario_IdAndFechaHoraBetween(usuarioId, inicioDelDia, finDelDia)) {
            throw new IllegalArgumentException("Ya tienes una cita agendada para ese día.");
        }

        // Esperar 7 días desde la última cita
        citaRepositorio.findTopByUsuario_IdOrderByFechaHoraDesc(usuarioId)
            .ifPresent(ultimaCita -> {
                if (fechaHora.isBefore(ultimaCita.getFechaHora().plusDays(7))) {
                    throw new IllegalArgumentException("Debes esperar al menos 7 días desde tu última cita para agendar otra.");
                }
            });

        // Validaciones existentes
        if (citaRepositorio.existsByLugarAndFechaHora(lugar, fechaHora)) {
            throw new IllegalArgumentException("Ya hay una cita agendada en ese lugar y hora.");
        }

        if (citaRepositorio.existsByMedico_IdAndFechaHora(medico.getId(), fechaHora)) {
            throw new IllegalArgumentException("El doctor ya tiene una cita en esa hora.");
        }

        if (citaRepositorio.existsByUsuario_IdAndFechaHora(usuarioId, fechaHora)) {
            throw new IllegalArgumentException("Ya tienes una cita agendada a esa hora.");
        }

        // Guardar cita
        Cita cita = new Cita();
        cita.setFechaHora(fechaHora);
        cita.setLugar(lugar);
        cita.setMedico(medico);
        cita.setEmail(citaDTO.getEmail());
        cita.setUsuario(usuario);

        return citaRepositorio.save(cita);
    }

    /**
     * Crear cita desde el chat MCP del doctor (con menos restricciones).
     * El doctor puede agendar citas para cualquier paciente.
     */
    public Cita crearCitaDesdeChat(String emailPaciente, LocalDateTime fechaHora, String lugar, Long medicoId) {
        Usuario paciente = usuarioRepositorio.findByEmail(emailPaciente);
        if (paciente == null) {
            throw new IllegalArgumentException("No se encontró un paciente con el email: " + emailPaciente);
        }

        Usuario medico = usuarioRepositorio.findById(medicoId)
                .orElseThrow(() -> new IllegalArgumentException("Médico no encontrado."));

        if (fechaHora == null) {
            throw new IllegalArgumentException("Debe indicar una fecha y hora para la cita.");
        }
        if (lugar == null || lugar.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar un lugar para la cita.");
        }
        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se pueden agendar citas en el pasado.");
        }

        int hora = fechaHora.getHour();
        if (hora < 6 || hora >= 22) {
            throw new IllegalArgumentException("Las citas solo se pueden agendar entre las 6:00 AM y las 10:00 PM.");
        }

        if (citaRepositorio.existsByMedico_IdAndFechaHora(medicoId, fechaHora)) {
            throw new IllegalArgumentException("El doctor ya tiene una cita en esa hora.");
        }

        Cita cita = new Cita();
        cita.setFechaHora(fechaHora);
        cita.setLugar(lugar);
        cita.setMedico(medico);
        cita.setEmail(emailPaciente);
        cita.setUsuario(paciente);

        return citaRepositorio.save(cita);
    }

    /**
     * Eliminar una cita por su ID.
     */
    public boolean eliminarCita(Long citaId) {
        Optional<Cita> cita = citaRepositorio.findById(citaId);
        if (cita.isPresent()) {
            citaRepositorio.deleteById(citaId);
            return true;
        }
        return false;
    }

    /**
     * Actualizar fecha/hora y/o lugar de una cita existente.
     */
    public Cita actualizarCita(Long citaId, LocalDateTime nuevaFechaHora, String nuevoLugar) {
        Cita cita = citaRepositorio.findById(citaId)
                .orElseThrow(() -> new IllegalArgumentException("Cita con ID " + citaId + " no encontrada."));

        if (nuevaFechaHora != null) {
            if (nuevaFechaHora.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("No se puede reprogramar a una fecha pasada.");
            }
            int hora = nuevaFechaHora.getHour();
            if (hora < 6 || hora >= 22) {
                throw new IllegalArgumentException("Las citas solo se pueden agendar entre las 6:00 AM y las 10:00 PM.");
            }
            cita.setFechaHora(nuevaFechaHora);
        }

        if (nuevoLugar != null && !nuevoLugar.isEmpty()) {
            cita.setLugar(nuevoLugar);
        }

        return citaRepositorio.save(cita);
    }

    /**
     * Buscar una cita por su ID.
     */
    public Optional<Cita> buscarCitaPorId(Long citaId) {
        return citaRepositorio.findById(citaId);
    }

    public List<Cita> obtenerCitasPorUsuario(Long usuarioId) {
        return citaRepositorio.findByUsuario_Id(usuarioId);
    }
    public List<Cita> obtenerCitasPorMedico(Long medicoId) {
        return citaRepositorio.findByMedicoId(medicoId);
    }
    
    public List<Cita> obtenerTodasLasCitas() {
        return citaRepositorio.findAll();
    }

}
