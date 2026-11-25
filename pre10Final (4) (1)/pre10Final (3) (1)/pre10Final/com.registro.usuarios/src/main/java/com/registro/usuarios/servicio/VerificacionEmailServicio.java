
package com.registro.usuarios.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.registro.usuarios.modelo.TokenVerificacionEmail;
import com.registro.usuarios.modelo.Usuario;
import com.registro.usuarios.repositorio.TokenVerificacionRepositorio;
import com.registro.usuarios.repositorio.UsuarioRepositorio;

@Service
public class VerificacionEmailServicio {

    private final TokenVerificacionRepositorio tokens;
    private final UsuarioRepositorio usuarios;
    private final RestClient cliente;

    private final String apiKey;
    private final String remitente;
    private final String urlPublica;

    private final SecureRandom random = new SecureRandom();

    public VerificacionEmailServicio(
            TokenVerificacionRepositorio tokens,
            UsuarioRepositorio usuarios,
            @Value("${RESEND_API_KEY:}") String apiKey,
            @Value("${RESEND_FROM_EMAIL:}") String remitente,
            @Value("${APP_PUBLIC_URL:http://localhost:8080}") String urlPublica) {

        this.tokens = tokens;
        this.usuarios = usuarios;
        this.apiKey = apiKey;
        this.remitente = remitente;
        this.urlPublica = urlPublica.replaceAll("/+$", "");

        SimpleClientHttpRequestFactory factory =
            new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        this.cliente = RestClient.builder()
            .requestFactory(factory)
            .build();
    }

    public boolean estaConfigurado() {
        return !apiKey.isBlank()
            && !remitente.isBlank()
            && !urlPublica.isBlank();
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }

    private String calcularHash(String token) {
        try {
            MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

            byte[] resultado = digest.digest(
                token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(resultado);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "No se pudo calcular el hash del token.",
                e
            );
        }
    }

    private LocalDateTime ahoraUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public boolean enviarConLimite(Usuario usuario) {

        if (!estaConfigurado()) {
            throw new IllegalStateException(
                "El servicio de correo no esta configurado."
            );
        }

        if (usuario.isEmailVerificado()) {
            return false;
        }

        LocalDateTime ahora = ahoraUtc();

        // No reenviar antes de 10 minutos
        var ultimo = tokens.findTopByUsuarioOrderByIdDesc(
            usuario
        );

        if (ultimo.isPresent()
                && ultimo.get().getCreadoEn()
                    .isAfter(ahora.minusMinutes(10))) {
            return false;
        }

        // Maximo 5 solicitudes por cuenta en 24 horas
        long total = tokens.countByUsuarioAndCreadoEnAfter(
            usuario,
            ahora.minusHours(24)
        );

        if (total >= 5) {
            return false;
        }

        String tokenOriginal = generarToken();

        TokenVerificacionEmail entidad =
            new TokenVerificacionEmail();

        entidad.setUsuario(usuario);
        entidad.setTokenHash(calcularHash(tokenOriginal));
        entidad.setCreadoEn(ahora);
        entidad.setExpiraEn(ahora.plusMinutes(30));
        entidad.setUsado(false);

        tokens.save(entidad);

        String enlace = urlPublica
            + "/verificar-cuenta?token="
            + tokenOriginal;

        String mensaje =
            "Bienvenido a NUTRI VIDA.\n\n"
            + "Para activar tu cuenta abre el siguiente enlace:\n\n"
            + enlace
            + "\n\nEste enlace vence en 30 minutos."
            + "\nSi no creaste esta cuenta, ignora este mensaje.";

        cliente.post()
            .uri("https://api.resend.com/emails")
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "from", remitente,
                "to", List.of(usuario.getEmail()),
                "subject", "Confirma tu cuenta de NUTRI VIDA",
                "text", mensaje
            ))
            .retrieve()
            .toBodilessEntity();

        return true;
    }

    @Transactional
    public boolean confirmar(String tokenOriginal) {

        if (tokenOriginal == null
                || !tokenOriginal.matches("[A-Za-z0-9_-]{43}")) {
            return false;
        }

        var encontrado = tokens.findByTokenHash(
            calcularHash(tokenOriginal)
        );

        if (encontrado.isEmpty()) {
            return false;
        }

        TokenVerificacionEmail token = encontrado.get();

        if (token.isUsado()
                || !token.getExpiraEn().isAfter(ahoraUtc())) {
            return false;
        }

        Usuario usuario = token.getUsuario();

        if (usuario.isEmailVerificado()) {
            return false;
        }

        usuario.setEmailVerificado(true);
        token.setUsado(true);

        usuarios.save(usuario);
        tokens.save(token);

        return true;
    }

    public void reenviar(String correo) {

        if (correo == null || correo.isBlank()) {
            return;
        }

        Usuario usuario = usuarios.findByEmailIgnoreCase(
            correo.trim()
        );

        if (usuario != null && !usuario.isEmailVerificado()) {
            enviarConLimite(usuario);
        }
    }
}
