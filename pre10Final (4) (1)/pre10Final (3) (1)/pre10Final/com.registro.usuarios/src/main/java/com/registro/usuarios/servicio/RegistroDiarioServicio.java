package com.registro.usuarios.servicio;

import com.registro.usuarios.dto.RegistroDiarioDTO;
import com.registro.usuarios.modelo.RegistroDiario;
import com.registro.usuarios.repositorio.RegistroDiarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class RegistroDiarioServicio {

    @Autowired
    private RegistroDiarioRepositorio repositorio;

    public RegistroDiario guardarRegistro(RegistroDiarioDTO dto) {
        RegistroDiario registro = new RegistroDiario();

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date fechaDate = formatter.parse(dto.getFecha());
            registro.setFecha(fechaDate);
        } catch (ParseException e) {
            e.printStackTrace();
            registro.setFecha(null);
        }


        registro.setCalorias(dto.getCalorias());
        registro.setProteinas(dto.getProteinas());
        registro.setGrasas(dto.getGrasas());
        registro.setCarbohidratos(dto.getCarbohidratos());
        registro.setAlimentos(dto.getAlimentos());

        return repositorio.save(registro);
    }
}
