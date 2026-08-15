# Ordenacao padronizada de tabelas

## Experiencia compartilhada

As tabelas de Relatorio de Produtos, Ordens de Servico, Clientes e Estoque usam o componente
`SortableHeader`. O cabecalho e um botao acessivel por teclado e apresenta:

- `↕` quando a coluna esta disponivel;
- `↑` quando a ordem ativa e crescente;
- `↓` quando a ordem ativa e decrescente.

O primeiro clique em uma coluna inativa seleciona ordem crescente. Cliques seguintes alternam
entre crescente e decrescente. Pesquisa, filtros, atualizacao e mudanca de pagina nao limpam a
ordenacao ativa.

## Relatorio de Produtos

Campos ordenaveis: produto, categoria, unidades vendidas, numero de vendas, preco medio e
faturamento. Detalhes nao e ordenavel.

O endpoint `GET /api/relatorios/produtos` recebe:

```text
sortBy=produto|categoria|unidadesVendidas|numeroVendas|precoMedio|faturamento
sortOrder=asc|desc
```

O backend mapeia somente esses valores para expressoes SQL conhecidas. A ordem e aplicada depois
dos filtros e da pesquisa e antes de `LIMIT`/`OFFSET`. Nome do produto e identificador sao usados
como desempate. O estado inicial continua sendo unidades vendidas em ordem decrescente.

CSV busca todas as paginas com os mesmos `sortBy` e `sortOrder`. A impressao/PDF usa a tabela ja
ordenada exibida na tela.

## Ordens de Servico

A API fornece a lista completa e a tela nao possui paginacao. Por isso a sequencia local e:

1. pesquisar em todo o conjunto;
2. ordenar todo o resultado;
3. renderizar.

Campos ordenaveis: numero da OS, cliente, equipamento, status, entrada e valor. Problema e Acoes
nao sao ordenaveis. Numero e valor usam comparacao numerica; entrada usa `Date`; texto usa
`Intl.Collator` para `pt-BR`.

Status segue a ordem existente em `OrdemServicoStatus`: Recebido, Em analise, Aguardando
aprovacao, Aprovado, Em manutencao, Pronto, Entregue e Cancelado. O estado inicial mudou de numero
da OS decrescente para entrada mais recente primeiro. O numero da OS decrescente e o desempate.

## Clientes

A API tambem fornece a lista completa e nao existe paginacao. A pesquisa e aplicada antes da
ordenacao. Campos ordenaveis: nome, status Ativo/Inativo e data de cadastro. Telefone, CPF, email e
Acoes nao sao ordenaveis. O estado inicial permanece nome crescente; nome e identificador sao os
desempates.

## Estoque

A API fornece a lista completa de produtos e a tela nao possui paginacao. A pesquisa por produto,
categoria ou descricao e aplicada sobre o conjunto completo; em seguida o resultado e ordenado e
renderizado. Assim, a ordenacao nao fica limitada a uma pagina ou subconjunto visivel.

Campos ordenaveis: produto, categoria, quantidade, estoque minimo, compra, preco a vista, preco a
prazo e status. Acoes nao e ordenavel. Quantidade e minimo usam comparacao numerica. Compra, a
vista e a prazo usam os valores numericos crus, nunca o texto formatado em reais.

O status cadastral existente e somente Ativo/Inativo. Na ordem crescente, Inativo vem antes de
Ativo; na decrescente, Ativo vem antes de Inativo. O aviso de estoque `Baixo` e uma condicao
calculada e nao cria um novo status para a ordenacao. O estado inicial preserva Produto A-Z, com
nome e identificador como desempates. Nenhum endpoint foi criado ou alterado.

## Codigo reutilizado

- `frontend/src/components/SortableHeader.jsx`: indicador, clique, foco e atributos ARIA.
- `frontend/src/utils/tableSorting.js`: ciclo de direcao e comparadores de texto, numero e data.
- `frontend/scripts/test-table-sorting.mjs`: testes deterministas sem dependencia do navegador.

## Validacao

O teste frontend cobre:

- OS numerica;
- valores 9, 90, 100 e 1000;
- datas em anos e meses diferentes;
- texto em portugues, acentos e caixa;
- ordem operacional de status;
- filtro antes da ordenacao;
- ordenacao global antes do recorte de uma pagina;
- ciclo crescente/decrescente.
- todas as oito colunas ordenaveis do Estoque nas duas direcoes;
- pesquisa do Estoque antes da ordenacao numerica.

O teste de integracao do backend cobre todas as colunas do Relatorio de Produtos, whitelist de
campos/direcoes e duas paginas ordenadas sobre o conjunto completo.

Validacoes executadas nesta entrega:

- 17 testes Maven aprovados, incluindo 5 do relatorio de produtos;
- cenarios gerais e especificos do Estoque no utilitario frontend aprovados;
- build de producao Vite concluido com 53 modulos;
- chamadas autenticadas confirmaram oito combinacoes de coluna/direcao, duas paginas globais e
  resposta HTTP 400 para campo de ordenacao fora da whitelist;
- a revisao visual automatizada nao foi executada porque nao havia navegador conectado a sessao.
