import { useEffect, useMemo, useState } from "react";
import { api } from "../services/api";
import arrowDownIcon from "../assets/icons/arrow-down-to-line.svg";
import fileTextIcon from "../assets/icons/file-text.svg";
import searchIcon from "../assets/icons/search.svg";

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL"
});

const reportTabs = [
  { id: "caixa", label: "Caixa" },
  { id: "vendas", label: "Vendas" },
  { id: "ordens", label: "Ordens de Servico" },
  { id: "estoque", label: "Estoque" },
  { id: "clientes", label: "Clientes" }
];

const STALE_ORDER_DAYS = 7;

function createEmptyReportData() {
  return {
    movimentos: [],
    vendas: [],
    ordens: [],
    produtos: [],
    clientes: [],
    movimentacoesEstoque: []
  };
}

export default function RelatoriosPage({ onFeedback }) {
  const [activeReport, setActiveReport] = useState("caixa");
  const [period, setPeriod] = useState(() => ({
    start: firstDayOfMonthIsoDate(),
    end: todayIsoDate()
  }));
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [reportData, setReportData] = useState(createEmptyReportData);

  useEffect(() => {
    refreshReports();
  }, []);

  const reportContext = useMemo(
    () => buildReportContext(reportData, period, search),
    [reportData, period, search]
  );

  function notify(feedback) {
    if (onFeedback) {
      onFeedback(feedback);
    }
  }

  async function refreshReports() {
    setLoading(true);
    notify(null);

    try {
      const [
        movimentosData,
        vendasData,
        ordensData,
        produtosData,
        clientesData,
        movimentacoesData
      ] = await Promise.all([
        api.get("/movimentos-caixa"),
        api.get("/vendas"),
        api.get("/ordens-servico"),
        api.get("/produtos"),
        api.get("/clientes"),
        api.get("/movimentacoes-estoque")
      ]);

      setReportData({
        movimentos: Array.isArray(movimentosData) ? movimentosData : [],
        vendas: Array.isArray(vendasData) ? vendasData : [],
        ordens: Array.isArray(ordensData) ? ordensData : [],
        produtos: Array.isArray(produtosData) ? produtosData : [],
        clientes: Array.isArray(clientesData) ? clientesData : [],
        movimentacoesEstoque: Array.isArray(movimentacoesData) ? movimentacoesData : []
      });
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel carregar os relatorios."
      });
    } finally {
      setLoading(false);
    }
  }

  function updatePeriod(field, value) {
    setPeriod((current) => ({
      ...current,
      [field]: value
    }));
  }

  function handleExportCsv() {
    const exportData = getExportData(activeReport, reportContext);
    const csv = toCsv(exportData.headers, exportData.rows);
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

  function handlePrint() {
    window.print();
  }

  return (
    <section className="management-page reports-page" aria-labelledby="reports-title">
      <div className="management-hero reports-hero">
        <div>
          <span className="section-label">Analise operacional</span>
          <h1 id="reports-title">Relatorios</h1>
          <p>Fechamentos por periodo para caixa, vendas, ordens, estoque e clientes.</p>
        </div>
        <div className="management-actions reports-actions">
          <button type="button" className="ghost-button" onClick={refreshReports} disabled={loading}>
            {loading ? "Atualizando..." : "Atualizar"}
          </button>
          <button type="button" className="secondary-button reports-action-button" onClick={handleExportCsv}>
            <SvgIcon src={arrowDownIcon} size={15} />
            CSV
          </button>
          <button type="button" className="primary-button reports-action-button" onClick={handlePrint}>
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
              onClick={() => setActiveReport(tab.id)}
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
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Buscar no relatorio atual..."
          />
        </label>
      </section>

      {loading ? (
        <div className="empty-state">Carregando relatorios...</div>
      ) : (
        renderReport(activeReport, reportContext)
      )}
    </section>
  );
}

function renderReport(activeReport, context) {
  if (activeReport === "vendas") {
    return <VendasReport report={context.vendas} />;
  }

  if (activeReport === "ordens") {
    return <OrdensReport report={context.ordens} />;
  }

  if (activeReport === "estoque") {
    return <EstoqueReport report={context.estoque} />;
  }

  if (activeReport === "clientes") {
    return <ClientesReport report={context.clientes} />;
  }

  return <CaixaReport report={context.caixa} />;
}

function CaixaReport({ report }) {
  return (
    <>
      <section className="report-summary-grid" aria-label="Resumo do caixa">
        <SummaryCard label="Entradas" value={currencyFormatter.format(report.entradas)} tone="active" />
        <SummaryCard label="Saidas" value={currencyFormatter.format(report.saidas)} tone="muted" />
        <SummaryCard label="Saldo" value={currencyFormatter.format(report.saldo)} tone={report.saldo < 0 ? "muted" : "gold"} />
        <SummaryCard label="Movimentos" value={report.rows.length} />
      </section>

      <section className="report-main-grid">
        <ReportPanel title="Movimentos de caixa">
          <div className="table-wrap">
            <table className="compact-table clients-table reports-table">
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Tipo</th>
                  <th>Categoria</th>
                  <th>Descricao</th>
                  <th>Pagamento</th>
                  <th>Vinculo</th>
                  <th>Valor</th>
                </tr>
              </thead>
              <tbody>
                {report.rows.length ? report.rows.map((movimento) => (
                  <tr key={movimento.idMovimentoCaixa}>
                    <td>{formatDateTime(movimento.dataMovimento)}</td>
                    <td>{humanizeLabel(movimento.tipo)}</td>
                    <td>{humanizeLabel(movimento.categoria)}</td>
                    <td>{movimento.descricao || "Nao informado"}</td>
                    <td>{humanizePayment(movimento.formaPagamento)}</td>
                    <td>{movementLinkLabel(movimento)}</td>
                    <td className={isEntrada(movimento) ? "report-money report-money--positive" : "report-money report-money--negative"}>
                      {isEntrada(movimento) ? "+" : "-"}{currencyFormatter.format(parseMoney(movimento.valor))}
                    </td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan="7">Nenhum movimento encontrado no periodo.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </ReportPanel>

        <ReportSidePanel title="Por forma de pagamento" rows={report.payments} valueFormatter={currencyFormatter.format} />
        <ReportSidePanel title="Por categoria" rows={report.categories} valueFormatter={currencyFormatter.format} />
      </section>
    </>
  );
}

function VendasReport({ report }) {
  return (
    <>
      <section className="report-summary-grid" aria-label="Resumo de vendas">
        <SummaryCard label="Total vendido" value={currencyFormatter.format(report.total)} tone="active" />
        <SummaryCard label="Vendas" value={report.rows.length} />
        <SummaryCard label="Ticket medio" value={currencyFormatter.format(report.ticketMedio)} tone="gold" />
        <SummaryCard label="Itens vendidos" value={report.itensVendidos} />
      </section>

      <section className="report-main-grid">
        <ReportPanel title="Vendas no periodo">
          <div className="table-wrap">
            <table className="compact-table clients-table reports-table">
              <thead>
                <tr>
                  <th>Venda</th>
                  <th>Data</th>
                  <th>Cliente</th>
                  <th>Pagamento</th>
                  <th>Itens</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                {report.rows.length ? report.rows.map((venda) => (
                  <tr key={venda.idVenda}>
                    <td>#{formatNumber(venda.idVenda)}</td>
                    <td>{formatDate(venda.dataVenda)}</td>
                    <td>{venda.clienteNome || "Venda balcao"}</td>
                    <td>{humanizePayment(venda.formaPagamento)}</td>
                    <td>{countVendaItems(venda)}</td>
                    <td className="report-money">{currencyFormatter.format(parseMoney(venda.valorTotal))}</td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan="6">Nenhuma venda encontrada no periodo.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </ReportPanel>

        <ReportSidePanel title="Formas de pagamento" rows={report.payments} valueFormatter={currencyFormatter.format} />
        <ReportSidePanel title="Produtos mais vendidos" rows={report.topProducts} valueFormatter={(value) => `${value} un.`} />
      </section>
    </>
  );
}

function OrdensReport({ report }) {
  return (
    <>
      <section className="report-summary-grid" aria-label="Resumo de ordens de servico">
        <SummaryCard label="Abertas" value={report.abertas} />
        <SummaryCard label="Prontas" value={report.prontas} tone="gold" />
        <SummaryCard label="Entregues" value={report.entregues} tone="active" />
        <SummaryCard label="Paradas 7d+" value={report.paradas} tone={report.paradas ? "muted" : undefined} />
        <SummaryCard label="Valor aberto" value={currencyFormatter.format(report.valorAberto)} />
      </section>

      <section className="report-main-grid">
        <ReportPanel title="Ordens de servico">
          <div className="table-wrap">
            <table className="compact-table clients-table reports-table">
              <thead>
                <tr>
                  <th>OS</th>
                  <th>Cliente</th>
                  <th>Equipamento</th>
                  <th>Status</th>
                  <th>Entrada</th>
                  <th>Idade</th>
                  <th>Valor</th>
                </tr>
              </thead>
              <tbody>
                {report.rows.length ? report.rows.map((ordem) => (
                  <tr key={ordem.idOrdemServico}>
                    <td>#{formatNumber(ordem.idOrdemServico)}</td>
                    <td>{ordem.cliente?.nome || "Cliente nao informado"}</td>
                    <td>{equipmentLabel(ordem.equipamento)}</td>
                    <td>{humanizeStatus(ordem.status)}</td>
                    <td>{formatDate(ordem.dataEntrada)}</td>
                    <td>{orderAgeLabel(ordem)}</td>
                    <td className="report-money">{currencyFormatter.format(parseMoney(ordem.valorServico))}</td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan="7">Nenhuma OS encontrada no periodo.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </ReportPanel>

        <ReportSidePanel title="Status das OS" rows={report.statusRows} valueFormatter={(value) => `${value}`} />
        <ReportSidePanel title="OS paradas" rows={report.staleRows} valueFormatter={(value) => `${value} dias`} />
      </section>
    </>
  );
}

function EstoqueReport({ report }) {
  return (
    <>
      <section className="report-summary-grid" aria-label="Resumo de estoque">
        <SummaryCard label="Produtos" value={report.rows.length} />
        <SummaryCard label="Ativos" value={report.ativos} tone="active" />
        <SummaryCard label="Estoque baixo" value={report.baixo} tone={report.baixo ? "muted" : undefined} />
        <SummaryCard label="Zerados" value={report.zerados} tone={report.zerados ? "muted" : undefined} />
        <SummaryCard label="Valor estoque" value={currencyFormatter.format(report.valorTotal)} tone="gold" />
      </section>

      <section className="report-main-grid">
        <ReportPanel title="Posicao atual do estoque">
          <div className="table-wrap">
            <table className="compact-table clients-table reports-table">
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Categoria</th>
                  <th>Qtd</th>
                  <th>Minimo</th>
                  <th>Venda</th>
                  <th>Total</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {report.rows.length ? report.rows.map((produto) => (
                  <tr key={produto.idProduto}>
                    <td>{produto.nome}</td>
                    <td>{produto.categoria || "Nao informado"}</td>
                    <td>{produto.quantidadeEstoque || 0}</td>
                    <td>{produto.estoqueMinimo || 0}</td>
                    <td>{currencyFormatter.format(parseMoney(produto.valorVenda))}</td>
                    <td className="report-money">{currencyFormatter.format(productStockValue(produto))}</td>
                    <td>{stockStatus(produto)}</td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan="7">Nenhum produto encontrado.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </ReportPanel>

        <ReportSidePanel title="Movimentacoes no periodo" rows={report.movementsByProduct} valueFormatter={(value) => `${value} un.`} />
        <ReportSidePanel title="Resumo de movimentos" rows={report.movementsSummary} valueFormatter={(value) => `${value} un.`} />
      </section>
    </>
  );
}

function ClientesReport({ report }) {
  return (
    <>
      <section className="report-summary-grid" aria-label="Resumo de clientes">
        <SummaryCard label="Clientes" value={report.rows.length} />
        <SummaryCard label="Ativos" value={report.ativos} tone="active" />
        <SummaryCard label="Inativos" value={report.inativos} tone={report.inativos ? "muted" : undefined} />
        <SummaryCard label="Atendidos" value={report.atendidos} tone="gold" />
        <SummaryCard label="Receita vinculada" value={currencyFormatter.format(report.receita)} />
      </section>

      <section className="report-main-grid">
        <ReportPanel title="Clientes no periodo">
          <div className="table-wrap">
            <table className="compact-table clients-table reports-table">
              <thead>
                <tr>
                  <th>Cliente</th>
                  <th>Telefone</th>
                  <th>Status</th>
                  <th>OS</th>
                  <th>Vendas</th>
                  <th>Servicos</th>
                  <th>Compras</th>
                </tr>
              </thead>
              <tbody>
                {report.rows.length ? report.rows.map((cliente) => (
                  <tr key={cliente.idCliente}>
                    <td>{cliente.nome}</td>
                    <td>{cliente.telefone || "Nao informado"}</td>
                    <td>{cliente.ativo ? "Ativo" : "Inativo"}</td>
                    <td>{cliente.totalOrdens}</td>
                    <td>{cliente.totalVendas}</td>
                    <td>{currencyFormatter.format(cliente.valorServicos)}</td>
                    <td className="report-money">{currencyFormatter.format(cliente.valorVendas)}</td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan="7">Nenhum cliente encontrado.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </ReportPanel>

        <ReportSidePanel title="Clientes por receita" rows={report.topClientes} valueFormatter={currencyFormatter.format} />
        <ReportSidePanel title="Clientes por OS" rows={report.topOrdens} valueFormatter={(value) => `${value}`} />
      </section>
    </>
  );
}

function ReportPanel({ title, children }) {
  return (
    <article className="panel report-panel">
      <div className="panel-head">
        <h2>{title}</h2>
      </div>
      {children}
    </article>
  );
}

function ReportSidePanel({ title, rows, valueFormatter }) {
  return (
    <article className="panel report-side-panel">
      <div className="panel-head">
        <h2>{title}</h2>
      </div>
      <div className="report-side-list">
        {rows.length ? rows.map((row) => (
          <div className="report-side-row" key={row.label}>
            <span>{row.label}</span>
            <strong>{valueFormatter(row.value)}</strong>
          </div>
        )) : (
          <div className="empty-state">Sem dados para exibir.</div>
        )}
      </div>
    </article>
  );
}

function SummaryCard({ label, value, tone }) {
  const toneClass = tone === "active"
    ? "client-summary-card--active"
    : tone === "muted"
    ? "client-summary-card--muted"
    : tone === "gold"
    ? "client-summary-card--gold"
    : "";

  return (
    <article className={`client-summary-card ${toneClass}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
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

function buildReportContext(data, period, search) {
  const clientesById = createIdMap(data.clientes, "idCliente");
  const produtosById = createIdMap(data.produtos, "idProduto");
  const movimentosPeriod = data.movimentos.filter((movimento) => isInPeriod(movimento.dataMovimento, period));
  const vendasPeriod = data.vendas.filter((venda) => isInPeriod(venda.dataVenda, period));
  const ordensPeriod = data.ordens.filter((ordem) => isInPeriod(ordem.dataEntrada, period));
  const movimentacoesPeriod = data.movimentacoesEstoque.filter((movimentacao) => (
    isInPeriod(movimentacao.dataMovimentacao, period)
  ));

  return {
    caixa: buildCaixaReport(movimentosPeriod, search),
    vendas: buildVendasReport(vendasPeriod, search, clientesById, produtosById),
    ordens: buildOrdensReport(ordensPeriod, search),
    estoque: buildEstoqueReport(data.produtos, movimentacoesPeriod, search, produtosById),
    clientes: buildClientesReport(data.clientes, vendasPeriod, ordensPeriod, search)
  };
}

function buildCaixaReport(movimentos, search) {
  const rows = movimentos.filter((movimento) => matchesSearch([
    movimento.idMovimentoCaixa,
    movimento.tipo,
    movimento.categoria,
    movimento.descricao,
    movimento.formaPagamento,
    movimento.venda?.idVenda ? `venda ${movimento.venda.idVenda}` : "",
    movimento.ordemServico?.idOrdemServico ? `os ${movimento.ordemServico.idOrdemServico}` : ""
  ], search));

  const entradas = sum(rows.filter(isEntrada), "valor");
  const saidas = sum(rows.filter((movimento) => !isEntrada(movimento)), "valor");

  return {
    rows,
    entradas,
    saidas,
    saldo: entradas - saidas,
    payments: groupMoneyRows(rows, (movimento) => humanizePayment(movimento.formaPagamento), (movimento) => signedMovementValue(movimento)),
    categories: groupMoneyRows(rows, (movimento) => humanizeLabel(movimento.categoria), (movimento) => signedMovementValue(movimento))
  };
}

function buildVendasReport(vendas, search, clientesById, produtosById) {
  const enriched = vendas.map((venda) => {
    const clienteId = venda.cliente?.idCliente;
    const cliente = clienteId ? clientesById.get(String(clienteId)) : null;

    return {
      ...venda,
      clienteNome: cliente?.nome || venda.cliente?.nome || ""
    };
  });

  const rows = enriched.filter((venda) => matchesSearch([
    venda.idVenda,
    venda.clienteNome,
    venda.formaPagamento,
    venda.valorTotal,
    ...(venda.itens || []).map((item) => productName(item.produto?.idProduto, produtosById))
  ], search));

  const total = sum(rows, "valorTotal");
  const itensVendidos = rows.reduce((totalItens, venda) => totalItens + countVendaItems(venda), 0);

  return {
    rows,
    total,
    itensVendidos,
    ticketMedio: rows.length ? total / rows.length : 0,
    payments: groupMoneyRows(rows, (venda) => humanizePayment(venda.formaPagamento), (venda) => parseMoney(venda.valorTotal)),
    topProducts: topProductsFromSales(rows, produtosById)
  };
}

function buildOrdensReport(ordens, search) {
  const rows = ordens.filter((ordem) => matchesSearch([
    ordem.idOrdemServico,
    ordem.cliente?.nome,
    ordem.cliente?.telefone,
    equipmentLabel(ordem.equipamento),
    ordem.problemaRelatado,
    ordem.status,
    ordem.diagnostico,
    ordem.servicoRealizado
  ], search));

  const abertasRows = rows.filter(isOpenOrder);
  const prontasRows = rows.filter((ordem) => normalize(ordem.status) === "PRONTO");
  const entreguesRows = rows.filter((ordem) => normalize(ordem.status) === "ENTREGUE");
  const staleOrders = abertasRows
    .filter((ordem) => daysSince(ordem.dataEntrada) >= STALE_ORDER_DAYS)
    .map((ordem) => ({
      label: `OS #${formatNumber(ordem.idOrdemServico)}`,
      value: daysSince(ordem.dataEntrada)
    }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 8);

  return {
    rows,
    abertas: abertasRows.length,
    prontas: prontasRows.length,
    entregues: entreguesRows.length,
    paradas: staleOrders.length,
    valorAberto: sum(abertasRows, "valorServico"),
    statusRows: groupCountRows(rows, (ordem) => humanizeStatus(ordem.status)),
    staleRows: staleOrders
  };
}

function buildEstoqueReport(produtos, movimentacoes, search, produtosById) {
  const rows = produtos.filter((produto) => matchesSearch([
    produto.nome,
    produto.categoria,
    produto.descricao,
    produto.quantidadeEstoque,
    produto.ativo ? "ativo" : "inativo",
    stockStatus(produto)
  ], search));

  const filteredProductIds = new Set(rows.map((produto) => String(produto.idProduto)));
  const movements = movimentacoes.filter((movimentacao) => {
    const idProduto = String(movimentacao.produto?.idProduto || "");
    const produto = produtosById.get(idProduto);

    return filteredProductIds.has(idProduto) && matchesSearch([
      productName(idProduto, produtosById),
      produto?.categoria,
      movimentacao.tipo,
      movimentacao.motivo,
      movimentacao.observacoes
    ], search);
  });

  return {
    rows,
    ativos: rows.filter((produto) => produto.ativo !== false).length,
    baixo: rows.filter(isLowStock).length,
    zerados: rows.filter((produto) => Number(produto.quantidadeEstoque || 0) <= 0).length,
    valorTotal: rows.reduce((total, produto) => total + productStockValue(produto), 0),
    movementsByProduct: topStockMovements(movements, produtosById),
    movementsSummary: groupCountQuantityRows(movements, (movimentacao) => humanizeLabel(movimentacao.tipo))
  };
}

function buildClientesReport(clientes, vendas, ordens, search) {
  const rows = clientes
    .filter((cliente) => matchesSearch([
      cliente.nome,
      cliente.telefone,
      cliente.cpf,
      cliente.email,
      cliente.endereco,
      cliente.ativo ? "ativo" : "inativo"
    ], search))
    .map((cliente) => {
      const clienteId = String(cliente.idCliente);
      const clienteVendas = vendas.filter((venda) => String(venda.cliente?.idCliente || "") === clienteId);
      const clienteOrdens = ordens.filter((ordem) => String(ordem.cliente?.idCliente || "") === clienteId);

      return {
        ...cliente,
        totalVendas: clienteVendas.length,
        totalOrdens: clienteOrdens.length,
        valorVendas: sum(clienteVendas, "valorTotal"),
        valorServicos: sum(clienteOrdens, "valorServico")
      };
    });

  const clientesComReceita = rows
    .map((cliente) => ({
      label: cliente.nome,
      value: cliente.valorVendas + cliente.valorServicos
    }))
    .filter((row) => row.value > 0)
    .sort((a, b) => b.value - a.value)
    .slice(0, 8);

  const clientesPorOrdens = rows
    .map((cliente) => ({
      label: cliente.nome,
      value: cliente.totalOrdens
    }))
    .filter((row) => row.value > 0)
    .sort((a, b) => b.value - a.value)
    .slice(0, 8);

  return {
    rows,
    ativos: rows.filter((cliente) => cliente.ativo !== false).length,
    inativos: rows.filter((cliente) => cliente.ativo === false).length,
    atendidos: rows.filter((cliente) => cliente.totalVendas > 0 || cliente.totalOrdens > 0).length,
    receita: rows.reduce((total, cliente) => total + cliente.valorVendas + cliente.valorServicos, 0),
    topClientes: clientesComReceita,
    topOrdens: clientesPorOrdens
  };
}

function getExportData(activeReport, context) {
  if (activeReport === "vendas") {
    return {
      headers: ["Venda", "Data", "Cliente", "Pagamento", "Itens", "Total"],
      rows: context.vendas.rows.map((venda) => [
        formatNumber(venda.idVenda),
        formatDate(venda.dataVenda),
        venda.clienteNome || "Venda balcao",
        humanizePayment(venda.formaPagamento),
        countVendaItems(venda),
        currencyFormatter.format(parseMoney(venda.valorTotal))
      ])
    };
  }

  if (activeReport === "ordens") {
    return {
      headers: ["OS", "Cliente", "Equipamento", "Status", "Entrada", "Idade", "Valor"],
      rows: context.ordens.rows.map((ordem) => [
        formatNumber(ordem.idOrdemServico),
        ordem.cliente?.nome || "Cliente nao informado",
        equipmentLabel(ordem.equipamento),
        humanizeStatus(ordem.status),
        formatDate(ordem.dataEntrada),
        orderAgeLabel(ordem),
        currencyFormatter.format(parseMoney(ordem.valorServico))
      ])
    };
  }

  if (activeReport === "estoque") {
    return {
      headers: ["Produto", "Categoria", "Quantidade", "Minimo", "Valor venda", "Valor total", "Status"],
      rows: context.estoque.rows.map((produto) => [
        produto.nome,
        produto.categoria || "Nao informado",
        produto.quantidadeEstoque || 0,
        produto.estoqueMinimo || 0,
        currencyFormatter.format(parseMoney(produto.valorVenda)),
        currencyFormatter.format(productStockValue(produto)),
        stockStatus(produto)
      ])
    };
  }

  if (activeReport === "clientes") {
    return {
      headers: ["Cliente", "Telefone", "Status", "OS", "Vendas", "Servicos", "Compras"],
      rows: context.clientes.rows.map((cliente) => [
        cliente.nome,
        cliente.telefone || "Nao informado",
        cliente.ativo ? "Ativo" : "Inativo",
        cliente.totalOrdens,
        cliente.totalVendas,
        currencyFormatter.format(cliente.valorServicos),
        currencyFormatter.format(cliente.valorVendas)
      ])
    };
  }

  return {
    headers: ["Data", "Tipo", "Categoria", "Descricao", "Pagamento", "Vinculo", "Valor"],
    rows: context.caixa.rows.map((movimento) => [
      formatDateTime(movimento.dataMovimento),
      humanizeLabel(movimento.tipo),
      humanizeLabel(movimento.categoria),
      movimento.descricao || "Nao informado",
      humanizePayment(movimento.formaPagamento),
      movementLinkLabel(movimento),
      `${isEntrada(movimento) ? "+" : "-"}${currencyFormatter.format(parseMoney(movimento.valor))}`
    ])
  };
}

function toCsv(headers, rows) {
  return [headers, ...rows]
    .map((row) => row.map(csvCell).join(";"))
    .join("\n");
}

function csvCell(value) {
  const text = String(value ?? "");
  return `"${text.replace(/"/g, '""')}"`;
}

function createIdMap(items, field) {
  return new Map(items.map((item) => [String(item[field]), item]));
}

function isInPeriod(value, period) {
  const dateKey = dateOnly(value);
  if (!dateKey) {
    return false;
  }

  if (period.start && dateKey < period.start) {
    return false;
  }

  if (period.end && dateKey > period.end) {
    return false;
  }

  return true;
}

function dateOnly(value) {
  if (!value) {
    return "";
  }

  return String(value).slice(0, 10);
}

function matchesSearch(fields, search) {
  const query = String(search || "").trim().toLowerCase();
  if (!query) {
    return true;
  }

  return fields.some((field) => String(field || "").toLowerCase().includes(query));
}

function sum(items, field) {
  return items.reduce((total, item) => total + parseMoney(item[field]), 0);
}

function groupMoneyRows(items, labelFactory, valueFactory) {
  const grouped = new Map();

  items.forEach((item) => {
    const label = labelFactory(item) || "Nao informado";
    grouped.set(label, (grouped.get(label) || 0) + valueFactory(item));
  });

  return Array.from(grouped.entries())
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => Math.abs(b.value) - Math.abs(a.value))
    .slice(0, 8);
}

function groupCountRows(items, labelFactory) {
  const grouped = new Map();

  items.forEach((item) => {
    const label = labelFactory(item) || "Nao informado";
    grouped.set(label, (grouped.get(label) || 0) + 1);
  });

  return Array.from(grouped.entries())
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value);
}

function groupCountQuantityRows(items, labelFactory) {
  const grouped = new Map();

  items.forEach((item) => {
    const label = labelFactory(item) || "Nao informado";
    grouped.set(label, (grouped.get(label) || 0) + Number(item.quantidade || 0));
  });

  return Array.from(grouped.entries())
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value);
}

function topProductsFromSales(vendas, produtosById) {
  const grouped = new Map();

  vendas.forEach((venda) => {
    (venda.itens || []).forEach((item) => {
      const idProduto = String(item.produto?.idProduto || "");
      const label = productName(idProduto, produtosById);
      grouped.set(label, (grouped.get(label) || 0) + Number(item.quantidade || 0));
    });
  });

  return Array.from(grouped.entries())
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 8);
}

function topStockMovements(movimentacoes, produtosById) {
  const grouped = new Map();

  movimentacoes.forEach((movimentacao) => {
    const idProduto = String(movimentacao.produto?.idProduto || "");
    const label = productName(idProduto, produtosById);
    grouped.set(label, (grouped.get(label) || 0) + Number(movimentacao.quantidade || 0));
  });

  return Array.from(grouped.entries())
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 8);
}

function productName(idProduto, produtosById) {
  const produto = produtosById.get(String(idProduto));
  return produto?.nome || `Produto #${idProduto || "0"}`;
}

function productStockValue(produto) {
  return Number(produto.quantidadeEstoque || 0) * parseMoney(produto.valorVenda);
}

function stockStatus(produto) {
  if (produto.ativo === false) {
    return "Inativo";
  }

  if (Number(produto.quantidadeEstoque || 0) <= 0) {
    return "Zerado";
  }

  if (isLowStock(produto)) {
    return "Estoque baixo";
  }

  return "OK";
}

function isLowStock(produto) {
  return Number(produto.quantidadeEstoque || 0) <= Number(produto.estoqueMinimo || 0);
}

function isEntrada(movimento) {
  return String(movimento.tipo || "").toUpperCase() === "ENTRADA";
}

function signedMovementValue(movimento) {
  return isEntrada(movimento) ? parseMoney(movimento.valor) : -parseMoney(movimento.valor);
}

function movementLinkLabel(movimento) {
  if (movimento.venda?.idVenda) {
    return `Venda #${formatNumber(movimento.venda.idVenda)}`;
  }

  if (movimento.ordemServico?.idOrdemServico) {
    return `OS #${formatNumber(movimento.ordemServico.idOrdemServico)}`;
  }

  return "Manual";
}

function countVendaItems(venda) {
  return (venda.itens || []).reduce((total, item) => total + Number(item.quantidade || 0), 0);
}

function isOpenOrder(ordem) {
  return !["ENTREGUE", "CANCELADO"].includes(normalize(ordem.status));
}

function orderAgeLabel(ordem) {
  const age = daysSince(ordem.dataEntrada);
  if (age < 0) {
    return "Sem data";
  }

  return `${age} dias`;
}

function daysSince(value) {
  const key = dateOnly(value);
  if (!key) {
    return -1;
  }

  const date = new Date(`${key}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return -1;
  }

  const today = new Date();
  const todayDate = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const diffMs = todayDate.getTime() - date.getTime();
  return Math.max(0, Math.floor(diffMs / 86400000));
}

function parseMoney(value) {
  const normalized = String(value || "0").replace(",", ".");
  const number = Number(normalized);
  return Number.isFinite(number) ? number : 0;
}

function humanizePayment(value) {
  if (!value) {
    return "Nao informado";
  }

  const normalized = normalize(value);
  const labels = {
    DINHEIRO: "Dinheiro",
    PIX: "PIX",
    CARTAO_DEBITO: "Cartao debito",
    CARTAO_CREDITO: "Cartao credito"
  };

  return labels[normalized] || humanizeLabel(value);
}

function humanizeStatus(status) {
  if (!status) {
    return "Sem status";
  }

  return humanizeLabel(status);
}

function humanizeLabel(value) {
  if (!value) {
    return "Nao informado";
  }

  return String(value)
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function normalize(value) {
  return String(value || "").trim().toUpperCase();
}

function equipmentLabel(equipamento) {
  if (!equipamento) {
    return "Equipamento nao informado";
  }

  return [equipamento.marca, equipamento.tipoEquipamento, equipamento.modelo]
    .filter(Boolean)
    .join(" - ");
}

function formatDate(value) {
  const key = dateOnly(value);
  if (!key) {
    return "Sem data";
  }

  return new Date(`${key}T00:00:00`).toLocaleDateString("pt-BR");
}

function formatDateTime(value) {
  if (!value) {
    return "Sem data";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return formatDate(value);
  }

  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short"
  });
}

function formatNumber(value) {
  return String(value || 0).padStart(6, "0");
}

function todayIsoDate() {
  return toLocalIsoDate(new Date());
}

function firstDayOfMonthIsoDate() {
  const date = new Date();
  date.setDate(1);
  return toLocalIsoDate(date);
}

function toLocalIsoDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
