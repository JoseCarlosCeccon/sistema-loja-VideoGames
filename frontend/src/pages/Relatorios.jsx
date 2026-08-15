import React, { useEffect, useMemo, useState } from "react";
import { api } from "../services/api";
import arrowDownIcon from "../assets/icons/arrow-down-to-line.svg";
import fileTextIcon from "../assets/icons/file-text.svg";
import searchIcon from "../assets/icons/search.svg";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const numberFormatter = new Intl.NumberFormat("pt-BR");
const percentFormatter = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });

const reportTabs = [
  { id: "caixa", label: "Caixa" },
  { id: "vendas", label: "Vendas" },
  { id: "ordens", label: "Ordens de Serviço" },
  { id: "estoque", label: "Estoque" },
  { id: "clientes", label: "Clientes" },
  { id: "produtos", label: "Produtos" }
];

const productSortOptions = [
  ["MAIS_UNIDADES", "Mais vendidos por quantidade"],
  ["MAIS_VENDAS", "Mais vendidos por número de vendas"],
  ["MAIOR_FATURAMENTO", "Maior faturamento"],
  ["MENOR_FATURAMENTO", "Menor faturamento"],
  ["MENOS_UNIDADES", "Menos vendidos"],
  ["NOME_ASC", "Nome A-Z"],
  ["NOME_DESC", "Nome Z-A"]
];

const emptyReport = {
  columns: [], rows: [], summary: {}, comparison: {}, rankings: {},
  page: 0, size: 20, totalElements: 0, totalPages: 0
};

const initialProductFilters = {
  idProduto: "", categoria: "", movimentacao: "TODOS", ordenarPor: "MAIS_UNIDADES"
};

export default function RelatoriosPage({ onFeedback }) {
  const [activeReport, setActiveReport] = useState("caixa");
  const [period, setPeriod] = useState(() => ({ start: firstDayOfMonthIsoDate(), end: todayIsoDate() }));
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [report, setReport] = useState(emptyReport);
  const [productFilters, setProductFilters] = useState(initialProductFilters);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [productOptions, setProductOptions] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [productDetail, setProductDetail] = useState(null);
  const [detailPage, setDetailPage] = useState(0);
  const [detailLoading, setDetailLoading] = useState(false);

  const categories = useMemo(() => (
    [...new Set(productOptions.map((product) => product.categoria).filter(Boolean))]
      .sort((left, right) => left.localeCompare(right, "pt-BR"))
  ), [productOptions]);

  useEffect(() => {
    refreshReport();
  }, [
    activeReport, period.start, period.end, search, page, pageSize,
    productFilters.idProduto, productFilters.categoria,
    productFilters.movimentacao, productFilters.ordenarPor
  ]);

  useEffect(() => {
    if (activeReport !== "produtos" || productOptions.length) return;
    api.get("/produtos")
      .then((data) => setProductOptions(Array.isArray(data) ? data : []))
      .catch(() => setProductOptions([]));
  }, [activeReport, productOptions.length]);

  useEffect(() => {
    if (selectedProduct) loadProductDetail(selectedProduct.id, detailPage);
  }, [selectedProduct?.id, detailPage, period.start, period.end]);

  function notify(feedback) {
    if (onFeedback) onFeedback(feedback);
  }

  function reportParams(requestPage = page, requestSize = pageSize) {
    const params = new URLSearchParams({ page: String(requestPage), size: String(requestSize) });
    if (period.start) params.set("inicio", period.start);
    if (period.end) params.set("fim", period.end);
    if (search.trim()) params.set("busca", search.trim());
    if (activeReport === "produtos") {
      if (productFilters.idProduto) params.set("idProduto", productFilters.idProduto);
      if (productFilters.categoria) params.set("categoria", productFilters.categoria);
      params.set("movimentacao", productFilters.movimentacao);
      params.set("ordenarPor", productFilters.ordenarPor);
    }
    return params;
  }

  async function refreshReport() {
    setLoading(true);
    notify(null);
    try {
      const data = await api.get(`/relatorios/${activeReport}?${reportParams().toString()}`);
      setReport({ ...emptyReport, ...(data || {}) });
    } catch (error) {
      setReport(emptyReport);
      notify({ type: "error", message: error.message || "Não foi possível carregar o relatório." });
    } finally {
      setLoading(false);
    }
  }

  async function loadProductDetail(idProduto, requestedPage) {
    setDetailLoading(true);
    try {
      const params = new URLSearchParams({ page: String(requestedPage), size: "20" });
      if (period.start) params.set("inicio", period.start);
      if (period.end) params.set("fim", period.end);
      const data = await api.get(`/relatorios/produtos/${idProduto}/vendas?${params.toString()}`);
      setProductDetail({ ...emptyReport, ...(data || {}) });
    } catch (error) {
      setProductDetail(null);
      notify({ type: "error", message: error.message || "Não foi possível carregar os detalhes do produto." });
    } finally {
      setDetailLoading(false);
    }
  }

  function updatePeriod(field, value) {
    setPage(0);
    setPeriod((current) => ({ ...current, [field]: value }));
  }

  function selectReport(reportId) {
    setPage(0);
    setSelectedProduct(null);
    setProductDetail(null);
    setActiveReport(reportId);
  }

  function updateProductFilter(field, value) {
    setPage(0);
    setProductFilters((current) => ({ ...current, [field]: value }));
  }

  function applyQuickPeriod(days) {
    const end = new Date();
    const start = new Date(end);
    start.setDate(start.getDate() - (days - 1));
    setPage(0);
    setProductFilters((current) => ({ ...current, movimentacao: "SEM_VENDAS" }));
    setPeriod({ start: localIsoDate(start), end: localIsoDate(end) });
  }

  async function handleExportCsv() {
    setExporting(true);
    try {
      let columns = report.columns || [];
      let rows = report.rows || [];
      if (activeReport === "produtos") {
        const firstBatch = await api.get(`/relatorios/produtos?${reportParams(0, 100).toString()}`);
        columns = firstBatch?.columns || [];
        rows = [...(firstBatch?.rows || [])];
        for (let currentPage = 1; currentPage < Number(firstBatch?.totalPages || 0); currentPage += 1) {
          const batch = await api.get(`/relatorios/produtos?${reportParams(currentPage, 100).toString()}`);
          rows.push(...(batch?.rows || []));
        }
      }

      const blob = new Blob(["\ufeff", toCsv(columns, rows)], { type: "text/csv;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `relatorio-${activeReport}-${period.start || "inicio"}-${period.end || "fim"}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      notify({ type: "success", message: "Relatório exportado em CSV." });
    } catch (error) {
      notify({ type: "error", message: error.message || "Não foi possível exportar o relatório." });
    } finally {
      setExporting(false);
    }
  }

  function openProductDetail(row) {
    setDetailPage(0);
    setProductDetail(null);
    setSelectedProduct(row);
  }

  const isProducts = activeReport === "produtos";

  return (
    <section className="management-page reports-page" aria-labelledby="reports-title">
      <div className="management-hero reports-hero">
        <div>
          <span className="section-label">Análise operacional</span>
          <h1 id="reports-title">Relatórios</h1>
          <p>Fechamentos por período com busca e paginação direto na API.</p>
        </div>
        <div className="management-actions reports-actions">
          <button type="button" className="ghost-button" onClick={refreshReport} disabled={loading}>
            {loading ? "Atualizando..." : "Atualizar"}
          </button>
          <button type="button" className="secondary-button reports-action-button" onClick={handleExportCsv} disabled={exporting || loading}>
            <SvgIcon src={arrowDownIcon} size={15} />
            {exporting ? "Exportando..." : "CSV"}
          </button>
          <button type="button" className="primary-button reports-action-button" onClick={() => window.print()}>
            <SvgIcon src={fileTextIcon} size={15} />
            Imprimir / PDF
          </button>
        </div>
      </div>

      <section className="reports-toolbar" aria-label="Filtros de relatórios">
        <div className="reports-tabs" role="tablist" aria-label="Tipos de relatório">
          {reportTabs.map((tab) => (
            <button key={tab.id} type="button" role="tab" aria-selected={activeReport === tab.id}
              className={activeReport === tab.id ? "reports-tab reports-tab--active" : "reports-tab"}
              onClick={() => selectReport(tab.id)}>
              {tab.label}
            </button>
          ))}
        </div>
        <div className="reports-period">
          <label className="field"><span>Início</span><input type="date" value={period.start} onChange={(event) => updatePeriod("start", event.target.value)} /></label>
          <label className="field"><span>Fim</span><input type="date" value={period.end} onChange={(event) => updatePeriod("end", event.target.value)} /></label>
        </div>
        <label className="search-room reports-search">
          <SvgIcon src={searchIcon} size={15} />
          <input type="search" value={search} onChange={(event) => { setPage(0); setSearch(event.target.value); }} placeholder="Buscar no relatório atual..." />
        </label>
      </section>

      {isProducts ? (
        <ProductFilters open={filtersOpen} onToggle={() => setFiltersOpen((current) => !current)}
          filters={productFilters} products={productOptions} categories={categories}
          onChange={updateProductFilter} onQuickPeriod={applyQuickPeriod}
          onClear={() => { setPage(0); setProductFilters(initialProductFilters); }} />
      ) : null}

      <div className="report-print-context">
        <strong>Relatório: {currentReportLabel(activeReport)}</strong>
        <span>Período: {formatDate(period.start)} a {formatDate(period.end)}</span>
        {isProducts ? <span>{appliedProductFilters(productFilters, productOptions)}</span> : null}
      </div>

      <ReportSummary summary={report.summary || {}} comparison={isProducts ? report.comparison || {} : {}} />
      {isProducts ? <ProductRankings rankings={report.rankings || {}} onSelect={openProductDetail} /> : null}

      <section className="panel report-panel">
        <div className="panel-head">
          <div>
            <h2>{currentReportLabel(activeReport)}</h2>
            {isProducts ? <small>Os valores são derivados das vendas efetivas do período.</small> : null}
          </div>
          <span className="report-count">{numberFormatter.format(report.totalElements || 0)} registros</span>
        </div>
        {loading ? <div className="empty-state">Carregando relatório...</div> : (
          <ReportTable columns={report.columns || []} rows={report.rows || []} onRowClick={isProducts ? openProductDetail : null} />
        )}
      </section>

      <footer className="reports-pagination">
        <button type="button" className="ghost-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={page <= 0 || loading}>Anterior</button>
        <span>Página {numberFormatter.format((report.page || 0) + 1)} de {numberFormatter.format(Math.max(report.totalPages || 1, 1))}</span>
        <button type="button" className="ghost-button" onClick={() => setPage((current) => current + 1)} disabled={loading || (report.totalPages || 0) <= page + 1}>Próxima</button>
        <label className="field reports-page-size">
          <span>Itens</span>
          <select value={pageSize} onChange={(event) => { setPage(0); setPageSize(Number(event.target.value)); }}>
            <option value="10">10</option><option value="20">20</option><option value="50">50</option><option value="100">100</option>
          </select>
        </label>
      </footer>

      {selectedProduct ? (
        <ProductDetailModal product={selectedProduct} detail={productDetail} loading={detailLoading} page={detailPage}
          onPage={setDetailPage} onClose={() => { setSelectedProduct(null); setProductDetail(null); setDetailPage(0); }} />
      ) : null}
    </section>
  );
}

function ProductFilters({ open, onToggle, filters, products, categories, onChange, onQuickPeriod, onClear }) {
  return (
    <section className="product-report-filters">
      <div className="product-report-filters__head">
        <div><strong>Filtros de produtos</strong><span>{activeProductFilterCount(filters)} ativos</span></div>
        <button type="button" className="ghost-button" onClick={onToggle} aria-expanded={open}>{open ? "Ocultar filtros" : "Filtros"}</button>
      </div>
      {open ? (
        <div className="product-report-filters__body">
          <label className="field"><span>Produto</span><select value={filters.idProduto} onChange={(event) => onChange("idProduto", event.target.value)}>
            <option value="">Todos os produtos</option>
            {products.map((product) => <option key={product.idProduto} value={product.idProduto}>{product.nome}</option>)}
          </select></label>
          <label className="field"><span>Categoria</span><select value={filters.categoria} onChange={(event) => onChange("categoria", event.target.value)}>
            <option value="">Todas</option>{categories.map((category) => <option key={category} value={category}>{category}</option>)}
          </select></label>
          <label className="field"><span>Movimentação</span><select value={filters.movimentacao} onChange={(event) => onChange("movimentacao", event.target.value)}>
            <option value="TODOS">Todos</option><option value="COM_VENDAS">Com vendas</option><option value="SEM_VENDAS">Sem vendas</option>
          </select></label>
          <label className="field product-report-sort"><span>Ordenar por</span><select value={filters.ordenarPor} onChange={(event) => onChange("ordenarPor", event.target.value)}>
            {productSortOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select></label>
          <div className="product-report-quick-periods"><span>Sem venda há</span>
            {[30, 60, 90].map((days) => <button key={days} type="button" className="ghost-button" onClick={() => onQuickPeriod(days)}>{days} dias</button>)}
          </div>
          <button type="button" className="ghost-button product-report-clear" onClick={onClear}>Limpar filtros</button>
        </div>
      ) : null}
    </section>
  );
}

function ReportSummary({ summary, comparison }) {
  const excluded = ["id", "produto", "categoria", "estoqueAtual", "ultimaVenda", "precoMedio"];
  const entries = Object.entries(summary).filter(([key]) => !excluded.includes(key));
  if (!entries.length) return null;
  return (
    <section className="report-summary-grid" aria-label="Resumo do relatório">
      {entries.map(([key, value]) => (
        <article key={key} className="report-summary-card">
          <span>{summaryLabel(key)}</span><strong>{formatSummaryValue(key, value)}</strong>
          <ComparisonBadge comparison={comparison[key]} />
        </article>
      ))}
    </section>
  );
}

function ComparisonBadge({ comparison }) {
  if (!comparison) return null;
  if (!comparison.disponivel) return <small className="report-comparison report-comparison--neutral">Sem base anterior</small>;
  const direction = comparison.direcao === "UP" ? "↑" : comparison.direcao === "DOWN" ? "↓" : "→";
  const cssDirection = comparison.direcao === "UP" ? "up" : comparison.direcao === "DOWN" ? "down" : "neutral";
  return <small className={`report-comparison report-comparison--${cssDirection}`}>{direction} {percentFormatter.format(Math.abs(Number(comparison.percentual) || 0))}% vs. período anterior</small>;
}

function ProductRankings({ rankings, onSelect }) {
  return (
    <section className="product-rankings" aria-label="Ranking de produtos">
      <RankingCard title="Mais vendidos" rows={rankings.maisVendidos || []} formatValue={(value) => `${numberFormatter.format(Number(value) || 0)} unidades`} onSelect={onSelect} />
      <RankingCard title="Maior faturamento" rows={rankings.maiorFaturamento || []} formatValue={(value) => currencyFormatter.format(Number(value) || 0)} onSelect={onSelect} />
    </section>
  );
}

function RankingCard({ title, rows, formatValue, onSelect }) {
  return (
    <section className="panel product-ranking-card">
      <div className="panel-head"><h2>{title}</h2></div>
      {rows.length ? <ol className="product-ranking-list">{rows.map((row) => (
        <li key={row.id}><button type="button" onClick={() => onSelect(row)}><span>{row.produto}</span><strong>{formatValue(row.valor)}</strong></button></li>
      ))}</ol> : <div className="empty-state product-ranking-empty">Sem vendas no período.</div>}
    </section>
  );
}

function ReportTable({ columns, rows, onRowClick }) {
  if (!rows.length) return <div className="empty-state">Nenhum registro encontrado para os filtros atuais.</div>;
  return (
    <div className="table-wrap"><table className="compact-table clients-table">
      <thead><tr>{columns.map((column) => <th key={column.key}>{column.label}</th>)}{onRowClick ? <th className="report-details-column">Detalhes</th> : null}</tr></thead>
      <tbody>{rows.map((row, index) => (
        <tr key={row.id || index} className={onRowClick ? "report-row--interactive" : ""}>
          {columns.map((column) => <td key={column.key} onClick={onRowClick ? () => onRowClick(row) : undefined}>{formatCellValue(row[column.key], column.type)}</td>)}
          {onRowClick ? <td className="report-details-column"><button type="button" className="table-action-button" onClick={() => onRowClick(row)}>Detalhes</button></td> : null}
        </tr>
      ))}</tbody>
    </table></div>
  );
}

function ProductDetailModal({ product, detail, loading, page, onPage, onClose }) {
  const summary = detail?.summary || {};
  const canGoNext = detail && (detail.totalPages || 0) > page + 1;
  return (
    <div className="modal-backdrop report-detail-backdrop" role="presentation">
      <section className="entry-modal entry-modal--client-records product-report-detail" role="dialog" aria-modal="true" aria-labelledby="product-report-detail-title">
        <header className="entry-modal__header"><div><span className="eyebrow">Desempenho no período</span><h2 id="product-report-detail-title">{summary.produto || product.produto}</h2><p>{summary.categoria || product.categoria || "Categoria não informada"}</p></div><button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">x</button></header>
        <div className="entry-form product-report-detail__body">
          {loading && !detail ? <div className="empty-state">Carregando detalhes...</div> : <>
            <div className="client-details__grid product-report-detail__summary">
              <DetailItem label="Quantidade vendida" value={numberFormatter.format(Number(summary.unidadesVendidas) || 0)} />
              <DetailItem label="Número de vendas" value={numberFormatter.format(Number(summary.vendasRealizadas) || 0)} />
              <DetailItem label="Faturamento" value={currencyFormatter.format(Number(summary.faturamento) || 0)} />
              <DetailItem label="Preço médio" value={currencyFormatter.format(Number(summary.precoMedio) || 0)} />
              <DetailItem label="Estoque atual" value={numberFormatter.format(Number(summary.estoqueAtual ?? product.estoqueAtual) || 0)} />
              <DetailItem label="Última venda" value={formatDate(summary.ultimaVenda)} />
            </div>
            <section className="product-report-history"><div className="panel-head"><h3>Vendas relacionadas</h3><span className="report-count">{numberFormatter.format(detail?.totalElements || 0)} vendas</span></div><ReportTable columns={detail?.columns || []} rows={detail?.rows || []} /></section>
            <footer className="entry-modal__footer product-detail-pagination">
              <button type="button" className="ghost-button" onClick={() => onPage(Math.max(0, page - 1))} disabled={page <= 0 || loading}>Anterior</button>
              <span>Página {page + 1} de {Math.max(detail?.totalPages || 1, 1)}</span>
              <button type="button" className="ghost-button" onClick={() => onPage(page + 1)} disabled={!canGoNext || loading}>Próxima</button>
              <button type="button" className="primary-button" onClick={onClose}>Fechar</button>
            </footer>
          </>}
        </div>
      </section>
    </div>
  );
}

function DetailItem({ label, value }) {
  return <article className="detail-item"><span>{label}</span><strong>{value === null || value === undefined || value === "" ? "-" : value}</strong></article>;
}

function SvgIcon({ src, size = 18 }) {
  return <span className="svg-icon" style={{ "--icon-url": `url("${src}")`, "--icon-size": `${size}px` }} aria-hidden="true" />;
}

function toCsv(columns, rows) {
  return [columns.map((column) => escapeCsv(column.label)).join(";"), ...rows.map((row) => columns.map((column) => escapeCsv(formatCellValue(row[column.key], column.type))).join(";"))].join("\n");
}

function escapeCsv(value) {
  const text = String(value ?? "");
  return /[;"\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

function formatCellValue(value, type) {
  if (value === null || value === undefined || value === "") return "-";
  if (type === "money") return currencyFormatter.format(Number(value) || 0);
  if (type === "number") return numberFormatter.format(Number(value) || 0);
  if (type === "boolean") return value ? "Sim" : "Não";
  if (type === "date") return formatDate(value);
  if (type === "datetime") return formatDateTime(value);
  return String(value);
}

function formatSummaryValue(key, value) {
  const normalized = String(key).toLowerCase();
  return ["valor", "total", "saldo", "ticket", "faturamento"].some((term) => normalized.includes(term))
    ? currencyFormatter.format(Number(value) || 0) : numberFormatter.format(Number(value) || 0);
}

function summaryLabel(key) {
  return ({ unidadesVendidas: "Unidades vendidas", vendasRealizadas: "Vendas realizadas", faturamentoProdutos: "Faturamento com produtos", produtosDiferentes: "Produtos diferentes vendidos" })[key] || humanizeLabel(key);
}

function currentReportLabel(activeReport) {
  return reportTabs.find((tab) => tab.id === activeReport)?.label || "Relatório";
}

function humanizeLabel(value) {
  return String(value || "").replace(/([a-z])([A-Z])/g, "$1 $2").replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function appliedProductFilters(filters, products) {
  const selectedProduct = products.find((product) => String(product.idProduto) === String(filters.idProduto));
  const movementLabels = { TODOS: "Todos", COM_VENDAS: "Com vendas", SEM_VENDAS: "Sem vendas" };
  const sortLabel = productSortOptions.find(([value]) => value === filters.ordenarPor)?.[1];
  return [`Produto: ${selectedProduct?.nome || "Todos"}`, `Categoria: ${filters.categoria || "Todas"}`, `Movimentação: ${movementLabels[filters.movimentacao]}`, `Ordenação: ${sortLabel}`].join(" | ");
}

function activeProductFilterCount(filters) {
  return [filters.idProduto, filters.categoria, filters.movimentacao !== "TODOS", filters.ordenarPor !== "MAIS_UNIDADES"].filter(Boolean).length;
}

function formatDate(value) {
  const date = parseDate(value);
  return date ? date.toLocaleDateString("pt-BR") : "-";
}

function formatDateTime(value) {
  const date = parseDate(value);
  return date ? date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" }) : "-";
}

function parseDate(value) {
  if (!value) return null;
  const date = new Date(String(value).includes("T") ? value : `${value}T00:00:00`);
  return Number.isNaN(date.getTime()) ? null : date;
}

function localIsoDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function todayIsoDate() { return localIsoDate(new Date()); }
function firstDayOfMonthIsoDate() { const date = new Date(); return localIsoDate(new Date(date.getFullYear(), date.getMonth(), 1)); }
