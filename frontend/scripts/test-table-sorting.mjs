import assert from "node:assert/strict";
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

console.log("Ordenacao de tabelas: 10 cenarios aprovados.");
