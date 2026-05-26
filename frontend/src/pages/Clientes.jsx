import { useEffect, useState } from "react";
import { api } from "../services/api";
import searchIcon from "../assets/icons/search.svg";

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL"
});

function createEmptyClienteForm() {
  return {
    nome: "",
    telefone: "",
    cpf: "",
    endereco: "",
    email: "",
    observacoes: "",
    ativo: true
  };
}

function clienteToForm(cliente) {
  return {
    nome: cliente?.nome || "",
    telefone: cliente?.telefone || "",
    cpf: cliente?.cpf || "",
    endereco: cliente?.endereco || "",
    email: cliente?.email || "",
    observacoes: cliente?.observacoes || "",
    ativo: cliente?.ativo !== false
  };
}

function buildClientePayload(form) {
  return {
    nome: String(form.nome || "").trim(),
    telefone: String(form.telefone || "").trim(),
    cpf: optionalText(form.cpf),
    endereco: optionalText(form.endereco),
    email: optionalText(form.email),
    observacoes: optionalText(form.observacoes),
    ativo: form.ativo !== false
  };
}

function filterClientes(clientes, search) {
  const query = String(search || "").trim().toLowerCase();
  if (!query) {
    return clientes;
  }

  return clientes.filter((cliente) => {
    const fields = [
      cliente.nome,
      cliente.telefone,
      cliente.cpf,
      cliente.email,
      cliente.endereco,
      cliente.observacoes,
      cliente.ativo ? "ativo" : "inativo"
    ];

    return fields.some((value) => String(value || "").toLowerCase().includes(query));
  });
}

export default function ClientesPage({
  clientes,
  onClientesChange,
  onFeedback
}) {
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [clienteModalOpen, setClienteModalOpen] = useState(false);
  const [clienteDetailsOpen, setClienteDetailsOpen] = useState(false);
  const [clienteRecordsOpen, setClienteRecordsOpen] = useState(false);
  const [selectedCliente, setSelectedCliente] = useState(null);
  const [clienteForm, setClienteForm] = useState(createEmptyClienteForm);
  const [clienteRecords, setClienteRecords] = useState({ ordens: [], vendas: [] });
  const [recordsLoading, setRecordsLoading] = useState(false);
  const [savingCliente, setSavingCliente] = useState(false);
  const filteredClientes = filterClientes(clientes, search);
  const totalClientes = clientes.length;
  const activeClientes = clientes.filter((cliente) => cliente.ativo).length;
  const inactiveClientes = clientes.length - activeClientes;

  useEffect(() => {
    refreshClientes({ silent: clientes.length > 0 });
  }, []);

  function notify(feedback) {
    if (onFeedback) {
      onFeedback(feedback);
    }
  }

  async function refreshClientes(options = {}) {
    const { silent = false } = options;

    if (!silent) {
      setLoading(true);
    }

    try {
      const clientesData = await api.get("/clientes");
      onClientesChange(Array.isArray(clientesData) ? clientesData : []);
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel carregar os clientes."
      });
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }

  function updateClienteField(field, value) {
    setClienteForm((current) => ({
      ...current,
      [field]: value
    }));
  }

  function openClienteCreateModal() {
    setSelectedCliente(null);
    setClienteForm(createEmptyClienteForm());
    setClienteModalOpen(true);
    notify(null);
  }

  function openClienteEditModal(cliente) {
    setSelectedCliente(cliente);
    setClienteForm(clienteToForm(cliente));
    setClienteModalOpen(true);
    notify(null);
  }

  function closeClienteModal() {
    if (savingCliente) {
      return;
    }

    setClienteModalOpen(false);
  }

  async function openClienteDetails(cliente) {
    setSelectedCliente(cliente);
    setClienteDetailsOpen(true);
    notify(null);

    try {
      const clienteAtualizado = await api.get(`/clientes/${cliente.idCliente}`);
      setSelectedCliente(clienteAtualizado || cliente);
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel carregar os detalhes do cliente."
      });
    }
  }

  function closeClienteDetails() {
    setClienteDetailsOpen(false);
  }

  async function openClienteRecords(cliente) {
    setSelectedCliente(cliente);
    setClienteRecords({ ordens: [], vendas: [] });
    setClienteRecordsOpen(true);
    setRecordsLoading(true);
    notify(null);

    try {
      const [ordensData, vendasData] = await Promise.all([
        api.get("/ordens-servico"),
        api.get("/vendas")
      ]);
      const clienteId = String(cliente.idCliente);
      const ordens = Array.isArray(ordensData)
        ? ordensData.filter((ordem) => String(ordem.cliente?.idCliente) === clienteId)
        : [];
      const vendas = Array.isArray(vendasData)
        ? vendasData.filter((venda) => String(venda.cliente?.idCliente) === clienteId)
        : [];

      setClienteRecords({ ordens, vendas });
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel carregar os registros do cliente."
      });
    } finally {
      setRecordsLoading(false);
    }
  }

  function closeClienteRecords() {
    setClienteRecordsOpen(false);
  }

  async function handleClienteSubmit(event) {
    event.preventDefault();
    const formElement = event.currentTarget?.form || event.currentTarget;
    if (formElement?.reportValidity && !formElement.reportValidity()) {
      return;
    }

    const payload = buildClientePayload(clienteForm);
    if (!payload.nome || !payload.telefone) {
      notify({ type: "error", message: "Informe nome e telefone para salvar o cliente." });
      return;
    }

    setSavingCliente(true);
    notify(null);

    try {
      const isEditing = Boolean(selectedCliente?.idCliente);
      const clienteSalvo = isEditing
        ? await api.put(`/clientes/${selectedCliente.idCliente}`, payload)
        : await api.post("/clientes", payload);

      notify({
        type: "success",
        message: isEditing
          ? `Cliente ${clienteSalvo?.nome || payload.nome} atualizado com sucesso.`
          : `Cliente ${clienteSalvo?.nome || payload.nome} cadastrado com sucesso.`
      });
      setClienteModalOpen(false);
      setSelectedCliente(clienteSalvo || null);
      await refreshClientes({ silent: true });
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel salvar o cliente."
      });
    } finally {
      setSavingCliente(false);
    }
  }

  async function handleClienteDelete(cliente) {
    const confirmed = window.confirm(`Excluir o cliente "${cliente.nome}"? Essa acao nao pode ser desfeita.`);
    if (!confirmed) {
      return;
    }

    setLoading(true);
    notify(null);

    try {
      await api.delete(`/clientes/${cliente.idCliente}`);
      notify({ type: "success", message: `Cliente ${cliente.nome} excluido com sucesso.` });
      await refreshClientes({ silent: true });
    } catch (error) {
      if (error.status === 409) {
        await handleClienteDeleteConflict(cliente);
        return;
      }

      notify({
        type: "error",
        message: error.message || "Nao foi possivel excluir o cliente. Verifique se ele possui OS ou vendas vinculadas."
      });
    } finally {
      setLoading(false);
    }
  }

  async function handleClienteDeleteConflict(cliente) {
    if (!cliente.ativo) {
      notify({
        type: "error",
        message: `Cliente ${cliente.nome} ja esta inativo e nao pode ser excluido porque possui historico vinculado.`
      });
      return;
    }

    const shouldInactivate = window.confirm(
      `O cliente "${cliente.nome}" possui OS, vendas ou outros registros vinculados e nao pode ser excluido sem apagar historico.\n\nDeseja inativar este cliente?`
    );

    if (!shouldInactivate) {
      notify({
        type: "error",
        message: "Exclusao cancelada. O cliente possui historico vinculado e foi mantido ativo."
      });
      return;
    }

    try {
      const payload = buildClientePayload({
        ...clienteToForm(cliente),
        ativo: false
      });
      await api.put(`/clientes/${cliente.idCliente}`, payload);
      notify({
        type: "success",
        message: `Cliente ${cliente.nome} possui historico vinculado, entao foi inativado com sucesso.`
      });
      await refreshClientes({ silent: true });
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel inativar o cliente."
      });
    }
  }

  return (
    <section className="management-page clientes-page" aria-labelledby="clientes-title">
      <div className="management-hero">
        <div>
          <span className="section-label">Manutencao de cadastro</span>
          <h1 id="clientes-title">Clientes</h1>
          <p>Gerencie os clientes cadastrados na Acao Games</p>
        </div>
        <button type="button" className="primary-button management-hero__button" onClick={openClienteCreateModal}>
          Novo Cliente
        </button>
      </div>

      <section className="client-toolbar" aria-label="Busca e resumo de clientes">
        <label className="search-room client-search">
          <SvgIcon src={searchIcon} size={15} />
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Buscar por nome, telefone, CPF ou e-mail..."
          />
        </label>
        <article className="client-summary-card">
          <span>Total</span>
          <strong>{totalClientes}</strong>
        </article>
        <article className="client-summary-card client-summary-card--active">
          <span>Ativos</span>
          <strong>{activeClientes}</strong>
        </article>
        <article className="client-summary-card client-summary-card--muted">
          <span>Inativos</span>
          <strong>{inactiveClientes}</strong>
        </article>
      </section>

      <ClientePanel title="Clientes cadastrados">
        {loading ? (
          <div className="empty-state">Carregando clientes...</div>
        ) : (
          <div className="table-wrap">
            <table className="compact-table clients-table">
              <thead>
                <tr>
                  <th>Cliente</th>
                  <th>Telefone</th>
                  <th>CPF</th>
                  <th>Email</th>
                  <th>Status</th>
                  <th>Cadastro</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {filteredClientes.length ? filteredClientes.map((cliente) => (
                  <tr key={cliente.idCliente}>
                    <td>
                      <div className="client-cell">
                        <span className="client-avatar">{clienteInitials(cliente)}</span>
                        <div>
                          <strong>{cliente.nome}</strong>
                          <small>#{formatClientNumber(cliente.idCliente)}</small>
                        </div>
                      </div>
                    </td>
                    <td>{displayValue(cliente.telefone)}</td>
                    <td>{displayValue(cliente.cpf)}</td>
                    <td>{displayValue(cliente.email)}</td>
                    <td>
                      <span className={`status-badge status-badge--${cliente.ativo ? "green" : "red"}`}>
                        {cliente.ativo ? "Ativo" : "Inativo"}
                      </span>
                    </td>
                    <td>{formatDateTime(cliente.dataCadastro)}</td>
                    <td>
                      <div className="row-actions">
                        <button type="button" className="table-action" onClick={() => openClienteDetails(cliente)}>
                          Ver
                        </button>
                        <button type="button" className="table-action" onClick={() => openClienteRecords(cliente)}>
                          Registros
                        </button>
                        <button type="button" className="table-action table-action--danger" onClick={() => handleClienteDelete(cliente)}>
                          Excluir
                        </button>
                      </div>
                    </td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan="7">
                      {search
                        ? "Nenhum cliente encontrado para a busca informada."
                        : "Nenhum cliente cadastrado ainda. Use o botao Novo Cliente para iniciar."}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </ClientePanel>

      {clienteModalOpen ? (
        <ClienteModal
          form={clienteForm}
          selectedCliente={selectedCliente}
          saving={savingCliente}
          onClose={closeClienteModal}
          onSubmit={handleClienteSubmit}
          onChange={updateClienteField}
        />
      ) : null}

      {clienteDetailsOpen && selectedCliente ? (
        <ClienteDetailsModal
          cliente={selectedCliente}
          onClose={closeClienteDetails}
          onEdit={() => {
            setClienteDetailsOpen(false);
            openClienteEditModal(selectedCliente);
          }}
        />
      ) : null}

      {clienteRecordsOpen && selectedCliente ? (
        <ClienteRecordsModal
          cliente={selectedCliente}
          records={clienteRecords}
          loading={recordsLoading}
          onClose={closeClienteRecords}
        />
      ) : null}
    </section>
  );
}

function ClienteModal({ form, selectedCliente, saving, onClose, onSubmit, onChange }) {
  const isEditing = Boolean(selectedCliente?.idCliente);

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal entry-modal--client" role="dialog" aria-modal="true" aria-labelledby="cliente-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">{isEditing ? "Editar cadastro" : "Novo cadastro"}</span>
            <h2 id="cliente-title">{isEditing ? "Editar Cliente" : "Novo Cliente"}</h2>
            <p>Use os mesmos campos definidos no cadastro de clientes do backend.</p>
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
              <legend>Dados do cliente</legend>
              <div className="form-grid-two">
                <label className="field">
                  <span>Nome</span>
                  <input required value={form.nome} onChange={(event) => onChange("nome", event.target.value)} />
                </label>
                <label className="field">
                  <span>Telefone</span>
                  <input required value={form.telefone} onChange={(event) => onChange("telefone", event.target.value)} />
                </label>
                <label className="field">
                  <span>CPF</span>
                  <input value={form.cpf} onChange={(event) => onChange("cpf", event.target.value)} />
                </label>
                <label className="field">
                  <span>Email</span>
                  <input type="email" value={form.email} onChange={(event) => onChange("email", event.target.value)} />
                </label>
                <label className="field field--wide">
                  <span>Endereco</span>
                  <input value={form.endereco} onChange={(event) => onChange("endereco", event.target.value)} />
                </label>
                <label className="field field--wide">
                  <span>Observacoes</span>
                  <textarea rows="4" value={form.observacoes} onChange={(event) => onChange("observacoes", event.target.value)} />
                </label>
                <label className="checkbox-filter client-active-toggle">
                  <input
                    type="checkbox"
                    checked={form.ativo}
                    onChange={(event) => onChange("ativo", event.target.checked)}
                  />
                  <span className="checkbox-control" />
                  <span>Cliente ativo</span>
                </label>
              </div>
            </fieldset>
          </div>

          <footer className="entry-modal__footer">
            <button type="button" className="ghost-button" onClick={onClose} disabled={saving}>
              Cancelar
            </button>
            <button type="submit" className="primary-button" disabled={saving}>
              {saving ? "Salvando..." : isEditing ? "Salvar alteracoes" : "Cadastrar cliente"}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

function ClienteDetailsModal({ cliente, onClose, onEdit }) {
  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal entry-modal--client" role="dialog" aria-modal="true" aria-labelledby="cliente-detail-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">Detalhes do cliente</span>
            <h2 id="cliente-detail-title">{cliente.nome}</h2>
            <p>Cadastro #{formatClientNumber(cliente.idCliente)} da Acao Games.</p>
          </div>
          <div className="entry-modal__actions">
            <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">
              x
            </button>
          </div>
        </header>

        <div className="entry-form client-details">
          <div className="client-details__grid">
            <DetailItem label="Telefone" value={cliente.telefone} />
            <DetailItem label="CPF" value={cliente.cpf} />
            <DetailItem label="Email" value={cliente.email} />
            <DetailItem label="Status" value={cliente.ativo ? "Ativo" : "Inativo"} />
            <DetailItem label="Endereco" value={cliente.endereco} wide />
            <DetailItem label="Observacoes" value={cliente.observacoes} wide />
            <DetailItem label="Data de cadastro" value={formatDateTime(cliente.dataCadastro)} />
          </div>

          <footer className="entry-modal__footer">
            <button type="button" className="ghost-button" onClick={onClose}>
              Fechar
            </button>
            <button type="button" className="primary-button" onClick={onEdit}>
              Editar cliente
            </button>
          </footer>
        </div>
      </section>
    </div>
  );
}

function ClienteRecordsModal({ cliente, records, loading, onClose }) {
  const totalServicos = records.ordens.reduce((total, ordem) => total + parseMoney(ordem.valorServico), 0);
  const totalCompras = records.vendas.reduce((total, venda) => total + parseMoney(venda.valorTotal), 0);

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal entry-modal--client-records" role="dialog" aria-modal="true" aria-labelledby="cliente-records-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">Historico do cliente</span>
            <h2 id="cliente-records-title">Registros de {cliente.nome}</h2>
            <p>Compras e servicos vinculados a este cadastro.</p>
          </div>
          <div className="entry-modal__actions">
            <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">
              x
            </button>
          </div>
        </header>

        <div className="entry-form client-records">
          <div className="records-summary">
            <article className="client-summary-card">
              <span>Servicos</span>
              <strong>{records.ordens.length}</strong>
            </article>
            <article className="client-summary-card client-summary-card--active">
              <span>Total em servicos</span>
              <strong>{currencyFormatter.format(totalServicos)}</strong>
            </article>
            <article className="client-summary-card">
              <span>Compras</span>
              <strong>{records.vendas.length}</strong>
            </article>
            <article className="client-summary-card client-summary-card--gold">
              <span>Total em compras</span>
              <strong>{currencyFormatter.format(totalCompras)}</strong>
            </article>
          </div>

          {loading ? (
            <div className="empty-state">Carregando registros...</div>
          ) : (
            <div className="records-grid">
              <section className="records-section">
                <div className="records-section__head">
                  <h3>Servicos / Ordens de servico</h3>
                  <span>{records.ordens.length} registro(s)</span>
                </div>
                {records.ordens.length ? (
                  <div className="table-wrap">
                    <table className="compact-table">
                      <thead>
                        <tr>
                          <th>OS</th>
                          <th>Equipamento</th>
                          <th>Status</th>
                          <th>Entrada</th>
                          <th>Valor</th>
                          <th>PDF</th>
                        </tr>
                      </thead>
                      <tbody>
                        {records.ordens.map((ordem) => (
                          <tr key={ordem.idOrdemServico}>
                            <td>#{formatRecordNumber(ordem.idOrdemServico)}</td>
                            <td>{equipmentLabel(ordem.equipamento)}</td>
                            <td>
                              <span className={`status-badge status-badge--${statusTone(ordem.status)}`}>
                                {humanizeStatus(ordem.status)}
                              </span>
                            </td>
                            <td>{formatDate(ordem.dataEntrada)}</td>
                            <td>{currencyFormatter.format(parseMoney(ordem.valorServico))}</td>
                            <td>
                              <button type="button" className="table-action" onClick={() => openServiceOrderPdf(ordem)}>
                                PDF
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="empty-state">Nenhuma ordem de servico encontrada para este cliente.</div>
                )}
              </section>

              <section className="records-section">
                <div className="records-section__head">
                  <h3>Compras / Vendas</h3>
                  <span>{records.vendas.length} registro(s)</span>
                </div>
                {records.vendas.length ? (
                  <div className="table-wrap">
                    <table className="compact-table">
                      <thead>
                        <tr>
                          <th>Venda</th>
                          <th>Data</th>
                          <th>Pagamento</th>
                          <th>Valor</th>
                          <th>PDF</th>
                        </tr>
                      </thead>
                      <tbody>
                        {records.vendas.map((venda) => (
                          <tr key={venda.idVenda}>
                            <td>#{formatRecordNumber(venda.idVenda)}</td>
                            <td>{formatDate(venda.dataVenda)}</td>
                            <td>{humanizeStatus(venda.formaPagamento)}</td>
                            <td>{currencyFormatter.format(parseMoney(venda.valorTotal))}</td>
                            <td>
                              <button type="button" className="table-action" onClick={() => openSaleReceiptPdf(venda)}>
                                PDF
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="empty-state">Nenhuma compra encontrada para este cliente.</div>
                )}
              </section>
            </div>
          )}

          <footer className="entry-modal__footer">
            <button type="button" className="ghost-button" onClick={onClose}>
              Fechar
            </button>
          </footer>
        </div>
      </section>
    </div>
  );
}

function ClientePanel({ title, children }) {
  return (
    <article className="panel">
      <div className="panel-head">
        <h2>{title}</h2>
      </div>
      {children}
    </article>
  );
}

function DetailItem({ label, value, wide = false }) {
  return (
    <article className={`detail-item ${wide ? "detail-item--wide" : ""}`}>
      <span>{label}</span>
      <strong>{displayValue(value)}</strong>
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

function formatDateTime(value) {
  if (!value) {
    return "Sem data";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Sem data";
  }

  return date.toLocaleDateString("pt-BR");
}

function formatDate(value) {
  if (!value) {
    return "Sem data";
  }

  return new Date(`${value}T00:00:00`).toLocaleDateString("pt-BR");
}

function formatRecordNumber(value) {
  return String(value || 0).padStart(6, "0");
}

function formatClientNumber(value) {
  return String(value || 0).padStart(6, "0");
}

function parseMoney(value) {
  const normalized = String(value || "0").replace(",", ".");
  const number = Number(normalized);
  return Number.isFinite(number) ? number : 0;
}

function equipmentLabel(equipamento) {
  if (!equipamento) {
    return "Equipamento nao informado";
  }

  return [equipamento.marca, equipamento.tipoEquipamento, equipamento.modelo]
    .filter(Boolean)
    .join(" - ");
}

function statusTone(status) {
  const normalized = String(status || "").toLowerCase();
  if (normalized.includes("pronto") || normalized.includes("entregue")) {
    return "green";
  }

  if (normalized.includes("aprovacao") || normalized.includes("analise") || normalized.includes("manutencao")) {
    return "amber";
  }

  if (normalized.includes("cancel")) {
    return "red";
  }

  return "blue";
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

function openServiceOrderPdf(ordem) {
  const idOrdemServico = typeof ordem === "object" ? ordem?.idOrdemServico : ordem;
  if (!idOrdemServico) {
    return;
  }

  window.open(api.fileUrl(`/ordens-servico/${idOrdemServico}/pdf`), "_blank", "noopener,noreferrer");
}

function openSaleReceiptPdf(venda) {
  const idVenda = typeof venda === "object" ? venda?.idVenda : venda;
  if (!idVenda) {
    return;
  }

  window.open(api.fileUrl(`/vendas/${idVenda}/pdf`), "_blank", "noopener,noreferrer");
}

function clienteInitials(cliente) {
  const parts = String(cliente?.nome || "CL").trim().split(/\s+/);
  const initials = parts.slice(0, 2).map((part) => part.charAt(0)).join("");
  return initials.toUpperCase() || "CL";
}

function displayValue(value) {
  const text = String(value || "").trim();
  return text || "Nao informado";
}

function optionalText(value) {
  const text = String(value || "").trim();
  return text.length ? text : null;
}
