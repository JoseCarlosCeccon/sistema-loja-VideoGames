# Campos pesquisaveis nos registros operacionais

## Objetivo

Os selects de entidades nas telas Registrar Entrada, Registrar Saida e Registrar Venda foram substituidos por campos de pesquisa com lista de resultados. O identificador interno continua sendo enviado para a API; o usuario pesquisa e seleciona pelo texto visivel.

## Campos alterados

- Registrar Entrada - cliente: nome, telefone, CPF ou email.
- Registrar Entrada - equipamento: marca, tipo, modelo ou descricao.
- Registrar Saida - ordem de servico: numero da OS, cliente, telefone, equipamento, numero de serie ou problema relatado.
- Registrar Venda - cliente: nome, telefone, CPF ou email. O campo vazio continua representando venda de balcao.
- Registrar Venda - produto: codigo, nome, categoria ou descricao.

Os campos de dominio fechado, como forma de pagamento, garantia, marca e tipo, continuam como select para impedir valores invalidos.

## Comportamento

- A busca ignora diferencas entre maiusculas, minusculas e acentos.
- E possivel navegar pelos resultados com as setas e selecionar com Enter.
- Texto digitado que nao corresponda a uma opcao nao passa na validacao do formulario.
- A selecao continua atualizando os comportamentos existentes, como preencher dados da OS e definir o preco do produto conforme a forma de pagamento.

## Validacao

- `npm run build`: build de producao concluido com 51 modulos transformados.
- Revisao estatica confirmou que os selects de ID de cliente, equipamento, OS e produto foram substituidos nas tres telas.
- A verificacao interativa automatizada nao foi executada porque nenhum navegador estava conectado a sessao de desenvolvimento.
