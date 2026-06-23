package com.clinica.servico_profissional;

import com.clinica.servico_profissional.model.Profissional;
import com.clinica.servico_profissional.model.Role;
import com.clinica.servico_profissional.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProfissionalRepository profissionalRepository;

    @Override
    public void run(String... args) {
        if (profissionalRepository.count() == 0) {
            profissionalRepository.save(new Profissional(
                    null,
                    "Dr. João Silva",
                    "dentista@clinica.com",
                    "CRO-SP12345",
                    "Ortodontia",
                    Role.DENTISTA,
                    true,
                    LocalDateTime.now()
            ));
            profissionalRepository.save(new Profissional(
                    null,
                    "Dra. Ana Costa",
                    "ana.costa@clinica.com",
                    "CRO-SP54321",
                    "Endodontia",
                    Role.DENTISTA,
                    true,
                    LocalDateTime.now()
            ));
            profissionalRepository.save(new Profissional(
                    null,
                    "Carlos Auxiliar",
                    "auxiliar@clinica.com",
                    "CRO-SP99999",
                    "Auxiliar Odontológico",
                    Role.AUXILIAR,
                    true,
                    LocalDateTime.now()
            ));
        }
    }
}
