# API - Servico Financeiro

Servico responsavel por receitas vinculadas a atendimentos, despesas e relatorio financeiro por periodo.

## Base URL

```text
http://localhost:8086
```

No frontend via Nginx:

```text
/api/receitas
/api/despesas
/api/relatorios
/api/financeiro
```

Todas as rotas, exceto health check, exigem JWT no header:

```http
Authorization: Bearer <token>
```

## Integracao

Ao cadastrar uma receita, o servico financeiro consulta o `servico-atendimento` para validar o atendimento informado. A chamada repassa o mesmo token JWT recebido na requisicao.

## Perfis

| Operacao | Perfis |
| --- | --- |
| Cadastrar, listar e atualizar receitas | GERENTE, ATENDENTE |
| Inativar receitas | GERENTE |
| Cadastrar, listar, atualizar e inativar despesas | GERENTE |
| Gerar relatorio financeiro | GERENTE |

## Enums

Forma de pagamento:

```text
DINHEIRO | PIX | CARTAO_CREDITO | CARTAO_DEBITO | CONVENIO
```

Categoria de despesa:

```text
MATERIAL | SALARIO | ALUGUEL | MANUTENCAO | IMPOSTO | OUTROS
```

Status financeiro:

```text
PENDENTE | PAGO | CANCELADO
```

## Endpoints

### Health check

```http
GET /financeiro/health
```

## Receitas

### Cadastrar receita

```http
POST /receitas
Content-Type: application/json
```

```json
{
  "atendimentoId": 10,
  "descricao": "Pagamento de atendimento",
  "valor": 250.00,
  "formaPagamento": "PIX",
  "status": "PAGO",
  "dataVencimento": "2026-07-10",
  "dataPagamento": "2026-07-01"
}
```

Retorna `201 Created` com a receita criada.

### Listar receitas

```http
GET /receitas?atendimentoId=10&pacienteId=4&profissionalId=2&status=PAGO&dataInicio=2026-07-01&dataFim=2026-07-31
```

Todos os filtros sao opcionais.

### Buscar receita por ID

```http
GET /receitas/{id}
```

### Atualizar status da receita

```http
PATCH /receitas/{id}/status
Content-Type: application/json
```

```json
{
  "status": "PAGO",
  "dataPagamento": "2026-07-01"
}
```

### Inativar receita

```http
DELETE /receitas/{id}
```

Retorna `204 No Content`.

## Despesas

### Cadastrar despesa

```http
POST /despesas
Content-Type: application/json
```

```json
{
  "descricao": "Compra de material odontologico",
  "categoria": "MATERIAL",
  "valor": 180.00,
  "status": "PENDENTE",
  "dataVencimento": "2026-07-20",
  "dataPagamento": null
}
```

Retorna `201 Created` com a despesa criada.

### Listar despesas

```http
GET /despesas?categoria=MATERIAL&status=PENDENTE&dataInicio=2026-07-01&dataFim=2026-07-31
```

Todos os filtros sao opcionais.

### Buscar despesa por ID

```http
GET /despesas/{id}
```

### Atualizar status da despesa

```http
PATCH /despesas/{id}/status
Content-Type: application/json
```

```json
{
  "status": "PAGO",
  "dataPagamento": "2026-07-05"
}
```

### Inativar despesa

```http
DELETE /despesas/{id}
```

Retorna `204 No Content`.

## Relatorio financeiro

```http
GET /relatorios/financeiro?dataInicio=2026-07-01&dataFim=2026-07-31
```

Resposta resumida:

```json
{
  "dataInicio": "2026-07-01",
  "dataFim": "2026-07-31",
  "totalReceitas": 1200.00,
  "totalDespesas": 450.00,
  "saldo": 750.00,
  "receitasPorFormaPagamento": {
    "PIX": 900.00,
    "DINHEIRO": 300.00
  },
  "despesasPorCategoria": {
    "MATERIAL": 450.00
  }
}
```