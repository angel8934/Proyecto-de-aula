
package com.registro.usuarios.seguridad;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;

@Configuration
@ConditionalOnProperty(
    name = "passkeys.enabled",
    havingValue = "true"
)
public class PasskeyJdbcConfiguration {

    @Bean
    public ApplicationRunner crearTablasPasskeys(
            JdbcTemplate jdbc) {

        return args -> {

            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS user_entities (
                    id VARCHAR(512)
                        CHARACTER SET ascii
                        COLLATE ascii_bin NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    display_name VARCHAR(255),
                    PRIMARY KEY (id)
                )
            """);

            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS user_credentials (
                    credential_id VARCHAR(512)
                        CHARACTER SET ascii
                        COLLATE ascii_bin NOT NULL,
                    user_entity_user_id VARCHAR(512)
                        CHARACTER SET ascii
                        COLLATE ascii_bin NOT NULL,
                    public_key MEDIUMBLOB NOT NULL,
                    signature_count BIGINT,
                    uv_initialized BOOLEAN,
                    backup_eligible BOOLEAN NOT NULL,
                    authenticator_transports VARCHAR(1000),
                    public_key_credential_type VARCHAR(100),
                    backup_state BOOLEAN NOT NULL,
                    attestation_object MEDIUMBLOB,
                    attestation_client_data_json MEDIUMBLOB,
                    created TIMESTAMP NULL,
                    last_used TIMESTAMP NULL,
                    label VARCHAR(1000) NOT NULL,
                    PRIMARY KEY (credential_id)
                )
            """);
        };
    }

    @Bean
    public JdbcPublicKeyCredentialUserEntityRepository
            publicKeyCredentialRepository(
                    JdbcOperations jdbc) {

        return new JdbcPublicKeyCredentialUserEntityRepository(
            jdbc
        );
    }

    @Bean
    public JdbcUserCredentialRepository
            userCredentialRepository(
                    JdbcOperations jdbc) {

        return new JdbcUserCredentialRepository(jdbc);
    }
}
