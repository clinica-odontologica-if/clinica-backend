package com.clinica.servico_profissional.service;

import com.clinica.servico_profissional.dto.ProfissionalRequest;
import com.clinica.servico_profissional.dto.ProfissionalResponse;
import com.clinica.servico_profissional.exception.RecursoNaoEncontradoException;
import com.clinica.servico_profissional.exception.RegraDeNegocioException;
import com.clinica.servico_profissional.model.Profissional;
import com.clinica.servico_profissional.model.Role;
import com.clinica.servico_profissional.repository.ProfissionalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfissionalServiceTest {

    @Mock
    private ProfissionalRepository profissionalRepository;

    @InjectMocks
    private ProfissionalService profissionalService;

    // ---------- cadastrar ----------

    @Test
    void cadastrar_comDadosValidos_deveSalvarComoAtivoERetornarResponse() {
        ProfissionalRequest request = new ProfissionalRequest(
                "Dra. Marina Souza", "marina@clinica.com",
                "CRO-SP11111", "Periodontia", Role.DENTISTA
        );

        when(profissionalRepository.existsByEmail("marina@clinica.com")).thenReturn(false);

        when(profissionalRepository.save(any(Profissional.class))).thenAnswer(invocacao -> {
            Profissional p = invocacao.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProfissionalResponse response = profissionalService.cadastrar(request);

        assertEquals(1L, response.getId());
        assertEquals("Dra. Marina Souza", response.getNome());
        assertTrue(response.isAtivo());
    }

    @Test
    void cadastrar_comEmailJaExistente_deveLancarRegraDeNegocio() {
        ProfissionalRequest request = new ProfissionalRequest(
                "Dra. Marina Souza", "marina@clinica.com",
                "CRO-SP11111", "Periodontia", Role.DENTISTA
        );

        when(profissionalRepository.existsByEmail("marina@clinica.com")).thenReturn(true);

        assertThrows(RegraDeNegocioException.class,
                () -> profissionalService.cadastrar(request));

        verify(profissionalRepository, never()).save(any());
    }

    @Test
    void cadastrar_comNomeEmBranco_deveLancarRegraDeNegocio() {
        ProfissionalRequest request = new ProfissionalRequest(
                "  ", "marina@clinica.com", "CRO-SP11111", "Periodontia", Role.DENTISTA
        );

        assertThrows(RegraDeNegocioException.class,
                () -> profissionalService.cadastrar(request));

        verify(profissionalRepository, never()).existsByEmail(any());
        verify(profissionalRepository, never()).save(any());
    }

    // ---------- atualizar ----------

    @Test
    void atualizar_comIdExistenteEEmailMantido_deveAtualizarOsDemaisCampos() {
        Profissional existente = new Profissional(
                1L, "Dra. Marina Souza", "marina@clinica.com",
                "CRO-SP11111", "Periodontia", Role.DENTISTA,
                true, LocalDateTime.now()
        );

        ProfissionalRequest request = new ProfissionalRequest(
                "Dra. Marina Souza Lima", "marina@clinica.com",
                "CRO-SP11111", "Ortodontia", Role.DENTISTA
        );

        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(profissionalRepository.save(any(Profissional.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfissionalResponse response = profissionalService.atualizar(1L, request);

        assertEquals("Dra. Marina Souza Lima", response.getNome());
        assertEquals("Ortodontia", response.getEspecialidade());
        verify(profissionalRepository, never()).existsByEmail(any());
    }

    @Test
    void atualizar_comEmailNovoDisponivel_devePermitirTroca() {
        Profissional existente = new Profissional(
                1L, "Dra. Marina Souza", "marina@clinica.com",
                "CRO-SP11111", "Periodontia", Role.DENTISTA,
                true, LocalDateTime.now()
        );

        ProfissionalRequest request = new ProfissionalRequest(
                "Dra. Marina Souza", "marina.souza@clinica.com",
                "CRO-SP11111", "Periodontia", Role.DENTISTA
        );

        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(profissionalRepository.existsByEmail("marina.souza@clinica.com")).thenReturn(false);
        when(profissionalRepository.save(any(Profissional.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfissionalResponse response = profissionalService.atualizar(1L, request);

        assertEquals("marina.souza@clinica.com", response.getEmail());
    }

    @Test
    void atualizar_comEmailJaPertencenteAOutroProfissional_deveLancarRegraDeNegocio() {
        Profissional marina = new Profissional(
                1L, "Dra. Marina Souza", "marina@clinica.com",
                "CRO-SP11111", "Periodontia", Role.DENTISTA,
                true, LocalDateTime.now()
        );

        ProfissionalRequest request = new ProfissionalRequest(
                "Dra. Marina Souza", "carlos@clinica.com",
                "CRO-SP11111", "Periodontia", Role.DENTISTA
        );

        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(marina));
        when(profissionalRepository.existsByEmail("carlos@clinica.com")).thenReturn(true);

        assertThrows(RegraDeNegocioException.class,
                () -> profissionalService.atualizar(1L, request));

        verify(profissionalRepository, never()).save(any());
    }

    @Test
    void atualizar_comIdInexistente_deveLancarRecursoNaoEncontrado() {
        ProfissionalRequest request = new ProfissionalRequest(
                "Dra. Marina Souza", "marina@clinica.com",
                "CRO-SP11111", "Periodontia", Role.DENTISTA
        );

        when(profissionalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> profissionalService.atualizar(999L, request));

        verify(profissionalRepository, never()).save(any());
    }

    // ---------- inativar ----------

    @Test
    void inativar_comIdExistente_deveMarcarAtivoComoFalseESalvar() {
        Profissional existente = new Profissional(
                1L, "Dra. Marina Souza", "marina@clinica.com",
                "CRO-SP11111", "Periodontia", Role.DENTISTA,
                true, LocalDateTime.now()
        );

        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(existente));

        profissionalService.inativar(1L);

        ArgumentCaptor<Profissional> captor = ArgumentCaptor.forClass(Profissional.class);
        verify(profissionalRepository).save(captor.capture());

        assertFalse(captor.getValue().isAtivo());
    }

    @Test
    void inativar_comIdInexistente_deveLancarRecursoNaoEncontrado() {
        when(profissionalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> profissionalService.inativar(999L));

        verify(profissionalRepository, never()).save(any());
    }
}