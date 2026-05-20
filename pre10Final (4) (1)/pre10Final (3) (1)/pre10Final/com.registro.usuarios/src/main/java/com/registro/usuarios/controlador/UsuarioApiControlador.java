package com.registro.usuarios.controlador;

import com.registro.usuarios.dto.UsuarioListaDTO;
import com.registro.usuarios.servicio.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * API REST para consultas paginadas de usuarios.
 * Reemplaza las cargas masivas de findAll() con paginación eficiente.
 * 
 * Ejemplos de uso:
 *   GET /api/usuarios/rol/1?page=0&size=20              → Pacientes, página 1
 *   GET /api/usuarios/rol/1?page=0&size=20&search=juan   → Buscar "juan" en pacientes
 *   GET /api/usuarios/rol/2?page=0&size=20              → Doctores, página 1
 *   GET /api/usuarios/buscar?search=garcia&page=0&size=10 → Buscar en todos los usuarios
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioApiControlador {

    @Autowired
    private UsuarioServicio usuarioServicio;

    /**
     * Lista usuarios por rol con paginación y búsqueda.
     * @param rolId  ID del rol (1=paciente, 2=doctor, 3=admin)
     * @param page   Número de página (0-indexed)
     * @param size   Tamaño de página (default 20, max 100)
     * @param search Texto de búsqueda opcional (nombre, apellido, email)
     * @param sort   Campo de ordenamiento (default: nombre)
     */
    @GetMapping("/rol/{rolId}")
    public Page<UsuarioListaDTO> listarPorRol(
            @PathVariable long rolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "nombre") String sort) {
        
        // Limitar tamaño máximo de página para evitar abusos
        size = Math.min(size, 100);
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sort).ascending());
        
        if (search.isEmpty()) {
            return usuarioServicio.listarUsuariosPorRolPaginado(rolId, pageRequest);
        }
        return usuarioServicio.buscarUsuariosPorRol(rolId, search, pageRequest);
    }

    /**
     * Busca usuarios en todos los roles con paginación.
     * Útil para dropdowns de selección de paciente (historial médico).
     */
    @GetMapping("/buscar")
    public Page<UsuarioListaDTO> buscarUsuarios(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        size = Math.min(size, 100);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("nombre").ascending());
        
        return usuarioServicio.buscarUsuarios(search, pageRequest);
    }

    /**
     * Endpoint para contar usuarios por rol (estadísticas del dashboard).
     */
    @GetMapping("/contar/{rolId}")
    public long contarPorRol(@PathVariable long rolId) {
        return usuarioServicio.contarUsuariosPorRol(rolId);
    }
}
