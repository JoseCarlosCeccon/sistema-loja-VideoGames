# Relatorio comercial de produtos

## Modelo e fonte dos dados

- `venda` representa o cabecalho comercial.
- `item_venda` representa cada produto da venda e relaciona `venda` a `produto`.
- `item_venda.quantidade`, `valor_unitario` e `subtotal` preservam quantidade, preco praticado e faturamento da linha.
- Uma venda e considerada efetiva quando possui uma entrada vinculada em `movimento_caixa`.
- Venda excluida nao participa do relatorio: seus itens sao removidos em cascata e seu movimento financeiro tambem e excluido.
- Edicao de venda recria seus itens e movimento dentro da mesma transacao; o relatorio, por ser derivado dessas tabelas, reflete a alteracao sem totais persistidos paralelos.

O modelo nao possui status de venda, desconto separado ou SKU. A pesquisa usa codigo do produto, nome, categoria e descricao. Um preco promocional ou negociado e representado pelo proprio `item_venda.valor_unitario`.

## Metricas

- Unidades vendidas: `SUM(item_venda.quantidade)`.
- Vendas realizadas: `COUNT(DISTINCT item_venda.id_venda)`.
- Faturamento: `SUM(item_venda.subtotal)`.
- Produtos diferentes: quantidade de produtos com unidades no periodo.
- Preco medio: media ponderada `SUM(subtotal) / SUM(quantidade)`.

O numero de vendas e sempre distinto. Uma venda com cinco unidades de um produto produz cinco unidades e uma venda, inclusive se o produto aparecer em mais de uma linha da mesma venda.

## Periodo anterior

Os limites sao inclusivos. A quantidade de dias e `fim - inicio + 1`; o periodo anterior termina no dia anterior ao inicio e retrocede a mesma quantidade. Se o valor anterior for zero, a comparacao percentual e marcada como indisponivel para evitar divisao por zero.

## Produtos sem saida

O filtro `SEM_VENDAS` retorna apenas produtos ativos, com estoque atual maior que zero e sem unidades em vendas efetivas no periodo. Os atalhos de 30, 60 e 90 dias da interface apenas preenchem os filtros gerais de data.

## Lucro e margem

`produto.valor_compra` e o custo atual do cadastro. Como `item_venda` nao preserva o custo na data da venda, ele nao e uma base historica confiavel. Por isso custo, lucro, margem e ranking de lucratividade nao sao calculados nesta entrega. Uma evolucao futura segura deve gravar o custo historico no item da venda no momento da transacao.

## API e desempenho

- `GET /api/relatorios/produtos`: tabela paginada, indicadores, comparacao e rankings.
- `GET /api/relatorios/produtos/{idProduto}/vendas`: resumo e vendas que compoem o detalhe do produto.

As agregacoes sao feitas no SQLite com CTEs, `SUM` e `COUNT(DISTINCT ...)`; o frontend recebe somente produtos agregados. Foram adicionados indices para os relacionamentos de itens e para a verificacao do movimento financeiro. Nao foram adicionadas colunas nem tabelas.

Filtros do endpoint principal:

```text
inicio=YYYY-MM-DD
fim=YYYY-MM-DD
busca=texto
idProduto=numero
categoria=texto
movimentacao=TODOS|COM_VENDAS|SEM_VENDAS
ordenarPor=MAIS_UNIDADES|MAIS_VENDAS|MAIOR_FATURAMENTO|MENOR_FATURAMENTO|MENOS_UNIDADES|NOME_ASC|NOME_DESC
page=0
size=20
```

## Interface, CSV e impressao

- A aba `Produtos` reutiliza datas, pesquisa, atualizacao, CSV, impressao e paginacao da tela de Relatorios.
- Os filtros adicionais ficam em um painel recolhivel e alteram a mesma consulta paginada da tabela.
- Os atalhos de 30, 60 e 90 dias selecionam `SEM_VENDAS` e atualizam o periodo geral.
- Cards exibem as quatro metricas e a variacao contra o periodo anterior quando existe uma base diferente de zero.
- Rankings simples mostram os cinco produtos com mais unidades e com maior faturamento.
- A linha e o botao `Detalhes` abrem um modal com o resumo e o historico paginado das vendas do produto.
- O CSV busca todas as paginas de produtos agregados usando exatamente os mesmos filtros e ordenacao da tela. As seis colunas comerciais sao as mesmas da tabela.
- A impressao reutiliza `window.print()`, inclui titulo, periodo, filtros aplicados, indicadores, rankings e a pagina visivel da tabela. Controles e modal sao ocultados no documento, preservando a regra existente de uma unica impressao.
- O filtro de status nao e exibido porque venda nao possui esse atributo no modelo atual.

## Testes automatizados do backend

`ProdutoRelatorioIntegrationTest` cobre:

- varias unidades em uma venda e varias vendas do mesmo produto;
- produtos diferentes na mesma venda;
- contagem distinta de vendas;
- preco medio ponderado e faturamento;
- edicao de quantidade e preco;
- exclusao e remocao do movimento financeiro;
- periodo anterior equivalente e base anterior zero;
- pesquisa, categoria, ordenacao e paginacao;
- produtos ativos com estoque e sem saida;
- detalhe e historico das vendas do produto.

Validacoes executadas na aplicacao local:

- os endpoints autenticados retornaram 4 unidades, 3 vendas, R$ 275,00 e 3 produtos distintos para os dados de referencia de maio de 2026;
- o detalhe do Cabo USB-C retornou 2 vendas e 2 unidades;
- o filtro sem vendas retornou o produto ativo com estoque esperado;
- o build de producao do React/Vite foi concluido sem erros;
- a revisao visual automatizada ficou indisponivel porque nao havia navegador conectado a sessao de validacao.
