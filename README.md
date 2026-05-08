# Clinica Docker - Sprint 1

Sistema distribuido para gerenciamento de clinicas odontologicas, baseado em microservicos com Java, Spring Boot, Docker e MySQL.

## Objetivo da Sprint 1

- Configurar ambiente com Docker Compose
- Implementar dois microservicos iniciais:
  - `servico-autenticacao`
  - `servico-paciente`
- Garantir funcionamento basico via API REST
- Disponibilizar telas simples de cadastro/login no servico de autenticacao

---

## Tecnologias Utilizadas

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Validation
- Thymeleaf (telas no servico de autenticacao)
- MySQL 8
- Docker
- Docker Compose
- Maven

---

## Arquitetura da Solucao

A aplicacao esta dividida em 3 containers:

1. **mysql**
   - Banco de dados relacional da sprint
   - Porta externa: `3306`

2. **auth-service**
   - Cadastro e login de usuarios
   - API REST + telas web
   - Porta externa: `8081`

3. **paciente-service**
   - CRUD de pacientes via API REST
   - Porta externa: `8082`

Comunicacao com banco:
- Os microservicos se conectam ao MySQL pelo hostname Docker `mysql`.
- Configuracao JPA com `spring.jpa.hibernate.ddl-auto=update`.

---

## Estrutura de Pastas

```text
clinica-docker/
├── docker-compose.yml
├── servico-autenticacao/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/clinica/autenticacao/
│       │   ├── ServicoAutenticacaoApplication.java
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   └── AuthPageController.java
│       │   ├── dto/
│       │   │   ├── CadastroUsuarioRequest.java
│       │   │   └── LoginRequest.java
│       │   ├── model/Usuario.java
│       │   └── repository/UsuarioRepository.java
│       └── resources/
│           ├── application.properties
│           └── templates/
│               ├── auth-register.html
│               ├── auth-login.html
│               └── auth-home.html
└── servico-paciente/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/
        ├── java/com/clinica/paciente/
        │   ├── ServicoPacienteApplication.java
        │   ├── controller/PacienteController.java
        │   ├── model/Paciente.java
        │   └── repository/PacienteRepository.java
        └── resources/application.properties
