import React, { useEffect, useState } from "react";
import { api } from "../services/api";
import searchIcon from "../assets/icons/search.svg";
import SortableHeader from "../components/SortableHeader";
import { nextSort, sortRows } from "../utils/tableSorting";

const marcas = ["Sony", "Microsoft", "Nintendo"];
const tiposEquipamento = ["Console", "Controle"];

const fallbackStatuses = [
  "RECEBIDO",
  "EM_ANALISE",
  "AGUARDANDO_APROVACAO",
  "APROVADO",
  "EM_MANUTENCAO",
  "PRONTO",
  "ENTREGUE",
  "CANCELADO"
];

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL"
});

const orderSortColumns = {
  os: { type: "number", value: (ordem) => ordem.idOrdemServico },
  cliente: { type: "text", value: (ordem) => ordem.cliente?.nome },
  equipamento: { type: "text", value: (ordem) => equipmentLabel(ordem.equipamento) },
  status: { type: "status", value: (ordem) => statusRank(ordem.status) },
  entrada: { type: "date", value: (ordem) => ordem.dataEntrada },
  valor: { type: "number", value: (ordem) => parseMoney(ordem.valorServico) }
};

function createEmptyOrderForm() {
  return {
    idCliente: "",
    idEquipamento: "",
    numeroSerie: "",
    cor: "",
    dataEntrada: todayIsoDate(),
    problemaRelatado: "",
    status: "RECEBIDO",
    diagnostico: "",
    servicoRealizado: "",
    valorServico: "",
    dataSaida: "",
    observacoes: ""
  };
}

function createEmptyEquipmentForm() {
  return {
    marca: "Sony",
    tipoEquipamento: "Console",
    modelo: "",
    descricao: "",
    ativo: true
  };
}

function orderToForm(ordem) {
  return {
    idCliente: ordem?.cliente?.idCliente ? String(ordem.cliente.idCliente) : "",
    idEquipamento: ordem?.equipamento?.idEquipamento ? String(ordem.equipamento.idEquipamento) : "",
    numeroSerie: ordem?.numeroSerie || "",
    cor: ordem?.cor || "",
    dataEntrada: ordem?.dataEntrada || todayIsoDate(),
    problemaRelatado: ordem?.problemaRelatado || "",
    status: ordem?.status || "RECEBIDO",
    diagnostico: ordem?.diagnostico || "",
    servicoRealizado: ordem?.servicoRealizado || "",
    valorServico: ordem?.valorServico ?? "",
    dataSaida: ordem?.dataSaida || "",
    observacoes: ordem?.observacoes || ""
  };
}

function equipmentToForm(equipamento) {
  return {
    marca: equipamento?.marca || "Sony",
    tipoEquipamento: equipamento?.tipoEquipamento || "Console",
    modelo: equipamento?.modelo || "",
    descricao: equipamento?.descricao || "",
    ativo: equipamento?.ativo !== false
  };
}

function buildOrderPayload(form) {
  return {
    idCliente: Number(form.idCliente),
    idEquipamento: Number(form.idEquipamento),
    numeroSerie: optionalText(form.numeroSerie),
    cor: optionalText(form.cor),
    dataEntrada: form.dataEntrada || null,
    problemaRelatado: String(form.problemaRelatado || "").trim(),
    status: optionalText(form.status),
    diagnostico: optionalText(form.diagnostico),
    servicoRealizado: optionalText(form.servicoRealizado),
    valorServico: form.valorServico === "" ? null : parseMoney(form.valorServico),
    dataSaida: form.dataSaida || null,
    observacoes: optionalText(form.observacoes)
  };
}

function buildEquipmentPayload(form) {
  return {
    marca: String(form.marca || "").trim(),
    tipoEquipamento: String(form.tipoEquipamento || "").trim(),
    modelo: String(form.modelo || "").trim(),
    descricao: optionalText(form.descricao),
    ativo: form.ativo !== false
  };
}

function filterOrders(ordens, search) {
  const query = String(search || "").trim().toLowerCase();
  if (!query) {
    return ordens;
  }

  return ordens.filter((ordem) => {
    const fields = [
      ordem.idOrdemServico,
      ordem.cliente?.nome,
      ordem.cliente?.telefone,
      equipmentLabel(ordem.equipamento),
      ordem.numeroSerie,
      ordem.cor,
      ordem.problemaRelatado,
      ordem.status,
      ordem.diagnostico,
      ordem.servicoRealizado
    ];

    return fields.some((value) => String(value || "").toLowerCase().includes(query));
  });
}

function filterEquipments(equipamentos, search) {
  const query = String(search || "").trim().toLowerCase();
  if (!query) {
    return equipamentos;
  }

  return equipamentos.filter((equipamento) => {
    const fields = [
      equipamento.marca,
      equipamento.tipoEquipamento,
      equipamento.modelo,
      equipamento.descricao,
      equipamento.ativo ? "ativo" : "inativo"
    ];

    return fields.some((value) => String(value || "").toLowerCase().includes(query));
  });
}

export default function EquipamentosPage({
  clientes,
  equipamentos,
  ordens,
  onClientesChange,
  onEquipamentosChange,
  onOrdensChange,
  onFeedback,
  onOpenSaida
}) {
  const [search, setSearch] = useState("");
  const [equipmentSearch, setEquipmentSearch] = useState("");
  const [statuses, setStatuses] = useState(fallbackStatuses);
  const [loading, setLoading] = useState(false);
  const [orderModalOpen, setOrderModalOpen] = useState(false);
  const [orderModalMode, setOrderModalMode] = useState("create");
  const [orderDetailsOpen, setOrderDetailsOpen] = useState(false);
  const [equipmentManagerOpen, setEquipmentManagerOpen] = useState(false);
  const [equipmentFormOpen, setEquipmentFormOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [selectedEquipment, setSelectedEquipment] = useState(null);
  const [orderForm, setOrderForm] = useState(createEmptyOrderForm);
  const [equipmentForm, setEquipmentForm] = useState(createEmptyEquipmentForm);
  const [savingOrder, setSavingOrder] = useState(false);
  const [savingEquipment, setSavingEquipment] = useState(false);
  const [orderSort, setOrderSort] = useState({ sortBy: "entrada", sortOrder: "desc" });

  const filteredOrders = sortRows(
    filterOrders(ordens, search),
    orderSort,
    orderSortColumns,
    (left, right) => Number(right.idOrdemServico || 0) - Number(left.idOrdemServico || 0)
  );
  const filteredEquipments = filterEquipments(equipamentos, equipmentSearch);
  const openOrders = ordens.filter((ordem) => !["ENTREGUE", "CANCELADO"].includes(String(ordem.status || ""))).length;
  const readyOrders = ordens.filter((ordem) => String(ordem.status || "") === "PRONTO").length;
  const activeEquipments = equipamentos.filter((equipamento) => equipamento.ativo).length;

  useEffect(() => {
    refreshData({ silent: clientes.length > 0 || equipamentos.length > 0 || ordens.length > 0 });
  }, []);

  function notify(feedback) {
    if (onFeedback) {
      onFeedback(feedback);
    }
  }

  function updateOrderSort(sortBy) {
    setOrderSort((current) => nextSort(current, sortBy));
  }

  async function refreshData(options = {}) {
    const { silent = false } = options;

    if (!silent) {
      setLoading(true);
    }

    try {
      const [clientesData, equipamentosData, ordensData, statusesData] = await Promise.all([
        api.get("/clientes"),
        api.get("/equipamentos"),
        api.get("/ordens-servico"),
        api.get("/ordens-servico/status")
      ]);

      onClientesChange(Array.isArray(clientesData) ? clientesData : []);
      onEquipamentosChange(Array.isArray(equipamentosData) ? equipamentosData : []);
      onOrdensChange(Array.isArray(ordensData) ? ordensData : []);
      setStatuses(Array.isArray(statusesData) && statusesData.length ? statusesData : fallbackStatuses);
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel carregar equipamentos e ordens de servico."
      });
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }

  function openOrderCreateModal() {
    setSelectedOrder(null);
    setOrderForm(createEmptyOrderForm());
    setOrderModalMode("create");
    setOrderModalOpen(true);
    notify(null);
  }

  function openOrderEditModal(ordem) {
    setSelectedOrder(ordem);
    setOrderForm(orderToForm(ordem));
    setOrderModalMode("edit");
    setOrderDetailsOpen(false);
    setOrderModalOpen(true);
    notify(null);
  }

  function openOrderBudgetModal(ordem) {
    setSelectedOrder(ordem);
    setOrderForm({
      ...orderToForm(ordem),
      status: "AGUARDANDO_APROVACAO"
    });
    setOrderModalMode("budget");
    setOrderDetailsOpen(false);
    setOrderModalOpen(true);
    notify(null);
  }

  function openOrderAuthorizationModal(ordem) {
    setSelectedOrder(ordem);
    setOrderForm({
      ...orderToForm(ordem),
      status: "APROVADO"
    });
    setOrderModalMode("authorization");
    setOrderDetailsOpen(false);
    setOrderModalOpen(true);
    notify(null);
  }

  async function openOrderDetails(ordem) {
    setSelectedOrder(ordem);
    setOrderDetailsOpen(true);
    notify(null);

    try {
      const ordemAtualizada = await api.get(`/ordens-servico/${ordem.idOrdemServico}`);
      setSelectedOrder(ordemAtualizada || ordem);
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel carregar os detalhes da OS."
      });
    }
  }

  function closeOrderModal() {
    if (!savingOrder) {
      setOrderModalOpen(false);
    }
  }

  function updateOrderField(field, value) {
    setOrderForm((current) => ({
      ...current,
      [field]: value
    }));
  }

  async function handleOrderSubmit(event) {
    event.preventDefault();
    const formElement = event.currentTarget?.form || event.currentTarget;
    if (formElement?.reportValidity && !formElement.reportValidity()) {
      return;
    }

    const payload = buildOrderPayload(orderForm);
    if (orderModalMode === "create") {
      payload.status = "RECEBIDO";
      payload.dataSaida = null;
    } else if (orderModalMode === "budget") {
      payload.status = "AGUARDANDO_APROVACAO";
      payload.dataSaida = selectedOrder?.dataSaida || null;
    } else if (orderModalMode === "authorization") {
      payload.status = "APROVADO";
      payload.dataSaida = selectedOrder?.dataSaida || null;
    } else {
      if (payload.status === "ENTREGUE") {
        notify({ type: "error", message: "Use Registrar Saida para entregar o aparelho e movimentar o caixa." });
        return;
      }
      payload.dataSaida = selectedOrder?.dataSaida || null;
    }

    if (!payload.idCliente || !payload.idEquipamento || !payload.problemaRelatado) {
      notify({ type: "error", message: "Informe cliente, equipamento e problema relatado para salvar a OS." });
      return;
    }

    setSavingOrder(true);
    notify(null);

    try {
      const isEditing = Boolean(selectedOrder?.idOrdemServico);
      const ordemSalva = isEditing
        ? await api.put(`/ordens-servico/${selectedOrder.idOrdemServico}`, payload)
        : await api.post("/ordens-servico", payload);

      notify({
        type: "success",
        message: orderSuccessMessage(orderModalMode, ordemSalva, selectedOrder)
      });
      setOrderModalOpen(false);
      setOrderForm(createEmptyOrderForm());
      setSelectedOrder(null);
      await refreshData({ silent: true });
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel salvar a OS."
      });
    } finally {
      setSavingOrder(false);
    }
  }

  async function handleOrderDelete(ordem) {
    const confirmed = window.confirm(`Excluir a OS #${formatNumber(ordem.idOrdemServico)}? Essa acao nao pode ser desfeita.`);
    if (!confirmed) {
      return;
    }

    setLoading(true);
    notify(null);

    try {
      await api.delete(`/ordens-servico/${ordem.idOrdemServico}`);
      notify({ type: "success", message: `OS #${formatNumber(ordem.idOrdemServico)} excluida com sucesso.` });
      await refreshData({ silent: true });
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel excluir a OS. Verifique se ela possui registros vinculados."
      });
    } finally {
      setLoading(false);
    }
  }

  function handleSendToSaida(ordem) {
    if (!onOpenSaida) {
      notify({ type: "error", message: "Abra Registrar Saida na tela principal para entregar o aparelho." });
      return;
    }

    setOrderDetailsOpen(false);
    onOpenSaida(ordem);
  }

  function openEquipmentManager() {
    setEquipmentManagerOpen(true);
    setEquipmentFormOpen(false);
    setSelectedEquipment(null);
    setEquipmentForm(createEmptyEquipmentForm());
    notify(null);
  }

  function openEquipmentCreateForm() {
    setSelectedEquipment(null);
    setEquipmentForm(createEmptyEquipmentForm());
    setEquipmentFormOpen(true);
    notify(null);
  }

  function openEquipmentEditForm(equipamento) {
    setSelectedEquipment(equipamento);
    setEquipmentForm(equipmentToForm(equipamento));
    setEquipmentFormOpen(true);
    notify(null);
  }

  function updateEquipmentField(field, value) {
    setEquipmentForm((current) => ({
      ...current,
      [field]: value
    }));
  }

  async function handleEquipmentSubmit(event) {
    event.preventDefault();
    const formElement = event.currentTarget?.form || event.currentTarget;
    if (formElement?.reportValidity && !formElement.reportValidity()) {
      return;
    }

    const payload = buildEquipmentPayload(equipmentForm);
    if (!payload.marca || !payload.tipoEquipamento || !payload.modelo) {
      notify({ type: "error", message: "Informe marca, tipo e modelo para salvar o equipamento." });
      return;
    }

    setSavingEquipment(true);
    notify(null);

    try {
      const isEditing = Boolean(selectedEquipment?.idEquipamento);
      const equipamentoSalvo = isEditing
        ? await api.put(`/equipamentos/${selectedEquipment.idEquipamento}`, payload)
        : await api.post("/equipamentos", payload);

      notify({
        type: "success",
        message: isEditing
          ? `Equipamento ${equipamentoSalvo?.modelo || payload.modelo} atualizado com sucesso.`
          : `Equipamento ${equipamentoSalvo?.modelo || payload.modelo} cadastrado com sucesso.`
      });
      setEquipmentFormOpen(false);
      setSelectedEquipment(null);
      setEquipmentForm(createEmptyEquipmentForm());
      await refreshData({ silent: true });
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel salvar o equipamento."
      });
    } finally {
      setSavingEquipment(false);
    }
  }

  async function handleEquipmentDelete(equipamento) {
    const confirmed = window.confirm(`Excluir o equipamento "${equipmentLabel(equipamento)}"?`);
    if (!confirmed) {
      return;
    }

    setLoading(true);
    notify(null);

    try {
      await api.delete(`/equipamentos/${equipamento.idEquipamento}`);
      notify({ type: "success", message: `Equipamento ${equipamento.modelo} excluido com sucesso.` });
      await refreshData({ silent: true });
    } catch (error) {
      if (error.status === 409) {
        await handleEquipmentDeleteConflict(equipamento);
        return;
      }

      notify({
        type: "error",
        message: error.message || "Nao foi possivel excluir o equipamento."
      });
    } finally {
      setLoading(false);
    }
  }

  async function handleEquipmentDeleteConflict(equipamento) {
    if (!equipamento.ativo) {
      notify({
        type: "error",
        message: `Equipamento ${equipamento.modelo} ja esta inativo e possui OS vinculada.`
      });
      return;
    }

    const shouldInactivate = window.confirm(
      `O equipamento "${equipmentLabel(equipamento)}" possui OS vinculada e nao pode ser excluido sem apagar historico.\n\nDeseja inativar este equipamento?`
    );

    if (!shouldInactivate) {
      notify({ type: "error", message: "Exclusao cancelada. O equipamento possui historico vinculado." });
      return;
    }

    try {
      const payload = buildEquipmentPayload({
        ...equipmentToForm(equipamento),
        ativo: false
      });
      await api.put(`/equipamentos/${equipamento.idEquipamento}`, payload);
      notify({
        type: "success",
        message: `Equipamento ${equipamento.modelo} possui historico vinculado, entao foi inativado.`
      });
      await refreshData({ silent: true });
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel inativar o equipamento."
      });
    }
  }

  return (
    <section className="management-page equipamentos-page" aria-labelledby="equipamentos-title">
      <div className="management-hero">
        <div>
          <span className="section-label">Manutencao tecnica</span>
          <h1 id="equipamentos-title">Equipamentos</h1>
          <p>Visualize, cadastre, edite e exclua ordens de servico da Acao Games.</p>
        </div>
        <div className="management-actions">
          <button type="button" className="ghost-button" onClick={openEquipmentManager}>
            Editar Equipamentos
          </button>
          <button type="button" className="primary-button management-hero__button" onClick={openOrderCreateModal}>
            Registrar entrada
          </button>
        </div>
      </div>

      <section className="client-toolbar" aria-label="Busca e resumo de ordens">
        <label className="search-room client-search">
          <SvgIcon src={searchIcon} size={15} />
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Buscar OS, cliente, equipamento, status ou serie..."
          />
        </label>
        <article className="client-summary-card">
          <span>Total OS</span>
          <strong>{ordens.length}</strong>
        </article>
        <article className="client-summary-card client-summary-card--active">
          <span>Abertas</span>
          <strong>{openOrders}</strong>
        </article>
        <article className="client-summary-card client-summary-card--gold">
          <span>Prontas</span>
          <strong>{readyOrders}</strong>
        </article>
        <article className="client-summary-card">
          <span>Modelos ativos</span>
          <strong>{activeEquipments}</strong>
        </article>
      </section>

      <Panel title="Ordens de servico">
        {loading ? (
          <div className="empty-state">Carregando ordens de servico...</div>
        ) : (
          <div className="table-wrap">
            <table className="compact-table clients-table">
              <thead>
                <tr>
                  <SortableHeader label="OS" sortKey="os" sort={orderSort} onSort={updateOrderSort} />
                  <SortableHeader label="Cliente" sortKey="cliente" sort={orderSort} onSort={updateOrderSort} />
                  <SortableHeader label="Equipamento" sortKey="equipamento" sort={orderSort} onSort={updateOrderSort} />
                  <th>Problema</th>
                  <SortableHeader label="Status" sortKey="status" sort={orderSort} onSort={updateOrderSort} />
                  <SortableHeader label="Entrada" sortKey="entrada" sort={orderSort} onSort={updateOrderSort} />
                  <SortableHeader label="Valor" sortKey="valor" sort={orderSort} onSort={updateOrderSort} />
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {filteredOrders.length ? filteredOrders.map((ordem) => (
                  <tr key={ordem.idOrdemServico}>
                    <td>#{formatNumber(ordem.idOrdemServico)}</td>
                    <td>{ordem.cliente?.nome || "Cliente nao informado"}</td>
                    <td>{equipmentLabel(ordem.equipamento)}</td>
                    <td>{ordem.problemaRelatado || "Nao informado"}</td>
                    <td>
                      <span className={`status-badge status-badge--${statusTone(ordem.status)}`}>
                        {humanizeStatus(ordem.status)}
                      </span>
                    </td>
                    <td>{formatDate(ordem.dataEntrada)}</td>
                    <td>{currencyFormatter.format(parseMoney(ordem.valorServico))}</td>
                    <td>
                      <div className="row-actions">
                        <button type="button" className="table-action" onClick={() => openOrderDetails(ordem)}>
                          Ver
                        </button>
                        {!isFinalizedOrder(ordem) ? (
                          <>
                            <button type="button" className="table-action table-action--edit" onClick={() => openOrderEditModal(ordem)}>
                              Editar
                            </button>
                            {canRegisterBudget(ordem) ? (
                              <button type="button" className="table-action" onClick={() => openOrderBudgetModal(ordem)}>
                                Orcamento
                              </button>
                            ) : null}
                            {canAuthorizeOrder(ordem) ? (
                              <button type="button" className="table-action" onClick={() => openOrderAuthorizationModal(ordem)}>
                                Autorizar
                              </button>
                            ) : null}
                            <button type="button" className="table-action table-action--edit" onClick={() => handleSendToSaida(ordem)}>
                              Saida
                            </button>
                          </>
                        ) : null}
                        <button type="button" className="table-action" onClick={() => openServiceOrderPdf(ordem)}>
                          PDF
                        </button>
                        <button type="button" className="table-action table-action--danger" onClick={() => handleOrderDelete(ordem)}>
                          Excluir
                        </button>
                      </div>
                    </td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan="8">
                      {search ? "Nenhuma OS encontrada para a busca informada." : "Nenhuma OS cadastrada ainda."}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      {orderModalOpen ? (
        <OrderModal
          clientes={clientes}
          equipamentos={equipamentos}
          statuses={statuses}
          form={orderForm}
          mode={orderModalMode}
          selectedOrder={selectedOrder}
          saving={savingOrder}
          onClose={closeOrderModal}
          onSubmit={handleOrderSubmit}
          onChange={updateOrderField}
        />
      ) : null}

      {orderDetailsOpen && selectedOrder ? (
        <OrderDetailsModal
          ordem={selectedOrder}
          onClose={() => setOrderDetailsOpen(false)}
          onEdit={() => {
            setOrderDetailsOpen(false);
            openOrderEditModal(selectedOrder);
          }}
          onBudget={() => openOrderBudgetModal(selectedOrder)}
          onAuthorize={() => openOrderAuthorizationModal(selectedOrder)}
          onSendToSaida={() => handleSendToSaida(selectedOrder)}
        />
      ) : null}

      {equipmentManagerOpen ? (
        <EquipmentManagerModal
          equipamentos={filteredEquipments}
          search={equipmentSearch}
          formOpen={equipmentFormOpen}
          form={equipmentForm}
          selectedEquipment={selectedEquipment}
          saving={savingEquipment}
          onSearch={setEquipmentSearch}
          onClose={() => setEquipmentManagerOpen(false)}
          onNew={openEquipmentCreateForm}
          onCancelForm={() => {
            setEquipmentFormOpen(false);
            setSelectedEquipment(null);
            setEquipmentForm(createEmptyEquipmentForm());
          }}
          onEdit={openEquipmentEditForm}
          onDelete={handleEquipmentDelete}
          onSubmit={handleEquipmentSubmit}
          onChange={updateEquipmentField}
        />
      ) : null}
    </section>
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

function OrderModal({ clientes, equipamentos, statuses, form, mode, selectedOrder, saving, onClose, onSubmit, onChange }) {
  const isCreate = mode === "create";
  const isBudget = mode === "budget";
  const isAuthorization = mode === "authorization";
  const isEditing = Boolean(selectedOrder?.idOrdemServico);
  const title = orderModalTitle(mode, selectedOrder);
  const subtitle = orderModalSubtitle(mode);
  const submitLabel = orderSubmitLabel(mode, isEditing);
  const statusOptions = statuses.filter((status) => status !== "ENTREGUE");
  const valueLabel = isBudget
    ? "Valor previsto"
    : isAuthorization
      ? "Valor autorizado"
      : "Valor do servico";
  const serviceLabel = isAuthorization ? "Servico autorizado" : "Servico realizado";

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal entry-modal--client-records" role="dialog" aria-modal="true" aria-labelledby="order-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">{orderModalEyebrow(mode)}</span>
            <h2 id="order-title">{title}</h2>
            <p>{subtitle}</p>
          </div>
          <div className="entry-modal__actions">
            <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">
              x
            </button>
          </div>
        </header>

        <form className="entry-form" onSubmit={onSubmit}>
          <div className="entry-form__grid">
            <fieldset className="form-card">
              <legend>Cliente e equipamento</legend>
              {isCreate || mode === "edit" ? (
                <div className="form-grid-two">
                  <label className="field field--wide">
                    <span>Cliente</span>
                    <select required value={form.idCliente} onChange={(event) => onChange("idCliente", event.target.value)}>
                      <option value="">Selecione o cliente</option>
                      {clientes.map((cliente) => (
                        <option key={cliente.idCliente} value={cliente.idCliente}>
                          {cliente.nome} {cliente.telefone ? `- ${cliente.telefone}` : ""}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="field field--wide">
                    <span>Equipamento</span>
                    <select required value={form.idEquipamento} onChange={(event) => onChange("idEquipamento", event.target.value)}>
                      <option value="">Selecione o equipamento</option>
                      {equipamentos.map((equipamento) => (
                        <option key={equipamento.idEquipamento} value={equipamento.idEquipamento}>
                          {equipmentLabel(equipamento)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="field">
                    <span>Numero de serie</span>
                    <input value={form.numeroSerie} onChange={(event) => onChange("numeroSerie", event.target.value)} />
                  </label>
                  <label className="field">
                    <span>Cor</span>
                    <input value={form.cor} onChange={(event) => onChange("cor", event.target.value)} />
                  </label>
                </div>
              ) : (
                <div className="selected-card selected-card--stacked">
                  <strong>{selectedOrder?.cliente?.nome || "Cliente nao informado"}</strong>
                  <span>{equipmentLabel(selectedOrder?.equipamento)}</span>
                  <span>OS #{formatNumber(selectedOrder?.idOrdemServico)}</span>
                </div>
              )}
            </fieldset>

            <fieldset className="form-card">
              <legend>Situacao</legend>
              <div className="form-grid-two">
                <label className="field">
                  <span>Data de entrada</span>
                  <input
                    type="date"
                    required
                    readOnly={isBudget || isAuthorization}
                    value={form.dataEntrada}
                    onChange={(event) => onChange("dataEntrada", event.target.value)}
                  />
                </label>
                {isCreate ? (
                  <div className="selected-card selected-card--stacked order-status-card">
                    <strong>Recebido</strong>
                    <span>Status inicial da entrada para manutencao.</span>
                  </div>
                ) : mode === "edit" ? (
                  <label className="field">
                    <span>Status</span>
                    <select value={form.status} onChange={(event) => onChange("status", event.target.value)}>
                      {statusOptions.map((status) => (
                        <option key={status} value={status}>{humanizeStatus(status)}</option>
                      ))}
                    </select>
                  </label>
                ) : (
                  <div className="selected-card selected-card--stacked order-status-card">
                    <strong>{humanizeStatus(form.status)}</strong>
                    <span>Essa atualizacao nao movimenta o caixa.</span>
                  </div>
                )}
                {!isCreate ? (
                  <label className="field">
                    <span>{valueLabel}</span>
                    <input type="number" min="0" step="0.01" value={form.valorServico} onChange={(event) => onChange("valorServico", event.target.value)} />
                  </label>
                ) : null}
                {!isCreate ? (
                  <label className="field">
                    <span>Data da atualizacao</span>
                    <input type="date" value={todayIsoDate()} readOnly />
                  </label>
                ) : null}
              </div>
            </fieldset>

            <fieldset className="form-card form-card--wide">
              <legend>Descricao tecnica</legend>
              <div className="form-grid-two">
                <label className="field field--wide">
                  <span>Problema relatado</span>
                  <textarea required rows="3" value={form.problemaRelatado} onChange={(event) => onChange("problemaRelatado", event.target.value)} />
                </label>
                <label className="field">
                  <span>Diagnostico</span>
                  <textarea rows="3" value={form.diagnostico} onChange={(event) => onChange("diagnostico", event.target.value)} />
                </label>
                <label className="field">
                  <span>{serviceLabel}</span>
                  <textarea rows="3" value={form.servicoRealizado} onChange={(event) => onChange("servicoRealizado", event.target.value)} />
                </label>
                <label className="field field--wide">
                  <span>Observacoes</span>
                  <textarea rows="3" value={form.observacoes} onChange={(event) => onChange("observacoes", event.target.value)} />
                </label>
              </div>
            </fieldset>
          </div>

          <footer className="entry-modal__footer">
            <button type="button" className="ghost-button" onClick={onClose} disabled={saving}>
              Cancelar
            </button>
            <button type="submit" className="primary-button" disabled={saving}>
              {saving ? "Salvando..." : submitLabel}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

function OrderDetailsModal({ ordem, onClose, onEdit, onBudget, onAuthorize, onSendToSaida }) {
  const finalized = isFinalizedOrder(ordem);

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal entry-modal--client" role="dialog" aria-modal="true" aria-labelledby="order-detail-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">Detalhes da OS</span>
            <h2 id="order-detail-title">OS #{formatNumber(ordem.idOrdemServico)}</h2>
            <p>{ordem.cliente?.nome || "Cliente nao informado"} - {equipmentLabel(ordem.equipamento)}</p>
          </div>
          <div className="entry-modal__actions">
            <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">
              x
            </button>
          </div>
        </header>

        <div className="entry-form client-details">
          <div className="client-details__grid">
            <DetailItem label="Cliente" value={ordem.cliente?.nome} />
            <DetailItem label="Telefone" value={ordem.cliente?.telefone} />
            <DetailItem label="Equipamento" value={equipmentLabel(ordem.equipamento)} wide />
            <DetailItem label="Serie" value={ordem.numeroSerie} />
            <DetailItem label="Cor" value={ordem.cor} />
            <DetailItem label="Status" value={humanizeStatus(ordem.status)} />
            <DetailItem label="Entrada" value={formatDate(ordem.dataEntrada)} />
            <DetailItem label="Atualizacao" value={formatDateTime(ordem.dataAtualizacao)} />
            {ordem.dataSaida ? (
              <DetailItem label="Saida" value={formatDate(ordem.dataSaida)} />
            ) : null}
            <DetailItem label="Valor previsto" value={currencyFormatter.format(parseMoney(ordem.valorServico))} />
            <DetailItem label="Problema relatado" value={ordem.problemaRelatado} wide />
            <DetailItem label="Diagnostico" value={ordem.diagnostico} wide />
            <DetailItem label="Servico realizado" value={ordem.servicoRealizado} wide />
            <DetailItem label="Observacoes" value={ordem.observacoes} wide />
          </div>

          <footer className="entry-modal__footer">
            <button type="button" className="ghost-button" onClick={onClose}>
              Fechar
            </button>
            {!finalized ? (
              <>
                {canRegisterBudget(ordem) ? (
                  <button type="button" className="secondary-button" onClick={onBudget}>
                    Registrar orçamento
                  </button>
                ) : null}
                {canAuthorizeOrder(ordem) ? (
                  <button type="button" className="secondary-button" onClick={onAuthorize}>
                    Autorizar serviço
                  </button>
                ) : null}
                <button type="button" className="gold-button" onClick={onSendToSaida}>
                  Enviar para saída
                </button>
              </>
            ) : null}
            <button type="button" className="secondary-button" onClick={() => openServiceOrderPdf(ordem)}>
              Abrir PDF
            </button>
            {!finalized ? (
              <button type="button" className="primary-button" onClick={onEdit}>
                Editar dados
              </button>
            ) : null}
          </footer>
        </div>
      </section>
    </div>
  );
}

function EquipmentManagerModal({
  equipamentos,
  search,
  formOpen,
  form,
  selectedEquipment,
  saving,
  onSearch,
  onClose,
  onNew,
  onCancelForm,
  onEdit,
  onDelete,
  onSubmit,
  onChange
}) {
  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal entry-modal--client-records" role="dialog" aria-modal="true" aria-labelledby="equipment-manager-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">Modelos cadastrados</span>
            <h2 id="equipment-manager-title">Editar Equipamentos</h2>
            <p>Cadastre, altere, exclua ou inative modelos usados nas ordens de servico.</p>
          </div>
          <div className="entry-modal__actions">
            <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">
              x
            </button>
          </div>
        </header>

        <div className="entry-form equipment-manager">
          <div className="equipment-manager__toolbar">
            <label className="search-room client-search">
              <SvgIcon src={searchIcon} size={15} />
              <input
                type="search"
                value={search}
                onChange={(event) => onSearch(event.target.value)}
                placeholder="Buscar marca, tipo, modelo ou descricao..."
              />
            </label>
            <button type="button" className="primary-button" onClick={onNew}>
              Novo equipamento
            </button>
          </div>

          {formOpen ? (
            <form className="equipment-edit-form" onSubmit={onSubmit}>
              <fieldset className="form-card form-card--wide">
                <legend>{selectedEquipment ? "Editar equipamento" : "Novo equipamento"}</legend>
                <div className="form-grid-two">
                  <label className="field">
                    <span>Marca</span>
                    <select required value={form.marca} onChange={(event) => onChange("marca", event.target.value)}>
                      {marcas.map((marca) => <option key={marca} value={marca}>{marca}</option>)}
                    </select>
                  </label>
                  <label className="field">
                    <span>Tipo</span>
                    <select required value={form.tipoEquipamento} onChange={(event) => onChange("tipoEquipamento", event.target.value)}>
                      {tiposEquipamento.map((tipo) => <option key={tipo} value={tipo}>{tipo}</option>)}
                    </select>
                  </label>
                  <label className="field field--wide">
                    <span>Modelo</span>
                    <input required value={form.modelo} onChange={(event) => onChange("modelo", event.target.value)} />
                  </label>
                  <label className="field field--wide">
                    <span>Descricao</span>
                    <textarea rows="3" value={form.descricao} onChange={(event) => onChange("descricao", event.target.value)} />
                  </label>
                  <label className="checkbox-filter client-active-toggle">
                    <input
                      type="checkbox"
                      checked={form.ativo}
                      onChange={(event) => onChange("ativo", event.target.checked)}
                    />
                    <span className="checkbox-control" />
                    <span>Equipamento ativo</span>
                  </label>
                </div>
                <div className="equipment-form-actions">
                  <button type="button" className="ghost-button" onClick={onCancelForm} disabled={saving}>
                    Cancelar
                  </button>
                  <button type="submit" className="primary-button" disabled={saving}>
                    {saving ? "Salvando..." : selectedEquipment ? "Salvar alteracoes" : "Cadastrar equipamento"}
                  </button>
                </div>
              </fieldset>
            </form>
          ) : null}

          <div className="table-wrap equipment-manager__table">
            <table className="compact-table clients-table">
              <thead>
                <tr>
                  <th>Equipamento</th>
                  <th>Marca</th>
                  <th>Tipo</th>
                  <th>Status</th>
                  <th>Cadastro</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {equipamentos.length ? equipamentos.map((equipamento) => (
                  <tr key={equipamento.idEquipamento}>
                    <td>
                      <div className="client-cell">
                        <span className="client-avatar">{equipmentInitials(equipamento)}</span>
                        <div>
                          <strong>{equipamento.modelo}</strong>
                          <small>{equipamento.descricao || "Sem descricao"}</small>
                        </div>
                      </div>
                    </td>
                    <td>{equipamento.marca}</td>
                    <td>{equipamento.tipoEquipamento}</td>
                    <td>
                      <span className={`status-badge status-badge--${equipamento.ativo ? "green" : "red"}`}>
                        {equipamento.ativo ? "Ativo" : "Inativo"}
                      </span>
                    </td>
                    <td>{formatDateTime(equipamento.dataCadastro)}</td>
                    <td>
                      <div className="row-actions">
                        <button type="button" className="table-action table-action--edit" onClick={() => onEdit(equipamento)}>
                          Editar
                        </button>
                        <button type="button" className="table-action table-action--danger" onClick={() => onDelete(equipamento)}>
                          Excluir
                        </button>
                      </div>
                    </td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan="6">
                      {search ? "Nenhum equipamento encontrado para a busca informada." : "Nenhum equipamento cadastrado ainda."}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

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

function openServiceOrderPdf(ordem) {
  const idOrdemServico = typeof ordem === "object" ? ordem?.idOrdemServico : ordem;
  if (!idOrdemServico) {
    return;
  }

  window.open(api.fileUrl(`/ordens-servico/${idOrdemServico}/pdf`), "_blank", "noopener,noreferrer");
}

function equipmentLabel(equipamento) {
  if (!equipamento) {
    return "Equipamento nao informado";
  }

  return [equipamento.marca, equipamento.tipoEquipamento, equipamento.modelo]
    .filter(Boolean)
    .join(" - ");
}

function equipmentInitials(equipamento) {
  return String(equipamento?.modelo || equipamento?.marca || "EQ")
    .trim()
    .slice(0, 2)
    .toUpperCase();
}

function isFinalizedOrder(ordem) {
  return ["ENTREGUE", "CANCELADO"].includes(String(ordem?.status || ""));
}

function canRegisterBudget(ordem) {
  const status = String(ordem?.status || "");
  return !isFinalizedOrder(ordem) && !["APROVADO", "EM_MANUTENCAO", "PRONTO"].includes(status);
}

function canAuthorizeOrder(ordem) {
  const status = String(ordem?.status || "");
  return !isFinalizedOrder(ordem) && !["APROVADO", "EM_MANUTENCAO", "PRONTO"].includes(status);
}

function orderModalEyebrow(mode) {
  if (mode === "budget") {
    return "Orcamento";
  }

  if (mode === "authorization") {
    return "Autorizacao";
  }

  return mode === "edit" ? "Editar dados tecnicos" : "Entrada para manutencao";
}

function orderModalTitle(mode, selectedOrder) {
  const orderNumber = selectedOrder?.idOrdemServico ? ` #${formatNumber(selectedOrder.idOrdemServico)}` : "";

  if (mode === "budget") {
    return `Registrar Orcamento${orderNumber}`;
  }

  if (mode === "authorization") {
    return `Servico Autorizado${orderNumber}`;
  }

  if (mode === "edit") {
    return `Editar OS${orderNumber}`;
  }

  return "Registrar Entrada";
}

function orderModalSubtitle(mode) {
  if (mode === "budget") {
    return "Salve o diagnostico e o valor previsto sem movimentar o caixa.";
  }

  if (mode === "authorization") {
    return "Marque a autorizacao do cliente e confirme o valor tecnico sem lancar pagamento.";
  }

  if (mode === "edit") {
    return "Atualize dados tecnicos e status intermediarios da manutencao.";
  }

  return "Cadastre o aparelho recebido e abra a ordem de servico.";
}

function orderSubmitLabel(mode, isEditing) {
  if (mode === "budget") {
    return "Salvar orcamento";
  }

  if (mode === "authorization") {
    return "Salvar autorizacao";
  }

  return isEditing ? "Salvar alteracoes" : "Registrar entrada";
}

function orderSuccessMessage(mode, ordemSalva, selectedOrder) {
  const idOrdemServico = ordemSalva?.idOrdemServico || selectedOrder?.idOrdemServico;
  const number = formatNumber(idOrdemServico);

  if (mode === "budget") {
    return `Orcamento registrado na OS #${number}. O caixa nao foi alterado.`;
  }

  if (mode === "authorization") {
    return `Autorizacao registrada na OS #${number}. O caixa nao foi alterado.`;
  }

  if (mode === "edit") {
    return `OS #${number} atualizada com sucesso.`;
  }

  return `Entrada registrada. OS #${number} criada com sucesso.`;
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

function statusRank(status) {
  const index = fallbackStatuses.indexOf(String(status || "").toUpperCase());
  return index >= 0 ? index : fallbackStatuses.length;
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

function formatDate(value) {
  if (!value) {
    return "Sem data";
  }

  return new Date(`${value}T00:00:00`).toLocaleDateString("pt-BR");
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

function formatNumber(value) {
  return String(value || 0).padStart(6, "0");
}

function parseMoney(value) {
  const normalized = String(value || "0").replace(",", ".");
  const number = Number(normalized);
  return Number.isFinite(number) ? number : 0;
}

function displayValue(value) {
  const text = String(value || "").trim();
  return text || "Nao informado";
}

function todayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

function optionalText(value) {
  const text = String(value || "").trim();
  return text.length ? text : null;
}
