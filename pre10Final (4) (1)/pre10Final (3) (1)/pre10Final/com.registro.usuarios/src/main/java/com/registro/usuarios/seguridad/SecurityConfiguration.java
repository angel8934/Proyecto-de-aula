package com.registro.usuarios.seguridad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.registro.usuarios.servicio.UsuarioServicio;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    @Lazy
    private UsuarioServicio usuarioServicio;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(usuarioServicio);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 🔒 Chat MCP exclusivo para doctores (debe ir ANTES de otras reglas)
                .requestMatchers("/chat/doctor").hasAuthority("ROLE_DOCTOR")
                // ✅ Permitimos acceso libre a la página principal y recursos estáticos
                .requestMatchers(
                    "/", 
                    "/paginaPrincipal", 
                    "/api/registro/guardar",
                    "/registro**",
                    "/js/**",
                    "/css/**",
                    "/img/**",
                    "/chat/openai",
                    "/chat/ollama",
                    "/sse",
                    "/mcp/**"
                ).permitAll()
                // 🔒 Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )
            // ✅ Configuración del login
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
                .successHandler((request, response, authentication) -> {
                    String role = authentication.getAuthorities().stream()
                            .findFirst()
                            .map(grantedAuthority -> grantedAuthority.getAuthority())
                            .orElse("");

                    switch (role) {
                        case "ROLE_ADMINISTRADOR":
                            response.sendRedirect("/administrador");
                            break;
                        case "ROLE_PACIENTE":
                            response.sendRedirect("/index");
                            break;
                        case "ROLE_DOCTOR":
                            response.sendRedirect("/doctor");
                            break;
                        default:
                            // ✅ Si el usuario no tiene rol, enviamos a la página principal
                            response.sendRedirect("/paginaPrincipal");
                            break;
                    }
                })
            )
            // ✅ Logout configurado
            .logout(logout -> logout
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/paginaPrincipal") // 👈 vuelve a la página principal al cerrar sesión
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(authenticationProvider());
        return authenticationManagerBuilder.build();
    }
}
