import assert from "node:assert/strict";
import {
  canEditServiceOrder,
  canRegisterServiceOrderExit,
  isServiceOrderWorkflowClosed,
  serviceOrderStatusLabel
} from "../src/utils/serviceOrderStatus.js";
import { compareText, nextSort, sortRows } from "../src/utils/tableSorting.js";

const numericColumn = { value: (row) => row.value, type: "number" };
const dateColumn = { value: (row) => row.value, type: "date" };
const textColumn = { value: (row) => row.value, type: "text" };
const columns = {
  os: numericColumn,
  valor: numericColumn,
  entrada: dateColumn,
  cliente: textColumn,
  status: numericColumn
};

function values(rows) {
  return rows.map((row) => row.value);
}

assert.deepEqual(
  values(sortRows([{ value: 10 }, { value: 2 }, { value: 6 }], { sortBy: "os", sortOrder: "asc" }, columns)),
  [2, 6, 10],
  "OS deve ser ordenada pelo numero real"
);

assert.deepEqual(
  values(sortRows([{ value: 9 }, { value: 90 }, { value: 100 }, { value: 1000 }], { sortBy: "valor", sortOrder: "asc" }, columns)),
  [9, 90, 100, 1000],
  "Dinheiro deve ser ordenado pelo valor numerico"
);

assert.deepEqual(
  values(sortRows(
    [{ value: "2025-12-31" }, { value: "2026-01-01" }, { value: "2024-06-15" }],
    { sortBy: "entrada", sortOrder: "desc" },
    columns
  )),
  ["2026-01-01", "2025-12-31", "2024-06-15"],
  "Datas devem usar o instante real"
);

assert.deepEqual(
  values(sortRows(
    [{ value: "Zoe" }, { value: "Álvaro" }, { value: "ana" }],
    { sortBy: "cliente", sortOrder: "asc" },
    columns
  )),
  ["Álvaro", "ana", "Zoe"],
  "Texto deve usar comparacao pt-BR sem diferenca irrelevante de caixa"
);

assert.equal(compareText("ação", "AÇÃO"), 0, "Comparacao textual deve ignorar caixa");

assert.deepEqual(
  values(sortRows([{ value: 6 }, { value: 0 }, { value: 3 }], { sortBy: "status", sortOrder: "asc" }, columns)),
  [0, 3, 6],
  "Status deve respeitar a ordem operacional fornecida"
);

const filtered = Array.from({ length: 25 }, (_, index) => ({ value: index + 1, keep: (index + 1) % 2 === 1 }))
  .filter((row) => row.keep);
const sortedBeforePagination = sortRows(filtered, { sortBy: "os", sortOrder: "desc" }, columns).slice(0, 5);
assert.deepEqual(values(sortedBeforePagination), [25, 23, 21, 19, 17], "Filtro e ordenacao devem ocorrer antes da pagina");

const allRows = Array.from({ length: 25 }, (_, index) => ({ value: index + 1 }));
assert.deepEqual(
  values(sortRows(allRows, { sortBy: "os", sortOrder: "desc" }, columns).slice(0, 10)),
  [25, 24, 23, 22, 21, 20, 19, 18, 17, 16],
  "A primeira pagina deve conter os maiores valores de todo o conjunto"
);

assert.deepEqual(nextSort({ sortBy: "cliente", sortOrder: "asc" }, "cliente"), { sortBy: "cliente", sortOrder: "desc" });
assert.deepEqual(nextSort({ sortBy: "cliente", sortOrder: "desc" }, "cliente"), { sortBy: "cliente", sortOrder: "asc" });
assert.deepEqual(nextSort({ sortBy: "cliente", sortOrder: "desc" }, "entrada"), { sortBy: "entrada", sortOrder: "asc" });

const stockRows = [
  { idProduto: 1, nome: "Cabo HDMI", categoria: "Cabos", descricao: "Cabo 2 metros", quantidade: "10", minimo: "2", compra: 90, vista: 100, prazo: 120, ativo: true },
  { idProduto: 2, nome: "Controle", categoria: "Controles", descricao: "Sem fio", quantidade: "2", minimo: "10", compra: 1000, vista: 1200, prazo: 1500, ativo: false },
  { idProduto: 3, nome: "Adaptador", categoria: "Cabos", descricao: "Adaptador de vídeo", quantidade: "20", minimo: "5", compra: 9, vista: 20, prazo: 30, ativo: true },
  { idProduto: 4, nome: "Fonte", categoria: "Fontes", descricao: "Fonte bivolt", quantidade: "5", minimo: "20", compra: 100, vista: 200, prazo: 250, ativo: false }
];
const stockColumns = {
  produto: { value: (row) => row.nome, type: "text" },
  categoria: { value: (row) => row.categoria, type: "text" },
  quantidade: { value: (row) => row.quantidade, type: "number" },
  minimo: { value: (row) => row.minimo, type: "number" },
  compra: { value: (row) => row.compra, type: "number" },
  vista: { value: (row) => row.vista, type: "number" },
  prazo: { value: (row) => row.prazo, type: "number" },
  status: { value: (row) => (row.ativo ? 1 : 0), type: "status" }
};
const stockTieBreaker = (left, right) => compareText(left.nome, right.nome) || left.idProduto - right.idProduto;

function stockIds(sortBy, sortOrder, rows = stockRows) {
  return sortRows(rows, { sortBy, sortOrder }, stockColumns, stockTieBreaker).map((row) => row.idProduto);
}

assert.deepEqual(stockIds("produto", "asc"), [3, 1, 2, 4], "Produto deve ordenar de A-Z");
assert.deepEqual(stockIds("produto", "desc"), [4, 2, 1, 3], "Produto deve ordenar de Z-A");
assert.deepEqual(stockIds("categoria", "asc"), [3, 1, 2, 4], "Categoria deve usar ordem textual");
assert.deepEqual(stockIds("categoria", "desc"), [4, 2, 3, 1], "Categoria deve inverter a ordem textual");
assert.deepEqual(stockIds("quantidade", "asc"), [2, 4, 1, 3], "Quantidade deve usar comparacao numerica");
assert.deepEqual(stockIds("quantidade", "desc"), [3, 1, 4, 2], "Quantidade deve inverter a comparacao numerica");
assert.deepEqual(stockIds("minimo", "asc"), [1, 3, 2, 4], "Estoque minimo deve usar comparacao numerica");
assert.deepEqual(stockIds("minimo", "desc"), [4, 2, 3, 1], "Estoque minimo deve inverter a comparacao numerica");
assert.deepEqual(stockIds("compra", "asc"), [3, 1, 4, 2], "Compra deve usar o valor monetario real");
assert.deepEqual(stockIds("compra", "desc"), [2, 4, 1, 3], "Compra deve inverter o valor monetario real");
assert.deepEqual(stockIds("vista", "asc"), [3, 1, 4, 2], "Preco a vista deve usar o valor monetario real");
assert.deepEqual(stockIds("vista", "desc"), [2, 4, 1, 3], "Preco a vista deve usar o valor monetario real");
assert.deepEqual(stockIds("prazo", "asc"), [3, 1, 4, 2], "Preco a prazo deve usar o valor monetario real");
assert.deepEqual(stockIds("prazo", "desc"), [2, 4, 1, 3], "Preco a prazo deve inverter o valor monetario real");
assert.deepEqual(stockIds("status", "asc"), [2, 4, 3, 1], "Status deve agrupar Inativo antes de Ativo na ordem crescente");
assert.deepEqual(stockIds("status", "desc"), [3, 1, 2, 4], "Status deve agrupar Ativo antes de Inativo na ordem decrescente");

const stockSearch = stockRows.filter((row) => [row.nome, row.categoria, row.descricao]
  .some((value) => String(value).toLocaleLowerCase("pt-BR").includes("cabo")));
assert.deepEqual(stockIds("quantidade", "desc", stockSearch), [3, 1], "Pesquisa deve ocorrer antes da ordenacao do estoque");

assert.equal(serviceOrderStatusLabel("CANCELADO"), "Não autorizado", "Status legado deve usar o novo rotulo");
assert.equal(isServiceOrderWorkflowClosed("CANCELADO"), true, "Nao autorizado deve permanecer fora do fluxo tecnico ativo");
assert.equal(canEditServiceOrder("CANCELADO"), true, "Nao autorizado deve poder ser alterado novamente");
assert.equal(canRegisterServiceOrderExit("CANCELADO"), true, "Nao autorizado deve permitir a devolucao do equipamento");
assert.equal(canRegisterServiceOrderExit("ENTREGUE"), false, "Ordem entregue nao deve permitir uma segunda saida");
assert.equal(canEditServiceOrder("ENTREGUE"), false, "Ordem entregue deve permanecer bloqueada");

console.log("Ordenacao de tabelas e regras de status de OS: cenarios aprovados.");
