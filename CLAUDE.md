# CLAUDE.md - Instrucoes de Desenvolvimento

Sistema de gestao para Clinica Odontologica.

Leia este arquivo completamente antes de gerar codigo para este repositorio.

## Contexto do Projeto

Este repositorio usa arquitetura de microsservicos com Spring Boot no backend, infraestrutura via Docker Compose e um frontend estatico para autenticacao.

A comunicacao entre servicos ocorre por HTTP/REST. O `servico-autenticacao` gera JWT, e os servicos protegidos devem validar esse token no header `Authorization: Bearer <token>`.

Componentes atuais:

- `config-server`: centraliza configuracoes via Spring Cloud Config.
- `discovery-server`: registra e descobre servicos via Eureka.
- `servico-autenticacao`: autentica usuarios, gera JWT e expoe rotas de setup/login/health.
- `servico-profissional`: servico de dominio para profissionais.
- `frontend-autenticacao`: frontend estatico servido por Nginx.
- `mysql`: banco relacional usado no ambiente Docker.

## Estrutura Atual do Repositorio

```text
clinica-backend/
|-- .github/workflows/
|   |-- autenticacao-ci.yml
|   |-- config-server-ci.yml
|   |-- discovery-server-ci.yml
|   `-- profissional-ci.yml
|-- config-server/
|   |-- Dockerfile
|   |-- pom.xml
|   `-- src/
|-- discovery-server/
|   |-- Dockerfile
|   |-- pom.xml
|   `-- src/
|-- frontend-autenticacao/
|   |-- Dockerfile
|   |-- index.html
|   |-- app.js
|   |-- styles.css
|   `-- nginx.conf
|-- servico-autenticacao/
|   |-- Dockerfile
|   |-- pom.xml
|   `-- src/
|-- servico-profissional/
|   |-- Dockerfile
|   |-- pom.xml
|   `-- src/
|-- tools/
|-- docs/
|-- docker-compose.yml
|-- pom.xml
`-- README.md
```

Observacao importante: os modulos Spring Boot atuais ficam diretamente nas pastas do modulo, por exemplo `servico-autenticacao/src`, `config-server/src` e `discovery-server/src`. Nao assuma pastas internas duplicadas como `servico-autenticacao/servico-autenticacao`.

## Servicos e Portas

| Componente | Porta | Funcao |
| --- | ---: | --- |
| `config-server` | 8888 | Spring Cloud Config Server |
| `discovery-server` | 8761 | Eureka Server |
| `servico-autenticacao` | 8081 | API de autenticacao e JWT |
| `servico-profissional` | 8082 | API de profissionais |
| `frontend-autenticacao` | 3000 | Frontend servido por Nginx |
| `mysql` | 3307:3306 | Banco MySQL do ambiente Docker |

## Ordem de Inicializacao Local

No Docker Compose, a ordem esperada e:

1. `mysql`
2. `config-server`
3. `discovery-server`
4. `servico-autenticacao`
5. `servico-profissional`
6. `frontend-autenticacao`

Comando principal:

```powershell
docker compose up --build
```

Depois de subir, o frontend fica em:

```text
http://localhost:3000
```

## Variaveis de Ambiente

Nunca hardcode credenciais em codigo. Use `.env`, variaveis de ambiente ou secrets do CI.

Variaveis usadas atualmente:

- `GIT_USERNAME`: usuario para acessar o repositorio remoto de configuracoes.
- `GIT_TOKEN`: token para acessar o repositorio remoto de configuracoes.
- `ADMIN_SENHA_GERENTE`: senha inicial do gerente.
- `ADMIN_SENHA_ATENDENTE`: senha inicial do atendente.
- `ADMIN_SENHA_DENTISTA`: senha inicial do dentista.
- `ADMIN_SENHA_AUXILIAR`: senha inicial do auxiliar.

## Frontend de Autenticacao

O frontend fica em `frontend-autenticacao/` e deve continuar simples, sem framework.

Arquivos:

```text
frontend-autenticacao/
|-- index.html      -> estrutura completa: login + dashboard
|-- styles.css      -> tema visual, layout responsivo e sidebar
|-- app.js          -> login, localStorage, decode JWT e roteamento interno
|-- nginx.conf      -> proxy /api/ para servico-autenticacao
`-- Dockerfile      -> imagem Nginx
```

Arquitetura atual do `index.html`:

```text
index.html
|-- #tela-login
|   `-- formulario de login
`-- #tela-dashboard
    |-- .sidebar
    |   |-- logo/menu
    |   `-- dados do usuario
    `-- #conteudo
        `-- area dinamica para paginas futuras
```

Regras do frontend:

- Nao usar React, Angular, Vue ou build step.
- Manter HTML, CSS e JavaScript puro.
- O token JWT deve ser salvo em `localStorage` usando a chave `clinica.auth.token`.
- O token nao deve ser exibido na interface.
- O perfil do usuario deve aparecer na sidebar.
- O frontend pode decodificar localmente o payload do JWT para ler `sub`, `role`, `id` e futuras claims como `nome`.
- O `nginx.conf` deve continuar fazendo proxy de `/api/` para `http://servico-autenticacao:8081/`.

Para adicionar uma nova pagina no dashboard:

1. Adicione um botao no menu com `data-page="nome-da-pagina"`.
2. Adicione um `<template id="pagina-nome-da-pagina">...</template>` no `index.html`.
3. Adicione o titulo correspondente em `pageTitles` no `app.js`.
4. Nao altere a estrutura geral de `#tela-dashboard`, `.sidebar` ou `#conteudo` sem necessidade.

## API de Autenticacao

Rotas publicas atuais do `servico-autenticacao`:

| Metodo | Rota | Uso |
| --- | --- | --- |
| `POST` | `/auth/login` | autentica usuario e retorna `{ "token": "..." }` |
| `GET` | `/auth/health` | health check |
| `POST` | `/auth/setup` | setup inicial do primeiro gerente |

Pelo frontend, as rotas sao chamadas com prefixo `/api` por causa do Nginx:

```text
POST /api/auth/login
GET /api/auth/health
POST /api/auth/setup
```

JWT atual:

- `sub`: email do usuario.
- `role`: perfil do usuario.
- `id`: id do usuario.
- `exp`: expiracao.

Se o frontend precisar mostrar o nome real do usuario sem chamar o backend, adicione a claim `nome` em `JwtUtil.gerarToken`.

## Perfis de Usuario

Perfis atuais:

```text
GERENTE | ATENDENTE | DENTISTA | AUXILIAR
```

Use `@PreAuthorize` nos controllers para proteger endpoints por perfil quando necessario.

Exemplos:

```java
@PreAuthorize("hasRole('GERENTE')")
@PostMapping
public ResponseEntity<ProfissionalResponse> cadastrar(@RequestBody ProfissionalRequest dto) { }

@PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
@GetMapping
public ResponseEntity<List<ProfissionalResponse>> listar() { }
```

## Estrutura de Pacotes dos Servicos de Dominio

Servicos de dominio devem seguir esta organizacao:

```text
com.clinica.servico_<nome>/
|-- controller/       -> classes @RestController
|-- service/          -> classes @Service com regras de negocio
|-- repository/       -> interfaces JpaRepository
|-- model/            -> entidades JPA e enums
|-- dto/              -> objetos Request e Response
|-- security/         -> JwtFilter, JwtUtil, SecurityConfig quando aplicavel
`-- exception/        -> excecoes customizadas e handler global
```

Regras:

- Nao coloque regra de negocio no controller.
- Nao acesse repository diretamente no controller; use service.
- Nao retorne entidade JPA em endpoint; retorne DTO.
- Nao coloque senha em DTO de resposta.
- Use injecao por construtor com `@RequiredArgsConstructor`.
- Use portugues para nomes de dominio e ingles para termos tecnicos comuns.

## Config Server

Localizacao:

```text
config-server/
```

Pacote base:

```text
com.clinica.config_server
```

Responsabilidades:

- Expor Spring Cloud Config na porta `8888`.
- Usar `@EnableConfigServer`.
- Buscar configuracoes no Git definido por `spring.cloud.config.server.git.uri`.
- Expor health check em `/actuator/health`.

Regras:

- Nao adicione regras de negocio no Config Server.
- Nao adicione controllers de dominio no Config Server.
- Mantenha credenciais do Git em variaveis de ambiente.

## Discovery Server

Localizacao:

```text
discovery-server/
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

## Nomenclatura

| Tipo | Padrao | Exemplo |
| --- | --- | --- |
| Controller | `{Entidade}Controller` | `ProfissionalController` |
| Service | `{Entidade}Service` | `ProfissionalService` |
| Repository | `{Entidade}Repository` | `ProfissionalRepository` |
| Entidade JPA | `{Entidade}` | `Profissional` |
| DTO entrada | `{Entidade}Request` | `ProfissionalRequest` |
| DTO saida | `{Entidade}Response` | `ProfissionalResponse` |
| Excecao | `{Motivo}Exception` | `RecursoNaoEncontradoException` |

Metodos comuns no service:

| Operacao | Nome |
| --- | --- |
| Criar | `cadastrar(Request dto)` |
| Buscar por ID | `buscarPorId(Long id)` |
| Listar ativos | `listarAtivos()` |
| Atualizar | `atualizar(Long id, Request dto)` |
| Inativar | `inativar(Long id)` |

## Padrao de Resposta da API

Resposta de sucesso: retorno direto do DTO.

Resposta de erro:

```json
{
  "status": 404,
  "erro": "Recurso nao encontrado",
  "mensagem": "Profissional com id 99 nao encontrado",
  "caminho": "/profissionais/99"
}
```

DTO recomendado:

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

## Seguranca - JWT

O `JwtFilter` e o `JwtUtil` devem ser consistentes entre os servicos protegidos.
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

- Toda entidade de dominio deve ter `ativo` com padrao `true`, quando fizer sentido para o dominio.
- Toda entidade de dominio deve ter `criadoEm` como `LocalDateTime`, quando fizer sentido para auditoria.
- Nomes de tabela ficam no plural e em portugues.
- Evite deletar registro de dominio; prefira inativar com `ativo = false`.

## Testes Automatizados

Todo modulo Spring Boot deve ter testes em:

```text
<modulo>/src/test/java
<modulo>/src/test/resources
```

Comandos por modulo:

```powershell
cd servico-autenticacao
.\mvnw.cmd clean test

cd ..\servico-profissional
.\mvnw.cmd clean test

cd ..\config-server
.\mvnw.cmd clean test

cd ..\discovery-server
.\mvnw.cmd clean test
```

Tambem e possivel rodar pela raiz, pois existe `pom.xml` agregador:

```powershell
mvn clean test
```

Regras para testes:

- Rode os testes do modulo alterado antes de finalizar uma tarefa.
- Se alterar contrato entre servicos, rode os testes dos modulos afetados.
- Se criar novo modulo Spring Boot, adicione ao menos um teste `contextLoads`.
- Para services de dominio, teste regras de negocio com JUnit e Mockito.
- Para controllers, teste status HTTP, DTOs e autorizacao com Spring MVC Test quando aplicavel.
- Nao dependa de Config Server, Eureka ou MySQL real em teste unitario.

## CI GitHub Actions

Workflows atuais:

- `.github/workflows/autenticacao-ci.yml`
- `.github/workflows/config-server-ci.yml`
- `.github/workflows/discovery-server-ci.yml`
- `.github/workflows/profissional-ci.yml`

Cada pipeline usa JDK 21, roda `mvn clean test` e deve ser mantido coerente com o caminho real do modulo.

## Regras Gerais

- Nao use `System.out.println`; use logger com `@Slf4j`.
- Nao faca `.get()` em `Optional`; use `.orElseThrow()`.
- Nao retorne entidade JPA diretamente.
- Nao coloque senha em DTO de resposta.
- Nao hardcode credenciais.
- Retorne `201 Created` para POST bem-sucedido quando criar recurso.
- Retorne `204 No Content` para inativacao bem-sucedida.
- Use `@RequiredArgsConstructor` e injecao por construtor.
- Exponha health check em servicos Dockerizados.
- Mantenha paths de Docker, Maven e CI coerentes com as pastas reais do repositorio.
- No frontend, preserve a arquitetura simples com `index.html`, `styles.css`, `app.js` e `nginx.conf`.

## Como usar este arquivo no Claude.ai

1. Crie um novo projeto em `claude.ai/projects`.
2. Nas instrucoes do projeto, cole o conteudo completo deste `CLAUDE.md`.
3. Adicione como contexto os arquivos do modulo afetado.
4. Ao pedir codigo, informe o modulo alterado e peca para considerar os testes automatizados.

Exemplo de prompt eficaz:

```text
Implemente a pagina de listagem de profissionais no frontend-autenticacao
seguindo a arquitetura de templates e troca do #conteudo descrita no CLAUDE.md.
```

Versao 1.2 - Sprint 2 - Clinica Odontologica
