# Servico de Atendimento - API

Base local:

```text
http://localhost:8084
```

Todos os endpoints, exceto health, exigem:

```http
Authorization: Bearer <token>
Content-Type: application/json
```

## Perfis

| Perfil | Permissao |
| --- | --- |
| `GERENTE` | Cria, lista, busca, atualiza status, realiza e cancela qualquer atendimento |
| `ATENDENTE` | Cria, lista, busca, atualiza status, realiza e cancela qualquer atendimento |
| `DENTISTA` | Lista, busca, realiza, cancela e atualiza status apenas dos proprios atendimentos |

## Health

```http
GET /atendimentos/health
```

Resposta:

```json
{
  "status": "UP",
  "service": "servico-atendimento"
}
```

## Criar atendimento

```http
POST /atendimentos
```

Perfis: `GERENTE`, `ATENDENTE`

Corpo:

```json
{
  "pacienteId": 1,
  "profissionalId": 2,
  "data": "2099-01-10",
  "hora": "09:00",
  "duracaoMinutos": 60,
  "observacoes": "Consulta inicial"
}
```

Regras:

- O paciente precisa existir e estar ativo no `servico-paciente`.
- O profissional precisa existir, estar ativo e ter perfil `DENTISTA` no `servico-profissional`.
- `duracaoMinutos` e opcional; quando nao informado, o sistema usa 60 minutos.
- A duracao aceita vai de 15 a 480 minutos.
- O atendimento deve terminar no mesmo dia.
- O paciente nao pode ter outro atendimento `AGENDADO` ou `CONFIRMADO` que sobreponha o mesmo intervalo de horario.
- O profissional nao pode ter outro atendimento `AGENDADO` ou `CONFIRMADO` que sobreponha o mesmo intervalo de horario.

Resposta: `201 Created`

## Listar atendimentos

```http
GET /atendimentos
```

Perfis: `GERENTE`, `ATENDENTE`, `DENTISTA`

Filtros opcionais:

| Query param | Exemplo |
| --- | --- |
| `pacienteId` | `/atendimentos?pacienteId=1` |
| `profissionalId` | `/atendimentos?profissionalId=2` |
| `data` | `/atendimentos?data=2099-01-10` |
| `status` | `/atendimentos?status=AGENDADO` |

Observacao: quando o usuario logado for `DENTISTA`, o filtro de profissional e forcado para o proprio profissional autenticado.

## Buscar por ID

```http
GET /atendimentos/{id}
```

Perfis: `GERENTE`, `ATENDENTE`, `DENTISTA`

Dentista so acessa atendimentos vinculados ao seu proprio profissional.

## Atualizar status

```http
PATCH /atendimentos/{id}/status
```

Perfis: `GERENTE`, `ATENDENTE`, `DENTISTA`

Corpo:

```json
{
  "status": "CONFIRMADO"
}
```

Status aceitos:

```text
AGENDADO | CONFIRMADO | REALIZADO | CANCELADO | NAO_COMPARECEU
```

Regras:

- Atendimento `CANCELADO` nao volta para outro status.
- Atendimento `REALIZADO` nao volta para outro status.
- Ao mudar para `REALIZADO`, o campo `realizadoEm` e preenchido.

## Realizar atendimento

```http
PATCH /atendimentos/{id}/realizar
```

Perfis: `GERENTE`, `ATENDENTE`, `DENTISTA`

Corpo:

```json
{
  "procedimentoRealizado": "Profilaxia",
  "observacoes": "Paciente sem queixas",
  "valor": 150.00
}
```

Regras:

- Atendimento `CANCELADO` nao pode ser realizado.
- Atendimento ja `REALIZADO` nao pode ser realizado novamente.
- O status muda para `REALIZADO`.
- O campo `realizadoEm` e preenchido.

## Cancelar atendimento

```http
PATCH /atendimentos/{id}/cancelar
```

Perfis: `GERENTE`, `ATENDENTE`, `DENTISTA`

Regras:

- Atendimento `REALIZADO` nao pode ser cancelado.
- Atendimento ja `CANCELADO` nao pode ser cancelado novamente.

## Padrao de resposta

Resposta de atendimento:

```json
{
  "id": 10,
  "pacienteId": 1,
  "pacienteNome": "Maria Silva",
  "profissionalId": 2,
  "profissionalNome": "Dr Joao",
  "profissionalEmail": "joao@clinica.com",
  "data": "2099-01-10",
  "hora": "09:00:00",
  "duracaoMinutos": 60,
  "status": "AGENDADO",
  "observacoes": "Consulta inicial",
  "procedimentoRealizado": null,
  "valor": null,
  "ativo": true,
  "criadoEm": "2026-06-29T10:00:00",
  "atualizadoEm": null,
  "realizadoEm": null
}
```

Resposta de erro:

```json
{
  "status": 400,
  "erro": "Regra de negocio violada",
  "mensagem": "Profissional ja possui atendimento nesse intervalo de horario",
  "caminho": "/atendimentos"
}
```

## Variaveis de ambiente

| Variavel | Uso |
| --- | --- |
| `SPRING_DATASOURCE_ATENDIMENTO_URL` | URL JDBC do banco de atendimento |
| `SPRING_DATASOURCE_ATENDIMENTO_USERNAME` | Usuario do banco |
| `SPRING_DATASOURCE_ATENDIMENTO_PASSWORD` | Senha do banco |
| `SERVICO_PACIENTE_URL` | Base URL do `servico-paciente` |
| `SERVICO_PROFISSIONAL_URL` | Base URL do `servico-profissional` |
| `JWT_SECRET` | Chave para validar JWT |
