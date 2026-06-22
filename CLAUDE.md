# CLAUDE.md - Instrucoes de Desenvolvimento

Sistema de Gestao para Clinica Odontologica.

Leia este arquivo completamente antes de gerar qualquer codigo.

## Contexto do Projeto

Sistema de gestao para clinica odontologica construido com arquitetura de microsservicos.
Cada servico de dominio e independente, tem seu proprio banco de dados e roda em container Docker.
A comunicacao entre servicos ocorre via HTTP/REST com autenticacao JWT.

A infraestrutura de microsservicos usa:

- `config-server`: centraliza configuracoes via Spring Cloud Config.
- `discovery-server`: registra e descobre servicos via Eureka.
- `servico-autenticacao`: autentica usuarios, gera JWT e registra-se no Eureka.
- `frontend-autenticacao`: interface web do fluxo de autenticacao.
- `mysql`: banco usado pelo servico de autenticacao.

## Estrutura Atual do Repositorio

```text
clinica-backend/
|-- .github/workflows/
|   |-- autenticacao-ci.yml
|   |-- config-server-ci.yml
|   `-- discovery-server-ci.yml
|-- config-server/
|   |-- Dockerfile
|   `-- config-server/
|       |-- pom.xml
|       `-- src/
|-- discovery-server/
|   |-- Dockerfile
|   `-- discovery-server/
|       |-- pom.xml
|       `-- src/
|-- frontend-autenticacao/
|   |-- Dockerfile
|   |-- index.html
|   |-- app.js
|   |-- styles.css
|   `-- nginx.conf
|-- servico-autenticacao/
|   |-- Dockerfile
|   `-- servico-autenticacao/
|       |-- pom.xml
|       `-- src/
|-- docs/
|-- docker-compose.yml
|-- pom.xml
`-- README.md
```

Observacao: os projetos Spring Boot estao em pastas internas duplicadas, por exemplo `config-server/config-server` e `discovery-server/discovery-server`. Respeite essa estrutura ao criar comandos, paths de CI e Dockerfiles.

## Servicos e Portas

| Componente | Porta | Funcao |
| --- | ---: | --- |
| `config-server` | 8888 | Spring Cloud Config Server |
| `discovery-server` | 8761 | Eureka Server |
| `servico-autenticacao` | 8081 | API de autenticacao e JWT |
| `frontend-autenticacao` | 3000 | Frontend servido por Nginx |
| `mysql` | 3307:3306 | Banco `clinica_auth` |

Servicos de dominio previstos:

| Servico | Porta | Banco |
| --- | ---: | --- |
| `servico-autenticacao` | 8081 | `clinica_auth` |
| `servico-profissional` | 8082 | `clinica_profissional` |
| `servico-paciente` | 8083 | `clinica_paciente` |

## Ordem de Inicializacao Local

No Docker Compose, a ordem correta e:

1. `mysql`
2. `config-server`
3. `discovery-server`
4. `servico-autenticacao`
5. `frontend-autenticacao`

O `servico-autenticacao` deve importar configuracoes do Config Server e registrar-se no Eureka:

```yaml
spring:
  config:
    import: optional:configserver:http://localhost:8888

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

Em containers, use os nomes dos servicos Docker:

```text
SPRING_CONFIG_IMPORT=configserver:http://config-server:8888
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-server:8761/eureka/
```

## Variaveis de Ambiente

Nunca hardcode credenciais. Use `.env`, variaveis do ambiente local ou secrets no CI.

Variaveis usadas atualmente:

- `GIT_USERNAME`: usuario para acessar o repositorio remoto de configuracoes.
- `GIT_TOKEN`: token para acessar o repositorio remoto de configuracoes.
- `ADMIN_SENHA_GERENTE`: senha inicial do gerente.
- `ADMIN_SENHA_ATENDENTE`: senha inicial do atendente.
- `ADMIN_SENHA_DENTISTA`: senha inicial do dentista.
- `ADMIN_SENHA_AUXILIAR`: senha inicial do auxiliar.

## Perfis de Usuario (RBAC)

```text
GERENTE | ATENDENTE | DENTISTA | AUXILIAR
```

Use `@PreAuthorize` nos controllers para proteger endpoints por perfil.

## Estrutura de Pacotes dos Servicos de Dominio

Todo servico de dominio deve seguir esta estrutura:

```text
com.clinica.servico_<nome>/
|-- controller/       -> classes @RestController
|-- service/          -> classes @Service com regras de negocio
|-- repository/       -> interfaces que estendem JpaRepository
|-- model/            -> classes @Entity e enums
|-- dto/              -> objetos Request e Response
|-- security/         -> JwtFilter, JwtUtil, SecurityConfig
`-- exception/        -> excecoes customizadas e handler global
```

Regras:

- NUNCA coloque logica de negocio no controller; ela pertence ao service.
- NUNCA acesse repository diretamente no controller; sempre use service.
- NUNCA retorne entidade JPA em endpoint; sempre retorne DTO de resposta.
- NUNCA coloque senha em DTO de resposta.
- SEMPRE use injecao por construtor com `@RequiredArgsConstructor`.

## Estrutura dos Servicos de Infraestrutura

### Config Server

Localizacao:

```text
config-server/config-server
```

Pacote base:

```text
com.clinica.config_server
```

Responsabilidades:

- Expor configuracoes centralizadas na porta `8888`.
- Usar `@EnableConfigServer`.
- Buscar configuracoes no repositorio Git definido por `spring.cloud.config.server.git.uri`.
- Expor health check em `/actuator/health`.

Regras:

- Nao adicione regras de negocio no Config Server.
- Nao adicione controllers de dominio no Config Server.
- Mantenha credenciais do Git em variaveis de ambiente.

### Discovery Server

Localizacao:

```text
discovery-server/discovery-server
```

Pacote base:

```text
com.clinica.discovery_server
```

Responsabilidades:

- Expor Eureka Server na porta `8761`.
- Usar `@EnableEurekaServer`.
- Nao registrar a si proprio no Eureka.
- Expor health check em `/actuator/health`.

Configuracao esperada:

```yaml
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

Regras:

- Nao adicione regras de negocio no Discovery Server.
- Nao adicione banco de dados no Discovery Server.
- Nao proteja a dashboard do Eureka com JWT sem definir antes uma estrategia clara de operacao.

## Nomenclatura

### Classes

| Tipo | Padrao | Exemplo |
| --- | --- | --- |
| Controller | `{Entidade}Controller` | `ProfissionalController` |
| Service | `{Entidade}Service` | `ProfissionalService` |
| Repository | `{Entidade}Repository` | `ProfissionalRepository` |
| Entidade JPA | `{Entidade}` | `Profissional` |
| DTO entrada | `{Entidade}Request` | `ProfissionalRequest` |
| DTO saida | `{Entidade}Response` | `ProfissionalResponse` |
| Excecao | `{Motivo}Exception` | `RecursoNaoEncontradoException` |

### Metodos no Service

| Operacao | Nome |
| --- | --- |
| Criar | `cadastrar(Request dto)` |
| Buscar por ID | `buscarPorId(Long id)` |
| Listar ativos | `listarAtivos()` |
| Atualizar | `atualizar(Long id, Request dto)` |
| Inativar | `inativar(Long id)` |

### Metodos no Controller

| HTTP | Padrao |
| --- | --- |
| `POST /` | `cadastrar(@RequestBody Request dto)` |
| `GET /` | `listar()` |
| `GET /{id}` | `buscarPorId(@PathVariable Long id)` |
| `PUT /{id}` | `atualizar(@PathVariable Long id, @RequestBody Request dto)` |
| `PATCH /{id}/inativar` | `inativar(@PathVariable Long id)` |

### Variaveis e Campos

- Use `camelCase` para variaveis e metodos: `nomeCompleto`, `dataNascimento`.
- Use `UPPER_SNAKE_CASE` para constantes: `EXPIRACAO_TOKEN`.
- Use portugues para nomes de dominio: `paciente`, `profissional`, `atendimento`.
- Use ingles para nomes tecnicos: `repository`, `service`, `controller`, `handler`.
- Campos de data: `LocalDate` para datas e `LocalDateTime` para data e hora.

## Padrao de Resposta da API

Resposta de sucesso: retorno direto do DTO.

```json
{
  "id": 1,
  "nome": "Dr. Joao Silva",
  "email": "joao@clinica.com",
  "especialidade": "Ortodontia",
  "role": "DENTISTA",
  "ativo": true
}
```

Resposta de erro: sempre use este formato.

```json
{
  "status": 404,
  "erro": "Recurso nao encontrado",
  "mensagem": "Profissional com id 99 nao encontrado",
  "caminho": "/profissionais/99"
}
```

Classe obrigatoria:

```java
@Data
@AllArgsConstructor
public class ErroResponse {
    private int status;
    private String erro;
    private String mensagem;
    private String caminho;
}
```

## Tratamento de Excecoes

Excecoes customizadas obrigatorias em cada servico de dominio:

```java
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}

public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
```

Handler global obrigatorio:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return ResponseEntity.status(404).body(
            new ErroResponse(404, "Recurso nao encontrado", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraDeNegocio(
            RegraDeNegocioException ex, HttpServletRequest request) {
        return ResponseEntity.status(400).body(
            new ErroResponse(400, "Dados invalidos", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleErroGenerico(
            Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(500).body(
            new ErroResponse(500, "Erro interno", "Ocorreu um erro inesperado", request.getRequestURI())
        );
    }
}
```

## Seguranca - JWT

O `JwtFilter` e `JwtUtil` devem ser consistentes entre os servicos protegidos.
Use o `servico-autenticacao` como referencia.

SecurityConfig padrao para servicos protegidos:

```java
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"status\":401,\"erro\":\"Token ausente ou invalido\"," +
                        "\"mensagem\":\"Informe um token JWT valido no cabecalho Authorization\"," +
                        "\"caminho\":\"" + request.getRequestURI() + "\"}"
                    );
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/*/health", "/error", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

Como proteger rotas por perfil:

```java
@PreAuthorize("hasRole('GERENTE')")
@PostMapping
public ResponseEntity<ProfissionalResponse> cadastrar(@RequestBody ProfissionalRequest dto) { }

@PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
@GetMapping
public ResponseEntity<List<PacienteResponse>> listar() { }
```

## Padrao de Entidade

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "profissionais")
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;
    private String especialidade;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean ativo = true;

    private LocalDateTime criadoEm = LocalDateTime.now();
}
```

Regras:

- Toda entidade tem `ativo` com padrao `true`.
- Toda entidade tem `criadoEm` como `LocalDateTime`.
- Nomes de tabela ficam no plural e em portugues: `profissionais`, `pacientes`.
- Nunca delete registro de dominio; inative com `ativo = false`.

## Padrao de DTO

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalRequest {
    private String nome;
    private String email;
    private String especialidade;
    private Role role;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalResponse {
    private Long id;
    private String nome;
    private String email;
    private String especialidade;
    private Role role;
    private boolean ativo;
}
```

## Testes Automatizados

Todo modulo Spring Boot deve ter testes automatizados em:

```text
<modulo>/src/test/java
<modulo>/src/test/resources
```

Testes existentes:

- `servico-autenticacao/servico-autenticacao/src/test/java/com/clinica/servico_autenticacao/ServicoAutenticacaoApplicationTests.java`
- `config-server/config-server/src/test/java/com/clinica/config_server/ConfigServerApplicationTests.java`
- `discovery-server/discovery-server/src/test/java/com/clinica/discovery_server/DiscoveryServerApplicationTests.java`

Comandos por modulo:

```powershell
cd servico-autenticacao/servico-autenticacao
./mvnw.cmd clean test

cd ../../config-server/config-server
./mvnw.cmd clean test

cd ../../discovery-server/discovery-server
./mvnw.cmd clean test
```

Tambem e valido usar Maven instalado:

```powershell
mvn clean test
```

Regras para testes:

- SEMPRE rode `mvn clean test` no modulo alterado antes de finalizar uma tarefa.
- Se alterar contrato entre servicos, rode os testes de todos os modulos afetados.
- Se criar um novo modulo Spring Boot, adicione ao menos um teste `contextLoads`.
- Para services de dominio, teste regras de negocio com JUnit e Mockito.
- Para controllers, teste status HTTP, DTOs e autorizacao com Spring MVC Test quando aplicavel.
- Para repository, use teste focado com banco em memoria ou ambiente de teste isolado.
- Nao dependa de Config Server, Eureka ou MySQL real em teste unitario.
- Use `src/test/resources/application.properties` ou `application.yml` para sobrescrever configuracoes externas durante testes.

### CI GitHub Actions

O repositorio possui pipelines separados:

- `.github/workflows/autenticacao-ci.yml`
- `.github/workflows/config-server-ci.yml`
- `.github/workflows/discovery-server-ci.yml`

Cada pipeline:

- usa JDK 21;
- roda `mvn clean test`;
- executa apenas quando ha mudancas no caminho do modulo correspondente.

Ao adicionar novo servico, crie tambem um workflow proprio seguindo o mesmo padrao.

## Regras Gerais

- NUNCA use `System.out.println`; use `log.info()` ou `log.error()` com `@Slf4j`.
- NUNCA faca `.get()` em `Optional`; use `.orElseThrow()`.
- NUNCA retorne entidade JPA diretamente.
- NUNCA coloque senha em DTO de resposta.
- NUNCA hardcode credenciais.
- SEMPRE inative registros com `ativo = false`.
- SEMPRE retorne `201 Created` para POST bem-sucedido.
- SEMPRE retorne `204 No Content` para inativacao bem-sucedida.
- SEMPRE use `@RequiredArgsConstructor` e injecao via construtor.
- SEMPRE exponha health check em servicos Dockerizados.
- SEMPRE mantenha paths de Docker, Maven e CI coerentes com as pastas internas duplicadas.

## Como usar este arquivo no Claude.ai

1. Crie um novo projeto em `claude.ai/projects`.
2. Nas instrucoes do projeto, cole o conteudo completo deste `CLAUDE.md`.
3. Adicione como contexto os arquivos do servico especifico.
4. Ao pedir codigo, informe o modulo afetado e peca para executar ou considerar os testes automatizados.

Exemplo de prompt eficaz:

```text
Implemente o PacienteService com os metodos cadastrar, buscarPorId,
listarAtivos, atualizar e inativar, seguindo os padroes do projeto.
Inclua testes automatizados do service.
```

Exemplo de prompt ruim:

```text
Faz um service de paciente pra mim.
```

Versao 1.1 - Sprint 2 - Clinica Odontologica
