import { useEffect, useState } from "react";
import { api } from "../services/api";
import arrowDownIcon from "../assets/icons/arrow-down-to-line.svg";
import fileTextIcon from "../assets/icons/file-text.svg";
import searchIcon from "../assets/icons/search.svg";

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL"
});

const numberFormatter = new Intl.NumberFormat("pt-BR");

const reportTabs = [
  { id: "caixa", label: "Caixa" },
  { id: "vendas", label: "Vendas" },
  { id: "ordens", label: "Ordens de Servico" },
  { id: "estoque", label: "Estoque" },
  { id: "clientes", label: "Clientes" }
];

const emptyReport = {
  columns: [],
  rows: [],
  summary: {},
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0
};

export default function RelatoriosPage({ onFeedback }) {
  const [activeReport, setActiveReport] = useState("caixa");
  const [period, setPeriod] = useState(() => ({
    start: firstDayOfMonthIsoDate(),
    end: todayIsoDate()
  }));
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [report, setReport] = useState(emptyReport);

  useEffect(() => {
    refreshReport();
  }, [activeReport, period.start, period.end, search, page, pageSize]);

  function notify(feedback) {
    if (onFeedback) {
      onFeedback(feedback);
    }
  }

  async function refreshReport() {
    setLoading(true);
    notify(null);

    try {
      const params = new URLSearchParams({
        page: String(page),
        size: String(pageSize)
      });

      if (period.start) {
        params.set("inicio", period.start);
      }
      if (period.end) {
        params.set("fim", period.end);
      }
      if (search.trim()) {
        params.set("busca", search.trim());
      }

      const data = await api.get(`/relatorios/${activeReport}?${params.toString()}`);
      setReport({ ...emptyReport, ...(data || {}) });
    } catch (error) {
      setReport(emptyReport);
      notify({
        type: "error",
        message: error.message || "Nao foi possivel carregar o relatorio."
      });
    } finally {
      setLoading(false);
    }
  }

  function updatePeriod(field, value) {
    setPage(0);
    setPeriod((current) => ({
      ...current,
      [field]: value
    }));
  }

  function selectReport(reportId) {
    setPage(0);
    setActiveReport(reportId);
  }

  function updateSearch(value) {
    setPage(0);
    setSearch(value);
  }

  function updatePageSize(value) {
    setPage(0);
    setPageSize(Number(value));
  }

  function handleExportCsv() {
    const csv = toCsv(report.columns || [], report.rows || []);
    const blob = new Blob(["\ufeff", csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `relatorio-${activeReport}-${period.start || "inicio"}-${period.end || "fim"}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);

    notify({ type: "success", message: "Relatorio exportado em CSV." });
  }

  return (
    <section className="management-page reports-page" aria-labelledby="reports-title">
      <div className="management-hero reports-hero">
        <div>
          <span className="section-label">Analise operacional</span>
          <h1 id="reports-title">Relatorios</h1>
          <p>Fechamentos por periodo com busca e paginacao direto na API.</p>
        </div>
        <div className="management-actions reports-actions">
          <button type="button" className="ghost-button" onClick={refreshReport} disabled={loading}>
            {loading ? "Atualizando..." : "Atualizar"}
          </button>
          <button type="button" className="secondary-button reports-action-button" onClick={handleExportCsv}>
            <SvgIcon src={arrowDownIcon} size={15} />
            CSV
          </button>
          <button type="button" className="primary-button reports-action-button" onClick={() => window.print()}>
            <SvgIcon src={fileTextIcon} size={15} />
            Imprimir / PDF
          </button>
        </div>
      </div>

      <section className="reports-toolbar" aria-label="Filtros de relatorios">
        <div className="reports-tabs" role="tablist" aria-label="Tipos de relatorio">
          {reportTabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              role="tab"
              aria-selected={activeReport === tab.id}
              className={activeReport === tab.id ? "reports-tab reports-tab--active" : "reports-tab"}
              onClick={() => selectReport(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="reports-period">
          <label className="field">
            <span>Inicio</span>
            <input type="date" value={period.start} onChange={(event) => updatePeriod("start", event.target.value)} />
          </label>
          <label className="field">
            <span>Fim</span>
            <input type="date" value={period.end} onChange={(event) => updatePeriod("end", event.target.value)} />
          </label>
        </div>

        <label className="search-room reports-search">
          <SvgIcon src={searchIcon} size={15} />
          <input
            type="search"
            value={search}
            onChange={(event) => updateSearch(event.target.value)}
            placeholder="Buscar no relatorio atual..."
          />
        </label>
      </section>

      <ReportSummary summary={report.summary || {}} />

      <section className="panel report-panel">
        <div className="panel-head">
          <h2>{currentReportLabel(activeReport)}</h2>
          <span className="report-count">{numberFormatter.format(report.totalElements || 0)} registros</span>
        </div>

        {loading ? (
          <div className="empty-state">Carregando relatorio...</div>
        ) : (
          <ReportTable columns={report.columns || []} rows={report.rows || []} />
        )}
      </section>

      <footer className="reports-pagination">
        <button type="button" className="ghost-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={page <= 0 || loading}>
          Anterior
        </button>
        <span>
          Pagina {numberFormatter.format((report.page || 0) + 1)} de {numberFormatter.format(Math.max(report.totalPages || 1, 1))}
        </span>
        <button
          type="button"
          className="ghost-button"
          onClick={() => setPage((current) => current + 1)}
          disabled={loading || (report.totalPages || 0) <= page + 1}
        >
          Proxima
        </button>
        <label className="field reports-page-size">
          <span>Itens</span>
          <select value={pageSize} onChange={(event) => updatePageSize(event.target.value)}>
            <option value="10">10</option>
            <option value="20">20</option>
            <option value="50">50</option>
            <option value="100">100</option>
          </select>
        </label>
      </footer>
    </section>
  );
}

function ReportSummary({ summary }) {
  const entries = Object.entries(summary);
  if (!entries.length) {
    return null;
  }

  return (
    <section className="report-summary-grid" aria-label="Resumo do relatorio">
      {entries.map(([key, value]) => (
        <article key={key} className="report-summary-card">
          <span>{humanizeLabel(key)}</span>
          <strong>{formatSummaryValue(key, value)}</strong>
        </article>
      ))}
    </section>
  );
}

function ReportTable({ columns, rows }) {
  if (!rows.length) {
    return <div className="empty-state">Nenhum registro encontrado para os filtros atuais.</div>;
  }

  return (
    <div className="table-wrap">
      <table className="compact-table clients-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key}>{column.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={row.id || index}>
              {columns.map((column) => (
                <td key={column.key}>{formatCellValue(row[column.key], column.type)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function SvgIcon({ src, size = 18 }) {
  return (
    <span
      className="svg-icon"
      style={{
        "--icon-url": `url("${src}")`,
        "--icon-size": `${size}px`
      }}
      aria-hidden="true"
    />
  );
}

function toCsv(columns, rows) {
  const header = columns.map((column) => escapeCsv(column.label)).join(";");
  const body = rows.map((row) => (
    columns.map((column) => escapeCsv(formatCellValue(row[column.key], column.type))).join(";")
  ));
  return [header, ...body].join("\n");
}

function escapeCsv(value) {
  const text = String(value ?? "");
  if (/[;"\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}

function formatCellValue(value, type) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  if (type === "money") {
    return currencyFormatter.format(Number(value) || 0);
  }

  if (type === "number") {
    return numberFormatter.format(Number(value) || 0);
  }

  if (type === "boolean") {
    return value ? "Sim" : "Nao";
  }

  if (type === "date") {
    return formatDate(value);
  }

  if (type === "datetime") {
    return formatDateTime(value);
  }

  return String(value);
}

function formatSummaryValue(key, value) {
  const normalized = String(key).toLowerCase();
  if (
    normalized.includes("valor")
    || normalized.includes("total")
    || normalized.includes("saldo")
    || normalized.includes("ticket")
  ) {
    return currencyFormatter.format(Number(value) || 0);
  }

  return numberFormatter.format(Number(value) || 0);
}

function currentReportLabel(activeReport) {
  return reportTabs.find((tab) => tab.id === activeReport)?.label || "Relatorio";
}

function humanizeLabel(value) {
  return String(value || "")
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatDate(value) {
  const date = parseDate(value);
  if (!date) {
    return "-";
  }

  return date.toLocaleDateString("pt-BR");
}

function formatDateTime(value) {
  const date = parseDate(value);
  if (!date) {
    return "-";
  }

  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short"
  });
}

function parseDate(value) {
  if (!value) {
    return null;
  }

  const date = new Date(String(value).includes("T") ? value : `${value}T00:00:00`);
  return Number.isNaN(date.getTime()) ? null : date;
}

function todayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

function firstDayOfMonthIsoDate() {
  const date = new Date();
  const firstDay = new Date(date.getFullYear(), date.getMonth(), 1);
  const year = firstDay.getFullYear();
  const month = String(firstDay.getMonth() + 1).padStart(2, "0");
  return `${year}-${month}-01`;
}
