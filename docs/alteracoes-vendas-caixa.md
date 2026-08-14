# Alteracoes em vendas, caixa, pesquisa e PDF

Este documento registra os marcos da correcao solicitada e a fonte de verdade adotada.

## Diagnostico inicial

- A venda e persistida em `venda`; seus produtos ficam em `item_venda`.
- Cada criacao ou edicao de venda gera uma entrada vinculada em `movimento_caixa`.
- A tela Caixa, o saldo atual e o saldo diario consultam `movimento_caixa`.
- Antes desta correcao, a Home somava `venda.valor_total` para "Vendas do dia" e reconstruia "Caixa do dia" somando vendas e servicos por datas de outras tabelas.
- Excluir apenas o movimento de uma venda removia o registro do Caixa, mas nao afetava os totais paralelos da Home.

## Marco 1 - fonte financeira unica para a Home

- `movimento_caixa` passa a ser a fonte dos dois indicadores financeiros da Home.
- "Vendas do dia" soma entradas do dia que possuem `id_venda`.
- "Caixa do dia" usa o saldo liquido real do dia (entradas menos saidas), incluindo vendas, servicos e lancamentos manuais.
- O movimento automatico passa a usar a data da venda, mantendo o periodo financeiro coerente em vendas criadas ou editadas com outra data.
- Subtotais de itens sao sempre derivados de quantidade x valor unitario; um subtotal enviado pelo cliente nao pode deixar venda e itens divergentes.
- O total da venda tambem e sempre derivado da soma desses itens. O campo de entrada legado `valorTotal` continua aceito na desserializacao, mas nao substitui o calculo canonico.
- Criacao, edicao de valor, alteracao de itens/pagamento, exclusao e desvinculacao foram cobertas por teste de integracao.

## Arquivos do marco 1

- `backend/src/main/java/com/acaogames/backend/repository/MovimentoCaixaRepository.java`
- `backend/src/main/java/com/acaogames/backend/service/DashboardService.java`
- `backend/src/main/java/com/acaogames/backend/service/VendaService.java`
- `backend/src/test/java/com/acaogames/backend/service/VendaDashboardIntegrationTest.java`

## Marco 2 - comprovante de venda no Caixa

- Movimentos vinculados a uma venda exibem a acao `PDF` na coluna de acoes do Caixa.
- A acao reutiliza o endpoint existente `GET /api/vendas/{id}/pdf` e o helper autenticado `api.fileUrl`.
- Nenhuma logica de geracao de comprovante foi duplicada no frontend ou no backend.

## Arquivos do marco 2

- `frontend/src/pages/Caixa.jsx`

## Marco 3 - pesquisa relacional

- A pesquisa do relatorio de vendas mantem os campos anteriores e passa a consultar tambem telefone, CPF e email do cliente.
- Produtos e itens relacionados sao pesquisados por nome, categoria, descricao e identificadores.
- A pesquisa do relatorio de caixa passa a resolver, na mesma query, dados da venda vinculada (cliente e produtos) e da OS vinculada (cliente, equipamento, numero de serie, problema, diagnostico e servico realizado).
- As relacoes sao consultadas com `EXISTS`, evitando duplicidade de vendas, contagens incorretas e requisicoes adicionais.
- Nao foi criada relacao entre venda e equipamento: no modelo atual, equipamento pertence a ordem de servico. Por isso equipamento e servico sao pesquisaveis no caixa e nas ordens; produtos/itens sao pesquisaveis nas vendas.

## Arquivos do marco 3

- `backend/src/main/java/com/acaogames/backend/service/RelatorioService.java`
- `backend/src/test/java/com/acaogames/backend/SQLiteDatabaseIntegrationTest.java`

## Marco 4 - comprovante de venda em via unica

- A revisao confirmou uma unica implementacao de comprovante em `VendaPdfService`, baseada em `comprovante-venda.jrxml`.
- Registro de venda na Home, historico do cliente e Caixa apontam para o mesmo endpoint `GET /api/vendas/{id}/pdf`.
- O template de venda ja nao possuia blocos duplicados nem os textos "Via do Cliente" e "Via da Loja"; ele foi preservado para evitar alteracao visual desnecessaria.
- Foi adicionado teste de regressao que gera o PDF repetidamente, confirma uma unica pagina para a venda de referencia e extrai o texto para garantir a ausencia dos dois rotulos de via.

## Arquivos do marco 4

- `backend/src/test/java/com/acaogames/backend/SQLiteDatabaseIntegrationTest.java`

## Validacao final

Executada em 14/08/2026:

- Backend: `backend/mvnw.cmd test` - 11 testes, sem falhas.
- Frontend: `npm run build` - 51 modulos transformados e build de producao concluido.
- Nova venda: venda e movimento criados; os dois indicadores financeiros passam a incluir o valor.
- Edicao de R$ 500,00 para R$ 400,00: venda, movimento e Home passam a R$ 400,00.
- Alteracao de itens e pagamento: total, forma de pagamento e movimento sao recriados de modo coerente na mesma transacao.
- Remocao/desvinculacao: ao apagar o movimento vinculado, a venda permanece no historico, mas deixa de compor os indicadores financeiros; ao apagar a venda, estoque e movimento tambem sao revertidos.
- PDF pelo Caixa: o build inclui a acao que abre o endpoint compartilhado da venda.
- Pesquisa: testes cobrem cliente, venda de balcao, produto/item, ID, equipamento e servico conforme as relacoes existentes.
- Via unica: o PDF foi gerado duas vezes; o documento de referencia possui uma pagina e nao contem "Via do Cliente" nem "Via da Loja".

## Comportamentos substituidos e riscos conhecidos

- "Caixa do dia" deixou de ser a soma paralela de vendas e servicos e agora representa o saldo liquido real do dia em `movimento_caixa`, incluindo saidas e lancamentos manuais.
- `valorTotal` e `subtotal` recebidos na requisicao nao substituem mais os calculos derivados dos itens. Integracoes externas que enviavam descontos apenas nesses campos devem representar o valor final nos valores unitarios dos itens.
- O modelo nao possui status de cancelamento de venda. O fluxo existente continua usando exclusao da venda ou exclusao/desvinculacao do movimento financeiro.
- O modelo tambem nao relaciona venda diretamente a equipamento; nenhum campo ou tabela artificial foi criado para simular essa relacao.

## Historico de commits

- `f9ae35f` - fonte financeira unica do dashboard e testes do ciclo de venda.
- `437ce3c` - comprovante de venda na tela Caixa.
- `bbaa336` - pesquisa relacional de vendas e caixa.
- `186a28f` - garantia automatizada de PDF de venda em via unica.
- `c0244a7` - total da venda derivado dos itens.
