# Header e status de Ordens de Servico

## Header

O header continua sendo montado em `frontend/src/App.jsx`. O controle de tema preserva o estado
`themeMode`, a persistencia em `localStorage` e a injecao do tema claro. A alteracao foi apenas de
apresentacao: o botao agora exibe somente o simbolo de lua ou sol e informa a acao por `title` e
`aria-label`.

O componente `frontend/src/components/UserMenu.jsx` concentra a identificacao e as acoes do
usuario. O botao mostra iniciais e nome no desktop e somente as iniciais em telas menores. O menu
exibe nome, perfil e a acao Sair. A acao continua recebendo o mesmo callback de logout de
`App.jsx`.

Interacoes suportadas:

- clique ou Enter/Espaco no botao para alternar o menu;
- seta para baixo para abrir e levar o foco para Sair;
- Escape para fechar e devolver o foco ao botao;
- Tab para navegar e fechar ao deixar o componente;
- clique fora para fechar;
- atributos `aria-haspopup`, `aria-expanded`, `aria-controls`, `role=menu` e `role=menuitem`.

## Status de OS

O componente `frontend/src/components/ServiceOrderStatusBadge.jsx` centraliza rotulo e tom visual
dos oito status reais definidos por `OrdemServicoStatus`:

- Recebido: azul;
- Em analise: violeta;
- Aguardando aprovacao: ambar;
- Aprovado: verde-azulado;
- Em manutencao: ciano;
- Pronto: verde;
- Entregue: neutro;
- Cancelado: vermelho.

O componente e utilizado na Home, na tabela de Equipamentos / Ordens de Servico e no historico de
OS do Cliente. Cada badge combina ponto e texto, tem altura minima de 28 px, formato pill e
`white-space: nowrap`. A coluna recebe largura minima moderada para acomodar o maior rotulo.

Os estilos do tema base/escuro ficam em `frontend/src/styles.css`; o tema claro sobrescreve somente
as variaveis de cor em `frontend/src/current-design.css`. Os badges genericos de Caixa, Estoque e
cadastros nao foram modificados.

## Escopo preservado

Nao foram alterados autenticacao, logout, permissoes, endpoints, banco, enum, ordem operacional ou
transicoes de status.
