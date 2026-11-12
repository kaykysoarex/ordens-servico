package entities;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Regra reutilizável para calcular e consultar a vigência da garantia.
public class PoliticaGarantia {

    private static final int DIAS_PADRAO = 90;

    private final int dias;

    public PoliticaGarantia() {
        this(DIAS_PADRAO);
    }

    public PoliticaGarantia(int dias) {
        this.dias = dias;
    }

    public Instant terminaEm(Instant conclusao) {
        return conclusao.plus(dias, ChronoUnit.DAYS);
    }

    public boolean estaVigente(Instant conclusao, Instant agora) {
        return conclusao != null && agora.isBefore(terminaEm(conclusao));
    }
}
