package services;

import java.util.List;

import entities.ItemServico;
import entities.OrdemServico;

// Monta o texto JSON que o painel em JavaScript lê.
// Datas saem em ISO 8601 (UTC, com Z): é o toString() do Instant.
public class ExportadorJson {

    public static String exportar(List<OrdemServico> ordens) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < ordens.size(); i++) {
            OrdemServico os = ordens.get(i);
            sb.append("  {");
            sb.append("\"id\": ").append(os.getId()).append(", ");
            sb.append("\"cliente\": ").append(texto(os.getCliente().getNome())).append(", ");
            sb.append("\"telefone\": ").append(texto(os.getCliente().getTelefone())).append(", ");
            sb.append("\"tipo\": ").append(texto(os.getEquipamento().getTipo().toString())).append(", ");
            sb.append("\"modelo\": ").append(texto(os.getEquipamento().getModelo())).append(", ");
            sb.append("\"defeito\": ").append(texto(os.getDefeito())).append(", ");
            sb.append("\"status\": ").append(texto(os.getStatus().toString())).append(", ");
            sb.append("\"prazoDias\": ").append(os.getPrazoDias()).append(", ");
            sb.append("\"abertura\": ").append(texto(os.getAbertura().toString())).append(", ");
            // OPERADOR TERNÁRIO: conclusão e garantia podem não existir (null no JSON)
            sb.append("\"conclusao\": ").append(os.getConclusao() == null ? "null" : texto(os.getConclusao().toString())).append(", ");
            sb.append("\"garantiaAte\": ").append(os.garantiaAte() == null ? "null" : texto(os.garantiaAte().toString())).append(", ");
            sb.append("\"total\": ").append(String.format("%.2f", os.total())).append(", ");
            sb.append("\"itens\": [");
            List<ItemServico> itens = os.getItens();
            for (int j = 0; j < itens.size(); j++) {
                ItemServico item = itens.get(j);
                if (j > 0) {
                    sb.append(", ");
                }
                sb.append("{\"descricao\": ").append(texto(item.getDescricao()));
                sb.append(", \"quantidade\": ").append(item.getQuantidade());
                sb.append(", \"preco\": ").append(String.format("%.2f", item.getPreco())).append("}");
            }
            sb.append("]}");
            if (i < ordens.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    // Coloca aspas e protege aspas e barras dentro do texto
    private static String texto(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
