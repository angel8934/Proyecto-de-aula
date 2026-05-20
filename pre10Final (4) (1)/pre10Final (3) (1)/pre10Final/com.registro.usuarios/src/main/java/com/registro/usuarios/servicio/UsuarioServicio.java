package com.registro.usuarios.servicio;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.registro.usuarios.dto.UsuarioListaDTO;
import com.registro.usuarios.dto.UsuarioRegistroDTO;
import com.registro.usuarios.modelo.Usuario;

public interface UsuarioServicio extends UserDetailsService {

    public Usuario guardar(UsuarioRegistroDTO registroDTO);
    
    public List<Usuario> listarUsuarios();
    
    public List<Usuario> listarUsuariosPorRol(long rol);
    
    public List<Usuario> listarDoctoresPorRol(long rol);
    
    public List<Usuario> listarAdministradoresPorRol(long rol);
    
    public Usuario buscarPorUsername(String username);
    
    // ===== MÉTODOS PAGINADOS =====
    
    /**
     * Lista usuarios por rol con paginación.
     * Retorna DTOs sin password para optimizar el JSON.
     */
    Page<UsuarioListaDTO> listarUsuariosPorRolPaginado(long rolId, Pageable pageable);
    
    /**
     * Busca usuarios por rol con filtro de texto y paginación.
     */
    Page<UsuarioListaDTO> buscarUsuariosPorRol(long rolId, String search, Pageable pageable);
    
    /**
     * Busca usuarios con filtro de texto y paginación (todos los roles).
     */
    Page<UsuarioListaDTO> buscarUsuarios(String search, Pageable pageable);
    
    /**
     * Cuenta usuarios por rol (para estadísticas).
     */
    long contarUsuariosPorRol(long rolId);
}
