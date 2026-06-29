package com.clinica.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Ferramenta utilitária para geração e verificação de hashes BCrypt.
 *
 * USO:
 *   mvn exec:java -pl tools/hash-generator
 *
 * IMPORTANTE:
 *   Este programa é exclusivo para uso do time de desenvolvimento.
 *   Nunca deve ser incluído em builds de produção como serviço ativo.
 */
public class HashGenerator {

    // Mesmo strength usado pelo BCryptPasswordEncoder nos serviços
    private static final int BCRYPT_STRENGTH = 12;

    // Senhas padrão do sistema — atualize aqui se as senhas mudarem
    private static final Map<String, String> SENHAS_PADRAO = new LinkedHashMap<>();

    static {
        SENHAS_PADRAO.put("gerente@clinica.com   (GERENTE)",    "Gerente@2025");
        SENHAS_PADRAO.put("atendente@clinica.com (ATENDENTE)",  "Atendente@2025");
        SENHAS_PADRAO.put("dentista@clinica.com  (DENTISTA)",   "Dentista@2025");
        SENHAS_PADRAO.put("auxiliar@clinica.com  (AUXILIAR)",   "Auxiliar@2025");
    }

    private final BCryptPasswordEncoder encoder;
    private final Scanner scanner;

    public HashGenerator() {
        this.encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        new HashGenerator().executar();
    }

    private void executar() {
        imprimirCabecalho();

        boolean rodando = true;
        while (rodando) {
            imprimirMenu();
            int opcao = lerOpcao();

            switch (opcao) {
                case 1 -> gerarHashInterativo();
                case 2 -> verificarHashInterativo();
                case 3 -> gerarHashesPadrao();
                case 0 -> rodando = false;
                default -> System.out.println("\n  [!] Opção inválida. Tente novamente.");
            }

            if (rodando) {
                System.out.println("\n  Pressione ENTER para continuar...");
                scanner.nextLine();
            }
        }

        System.out.println("\n  Encerrando. Até mais!\n");
        scanner.close();
    }

    // ─── Opção 1: Gerar hash de uma senha qualquer ───────────────────────────

    private void gerarHashInterativo() {
        System.out.println("\n── Gerar Hash ──────────────────────────────");
        System.out.print("  Digite a senha: ");
        String senha = scanner.nextLine().trim();

        if (senha.isEmpty()) {
            System.out.println("  [!] Senha não pode ser vazia.");
            return;
        }

        System.out.println("\n  Gerando hash (strength=" + BCRYPT_STRENGTH + ")...");
        String hash = encoder.encode(senha);

        System.out.println("\n  ✔ Hash gerado:");
        System.out.println("  " + hash);
        System.out.println("\n  Copie o hash acima e cole no script SQL.");
    }

    // ─── Opção 2: Verificar se senha bate com hash ───────────────────────────

    private void verificarHashInterativo() {
        System.out.println("\n── Verificar Hash ──────────────────────────");
        System.out.print("  Digite a senha em texto puro: ");
        String senha = scanner.nextLine().trim();

        System.out.print("  Cole o hash BCrypt: ");
        String hash = scanner.nextLine().trim();

        if (senha.isEmpty() || hash.isEmpty()) {
            System.out.println("  [!] Senha e hash são obrigatórios.");
            return;
        }

        System.out.println("\n  Verificando...");
        boolean bate = encoder.matches(senha, hash);

        if (bate) {
            System.out.println("  ✔ SENHA CORRETA — a senha bate com o hash.");
        } else {
            System.out.println("  ✘ SENHA INCORRETA — a senha NÃO bate com o hash.");
        }
    }

    // ─── Opção 3: Gerar hashes de todas as senhas padrão ────────────────────

    private void gerarHashesPadrao() {
        System.out.println("\n── Hashes das Senhas Padrão do Sistema ─────");
        System.out.println("  Strength: " + BCRYPT_STRENGTH + " rounds");
        System.out.println("  Aguarde, isso pode levar alguns segundos...\n");

        StringBuilder sqlBlock = new StringBuilder();
        sqlBlock.append("-- Hashes gerados em: ")
                .append(java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter
                                .ofPattern("dd/MM/yyyy HH:mm:ss")))
                .append("\n\n");

        for (Map.Entry<String, String> entrada : SENHAS_PADRAO.entrySet()) {
            String identificador = entrada.getKey();
            String senha         = entrada.getValue();
            String hash          = encoder.encode(senha);

            System.out.println("  Usuário : " + identificador);
            System.out.println("  Senha   : " + senha);
            System.out.println("  Hash    : " + hash);
            System.out.println();

            sqlBlock.append("-- ").append(identificador).append("\n");
            sqlBlock.append("-- Senha: ").append(senha).append("\n");
            sqlBlock.append("'").append(hash).append("'\n\n");
        }

        System.out.println("────────────────────────────────────────────");
        System.out.println("  Bloco SQL pronto para colar no V2:\n");
        System.out.println(sqlBlock);
        System.out.println("────────────────────────────────────────────");
        System.out.println("  ✔ Copie os hashes acima e atualize o arquivo:");
        System.out.println("  servico-autenticacao/src/main/resources/db/migration/V2__inserir_usuarios_iniciais.sql");
    }

    // ─── Utilitários ─────────────────────────────────────────────────────────

    private void imprimirCabecalho() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║   Clínica Odontológica — Hash Generator  ║");
        System.out.println("  ║   Uso exclusivo do time de desenvolvimento║");
        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println();
    }

    private void imprimirMenu() {
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │  1. Gerar hash de uma senha             │");
        System.out.println("  │  2. Verificar senha contra hash         │");
        System.out.println("  │  3. Gerar hashes das senhas padrão      │");
        System.out.println("  │  0. Sair                                │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.print("  Opção: ");
    }

    private int lerOpcao() {
        try {
            int opcao = scanner.nextInt();
            scanner.nextLine(); // consumir quebra de linha
            return opcao;
        } catch (InputMismatchException e) {
            scanner.nextLine(); // limpar buffer
            return -1;
        }
    }
}