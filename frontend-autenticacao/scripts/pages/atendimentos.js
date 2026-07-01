import { getAuthHeaders, getErrorMessage, getToken, parseResponse } from "../api.js";
import { setPageMessage } from "../messages.js";
import { hasAnyRole } from "../session.js";
import { escapeHtml, formatData } from "../utils.js";

const canAgendarAtendimento = () => hasAnyRole("GERENTE", "ATENDENTE");
const canGerenciarAtendimento = () => hasAnyRole("GERENTE", "ATENDENTE", "DENTISTA");

const statusLabels = {
  AGENDADO: "Agendado",
  CONFIRMADO: "Confirmado",
  REALIZADO: "Realizado",
  CANCELADO: "Cancelado",
  NAO_COMPARECEU: "Nao compareceu",
};

const fetchJson = async (url, options = {}, fallbackMessage) => {
  const response = await fetch(url, options);
  const data = await parseResponse(response);

  if (!response.ok) {
    throw new Error(getErrorMessage(data, fallbackMessage));
  }

  return data;
};

const preencherSelect = (select, items, placeholder, labelFn) => {
  if (!select) return;

  const options = [
    `<option value="">${escapeHtml(placeholder)}</option>`,
    ...items.map((item) => `<option value="${item.id}">${escapeHtml(labelFn(item))}</option>`),
  ];

  select.innerHTML = options.join("");
};

const carregarPacientes = async () => {
  const busca = document.querySelector("#atendimentoPacienteBusca")?.value?.trim() || "";
  const select = document.querySelector("#atendimentoPacienteId");

  preencherSelect(select, [], "Carregando pacientes...", () => "");

  const params = new URLSearchParams();
  if (busca) params.set("busca", busca);

  const pacientes = await fetchJson(
    `/api/pacientes${params.toString() ? `?${params}` : ""}`,
    { headers: { Authorization: `Bearer ${getToken()}` } },
    "Nao foi possivel buscar pacientes."
  );

  preencherSelect(
    select,
    Array.isArray(pacientes) ? pacientes : [],
    "Selecione um paciente",
    (paciente) => `${paciente.nome || "Paciente"} - ${paciente.cpf || "sem CPF"}`
  );
};

const carregarProfissionais = async () => {
  const busca = document.querySelector("#atendimentoProfissionalBusca")?.value?.trim() || "";
  const select = document.querySelector("#atendimentoProfissionalId");

  preencherSelect(select, [], "Carregando dentistas...", () => "");

  const params = new URLSearchParams();
  params.set("role", "DENTISTA");
  if (busca) params.set("busca", busca);

  const profissionais = await fetchJson(
    `/api/profissionais?${params}`,
    { headers: { Authorization: `Bearer ${getToken()}` } },
    "Nao foi possivel buscar dentistas."
  );

  preencherSelect(
    select,
    Array.isArray(profissionais) ? profissionais : [],
    "Selecione um dentista",
    (profissional) => {
      const detalhe = profissional.especialidade || profissional.cro || "dentista";
      return `${profissional.nome || "Profissional"} - ${detalhe}`;
    }
  );
};

const atendimentoPayloadFromForm = (form) => {
  const formData = new FormData(form);

  return {
    pacienteId: Number(formData.get("pacienteId")),
    profissionalId: Number(formData.get("profissionalId")),
    data: formData.get("data"),
    hora: formData.get("hora"),
    observacoes: formData.get("observacoes") || null,
  };
};

const realizacaoPayloadFromForm = (form) => {
  const formData = new FormData(form);
  const valor = formData.get("valor");

  return {
    procedimentoRealizado: formData.get("procedimentoRealizado") || null,
    observacoes: formData.get("observacoes") || null,
    valor: valor === "" || valor === null ? null : Number(valor),
  };
};

const preencherDataPadrao = () => {
  const hoje = new Date().toISOString().slice(0, 10);
  const dataInput = document.querySelector("#atendimentoData");
  const filtroData = document.querySelector("#filtroAtendimentoData");

  if (dataInput && !dataInput.value) dataInput.value = hoje;
  if (filtroData && !filtroData.value) filtroData.value = hoje;
};

const limparAtendimentoForm = () => {
  const form = document.querySelector("#atendimentoForm");
  if (!form) return;

  form.reset();
  document.querySelector("#atendimentoPacienteId").innerHTML = '<option value="">Busque e selecione um paciente</option>';
  document.querySelector("#atendimentoProfissionalId").innerHTML = '<option value="">Busque e selecione um dentista</option>';
  preencherDataPadrao();
};

const formatHora = (hora) => {
  if (!hora) return "-";
  return String(hora).slice(0, 5);
};

const isFinalizado = (status) => ["REALIZADO", "CANCELADO", "NAO_COMPARECEU"].includes(status);

const renderAcoesAtendimento = (atendimento) => {
  if (!canGerenciarAtendimento()) return "-";

  const status = atendimento.status || "AGENDADO";
  const id = atendimento.id;
  const botoes = [];

  if (status === "AGENDADO") {
    botoes.push(`<button type="button" class="table-action" data-action="confirmar" data-id="${id}">Confirmar</button>`);
  }

  if (!isFinalizado(status)) {
    botoes.push(`<button type="button" class="table-action" data-action="realizar" data-id="${id}">Realizar</button>`);
    botoes.push(`<button type="button" class="table-action danger" data-action="cancelar" data-id="${id}">Cancelar</button>`);
  }

  return botoes.length ? `<div class="table-actions">${botoes.join("")}</div>` : "-";
};

const renderAtendimentos = (atendimentos) => {
  const tbody = document.querySelector("#atendimentosTabela");
  if (!tbody) return;

  if (!atendimentos.length) {
    tbody.innerHTML = '<tr><td colspan="7">Nenhum atendimento encontrado.</td></tr>';
    return;
  }

  tbody.innerHTML = atendimentos
    .map((atendimento) => {
      const status = atendimento.status || "AGENDADO";
      const statusLabel = statusLabels[status] || status;

      return `
        <tr>
          <td>${escapeHtml(formatData(atendimento.data))}</td>
          <td>${escapeHtml(formatHora(atendimento.hora))}</td>
          <td>${escapeHtml(atendimento.pacienteNome || `Paciente #${atendimento.pacienteId || "-"}`)}</td>
          <td>${escapeHtml(atendimento.profissionalNome || `Profissional #${atendimento.profissionalId || "-"}`)}</td>
          <td><span class="status-badge status-${escapeHtml(status.toLowerCase())}">${escapeHtml(statusLabel)}</span></td>
          <td>${escapeHtml(atendimento.observacoes || "-")}</td>
          <td>${renderAcoesAtendimento(atendimento)}</td>
        </tr>
      `;
    })
    .join("");
};

const carregarAtendimentos = async () => {
  const status = document.querySelector("#atendimentosStatus");
  const tbody = document.querySelector("#atendimentosTabela");
  if (!status || !tbody) return;

  status.textContent = "carregando";
  status.className = "status-pill";
  tbody.innerHTML = '<tr><td colspan="7">Carregando...</td></tr>';

  const params = new URLSearchParams();
  const data = document.querySelector("#filtroAtendimentoData")?.value;
  const statusFiltro = document.querySelector("#filtroAtendimentoStatus")?.value;
  if (data) params.set("data", data);
  if (statusFiltro) params.set("status", statusFiltro);

  try {
    const atendimentos = await fetchJson(
      `/api/atendimentos${params.toString() ? `?${params}` : ""}`,
      { headers: { Authorization: `Bearer ${getToken()}` } },
      "Nao foi possivel carregar atendimentos."
    );

    renderAtendimentos(Array.isArray(atendimentos) ? atendimentos : []);
    status.textContent = "online";
    status.className = "status-pill ok";
  } catch (error) {
    tbody.innerHTML = `<tr><td colspan="7">${escapeHtml(error.message)}</td></tr>`;
    status.textContent = "erro";
    status.className = "status-pill offline";
  }
};

const salvarAtendimento = async (event) => {
  event.preventDefault();

  if (!canAgendarAtendimento()) {
    setPageMessage("#atendimentoMessage", "Seu perfil nao permite criar atendimentos.", "error");
    return;
  }

  const button = document.querySelector("#salvarAtendimentoButton");
  const payload = atendimentoPayloadFromForm(event.currentTarget);

  button.disabled = true;
  setPageMessage("#atendimentoMessage", "Agendando...");

  try {
    await fetchJson(
      "/api/atendimentos",
      {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(payload),
      },
      "Nao foi possivel agendar o atendimento."
    );

    limparAtendimentoForm();
    setPageMessage("#atendimentoMessage", "Atendimento agendado com sucesso.", "success");
    await carregarAtendimentos();
  } catch (error) {
    setPageMessage("#atendimentoMessage", error.message, "error");
  } finally {
    button.disabled = false;
  }
};

const atualizarStatusAtendimento = async (id, status) => {
  await fetchJson(
    `/api/atendimentos/${id}/status`,
    {
      method: "PATCH",
      headers: getAuthHeaders(),
      body: JSON.stringify({ status }),
    },
    "Nao foi possivel atualizar o status do atendimento."
  );
};

const cancelarAtendimento = async (id) => {
  await fetchJson(
    `/api/atendimentos/${id}/cancelar`,
    {
      method: "PATCH",
      headers: { Authorization: `Bearer ${getToken()}` },
    },
    "Nao foi possivel cancelar o atendimento."
  );
};

const abrirRealizacaoPanel = async (id) => {
  const panel = document.querySelector("#realizacaoAtendimentoPanel");
  const form = document.querySelector("#realizacaoAtendimentoForm");
  if (!panel || !form) return;

  setPageMessage("#realizacaoAtendimentoMessage", "Carregando atendimento...");

  try {
    const atendimento = await fetchJson(
      `/api/atendimentos/${id}`,
      { headers: { Authorization: `Bearer ${getToken()}` } },
      "Nao foi possivel carregar o atendimento."
    );

    document.querySelector("#realizacaoAtendimentoId").value = atendimento.id;
    document.querySelector("#realizacaoProcedimento").value = atendimento.procedimentoRealizado || "";
    document.querySelector("#realizacaoObservacoes").value = atendimento.observacoes || "";
    document.querySelector("#realizacaoValor").value = atendimento.valor ?? "";
    document.querySelector("#realizacaoAtendimentoTitulo").textContent = `Realizar atendimento #${atendimento.id}`;

    panel.hidden = false;
    setPageMessage("#realizacaoAtendimentoMessage", "");
    document.querySelector("#realizacaoProcedimento").focus();
  } catch (error) {
    panel.hidden = false;
    setPageMessage("#realizacaoAtendimentoMessage", error.message, "error");
  }
};

const fecharRealizacaoPanel = () => {
  const panel = document.querySelector("#realizacaoAtendimentoPanel");
  const form = document.querySelector("#realizacaoAtendimentoForm");
  form?.reset();
  if (panel) panel.hidden = true;
  setPageMessage("#realizacaoAtendimentoMessage", "");
};

const salvarRealizacao = async (event) => {
  event.preventDefault();

  const form = event.currentTarget;
  const id = document.querySelector("#realizacaoAtendimentoId")?.value;
  const button = document.querySelector("#salvarRealizacaoButton");
  if (!id) return;

  button.disabled = true;
  setPageMessage("#realizacaoAtendimentoMessage", "Registrando atendimento...");

  try {
    await fetchJson(
      `/api/atendimentos/${id}/realizar`,
      {
        method: "PATCH",
        headers: getAuthHeaders(),
        body: JSON.stringify(realizacaoPayloadFromForm(form)),
      },
      "Nao foi possivel realizar o atendimento."
    );

    fecharRealizacaoPanel();
    setPageMessage("#atendimentoMessage", "Atendimento realizado com sucesso.", "success");
    await carregarAtendimentos();
  } catch (error) {
    setPageMessage("#realizacaoAtendimentoMessage", error.message, "error");
  } finally {
    button.disabled = false;
  }
};

const executarAcaoTabela = async (event) => {
  const button = event.target.closest("button[data-action]");
  if (!button) return;

  const { action, id } = button.dataset;
  if (!id) return;

  button.disabled = true;
  setPageMessage("#atendimentoMessage", "Processando...");

  try {
    if (action === "confirmar") {
      await atualizarStatusAtendimento(id, "CONFIRMADO");
      setPageMessage("#atendimentoMessage", "Atendimento confirmado.", "success");
      await carregarAtendimentos();
    }

    if (action === "cancelar") {
      await cancelarAtendimento(id);
      setPageMessage("#atendimentoMessage", "Atendimento cancelado.", "success");
      await carregarAtendimentos();
    }

    if (action === "realizar") {
      await abrirRealizacaoPanel(id);
      setPageMessage("#atendimentoMessage", "");
    }
  } catch (error) {
    setPageMessage("#atendimentoMessage", error.message, "error");
  } finally {
    button.disabled = false;
  }
};

const bindBuscaComEnter = (input, callback) => {
  input?.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      callback();
    }
  });
};

export const initAtendimentosPage = () => {
  const form = document.querySelector("#atendimentoForm");
  const filtroForm = document.querySelector("#atendimentoFiltroForm");
  const realizacaoForm = document.querySelector("#realizacaoAtendimentoForm");
  const workspace = form?.closest(".workspace-grid");
  const formPanel = form?.closest(".panel");

  if (!form || form.dataset.ready === "true") return;

  if (!canAgendarAtendimento()) {
    formPanel.hidden = true;
    workspace.classList.add("single-panel");
  }

  form.dataset.ready = "true";
  preencherDataPadrao();

  form.addEventListener("submit", salvarAtendimento);
  realizacaoForm?.addEventListener("submit", salvarRealizacao);
  filtroForm.addEventListener("submit", (event) => {
    event.preventDefault();
    carregarAtendimentos();
  });

  document.querySelector("#atendimentosTabela")?.addEventListener("click", executarAcaoTabela);
  document.querySelector("#recarregarAtendimentosButton").addEventListener("click", carregarAtendimentos);
  document.querySelector("#fecharRealizacaoAtendimentoButton")?.addEventListener("click", fecharRealizacaoPanel);
  document.querySelector("#cancelarRealizacaoButton")?.addEventListener("click", fecharRealizacaoPanel);
  document.querySelector("#limparAtendimentoButton").addEventListener("click", () => {
    limparAtendimentoForm();
    setPageMessage("#atendimentoMessage", "");
  });
  document.querySelector("#buscarPacientesAtendimentoButton").addEventListener("click", async () => {
    try {
      await carregarPacientes();
    } catch (error) {
      setPageMessage("#atendimentoMessage", error.message, "error");
    }
  });
  document.querySelector("#buscarProfissionaisAtendimentoButton").addEventListener("click", async () => {
    try {
      await carregarProfissionais();
    } catch (error) {
      setPageMessage("#atendimentoMessage", error.message, "error");
    }
  });

  bindBuscaComEnter(document.querySelector("#atendimentoPacienteBusca"), carregarPacientes);
  bindBuscaComEnter(document.querySelector("#atendimentoProfissionalBusca"), carregarProfissionais);

  carregarAtendimentos();
};
