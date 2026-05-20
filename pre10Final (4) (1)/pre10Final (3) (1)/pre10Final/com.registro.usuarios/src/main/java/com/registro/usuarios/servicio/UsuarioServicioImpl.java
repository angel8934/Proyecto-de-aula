package com.registro.usuarios.servicio;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.registro.usuarios.dto.UsuarioListaDTO;
import com.registro.usuarios.dto.UsuarioRegistroDTO;
import com.registro.usuarios.modelo.Rol;
import com.registro.usuarios.modelo.Usuario;
import com.registro.usuarios.repositorio.RolRepositorio;
import com.registro.usuarios.repositorio.UsuarioRepositorio;


@Service
public class UsuarioServicioImpl implements UsuarioServicio {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private RolRepositorio rolRepositorio;

    // Constructor para inyección de dependencias
    @Autowired
    public UsuarioServicioImpl(UsuarioRepositorio usuarioRepositorio, RolRepositorio rolRepositorio, BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Usuario guardar(UsuarioRegistroDTO registroDTO) {
        // Busca el rol "ROLE_PACIENTE" en la base de datos
        Rol rol = rolRepositorio.findByNombre("ROLE_PACIENTE");

        if (rol == null) {
            throw new RuntimeException("El rol 'ROLE_PACIENTE' no existe en la base de datos");
        }

        // Crea el nuevo usuario y asigna el rol existente
        Usuario usuario = new Usuario(registroDTO.getNombre(), registroDTO.getApellido(),registroDTO.getEdad(), registroDTO.getCelular(), registroDTO.getEmail(),
                passwordEncoder.encode(registroDTO.getPassword()), Arrays.asList(rol));

        return usuarioRepositorio.save(usuario);
    }



	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Usuario usuario = usuarioRepositorio.findByEmail(username);
		if(usuario == null) {
			throw new UsernameNotFoundException("Usuario o password invalidos ");
		} 
		return new User(usuario.getEmail(), usuario.getPassword(), mapearAutoridadesRoles(usuario.getRoles()));
	}

	
	private Collection<? extends GrantedAuthority> mapearAutoridadesRoles(Collection<Rol> roles){
		return roles.stream().map(role  ->  new SimpleGrantedAuthority(role.getNombre())).collect(Collectors.toList());
	}
	
	
	@Override
	public List<Usuario> listarUsuarios() {
		return usuarioRepositorio.findAll();
	} 
	
	public List<Usuario> listarUsuariosPorRol(long rolId) {
        return usuarioRepositorio.findByRoles_Id(rolId); 
    }
	
	public List<Usuario> listarDoctoresPorRol(long rolId) {
        return usuarioRepositorio.findByRoles_Id(rolId); 
    }
	public List<Usuario> listarAdministradoresPorRol(long rolId) {
        return usuarioRepositorio.findByRoles_Id(rolId); 
    }
	@Override
    public Usuario buscarPorUsername(String username) {
        // Cambia el nombre del método para que busque por email
        return usuarioRepositorio.findByEmail(username);
    }
    
    // ===== MÉTODOS PAGINADOS (OPTIMIZADOS) =====
    
    /**
     * Convierte un Usuario a UsuarioListaDTO (sin password).
     * Reduce el tamaño del JSON significativamente.
     */
    private UsuarioListaDTO convertirADTO(Usuario usuario) {
        return new UsuarioListaDTO(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getEdad(),
            usuario.getCelular(),
            usuario.getEmail()
        );
    }
    
    @Override
    public Page<UsuarioListaDTO> listarUsuariosPorRolPaginado(long rolId, Pageable pageable) {
        Page<Usuario> paginaUsuarios = usuarioRepositorio.findByRoles_Id(rolId, pageable);
        return paginaUsuarios.map(this::convertirADTO);
    }
    
    @Override
    public Page<UsuarioListaDTO> buscarUsuariosPorRol(long rolId, String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return listarUsuariosPorRolPaginado(rolId, pageable);
        }
        Page<Usuario> paginaUsuarios = usuarioRepositorio.buscarPorRolYFiltro(rolId, search.trim(), pageable);
        return paginaUsuarios.map(this::convertirADTO);
    }
    
    @Override
    public Page<UsuarioListaDTO> buscarUsuarios(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            Page<Usuario> paginaUsuarios = usuarioRepositorio.findAll(pageable);
            return paginaUsuarios.map(this::convertirADTO);
        }
        Page<Usuario> paginaUsuarios = usuarioRepositorio.buscarPorFiltro(search.trim(), pageable);
        return paginaUsuarios.map(this::convertirADTO);
    }
    
    @Override
    public long contarUsuariosPorRol(long rolId) {
        return usuarioRepositorio.countByRoles_Id(rolId);
    }
}
