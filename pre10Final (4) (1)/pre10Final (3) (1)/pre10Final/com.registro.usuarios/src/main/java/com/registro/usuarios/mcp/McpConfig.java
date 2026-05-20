package com.registro.usuarios.mcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    // Registra la clase de herramientas como un Bean para que el servidor MCP la
    // detecte
    // y exponga sus métodos (marcados con @Tool) a los clientes MCP.
    @Bean
    public CitaMcpTools citaMcpTools() {
        return new CitaMcpTools();
    }
}
