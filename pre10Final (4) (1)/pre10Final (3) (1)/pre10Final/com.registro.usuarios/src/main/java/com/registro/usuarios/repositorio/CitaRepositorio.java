package com.registro.usuarios.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.registro.usuarios.modelo.Cita;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepositorio extends JpaRepository<Cita, Long> {
    List<Cita> findByUsuario_Id(Long usuarioId);
    
    List<Cita> findByMedicoId(Long medicoId);
    
    boolean existsByLugarAndFechaHora(String lugar, LocalDateTime fechaHora);

    boolean existsByMedico_IdAndFechaHora(Long medicoId, LocalDateTime fechaHora);

    boolean existsByUsuario_IdAndFechaHora(Long usuarioId, LocalDateTime fechaHora);
    
    boolean existsByUsuario_IdAndFechaHoraBetween(Long usuarioId, LocalDateTime inicio, LocalDateTime fin);

    Optional<Cita> findTopByUsuario_IdOrderByFechaHoraDesc(Long usuarioId);
    
    List<Cita> findByUsuario_IdAndFechaHoraBetween(Long usuarioId, LocalDateTime desde, LocalDateTime hasta);

    // ===== CONSULTAS PAGINADAS =====
    
    /**
     * Citas de un usuario con paginación.
     */
    Page<Cita> findByUsuario_Id(Long usuarioId, Pageable pageable);
    
    /**
     * Citas de un médico con paginación.
     */
    Page<Cita> findByMedicoId(Long medicoId, Pageable pageable);
    
    /**
     * Todas las citas con paginación.
     */
    Page<Cita> findAll(Pageable pageable);
}
