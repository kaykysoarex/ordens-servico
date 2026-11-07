# FrioOS · Ordens de Serviço com Garantia (v3)

Projeto de estudos em **Java** (console) + **JavaScript** (painel responsivo) para gerenciar ordens de serviço de manutenção de geladeiras e freezers.

## Estrutura

```
ordens-servico/
├── java/src/
│   ├── entities/          Cliente, Equipamento, ItemServico, OrdemServico
│   │   └── enums/         StatusOS, TipoEquipamento
│   ├── services/          GestorOrdens, ExportadorJson
│   └── application/       Program (menu no console)
└── web/                   Aplicativo web: index.html, css/style.css, js/app.js
```

## Aplicativo web (HTML, CSS e JavaScript)

Abra `web/index.html` no navegador. Funciona sem instalar nada e sem internet.

- Criar ordens, adicionar itens, iniciar, concluir e cancelar.
- Busca e filtros (abertas, atrasadas, em garantia...).
- **Os dados ficam salvos no navegador** (localStorage).
- Botão **Dados**: importar, baixar JSON e carregar exemplo.
- Layout responsivo, com tema claro e escuro automáticos.

## Java (console)

No VS Code, abra a pasta `java` e rode `src/application/Program.java`. Pelo PowerShell, dentro da pasta `java`:

```powershell
mkdir out
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src).FullName
java -cp out application.Program
```

Se os acentos aparecerem como `?`, use `java -Dfile.encoding=UTF-8 -cp out application.Program`.

Menu: abrir, adicionar item, iniciar, concluir, **cancelar**, listar (todas, por status, atrasadas), consultar garantia, exemplo e exportar JSON.

## Ligação entre os dois

No Java, use a opção **10** e copie o JSON. No aplicativo web, abra **Dados**, cole e clique em **Importar**.

## Limitações conhecidas

- O Java guarda os dados só na memória (somem ao fechar). Persistir em arquivo exige o conteúdo da seção 15 do curso.
- Valores em `Double`; sistemas reais usam `BigDecimal`.
- Java e painel não se falam automaticamente: isso exige uma API (Spring Boot).
- Sem testes automatizados (JUnit).
- Garantia: 90 dias após a conclusão.
