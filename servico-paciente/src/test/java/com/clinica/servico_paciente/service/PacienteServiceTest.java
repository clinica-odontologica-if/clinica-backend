package com.clinica.servico_paciente.service;

import com.clinica.servico_paciente.dto.PacienteResponse;
import com.clinica.servico_paciente.model.Paciente;
import com.clinica.servico_paciente.repository.PacienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PacienteService")
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteService pacienteService;

    @Test
    @DisplayName("deve listar pacientes ativos aplicando filtros")
    void deveListarPacientesAtivosAplicandoFiltros() {
        Paciente paciente = new Paciente(
                1L,
                "Maria Silva",
                "12345678901",
                LocalDate.of(1990, 1, 10),
                "maria@clinica.com",
                "31999990000",
                "Rua A",
                "Sem observacoes",
                true,
                null
        );

        when(pacienteRepository.buscarAtivosComFiltros("maria", "12345678901"))
                .thenReturn(List.of(paciente));

        List<PacienteResponse> response = pacienteService.listarAtivos(
                " Maria ",
                "123.456.789-01"
        );

        assertThat(response).hasSize(1);
        assertThat(response.get(0).nome()).isEqualTo("Maria Silva");
        verify(pacienteRepository).buscarAtivosComFiltros("maria", "12345678901");
    }
}
