package services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import entities.Cliente;
import entities.Equipamento;
import entities.OrdemServico;
import entities.enums.StatusOS;

// Guarda a lista de ordens e as buscas. Os dados ficam só na memória (somem ao fechar).
public class GestorOrdens {

    private final List<OrdemServico> ordens = new ArrayList<>();
    private int proximoId = 1;

    public OrdemServico abrir(Cliente cliente, Equipamento equipamento,
                              String defeito, int prazoDias, Instant abertura) {
        OrdemServico os = new OrdemServico(proximoId, cliente, equipamento, defeito, prazoDias, abertura);
        proximoId++;
        ordens.add(os);
        return os;
    }

    public OrdemServico buscar(int id) {
        for (OrdemServico os : ordens) {
            if (os.getId() == id) {
                return os;
            }
        }
        return null;                         // não achou
    }

    public List<OrdemServico> todas() {
        return new ArrayList<>(ordens);      // cópia: ninguém altera a lista interna
    }

    public List<OrdemServico> porStatus(StatusOS status) {
        List<OrdemServico> resultado = new ArrayList<>();
        for (OrdemServico os : ordens) {
            if (os.getStatus() == status) {  // enum se compara com ==
                resultado.add(os);
            }
        }
        return resultado;
    }

    public List<OrdemServico> atrasadas(Instant agora) {
        List<OrdemServico> resultado = new ArrayList<>();
        for (OrdemServico os : ordens) {
            if (os.atrasada(agora)) {
                resultado.add(os);
            }
        }
        return resultado;
    }
}
