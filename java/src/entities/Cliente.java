package entities;

public class Cliente {

    // ENCAPSULAMENTO: atributos privados, acesso só por getters
    private String nome;
    private String telefone;

    // CONSTRUTOR: toda ordem nasce com um cliente completo
    public Cliente(String nome, String telefone) {
        this.nome = nome;
        this.telefone = telefone;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefone() {
        return telefone;
    }
}
