package entities;

// Peça ou serviço cobrado dentro de uma ordem
public class ItemServico {

    private String descricao;
    private Integer quantidade;
    private final Double preco;

    public ItemServico(String descricao, Integer quantidade, Double preco) {
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.preco = preco;
    }

    public String getDescricao() {
        return descricao;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public Double getPreco() {
        return preco;
    }

    public Double subTotal() {
        return quantidade * preco;
    }
}
