# API - Servico de Estoque

Servico responsavel pelo cadastro de materiais, controle de saldo e registro de movimentacoes de estoque.

## Base URL

```text
http://localhost:8085
```

No frontend via Nginx:

```text
/api/materiais
/api/estoque
```

Todas as rotas, exceto health check, exigem JWT no header:

```http
Authorization: Bearer <token>
```

## Perfis

| Operacao | Perfis |
| --- | --- |
| Listar e consultar materiais | GERENTE, ATENDENTE, DENTISTA, AUXILIAR |
| Cadastrar e atualizar materiais | GERENTE, AUXILIAR |
| Registrar e listar movimentacoes | GERENTE, AUXILIAR |
| Inativar material | GERENTE |
| Alertas de baixo estoque | GERENTE, AUXILIAR |

## Enums

Unidade de medida:

```text
UNIDADE | CAIXA | PACOTE | ML | MG | G
```

Tipo de movimentacao:

```text
ENTRADA | SAIDA | AJUSTE
```

## Endpoints

### Health check

```http
GET /estoque/health
```

### Listar materiais

```http
GET /materiais?busca=luva&categoria=descartavel&baixoEstoque=true&ativo=true
```

Filtros opcionais:

| Parametro | Tipo | Descricao |
| --- | --- | --- |
| `busca` | string | Busca por nome/descricao |
| `categoria` | string | Filtra por categoria |
| `baixoEstoque` | boolean | Filtra materiais abaixo do minimo |
| `ativo` | boolean | Filtra ativos/inativos |

### Buscar material por ID

```http
GET /materiais/{id}
```

### Cadastrar material

```http
POST /materiais
Content-Type: application/json
```

```json
{
  "nome": "Luva descartavel",
  "descricao": "Caixa com 100 unidades",
  "categoria": "Descartaveis",
  "unidadeMedida": "CAIXA",
  "quantidadeAtual": 10,
  "quantidadeMinima": 3
}
```

Retorna `201 Created` com o material criado.

### Atualizar material

```http
PUT /materiais/{id}
Content-Type: application/json
```

Usa o mesmo corpo do cadastro.

### Inativar material

```http
PATCH /materiais/{id}/inativar
```

Retorna `204 No Content`.

### Listar alertas de baixo estoque

```http
GET /materiais/alertas/baixo-estoque
```

### Registrar movimentacao

```http
POST /materiais/{id}/movimentacoes
Content-Type: application/json
```

```json
{
  "tipo": "SAIDA",
  "quantidade": 2,
  "motivo": "Uso em atendimento"
}
```

Regras principais:

- `ENTRADA` soma a quantidade ao saldo.
- `SAIDA` reduz a quantidade e nao permite saldo negativo.
- `AJUSTE` define o saldo para a quantidade informada e exige motivo.

### Listar movimentacoes de um material

```http
GET /materiais/{id}/movimentacoes
```