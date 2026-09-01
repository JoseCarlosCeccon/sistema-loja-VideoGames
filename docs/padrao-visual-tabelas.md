# Padrao visual das tabelas de gestao

## Escopo

O refinamento foi aplicado às tabelas principais de Clientes, Estoque, Ordens de Serviço,
cadastro de Equipamentos e Caixa. Ele altera somente apresentação e interação; filtros,
ordenação, permissões implícitas nos estados e chamadas da API continuam sendo as mesmas.

## Componentes compartilhados

- `EntityCell`: cria a hierarquia visual da primeira coluna com ícone semântico ou foto real,
  nome em destaque e metadado/descrição secundária com limite de duas linhas.
- `RowActions`: mantém até duas ações frequentes visíveis e leva ações secundárias ou destrutivas
  para um menu de reticências. O menu é renderizado em portal para não ser recortado pelo
  `overflow` horizontal da tabela, fecha com clique externo ou `Escape` e possui semântica de menu.
- `entityIcons`: centraliza a seleção de ícones. Clientes usam usuário; consoles e controles usam
  gamepad; produtos físicos usam pacote; serviços usam documento. Categorias ou tipos desconhecidos
  usam o ícone neutro de pacote, sem criar novos valores de domínio.
- `SortableHeader`: permanece como a única implementação de ordenação por cabeçalho nas telas que
  já possuem ordenação.

## Regras por tela

- Clientes: `Ver` e `Editar` ficam visíveis; `Registros` e `Excluir` ficam no menu.
- Estoque: `Ver` e `Editar` ficam visíveis; `Excluir` fica no menu. A foto real do produto é
  preservada e o ícone semântico substitui somente as antigas iniciais quando não há foto.
- Ordens de Serviço: `Ver` e, quando permitido pelo status, `Saída` ficam visíveis. `Editar`,
  `Orçamento`, `Autorizar`, `PDF` e `Excluir` ficam no menu respeitando exatamente as condições
  anteriores. Ordens entregues continuam sem ações de edição/saída; ordens não autorizadas podem
  ser editadas ou enviadas para saída para registrar a devolução do equipamento ao cliente.
- Cadastro de Equipamentos: `Editar` fica visível e `Excluir` fica no menu.
- Caixa: `Editar` fica visível; `PDF da venda`, quando há venda vinculada, e `Excluir` ficam no menu.

## Densidade e leitura

- Linhas usam espaçamento vertical compacto e `hover` discreto.
- Quantidades e valores monetários usam alinhamento à direita e algarismos tabulares.
- Descrições extensas são limitadas a duas linhas; o conteúdo completo permanece disponível no
  atributo `title` nativo.
- Status genéricos usam ponto colorido e texto. Os oito status reais de OS continuam usando o
  componente especializado `ServiceOrderStatusBadge`.
- Os estilos possuem variantes para os temas escuro e claro e mantêm rolagem horizontal em telas
  estreitas.

## Backend e banco

Nenhum endpoint, model, tabela ou migração foi alterado. O trabalho reutiliza os dados já
carregados pelas páginas e não cria uma segunda lógica de pesquisa, ordenação ou regra de status.
