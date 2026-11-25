
package com.registro.usuarios.repositorio;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.registro.usuarios.modelo.TokenVerificacionEmail;
import com.registro.usuarios.modelo.Usuario;

public interface TokenVerificacionRepositorio
        extends JpaRepository<TokenVerificacionEmail, Long> {

    Optional<TokenVerificacionEmail> findByTokenHash(
        String tokenHash
    );

    Optional<TokenVerificacionEmail> findTopByUsuarioOrderByIdDesc(
        Usuario usuario
    );

    long countByUsuarioAndCreadoEnAfter(
        Usuario usuario,
        LocalDateTime fecha
    );
}
