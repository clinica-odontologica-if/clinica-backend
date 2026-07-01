# Sprint 4 - Planejamento Tecnico

## Objetivo

Implementar os servicos de estoque e financeiro, integrar os novos fluxos ao sistema da clinica e preparar a entrega final com testes, documentacao e execucao via Docker.

A Sprint 4 deve priorizar estabilidade e rastreabilidade. Estoque e financeiro precisam funcionar de forma simples, mas profissional: com historico, validacoes, controle de acesso, testes automatizados e contratos HTTP claros.

## Modulos envolvidos

| Modulo | Papel |
| --- | --- |
| `servico-estoque` | Gerenciar materiais, saldos, estoque minimo e movimentacoes |
| `servico-financeiro` | Gerenciar receitas, despesas e relatorios financeiros |
| `servico-atendimento` | Fonte de validacao para receitas vinculadas a atendimentos |
| `servico-autenticacao` | Emissao de JWT e perfis de acesso |
| `frontend-autenticacao` | Interface simples para estoque, financeiro e relatorios |
| `mysql` | Persistencia dos novos bancos/schema dos servicos |

## Portas planejadas

| Servico | Porta |
| --- | ---: |
| `servico-estoque` | 8085 |
| `servico-financeiro` | 8086 |

## Padroes tecnicos

Os novos servicos devem seguir o padrao ja usado nos servicos de dominio:

```text
com.clinica.servico_<nome>/
|-- controller/
|-- service/
|-- repository/
|-- model/
|-- dto/
|-- security/
`-- exception/
```

Regras obrigatorias:

- Validar JWT em todos os endpoints protegidos.
- Expor health check publico em `/<recurso>/health`.
- Usar DTOs para entrada e saida.
- Nao retornar entidades JPA diretamente.
- Usar `@PreAuthorize` para perfis.
- Usar Flyway para schema inicial.
- Adicionar Dockerfile, entrada no `docker-compose.yml`, modulo no `pom.xml` raiz e workflow CI.
- Criar testes unitarios, repository/controller e integracao onde houver regra entre camadas.

## servico-estoque

### Responsabilidade

Controlar materiais usados pela clinica, quantidade disponivel, quantidade minima, situacao de estoque baixo e historico de movimentacoes.

### Entidades

#### Material

Campos planejados:

| Campo | Tipo | Observacao |
| --- | --- | --- |
| `id` | Long | Identificador |
| `nome` | String | Obrigatorio |
| `descricao` | String | Opcional |
| `categoria` | String | Ex.: descartavel, medicamento, instrumento |
| `unidadeMedida` | Enum | `UNIDADE`, `CAIXA`, `PACOTE`, `ML`, `MG`, `G` |
| `quantidadeAtual` | BigDecimal | Saldo atual, nunca negativo |
| `quantidadeMinima` | BigDecimal | Limite para alerta |
| `ativo` | boolean | Inativacao logica |
| `criadoEm` | LocalDateTime | Auditoria |
| `atualizadoEm` | LocalDateTime | Auditoria |

#### MovimentacaoEstoque

Campos planejados:

| Campo | Tipo | Observacao |
| --- | --- | --- |
| `id` | Long | Identificador |
| `materialId` | Long | Material movimentado |
| `tipo` | Enum | `ENTRADA`, `SAIDA`, `AJUSTE` |
| `quantidade` | BigDecimal | Valor positivo |
| `saldoAnterior` | BigDecimal | Saldo antes da movimentacao |
| `saldoAtual` | BigDecimal | Saldo depois da movimentacao |
| `motivo` | String | Obrigatorio para saida e ajuste |
| `usuarioEmail` | String | Email extraido do JWT, quando disponivel |
| `criadoEm` | LocalDateTime | Data da movimentacao |

### Regras de negocio

- Material novo inicia com `quantidadeAtual` informada ou `0`.
- `quantidadeMinima` nao pode ser negativa.
- Entrada soma ao saldo atual.
- Saida subtrai do saldo atual.
- Saida nao pode deixar saldo negativo.
- Ajuste define um novo saldo e registra saldo anterior/saldo atual.
- Material inativo nao pode receber movimentacao.
- Estoque baixo ocorre quando `quantidadeAtual <= quantidadeMinima`.
- Inativar material nao apaga historico de movimentacoes.

### Endpoints planejados

| Metodo | Rota | Perfis | Uso |
| --- | --- | --- | --- |
| `GET` | `/estoque/health` | Publico | Health check |
| `POST` | `/materiais` | `GERENTE`, `AUXILIAR` | Cadastrar material |
| `GET` | `/materiais` | `GERENTE`, `ATENDENTE`, `DENTISTA`, `AUXILIAR` | Listar materiais com filtros |
| `GET` | `/materiais/{id}` | `GERENTE`, `ATENDENTE`, `DENTISTA`, `AUXILIAR` | Buscar material |
| `PUT` | `/materiais/{id}` | `GERENTE`, `AUXILIAR` | Atualizar material |
| `DELETE` | `/materiais/{id}` | `GERENTE` | Inativar material |
| `POST` | `/materiais/{id}/movimentacoes` | `GERENTE`, `AUXILIAR` | Registrar entrada, saida ou ajuste |
| `GET` | `/materiais/{id}/movimentacoes` | `GERENTE`, `AUXILIAR` | Historico de material |
| `GET` | `/materiais/alertas/baixo-estoque` | `GERENTE`, `AUXILIAR` | Listar materiais abaixo do minimo |

### Filtros planejados

`GET /materiais`:

- `busca`: nome, descricao ou categoria.
- `categoria`: categoria exata.
- `baixoEstoque`: `true` ou `false`.
- `ativo`: `true` ou `false`.

## servico-financeiro

### Responsabilidade

Controlar receitas vinculadas a atendimentos, despesas operacionais e relatorios financeiros por periodo.

### Entidades

#### Receita

Campos planejados:

| Campo | Tipo | Observacao |
| --- | --- | --- |
| `id` | Long | Identificador |
| `atendimentoId` | Long | Obrigatorio para receita clinica |
| `pacienteId` | Long | Copiado do atendimento, quando retornado |
| `profissionalId` | Long | Copiado do atendimento, quando retornado |
| `descricao` | String | Ex.: Pagamento de atendimento |
| `valor` | BigDecimal | Maior que zero |
| `formaPagamento` | Enum | `DINHEIRO`, `PIX`, `CARTAO_CREDITO`, `CARTAO_DEBITO`, `CONVENIO` |
| `status` | Enum | `PENDENTE`, `PAGO`, `CANCELADO` |
| `dataVencimento` | LocalDate | Opcional |
| `dataPagamento` | LocalDate | Obrigatoria quando `PAGO` |
| `ativo` | boolean | Inativacao logica |
| `criadoEm` | LocalDateTime | Auditoria |
| `atualizadoEm` | LocalDateTime | Auditoria |

#### Despesa

Campos planejados:

| Campo | Tipo | Observacao |
| --- | --- | --- |
| `id` | Long | Identificador |
| `descricao` | String | Obrigatorio |
| `categoria` | Enum | `MATERIAL`, `SALARIO`, `ALUGUEL`, `MANUTENCAO`, `IMPOSTO`, `OUTROS` |
| `valor` | BigDecimal | Maior que zero |
| `status` | Enum | `PENDENTE`, `PAGO`, `CANCELADO` |
| `dataVencimento` | LocalDate | Opcional |
| `dataPagamento` | LocalDate | Obrigatoria quando `PAGO` |
| `ativo` | boolean | Inativacao logica |
| `criadoEm` | LocalDateTime | Auditoria |
| `atualizadoEm` | LocalDateTime | Auditoria |

### Regras de negocio

- Receita vinculada a atendimento deve validar existencia no `servico-atendimento`.
- Receita de atendimento deve preferencialmente estar vinculada a atendimento `REALIZADO`.
- Nao permitir duas receitas ativas para o mesmo atendimento, salvo se definirmos pagamentos parciais futuramente.
- Valor de receita e despesa deve ser maior que zero.
- Status `CANCELADO` nao volta para `PENDENTE` ou `PAGO`.
- Status `PAGO` exige `dataPagamento`.
- Relatorio considera receitas/despesas ativas e nao canceladas.
- Periodo do relatorio exige `dataInicio <= dataFim`.

### Endpoints planejados

| Metodo | Rota | Perfis | Uso |
| --- | --- | --- | --- |
| `GET` | `/financeiro/health` | Publico | Health check |
| `POST` | `/receitas` | `GERENTE`, `ATENDENTE` | Registrar receita |
| `GET` | `/receitas` | `GERENTE`, `ATENDENTE` | Listar receitas com filtros |
| `GET` | `/receitas/{id}` | `GERENTE`, `ATENDENTE` | Buscar receita |
| `PATCH` | `/receitas/{id}/status` | `GERENTE`, `ATENDENTE` | Atualizar status |
| `DELETE` | `/receitas/{id}` | `GERENTE` | Cancelar/inativar receita |
| `POST` | `/despesas` | `GERENTE` | Registrar despesa |
| `GET` | `/despesas` | `GERENTE` | Listar despesas com filtros |
| `GET` | `/despesas/{id}` | `GERENTE` | Buscar despesa |
| `PATCH` | `/despesas/{id}/status` | `GERENTE` | Atualizar status |
| `DELETE` | `/despesas/{id}` | `GERENTE` | Cancelar/inativar despesa |
| `GET` | `/relatorios/financeiro` | `GERENTE` | Relatorio por periodo |

### Filtros planejados

`GET /receitas`:

- `atendimentoId`
- `pacienteId`
- `profissionalId`
- `status`
- `dataInicio`
- `dataFim`

`GET /despesas`:

- `categoria`
- `status`
- `dataInicio`
- `dataFim`

### Relatorio financeiro

`GET /relatorios/financeiro?dataInicio=2026-07-01&dataFim=2026-07-31`

Resposta planejada:

```json
{
  "dataInicio": "2026-07-01",
  "dataFim": "2026-07-31",
  "totalReceitas": 1500.00,
  "totalDespesas": 650.00,
  "saldo": 850.00,
  "receitasPorFormaPagamento": {
    "PIX": 900.00,
    "CARTAO_CREDITO": 600.00
  },
  "despesasPorCategoria": {
    "MATERIAL": 300.00,
    "ALUGUEL": 350.00
  }
}
```

## Integracoes HTTP

### servico-financeiro -> servico-atendimento

O financeiro deve validar atendimento antes de registrar receita.

Endpoint usado:

```http
GET /atendimentos/{id}
Authorization: Bearer <token>
```

Dados necessarios do atendimento:

- `id`
- `pacienteId`
- `profissionalId`
- `status`
- `valor`, se existir no atendimento realizado

Regra inicial:

- Permitir receita apenas para atendimento existente.
- Recomendar atendimento `REALIZADO` para receita paga.
- Se o atendimento estiver `CANCELADO`, bloquear receita.

### frontend -> novos servicos

O `frontend-autenticacao` deve continuar usando proxy `/api/` do Nginx. Na Sprint 4, o Nginx precisara rotear tambem:

| Prefixo | Destino Docker |
| --- | --- |
| `/api/materiais` | `http://servico-estoque:8085/materiais` |
| `/api/estoque` | `http://servico-estoque:8085/estoque` |
| `/api/receitas` | `http://servico-financeiro:8086/receitas` |
| `/api/despesas` | `http://servico-financeiro:8086/despesas` |
| `/api/relatorios` | `http://servico-financeiro:8086/relatorios` |

## Controle de acesso

| Perfil | Estoque | Financeiro |
| --- | --- | --- |
| `GERENTE` | Acesso total | Acesso total |
| `ATENDENTE` | Consulta materiais | Registra e consulta receitas |
| `DENTISTA` | Consulta materiais | Sem acesso financeiro geral |
| `AUXILIAR` | Gerencia materiais e movimentacoes | Sem acesso financeiro |

## Bancos e variaveis de ambiente

### Estoque

| Variavel | Uso |
| --- | --- |
| `SPRING_DATASOURCE_ESTOQUE_URL` | URL JDBC do banco de estoque |
| `SPRING_DATASOURCE_ESTOQUE_USERNAME` | Usuario do banco |
| `SPRING_DATASOURCE_ESTOQUE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave para validar JWT |

### Financeiro

| Variavel | Uso |
| --- | --- |
| `SPRING_DATASOURCE_FINANCEIRO_URL` | URL JDBC do banco financeiro |
| `SPRING_DATASOURCE_FINANCEIRO_USERNAME` | Usuario do banco |
| `SPRING_DATASOURCE_FINANCEIRO_PASSWORD` | Senha do banco |
| `SERVICO_ATENDIMENTO_URL` | Base URL do `servico-atendimento` |
| `JWT_SECRET` | Chave para validar JWT |

## Testes planejados

### servico-estoque

- `contextLoads`.
- Service: cadastro, atualizacao, inativacao e baixo estoque.
- Service: entrada, saida, ajuste e bloqueio de saldo negativo.
- Repository: filtros e historico.
- Controller: status HTTP, validacao e autorizacao.
- Integracao: fluxo cadastro -> entrada -> saida -> alerta.

### servico-financeiro

- `contextLoads`.
- Service: receita, despesa, status e relatorio.
- Service: bloqueio de receita para atendimento cancelado.
- Repository: filtros por periodo/status/categoria.
- Controller: status HTTP, validacao e autorizacao.
- Integracao: receita vinculada a atendimento mockado -> relatorio.

### Sistema completo

- `mvn clean test` pela raiz.
- `docker compose config --quiet`.
- `docker compose up --build` para validacao manual final.
- Fluxos Postman: login, paciente, profissional, atendimento, estoque e financeiro.

## Ordem dos blocos da Sprint 4

1. Planejamento tecnico.
2. Base do `servico-estoque`.
3. Dominio de materiais e alertas.
4. Movimentacoes de estoque.
5. Base do `servico-financeiro`.
6. Receitas, despesas e relatorio financeiro.
7. Frontend de estoque e financeiro.
8. Testes finais, Docker e documentacao de entrega.

## Decisoes para evitar excesso de escopo

- Nao implementar pagamento parcial nesta sprint.
- Nao automatizar baixa de estoque por procedimento nesta sprint.
- Nao implementar dashboard grafico complexo.
- Nao criar gateway dedicado; manter proxy Nginx simples do frontend.
- Nao acoplar financeiro diretamente ao banco de atendimento; integracao sera via HTTP/REST.
