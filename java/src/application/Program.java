package application;

import entities.Cliente;
import entities.Equipamento;
import entities.ItemServico;
import entities.OrdemServico;
import entities.enums.StatusOS;
import entities.enums.TipoEquipamento;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import services.ExportadorJson;
import services.GestorOrdens;

public class Program {

    // FORMATADOR: o Instant (UTC) é mostrado no horário de São Paulo
    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);                 // números com ponto (ex.: 50.00)
        try (final Scanner sc = new Scanner(System.in)) {
            final GestorOrdens gestor = new GestorOrdens();

            int opcao = -1;
            while (opcao != 0) {
                menu();
                opcao = lerInt(sc, "Opção: ");
                final Instant agora = Instant.now();  // momento atual, em UTC

                switch (opcao) {
                    case 1 -> abrirOrdem(sc, gestor, agora);
                    case 2 -> adicionarItem(sc, gestor);
                    case 3 -> iniciar(sc, gestor);
                    case 4 -> concluir(sc, gestor, agora);
                    case 5 -> listar(gestor.todas(), agora);
                    case 6 -> listarPorStatus(sc, gestor, agora);
                    case 7 -> listarAtrasadas(gestor, agora);
                    case 8 -> consultarGarantia(sc, gestor, agora);
                    case 9 -> carregarExemplo(gestor, agora);
                    case 10 -> exportar(gestor);
                    case 11 -> cancelar(sc, gestor);
                    case 0 -> System.out.println("Até logo!");
                    default -> System.out.println("Opção inválida.");
                }
            }
        }
    }

    static void menu() {
        System.out.println();
        System.out.println("===== ORDENS DE SERVIÇO =====");
        System.out.println("1  - Abrir ordem");
        System.out.println("2  - Adicionar item a uma ordem");
        System.out.println("3  - Iniciar atendimento");
        System.out.println("4  - Concluir atendimento");
        System.out.println("5  - Listar todas");
        System.out.println("6  - Listar por status");
        System.out.println("7  - Listar atrasadas");
        System.out.println("8  - Consultar garantia");
        System.out.println("9  - Carregar dados de exemplo");
        System.out.println("10 - Exportar JSON para o painel");
        System.out.println("11 - Cancelar ordem");
        System.out.println("0  - Sair");
    }

    // ENTRADA DE DADOS --------------------------------------------------
    // VALIDAÇÃO sem exceções: hasNextInt()/hasNextDouble() conferem se o próximo
    // texto é um número ANTES de ler. Se não for, descarta e pede de novo.

    static int lerInt(Scanner sc, String msg) {
        System.out.print(msg);
        while (!sc.hasNextInt()) {
            sc.next();                // descarta o que não é número
            System.out.print("Digite um número inteiro: ");
        }
        int n = sc.nextInt();
        sc.nextLine();                // descarta o ENTER que sobrou no buffer
        return n;
    }

    static double lerDouble(Scanner sc, String msg) {
        System.out.print(msg);
        while (!sc.hasNextDouble()) {
            sc.next();
            System.out.print("Digite um número (use ponto): ");
        }
        double d = sc.nextDouble();
        sc.nextLine();
        return d;
    }

    static String lerTexto(Scanner sc, String msg) {
        System.out.print(msg);
        return sc.nextLine();
    }

    static TipoEquipamento lerTipo(Scanner sc) {
        int n = lerInt(sc, "Tipo (1-Geladeira 2-Freezer 3-Expositor 4-Outro): ");
        return switch (n) {
            case 1 -> TipoEquipamento.GELADEIRA;
            case 2 -> TipoEquipamento.FREEZER;
            case 3 -> TipoEquipamento.EXPOSITOR;
            default -> TipoEquipamento.OUTRO;
        };
    }

    // OPERAÇÕES ----------------------------------------------------------

    static void abrirOrdem(Scanner sc, GestorOrdens gestor, Instant agora) {
        String nome = lerTexto(sc, "Nome do cliente: ");
        String telefone = lerTexto(sc, "Telefone: ");
        TipoEquipamento tipo = lerTipo(sc);
        String modelo = lerTexto(sc, "Marca/modelo: ");
        String defeito = lerTexto(sc, "Defeito relatado: ");
        int prazo = lerInt(sc, "Prazo de atendimento (dias): ");

        Cliente cliente = new Cliente(nome, telefone);
        Equipamento equipamento = new Equipamento(tipo, modelo);
        OrdemServico os = gestor.abrir(cliente, equipamento, defeito, prazo, agora);

        System.out.println("Ordem #" + os.getId() + " aberta. Prazo até " + FMT.format(os.prazoFinal()));
    }

    static void adicionarItem(Scanner sc, GestorOrdens gestor) {
        OrdemServico os = gestor.buscar(lerInt(sc, "Número da ordem: "));
        if (os == null) {
            System.out.println("Ordem não encontrada.");
            return;
        }
        if (os.getStatus() == StatusOS.CONCLUIDA || os.getStatus() == StatusOS.CANCELADA) {
            System.out.println("Ordem concluída ou cancelada: não aceita novos itens.");
            return;
        }
        String descricao = lerTexto(sc, "Descrição do item: ");
        int quantidade = lerInt(sc, "Quantidade: ");
        double preco = lerDouble(sc, "Preço unitário: ");
        os.adicionarItem(new ItemServico(descricao, quantidade, preco));
        System.out.printf("Item adicionado. Total da ordem: R$ %.2f%n", os.total());
    }

    static void iniciar(Scanner sc, GestorOrdens gestor) {
        OrdemServico os = gestor.buscar(lerInt(sc, "Número da ordem: "));
        if (os == null) {
            System.out.println("Ordem não encontrada.");
        } else if (os.iniciar()) {
            System.out.println("Atendimento iniciado.");
        } else {
            System.out.println("Só é possível iniciar uma ordem ABERTA.");
        }
    }

    static void concluir(Scanner sc, GestorOrdens gestor, Instant agora) {
        OrdemServico os = gestor.buscar(lerInt(sc, "Número da ordem: "));
        if (os == null) {
            System.out.println("Ordem não encontrada.");
        } else if (os.concluir(agora)) {
            System.out.println("Ordem concluída. Garantia até " + FMT.format(os.garantiaAte()));
        } else {
            System.out.println("Só é possível concluir uma ordem EM_ANDAMENTO.");
        }
    }

    static void cancelar(Scanner sc, GestorOrdens gestor) {
        OrdemServico os = gestor.buscar(lerInt(sc, "Número da ordem: "));
        if (os == null) {
            System.out.println("Ordem não encontrada.");
        } else if (os.cancelar()) {
            System.out.println("Ordem cancelada.");
        } else {
            System.out.println("Ordem concluída ou já cancelada: não dá para cancelar.");
        }
    }

    static void listarPorStatus(Scanner sc, GestorOrdens gestor, Instant agora) {
        int n = lerInt(sc, "Status (1-Aberta 2-Em andamento 3-Concluída 4-Cancelada): ");
        StatusOS status = switch (n) {
            case 1 -> StatusOS.ABERTA;
            case 2 -> StatusOS.EM_ANDAMENTO;
            case 3 -> StatusOS.CONCLUIDA;
            case 4 -> StatusOS.CANCELADA;
            default -> null;
        };
        if (status == null) {
            System.out.println("Status inválido.");
            return;
        }
        listar(gestor.porStatus(status), agora);
    }

    static void listarAtrasadas(GestorOrdens gestor, Instant agora) {
        List<OrdemServico> atrasadas = gestor.atrasadas(agora);
        listar(atrasadas, agora);
        for (OrdemServico os : atrasadas) {
            long dias = Duration.between(os.prazoFinal(), agora).toDays();
            System.out.println("-> Ordem #" + os.getId() + " está " + dias + " dia(s) além do prazo.");
        }
    }

    static void consultarGarantia(Scanner sc, GestorOrdens gestor, Instant agora) {
        OrdemServico os = gestor.buscar(lerInt(sc, "Número da ordem: "));
        if (os == null) {
            System.out.println("Ordem não encontrada.");
            return;
        }
        if (os.getConclusao() == null) {
            System.out.println("Ordem ainda não concluída: a garantia só começa na conclusão.");
            return;
        }
        if (os.emGarantia(agora)) {
            long restam = Duration.between(agora, os.garantiaAte()).toDays();
            System.out.println("EM GARANTIA até " + FMT.format(os.garantiaAte()) + " (restam " + restam + " dia(s)).");
        } else {
            long vencida = Duration.between(os.garantiaAte(), agora).toDays();
            System.out.println("GARANTIA VENCIDA em " + FMT.format(os.garantiaAte()) + " (há " + vencida + " dia(s)).");
        }
    }

    static void exportar(GestorOrdens gestor) {
        System.out.println("--- COPIE O JSON ABAIXO E COLE NO PAINEL ---");
        System.out.println(ExportadorJson.exportar(gestor.todas()));
        System.out.println("--- FIM DO JSON ---");
    }

    // EXIBIÇÃO ------------------------------------------------------------

    static void listar(List<OrdemServico> lista, Instant agora) {
        if (lista.isEmpty()) {
            System.out.println("Nenhuma ordem encontrada.");
            return;
        }
        for (OrdemServico os : lista) {
            descrever(os, agora);
        }
    }

    static void descrever(OrdemServico os, Instant agora) {
        System.out.println();
        System.out.println("#" + os.getId() + " [" + os.getStatus() + "] " + os.getCliente().getNome()
                + " - " + os.getEquipamento().getTipo() + " " + os.getEquipamento().getModelo());
        System.out.println("   Defeito: " + os.getDefeito());
        System.out.println("   Aberta em " + FMT.format(os.getAbertura())
                + " | prazo até " + FMT.format(os.prazoFinal())
                + (os.atrasada(agora) ? " (ATRASADA)" : ""));
        System.out.printf("   Total: R$ %.2f%n", os.total());
        if (os.getConclusao() != null) {
            Duration atendimento = os.tempoAtendimento();
            System.out.println("   Concluída em " + FMT.format(os.getConclusao())
                    + " (atendimento de " + atendimento.toHours() + "h)"
                    + " | garantia " + (os.emGarantia(agora) ? "ATIVA" : "VENCIDA")
                    + " até " + FMT.format(os.garantiaAte()));
        }
    }

    // DADOS DE EXEMPLO ----------------------------------------------------
    // Datas relativas a "agora": o exemplo sempre tem garantia ativa, vencida e atraso.

    static Instant diasAtras(Instant base, long dias) {
        return base.minus(dias, ChronoUnit.DAYS);
    }

    static void carregarExemplo(GestorOrdens gestor, Instant agora) {
        OrdemServico a = gestor.abrir(new Cliente("João Pereira", "31 99999-1111"),
                new Equipamento(TipoEquipamento.FREEZER, "Consul CHB"), "Não congela", 3, diasAtras(agora, 100));
        a.adicionarItem(new ItemServico("Compressor", 1, 450.0));
        a.adicionarItem(new ItemServico("Gás refrigerante", 1, 120.0));
        a.iniciar();
        a.concluir(diasAtras(agora, 95));                 // garantia já vencida

        OrdemServico b = gestor.abrir(new Cliente("Maria Souza", "31 98888-2222"),
                new Equipamento(TipoEquipamento.GELADEIRA, "Brastemp BRM44"), "Faz barulho", 3, diasAtras(agora, 20));
        b.adicionarItem(new ItemServico("Termostato", 1, 85.0));
        b.iniciar();
        b.concluir(diasAtras(agora, 18));                 // garantia ativa

        OrdemServico c = gestor.abrir(new Cliente("Padaria Bom Pão", "31 97777-3333"),
                new Equipamento(TipoEquipamento.EXPOSITOR, "Metalfrio VB40"), "Não liga", 3, diasAtras(agora, 6));
        c.adicionarItem(new ItemServico("Placa eletrônica", 1, 230.0));
        c.iniciar();                                      // em andamento e atrasada

        gestor.abrir(new Cliente("Carlos Lima", "31 96666-4444"),
                new Equipamento(TipoEquipamento.GELADEIRA, "Electrolux DF36"), "Gelo no fundo", 5, diasAtras(agora, 1));

        System.out.println("4 ordens de exemplo carregadas.");
    }
}
