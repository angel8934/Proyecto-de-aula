package com.registro.usuarios.repositorio;

import com.registro.usuarios.modelo.RegistroDiario;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RegistroDiarioRepositorio extends MongoRepository<RegistroDiario, String> {
    
}
