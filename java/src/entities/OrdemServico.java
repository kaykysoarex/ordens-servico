package entities;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import entities.enums.StatusOS;

public class OrdemServico {

    // MEMBRO ESTÁTICO: constante da classe, igual para todas as ordens
    private static final int DIAS_GARANTIA = 90;

    private final Integer id;
    private final String defeito;
    private final Integer prazoDias;
    private final Instant abertura;     // DATA-HORA GLOBAL: momento exato, sempre em UTC
    private Instant conclusao;    // fica null até a ordem ser concluída
    private StatusOS status;

    // Associações: cliente e equipamento podem existir fora desta ordem.
    private final Cliente cliente;
    private final Equipamento equipamento;
    // Composição: o orçamento nasce e termina junto com a ordem.
    private final Orcamento orcamento = new Orcamento();

    public OrdemServico(Integer id, Cliente cliente, Equipamento equipamento,
                        String defeito, Integer prazoDias, Instant abertura) {
        this.id = id;
        this.cliente = cliente;
        this.equipamento = equipamento;
        this.defeito = defeito;
        this.prazoDias = prazoDias;
        this.abertura = abertura;
        this.status = StatusOS.ABERTA;
    }

    public Integer getId() { return id; }
    public String getDefeito() { return defeito; }
    public Integer getPrazoDias() { return prazoDias; }
    public Instant getAbertura() { return abertura; }
    public Instant getConclusao() { return conclusao; }
    public StatusOS getStatus() { return status; }
    public Cliente getCliente() { return cliente; }
    public Equipamento getEquipamento() { return equipamento; }
    public List<ItemServico> getItens() { return orcamento.getItens(); }

    public void adicionarItem(ItemServico item) {
        orcamento.adicionar(item);
    }

    public Double total() {
        return orcamento.total();
    }

    // Regras do ciclo de vida: devolvem false se a transição não é permitida
    public boolean iniciar() {
        if (status != StatusOS.ABERTA) {
            return false;
        }
        status = StatusOS.EM_ANDAMENTO;
        return true;
    }

    public boolean concluir(Instant momento) {
        if (status != StatusOS.EM_ANDAMENTO) {
            return false;
        }
        conclusao = momento;
        status = StatusOS.CONCLUIDA;
        return true;
    }

    public boolean cancelar() {
        if (status == StatusOS.CONCLUIDA || status == StatusOS.CANCELADA) {
            return false;
        }
        status = StatusOS.CANCELADA;
        return true;
    }

    // CÁLCULOS COM DATA-HORA ----------------------------------------

    public Instant prazoFinal() {
        return abertura.plus(prazoDias, ChronoUnit.DAYS);       // Instant + tempo
    }

    public boolean atrasada(Instant agora) {
        return status != StatusOS.CONCLUIDA && status != StatusOS.CANCELADA && agora.isAfter(prazoFinal());
    }

    public Instant garantiaAte() {
        if (conclusao == null) {
            return null;
        }
        return conclusao.plus(DIAS_GARANTIA, ChronoUnit.DAYS);
    }

    public boolean emGarantia(Instant agora) {
        Instant fim = garantiaAte();
        return fim != null && agora.isBefore(fim);
    }

    public Duration tempoAtendimento() {                         // DURATION entre dois instantes
        if (conclusao == null) {
            return null;
        }
        return Duration.between(abertura, conclusao);
    }
}
