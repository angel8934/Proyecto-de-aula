package com.registro.usuarios.repositorio;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.registro.usuarios.modelo.Usuario;

@Repository
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long>{

	public Usuario findByEmail(String email);
	
	 List<Usuario> findByRoles_Id(Long rolId); 
	 
	 // ===== CONSULTAS PAGINADAS =====
	 
	 /**
	  * Busca usuarios por rol con paginación.
	  * Evita cargar los 40k+ registros de golpe.
	  */
	 Page<Usuario> findByRoles_Id(Long rolId, Pageable pageable);
	 
	 /**
	  * Busca usuarios por rol con filtro de búsqueda y paginación.
	  * Permite buscar por nombre, apellido o email.
	  */
	 @Query("SELECT u FROM Usuario u JOIN u.roles r WHERE r.id = :rolId " +
	        "AND (LOWER(u.nombre) LIKE LOWER(CONCAT('%', :search, '%')) " +
	        "OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :search, '%')) " +
	        "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
	 Page<Usuario> buscarPorRolYFiltro(
			 @Param("rolId") Long rolId, 
			 @Param("search") String search, 
			 Pageable pageable);
	 
	 /**
	  * Busca usuarios con filtro de búsqueda y paginación (sin filtrar por rol).
	  * Para dropdowns de selección de paciente.
	  */
	 @Query("SELECT u FROM Usuario u WHERE " +
	        "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :search, '%')) " +
	        "OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :search, '%')) " +
	        "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
	 Page<Usuario> buscarPorFiltro(
			 @Param("search") String search, 
			 Pageable pageable);
	 
	 /**
	  * Cuenta usuarios por rol (para estadísticas sin cargar datos).
	  */
	 long countByRoles_Id(Long rolId);
}
