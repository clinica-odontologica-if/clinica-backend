package com.clinica.servico_atendimento.repository;

import com.clinica.servico_atendimento.model.Atendimento;
import com.clinica.servico_atendimento.model.StatusAtendimento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("AtendimentoRepository")
class AtendimentoRepositoryTest {

    @Autowired
    private AtendimentoRepository atendimentoRepository;

    @Test
    @DisplayName("deve identificar conflito de horario para profissional")
    void deveIdentificarConflitoDeHorarioParaProfissional() {
        Atendimento atendimento = atendimentoBase();
        atendimentoRepository.saveAndFlush(atendimento);

        boolean existeConflito = atendimentoRepository
                .existsByProfissionalIdAndDataAtendimentoAndHoraAtendimentoAndStatusInAndAtivoTrue(
                        2L,
                        LocalDate.of(2026, 7, 10),
                        LocalTime.of(14, 30),
                        List.of(StatusAtendimento.AGENDADO, StatusAtendimento.CONFIRMADO)
                );

        assertThat(existeConflito).isTrue();
    }

    @Test
    @DisplayName("deve filtrar atendimentos ativos")
    void deveFiltrarAtendimentosAtivos() {
        Atendimento atendimento = atendimentoBase();
        atendimentoRepository.saveAndFlush(atendimento);

        List<Atendimento> encontrados = atendimentoRepository.buscarAtivosComFiltros(
                1L,
                2L,
                LocalDate.of(2026, 7, 10),
                StatusAtendimento.AGENDADO
        );

        assertThat(encontrados).hasSize(1);
        assertThat(encontrados.get(0).getPacienteNome()).isEqualTo("Maria Silva");
    }

    private Atendimento atendimentoBase() {
        Atendimento atendimento = new Atendimento();
        atendimento.setPacienteId(1L);
        atendimento.setPacienteNome("Maria Silva");
        atendimento.setProfissionalId(2L);
        atendimento.setProfissionalNome("Dra Ana");
        atendimento.setProfissionalEmail("ana@clinica.com");
        atendimento.setDataAtendimento(LocalDate.of(2026, 7, 10));
        atendimento.setHoraAtendimento(LocalTime.of(14, 30));
        atendimento.setStatus(StatusAtendimento.AGENDADO);
        atendimento.setObservacoes("Primeira consulta");
        atendimento.setAtivo(true);
        return atendimento;
    }
}
