# Clinica Odontologica - Backend

Sistema de gestao para clinica odontologica usando arquitetura de microsservicos com Spring Boot, MySQL, Docker Compose, Eureka, Config Server e frontend estatico.

## Modulos

| Modulo | Porta | Responsabilidade |
| --- | ---: | --- |
| `config-server` | 8888 | Centraliza configuracoes via Spring Cloud Config |
| `discovery-server` | 8761 | Service discovery com Eureka |
| `servico-autenticacao` | 8081 | Login, setup inicial e emissao de JWT |
| `servico-profissional` | 8082 | Cadastro, consulta e filtros de profissionais |
| `servico-paciente` | 8083 | Cadastro, consulta e filtros de pacientes |
| `servico-atendimento` | 8084 | Agendamento, listagem, realizacao e cancelamento de atendimentos |
| `frontend-autenticacao` | 3000 | Frontend estatico servido por Nginx |

## Como subir localmente

Crie um arquivo `.env` na raiz com as variaveis necessarias:

```env
GIT_USERNAME=seu_usuario
GIT_TOKEN=seu_token
JWT_SECRET=desenvolvimento-local-chave-minima-32-chars
ADMIN_SENHA_GERENTE=senha
ADMIN_SENHA_ATENDENTE=senha
ADMIN_SENHA_DENTISTA=senha
ADMIN_SENHA_AUXILIAR=senha
```

Suba o ambiente:

```powershell
docker compose up --build
```

Acesse:

- Frontend: http://localhost:3000
- Eureka: http://localhost:8761
- Auth health: http://localhost:8081/auth/health
- Atendimento health: http://localhost:8084/atendimentos/health

## Ordem esperada dos servicos

1. Bancos MySQL
2. `config-server`
3. `discovery-server`
4. `servico-autenticacao`
5. `servico-profissional`
6. `servico-paciente`
7. `servico-atendimento`
8. `frontend-autenticacao`

## Testes

Rodar todos os testes pela raiz:

```powershell
mvn clean test
```

Rodar apenas atendimento:

```powershell
mvn -q -pl servico-atendimento test
```

Rodar dentro do modulo:

```powershell
cd servico-atendimento
mvn -q test
```

## APIs principais

- Autenticacao: `POST /auth/login`
- Profissionais: `GET /profissionais`, `GET /profissionais/me`
- Pacientes: `GET /pacientes`
- Atendimentos: consulte [docs/servico-atendimento-api.md](docs/servico-atendimento-api.md)

## Seguranca

Os servicos de dominio usam JWT no header:

```http
Authorization: Bearer <token>
```

Perfis atuais:

```text
GERENTE | ATENDENTE | DENTISTA | AUXILIAR
```

No `servico-atendimento`, gerente e atendente podem gerenciar atendimentos de forma ampla. Dentista visualiza e altera apenas atendimentos vinculados ao seu proprio cadastro profissional.

## Documentacao e Postman

- Guia da API de atendimento: `docs/servico-atendimento-api.md`
- Colecao Postman de atendimento: `docs/servico-atendimento.postman_collection.json`
