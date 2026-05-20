package com.registro.usuarios.servicio;

import com.registro.usuarios.modelo.HistorialMedico;
import com.registro.usuarios.repositorio.HistorialMedicoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistorialMedicoServicio {

    @Autowired
    private HistorialMedicoRepositorio historialMedicoRepositorio;

    public List<HistorialMedico> obtenerTodos() {
        return historialMedicoRepositorio.findAll();
    }

    public HistorialMedico guardarHistorial(HistorialMedico historialMedico) {
        return historialMedicoRepositorio.save(historialMedico);
    }
    
    
    public List<HistorialMedico> buscarPorUsuarioId(Long usuarioId) {
        return historialMedicoRepositorio.findByUsuario_Id(usuarioId);
    }
}
