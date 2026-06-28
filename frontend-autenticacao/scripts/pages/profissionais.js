import { getAuthHeaders, getErrorMessage, getToken, parseResponse } from "../api.js";
import { setPageMessage } from "../messages.js";
import { hasAnyRole } from "../session.js";
import { escapeHtml } from "../utils.js";

const canManageProfissionais = () => hasAnyRole("GERENTE");

const renderProfissionais = (profissionais) => {
  const tbody = document.querySelector("#profissionaisTabela");
  if (!tbody) return;

  if (!profissionais.length) {
    tbody.innerHTML = '<tr><td colspan="5">Nenhum profissional cadastrado.</td></tr>';
    return;
  }

  tbody.innerHTML = profissionais
    .map((profissional) => {
      const detalhes = profissional.role === "DENTISTA"
        ? `${profissional.cro || "-"} / ${profissional.especialidade || "-"}`
        : "-";
      const actions = canManageProfissionais()
        ? `
          <div class="table-actions">
            <button type="button" class="table-action" data-action="editar" data-id="${profissional.id}">Editar</button>
            <button type="button" class="table-action danger" data-action="inativar" data-id="${profissional.id}">Inativar</button>
          </div>
        `
        : "-";

      return `
        <tr>
          <td>${escapeHtml(profissional.nome)}</td>
          <td>${escapeHtml(profissional.email)}</td>
          <td>${escapeHtml(profissional.role)}</td>
          <td>${escapeHtml(detalhes)}</td>
          <td>${actions}</td>
        </tr>
      `;
    })
    .join("");
};

const carregarProfissionais = async () => {
  const status = document.querySelector("#profissionaisStatus");
  const tbody = document.querySelector("#profissionaisTabela");
  if (!status || !tbody) return;

  status.textContent = "carregando";
  status.className = "status-pill";
  tbody.innerHTML = '<tr><td colspan="5">Carregando...</td></tr>';

  try {
    const response = await fetch("/api/profissionais", {
      headers: { Authorization: `Bearer ${getToken()}` },
    });
    const data = await parseResponse(response);

    if (!response.ok) {
      throw new Error(getErrorMessage(data, "Nao foi possivel carregar profissionais."));
    }

    renderProfissionais(Array.isArray(data) ? data : []);
    status.textContent = "online";
    status.className = "status-pill ok";
  } catch (error) {
    tbody.innerHTML = `<tr><td colspan="5">${escapeHtml(error.message)}</td></tr>`;
    status.textContent = "erro";
    status.className = "status-pill offline";
  }
};

const toggleCamposDentista = () => {
  const role = document.querySelector("#profissionalRole");
  const campos = document.querySelector("#camposDentista");
  const cro = document.querySelector("#profissionalCro");
  const especialidade = document.querySelector("#profissionalEspecialidade");
  if (!role || !campos || !cro || !especialidade) return;

  const isDentista = role.value === "DENTISTA";
  campos.hidden = !isDentista;
  cro.required = isDentista;
  especialidade.required = isDentista;
};

const preencherProfissionalForm = (profissional) => {
  document.querySelector("#profissionalId").value = profissional.id || "";
  document.querySelector("#profissionalNome").value = profissional.nome || "";
  document.querySelector("#profissionalEmail").value = profissional.email || "";
  document.querySelector("#profissionalRole").value = profissional.role || "ATENDENTE";
  document.querySelector("#profissionalCro").value = profissional.cro || "";
  document.querySelector("#profissionalEspecialidade").value = profissional.especialidade || "";
  document.querySelector("#profissionalFormTitulo").textContent = "Editar profissional";
  document.querySelector("#salvarProfissionalButton").textContent = "Atualizar";
  document.querySelector("#cancelarEdicaoProfissionalButton").hidden = false;
  toggleCamposDentista();
};

const limparProfissionalForm = () => {
  const formProfissional = document.querySelector("#profissionalForm");
  if (!formProfissional) return;

  formProfissional.reset();
  document.querySelector("#profissionalId").value = "";
  document.querySelector("#profissionalFormTitulo").textContent = "Cadastrar profissional";
  document.querySelector("#salvarProfissionalButton").textContent = "Salvar";
  document.querySelector("#cancelarEdicaoProfissionalButton").hidden = true;
  toggleCamposDentista();
};

const profissionalPayloadFromForm = (formProfissional) => {
  const formData = new FormData(formProfissional);
  const payload = {
    nome: formData.get("nome"),
    email: formData.get("email"),
    role: formData.get("role"),
  };

  if (payload.role === "DENTISTA") {
    payload.cro = formData.get("cro");
    payload.especialidade = formData.get("especialidade");
  }

  return payload;
};

const salvarProfissional = async (event) => {
  event.preventDefault();

  if (!canManageProfissionais()) {
    setPageMessage("#profissionalMessage", "Seu perfil nao permite alterar profissionais.", "error");
    return;
  }

  const formProfissional = event.currentTarget;
  const button = document.querySelector("#salvarProfissionalButton");
  const id = document.querySelector("#profissionalId").value;
  const isEdicao = Boolean(id);
  const payload = profissionalPayloadFromForm(formProfissional);

  button.disabled = true;
  setPageMessage("#profissionalMessage", isEdicao ? "Atualizando..." : "Salvando...");

  try {
    const response = await fetch(isEdicao ? `/api/profissionais/${id}` : "/api/profissionais", {
      method: isEdicao ? "PUT" : "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });
    const data = await parseResponse(response);

    if (!response.ok) {
      throw new Error(getErrorMessage(data, "Nao foi possivel cadastrar o profissional."));
    }

    limparProfissionalForm();
    setPageMessage("#profissionalMessage", isEdicao ? "Profissional atualizado com sucesso." : "Profissional cadastrado com sucesso.", "success");
    await carregarProfissionais();
  } catch (error) {
    setPageMessage("#profissionalMessage", error.message, "error");
  } finally {
    button.disabled = false;
  }
};

const buscarProfissional = async (id) => {
  const response = await fetch(`/api/profissionais/${id}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
  const data = await parseResponse(response);

  if (!response.ok) {
    throw new Error(getErrorMessage(data, "Nao foi possivel carregar o profissional."));
  }

  return data;
};

const inativarProfissional = async (id) => {
  if (!canManageProfissionais()) {
    setPageMessage("#profissionalMessage", "Seu perfil nao permite inativar profissionais.", "error");
    return;
  }

  const confirmar = window.confirm("Inativar este profissional?");
  if (!confirmar) return;

  setPageMessage("#profissionalMessage", "Inativando...");

  try {
    const response = await fetch(`/api/profissionais/${id}/inativar`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${getToken()}` },
    });
    const data = await parseResponse(response);

    if (!response.ok) {
      throw new Error(getErrorMessage(data, "Nao foi possivel inativar o profissional."));
    }

    limparProfissionalForm();
    setPageMessage("#profissionalMessage", "Profissional inativado com sucesso.", "success");
    await carregarProfissionais();
  } catch (error) {
    setPageMessage("#profissionalMessage", error.message, "error");
  }
};

const handleProfissionalAction = async (event) => {
  const button = event.target.closest("button[data-action]");
  if (!button) return;

  const { action, id } = button.dataset;

  try {
    if (action === "editar") {
      preencherProfissionalForm(await buscarProfissional(id));
      setPageMessage("#profissionalMessage", "Editando profissional selecionado.");
      return;
    }

    if (action === "inativar") {
      await inativarProfissional(id);
    }
  } catch (error) {
    setPageMessage("#profissionalMessage", error.message, "error");
  }
};

export const initProfissionaisPage = () => {
  const formProfissional = document.querySelector("#profissionalForm");
  const role = document.querySelector("#profissionalRole");
  const reloadButton = document.querySelector("#recarregarProfissionaisButton");
  const cancelButton = document.querySelector("#cancelarEdicaoProfissionalButton");
  const tbody = document.querySelector("#profissionaisTabela");
  const workspace = formProfissional?.closest(".workspace-grid");
  const formPanel = formProfissional?.closest(".panel");

  if (!formProfissional || formProfissional.dataset.ready === "true") return;

  if (!canManageProfissionais()) {
    formPanel.hidden = true;
    workspace.classList.add("single-panel");
  }

  formProfissional.dataset.ready = "true";
  role.addEventListener("change", toggleCamposDentista);
  formProfissional.addEventListener("submit", salvarProfissional);
  reloadButton.addEventListener("click", carregarProfissionais);
  cancelButton.addEventListener("click", () => {
    limparProfissionalForm();
    setPageMessage("#profissionalMessage", "");
  });
  tbody.addEventListener("click", handleProfissionalAction);

  toggleCamposDentista();
  carregarProfissionais();
};
