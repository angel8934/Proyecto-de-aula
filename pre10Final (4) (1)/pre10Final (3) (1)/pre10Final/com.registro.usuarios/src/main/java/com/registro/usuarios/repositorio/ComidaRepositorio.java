package com.registro.usuarios.repositorio;



import com.registro.usuarios.modelo.Comida;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComidaRepositorio extends MongoRepository<Comida,String>{
	// Aquí se agregan consultas personalizadas si lo necesitas más adelante

}
