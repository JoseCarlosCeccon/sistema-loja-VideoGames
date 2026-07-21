import { lazy, startTransition, Suspense, useEffect, useLayoutEffect, useState } from "react";
import { api, AUTH_REJECTED_EVENT, AUTH_STORAGE_KEY } from "./services/api";
import LoginPage from "./pages/Login";
import { formatCpf, formatPhone } from "./utils/masks";
import currentDesignCss from "./current-design.css?raw";
import fundoPersonagensUrl from "./assets/images/FUNDO DE PERSONAGENS.jpg";
import logoAcaoGames from "./assets/images/logo acao games.jpg";
import arrowDownIcon from "./assets/icons/arrow-down-to-line.svg";
import arrowUpIcon from "./assets/icons/arrow-up-from-line.svg";
import cartIcon from "./assets/icons/bootstrap_cart_plus.svg";
import chevronRightIcon from "./assets/icons/chevron-right.svg";
import gameIcon from "./assets/icons/gamepad.svg";
import graphIcon from "./assets/icons/graph-icon.svg";
import homeIcon from "./assets/icons/home.svg";
import packageIcon from "./assets/icons/package.svg";
import cashRegisterIcon from "./assets/icons/phosphor_cash_register.svg";
import userIcon from "./assets/icons/user.svg";

const CaixaPage = lazy(() => import("./pages/Caixa"));
const ClientesPage = lazy(() => import("./pages/Clientes"));
const EquipamentosPage = lazy(() => import("./pages/Equipamentos"));
const EstoquePage = lazy(() => import("./pages/Estoque"));
const RelatoriosPage = lazy(() => import("./pages/Relatorios"));

const navItems = [
  { id: "dashboard", label: "Home", icon: <SvgIcon src={homeIcon} size={14} /> },
  { id: "clientes", label: "Clientes", icon: <SvgIcon src={userIcon} size={14} /> },
  { id: "equipamentos", label: "Equipamentos", icon: <SvgIcon src={gameIcon} size={14} /> },
  { id: "produtos", label: "Estoque", icon: <SvgIcon src={packageIcon} size={14} /> },
  { id: "caixa", label: "Caixa", icon: <SvgIcon src={cashRegisterIcon} size={14} /> },
  { id: "relatorios", label: "Relatorios", icon: <SvgIcon src={graphIcon} size={14} /> }
];

const actionItems = [
  {
    id: "entrada",
    label: "Registrar Entrada",
    description: "Entrada para manutenção de consoles e controles",
    icon: <SvgIcon src={arrowDownIcon} size={20} />,
    tone: "cyan"
  },
  {
    id: "saida",
    label: "Registrar Saída",
    description: "Saída / entrega de equipamentos prontos",
    icon: <SvgIcon src={arrowUpIcon} size={20} />,
    tone: "slate"
  },
  {
    id: "venda",
    label: "Registrar Venda",
    description: "Realizar nova venda de produtos e serviços",
    icon: <SvgIcon src={cartIcon} size={20} />,
    tone: "gold"
  }
];

const emptyDashboard = {
  ordensAbertas: 0,
  ordensProntas: 0,
  produtosEstoqueBaixo: 0,
  valorServicoEmAberto: 0,
  vendasDoDia: 0,
  caixaDoDia: 0,
  saldoCaixa: 0,
  ordensRecentes: [],
  vendasRecentes: []
};

const marcas = ["Sony", "Microsoft", "Nintendo"];
const tiposEquipamento = ["Console", "Controle"];
const formasPagamento = [
  { value: "DINHEIRO", label: "Dinheiro" },
  { value: "PIX", label: "PIX" },
  { value: "CARTAO_DEBITO", label: "Cartao debito" },
  { value: "CARTAO_CREDITO", label: "Cartao credito" }
];
const categoriasProduto = ["Produto", "Acessorio", "Peca", "Insumo", "Servico"];

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL"
});

const currentDesignStyleId = "acao-games-current-design";
const authStorageKey = AUTH_STORAGE_KEY;
const currentDesignStyleContent = currentDesignCss.replace(
  /url\("\.\/assets\/images\/FUNDO DE PERSONAGENS\.jpg"\)/g,
  `url("${fundoPersonagensUrl}")`
);

function App() {
  const [authUser, setAuthUser] = useState(() => readStoredUser());
  const [activeSection, setActiveSection] = useState("dashboard");
  const [themeMode, setThemeMode] = useState(() => {
    if (typeof window === "undefined") {
      return "light";
    }

    const savedTheme = window.localStorage.getItem("acao-games-theme-mode");
    if (savedTheme === "dark" || savedTheme === "light") {
      return savedTheme;
    }

    return window.localStorage.getItem("acao-games-design-mode") === "legacy" ? "dark" : "light";
  });
  const [dashboard, setDashboard] = useState(emptyDashboard);
  const [produtos, setProdutos] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [equipamentos, setEquipamentos] = useState([]);
  const [ordens, setOrdens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [feedback, setFeedback] = useState(null);
  const [authNotice, setAuthNotice] = useState("");
  const [entradaModalOpen, setEntradaModalOpen] = useState(false);
  const [saidaModalOpen, setSaidaModalOpen] = useState(false);
  const [vendaModalOpen, setVendaModalOpen] = useState(false);
  const [clienteMode, setClienteMode] = useState("existente");
  const [equipamentoMode, setEquipamentoMode] = useState("existente");
  const [entradaForm, setEntradaForm] = useState(createEmptyEntradaForm);
  const [saidaForm, setSaidaForm] = useState(createEmptySaidaForm);
  const [vendaForm, setVendaForm] = useState(createEmptyVendaForm);
  const [vendaProductFormOpen, setVendaProductFormOpen] = useState(false);
  const [vendaProductTargetIndex, setVendaProductTargetIndex] = useState(0);
  const [vendaProductForm, setVendaProductForm] = useState(createEmptyVendaProductForm);
  const [savingEntrada, setSavingEntrada] = useState(false);
  const [savingSaida, setSavingSaida] = useState(false);
  const [savingVenda, setSavingVenda] = useState(false);
  const [savingVendaProduct, setSavingVendaProduct] = useState(false);
  const [createdOrder, setCreatedOrder] = useState(null);

  useEffect(() => {
    function handleAuthRejected(event) {
      window.localStorage.removeItem(authStorageKey);
      setAuthUser(null);
      setAuthNotice(event.detail?.message || "Sua sessao expirou. Entre novamente.");
      setFeedback(null);
      setDashboard(emptyDashboard);
      setProdutos([]);
      setClientes([]);
      setEquipamentos([]);
      setOrdens([]);
      setEntradaModalOpen(false);
      setSaidaModalOpen(false);
      setVendaModalOpen(false);
      setVendaProductFormOpen(false);
      startTransition(() => {
        setActiveSection("dashboard");
      });
    }

    window.addEventListener(AUTH_REJECTED_EVENT, handleAuthRejected);
    return () => window.removeEventListener(AUTH_REJECTED_EVENT, handleAuthRejected);
  }, []);

  useEffect(() => {
    if (authUser) {
      loadDashboard();
      return;
    }

    setLoading(false);
  }, [authUser]);

  useLayoutEffect(() => {
    const existingStyle = document.getElementById(currentDesignStyleId);

    if (themeMode === "dark") {
      existingStyle?.remove();
      window.localStorage.setItem("acao-games-theme-mode", "dark");
      return;
    }

    if (!existingStyle) {
      const style = document.createElement("style");
      style.id = currentDesignStyleId;
      style.textContent = currentDesignStyleContent;
      document.head.appendChild(style);
    } else if (existingStyle.textContent !== currentDesignStyleContent) {
      existingStyle.textContent = currentDesignStyleContent;
    }

    window.localStorage.setItem("acao-games-theme-mode", "light");
  }, [themeMode]);

  async function loadDashboard() {
    setLoading(true);

    try {
      const [dashboardData, produtosData, clientesData, equipamentosData, ordensData] = await Promise.all([
        api.get("/dashboard"),
        api.get("/produtos"),
        api.get("/clientes"),
        api.get("/equipamentos"),
        api.get("/ordens-servico")
      ]);

      setDashboard({
        ...emptyDashboard,
        ...(dashboardData || {}),
        ordensRecentes: dashboardData?.ordensRecentes || [],
        vendasRecentes: dashboardData?.vendasRecentes || []
      });
      setProdutos(Array.isArray(produtosData) ? produtosData : []);
      setClientes(Array.isArray(clientesData) ? clientesData : []);
      setEquipamentos(Array.isArray(equipamentosData) ? equipamentosData : []);
      setOrdens(Array.isArray(ordensData) ? ordensData : []);
    } catch (error) {
      setDashboard(emptyDashboard);
      setProdutos([]);
      setClientes([]);
      setEquipamentos([]);
      setOrdens([]);
      setFeedback({
        type: "error",
        message: error.message || "Nao foi possivel carregar os dados do painel."
      });
    } finally {
      setLoading(false);
    }
  }

  function handleAuthenticated(usuario) {
    const sessionUser = sanitizeSessionUser(usuario);
    if (!sessionUser) {
      setFeedback({ type: "error", message: "Resposta de login invalida." });
      return;
    }

    setAuthUser(sessionUser);
    window.localStorage.setItem(authStorageKey, JSON.stringify(sessionUser));
    setAuthNotice("");
    setFeedback(null);
  }

  function handleLogout() {
    window.localStorage.removeItem(authStorageKey);
    setAuthUser(null);
    setAuthNotice("");
    setFeedback(null);
    startTransition(() => {
      setActiveSection("dashboard");
    });
  }

  function navigateToDashboard() {
    startTransition(() => {
      setActiveSection("dashboard");
    });

    document.getElementById("dashboard")?.scrollIntoView({
      behavior: "smooth",
      block: "start"
    });
  }

  function openClientesPage() {
    startTransition(() => {
      setActiveSection("clientes");
    });
    setFeedback(null);
  }

  function openEquipamentosPage() {
    startTransition(() => {
      setActiveSection("equipamentos");
    });
    setFeedback(null);
  }

  function openEstoquePage() {
    startTransition(() => {
      setActiveSection("produtos");
    });
    setFeedback(null);
  }

  function openCaixaPage() {
    startTransition(() => {
      setActiveSection("caixa");
    });
    setFeedback(null);
  }

  function openRelatoriosPage() {
    startTransition(() => {
      setActiveSection("relatorios");
    });
    setFeedback(null);
  }

  function handleMenuItem(item) {
    if (item.id === "dashboard") {
      navigateToDashboard();
      return;
    }

    if (item.id === "clientes") {
      openClientesPage();
      return;
    }

    if (item.id === "equipamentos") {
      openEquipamentosPage();
      return;
    }

    if (item.id === "produtos") {
      openEstoquePage();
      return;
    }

    if (item.id === "caixa") {
      openCaixaPage();
      return;
    }

    if (item.id === "relatorios") {
      openRelatoriosPage();
      return;
    }

  }

  function handleAction(action) {
    if (action.id === "entrada") {
      openEntradaModal();
      return;
    }

    if (action.id === "saida") {
      openSaidaModal();
      return;
    }

    if (action.id === "venda") {
      openVendaModal();
      return;
    }

  }

  function openEntradaModal() {
    startTransition(() => {
      setActiveSection("entrada");
    });
    setFeedback(null);
    setCreatedOrder(null);
    setEntradaModalOpen(true);
  }

  function closeEntradaModal() {
    if (savingEntrada) {
      return;
    }
    setEntradaModalOpen(false);
  }

  function openSaidaModal(ordem = null) {
    startTransition(() => {
      setActiveSection("saida");
    });
    setFeedback(null);
    setSaidaForm(() => {
      const nextForm = createEmptySaidaForm();
      if (!ordem?.idOrdemServico) {
        return nextForm;
      }

      return {
        ...nextForm,
        idOrdemServico: String(ordem.idOrdemServico),
        valorServico: ordem.valorServico != null ? String(ordem.valorServico) : "",
        diagnostico: ordem.diagnostico || "",
        servicoRealizado: ordem.servicoRealizado || ""
      };
    });
    setSaidaModalOpen(true);
  }

  function closeSaidaModal() {
    if (savingSaida) {
      return;
    }
    setSaidaModalOpen(false);
  }

  function openVendaModal() {
    startTransition(() => {
      setActiveSection("venda");
    });
    setFeedback(null);
    setVendaForm(createEmptyVendaForm());
    setVendaProductForm(createEmptyVendaProductForm());
    setVendaProductFormOpen(false);
    setVendaProductTargetIndex(0);
    setVendaModalOpen(true);
  }

  function closeVendaModal() {
    if (savingVenda || savingVendaProduct) {
      return;
    }
    setVendaModalOpen(false);
    setVendaProductFormOpen(false);
  }

  function resetEntradaForm() {
    setEntradaForm(createEmptyEntradaForm());
    setClienteMode("existente");
    setEquipamentoMode("existente");
    setCreatedOrder(null);
  }

  function updateEntradaField(field, value) {
    const formattedValue = field === "clienteTelefone"
      ? formatPhone(value)
      : field === "clienteCpf"
        ? formatCpf(value)
        : value;

    setEntradaForm((current) => ({
      ...current,
      [field]: formattedValue
    }));
  }

  function updateSaidaField(field, value) {
    setSaidaForm((current) => {
      if (field !== "idOrdemServico") {
        return {
          ...current,
          [field]: value
        };
      }

      const selected = ordens.find((ordem) => String(ordem.idOrdemServico) === String(value));
      return {
        ...current,
        idOrdemServico: value,
        valorServico: selected?.valorServico ? String(selected.valorServico) : current.valorServico,
        diagnostico: selected?.diagnostico || current.diagnostico,
        servicoRealizado: selected?.servicoRealizado || current.servicoRealizado
      };
    });
  }

  function updateVendaField(field, value) {
    setVendaForm((current) => {
      if (field !== "formaPagamento") {
        return {
          ...current,
          [field]: value
        };
      }

      return {
        ...current,
        formaPagamento: value,
        itens: current.itens.map((item) => {
          const produto = produtos.find((produtoItem) => String(produtoItem.idProduto) === String(item.idProduto));
          if (!produto) {
            return item;
          }

          return {
            ...item,
            valorUnitario: String(productSalePrice(produto, value))
          };
        })
      };
    });
  }

  function updateVendaItem(index, field, value) {
    setVendaForm((current) => {
      const itens = current.itens.map((item, itemIndex) => {
        if (itemIndex !== index) {
          return item;
        }

        if (field === "idProduto") {
          const produto = produtos.find((produtoItem) => String(produtoItem.idProduto) === String(value));
          return {
            ...item,
            idProduto: value,
            valorUnitario: produto ? String(productSalePrice(produto, current.formaPagamento)) : item.valorUnitario
          };
        }

        return {
          ...item,
          [field]: value
        };
      });

      return {
        ...current,
        itens
      };
    });
  }

  function addVendaItem() {
    setVendaForm((current) => ({
      ...current,
      itens: [...current.itens, createVendaItem()]
    }));
  }

  function removeVendaItem(index) {
    setVendaProductFormOpen(false);
    setVendaForm((current) => ({
      ...current,
      itens: current.itens.length > 1
        ? current.itens.filter((_, itemIndex) => itemIndex !== index)
        : current.itens
    }));
  }

  function openVendaProductForm(index = 0) {
    setVendaProductTargetIndex(index);
    setVendaProductForm(createEmptyVendaProductForm());
    setVendaProductFormOpen(true);
    setFeedback(null);
  }

  function closeVendaProductForm() {
    if (savingVendaProduct) {
      return;
    }

    setVendaProductFormOpen(false);
  }

  function updateVendaProductField(field, value) {
    setVendaProductForm((current) => ({
      ...current,
      [field]: value
    }));
  }

  async function handleVendaProductSubmit(event) {
    event.preventDefault();

    const payload = buildVendaProductPayload(vendaProductForm);
    if (!payload.nome || !payload.categoria) {
      setFeedback({ type: "error", message: "Informe nome e categoria para cadastrar o produto." });
      return;
    }

    if (hasNegativeProductNumbers(vendaProductForm)) {
      setFeedback({ type: "error", message: "Quantidade, estoque minimo e valores nao podem ser negativos." });
      return;
    }

    setSavingVendaProduct(true);
    setFeedback(null);

    try {
      const produto = await api.post("/produtos", payload);
      const idProduto = produto?.idProduto;

      if (idProduto) {
        setProdutos((current) => {
          const lista = Array.isArray(current) ? current : [];
          if (lista.some((item) => String(item.idProduto) === String(idProduto))) {
            return lista.map((item) => String(item.idProduto) === String(idProduto) ? produto : item);
          }

          return [...lista, produto];
        });

        setVendaForm((current) => {
          const targetIndex = Math.min(Number(vendaProductTargetIndex) || 0, current.itens.length - 1);
          return {
            ...current,
            itens: current.itens.map((item, itemIndex) => {
              if (itemIndex !== targetIndex) {
                return item;
              }

              return {
                ...item,
                idProduto: String(idProduto),
                quantidade: item.quantidade || 1,
                valorUnitario: String(productSalePrice(produto, current.formaPagamento))
              };
            })
          };
        });
      }

      setVendaProductForm(createEmptyVendaProductForm());
      setVendaProductFormOpen(false);
      setFeedback({
        type: "success",
        message: `Produto ${produto?.nome || payload.nome} cadastrado e selecionado na venda.`
      });
    } catch (error) {
      setFeedback({
        type: "error",
        message: error.message || "Nao foi possivel cadastrar o produto."
      });
    } finally {
      setSavingVendaProduct(false);
    }
  }

  async function handleEntradaSubmit(event, shouldOpenPdf = false) {
    event.preventDefault();
    if (!validateFormEvent(event)) {
      return;
    }

    setSavingEntrada(true);
    setFeedback(null);

    try {
      const cliente = await resolveCliente();
      const equipamento = await resolveEquipamento();
      const ordem = await api.post("/ordens-servico", {
        idCliente: cliente.idCliente,
        idEquipamento: equipamento.idEquipamento,
        numeroSerie: optionalText(entradaForm.numeroSerie),
        cor: optionalText(entradaForm.cor),
        dataEntrada: entradaForm.dataEntrada,
        problemaRelatado: entradaForm.problemaRelatado,
        status: "RECEBIDO",
        observacoes: buildEntradaObservacoes(entradaForm)
      });

      setCreatedOrder(ordem);
      setFeedback({
        type: "success",
        message: `Entrada registrada com sucesso. OS #${formatOrderNumber(ordem.idOrdemServico)} criada.`
      });
      await loadDashboard();

      if (shouldOpenPdf) {
        openServiceOrderPdf(ordem);
      }
    } catch (error) {
      setFeedback({
        type: "error",
        message: error.message || "Nao foi possivel registrar a entrada."
      });
    } finally {
      setSavingEntrada(false);
    }
  }

  async function handleSaidaSubmit(event, shouldOpenPdf = false) {
    event.preventDefault();
    if (!validateFormEvent(event)) {
      return;
    }

    setSavingSaida(true);
    setFeedback(null);

    try {
      const valorServico = parseMoney(saidaForm.valorServico);
      const ordem = await api.post(`/ordens-servico/${saidaForm.idOrdemServico}/saida`, {
        dataSaida: saidaForm.dataSaida,
        diagnostico: optionalText(saidaForm.diagnostico),
        servicoRealizado: optionalText(saidaForm.servicoRealizado),
        valorServico,
        formaPagamento: valorServico > 0 ? saidaForm.formaPagamento : null,
        observacoes: optionalText(saidaForm.observacoes)
      });

      setFeedback({
        type: "success",
        message: `Saida registrada. OS #${formatOrderNumber(ordem.idOrdemServico)} entregue e caixa atualizado.`
      });
      setSaidaModalOpen(false);
      await loadDashboard();

      if (shouldOpenPdf) {
        openServiceOrderPdf(ordem);
      }
    } catch (error) {
      setFeedback({
        type: "error",
        message: error.message || "Nao foi possivel registrar a saida."
      });
    } finally {
      setSavingSaida(false);
    }
  }

  async function handleVendaSubmit(event, shouldOpenPdf = false) {
    event.preventDefault();
    if (!validateFormEvent(event)) {
      return;
    }

    setSavingVenda(true);
    setFeedback(null);

    try {
      const venda = await api.post("/vendas", {
        idCliente: vendaForm.idCliente ? Number(vendaForm.idCliente) : null,
        dataVenda: vendaForm.dataVenda,
        formaPagamento: vendaForm.formaPagamento,
        observacoes: optionalText(vendaForm.observacoes),
        itens: vendaForm.itens.map((item) => ({
          idProduto: Number(item.idProduto),
          quantidade: Number(item.quantidade),
          valorUnitario: parseMoney(item.valorUnitario)
        }))
      });

      setFeedback({
        type: "success",
        message: `Venda #${formatOrderNumber(venda.idVenda)} registrada. Estoque e caixa atualizados.`
      });
      setVendaModalOpen(false);
      await loadDashboard();

      if (shouldOpenPdf) {
        openSaleReceiptPdf(venda);
      }
    } catch (error) {
      setFeedback({
        type: "error",
        message: error.message || "Nao foi possivel registrar a venda."
      });
    } finally {
      setSavingVenda(false);
    }
  }

  async function resolveCliente() {
    if (clienteMode === "existente") {
      const selected = clientes.find((cliente) => String(cliente.idCliente) === String(entradaForm.idCliente));
      if (!selected) {
        throw new Error("Selecione um cliente cadastrado ou marque a opcao de novo cliente.");
      }
      return selected;
    }

    return api.post("/clientes", {
      nome: entradaForm.clienteNome,
      telefone: formatPhone(entradaForm.clienteTelefone),
      cpf: optionalText(formatCpf(entradaForm.clienteCpf)),
      endereco: optionalText(entradaForm.clienteEndereco),
      email: optionalText(entradaForm.clienteEmail),
      observacoes: optionalText(entradaForm.clienteObservacoes),
      ativo: true
    });
  }

  async function resolveEquipamento() {
    if (equipamentoMode === "existente") {
      const selected = equipamentos.find(
        (equipamento) => String(equipamento.idEquipamento) === String(entradaForm.idEquipamento)
      );
      if (!selected) {
        throw new Error("Selecione um equipamento cadastrado ou marque a opcao de novo modelo.");
      }
      return selected;
    }

    return api.post("/equipamentos", {
      marca: entradaForm.marca,
      tipoEquipamento: entradaForm.tipoEquipamento,
      modelo: entradaForm.modelo,
      descricao: optionalText(entradaForm.equipamentoDescricao),
      ativo: true
    });
  }

  const recentOrders = (dashboard.ordensRecentes || []).slice(0, 5);
  const recentSales = (dashboard.vendasRecentes || []).slice(0, 5);
  const isStandalonePage = activeSection === "clientes"
    || activeSection === "equipamentos"
    || activeSection === "produtos"
    || activeSection === "caixa"
    || activeSection === "relatorios";

  if (!authUser) {
    return <LoginPage message={authNotice} onAuthenticated={handleAuthenticated} />;
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <button type="button" className="brand-link" onClick={navigateToDashboard} aria-label="Acao Games">
          <img src={logoAcaoGames} alt="Logo Acao Games" className="brand-logo-image" />
        </button>

        <nav className="top-nav" aria-label="Navegacao principal">
          {navItems.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`top-nav__item ${activeSection === item.id ? "top-nav__item--active" : ""}`}
              onClick={() => handleMenuItem(item)}
            >
              {item.icon}
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="header-actions">
          <button
            type="button"
            className="header-link theme-toggle-button"
            aria-pressed={themeMode === "dark"}
            onClick={() => setThemeMode((current) => (current === "light" ? "dark" : "light"))}
          >
            <span className={`theme-toggle-icon theme-toggle-icon--${themeMode === "light" ? "dark" : "light"}`} aria-hidden="true" />
            {themeMode === "light" ? "Tema escuro" : "Tema claro"}
          </button>
          <div className="header-user-chip">
            <span>{authUser.nome || authUser.login}</span>
            <small>{humanizeStatus(authUser.perfil)}</small>
          </div>
          <button type="button" className="header-link" onClick={handleLogout}>
            Sair
          </button>
        </div>
      </header>

      <main className="main-content">
        {feedback ? (
          <div className={`feedback feedback--${feedback.type}`}>
            {feedback.message}
          </div>
        ) : null}

        <Suspense fallback={<div className="empty-state">Carregando pagina...</div>}>
          {activeSection === "clientes" ? (
            <ClientesPage
              clientes={clientes}
              onClientesChange={setClientes}
              onFeedback={setFeedback}
            />
          ) : null}

          {activeSection === "equipamentos" ? (
            <EquipamentosPage
              clientes={clientes}
              equipamentos={equipamentos}
              ordens={ordens}
              onClientesChange={setClientes}
              onEquipamentosChange={setEquipamentos}
              onOrdensChange={setOrdens}
              onFeedback={setFeedback}
              onOpenSaida={openSaidaModal}
            />
          ) : null}

          {activeSection === "produtos" ? (
            <EstoquePage
              produtos={produtos}
              onProdutosChange={setProdutos}
              onFeedback={setFeedback}
            />
          ) : null}

          {activeSection === "caixa" ? (
            <CaixaPage
              onFeedback={setFeedback}
              onDashboardRefresh={loadDashboard}
            />
          ) : null}

          {activeSection === "relatorios" ? (
            <RelatoriosPage
              onFeedback={setFeedback}
            />
          ) : null}
        </Suspense>

        <div className={isStandalonePage ? "screen-hidden" : "dashboard-view"}>
        <section id="dashboard" className="hero-lobby">
          <div className="hero-lobby__center">
            <span className="section-label">Painel operacional</span>
            <h1>Central de Atendimento</h1>
            <p>Registre entradas, finalize saídas e acompanhe as vendas da Ação Games.</p>
            <div className="hero-cta-row">
              {actionItems.map((action) => (
                <button
                  key={action.id}
                  type="button"
                  className={`hero-cta hero-cta--${action.tone}`}
                  onClick={() => handleAction(action)}
                >
                  <span className="hero-cta__icon">{action.icon}</span>
                  <span className="hero-cta__copy">
                    <strong>{action.label}</strong>
                    <small>{action.description}</small>
                  </span>
                  <SvgIcon src={chevronRightIcon} size={15} />
                </button>
              ))}
            </div>
          </div>
        </section>

        <section className="metric-grid" aria-label="Resumo">
          <MetricCard label="OS em aberto" value={dashboard.ordensAbertas} accent="cyan" />
          <MetricCard label="Prontos para entrega" value={dashboard.ordensProntas} accent="green" />
          <MetricCard label="Vendas do dia" value={currencyFormatter.format(dashboard.vendasDoDia || 0)} accent="blue" />
          <MetricCard label="Caixa do dia" value={currencyFormatter.format(dashboard.caixaDoDia || 0)} accent="gold" />
          <MetricCard label="Estoque baixo" value={dashboard.produtosEstoqueBaixo} accent="red" />
        </section>

        <section className="dashboard-main-grid">
          <Panel title="Ordens de Servico Recentes">
            <div className="table-wrap">
              <table className="compact-table">
                <thead>
                  <tr>
                    <th>N OS</th>
                    <th>Cliente</th>
                    <th>Equipamento</th>
                    <th>Problema</th>
                    <th>Status</th>
                    <th>Entrada</th>
                    <th>PDF</th>
                  </tr>
                </thead>
                <tbody>
                  {recentOrders.length ? recentOrders.map((ordem) => (
                    <tr key={ordem.idOrdemServico}>
                      <td>#{formatOrderNumber(ordem.idOrdemServico)}</td>
                      <td>{ordem.cliente?.nome || "Cliente nao informado"}</td>
                      <td>{equipamentoLabel(ordem.equipamento)}</td>
                      <td>{ordem.problemaRelatado || "Nao informado"}</td>
                      <td><StatusBadge status={ordem.status} /></td>
                      <td>{formatDate(ordem.dataEntrada)}</td>
                      <td>
                        <button type="button" className="table-action" onClick={() => openServiceOrderPdf(ordem)}>
                          PDF
                        </button>
                      </td>
                    </tr>
                  )) : (
                    <tr>
                      <td colSpan="7">Nenhuma ordem de servico cadastrada ainda.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </Panel>

          <Panel title="Vendas Recentes">
            <div className="sales-list">
              {recentSales.length ? recentSales.map((item) => (
                <article key={item.idVenda || item.idProduto || item.nome} className="sale-row">
                  <div className="sale-thumb">{saleInitials(item)}</div>
                  <div>
                    <strong>{item.nome || `Venda #${formatOrderNumber(item.idVenda)}`}</strong>
                    <span>{item.categoria || item.formaPagamento || "Venda"}</span>
                  </div>
                  <strong>{currencyFormatter.format(item.valorTotal || item.valorVenda || 0)}</strong>
                </article>
              )) : (
                <div className="empty-state">Nenhuma venda registrada ainda.</div>
              )}
            </div>
          </Panel>
        </section>

        {loading ? <div className="loading-overlay">Carregando dados...</div> : null}
        </div>
      </main>

      {entradaModalOpen ? (
        <EntradaModal
          clientes={clientes}
          equipamentos={equipamentos}
          clienteMode={clienteMode}
          equipamentoMode={equipamentoMode}
          form={entradaForm}
          createdOrder={createdOrder}
          saving={savingEntrada}
          onClose={closeEntradaModal}
          onSubmit={handleEntradaSubmit}
          onOpenPdf={openServiceOrderPdf}
          onReset={resetEntradaForm}
          onChange={updateEntradaField}
          onClienteModeChange={setClienteMode}
          onEquipamentoModeChange={setEquipamentoMode}
        />
      ) : null}

      {saidaModalOpen ? (
        <SaidaModal
          ordens={ordens}
          form={saidaForm}
          saving={savingSaida}
          onClose={closeSaidaModal}
          onSubmit={handleSaidaSubmit}
          onChange={updateSaidaField}
        />
      ) : null}

      {vendaModalOpen ? (
        <VendaModal
          clientes={clientes}
          produtos={produtos}
          categoriasProduto={categoriasProduto}
          form={vendaForm}
          saving={savingVenda}
          productFormOpen={vendaProductFormOpen}
          productTargetIndex={vendaProductTargetIndex}
          productForm={vendaProductForm}
          savingProduct={savingVendaProduct}
          onClose={closeVendaModal}
          onSubmit={handleVendaSubmit}
          onChange={updateVendaField}
          onItemChange={updateVendaItem}
          onAddItem={addVendaItem}
          onRemoveItem={removeVendaItem}
          onOpenProductForm={openVendaProductForm}
          onCloseProductForm={closeVendaProductForm}
          onProductTargetChange={(value) => setVendaProductTargetIndex(Number(value) || 0)}
          onProductChange={updateVendaProductField}
          onProductSubmit={handleVendaProductSubmit}
        />
      ) : null}
    </div>
  );
}

function EntradaModal({
  clientes,
  equipamentos,
  clienteMode,
  equipamentoMode,
  form,
  createdOrder,
  saving,
  onClose,
  onSubmit,
  onOpenPdf,
  onReset,
  onChange,
  onClienteModeChange,
  onEquipamentoModeChange
}) {
  const selectedCliente = clientes.find((cliente) => String(cliente.idCliente) === String(form.idCliente));
  const selectedEquipamento = equipamentos.find(
    (equipamento) => String(equipamento.idEquipamento) === String(form.idEquipamento)
  );

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal" role="dialog" aria-modal="true" aria-labelledby="entrada-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">Nova ordem de servico</span>
            <h2 id="entrada-title">Registrar Entrada</h2>
            <p>Preencha os dados recebidos no balcao e gere a OS para impressao.</p>
          </div>
          <div className="entry-modal__actions">
            <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">
              x
            </button>
          </div>
        </header>

        <form id="entrada-form" className="entry-form" onSubmit={(event) => onSubmit(event, false)}>
          <div className="entry-form__grid">
            <fieldset className="form-card">
              <legend>Cliente</legend>
              <SegmentedControl
                value={clienteMode}
                options={[
                  { value: "existente", label: "Cadastrado" },
                  { value: "novo", label: "Novo cliente" }
                ]}
                onChange={onClienteModeChange}
              />

              {clienteMode === "existente" ? (
                <>
                  <label className="field">
                    <span>Cliente cadastrado</span>
                    <select required value={form.idCliente} onChange={(event) => onChange("idCliente", event.target.value)}>
                      <option value="">Selecione o cliente</option>
                      {clientes.map((cliente) => (
                        <option key={cliente.idCliente} value={cliente.idCliente}>
                          {cliente.nome} {cliente.telefone ? `- ${cliente.telefone}` : ""}
                        </option>
                      ))}
                    </select>
                  </label>
                  {!clientes.length ? (
                    <p className="form-hint">Nenhum cliente cadastrado. Use a opcao "Novo cliente".</p>
                  ) : null}
                  {selectedCliente ? (
                    <div className="selected-card">
                      <strong>{selectedCliente.nome}</strong>
                      <span>{selectedCliente.telefone || "Telefone nao informado"}</span>
                    </div>
                  ) : null}
                </>
              ) : (
                <div className="form-grid-two">
                  <label className="field">
                    <span>Nome</span>
                    <input required value={form.clienteNome} onChange={(event) => onChange("clienteNome", event.target.value)} />
                  </label>
                  <label className="field">
                    <span>Telefone</span>
                    <input
                      required
                      inputMode="numeric"
                      maxLength={15}
                      placeholder="(28) 99999-9999"
                      value={form.clienteTelefone}
                      onChange={(event) => onChange("clienteTelefone", event.target.value)}
                    />
                  </label>
                  <label className="field">
                    <span>CPF opcional</span>
                    <input
                      inputMode="numeric"
                      maxLength={14}
                      placeholder="000.000.000-00"
                      value={form.clienteCpf}
                      onChange={(event) => onChange("clienteCpf", event.target.value)}
                    />
                  </label>
                  <label className="field">
                    <span>Email opcional</span>
                    <input type="email" value={form.clienteEmail} onChange={(event) => onChange("clienteEmail", event.target.value)} />
                  </label>
                  <label className="field field--wide">
                    <span>Endereco</span>
                    <input value={form.clienteEndereco} onChange={(event) => onChange("clienteEndereco", event.target.value)} />
                  </label>
                  <label className="field field--wide">
                    <span>Observacoes do cliente</span>
                    <textarea rows="2" value={form.clienteObservacoes} onChange={(event) => onChange("clienteObservacoes", event.target.value)} />
                  </label>
                </div>
              )}
            </fieldset>

            <fieldset className="form-card">
              <legend>Equipamento</legend>
              <SegmentedControl
                value={equipamentoMode}
                options={[
                  { value: "existente", label: "Modelo cadastrado" },
                  { value: "novo", label: "Novo modelo" }
                ]}
                onChange={onEquipamentoModeChange}
              />

              {equipamentoMode === "existente" ? (
                <>
                  <label className="field">
                    <span>Modelo do equipamento</span>
                    <select required value={form.idEquipamento} onChange={(event) => onChange("idEquipamento", event.target.value)}>
                      <option value="">Selecione o modelo</option>
                      {equipamentos.map((equipamento) => (
                        <option key={equipamento.idEquipamento} value={equipamento.idEquipamento}>
                          {equipamentoLabel(equipamento)}
                        </option>
                      ))}
                    </select>
                  </label>
                  {!equipamentos.length ? (
                    <p className="form-hint">Nenhum modelo cadastrado. Use a opcao "Novo modelo".</p>
                  ) : null}
                  {selectedEquipamento ? (
                    <div className="selected-card">
                      <strong>{selectedEquipamento.modelo}</strong>
                      <span>{selectedEquipamento.marca} - {selectedEquipamento.tipoEquipamento}</span>
                    </div>
                  ) : null}
                </>
              ) : (
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
                    <span>Descricao do modelo</span>
                    <textarea rows="2" value={form.equipamentoDescricao} onChange={(event) => onChange("equipamentoDescricao", event.target.value)} />
                  </label>
                </div>
              )}
            </fieldset>

            <fieldset className="form-card form-card--wide">
              <legend>Dados da entrada</legend>
              <div className="form-grid-three">
                <label className="field">
                  <span>Data de entrada</span>
                  <input type="date" required value={form.dataEntrada} onChange={(event) => onChange("dataEntrada", event.target.value)} />
                </label>
                <label className="field">
                  <span>Numero de serie</span>
                  <input value={form.numeroSerie} onChange={(event) => onChange("numeroSerie", event.target.value)} />
                </label>
                <label className="field">
                  <span>Cor</span>
                  <input value={form.cor} onChange={(event) => onChange("cor", event.target.value)} />
                </label>
                <label className="field field--wide">
                  <span>Problema relatado</span>
                  <textarea required rows="3" value={form.problemaRelatado} onChange={(event) => onChange("problemaRelatado", event.target.value)} />
                </label>
                <label className="field">
                  <span>Acessorios recebidos</span>
                  <input placeholder="Controle, cabo HDMI, fonte..." value={form.acessorios} onChange={(event) => onChange("acessorios", event.target.value)} />
                </label>
                <label className="field">
                  <span>Estado fisico</span>
                  <input placeholder="Riscos, marcas, lacres..." value={form.estadoFisico} onChange={(event) => onChange("estadoFisico", event.target.value)} />
                </label>
                <label className="field field--wide">
                  <span>Observacoes internas</span>
                  <textarea rows="3" value={form.observacoes} onChange={(event) => onChange("observacoes", event.target.value)} />
                </label>
              </div>
            </fieldset>
          </div>

          {createdOrder ? (
            <div className="created-order-card">
              <div>
                <span>Registro concluido</span>
                <strong>OS #{formatOrderNumber(createdOrder.idOrdemServico)}</strong>
                <small>{createdOrder.cliente?.nome} - {equipamentoLabel(createdOrder.equipamento)}</small>
              </div>
              <button type="button" className="secondary-button" onClick={() => onOpenPdf(createdOrder)}>
                Gerar PDF
              </button>
            </div>
          ) : null}

          <footer className="entry-modal__footer">
            <button type="button" className="ghost-button" onClick={onClose} disabled={saving}>
              Cancelar
            </button>
            <button type="button" className="ghost-button" onClick={onReset} disabled={saving}>
              Limpar
            </button>
            <button type="submit" className="primary-button" disabled={saving}>
              {saving ? "Salvando..." : "Salvar entrada"}
            </button>
            <button type="button" className="gold-button" disabled={saving} onClick={(event) => onSubmit(event, true)}>
              Salvar e gerar PDF
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

function SaidaModal({ ordens, form, saving, onClose, onSubmit, onChange }) {
  const ordensDisponiveis = ordens.filter((ordem) => !["ENTREGUE", "CANCELADO"].includes(String(ordem.status || "")));
  const selectedOrder = ordens.find((ordem) => String(ordem.idOrdemServico) === String(form.idOrdemServico));
  const valorServico = parseMoney(form.valorServico);

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal" role="dialog" aria-modal="true" aria-labelledby="saida-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">Entrega ao cliente</span>
            <h2 id="saida-title">Registrar Saida</h2>
            <p>Finalize a OS, registre o pagamento do servico e movimente o caixa.</p>
          </div>
          <div className="entry-modal__actions">
            <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">
              x
            </button>
          </div>
        </header>

        <form id="saida-form" className="entry-form" onSubmit={(event) => onSubmit(event, false)}>
          <div className="entry-form__grid">
            <fieldset className="form-card form-card--wide">
              <legend>Ordem de servico</legend>
              <label className="field">
                <span>OS para entrega</span>
                <select required value={form.idOrdemServico} onChange={(event) => onChange("idOrdemServico", event.target.value)}>
                  <option value="">Selecione uma OS</option>
                  {ordensDisponiveis.map((ordem) => (
                    <option key={ordem.idOrdemServico} value={ordem.idOrdemServico}>
                      #{formatOrderNumber(ordem.idOrdemServico)} - {ordem.cliente?.nome || "Cliente"} - {equipamentoLabel(ordem.equipamento)}
                    </option>
                  ))}
                </select>
              </label>

              {!ordensDisponiveis.length ? (
                <p className="form-hint">Nenhuma OS aberta ou pronta para entrega foi encontrada.</p>
              ) : null}

              {selectedOrder ? (
                <div className="selected-card selected-card--stacked">
                  <strong>#{formatOrderNumber(selectedOrder.idOrdemServico)} - {selectedOrder.cliente?.nome}</strong>
                  <span>{equipamentoLabel(selectedOrder.equipamento)}</span>
                  <span>{selectedOrder.problemaRelatado || "Problema nao informado"}</span>
                </div>
              ) : null}
            </fieldset>

            <fieldset className="form-card">
              <legend>Pagamento</legend>
              <div className="form-grid-two">
                <label className="field">
                  <span>Data de saida</span>
                  <input type="date" required value={form.dataSaida} onChange={(event) => onChange("dataSaida", event.target.value)} />
                </label>
                <label className="field">
                  <span>Valor do servico</span>
                  <input type="number" min="0" step="0.01" value={form.valorServico} onChange={(event) => onChange("valorServico", event.target.value)} />
                </label>
                <label className="field field--wide">
                  <span>Forma de pagamento</span>
                  <select required={valorServico > 0} value={form.formaPagamento} onChange={(event) => onChange("formaPagamento", event.target.value)}>
                    {formasPagamento.map((forma) => (
                      <option key={forma.value} value={forma.value}>{forma.label}</option>
                    ))}
                  </select>
                </label>
              </div>
              <div className="total-card">
                <span>Total do servico</span>
                <strong>{currencyFormatter.format(valorServico)}</strong>
                <small>Esse valor entra no caixa do dia ao registrar a saida.</small>
              </div>
            </fieldset>

            <fieldset className="form-card">
              <legend>Conclusao tecnica</legend>
              <div className="form-grid-two">
                <label className="field field--wide">
                  <span>Diagnostico</span>
                  <textarea rows="3" value={form.diagnostico} onChange={(event) => onChange("diagnostico", event.target.value)} />
                </label>
                <label className="field field--wide">
                  <span>Servico realizado</span>
                  <textarea rows="3" value={form.servicoRealizado} onChange={(event) => onChange("servicoRealizado", event.target.value)} />
                </label>
                <label className="field field--wide">
                  <span>Observacoes de saida</span>
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
              {saving ? "Salvando..." : "Salvar saida"}
            </button>
            <button type="button" className="gold-button" disabled={saving} onClick={(event) => onSubmit(event, true)}>
              Salvar saida e gerar PDF
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

function VendaModal({
  clientes,
  produtos,
  categoriasProduto,
  form,
  saving,
  productFormOpen,
  productTargetIndex,
  productForm,
  savingProduct,
  onClose,
  onSubmit,
  onChange,
  onItemChange,
  onAddItem,
  onRemoveItem,
  onOpenProductForm,
  onCloseProductForm,
  onProductTargetChange,
  onProductChange,
  onProductSubmit
}) {
  const total = calculateVendaTotal(form.itens);

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal" role="dialog" aria-modal="true" aria-labelledby="venda-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">Venda de produtos</span>
            <h2 id="venda-title">Registrar Venda</h2>
            <p>Selecione os produtos vendidos, a forma de pagamento e confirme a baixa no estoque.</p>
          </div>
          <div className="entry-modal__actions">
            <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">
              x
            </button>
          </div>
        </header>

        <form id="venda-form" className="entry-form" onSubmit={(event) => onSubmit(event, false)}>
          <div className="entry-form__grid">
            <fieldset className="form-card">
              <legend>Dados da venda</legend>
              <div className="form-grid-two">
                <label className="field">
                  <span>Data</span>
                  <input type="date" required value={form.dataVenda} onChange={(event) => onChange("dataVenda", event.target.value)} />
                </label>
                <label className="field">
                  <span>Forma de pagamento</span>
                  <select required value={form.formaPagamento} onChange={(event) => onChange("formaPagamento", event.target.value)}>
                    {formasPagamento.map((forma) => (
                      <option key={forma.value} value={forma.value}>{forma.label}</option>
                    ))}
                  </select>
                </label>
                <label className="field field--wide">
                  <span>Cliente</span>
                  <select value={form.idCliente} onChange={(event) => onChange("idCliente", event.target.value)}>
                    <option value="">Venda balcao / sem cliente</option>
                    {clientes.map((cliente) => (
                      <option key={cliente.idCliente} value={cliente.idCliente}>
                        {cliente.nome} {cliente.telefone ? `- ${cliente.telefone}` : ""}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field field--wide">
                  <span>Observacoes</span>
                  <textarea rows="2" value={form.observacoes} onChange={(event) => onChange("observacoes", event.target.value)} />
                </label>
              </div>
            </fieldset>

            <fieldset className="form-card">
              <legend>Resumo</legend>
              <div className="total-card total-card--large">
                <span>Total da venda</span>
                <strong>{currencyFormatter.format(total)}</strong>
                <small>Ao salvar, o estoque baixa e o caixa recebe uma entrada.</small>
              </div>
            </fieldset>

            <fieldset className="form-card form-card--wide">
              <legend>Produtos</legend>
              <div className="sale-items">
                {form.itens.map((item, index) => {
                  const produto = produtos.find((produtoItem) => String(produtoItem.idProduto) === String(item.idProduto));
                  const subtotal = Number(item.quantidade || 0) * parseMoney(item.valorUnitario);

                  return (
                    <div className="sale-item-row" key={index}>
                      <label className="field">
                        <span>Produto</span>
                        <select required value={item.idProduto} onChange={(event) => onItemChange(index, "idProduto", event.target.value)}>
                          <option value="">Selecione</option>
                          {produtos.map((produtoItem) => (
                            <option key={produtoItem.idProduto} value={produtoItem.idProduto}>
                              {produtoItem.nome} - estoque {produtoItem.quantidadeEstoque}
                            </option>
                          ))}
                        </select>
                      </label>
                      <label className="field">
                        <span>Qtd</span>
                        <input type="number" min="1" required value={item.quantidade} onChange={(event) => onItemChange(index, "quantidade", event.target.value)} />
                      </label>
                      <label className="field">
                        <span>Valor unitario</span>
                        <input type="number" min="0" step="0.01" required value={item.valorUnitario} onChange={(event) => onItemChange(index, "valorUnitario", event.target.value)} />
                      </label>
                      <div className="sale-item-subtotal">
                        <span>{produto?.categoria || "Subtotal"}</span>
                        <strong>{currencyFormatter.format(subtotal)}</strong>
                      </div>
                      <button type="button" className="remove-item-button" onClick={() => onRemoveItem(index)} disabled={form.itens.length === 1}>
                        Remover
                      </button>
                    </div>
                  );
                })}
              </div>
              <div className="sale-products-actions">
                <button type="button" className="secondary-button" onClick={onAddItem}>
                  Adicionar produto
                </button>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => onOpenProductForm(Math.max(form.itens.length - 1, 0))}
                >
                  Criar produto
                </button>
              </div>
              {productFormOpen ? (
                <div className="quick-product-panel">
                  <div className="quick-product-panel__head">
                    <strong>Novo produto</strong>
                    <button type="button" className="ghost-button" onClick={onCloseProductForm} disabled={savingProduct}>
                      Fechar
                    </button>
                  </div>
                  <div className="form-grid-three">
                    <label className="field">
                      <span>Linha da venda</span>
                      <select value={productTargetIndex} onChange={(event) => onProductTargetChange(event.target.value)}>
                        {form.itens.map((_, index) => (
                          <option key={index} value={index}>Item {index + 1}</option>
                        ))}
                      </select>
                    </label>
                    <label className="field">
                      <span>Nome</span>
                      <input value={productForm.nome} onChange={(event) => onProductChange("nome", event.target.value)} />
                    </label>
                    <label className="field">
                      <span>Categoria</span>
                      <input
                        list="sale-product-categories"
                        value={productForm.categoria}
                        onChange={(event) => onProductChange("categoria", event.target.value)}
                      />
                      <datalist id="sale-product-categories">
                        {categoriasProduto.map((categoria) => (
                          <option key={categoria} value={categoria} />
                        ))}
                      </datalist>
                    </label>
                    <label className="field">
                      <span>Quantidade</span>
                      <input
                        type="number"
                        min="0"
                        step="1"
                        value={productForm.quantidadeEstoque}
                        onChange={(event) => onProductChange("quantidadeEstoque", event.target.value)}
                      />
                    </label>
                    <label className="field">
                      <span>Estoque minimo</span>
                      <input
                        type="number"
                        min="0"
                        step="1"
                        value={productForm.estoqueMinimo}
                        onChange={(event) => onProductChange("estoqueMinimo", event.target.value)}
                      />
                    </label>
                    <label className="field">
                      <span>Valor de compra</span>
                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={productForm.valorCompra}
                        onChange={(event) => onProductChange("valorCompra", event.target.value)}
                      />
                    </label>
                    <label className="field">
                      <span>Valor a vista</span>
                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={productForm.valorVenda}
                        onChange={(event) => onProductChange("valorVenda", event.target.value)}
                      />
                    </label>
                    <label className="field">
                      <span>Valor a prazo</span>
                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={productForm.valorVendaPrazo}
                        onChange={(event) => onProductChange("valorVendaPrazo", event.target.value)}
                      />
                    </label>
                    <label className="field field--wide">
                      <span>Descricao</span>
                      <textarea rows="2" value={productForm.descricao} onChange={(event) => onProductChange("descricao", event.target.value)} />
                    </label>
                  </div>
                  <div className="quick-product-panel__actions">
                    <button type="button" className="primary-button" onClick={onProductSubmit} disabled={savingProduct}>
                      {savingProduct ? "Cadastrando..." : "Cadastrar e selecionar"}
                    </button>
                  </div>
                </div>
              ) : null}
              {!produtos.length ? (
                <p className="form-hint">Nenhum produto cadastrado para venda.</p>
              ) : null}
            </fieldset>
          </div>

          <footer className="entry-modal__footer">
            <button type="button" className="ghost-button" onClick={onClose} disabled={saving}>
              Cancelar
            </button>
            <button type="submit" className="primary-button" disabled={saving}>
              {saving ? "Salvando..." : "Salvar venda"}
            </button>
            <button type="button" className="gold-button" disabled={saving} onClick={(event) => onSubmit(event, true)}>
              Salvar venda e gerar PDF
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

function SegmentedControl({ value, options, onChange }) {
  return (
    <div className="segmented-control">
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={value === option.value ? "segmented-control__item segmented-control__item--active" : "segmented-control__item"}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
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

function MetricCard({ label, value, accent }) {
  return (
    <article className={`metric-card metric-card--${accent}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
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

function StatusBadge({ status }) {
  const normalized = String(status || "").toLowerCase();
  const tone =
    normalized.includes("pronto")
      ? "green"
      : normalized.includes("aprovacao") || normalized.includes("analise") || normalized.includes("manutencao")
      ? "amber"
      : normalized.includes("cancel")
      ? "red"
      : "blue";

  return (
    <span className={`status-badge status-badge--${tone}`}>
      {humanizeStatus(status)}
    </span>
  );
}

function createEmptyEntradaForm() {
  return {
    idCliente: "",
    clienteNome: "",
    clienteTelefone: "",
    clienteCpf: "",
    clienteEmail: "",
    clienteEndereco: "",
    clienteObservacoes: "",
    idEquipamento: "",
    marca: "Sony",
    tipoEquipamento: "Console",
    modelo: "",
    equipamentoDescricao: "",
    dataEntrada: todayIsoDate(),
    numeroSerie: "",
    cor: "",
    problemaRelatado: "",
    acessorios: "",
    estadoFisico: "",
    observacoes: ""
  };
}

function createEmptySaidaForm() {
  return {
    idOrdemServico: "",
    dataSaida: todayIsoDate(),
    valorServico: "",
    formaPagamento: "PIX",
    diagnostico: "",
    servicoRealizado: "",
    observacoes: ""
  };
}

function createVendaItem() {
  return {
    idProduto: "",
    quantidade: 1,
    valorUnitario: ""
  };
}

function createEmptyVendaForm() {
  return {
    idCliente: "",
    dataVenda: todayIsoDate(),
    formaPagamento: "PIX",
    observacoes: "",
    itens: [createVendaItem()]
  };
}

function createEmptyVendaProductForm() {
  return {
    nome: "",
    categoria: "Produto",
    descricao: "",
    quantidadeEstoque: 1,
    estoqueMinimo: 0,
    valorCompra: 0,
    valorVenda: 0,
    valorVendaPrazo: 0
  };
}

function buildVendaProductPayload(form) {
  return {
    nome: String(form.nome || "").trim(),
    categoria: String(form.categoria || "").trim(),
    descricao: optionalText(form.descricao),
    quantidadeEstoque: toNonNegativeInteger(form.quantidadeEstoque),
    estoqueMinimo: toNonNegativeInteger(form.estoqueMinimo),
    valorCompra: parseMoney(form.valorCompra),
    valorVenda: parseMoney(form.valorVenda),
    valorVendaVista: parseMoney(form.valorVenda),
    valorVendaPrazo: parseMoney(form.valorVendaPrazo),
    ativo: true
  };
}

function buildEntradaObservacoes(form) {
  return [
    form.observacoes ? `Observacoes: ${form.observacoes}` : "",
    form.acessorios ? `Acessorios recebidos: ${form.acessorios}` : "",
    form.estadoFisico ? `Estado fisico: ${form.estadoFisico}` : ""
  ].filter(Boolean).join("\n");
}

function parseMoney(value) {
  const normalized = String(value || "0").replace(",", ".");
  const number = Number(normalized);
  return Number.isFinite(number) ? number : 0;
}

function calculateVendaTotal(itens) {
  return itens.reduce((total, item) => {
    return total + Number(item.quantidade || 0) * parseMoney(item.valorUnitario);
  }, 0);
}

function hasNegativeProductNumbers(form) {
  return [
    form.quantidadeEstoque,
    form.estoqueMinimo,
    form.valorCompra,
    form.valorVenda,
    form.valorVendaPrazo
  ].some((value) => Number(value) < 0);
}

function productSalePrice(produto, formaPagamento) {
  if (!produto) {
    return 0;
  }

  if (formaPagamento === "CARTAO_CREDITO") {
    return productTermPrice(produto);
  }

  return productCashPrice(produto);
}

function productCashPrice(produto) {
  return parseMoney(produto?.valorVendaVista ?? produto?.valorVenda);
}

function productTermPrice(produto) {
  const termPrice = produto?.valorVendaPrazo;
  return termPrice == null ? productCashPrice(produto) : parseMoney(termPrice);
}

function toNonNegativeInteger(value) {
  const number = Number(value);
  if (!Number.isFinite(number) || number < 0) {
    return 0;
  }

  return Math.floor(number);
}

function validateFormEvent(event) {
  const formElement = event.currentTarget?.form || event.currentTarget;
  if (formElement?.reportValidity && !formElement.reportValidity()) {
    return false;
  }

  return true;
}

function openServiceOrderPdf(ordem) {
  const idOrdemServico = typeof ordem === "object" ? ordem?.idOrdemServico : ordem;
  if (!idOrdemServico) {
    return;
  }

  const pdfUrl = api.fileUrl(`/ordens-servico/${idOrdemServico}/pdf`);
  window.open(pdfUrl, "_blank", "noopener,noreferrer");
}

function openSaleReceiptPdf(venda) {
  const idVenda = typeof venda === "object" ? venda?.idVenda : venda;
  if (!idVenda) {
    return;
  }

  const pdfUrl = api.fileUrl(`/vendas/${idVenda}/pdf`);
  window.open(pdfUrl, "_blank", "noopener,noreferrer");
}

function equipamentoLabel(equipamento) {
  if (!equipamento) {
    return "Equipamento nao informado";
  }

  return [equipamento.marca, equipamento.tipoEquipamento, equipamento.modelo]
    .filter(Boolean)
    .join(" - ");
}

function humanizeStatus(status) {
  if (!status) {
    return "Sem status";
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

function formatOrderNumber(value) {
  return String(value || 0).padStart(6, "0");
}

function saleInitials(item) {
  const label = item.nome || `V${item.idVenda || ""}`;
  return label.slice(0, 2).toUpperCase();
}

function todayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

function optionalText(value) {
  const text = String(value || "").trim();
  return text.length ? text : null;
}

function readStoredUser() {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    const stored = window.localStorage.getItem(authStorageKey);
    const sessionUser = stored ? sanitizeSessionUser(JSON.parse(stored)) : null;
    if (stored && !sessionUser) {
      window.localStorage.removeItem(authStorageKey);
    }
    return sessionUser;
  } catch {
    window.localStorage.removeItem(authStorageKey);
    return null;
  }
}

function sanitizeSessionUser(usuario) {
  if (!usuario || !usuario.token) {
    return null;
  }

  return {
    idUsuario: usuario.idUsuario,
    nome: usuario.nome || "",
    login: usuario.login || "",
    perfil: usuario.perfil || "",
    ativo: usuario.ativo !== false,
    token: usuario.token || ""
  };
}

export default App;
