# Refinamento da Home / Dashboard

## Escopo

A Home continua sendo implementada em `frontend/src/App.jsx`. Os estilos compartilhados do tema
base/escuro ficam em `frontend/src/styles.css`; os ajustes do tema claro ficam em
`frontend/src/current-design.css`. Nenhum endpoint, tabela ou calculo do dashboard foi alterado.

## Dados reutilizados

A tela continua usando `GET /api/dashboard` e as listas que ja eram carregadas na inicializacao.
Foram aproveitados:

- `ordensAbertas`, `ordensProntas`, `vendasDoDia`, `caixaDoDia` e `produtosEstoqueBaixo`;
- `ordensPorStatus` para a quantidade aguardando aprovacao;
- a lista de ordens ja carregada para localizar a OS pronta mais antiga;
- `produtosCadastrados` para informar quantos produtos sao monitorados;
- `idVenda`, `formaPagamento`, `dataVenda` e `valorTotal` das vendas recentes.

O dashboard fornece somente o identificador do cliente nas vendas recentes. O nome do cliente nao
foi exibido porque isso exigiria ampliar a consulta/resposta do backend. `dataVenda` guarda apenas a
data, portanto nenhum horario ficticio foi apresentado.

## Melhorias aplicadas

- Central de Atendimento e atalhos com menor altura e espacamento vertical.
- Valor dos indicadores como elemento principal, seguido do titulo e do detalhe opcional.
- Problema da OS limitado a duas linhas, mantendo o texto completo no atributo `title`.
- Acao de PDF compacta com o icone de documento ja existente e rotulo acessivel.
- Link `Ver todas` de OS direcionando para a secao Equipamentos.
- Link `Ver todas` de vendas direcionando para a secao Caixa.
- Vendas recentes separando numero, valor, forma de pagamento e data.
- Formas de pagamento exibidas com os mesmos rotulos usados pelo formulario de venda.
- Cards de vendas mais compactos e adaptacao para telas menores.

## Regras preservadas

- Os cinco calculos do dashboard permanecem no backend existente.
- Os tres atalhos continuam chamando os mesmos handlers.
- O PDF continua usando o mesmo endpoint e a mesma funcao.
- Nao foram adicionadas requisicoes, dependencias ou rotas.
- Autenticacao e barra superior foram preservadas.

## Validacao

O build de producao e os testes automatizados do projeto devem ser executados apos mudancas na
Home. A verificacao visual deve contemplar desktop, tablet e tela pequena nos temas claro e escuro,
com atencao a truncamento, overflow e alinhamento dos dois paineis inferiores.
