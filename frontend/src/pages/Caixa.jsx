import { useEffect, useState } from "react";
import { api } from "../services/api";
import searchIcon from "../assets/icons/search.svg";

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL"
});

const formasPagamento = [
  { value: "", label: "Nao informado" },
  { value: "DINHEIRO", label: "Dinheiro" },
  { value: "PIX", label: "PIX" },
  { value: "CARTAO_DEBITO", label: "Cartao debito" },
  { value: "CARTAO_CREDITO", label: "Cartao credito" }
];

const categoriasCaixa = ["VENDA", "SERVICO", "SUPRIMENTO", "SANGRIA", "DESPESA", "AJUSTE"];

function createEmptyCashForm() {
  return {
    tipo: "ENTRADA",
    descricao: "",
    valor: "",
    dataMovimento: nowDateTimeInput(),
    formaPagamento: "DINHEIRO",
    categoria: "AJUSTE"
  };
}

function movementToForm(movimento) {
  return {
    tipo: movimento?.tipo || "ENTRADA",
    descricao: movimento?.descricao || "",
    valor: movimento?.valor ?? "",
    dataMovimento: toDateTimeInput(movimento?.dataMovimento),
    formaPagamento: movimento?.formaPagamento || "",
    categoria: movimento?.categoria || "AJUSTE"
  };
}

function buildCashPayload(form, selectedMovement) {
  return {
    idVenda: selectedMovement?.venda?.idVenda || null,
    idOrdemServico: selectedMovement?.ordemServico?.idOrdemServico || null,
    tipo: form.tipo,
    descricao: String(form.descricao || "").trim(),
    valor: parseMoney(form.valor),
    dataMovimento: form.dataMovimento || null,
    formaPagamento: optionalText(form.formaPagamento),
    categoria: String(form.categoria || "").trim()
  };
}

function filterMovements(movimentos, search, typeFilter, periodFilter) {
  const query = String(search || "").trim().toLowerCase();

  return movimentos.filter((movimento) => {
    const matchesType = typeFilter === "TODOS" || String(movimento.tipo || "") === typeFilter;
    const matchesPeriod = matchesMovementPeriod(movimento.dataMovimento, periodFilter);
    if (!matchesType || !matchesPeriod) {
      return false;
    }

    if (!query) {
      return true;
    }

    const fields = [
      movimento.idMovimentoCaixa,
      movimento.tipo,
      movimento.descricao,
      movimento.categoria,
      movimento.formaPagamento,
      movimento.venda?.idVenda ? `venda ${movimento.venda.idVenda}` : "",
      movimento.ordemServico?.idOrdemServico ? `os ${movimento.ordemServico.idOrdemServico}` : ""
    ];

    return fields.some((value) => String(value || "").toLowerCase().includes(query));
  });
}

function matchesMovementPeriod(dataMovimento, periodFilter) {
  if (!periodFilter || periodFilter === "TODOS") {
    return true;
  }

  const parsedDate = parseDateOnly(dataMovimento);
  if (!parsedDate) {
    return false;
  }

  const today = new Date();

  if (periodFilter === "DIA") {
    return parsedDate.getTime() === parseDateOnly(today.toISOString())?.getTime();
  }

  if (periodFilter === "MES") {
    return parsedDate.getFullYear() === today.getFullYear() && parsedDate.getMonth() === today.getMonth();
  }

  if (periodFilter === "SEMANA") {
    const weekStart = startOfWeek(today);
    const weekEnd = new Date(weekStart);
    weekEnd.setDate(weekStart.getDate() + 7);
    return parsedDate >= weekStart && parsedDate < weekEnd;
  }

  return true;
}

function parseDateOnly(value) {
  if (!value) {
    return null;
  }

  const date = new Date(String(value).includes("T") ? value : `${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return null;
  }

  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function startOfWeek(date) {
  const normalized = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const day = normalized.getDay();
  const offset = day === 0 ? -6 : 1 - day;
  normalized.setDate(normalized.getDate() + offset);
  return normalized;
}

export default function CaixaPage({ onFeedback, onDashboardRefresh }) {
  const [movimentos, setMovimentos] = useState([]);
  const [saldoTotal, setSaldoTotal] = useState(0);
  const [saldoHoje, setSaldoHoje] = useState(0);
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("TODOS");
  const [periodFilter, setPeriodFilter] = useState("TODOS");
  const [loading, setLoading] = useState(false);
  const [cashModalOpen, setCashModalOpen] = useState(false);
  const [selectedMovement, setSelectedMovement] = useState(null);
  const [cashForm, setCashForm] = useState(createEmptyCashForm);
  const [savingCash, setSavingCash] = useState(false);

  const movimentosHoje = movimentos.filter((movimento) => dateKey(movimento.dataMovimento) === todayIsoDate());
  const entradasHoje = sumByType(movimentosHoje, "ENTRADA");
  const saidasHoje = sumByType(movimentosHoje, "SAIDA");
  const filteredMovements = filterMovements(movimentos, search, typeFilter, periodFilter);

  useEffect(() => {
    refreshCash();
  }, []);

  function notify(feedback) {
    if (onFeedback) {
      onFeedback(feedback);
    }
  }

  async function refreshCash() {
    setLoading(true);

    try {
      const [movimentosData, saldoData, saldoHojeData] = await Promise.all([
        api.get("/movimentos-caixa"),
        api.get("/movimentos-caixa/saldo"),
        api.get("/movimentos-caixa/saldo-hoje")
      ]);

      setMovimentos(Array.isArray(movimentosData) ? movimentosData : []);
      setSaldoTotal(parseMoney(saldoData?.saldo));
      setSaldoHoje(parseMoney(saldoHojeData?.saldo));
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel carregar o caixa."
      });
    } finally {
      setLoading(false);
    }
  }

  function openCashCreateModal(tipo = "ENTRADA") {
    setSelectedMovement(null);
    setCashForm({
      ...createEmptyCashForm(),
      tipo,
      categoria: tipo === "SAIDA" ? "DESPESA" : "AJUSTE"
    });
    setCashModalOpen(true);
    notify(null);
  }

  function openCashEditModal(movimento) {
    setSelectedMovement(movimento);
    setCashForm(movementToForm(movimento));
    setCashModalOpen(true);
    notify(null);
  }

  function closeCashModal() {
    if (!savingCash) {
      setCashModalOpen(false);
    }
  }

  function updateCashField(field, value) {
    setCashForm((current) => {
      if (field !== "tipo") {
        return {
          ...current,
          [field]: value
        };
      }

      return {
        ...current,
        tipo: value,
        categoria: value === "SAIDA" && current.categoria === "AJUSTE" ? "DESPESA" : current.categoria
      };
    });
  }

  async function handleCashSubmit(event) {
    event.preventDefault();
    const formElement = event.currentTarget?.form || event.currentTarget;
    if (formElement?.reportValidity && !formElement.reportValidity()) {
      return;
    }

    const payload = buildCashPayload(cashForm, selectedMovement);
    if (!payload.descricao || !payload.categoria) {
      notify({ type: "error", message: "Informe descricao e categoria para salvar o movimento." });
      return;
    }

    if (payload.valor < 0) {
      notify({ type: "error", message: "O valor nao pode ser negativo." });
      return;
    }

    setSavingCash(true);
    notify(null);

    try {
      const isEditing = Boolean(selectedMovement?.idMovimentoCaixa);
      const saved = isEditing
        ? await api.put(`/movimentos-caixa/${selectedMovement.idMovimentoCaixa}`, payload)
        : await api.post("/movimentos-caixa", payload);

      notify({
        type: "success",
        message: isEditing
          ? `Movimento #${formatMovementNumber(saved?.idMovimentoCaixa || selectedMovement.idMovimentoCaixa)} atualizado com sucesso.`
          : `Movimento #${formatMovementNumber(saved?.idMovimentoCaixa)} lancado no caixa.`
      });
      setCashModalOpen(false);
      await refreshCash();
      if (onDashboardRefresh) {
        await onDashboardRefresh();
      }
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel salvar o movimento de caixa."
      });
    } finally {
      setSavingCash(false);
    }
  }

  async function handleCashDelete(movimento) {
    const confirmed = window.confirm(`Excluir o movimento #${formatMovementNumber(movimento.idMovimentoCaixa)}?`);
    if (!confirmed) {
      return;
    }

    setLoading(true);
    notify(null);

    try {
      await api.delete(`/movimentos-caixa/${movimento.idMovimentoCaixa}`);
      notify({ type: "success", message: "Movimento de caixa excluido com sucesso." });
      await refreshCash();
      if (onDashboardRefresh) {
        await onDashboardRefresh();
      }
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel excluir o movimento de caixa."
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="management-page caixa-page" aria-labelledby="caixa-title">
      <div className="management-hero">
        <div>
          <span className="section-label">Controle financeiro</span>
          <h1 id="caixa-title">Caixa</h1>
          <p>Acompanhe entradas, saidas, saldo do dia e lancamentos manuais da Acao Games.</p>
        </div>
        <div className="management-actions">
          <button type="button" className="ghost-button" onClick={() => openCashCreateModal("SAIDA")}>
            Nova saida
          </button>
          <button type="button" className="primary-button management-hero__button" onClick={() => openCashCreateModal("ENTRADA")}>
            Nova entrada
          </button>
        </div>
      </div>

      <section className="client-toolbar caixa-toolbar" aria-label="Busca e resumo de caixa">
        <label className="search-room client-search">
          <SvgIcon src={searchIcon} size={15} />
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Buscar por descricao, categoria, pagamento ou vinculo..."
          />
        </label>
        <article className={`client-summary-card ${saldoTotal < 0 ? "client-summary-card--muted" : "client-summary-card--active"}`}>
          <span>Saldo atual</span>
          <strong>{currencyFormatter.format(saldoTotal)}</strong>
        </article>
        <article className={`client-summary-card ${saldoHoje < 0 ? "client-summary-card--muted" : "client-summary-card--gold"}`}>
          <span>Caixa hoje</span>
          <strong>{currencyFormatter.format(saldoHoje)}</strong>
        </article>
        <article className="client-summary-card client-summary-card--active">
          <span>Entradas hoje</span>
          <strong>{currencyFormatter.format(entradasHoje)}</strong>
        </article>
        <article className="client-summary-card client-summary-card--muted">
          <span>Saidas hoje</span>
          <strong>{currencyFormatter.format(saidasHoje)}</strong>
        </article>
      </section>

      <section className="filters-bar caixa-filters" aria-label="Filtros do caixa">
        <div className="filter-group">
          <span className="filter-group__label">Periodo</span>
          <button
            type="button"
            className={`cash-filter-button ${periodFilter === "TODOS" ? "cash-filter-button--active" : ""}`}
            onClick={() => setPeriodFilter("TODOS")}
          >
            Total
          </button>
          <button
            type="button"
            className={`cash-filter-button ${periodFilter === "DIA" ? "cash-filter-button--active" : ""}`}
            onClick={() => setPeriodFilter("DIA")}
          >
            Dia
          </button>
          <button
            type="button"
            className={`cash-filter-button ${periodFilter === "SEMANA" ? "cash-filter-button--active" : ""}`}
            onClick={() => setPeriodFilter("SEMANA")}
          >
            Semana
          </button>
          <button
            type="button"
            className={`cash-filter-button ${periodFilter === "MES" ? "cash-filter-button--active" : ""}`}
            onClick={() => setPeriodFilter("MES")}
          >
            Mes
          </button>
        </div>

        <div className="filter-group">
          <span className="filter-group__label">Tipo</span>
          <button
            type="button"
            className={`cash-filter-button ${typeFilter === "TODOS" ? "cash-filter-button--active" : ""}`}
            onClick={() => setTypeFilter("TODOS")}
          >
            Todos
          </button>
          <button
            type="button"
            className={`cash-filter-button ${typeFilter === "ENTRADA" ? "cash-filter-button--active" : ""}`}
            onClick={() => setTypeFilter("ENTRADA")}
          >
            Entradas
          </button>
          <button
            type="button"
            className={`cash-filter-button ${typeFilter === "SAIDA" ? "cash-filter-button--active" : ""}`}
            onClick={() => setTypeFilter("SAIDA")}
          >
            Saidas
          </button>
        </div>
      </section>

      <Panel title="Movimentacoes do caixa">
        {loading ? (
          <div className="empty-state">Carregando movimentos do caixa...</div>
        ) : (
          <div className="table-wrap">
            <table className="compact-table clients-table">
              <thead>
                <tr>
                  <th>Movimento</th>
                  <th>Descricao</th>
                  <th>Categoria</th>
                  <th>Pagamento</th>
                  <th>Vinculo</th>
                  <th>Data</th>
                  <th>Valor</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {filteredMovements.length ? filteredMovements.map((movimento) => (
                  <tr key={movimento.idMovimentoCaixa}>
                    <td>
                      <div className="cash-movement-cell">
                        <span className={`status-badge status-badge--${isEntrada(movimento) ? "green" : "red"}`}>
                          {humanizeStatus(movimento.tipo)}
                        </span>
                        <small>#{formatMovementNumber(movimento.idMovimentoCaixa)}</small>
                      </div>
                    </td>
                    <td>{movimento.descricao || "Sem descricao"}</td>
                    <td>{humanizeStatus(movimento.categoria)}</td>
                    <td>{humanizeStatus(movimento.formaPagamento)}</td>
                    <td>{movementLinkLabel(movimento)}</td>
                    <td>{formatDateTime(movimento.dataMovimento)}</td>
                    <td>
                      <strong className={isEntrada(movimento) ? "cash-value cash-value--positive" : "cash-value cash-value--negative"}>
                        {isEntrada(movimento) ? "+" : "-"}{currencyFormatter.format(parseMoney(movimento.valor))}
                      </strong>
                    </td>
                    <td>
                      <div className="row-actions">
                        <button type="button" className="table-action table-action--edit" onClick={() => openCashEditModal(movimento)}>
                          Editar
                        </button>
                        <button type="button" className="table-action table-action--danger" onClick={() => handleCashDelete(movimento)}>
                          Excluir
                        </button>
                      </div>
                    </td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan="8">
                      {search || typeFilter !== "TODOS"
                        ? "Nenhum movimento encontrado para os filtros informados."
                        : "Nenhum movimento de caixa registrado ainda."}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      {cashModalOpen ? (
        <CashModal
          form={cashForm}
          selectedMovement={selectedMovement}
          saving={savingCash}
          onClose={closeCashModal}
          onSubmit={handleCashSubmit}
          onChange={updateCashField}
        />
      ) : null}
    </section>
  );
}

function CashModal({ form, selectedMovement, saving, onClose, onSubmit, onChange }) {
  const isEditing = Boolean(selectedMovement?.idMovimentoCaixa);

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal entry-modal--client" role="dialog" aria-modal="true" aria-labelledby="cash-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">{isEditing ? "Editar movimento" : "Novo movimento"}</span>
            <h2 id="cash-title">{isEditing ? `Editar Movimento #${formatMovementNumber(selectedMovement.idMovimentoCaixa)}` : "Lancamento de Caixa"}</h2>
            <p>Registre entradas e saidas manuais sem alterar vendas ou ordens de servico.</p>
          </div>
          <div className="entry-modal__actions">
            <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">
              x
            </button>
          </div>
        </header>

        <form className="entry-form" onSubmit={onSubmit}>
          <div className="entry-form__grid entry-form__grid--single">
            <fieldset className="form-card form-card--wide">
              <legend>Dados do movimento</legend>
              <div className="form-grid-two">
                <label className="field">
                  <span>Tipo</span>
                  <select required value={form.tipo} onChange={(event) => onChange("tipo", event.target.value)}>
                    <option value="ENTRADA">Entrada</option>
                    <option value="SAIDA">Saida</option>
                  </select>
                </label>
                <label className="field">
                  <span>Valor</span>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    required
                    value={form.valor}
                    onChange={(event) => onChange("valor", event.target.value)}
                  />
                </label>
                <label className="field">
                  <span>Data e hora</span>
                  <input
                    type="datetime-local"
                    required
                    value={form.dataMovimento}
                    onChange={(event) => onChange("dataMovimento", event.target.value)}
                  />
                </label>
                <label className="field">
                  <span>Forma de pagamento</span>
                  <select value={form.formaPagamento} onChange={(event) => onChange("formaPagamento", event.target.value)}>
                    {formasPagamento.map((forma) => (
                      <option key={forma.value || "none"} value={forma.value}>{forma.label}</option>
                    ))}
                  </select>
                </label>
                <label className="field field--wide">
                  <span>Categoria</span>
                  <input
                    required
                    list="cash-categories"
                    value={form.categoria}
                    onChange={(event) => onChange("categoria", event.target.value)}
                  />
                  <datalist id="cash-categories">
                    {categoriasCaixa.map((categoria) => (
                      <option key={categoria} value={categoria} />
                    ))}
                  </datalist>
                </label>
                <label className="field field--wide">
                  <span>Descricao</span>
                  <textarea
                    rows="4"
                    required
                    value={form.descricao}
                    onChange={(event) => onChange("descricao", event.target.value)}
                  />
                </label>
              </div>
            </fieldset>
          </div>

          <footer className="entry-modal__footer">
            <button type="button" className="ghost-button" onClick={onClose} disabled={saving}>
              Cancelar
            </button>
            <button type="submit" className="primary-button" disabled={saving}>
              {saving ? "Salvando..." : isEditing ? "Salvar alteracoes" : "Salvar movimento"}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

function Panel({ title, children }) {
  return (
    <article className="panel">
      <div className="panel-head">
        <h2>{title}</h2>
      </div>
      {children}
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

function isEntrada(movimento) {
  return String(movimento?.tipo || "").toUpperCase() === "ENTRADA";
}

function sumByType(movimentos, tipo) {
  return movimentos
    .filter((movimento) => String(movimento.tipo || "") === tipo)
    .reduce((total, movimento) => total + parseMoney(movimento.valor), 0);
}

function movementLinkLabel(movimento) {
  if (movimento?.venda?.idVenda) {
    return `Venda #${formatMovementNumber(movimento.venda.idVenda)}`;
  }

  if (movimento?.ordemServico?.idOrdemServico) {
    return `OS #${formatMovementNumber(movimento.ordemServico.idOrdemServico)}`;
  }

  return "Manual";
}

function parseMoney(value) {
  const normalized = String(value || "0").replace(",", ".");
  const number = Number(normalized);
  return Number.isFinite(number) ? number : 0;
}

function formatDateTime(value) {
  if (!value) {
    return "Sem data";
  }

  const date = new Date(String(value).includes("T") ? value : `${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return "Sem data";
  }

  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short"
  });
}

function toDateTimeInput(value) {
  if (!value) {
    return nowDateTimeInput();
  }

  const text = String(value);
  if (text.includes("T")) {
    return text.slice(0, 16);
  }

  const date = new Date(text);
  if (Number.isNaN(date.getTime())) {
    return nowDateTimeInput();
  }

  return toLocalDateTimeInput(date);
}

function nowDateTimeInput() {
  return toLocalDateTimeInput(new Date());
}

function toLocalDateTimeInput(date) {
  const offsetMs = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

function dateKey(value) {
  if (!value) {
    return "";
  }

  return String(value).slice(0, 10);
}

function todayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

function formatMovementNumber(value) {
  return String(value || 0).padStart(6, "0");
}

function humanizeStatus(status) {
  if (!status) {
    return "Nao informado";
  }

  return String(status)
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function optionalText(value) {
  const text = String(value || "").trim();
  return text.length ? text : null;
}
