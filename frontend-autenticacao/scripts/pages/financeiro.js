import { getAuthHeaders, getErrorMessage, parseResponse } from "../api.js";
import { setPageMessage } from "../messages.js";
import { hasAnyRole } from "../session.js";
import { escapeHtml, formatData } from "../utils.js";

const canManageReceitas = () => hasAnyRole("GERENTE", "ATENDENTE");
const canManageFinanceiro = () => hasAnyRole("GERENTE");

let atendimentosFinanceiro = [];
let receitasFinanceiro = [];

const fetchJson = async (url, options = {}, fallback = "Nao foi possivel concluir a operacao.") => {
  const response = await fetch(url, {
    ...options,
    headers: {
      ...getAuthHeaders(),
      ...(options.headers || {}),
    },
  });
  const data = await parseResponse(response);
  if (!response.ok) {
    throw new Error(getErrorMessage(data, fallback));
  }
  return data;
};

const toIsoDate = (date) => date.toISOString().slice(0, 10);

const getPeriodoPadrao = () => {
  const hoje = new Date();
  const inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
  return {
    dataInicio: toIsoDate(inicio),
    dataFim: toIsoDate(hoje),
  };
};

const formatMoeda = (valor) =>
  Number(valor || 0).toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });

const formatHora = (hora) => String(hora || "").slice(0, 5) || "--:--";

const formatEnum = (valor) =>
  String(valor || "-")
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/^./, (letra) => letra.toUpperCase());

const getAtendimentoLabel = (atendimento) => [
  `${formatData(atendimento.data)} ${formatHora(atendimento.hora)}`,
  atendimento.pacienteNome || "Paciente nao informado",
  atendimento.profissionalNome || "Dentista nao informado",
  formatEnum(atendimento.status),
].join(" - ");

const getAtendimentoResumo = (atendimentoId) => {
  const atendimento = atendimentosFinanceiro.find((item) => Number(item.id) === Number(atendimentoId));
  return atendimento ? getAtendimentoLabel(atendimento) : `Atendimento #${atendimentoId ?? "-"}`;
};

const getAtendimentosComReceita = () => new Set(receitasFinanceiro.map((receita) => Number(receita.atendimentoId)));

const isAtendimentoSelecionavel = (atendimento) => {
  const status = String(atendimento.status || "").toUpperCase();
  return atendimento.ativo && !["CANCELADO", "NAO_COMPARECEU"].includes(status) && !getAtendimentosComReceita().has(Number(atendimento.id));
};

const setLoading = (selector, colspan = 5) => {
  const tbody = document.querySelector(selector);
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="${colspan}">Carregando...</td></tr>`;
  }
};

const getPagamentoFiltroParams = () => {
  const params = new URLSearchParams();
  const busca = document.querySelector("#pagamentoBuscaAtendimento")?.value?.trim();
  const dataInicio = document.querySelector("#pagamentoDataInicio")?.value;
  const dataFim = document.querySelector("#pagamentoDataFim")?.value;
  const status = document.querySelector("#pagamentoStatusAtendimento")?.value;

  if (busca) params.set("busca", busca);
  if (dataInicio) params.set("dataInicio", dataInicio);
  if (dataFim) params.set("dataFim", dataFim);
  if (status) params.set("status", status);

  return params;
};

const limparAtendimentoSelecionado = () => {
  const atendimentoId = document.querySelector("#receitaAtendimentoId");
  const resumo = document.querySelector("#atendimentoSelecionadoResumo");
  if (atendimentoId) atendimentoId.value = "";
  if (resumo) resumo.textContent = "Nenhum atendimento selecionado.";
};

const renderAtendimentosPagamento = (atendimentos = []) => {
  const tbody = document.querySelector("#pagamentoAtendimentosTabela");
  if (!tbody) return;

  const selecionaveis = atendimentos.filter(isAtendimentoSelecionavel);
  if (!selecionaveis.length) {
    tbody.innerHTML = '<tr><td colspan="6">Nenhum atendimento disponivel para pagamento.</td></tr>';
    limparAtendimentoSelecionado();
    return;
  }

  tbody.innerHTML = selecionaveis
    .map((atendimento) => `
      <tr>
        <td>${escapeHtml(formatData(atendimento.data))}<br><small>${escapeHtml(formatHora(atendimento.hora))}</small></td>
        <td>${escapeHtml(atendimento.pacienteNome || "-")}</td>
        <td>${escapeHtml(atendimento.profissionalNome || "-")}</td>
        <td><span class="status-badge status-${String(atendimento.status || "").toLowerCase()}">${escapeHtml(formatEnum(atendimento.status))}</span></td>
        <td>${formatMoeda(atendimento.valor)}</td>
        <td><button type="button" class="table-action" data-action="selecionar-atendimento" data-id="${atendimento.id}">Selecionar</button></td>
      </tr>
    `)
    .join("");
};

const carregarAtendimentosParaReceita = async () => {
  const tbody = document.querySelector("#pagamentoAtendimentosTabela");
  if (!tbody || !canManageReceitas()) return;

  setLoading("#pagamentoAtendimentosTabela", 6);
  try {
    const params = getPagamentoFiltroParams();
    const atendimentos = await fetchJson(
      `/api/atendimentos${params.toString() ? `?${params}` : ""}`,
      {},
      "Nao foi possivel carregar atendimentos."
    );
    atendimentosFinanceiro = Array.isArray(atendimentos) ? atendimentos : [];
    renderAtendimentosPagamento(atendimentosFinanceiro);
  } catch (error) {
    tbody.innerHTML = `<tr><td colspan="6">${escapeHtml(error.message)}</td></tr>`;
    setPageMessage("#receitaMessage", error.message, "error");
  }
};

const selecionarAtendimentoParaPagamento = (atendimentoId) => {
  const atendimento = atendimentosFinanceiro.find((item) => Number(item.id) === Number(atendimentoId));
  if (!atendimento) return;

  document.querySelector("#receitaAtendimentoId").value = atendimento.id;
  document.querySelector("#atendimentoSelecionadoResumo").textContent = `Selecionado: ${getAtendimentoLabel(atendimento)}`;

  if (atendimento.valor && Number(atendimento.valor) > 0) {
    document.querySelector("#receitaValor").value = Number(atendimento.valor).toFixed(2);
  }
};

const handlePagamentoAtendimentoAction = (event) => {
  const button = event.target.closest("button[data-action='selecionar-atendimento']");
  if (!button) return;
  selecionarAtendimentoParaPagamento(button.dataset.id);
};

const receitaPayloadFromForm = (form) => {
  const formData = new FormData(form);
  const atendimentoId = Number(formData.get("atendimentoId"));
  const status = formData.get("status") || "PENDENTE";
  const dataPagamento = formData.get("dataPagamento") || null;

  if (!atendimentoId) {
    throw new Error("Selecione um atendimento para registrar o pagamento.");
  }

  if (status === "PAGO" && !dataPagamento) {
    throw new Error("Informe a data de pagamento para registrar receita paga.");
  }

  return {
    atendimentoId,
    descricao: String(formData.get("descricao") || "Pagamento de atendimento").trim(),
    valor: Number(formData.get("valor")),
    formaPagamento: formData.get("formaPagamento"),
    status,
    dataVencimento: formData.get("dataVencimento") || null,
    dataPagamento,
  };
};

const despesaPayloadFromForm = (form) => {
  const formData = new FormData(form);
  const status = formData.get("status") || "PENDENTE";
  const dataPagamento = formData.get("dataPagamento") || null;

  if (status === "PAGO" && !dataPagamento) {
    throw new Error("Informe a data de pagamento para registrar despesa paga.");
  }

  return {
    descricao: String(formData.get("descricao") || "").trim(),
    categoria: formData.get("categoria"),
    valor: Number(formData.get("valor")),
    status,
    dataVencimento: formData.get("dataVencimento") || null,
    dataPagamento,
  };
};

const renderResumoRestrito = (receitas = []) => {
  const totalReceitas = receitas.reduce((total, item) => total + Number(item.valor || 0), 0);
  document.querySelector("#financeiroTotalReceitas").textContent = formatMoeda(totalReceitas);
  document.querySelector("#financeiroTotalDespesas").textContent = "Restrito";
  document.querySelector("#financeiroSaldo").textContent = "Restrito";
};

const renderResumo = (relatorio, receitas = [], despesas = []) => {
  const totalReceitas = relatorio?.totalReceitas ?? receitas.reduce((total, item) => total + Number(item.valor || 0), 0);
  const totalDespesas = relatorio?.totalDespesas ?? despesas.reduce((total, item) => total + Number(item.valor || 0), 0);
  const saldo = relatorio?.saldo ?? totalReceitas - totalDespesas;

  document.querySelector("#financeiroTotalReceitas").textContent = formatMoeda(totalReceitas);
  document.querySelector("#financeiroTotalDespesas").textContent = formatMoeda(totalDespesas);
  document.querySelector("#financeiroSaldo").textContent = formatMoeda(saldo);
};

const renderReceitas = (receitas = []) => {
  const tbody = document.querySelector("#receitasTabela");
  if (!receitas.length) {
    tbody.innerHTML = '<tr><td colspan="5">Nenhuma receita encontrada.</td></tr>';
    return;
  }

  tbody.innerHTML = receitas
    .map(
      (receita) => `
        <tr>
          <td>${escapeHtml(getAtendimentoResumo(receita.atendimentoId))}</td>
          <td>${formatMoeda(receita.valor)}</td>
          <td>${escapeHtml(formatEnum(receita.formaPagamento))}</td>
          <td><span class="status-badge status-${String(receita.status || "").toLowerCase()}">${escapeHtml(formatEnum(receita.status))}</span></td>
          <td>${escapeHtml(formatData(receita.dataPagamento || receita.dataVencimento))}</td>
        </tr>
      `,
    )
    .join("");
};

const renderDespesas = (despesas = []) => {
  const tbody = document.querySelector("#despesasTabela");
  if (!canManageFinanceiro()) {
    tbody.innerHTML = '<tr><td colspan="5">Acesso restrito ao gerente.</td></tr>';
    return;
  }

  if (!despesas.length) {
    tbody.innerHTML = '<tr><td colspan="5">Nenhuma despesa encontrada.</td></tr>';
    return;
  }

  tbody.innerHTML = despesas
    .map(
      (despesa) => `
        <tr>
          <td>${escapeHtml(despesa.descricao)}</td>
          <td>${formatMoeda(despesa.valor)}</td>
          <td>${escapeHtml(formatEnum(despesa.categoria))}</td>
          <td><span class="status-badge status-${String(despesa.status || "").toLowerCase()}">${escapeHtml(formatEnum(despesa.status))}</span></td>
          <td>${escapeHtml(formatData(despesa.dataPagamento || despesa.dataVencimento))}</td>
        </tr>
      `,
    )
    .join("");
};

const getPeriodoFromForm = () => {
  const dataInicio = document.querySelector("#financeiroDataInicio").value;
  const dataFim = document.querySelector("#financeiroDataFim").value;
  return { dataInicio, dataFim };
};

const buildPeriodoQuery = ({ dataInicio, dataFim }) => {
  const params = new URLSearchParams();
  if (dataInicio) params.set("dataInicio", dataInicio);
  if (dataFim) params.set("dataFim", dataFim);
  return params.toString();
};

const carregarFinanceiro = async () => {
  if (!canManageReceitas() && !canManageFinanceiro()) {
    renderReceitas([]);
    renderDespesas([]);
    setPageMessage("#receitaMessage", "Seu perfil nao possui acesso ao financeiro.", "error");
    return;
  }

  const periodo = getPeriodoFromForm();
  const query = buildPeriodoQuery(periodo);
  document.querySelector("#financeiroStatus").textContent = "carregando";
  setLoading("#receitasTabela");
  setLoading("#despesasTabela");

  try {
    const receitas = await fetchJson(`/api/receitas?${query}`, {}, "Nao foi possivel carregar receitas.");
    receitasFinanceiro = Array.isArray(receitas) ? receitas : [];
    await carregarAtendimentosParaReceita();
    renderReceitas(receitasFinanceiro);

    if (!canManageFinanceiro()) {
      renderResumoRestrito(receitasFinanceiro);
      renderDespesas([]);
      document.querySelector("#financeiroStatus").textContent = "atualizado";
      return;
    }

    const [despesas, relatorio] = await Promise.all([
      fetchJson(`/api/despesas?${query}`, {}, "Nao foi possivel carregar despesas."),
      fetchJson(`/api/relatorios/financeiro?${query}`, {}, "Nao foi possivel carregar o relatorio."),
    ]);

    renderDespesas(despesas);
    renderResumo(relatorio, receitasFinanceiro, despesas);
    document.querySelector("#financeiroStatus").textContent = "atualizado";
  } catch (error) {
    setPageMessage("#receitaMessage", error.message, "error");
    document.querySelector("#financeiroStatus").textContent = "erro";
  }
};

const salvarReceita = async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const button = document.querySelector("#salvarReceitaButton");
  button.disabled = true;

  try {
    const payload = receitaPayloadFromForm(form);
    await fetchJson(
      "/api/receitas",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
      "Nao foi possivel registrar a receita.",
    );
    form.reset();
    document.querySelector("#receitaDescricao").value = "Pagamento de atendimento";
    limparAtendimentoSelecionado();
    setPageMessage("#receitaMessage", "Receita registrada com sucesso.", "success");
    await carregarFinanceiro();
  } catch (error) {
    setPageMessage("#receitaMessage", error.message, "error");
  } finally {
    button.disabled = false;
  }
};

const salvarDespesa = async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const button = document.querySelector("#salvarDespesaButton");
  button.disabled = true;

  try {
    const payload = despesaPayloadFromForm(form);
    await fetchJson(
      "/api/despesas",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
      "Nao foi possivel registrar a despesa.",
    );
    form.reset();
    setPageMessage("#despesaMessage", "Despesa registrada com sucesso.", "success");
    await carregarFinanceiro();
  } catch (error) {
    setPageMessage("#despesaMessage", error.message, "error");
  } finally {
    button.disabled = false;
  }
};

const configurarPeriodoInicial = () => {
  const { dataInicio, dataFim } = getPeriodoPadrao();
  document.querySelector("#financeiroDataInicio").value = dataInicio;
  document.querySelector("#financeiroDataFim").value = dataFim;
  document.querySelector("#pagamentoDataInicio").value = dataInicio;
  document.querySelector("#pagamentoDataFim").value = dataFim;
};

const aplicarRestricoesDePerfil = () => {
  if (!canManageReceitas()) {
    document.querySelector("#receitaForm")?.closest(".panel")?.setAttribute("hidden", "");
  }

  if (!canManageFinanceiro()) {
    document.querySelector("#despesaForm")?.closest(".panel")?.setAttribute("hidden", "");
  }
};

export const initFinanceiroPage = () => {
  configurarPeriodoInicial();
  aplicarRestricoesDePerfil();

  document.querySelector("#pagamentoAtendimentosTabela")?.addEventListener("click", handlePagamentoAtendimentoAction);
  document.querySelector("#buscarAtendimentosPagamentoButton")?.addEventListener("click", carregarAtendimentosParaReceita);
  document.querySelector("#receitaForm")?.addEventListener("submit", salvarReceita);
  document.querySelector("#despesaForm")?.addEventListener("submit", salvarDespesa);
  document.querySelector("#financeiroFiltroForm")?.addEventListener("submit", (event) => {
    event.preventDefault();
    carregarFinanceiro();
  });
  document.querySelector("#recarregarFinanceiroButton")?.addEventListener("click", carregarFinanceiro);

  carregarFinanceiro();
};