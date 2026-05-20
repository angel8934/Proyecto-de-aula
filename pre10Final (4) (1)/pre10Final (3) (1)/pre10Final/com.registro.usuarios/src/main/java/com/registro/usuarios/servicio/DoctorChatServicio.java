package com.registro.usuarios.servicio;

import com.registro.usuarios.modelo.Cita;
import com.registro.usuarios.modelo.Usuario;
import com.registro.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio de chat IA exclusivo para doctores.
 * Actúa como un agente que decide qué herramienta MCP usar
 * según la pregunta en lenguaje natural del doctor.
 */
@Service
public class DoctorChatServicio {

    @Autowired
    private CitaServicio citaServicio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Value("${openai.api.key}")
    private String apiKey;

    /**
     * Procesa el mensaje del doctor:
     * 1. Detecta la intención (qué herramienta MCP usar)
     * 2. Ejecuta la herramienta correspondiente
     * 3. Envía los datos al LLM para generar una respuesta en lenguaje natural
     */
    public String procesarMensajeDoctor(String mensaje, Long medicoId) {
        try {
            // PASO 1: Detectar intención y ejecutar la herramienta MCP correspondiente
            String datosCitas = ejecutarHerramientaMcp(mensaje, medicoId);

            // PASO 2: Enviar al LLM con el contexto de los datos
            return llamarLlmConContexto(mensaje, datosCitas);

        } catch (Exception e) {
            return "Error procesando tu consulta: " + e.getMessage();
        }
    }

    /**
     * Simula el comportamiento del agente MCP:
     * analiza la pregunta en lenguaje natural y decide qué herramienta ejecutar.
     */
    private String ejecutarHerramientaMcp(String mensaje, Long medicoId) {
        String msgLower = mensaje.toLowerCase();

        // ═══════════════════════════════════════════════════
        // HERRAMIENTA: ELIMINAR CITA
        // ═══════════════════════════════════════════════════
        if (contieneAlguno(msgLower, "eliminar cita", "elimina la cita", "borrar cita", "borra la cita",
                "cancelar cita", "cancela la cita", "quitar cita", "eliminar la cita",
                "borrar la cita", "cancelar la cita", "elimina cita", "borra cita", "cancela cita",
                "elimine la cita", "borre la cita", "cancele la cita")) {
            Long citaId = extraerNumero(mensaje);
            if (citaId != null) {
                try {
                    // Buscar la cita antes de eliminar para mostrar info
                    Optional<Cita> citaOpt = citaServicio.buscarCitaPorId(citaId);
                    if (citaOpt.isPresent()) {
                        Cita cita = citaOpt.get();
                        String infoCita = formatearCitaIndividual(cita);
                        boolean eliminada = citaServicio.eliminarCita(citaId);
                        if (eliminada) {
                            return "✅ CITA ELIMINADA EXITOSAMENTE.\n\nDetalles de la cita eliminada:\n" + infoCita;
                        }
                    }
                    return "❌ No se encontró una cita con el ID " + citaId + ".";
                } catch (Exception e) {
                    return "❌ Error al eliminar la cita: " + e.getMessage();
                }
            }
            return "⚠️ Para eliminar una cita, indique el ID. Ejemplo: 'Eliminar cita 5'";
        }

        // ═══════════════════════════════════════════════════
        // HERRAMIENTA: ACTUALIZAR / MODIFICAR CITA
        // ═══════════════════════════════════════════════════
        if (contieneAlguno(msgLower, "actualizar cita", "actualiza la cita", "modificar cita",
                "modifica la cita", "reprogramar cita", "reprograma la cita", "cambiar cita",
                "cambia la cita", "mover cita", "mueve la cita", "reagendar cita",
                "actualizar la cita", "modificar la cita", "cambiar la cita", "mover la cita",
                "actualice la cita", "modifique la cita", "reprograme la cita")) {
            Long citaId = extraerNumero(mensaje);
            if (citaId != null) {
                try {
                    LocalDateTime nuevaFecha = extraerFechaHora(mensaje);
                    String nuevoLugar = extraerLugar(mensaje);
                    
                    if (nuevaFecha == null && (nuevoLugar == null || nuevoLugar.isEmpty())) {
                        // Mostrar la cita actual para que el doctor sepa qué modificar
                        Optional<Cita> citaOpt = citaServicio.buscarCitaPorId(citaId);
                        if (citaOpt.isPresent()) {
                            return "📋 Cita actual:\n" + formatearCitaIndividual(citaOpt.get()) +
                                   "\n\n⚠️ Indique la nueva fecha/hora y/o lugar. Ejemplo:\n" +
                                   "'Actualizar cita " + citaId + " para el 2026-06-15 10:00 en Consultorio B'";
                        }
                        return "❌ No se encontró la cita con ID " + citaId + ".";
                    }
                    
                    Cita citaActualizada = citaServicio.actualizarCita(citaId, nuevaFecha, nuevoLugar);
                    return "✅ CITA ACTUALIZADA EXITOSAMENTE.\n\nNuevos datos:\n" + formatearCitaIndividual(citaActualizada);
                } catch (Exception e) {
                    return "❌ Error al actualizar la cita: " + e.getMessage();
                }
            }
            return "⚠️ Para actualizar una cita, indique el ID. Ejemplo: 'Actualizar cita 5 para el 2026-06-20 14:00 en Consultorio A'";
        }

        // ═══════════════════════════════════════════════════
        // HERRAMIENTA: CREAR CITA
        // ═══════════════════════════════════════════════════
        if (contieneAlguno(msgLower, "crear cita", "crea una cita", "agendar cita", "agenda una cita",
                "nueva cita", "programar cita", "programa una cita", "agendar una cita",
                "crear una cita", "nueva cita para", "registrar cita", "registra una cita",
                "cree una cita", "agende una cita", "programe una cita")) {
            try {
                String emailPaciente = extraerEmail(mensaje);
                LocalDateTime fechaHora = extraerFechaHora(mensaje);
                String lugar = extraerLugar(mensaje);

                if (emailPaciente == null || emailPaciente.isEmpty()) {
                    return "⚠️ Para crear una cita indique el email del paciente.\n\n" +
                           "📝 Ejemplo: 'Crear cita para paciente@email.com el 2026-06-15 10:00 en Consultorio A'";
                }
                if (fechaHora == null) {
                    return "⚠️ Indique la fecha y hora de la cita.\n\n" +
                           "📝 Ejemplo: 'Crear cita para " + emailPaciente + " el 2026-06-15 10:00 en Consultorio A'";
                }
                if (lugar == null || lugar.isEmpty()) {
                    return "⚠️ Indique el lugar de la cita.\n\n" +
                           "📝 Ejemplo: 'Crear cita para " + emailPaciente + " el 2026-06-15 10:00 en Consultorio A'";
                }

                Cita nuevaCita = citaServicio.crearCitaDesdeChat(emailPaciente, fechaHora, lugar, medicoId);
                return "✅ CITA CREADA EXITOSAMENTE.\n\nDetalles:\n" + formatearCitaIndividual(nuevaCita);
            } catch (Exception e) {
                return "❌ Error al crear la cita: " + e.getMessage();
            }
        }

        // ═══════════════════════════════════════════════════
        // HERRAMIENTA: VER CITA ESPECÍFICA POR ID
        // ═══════════════════════════════════════════════════
        if (contieneAlguno(msgLower, "ver cita", "detalle cita", "detalles cita", "info cita",
                "información cita", "buscar cita", "consultar cita", "cita con id", "cita número",
                "ver la cita", "detalles de la cita", "información de la cita")) {
            Long citaId = extraerNumero(mensaje);
            if (citaId != null) {
                Optional<Cita> citaOpt = citaServicio.buscarCitaPorId(citaId);
                if (citaOpt.isPresent()) {
                    return "📋 Detalles de la cita:\n" + formatearCitaIndividual(citaOpt.get());
                }
                return "❌ No se encontró una cita con el ID " + citaId + ".";
            }
            return "⚠️ Indique el ID de la cita. Ejemplo: 'Ver cita 5'";
        }

        // ═══════════════════════════════════════════════════
        // HERRAMIENTA: MIS CITAS (del doctor autenticado)
        // ═══════════════════════════════════════════════════
        if (contieneAlguno(msgLower, "mis citas", "mi agenda", "tengo citas", "mis pacientes",
                "citas de hoy", "citas mañana", "citas programadas", "mi horario", "mis consultas")) {
            List<Cita> citas = citaServicio.obtenerCitasPorMedico(medicoId);
            return formatearCitas(citas, "sus citas asignadas");
        }

        // HERRAMIENTA: CITAS POR PACIENTE (ID)
        if (contieneAlguno(msgLower, "citas del paciente", "paciente con id", "usuario con id", "citas de usuario")) {
            Long idPaciente = extraerNumero(mensaje);
            if (idPaciente != null) {
                List<Cita> citas = citaServicio.obtenerCitasPorUsuario(idPaciente);
                return formatearCitas(citas, "las citas del paciente con ID " + idPaciente);
            }
            return "No se pudo identificar el ID del paciente. Por favor indique el ID numérico.";
        }

        // HERRAMIENTA: CITAS POR EMAIL DEL PACIENTE
        if (contieneAlguno(msgLower, "citas de ") && msgLower.contains("@")) {
            String email = extraerEmail(mensaje);
            if (email != null) {
                Usuario paciente = usuarioRepositorio.findByEmail(email);
                if (paciente != null) {
                    List<Cita> citas = citaServicio.obtenerCitasPorUsuario(paciente.getId());
                    return formatearCitas(citas, "las citas de " + paciente.getNombre() + " " + paciente.getApellido() + " (" + email + ")");
                }
                return "❌ No se encontró un paciente con el email: " + email;
            }
        }

        // HERRAMIENTA: CITAS POR MÉDICO ESPECÍFICO
        if (contieneAlguno(msgLower, "citas del medico", "citas del médico", "citas del doctor",
                "doctor con id", "medico con id", "médico con id")) {
            Long idMedico = extraerNumero(mensaje);
            if (idMedico != null) {
                List<Cita> citas = citaServicio.obtenerCitasPorMedico(idMedico);
                return formatearCitas(citas, "las citas del médico con ID " + idMedico);
            }
            return "No se pudo identificar el ID del médico.";
        }

        // HERRAMIENTA: TODAS LAS CITAS
        if (contieneAlguno(msgLower, "todas las citas", "listado completo", "todas", "listar citas",
                "ver todas", "cuantas citas hay", "cuántas citas hay", "resumen de citas", "resumen general")) {
            List<Cita> citas = citaServicio.obtenerTodasLasCitas();
            return formatearCitas(citas, "todas las citas del sistema");
        }

        // HERRAMIENTA: FILTRAR POR FECHA
        if (contieneAlguno(msgLower, "mañana", "hoy", "esta semana", "próxima", "proxima")) {
            List<Cita> todasCitas = citaServicio.obtenerCitasPorMedico(medicoId);
            LocalDate fechaFiltro = obtenerFechaDesdeMensaje(msgLower);
            List<Cita> filtradas = todasCitas.stream()
                    .filter(c -> c.getFechaHora().toLocalDate().equals(fechaFiltro))
                    .collect(Collectors.toList());
            String labelFecha = fechaFiltro.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return formatearCitas(filtradas, "sus citas para el " + labelFecha);
        }

        // Sin herramienta detectada: responder sin datos de citas
        return null;
    }

    /**
     * Llama al LLM (OpenAI) con el contexto de las citas obtenidas por MCP.
     */
    private String llamarLlmConContexto(String mensajeDoctor, String datosCitas) {
        String url = "https://api.openai.com/v1/chat/completions";
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4.1-mini");

        List<Map<String, String>> messages = new ArrayList<>();

        // System prompt para el agente doctor
        String systemPrompt = """
            Eres un asistente inteligente exclusivo para doctores en un sistema de gestión médica.
            Tienes acceso a herramientas MCP que te permiten gestionar citas médicas en la base de datos.

            Tus capacidades incluyen:
            - 📋 VER CITAS: Consultar citas del doctor, de un paciente, o todas las citas del sistema
            - ➕ CREAR CITAS: Agendar nuevas citas para pacientes
            - ✏️ MODIFICAR CITAS: Cambiar fecha/hora o lugar de citas existentes
            - 🗑️ ELIMINAR CITAS: Cancelar y eliminar citas

            Reglas:
            - Responde siempre en español
            - Sé conciso y profesional
            - Si recibes datos de citas, preséntalos de forma clara y organizada
            - Si no hay citas, indícalo amablemente
            - Si el doctor pregunta algo que no tiene que ver con citas, responde de forma general
              como asistente médico profesional
            - Cuando se realice una acción (crear, eliminar, actualizar), confirma la operación claramente
            """;

        if (datosCitas != null) {
            systemPrompt += "\n\nDATOS OBTENIDOS POR HERRAMIENTA MCP:\n" + datosCitas;
        }

        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", mensajeDoctor));

        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");
            return message.get("content").toString();

        } catch (RestClientException e) {
            return "Error al comunicarse con el servicio de IA: " + e.getMessage();
        }
    }

    // ── Utilidades ──

    private String formatearCitas(List<Cita> citas, String contexto) {
        if (citas == null || citas.isEmpty()) {
            return "No se encontraron citas para " + contexto + ".";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Se encontraron ").append(citas.size()).append(" cita(s) para ").append(contexto).append(":\n\n");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Cita c : citas) {
            sb.append("• ID Cita: ").append(c.getId())
              .append(" | Fecha: ").append(c.getFechaHora().format(fmt))
              .append(" | Lugar: ").append(c.getLugar())
              .append(" | Paciente: ").append(c.getUsuario().getNombre()).append(" ").append(c.getUsuario().getApellido())
              .append(" | Médico: ").append(c.getMedico().getNombre()).append(" ").append(c.getMedico().getApellido())
              .append(" | Email: ").append(c.getEmail())
              .append("\n");
        }
        return sb.toString();
    }

    private String formatearCitaIndividual(Cita cita) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return "• ID: " + cita.getId() +
               "\n• Fecha/Hora: " + cita.getFechaHora().format(fmt) +
               "\n• Lugar: " + cita.getLugar() +
               "\n• Paciente: " + cita.getUsuario().getNombre() + " " + cita.getUsuario().getApellido() +
               "\n• Médico: " + cita.getMedico().getNombre() + " " + cita.getMedico().getApellido() +
               "\n• Email: " + cita.getEmail();
    }

    private boolean contieneAlguno(String texto, String... palabras) {
        for (String p : palabras) {
            if (texto.contains(p)) return true;
        }
        return false;
    }

    private Long extraerNumero(String texto) {
        // Buscar números que no sean parte de una fecha (evitar capturar 2026, 06, 15, etc.)
        // Primero intentar extraer después de "cita" o "id"
        Pattern pCita = Pattern.compile("(?:cita|id)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher mCita = pCita.matcher(texto);
        if (mCita.find()) {
            return Long.parseLong(mCita.group(1));
        }

        // Fallback: buscar cualquier número
        Matcher m = Pattern.compile("\\d+").matcher(texto);
        if (m.find()) {
            return Long.parseLong(m.group());
        }
        return null;
    }

    /**
     * Extrae un email del mensaje.
     */
    private String extraerEmail(String texto) {
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = emailPattern.matcher(texto);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * Extrae fecha y hora del mensaje en formato "2026-06-15 10:00" o "15/06/2026 10:00".
     */
    private LocalDateTime extraerFechaHora(String texto) {
        // Formato ISO: 2026-06-15 10:00
        Pattern p1 = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{1,2}:\\d{2})");
        Matcher m1 = p1.matcher(texto);
        if (m1.find()) {
            try {
                String fechaStr = m1.group(1) + "T" + m1.group(2);
                return LocalDateTime.parse(fechaStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'H:mm"));
            } catch (DateTimeParseException e) {
                // intentar siguiente formato
            }
        }

        // Formato dd/MM/yyyy HH:mm
        Pattern p2 = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(\\d{1,2}:\\d{2})");
        Matcher m2 = p2.matcher(texto);
        if (m2.find()) {
            try {
                String fechaStr = m2.group(1) + " " + m2.group(2);
                return LocalDateTime.parse(fechaStr, DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm"));
            } catch (DateTimeParseException e) {
                // no se pudo parsear
            }
        }

        return null;
    }

    /**
     * Extrae el lugar del mensaje (texto después de "en ").
     */
    private String extraerLugar(String texto) {
        // Buscar "en <lugar>" al final o antes de otra cláusula
        Pattern p = Pattern.compile("(?:\\ben\\s+)(Consultorio\\s+\\w+|Hospital\\s+[^,\\.]+|Clínica\\s+[^,\\.]+|Sala\\s+\\w+|[A-Z][a-záéíóúñ\\s]+(?:\\s+\\w+)?)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) {
            return m.group(1).trim();
        }

        // Patrón más simple: "en <texto>" al final del mensaje
        Pattern p2 = Pattern.compile("\\ben\\s+(.+?)$", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(texto.trim());
        if (m2.find()) {
            String lugar = m2.group(1).trim();
            // No considerar "en el pasado", "en esa hora", etc. como lugar
            if (!lugar.toLowerCase().matches("(?:el pasado|esa hora|ese día|ese lugar|la base.*|la cita.*)")) {
                return lugar;
            }
        }

        return null;
    }

    private LocalDate obtenerFechaDesdeMensaje(String mensaje) {
        if (mensaje.contains("mañana")) {
            return LocalDate.now().plusDays(1);
        } else if (mensaje.contains("hoy")) {
            return LocalDate.now();
        }
        return LocalDate.now();
    }
}
