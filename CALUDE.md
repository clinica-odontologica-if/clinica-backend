CLAUDE.md — Instruções de Desenvolvimento
Sistema de Gestão para Clínica Odontológica
Leia este arquivo completamente antes de gerar qualquer código.

Contexto do Projeto
Sistema de gestão para clínica odontológica construído com arquitetura de microsserviços.
Cada serviço é independente, tem seu próprio banco de dados e roda em container Docker.
A comunicação entre serviços ocorre via HTTP/REST com autenticação JWT.
Serviços existentes

servico-autenticacao → porta 8081 → banco clinica_auth
servico-profissional  → porta 8082 → banco clinica_profissional
servico-paciente      → porta 8083 → banco clinica_paciente

Perfis de usuário (RBAC)
GERENTE | ATENDENTE | DENTISTA | AUXILIAR

1. Estrutura de Pacotes
   Todo serviço deve seguir exatamente esta estrutura de pacotes:
   com.clinica.servico_<nome>/
   ├── controller/       → classes @RestController
   ├── service/          → classes @Service com regras de negócio
   ├── repository/       → interfaces que estendem JpaRepository
   ├── model/            → classes @Entity e enums
   ├── dto/              → objetos de entrada (Request) e saída (Response)
   ├── security/         → JwtFilter, JwtUtil, SecurityConfig
   └── exception/        → exceções customizadas e handler global
   Regras de pacotes

NUNCA coloque lógica de negócio no controller — ela pertence ao service
NUNCA acesse o repository diretamente no controller — sempre via service
NUNCA use a entidade (@Entity) como retorno de endpoint — use sempre um DTO de resposta


2. Nomenclatura
   Classes
   TipoPadrãoExemploController{Entidade}ControllerProfissionalControllerService{Entidade}ServiceProfissionalServiceRepository{Entidade}RepositoryProfissionalRepositoryEntidade JPA{Entidade}ProfissionalDTO entrada{Entidade}RequestProfissionalRequestDTO saída{Entidade}ResponseProfissionalResponseExceção{Motivo}ExceptionRecursoNaoEncontradoException
   Métodos no Service
   OperaçãoNome do métodoCriarcadastrar(Request dto)Buscar por IDbuscarPorId(Long id)Listar todoslistarAtivos()Atualizaratualizar(Long id, Request dto)Inativarinativar(Long id)
   Métodos no Controller
   HTTPPadrãoExemploPOSTcadastrar(@RequestBody Request dto)cadastrar(@RequestBody ProfissionalRequest dto)GET /listar()listar()GET /{id}buscarPorId(@PathVariable Long id)buscarPorId(@PathVariable Long id)PUT /{id}atualizar(@PathVariable Long id, @RequestBody Request dto)PATCH /{id}/inativarinativar(@PathVariable Long id)
   Variáveis e campos

Use camelCase para variáveis e métodos: nomeCompleto, dataNascimento
Use UPPER_SNAKE_CASE para constantes: EXPIRACAO_TOKEN
Use português para nomes de domínio: paciente, profissional, atendimento
Use inglês para nomes técnicos: repository, service, controller, handler
Campos de data: sempre LocalDate para datas, LocalDateTime para data+hora


3. Padrão de Resposta da API
   Resposta de sucesso — retorno direto do DTO
   json// GET /profissionais/1 → 200 OK
   {
   "id": 1,
   "nome": "Dr. João Silva",
   "email": "joao@clinica.com",
   "especialidade": "Ortodontia",
   "role": "DENTISTA",
   "ativo": true
   }
   Resposta de erro — sempre use este formato exato
   json// 404 Not Found
   {
   "status": 404,
   "erro": "Recurso não encontrado",
   "mensagem": "Profissional com id 99 não encontrado",
   "caminho": "/profissionais/99"
   }
   json// 403 Forbidden
   {
   "status": 403,
   "erro": "Acesso negado",
   "mensagem": "Seu perfil não tem permissão para acessar este recurso",
   "caminho": "/profissionais"
   }
   json// 401 Unauthorized
   {
   "status": 401,
   "erro": "Token ausente ou inválido",
   "mensagem": "Informe um token JWT válido no cabeçalho Authorization",
   "caminho": "/profissionais"
   }
   json// 400 Bad Request
   {
   "status": 400,
   "erro": "Dados inválidos",
   "mensagem": "O campo email é obrigatório",
   "caminho": "/profissionais"
   }
   Classe ErroResponse (usar em todos os serviços)
   java// dto/ErroResponse.java
   @Data
   @AllArgsConstructor
   public class ErroResponse {
   private int status;
   private String erro;
   private String mensagem;
   private String caminho;
   }

4. Tratamento de Exceções
   Exceções customizadas obrigatórias em cada serviço
   java// exception/RecursoNaoEncontradoException.java
   public class RecursoNaoEncontradoException extends RuntimeException {
   public RecursoNaoEncontradoException(String mensagem) {
   super(mensagem);
   }
   }

// exception/RegraDeNegocioException.java
public class RegraDeNegocioException extends RuntimeException {
public RegraDeNegocioException(String mensagem) {
super(mensagem);
}
}
Handler global obrigatório em cada serviço
java// exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return ResponseEntity.status(404).body(
            new ErroResponse(404, "Recurso não encontrado", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraDeNegocio(
            RegraDeNegocioException ex, HttpServletRequest request) {
        return ResponseEntity.status(400).body(
            new ErroResponse(400, "Dados inválidos", ex.getMessage(), request.getRequestURI())
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
Como usar no Service
java// CORRETO
public Profissional buscarPorId(Long id) {
return profissionalRepository.findById(id)
.orElseThrow(() -> new RecursoNaoEncontradoException(
"Profissional com id " + id + " não encontrado"
));
}

// ERRADO — nunca faça isso
public Profissional buscarPorId(Long id) {
return profissionalRepository.findById(id).get(); // NullPointerException em produção
}

5. Segurança — JWT
   O JwtFilter e JwtUtil são idênticos em todos os serviços — copie do servico-autenticacao sem modificar.
   SecurityConfig padrão para serviços não-auth
   java@Configuration
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
   "{\"status\":401,\"erro\":\"Token ausente ou inválido\"," +
   "\"mensagem\":\"Informe um token JWT válido no cabeçalho Authorization\"," +
   "\"caminho\":\"" + request.getRequestURI() + "\"}"
   );
   })
   )
   .authorizeHttpRequests(auth -> auth
   .requestMatchers("/*/health", "/error").permitAll()
   .anyRequest().authenticated()
   )
   .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
   }
   }
   Como proteger rotas por perfil
   java// No controller — use @PreAuthorize com ROLE_ como prefixo
   @PreAuthorize("hasRole('GERENTE')")
   @PostMapping
   public ResponseEntity<ProfissionalResponse> cadastrar(@RequestBody ProfissionalRequest dto) { }

@PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
@GetMapping
public ResponseEntity<List<PacienteResponse>> listar() { }

6. Padrão de Controller
   java@RestController
   @RequestMapping("/profissionais")
   @RequiredArgsConstructor
   public class ProfissionalController {

   private final ProfissionalService profissionalService;

   @PreAuthorize("hasRole('GERENTE')")
   @PostMapping
   public ResponseEntity<ProfissionalResponse> cadastrar(@RequestBody ProfissionalRequest dto) {
   return ResponseEntity.status(201).body(profissionalService.cadastrar(dto));
   }

   @PreAuthorize("hasRole('GERENTE')")
   @GetMapping
   public ResponseEntity<List<ProfissionalResponse>> listar() {
   return ResponseEntity.ok(profissionalService.listarAtivos());
   }

   @PreAuthorize("hasRole('GERENTE')")
   @GetMapping("/{id}")
   public ResponseEntity<ProfissionalResponse> buscarPorId(@PathVariable Long id) {
   return ResponseEntity.ok(profissionalService.buscarPorId(id));
   }

   @PreAuthorize("hasRole('GERENTE')")
   @PutMapping("/{id}")
   public ResponseEntity<ProfissionalResponse> atualizar(
   @PathVariable Long id, @RequestBody ProfissionalRequest dto) {
   return ResponseEntity.ok(profissionalService.atualizar(id, dto));
   }

   @PreAuthorize("hasRole('GERENTE')")
   @PatchMapping("/{id}/inativar")
   public ResponseEntity<Void> inativar(@PathVariable Long id) {
   profissionalService.inativar(id);
   return ResponseEntity.noContent().build();
   }

   @GetMapping("/health")
   public ResponseEntity<Map<String, String>> health() {
   return ResponseEntity.ok(Map.of("status", "ok"));
   }
   }

7. Padrão de Entidade
   java@Data
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

   private boolean ativo = true; // padrão sempre true no cadastro

   private LocalDateTime criadoEm = LocalDateTime.now();
   }
   Regras de entidade

Todo entity tem ativo (boolean, padrão true) — nunca delete, sempre inative
Todo entity tem criadoEm (LocalDateTime) — auditoria mínima
Nomes de tabela sempre no plural e em português: profissionais, pacientes
NUNCA exponha a entidade diretamente na API — sempre converta para DTO


8. Padrão de DTO
   java// Request — entrada de dados
   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   public class ProfissionalRequest {
   private String nome;
   private String email;
   private String especialidade;
   private Role role;
   }

// Response — saída de dados (nunca inclua senha)
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

9. Regras Gerais — Sempre siga

NUNCA use System.out.println — use log.info() / log.error() com @Slf4j
NUNCA faça .get() em Optional — use .orElseThrow()
NUNCA retorne a entidade JPA diretamente — sempre converta para DTO
NUNCA coloque senha em nenhum DTO de resposta
NUNCA hardcode credenciais — use variáveis de ambiente
SEMPRE que inativar, use ativo = false — nunca repository.delete()
SEMPRE retorne 201 Created para POST bem-sucedido, 204 No Content para inativação
SEMPRE adicione @RequiredArgsConstructor + injeção via construtor — nunca @Autowired
SEMPRE use Optional no repository e trate no service com orElseThrow


10. Como usar este arquivo no Claude.ai

Crie um novo projeto em claude.ai/projects
Nas instruções do projeto, cole o conteúdo completo deste CLAUDE.md
Adicione como contexto do projeto os arquivos do seu serviço específico
Ao pedir código, o Claude seguirá automaticamente todos os padrões acima

Exemplo de prompt eficaz
Implemente o PacienteService com os métodos cadastrar, buscarPorId,
listarAtivos, atualizar e inativar, seguindo os padrões do projeto.
Exemplo de prompt ruim
Faz um service de paciente pra mim

Versão 1.0 — Sprint 2 — Clínica Odontológica