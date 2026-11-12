package entities;

import entities.enums.TipoEquipamento;

public class Equipamento {
    private final TipoEquipamento tipo;   // ENUM usado como tipo de um atributo
    private final String modelo;

    public Equipamento(TipoEquipamento tipo, String modelo) {
        this.tipo = tipo;
        this.modelo = modelo;
    }

    public TipoEquipamento getTipo() {
        return tipo;
    }

    public String getModelo() {
        return modelo;
    }
}
