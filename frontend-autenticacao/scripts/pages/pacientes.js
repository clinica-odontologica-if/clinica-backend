import { getAuthHeaders, getErrorMessage, getToken, parseResponse } from "../api.js";
import { setPageMessage } from "../messages.js";
import { hasAnyRole } from "../session.js";
import { escapeHtml, formatCpf, formatData, formatTelefone } from "../utils.js";

const canManagePacientes = () => hasAnyRole("GERENTE", "ATENDENTE");
const canInativarPacientes = () => hasAnyRole("GERENTE");

const renderPacientes = (pacientes) => {
  const tbody = document.querySelector("#pacientesTabela");
  if (!tbody) return;

  if (!pacientes.length) {
    tbody.innerHTML = '<tr><td colspan="5">Nenhum paciente cadastrado.</td></tr>';
    return;
  }

  tbody.innerHTML = pacientes
    .map((paciente) => {
      const editarButton = canManagePacientes()
        ? `<button type="button" class="table-action" data-action="editar" data-id="${paciente.id}">Editar</button>`
        : "";
      const inativarButton = canInativarPacientes()
        ? `<button type="button" class="table-action danger" data-action="inativar" data-id="${paciente.id}">Inativar</button>`
        : "";

      return `
        <tr>
          <td>${escapeHtml(paciente.nome)}</td>
          <td>${escapeHtml(formatCpf(paciente.cpf))}</td>
          <td>${escapeHtml(formatTelefone(paciente.telefone))}</td>
          <td>${escapeHtml(paciente.email || "-")}</td>
          <td>
            <div class="table-actions">
              <button type="button" class="table-action" data-action="visualizar" data-id="${paciente.id}">Ver</button>
              ${editarButton}
              ${inativarButton}
            </div>
          </td>
        </tr>
      `;
    })
    .join("");
};

const carregarPacientes = async () => {
  const status = document.querySelector("#pacientesStatus");
  const tbody = document.querySelector("#pacientesTabela");
  if (!status || !tbody) return;

  status.textContent = "carregando";
  status.className = "status-pill";
  tbody.innerHTML = '<tr><td colspan="5">Carregando...</td></tr>';

  try {
    const response = await fetch("/api/pacientes", {
      headers: { Authorization: `Bearer ${getToken()}` },
    });
    const data = await parseResponse(response);

    if (!response.ok) {
      throw new Error(getErrorMessage(data, "Nao foi possivel carregar pacientes."));
    }

    renderPacientes(Array.isArray(data) ? data : []);
    status.textContent = "online";
    status.className = "status-pill ok";
  } catch (error) {
    tbody.innerHTML = `<tr><td colspan="5">${escapeHtml(error.message)}</td></tr>`;
    status.textContent = "erro";
    status.className = "status-pill offline";
  }
};

const preencherPacienteForm = (paciente) => {
  document.querySelector("#pacienteId").value = paciente.id || "";
  document.querySelector("#pacienteNome").value = paciente.nome || "";
  document.querySelector("#pacienteCpf").value = formatCpf(paciente.cpf);
  document.querySelector("#pacienteDataNascimento").value = paciente.dataNascimento || "";
  document.querySelector("#pacienteTelefone").value = formatTelefone(paciente.telefone);
  document.querySelector("#pacienteEmail").value = paciente.email || "";
  document.querySelector("#pacienteEndereco").value = paciente.endereco || "";
  document.querySelector("#pacienteObservacoes").value = paciente.observacoes || "";
  document.querySelector("#pacienteFormTitulo").textContent = "Editar paciente";
  document.querySelector("#salvarPacienteButton").textContent = "Atualizar";
  document.querySelector("#cancelarEdicaoPacienteButton").hidden = false;
};

const limparPacienteForm = () => {
  const formPaciente = document.querySelector("#pacienteForm");
  if (!formPaciente) return;

  formPaciente.reset();
  document.querySelector("#pacienteId").value = "";
  document.querySelector("#pacienteFormTitulo").textContent = "Cadastrar paciente";
  document.querySelector("#salvarPacienteButton").textContent = "Salvar";
  document.querySelector("#cancelarEdicaoPacienteButton").hidden = true;
};

const pacientePayloadFromForm = (formPaciente) => {
  const formData = new FormData(formPaciente);
  return {
    nome: formData.get("nome"),
    cpf: formData.get("cpf"),
    dataNascimento: formData.get("dataNascimento"),
    telefone: formData.get("telefone"),
    email: formData.get("email") || null,
    endereco: formData.get("endereco") || null,
    observacoes: formData.get("observacoes") || null,
  };
};

const salvarPaciente = async (event) => {
  event.preventDefault();

  if (!canManagePacientes()) {
    setPageMessage("#pacienteMessage", "Seu perfil nao permite alterar pacientes.", "error");
    return;
  }

  const formPaciente = event.currentTarget;
  const button = document.querySelector("#salvarPacienteButton");
  const id = document.querySelector("#pacienteId").value;
  const isEdicao = Boolean(id);
  const payload = pacientePayloadFromForm(formPaciente);

  button.disabled = true;
  setPageMessage("#pacienteMessage", isEdicao ? "Atualizando..." : "Salvando...");

  try {
    const response = await fetch(isEdicao ? `/api/pacientes/${id}` : "/api/pacientes", {
      method: isEdicao ? "PUT" : "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });
    const data = await parseResponse(response);

    if (!response.ok) {
      throw new Error(getErrorMessage(data, "Nao foi possivel salvar o paciente."));
    }

    limparPacienteForm();
    setPageMessage("#pacienteMessage", isEdicao ? "Paciente atualizado com sucesso." : "Paciente cadastrado com sucesso.", "success");
    await carregarPacientes();
  } catch (error) {
    setPageMessage("#pacienteMessage", error.message, "error");
  } finally {
    button.disabled = false;
  }
};

const buscarPaciente = async (id) => {
  const response = await fetch(`/api/pacientes/${id}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
  const data = await parseResponse(response);

  if (!response.ok) {
    throw new Error(getErrorMessage(data, "Nao foi possivel carregar o paciente."));
  }

  return data;
};

const visualizarPaciente = (paciente) => {
  window.alert([
    `Nome: ${paciente.nome || "-"}`,
    `CPF: ${formatCpf(paciente.cpf)}`,
    `Nascimento: ${formatData(paciente.dataNascimento)}`,
    `Telefone: ${formatTelefone(paciente.telefone)}`,
    `Email: ${paciente.email || "-"}`,
    `Endereco: ${paciente.endereco || "-"}`,
    `Observacoes: ${paciente.observacoes || "-"}`,
  ].join("\n"));
};

const inativarPaciente = async (id) => {
  if (!canInativarPacientes()) {
    setPageMessage("#pacienteMessage", "Seu perfil nao permite inativar pacientes.", "error");
    return;
  }

  const confirmar = window.confirm("Inativar este paciente?");
  if (!confirmar) return;

  setPageMessage("#pacienteMessage", "Inativando...");

  try {
    const response = await fetch(`/api/pacientes/${id}/inativar`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${getToken()}` },
    });
    const data = await parseResponse(response);

    if (!response.ok) {
      throw new Error(getErrorMessage(data, "Nao foi possivel inativar o paciente."));
    }

    limparPacienteForm();
    setPageMessage("#pacienteMessage", "Paciente inativado com sucesso.", "success");
    await carregarPacientes();
  } catch (error) {
    setPageMessage("#pacienteMessage", error.message, "error");
  }
};

const handlePacienteAction = async (event) => {
  const button = event.target.closest("button[data-action]");
  if (!button) return;

  const { action, id } = button.dataset;

  try {
    if (action === "visualizar") {
      visualizarPaciente(await buscarPaciente(id));
      return;
    }

    if (action === "editar") {
      preencherPacienteForm(await buscarPaciente(id));
      setPageMessage("#pacienteMessage", "Editando paciente selecionado.");
      return;
    }

    if (action === "inativar") {
      await inativarPaciente(id);
    }
  } catch (error) {
    setPageMessage("#pacienteMessage", error.message, "error");
  }
};

export const initPacientesPage = () => {
  const formPaciente = document.querySelector("#pacienteForm");
  const reloadButton = document.querySelector("#recarregarPacientesButton");
  const cancelButton = document.querySelector("#cancelarEdicaoPacienteButton");
  const tbody = document.querySelector("#pacientesTabela");
  const workspace = formPaciente?.closest(".workspace-grid");
  const formPanel = formPaciente?.closest(".panel");

  if (!formPaciente || formPaciente.dataset.ready === "true") return;

  if (!canManagePacientes()) {
    formPanel.hidden = true;
    workspace.classList.add("single-panel");
  }

  formPaciente.dataset.ready = "true";
  formPaciente.addEventListener("submit", salvarPaciente);
  reloadButton.addEventListener("click", carregarPacientes);
  cancelButton.addEventListener("click", () => {
    limparPacienteForm();
    setPageMessage("#pacienteMessage", "");
  });
  tbody.addEventListener("click", handlePacienteAction);

  carregarPacientes();
};
