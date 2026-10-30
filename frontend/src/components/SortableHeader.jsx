import React from "react";

export default function SortableHeader({ label, sortKey, sort, onSort, className = "" }) {
  const active = sort?.sortBy === sortKey;
  const direction = active ? sort.sortOrder : null;
  const indicator = direction === "asc" ? "↑" : direction === "desc" ? "↓" : "↕";
  const ariaSort = direction === "asc" ? "ascending" : direction === "desc" ? "descending" : "none";
  const nextDirection = active && direction === "asc" ? "decrescente" : "crescente";

  return (
    <th className={`sortable-header ${active ? "sortable-header--active" : ""} ${className}`.trim()} aria-sort={ariaSort}>
      <button
        type="button"
        className="sortable-header__button"
        onClick={() => onSort(sortKey)}
        title={`Ordenar ${label} em ordem ${nextDirection}`}
        aria-label={`Ordenar ${label} em ordem ${nextDirection}`}
      >
        <span>{label}</span>
        <span className="sortable-header__indicator" aria-hidden="true">{indicator}</span>
      </button>
    </th>
  );
}
