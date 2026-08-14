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
- Criacao, edicao de valor, alteracao de itens/pagamento, exclusao e desvinculacao foram cobertas por teste de integracao.

## Arquivos do marco 1

- `backend/src/main/java/com/acaogames/backend/repository/MovimentoCaixaRepository.java`
- `backend/src/main/java/com/acaogames/backend/service/DashboardService.java`
- `backend/src/main/java/com/acaogames/backend/service/VendaService.java`
- `backend/src/test/java/com/acaogames/backend/service/VendaDashboardIntegrationTest.java`
