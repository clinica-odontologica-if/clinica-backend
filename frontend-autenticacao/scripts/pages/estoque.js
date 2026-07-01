import { getAuthHeaders, getErrorMessage, getToken, parseResponse } from "../api.js";
import { setPageMessage } from "../messages.js";
import { hasAnyRole } from "../session.js";
import { escapeHtml } from "../utils.js";

const canManageEstoque = () => hasAnyRole("GERENTE", "AUXILIAR");
const canInativarMaterial = () => hasAnyRole("GERENTE");

const fetchJson = async (url, options = {}, fallbackMessage) => {
  const response = await fetch(url, options);
  const data = await parseResponse(response);
  if (!response.ok) throw new Error(getErrorMessage(data, fallbackMessage));
  return data;
};

const formatNumero = (valor) => Number(valor ?? 0).toLocaleString("pt-BR", { minimumFractionDigits: 0, maximumFractionDigits: 2 });

const materialPayloadFromForm = (form) => {
  const formData = new FormData(form);
  const quantidadeAtual = formData.get("quantidadeAtual");
  return {
    nome: formData.get("nome"),
    descricao: formData.get("descricao") || null,
    categoria: formData.get("categoria") || null,
    unidadeMedida: formData.get("unidadeMedida"),
    quantidadeAtual: quantidadeAtual === "" ? 0 : Number(quantidadeAtual),
    quantidadeMinima: Number(formData.get("quantidadeMinima") || 0),
  };
};

const movimentacaoPayloadFromForm = (form) => {
  const formData = new FormData(form);
  return {
    tipo: formData.get("tipo"),
    quantidade: Number(formData.get("quantidade") || 0),
    motivo: formData.get("motivo") || null,
  };
};

const renderMaterialOptions = (materiais) => {
  const select = document.querySelector("#movimentacaoMaterialId");
  if (!select) return;
  select.innerHTML = '<option value="">Selecione um material</option>' + materiais
    .map((material) => `<option value="${material.id}">${escapeHtml(material.nome)} (${formatNumero(material.quantidadeAtual)} ${escapeHtml(material.unidadeMedida)})</option>`)
    .join("");
};

const renderMateriais = (materiais) => {
  const tbody = document.querySelector("#materiaisTabela");
  if (!tbody) return;

  if (!materiais.length) {
    tbody.innerHTML = '<tr><td colspan="6">Nenhum material cadastrado.</td></tr>';
    renderMaterialOptions([]);
    return;
  }

  tbody.innerHTML = materiais.map((material) => {
    const editarButton = canManageEstoque()
      ? `<button type="button" class="table-action" data-action="editar" data-id="${material.id}">Editar</button>`
      : "";
    const inativarButton = canInativarMaterial()
      ? `<button type="button" class="table-action danger" data-action="inativar" data-id="${material.id}">Inativar</button>`
      : "";
    const status = material.baixoEstoque ? "Baixo estoque" : "Ok";
    const statusClass = material.baixoEstoque ? "status-cancelado" : "status-realizado";

    return `
      <tr>
        <td>${escapeHtml(material.nome)}</td>
        <td>${escapeHtml(material.categoria || "-")}</td>
        <td>${formatNumero(material.quantidadeAtual)} ${escapeHtml(material.unidadeMedida)}</td>
        <td>${formatNumero(material.quantidadeMinima)}</td>
        <td><span class="status-badge ${statusClass}">${status}</span></td>
        <td><div class="table-actions">${editarButton}${inativarButton}</div></td>
      </tr>
    `;
  }).join("");

  renderMaterialOptions(materiais);
};

const carregarMateriais = async () => {
  const status = document.querySelector("#estoqueStatus");
  const tbody = document.querySelector("#materiaisTabela");
  if (!status || !tbody) return [];

  const params = new URLSearchParams();
  const busca = document.querySelector("#filtroMaterialBusca")?.value;
  const categoria = document.querySelector("#filtroMaterialCategoria")?.value;
  const baixoEstoque = document.querySelector("#filtroMaterialBaixoEstoque")?.value;
  if (busca) params.set("busca", busca);
  if (categoria) params.set("categoria", categoria);
  if (baixoEstoque) params.set("baixoEstoque", baixoEstoque);

  status.textContent = "carregando";
  status.className = "status-pill";
  tbody.innerHTML = '<tr><td colspan="6">Carregando...</td></tr>';

  try {
    const materiais = await fetchJson(
      `/api/materiais${params.toString() ? `?${params}` : ""}`,
      { headers: { Authorization: `Bearer ${getToken()}` } },
      "Nao foi possivel carregar materiais."
    );
    const lista = Array.isArray(materiais) ? materiais : [];
    renderMateriais(lista);
    status.textContent = "online";
    status.className = "status-pill ok";
    return lista;
  } catch (error) {
    tbody.innerHTML = `<tr><td colspan="6">${escapeHtml(error.message)}</td></tr>`;
    status.textContent = "erro";
    status.className = "status-pill offline";
    return [];
  }
};

const limparMaterialForm = () => {
  const form = document.querySelector("#materialForm");
  if (!form) return;
  form.reset();
  document.querySelector("#materialId").value = "";
  document.querySelector("#materialFormTitulo").textContent = "Cadastrar material";
  document.querySelector("#salvarMaterialButton").textContent = "Salvar";
  document.querySelector("#cancelarEdicaoMaterialButton").hidden = true;
};

const preencherMaterialForm = (material) => {
  document.querySelector("#materialId").value = material.id || "";
  document.querySelector("#materialNome").value = material.nome || "";
  document.querySelector("#materialDescricao").value = material.descricao || "";
  document.querySelector("#materialCategoria").value = material.categoria || "";
  document.querySelector("#materialUnidadeMedida").value = material.unidadeMedida || "UNIDADE";
  document.querySelector("#materialQuantidadeAtual").value = material.quantidadeAtual ?? 0;
  document.querySelector("#materialQuantidadeMinima").value = material.quantidadeMinima ?? 0;
  document.querySelector("#materialFormTitulo").textContent = "Editar material";
  document.querySelector("#salvarMaterialButton").textContent = "Atualizar";
  document.querySelector("#cancelarEdicaoMaterialButton").hidden = false;
};

const salvarMaterial = async (event) => {
  event.preventDefault();
  if (!canManageEstoque()) {
    setPageMessage("#materialMessage", "Seu perfil nao permite alterar estoque.", "error");
    return;
  }

  const form = event.currentTarget;
  const button = document.querySelector("#salvarMaterialButton");
  const id = document.querySelector("#materialId").value;
  const isEdicao = Boolean(id);

  button.disabled = true;
  setPageMessage("#materialMessage", isEdicao ? "Atualizando..." : "Salvando...");

  try {
    await fetchJson(
      isEdicao ? `/api/materiais/${id}` : "/api/materiais",
      { method: isEdicao ? "PUT" : "POST", headers: getAuthHeaders(), body: JSON.stringify(materialPayloadFromForm(form)) },
      "Nao foi possivel salvar o material."
    );
    limparMaterialForm();
    setPageMessage("#materialMessage", isEdicao ? "Material atualizado." : "Material cadastrado.", "success");
    await carregarMateriais();
  } catch (error) {
    setPageMessage("#materialMessage", error.message, "error");
  } finally {
    button.disabled = false;
  }
};

const buscarMaterial = (id) => fetchJson(`/api/materiais/${id}`, { headers: { Authorization: `Bearer ${getToken()}` } }, "Nao foi possivel carregar material.");

const inativarMaterial = async (id) => {
  if (!window.confirm("Inativar este material?")) return;
  await fetchJson(`/api/materiais/${id}/inativar`, { method: "PATCH", headers: { Authorization: `Bearer ${getToken()}` } }, "Nao foi possivel inativar material.");
  setPageMessage("#materialMessage", "Material inativado.", "success");
  await carregarMateriais();
};

const handleMaterialAction = async (event) => {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  const { action, id } = button.dataset;

  try {
    if (action === "editar") {
      preencherMaterialForm(await buscarMaterial(id));
      setPageMessage("#materialMessage", "Editando material selecionado.");
    }
    if (action === "inativar") await inativarMaterial(id);
  } catch (error) {
    setPageMessage("#materialMessage", error.message, "error");
  }
};

const registrarMovimentacao = async (event) => {
  event.preventDefault();
  if (!canManageEstoque()) {
    setPageMessage("#movimentacaoMessage", "Seu perfil nao permite movimentar estoque.", "error");
    return;
  }

  const form = event.currentTarget;
  const materialId = new FormData(form).get("materialId");
  const button = document.querySelector("#registrarMovimentacaoButton");

  button.disabled = true;
  setPageMessage("#movimentacaoMessage", "Registrando...");

  try {
    await fetchJson(
      `/api/materiais/${materialId}/movimentacoes`,
      { method: "POST", headers: getAuthHeaders(), body: JSON.stringify(movimentacaoPayloadFromForm(form)) },
      "Nao foi possivel registrar movimentacao."
    );
    form.reset();
    setPageMessage("#movimentacaoMessage", "Movimentacao registrada.", "success");
    await carregarMateriais();
  } catch (error) {
    setPageMessage("#movimentacaoMessage", error.message, "error");
  } finally {
    button.disabled = false;
  }
};

export const initEstoquePage = () => {
  const materialForm = document.querySelector("#materialForm");
  const movimentacaoForm = document.querySelector("#movimentacaoForm");
  const tabela = document.querySelector("#materiaisTabela");
  if (!materialForm || materialForm.dataset.ready === "true") return;

  if (!canManageEstoque()) {
    document.querySelectorAll(".estoque-grid .panel").forEach((panel) => { panel.hidden = true; });
  }

  materialForm.dataset.ready = "true";
  materialForm.addEventListener("submit", salvarMaterial);
  movimentacaoForm.addEventListener("submit", registrarMovimentacao);
  tabela.addEventListener("click", handleMaterialAction);
  document.querySelector("#recarregarMateriaisButton").addEventListener("click", carregarMateriais);
  document.querySelector("#cancelarEdicaoMaterialButton").addEventListener("click", () => {
    limparMaterialForm();
    setPageMessage("#materialMessage", "");
  });
  document.querySelector("#materialFiltroForm").addEventListener("submit", (event) => {
    event.preventDefault();
    carregarMateriais();
  });

  carregarMateriais();
};