# Notas de entrada e saida no topo da pagina

## Causa

Quando a antiga segunda via foi removida dos templates Jasper, a via preservada continuou posicionada a partir de `y=397`. Por isso as notas de entrada e saida eram impressas a partir do meio da folha.

## Correcao

- Todos os elementos das notas foram deslocados 397 pontos para cima.
- O cabecalho agora comeca em `y=0`, imediatamente apos a margem superior de impressao.
- A banda `title` foi reduzida de 798 para 401 pontos, mantendo o mesmo espacamento interno e sem alterar os dados exibidos.
- A alteracao foi aplicada aos templates de entrada e de saida.

## Validacao

- Os dois JRXML sao compilados nos testes automatizados.
- Um teste de layout confirma que o primeiro elemento comeca em `y=0`, a banda possui 401 pontos e nenhum elemento ultrapassa seu limite.
- Os PDFs de entrada e saida foram renderizados em A4 e revisados visualmente: ambos possuem uma pagina, iniciam no topo da area imprimivel e nao apresentam cortes ou sobreposicoes.
