
package com.registro.usuarios.seguridad;

import com.registro.usuarios.servicio.UsuarioServicio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    @Lazy
    private UsuarioServicio usuarioServicio;

    // Permite activar o desactivar las passkeys desde Railway.
    @Value("${passkeys.enabled:false}")
    private boolean passkeysEnabled;

    // Dominio donde funciona NUTRI VIDA.
    @Value("${passkeys.rp-id:localhost}")
    private String rpId;

    // Origen de la APK Android firmada.
    @Value("${passkeys.android-origin:}")
    private String origenAndroid;

    // ==========================================
    // CONTRASEÑAS
    // ==========================================

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider auth =
                new DaoAuthenticationProvider();

        auth.setUserDetailsService(usuarioServicio);
        auth.setPasswordEncoder(passwordEncoder());

        return auth;
    }

    // ==========================================
    // SEGURIDAD PRINCIPAL
    // ==========================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        // ======================================
        // PASSKEYS / HUELLA ANDROID
        // ======================================

        if (passkeysEnabled) {

            // Evitar iniciar con una configuración
            // biométrica incompleta.
            if (origenAndroid == null ||
                    origenAndroid.isBlank()) {

                throw new IllegalStateException(
                    "Falta configurar PASSKEY_ANDROID_ORIGIN "
                    + "en Railway."
                );
            }

            // Proteger las operaciones de WebAuthn
            // mediante token CSRF.
            http.csrf(csrf -> csrf
                .requireCsrfProtectionMatcher(
                    new OrRequestMatcher(

                        new AntPathRequestMatcher(
                            "/webauthn/**",
                            "POST"
                        ),

                        new AntPathRequestMatcher(
                            "/login/webauthn",
                            "POST"
                        )
                    )
                )
            );

            // Aceptar el origen criptográfico
            // de la APK Android autorizada.
            http.webAuthn(webAuthn -> webAuthn
                .rpName("NUTRI VIDA")
                .rpId(rpId)
                .allowedOrigins(origenAndroid)
            );

        } else {

            // Conservar la configuración anterior
            // mientras las passkeys estén desactivadas.
            http.csrf(csrf -> csrf.disable());
        }

        // ======================================
        // PERMISOS Y RUTAS
        // ======================================

        http.authorizeHttpRequests(auth -> auth

            // Chat exclusivo para doctores.
            .requestMatchers("/chat/doctor")
                .hasAuthority("ROLE_DOCTOR")

            // Endpoints públicos de autenticación
            // con passkey.
            .requestMatchers(
                "/webauthn/authenticate/options",
                "/login/webauthn",
                "/login/webauthn.js"
            )
            .permitAll()

            // Recursos y páginas públicas.
            .requestMatchers(
                "/",
                "/paginaPrincipal",
                "/api/registro/guardar",
                "/registro",
                "/registro/**",
                "/verificar-cuenta",

                // Asociación entre APK y dominio.
                "/.well-known/**",

                "/js/**",
                "/css/**",
                "/img/**",
                "/chat/openai",
                "/chat/ollama",
                "/sse",
                "/mcp/**"
            )
            .permitAll()

            // Las demás páginas requieren sesión.
            .anyRequest()
                .authenticated()
        )

        // ======================================
        // LOGIN CON CORREO Y CONTRASEÑA
        // ======================================

        .formLogin(form -> form

            .loginPage("/login")
            .permitAll()

            .successHandler(
                (request, response, authentication) -> {

                    String role = authentication
                        .getAuthorities()
                        .stream()
                        .findFirst()
                        .map(authority ->
                            authority.getAuthority()
                        )
                        .orElse("");

                    switch (role) {

                        case "ROLE_ADMINISTRADOR":
                            response.sendRedirect(
                                "/administrador"
                            );
                            break;

                        case "ROLE_PACIENTE":
                            response.sendRedirect(
                                "/index"
                            );
                            break;

                        case "ROLE_DOCTOR":
                            response.sendRedirect(
                                "/doctor"
                            );
                            break;

                        default:
                            response.sendRedirect(
                                "/paginaPrincipal"
                            );
                            break;
                    }
                }
            )
        )

        // ======================================
        // CERRAR SESIÓN
        // ======================================

        .logout(logout -> logout

            .invalidateHttpSession(true)
            .clearAuthentication(true)

            .logoutRequestMatcher(
                new AntPathRequestMatcher("/logout")
            )

            .logoutSuccessUrl("/paginaPrincipal")
            .permitAll()
        );

        return http.build();
    }

    // ==========================================
    // ADMINISTRADOR DE AUTENTICACIÓN
    // ==========================================

    @Bean
    public AuthenticationManager authManager(
            HttpSecurity http) throws Exception {

        AuthenticationManagerBuilder builder =
            http.getSharedObject(
                AuthenticationManagerBuilder.class
            );

        builder.authenticationProvider(
            authenticationProvider()
        );

        return builder.build();
    }
}
