package entities;

import java.util.ArrayList;
import java.util.List;

// Parte interna da ordem: reúne os itens e calcula o valor cobrado.
public class Orcamento {

    private final List<ItemServico> itens = new ArrayList<>();

    public void adicionar(ItemServico item) {
        itens.add(item);
    }

    public List<ItemServico> getItens() {
        return new ArrayList<>(itens);
    }

    public Double total() {
        double soma = 0.0;
        for (ItemServico item : itens) {
            soma += item.subTotal();
        }
        return soma;
    }
}
