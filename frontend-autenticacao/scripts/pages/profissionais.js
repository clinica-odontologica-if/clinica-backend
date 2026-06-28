import { getAuthHeaders, getErrorMessage, getToken, parseResponse } from "../api.js";
import { setPageMessage } from "../messages.js";
import { escapeHtml } from "../utils.js";

const renderProfissionais = (profissionais) => {
  const tbody = document.querySelector("#profissionaisTabela");
  if (!tbody) return;

  if (!profissionais.length) {
    tbody.innerHTML = '<tr><td colspan="4">Nenhum profissional cadastrado.</td></tr>';
    return;
  }

  tbody.innerHTML = profissionais
    .map((profissional) => {
      const detalhes = profissional.role === "DENTISTA"
        ? `${profissional.cro || "-"} / ${profissional.especialidade || "-"}`
        : "-";

      return `
        <tr>
          <td>${escapeHtml(profissional.nome)}</td>
          <td>${escapeHtml(profissional.email)}</td>
          <td>${escapeHtml(profissional.role)}</td>
          <td>${escapeHtml(detalhes)}</td>
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
  tbody.innerHTML = '<tr><td colspan="4">Carregando...</td></tr>';

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
    tbody.innerHTML = `<tr><td colspan="4">${escapeHtml(error.message)}</td></tr>`;
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

const cadastrarProfissional = async (event) => {
  event.preventDefault();

  const formProfissional = event.currentTarget;
  const button = document.querySelector("#salvarProfissionalButton");
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

  button.disabled = true;
  setPageMessage("#profissionalMessage", "Salvando...");

  try {
    const response = await fetch("/api/profissionais", {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });
    const data = await parseResponse(response);

    if (!response.ok) {
      throw new Error(getErrorMessage(data, "Nao foi possivel cadastrar o profissional."));
    }

    formProfissional.reset();
    toggleCamposDentista();
    setPageMessage("#profissionalMessage", "Profissional cadastrado com sucesso.", "success");
    await carregarProfissionais();
  } catch (error) {
    setPageMessage("#profissionalMessage", error.message, "error");
  } finally {
    button.disabled = false;
  }
};

export const initProfissionaisPage = () => {
  const formProfissional = document.querySelector("#profissionalForm");
  const role = document.querySelector("#profissionalRole");
  const reloadButton = document.querySelector("#recarregarProfissionaisButton");

  if (!formProfissional || formProfissional.dataset.ready === "true") return;

  formProfissional.dataset.ready = "true";
  role.addEventListener("change", toggleCamposDentista);
  formProfissional.addEventListener("submit", cadastrarProfissional);
  reloadButton.addEventListener("click", carregarProfissionais);

  toggleCamposDentista();
  carregarProfissionais();
};
