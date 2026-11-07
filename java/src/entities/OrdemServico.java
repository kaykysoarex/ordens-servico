package entities;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import entities.enums.StatusOS;

public class OrdemServico {

    // MEMBRO ESTÁTICO: constante da classe, igual para todas as ordens
    private static final int DIAS_GARANTIA = 90;

    private Integer id;
    private String defeito;
    private Integer prazoDias;
    private Instant abertura;     // DATA-HORA GLOBAL: momento exato, sempre em UTC
    private Instant conclusao;    // fica null até a ordem ser concluída
    private StatusOS status;

    // COMPOSIÇÃO: a ordem "tem um" cliente, "tem um" equipamento e "tem vários" itens
    private Cliente cliente;
    private Equipamento equipamento;
    private List<ItemServico> itens = new ArrayList<>();

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
    public List<ItemServico> getItens() { return itens; }

    public void adicionarItem(ItemServico item) {
        itens.add(item);
    }

    public Double total() {
        double soma = 0.0;
        for (ItemServico item : itens) {      // LISTA percorrida com for-each
            soma += item.subTotal();
        }
        return soma;
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
