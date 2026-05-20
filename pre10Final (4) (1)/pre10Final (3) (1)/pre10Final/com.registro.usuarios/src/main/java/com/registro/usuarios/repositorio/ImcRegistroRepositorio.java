package com.registro.usuarios.repositorio;

import com.registro.usuarios.modelo.ImcRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImcRegistroRepositorio extends JpaRepository<ImcRegistro, Long> {

    List<ImcRegistro> findByUsuarioIdOrderByFechaRegistroDesc(long usuarioId);
}
