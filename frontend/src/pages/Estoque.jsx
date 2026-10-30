import React, { useEffect, useState } from "react";
import { api } from "../services/api";
import searchIcon from "../assets/icons/search.svg";
import EntityCell from "../components/EntityCell";
import RowActions from "../components/RowActions";
import SortableHeader from "../components/SortableHeader";
import { productCategoryIcon } from "../utils/entityIcons";
import { compareText, nextSort, sortRows } from "../utils/tableSorting";

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL"
});

const defaultCategories = ["Produto", "Acessorio", "Peca", "Insumo", "Servico"];
const MAX_PRODUCT_PHOTO_BYTES = 512000;
const allowedProductPhotoTypes = ["image/jpeg", "image/jpg", "image/png", "image/webp"];

const stockSortColumns = {
  produto: { value: (produto) => produto.nome, type: "text" },
  categoria: { value: (produto) => produto.categoria, type: "text" },
  quantidade: { value: (produto) => produto.quantidadeEstoque, type: "number" },
  minimo: { value: (produto) => produto.estoqueMinimo, type: "number" },
  compra: { value: (produto) => parseMoney(produto.valorCompra), type: "number" },
  vista: { value: (produto) => productCashPrice(produto), type: "number" },
  prazo: { value: (produto) => productTermPrice(produto), type: "number" },
  status: { value: (produto) => (produto.ativo ? 1 : 0), type: "status" }
};

function createEmptyProductForm() {
  return {
    nome: "",
    categoria: "Produto",
    descricao: "",
    quantidadeEstoque: 0,
    estoqueMinimo: 0,
    valorCompra: 0,
    valorVenda: 0,
    valorVendaVista: 0,
    valorVendaPrazo: 0,
    fotoFile: null,
    fotoPreviewUrl: "",
    removerFoto: false,
    ativo: true
  };
}

function productToForm(produto) {
  return {
    nome: produto?.nome || "",
    categoria: produto?.categoria || "Produto",
    descricao: produto?.descricao || "",
    quantidadeEstoque: produto?.quantidadeEstoque ?? 0,
    estoqueMinimo: produto?.estoqueMinimo ?? 0,
    valorCompra: produto?.valorCompra ?? 0,
    valorVenda: productCashPrice(produto),
    valorVendaVista: productCashPrice(produto),
    valorVendaPrazo: productTermPrice(produto),
    fotoFile: null,
    fotoPreviewUrl: produto?.possuiFoto ? productPhotoUrl(produto) : "",
    removerFoto: false,
    ativo: produto?.ativo !== false
  };
}

function buildProductPayload(form) {
  return {
    nome: String(form.nome || "").trim(),
    categoria: String(form.categoria || "").trim(),
    descricao: optionalText(form.descricao),
    quantidadeEstoque: toNonNegativeInteger(form.quantidadeEstoque),
    estoqueMinimo: toNonNegativeInteger(form.estoqueMinimo),
    valorCompra: parseMoney(form.valorCompra),
    valorVenda: parseMoney(form.valorVendaVista),
    valorVendaVista: parseMoney(form.valorVendaVista),
    valorVendaPrazo: parseMoney(form.valorVendaPrazo),
    ativo: form.ativo !== false
  };
}

function filterProducts(produtos, search) {
  const query = String(search || "").trim().toLowerCase();
  if (!query) {
    return produtos;
  }

  return produtos.filter((produto) => {
    const fields = [
      produto.nome,
      produto.categoria,
      produto.descricao,
      produto.quantidadeEstoque,
      produto.estoqueMinimo,
      produto.ativo ? "ativo" : "inativo"
    ];

    return fields.some((value) => String(value || "").toLowerCase().includes(query));
  });
}

export default function EstoquePage({ produtos, onProdutosChange, onFeedback }) {
  const [search, setSearch] = useState("");
  const [stockSort, setStockSort] = useState({ sortBy: "produto", sortOrder: "asc" });
  const [loading, setLoading] = useState(false);
  const [productModalOpen, setProductModalOpen] = useState(false);
  const [productDetailsOpen, setProductDetailsOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [productForm, setProductForm] = useState(createEmptyProductForm);
  const [savingProduct, setSavingProduct] = useState(false);

  const filteredProducts = sortRows(
    filterProducts(produtos, search),
    stockSort,
    stockSortColumns,
    (left, right) => compareText(left.nome, right.nome) || Number(left.idProduto || 0) - Number(right.idProduto || 0)
  );
  const activeProducts = produtos.filter((produto) => produto.ativo).length;
  const lowStockProducts = produtos.filter((produto) => isLowStock(produto)).length;
  const totalStockValue = produtos.reduce((total, produto) => {
    return total + Number(produto.quantidadeEstoque || 0) * productCashPrice(produto);
  }, 0);

  useEffect(() => {
    refreshProducts({ silent: produtos.length > 0 });
  }, []);

  function notify(feedback) {
    if (onFeedback) {
      onFeedback(feedback);
    }
  }

  function updateStockSort(sortBy) {
    setStockSort((current) => nextSort(current, sortBy));
  }

  async function refreshProducts(options = {}) {
    const { silent = false } = options;

    if (!silent) {
      setLoading(true);
    }

    try {
      const produtosData = await api.get("/produtos");
      onProdutosChange(Array.isArray(produtosData) ? produtosData : []);
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel carregar os produtos do estoque."
      });
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }

  function openProductCreateModal() {
    setSelectedProduct(null);
    setProductForm(createEmptyProductForm());
    setProductModalOpen(true);
    notify(null);
  }

  function openProductEditModal(produto) {
    setSelectedProduct(produto);
    setProductForm(productToForm(produto));
    setProductModalOpen(true);
    notify(null);
  }

  async function openProductDetails(produto) {
    setSelectedProduct(produto);
    setProductDetailsOpen(true);
    notify(null);

    try {
      const produtoAtualizado = await api.get(`/produtos/${produto.idProduto}`);
      setSelectedProduct(produtoAtualizado || produto);
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel carregar os detalhes do produto."
      });
    }
  }

  function closeProductModal() {
    if (!savingProduct) {
      setProductModalOpen(false);
    }
  }

  function updateProductField(field, value) {
    setProductForm((current) => ({
      ...current,
      [field]: value
    }));
  }

  function updateProductPhoto(file) {
    if (!file) {
      return;
    }

    if (!allowedProductPhotoTypes.includes(file.type)) {
      notify({ type: "error", message: "Formato de foto invalido. Use JPG, PNG ou WebP." });
      return;
    }

    if (file.size > MAX_PRODUCT_PHOTO_BYTES) {
      notify({ type: "error", message: "A foto deve ter no maximo 500 KB. Reduza para cerca de 600x600 px." });
      return;
    }

    setProductForm((current) => ({
      ...current,
      fotoFile: file,
      fotoPreviewUrl: URL.createObjectURL(file),
      removerFoto: false
    }));
  }

  function removeProductPhoto() {
    setProductForm((current) => ({
      ...current,
      fotoFile: null,
      fotoPreviewUrl: "",
      removerFoto: Boolean(selectedProduct?.possuiFoto)
    }));
  }

  async function handleProductSubmit(event) {
    event.preventDefault();
    const formElement = event.currentTarget?.form || event.currentTarget;
    if (formElement?.reportValidity && !formElement.reportValidity()) {
      return;
    }

    const payload = buildProductPayload(productForm);
    if (!payload.nome || !payload.categoria) {
      notify({ type: "error", message: "Informe nome e categoria para salvar o produto." });
      return;
    }

    if (hasNegativeNumbers(productForm)) {
      notify({ type: "error", message: "Quantidade, estoque minimo e valores nao podem ser negativos." });
      return;
    }

    setSavingProduct(true);
    notify(null);

    try {
      const isEditing = Boolean(selectedProduct?.idProduto);
      const produtoSalvo = isEditing
        ? await api.put(`/produtos/${selectedProduct.idProduto}`, payload)
        : await api.post("/produtos", payload);
      const idProduto = produtoSalvo?.idProduto || selectedProduct?.idProduto;

      if (idProduto && productForm.removerFoto) {
        await api.delete(`/produtos/${idProduto}/foto`);
      }

      if (idProduto && productForm.fotoFile) {
        const formData = new FormData();
        formData.append("foto", productForm.fotoFile);
        await api.upload(`/produtos/${idProduto}/foto`, formData);
      }

      notify({
        type: "success",
        message: isEditing
          ? `Produto ${produtoSalvo?.nome || payload.nome} atualizado com sucesso.`
          : `Produto ${produtoSalvo?.nome || payload.nome} cadastrado com sucesso.`
      });
      setProductModalOpen(false);
      setSelectedProduct(produtoSalvo || null);
      await refreshProducts({ silent: true });
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel salvar o produto."
      });
    } finally {
      setSavingProduct(false);
    }
  }

  async function handleProductDelete(produto) {
    const confirmed = window.confirm(`Excluir o produto "${produto.nome}"? Essa acao nao pode ser desfeita.`);
    if (!confirmed) {
      return;
    }

    setLoading(true);
    notify(null);

    try {
      await api.delete(`/produtos/${produto.idProduto}`);
      notify({ type: "success", message: `Produto ${produto.nome} excluido com sucesso.` });
      await refreshProducts({ silent: true });
    } catch (error) {
      if (error.status === 409) {
        await handleProductDeleteConflict(produto);
        return;
      }

      notify({
        type: "error",
        message: error.message || "Nao foi possivel excluir o produto."
      });
    } finally {
      setLoading(false);
    }
  }

  async function handleProductDeleteConflict(produto) {
    if (!produto.ativo) {
      notify({
        type: "error",
        message: `Produto ${produto.nome} ja esta inativo e possui historico vinculado.`
      });
      return;
    }

    const shouldInactivate = window.confirm(
      `O produto "${produto.nome}" possui vendas, servicos ou movimentacoes vinculadas e nao pode ser excluido sem apagar historico.\n\nDeseja inativar este produto?`
    );

    if (!shouldInactivate) {
      notify({ type: "error", message: "Exclusao cancelada. O produto possui historico vinculado." });
      return;
    }

    try {
      const payload = buildProductPayload({
        ...productToForm(produto),
        ativo: false
      });
      await api.put(`/produtos/${produto.idProduto}`, payload);
      notify({
        type: "success",
        message: `Produto ${produto.nome} possui historico vinculado, entao foi inativado.`
      });
      await refreshProducts({ silent: true });
    } catch (error) {
      notify({
        type: "error",
        message: error.message || "Nao foi possivel inativar o produto."
      });
    }
  }

  return (
    <section className="management-page estoque-page" aria-labelledby="estoque-title">
      <div className="management-hero">
        <div>
          <span className="section-label">Controle de estoque</span>
          <h1 id="estoque-title">Estoque</h1>
          <p>Visualize, cadastre, edite e remova produtos da Acao Games.</p>
        </div>
        <button type="button" className="primary-button management-hero__button" onClick={openProductCreateModal}>
          Novo Produto
        </button>
      </div>

      <section className="client-toolbar estoque-toolbar" aria-label="Busca e resumo de estoque">
        <label className="search-room client-search">
          <SvgIcon src={searchIcon} size={15} />
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Buscar produto, categoria ou descricao..."
          />
        </label>
        <article className="client-summary-card">
          <span>Total</span>
          <strong>{produtos.length}</strong>
        </article>
        <article className="client-summary-card client-summary-card--active">
          <span>Ativos</span>
          <strong>{activeProducts}</strong>
        </article>
        <article className="client-summary-card client-summary-card--muted">
          <span>Estoque baixo</span>
          <strong>{lowStockProducts}</strong>
        </article>
        <article className="client-summary-card client-summary-card--gold">
          <span>Valor estoque</span>
          <strong>{currencyFormatter.format(totalStockValue)}</strong>
        </article>
      </section>

      <Panel title="Produtos em estoque">
        {loading ? (
          <div className="empty-state">Carregando produtos...</div>
        ) : (
          <div className="table-wrap">
            <table className="compact-table clients-table data-table">
              <thead>
                <tr>
                  <SortableHeader label="Produto" sortKey="produto" sort={stockSort} onSort={updateStockSort} />
                  <SortableHeader label="Categoria" sortKey="categoria" sort={stockSort} onSort={updateStockSort} />
                  <SortableHeader label="Qtd" sortKey="quantidade" sort={stockSort} onSort={updateStockSort} className="numeric-column" />
                  <SortableHeader label="Mínimo" sortKey="minimo" sort={stockSort} onSort={updateStockSort} className="numeric-column" />
                  <SortableHeader label="Compra" sortKey="compra" sort={stockSort} onSort={updateStockSort} className="money-column" />
                  <SortableHeader label="À vista" sortKey="vista" sort={stockSort} onSort={updateStockSort} className="money-column" />
                  <SortableHeader label="À prazo" sortKey="prazo" sort={stockSort} onSort={updateStockSort} className="money-column" />
                  <SortableHeader label="Status" sortKey="status" sort={stockSort} onSort={updateStockSort} />
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {filteredProducts.length ? filteredProducts.map((produto) => (
                  <tr key={produto.idProduto}>
                    <td>
                      <EntityCell
                        title={produto.nome}
                        subtitle={produto.descricao || "Sem descricao"}
                        iconSrc={productCategoryIcon(produto.categoria)}
                        imageSrc={produto.possuiFoto ? productPhotoUrl(produto) : ""}
                        imageAlt={produto.possuiFoto ? `Foto de ${produto.nome}` : ""}
                        tone="product"
                      />
                    </td>
                    <td>{produto.categoria}</td>
                    <td className="numeric-column">{produto.quantidadeEstoque}</td>
                    <td className="numeric-column">{produto.estoqueMinimo}</td>
                    <td className="money-column">{currencyFormatter.format(parseMoney(produto.valorCompra))}</td>
                    <td className="money-column">{currencyFormatter.format(productCashPrice(produto))}</td>
                    <td className="money-column">{currencyFormatter.format(productTermPrice(produto))}</td>
                    <td>
                      <div className="stock-status-stack">
                        <span className={`status-badge status-badge--${produto.ativo ? "green" : "red"}`}>
                          {produto.ativo ? "Ativo" : "Inativo"}
                        </span>
                        {isLowStock(produto) ? (
                          <span className="status-badge status-badge--red">Baixo</span>
                        ) : null}
                      </div>
                    </td>
                    <td>
                      <RowActions
                        menuLabel={`Mais acoes de ${produto.nome}`}
                        primary={[
                          { key: "view", label: "Ver", onClick: () => openProductDetails(produto) },
                          { key: "edit", label: "Editar", className: "table-action--edit", onClick: () => openProductEditModal(produto) }
                        ]}
                        secondary={[
                          { key: "delete", label: "Excluir", danger: true, onClick: () => handleProductDelete(produto) }
                        ]}
                      />
                    </td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan="9">
                      {search ? "Nenhum produto encontrado para a busca informada." : "Nenhum produto cadastrado ainda."}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      {productModalOpen ? (
        <ProductModal
          form={productForm}
          selectedProduct={selectedProduct}
          saving={savingProduct}
          onClose={closeProductModal}
          onSubmit={handleProductSubmit}
          onChange={updateProductField}
          onPhotoChange={updateProductPhoto}
          onRemovePhoto={removeProductPhoto}
        />
      ) : null}

      {productDetailsOpen && selectedProduct ? (
        <ProductDetailsModal
          produto={selectedProduct}
          onClose={() => setProductDetailsOpen(false)}
          onEdit={() => {
            setProductDetailsOpen(false);
            openProductEditModal(selectedProduct);
          }}
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

function ProductModal({ form, selectedProduct, saving, onClose, onSubmit, onChange, onPhotoChange, onRemovePhoto }) {
  const isEditing = Boolean(selectedProduct?.idProduto);

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal entry-modal--client" role="dialog" aria-modal="true" aria-labelledby="product-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">{isEditing ? "Editar produto" : "Novo produto"}</span>
            <h2 id="product-title">{isEditing ? "Editar Produto" : "Novo Produto"}</h2>
            <p>Atualize os dados de estoque, custo e venda do produto.</p>
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
              <legend>Dados do produto</legend>
              <div className="form-grid-two">
                <label className="field">
                  <span>Nome</span>
                  <input required value={form.nome} onChange={(event) => onChange("nome", event.target.value)} />
                </label>
                <label className="field">
                  <span>Categoria</span>
                  <input
                    required
                    list="stock-categories"
                    value={form.categoria}
                    onChange={(event) => onChange("categoria", event.target.value)}
                  />
                  <datalist id="stock-categories">
                    {defaultCategories.map((categoria) => (
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
                    required
                    value={form.quantidadeEstoque}
                    onChange={(event) => onChange("quantidadeEstoque", event.target.value)}
                  />
                </label>
                <label className="field">
                  <span>Estoque minimo</span>
                  <input
                    type="number"
                    min="0"
                    step="1"
                    required
                    value={form.estoqueMinimo}
                    onChange={(event) => onChange("estoqueMinimo", event.target.value)}
                  />
                </label>
                <label className="field">
                  <span>Valor de compra</span>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    required
                    value={form.valorCompra}
                    onChange={(event) => onChange("valorCompra", event.target.value)}
                  />
                </label>
                <label className="field">
                  <span>Valor a vista</span>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    required
                    value={form.valorVendaVista}
                    onChange={(event) => onChange("valorVendaVista", event.target.value)}
                  />
                </label>
                <label className="field">
                  <span>Valor a prazo</span>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    required
                    value={form.valorVendaPrazo}
                    onChange={(event) => onChange("valorVendaPrazo", event.target.value)}
                  />
                </label>
                <label className="field field--wide">
                  <span>Descricao</span>
                  <textarea rows="4" value={form.descricao} onChange={(event) => onChange("descricao", event.target.value)} />
                </label>
                <div className="field field--wide product-photo-field">
                  <span>Foto do produto</span>
                  <div className="product-photo-picker">
                    <div className="product-photo-preview">
                      {form.fotoPreviewUrl ? (
                        <img src={form.fotoPreviewUrl} alt={`Foto de ${form.nome || "produto"}`} />
                      ) : (
                        <SvgIcon src={productCategoryIcon(form.categoria)} size={24} />
                      )}
                    </div>
                    <div className="product-photo-actions">
                      <label className="secondary-button product-photo-input">
                        Escolher foto
                        <input
                          type="file"
                          accept="image/jpeg,image/jpg,image/png,image/webp"
                          onChange={(event) => onPhotoChange(event.target.files?.[0])}
                        />
                      </label>
                      {form.fotoPreviewUrl ? (
                        <button type="button" className="ghost-button" onClick={onRemovePhoto}>
                          Remover foto
                        </button>
                      ) : null}
                      <small>Ideal: JPG/WebP ate 300 KB. Limite maximo: 500 KB.</small>
                    </div>
                  </div>
                </div>
                <label className="checkbox-filter client-active-toggle">
                  <input
                    type="checkbox"
                    checked={form.ativo}
                    onChange={(event) => onChange("ativo", event.target.checked)}
                  />
                  <span className="checkbox-control" />
                  <span>Produto ativo</span>
                </label>
              </div>
            </fieldset>
          </div>

          <footer className="entry-modal__footer">
            <button type="button" className="ghost-button" onClick={onClose} disabled={saving}>
              Cancelar
            </button>
            <button type="submit" className="primary-button" disabled={saving}>
              {saving ? "Salvando..." : isEditing ? "Salvar alteracoes" : "Cadastrar produto"}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

function ProductDetailsModal({ produto, onClose, onEdit }) {
  const margem = productCashPrice(produto) - parseMoney(produto.valorCompra);
  const valorTotal = Number(produto.quantidadeEstoque || 0) * productCashPrice(produto);

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="entry-modal entry-modal--client" role="dialog" aria-modal="true" aria-labelledby="product-detail-title">
        <header className="entry-modal__header">
          <div>
            <span className="eyebrow">Detalhes do produto</span>
            <h2 id="product-detail-title">{produto.nome}</h2>
            <p>{produto.categoria || "Categoria nao informada"}</p>
          </div>
          <div className="entry-modal__actions">
            <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">
              x
            </button>
          </div>
        </header>

        <div className="entry-form client-details">
          {produto.possuiFoto ? (
            <div className="product-detail-photo">
              <img src={productPhotoUrl(produto)} alt={`Foto de ${produto.nome}`} />
            </div>
          ) : null}
          <div className="client-details__grid">
            <DetailItem label="Categoria" value={produto.categoria} />
            <DetailItem label="Status" value={produto.ativo ? "Ativo" : "Inativo"} />
            <DetailItem label="Quantidade atual" value={produto.quantidadeEstoque} />
            <DetailItem label="Estoque minimo" value={produto.estoqueMinimo} />
            <DetailItem label="Valor de compra" value={currencyFormatter.format(parseMoney(produto.valorCompra))} />
            <DetailItem label="Valor a vista" value={currencyFormatter.format(productCashPrice(produto))} />
            <DetailItem label="Valor a prazo" value={currencyFormatter.format(productTermPrice(produto))} />
            <DetailItem label="Margem unitaria" value={currencyFormatter.format(margem)} />
            <DetailItem label="Valor total em estoque" value={currencyFormatter.format(valorTotal)} />
            <DetailItem label="Cadastro" value={formatDateTime(produto.dataCadastro)} />
            <DetailItem label="Descricao" value={produto.descricao} wide />
          </div>

          <footer className="entry-modal__footer">
            <button type="button" className="ghost-button" onClick={onClose}>
              Fechar
            </button>
            <button type="button" className="primary-button" onClick={onEdit}>
              Editar produto
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

function isLowStock(produto) {
  return Number(produto.quantidadeEstoque || 0) <= Number(produto.estoqueMinimo || 0);
}

function hasNegativeNumbers(form) {
  return [
    form.quantidadeEstoque,
    form.estoqueMinimo,
    form.valorCompra,
    form.valorVendaVista,
    form.valorVendaPrazo
  ].some((value) => Number(value) < 0);
}

function productCashPrice(produto) {
  return parseMoney(produto?.valorVendaVista ?? produto?.valorVenda);
}

function productTermPrice(produto) {
  const cashPrice = productCashPrice(produto);
  const termPrice = produto?.valorVendaPrazo;
  return termPrice == null ? cashPrice : parseMoney(termPrice);
}

function toNonNegativeInteger(value) {
  const number = Number(value);
  if (!Number.isFinite(number) || number < 0) {
    return 0;
  }

  return Math.floor(number);
}

function parseMoney(value) {
  const normalized = String(value || "0").replace(",", ".");
  const number = Number(normalized);
  return Number.isFinite(number) ? number : 0;
}

function productPhotoUrl(produto) {
  if (!produto?.idProduto) {
    return "";
  }

  const version = produto.fotoTamanho || Date.now();
  return api.fileUrl(`/produtos/${produto.idProduto}/foto?v=${version}`);
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

function displayValue(value) {
  const text = String(value ?? "").trim();
  return text || "Nao informado";
}

function optionalText(value) {
  const text = String(value || "").trim();
  return text.length ? text : null;
}
