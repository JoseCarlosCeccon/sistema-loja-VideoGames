const portugueseCollator = new Intl.Collator("pt-BR", {
  sensitivity: "base",
  numeric: true
});

export function nextSort(current, sortBy) {
  if (current?.sortBy === sortBy) {
    return { sortBy, sortOrder: current.sortOrder === "asc" ? "desc" : "asc" };
  }
  return { sortBy, sortOrder: "asc" };
}

export function sortRows(rows, sort, columns, tieBreaker) {
  const column = columns?.[sort?.sortBy];
  if (!column) {
    return [...rows];
  }

  const direction = sort.sortOrder === "desc" ? -1 : 1;
  return [...rows].sort((left, right) => {
    const compared = compareValues(column.value(left), column.value(right), column.type);
    if (compared !== 0) {
      return compared * direction;
    }
    return typeof tieBreaker === "function" ? tieBreaker(left, right) : 0;
  });
}

export function compareText(left, right) {
  return portugueseCollator.compare(String(left ?? ""), String(right ?? ""));
}

function compareValues(left, right, type) {
  if (type === "number" || type === "status") {
    return toFiniteNumber(left) - toFiniteNumber(right);
  }
  if (type === "date") {
    return toTimestamp(left) - toTimestamp(right);
  }
  return compareText(left, right);
}

function toFiniteNumber(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function toTimestamp(value) {
  if (!value) {
    return 0;
  }
  const parsed = new Date(String(value).includes("T") ? value : `${value}T00:00:00`).getTime();
  return Number.isFinite(parsed) ? parsed : 0;
}
