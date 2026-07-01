import { getAuthHeaders, getErrorMessage, parseResponse } from "../api.js";
import { setPageMessage } from "../messages.js";
import { hasAnyRole } from "../session.js";
import { escapeHtml, formatData } from "../utils.js";

const canManageReceitas = () => hasAnyRole("GERENTE", "ATENDENTE");
const canManageFinanceiro = () => hasAnyRole("GERENTE");

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

const formatEnum = (valor) =>
  String(valor || "-")
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/^./, (letra) => letra.toUpperCase());

const setLoading = (selector, colspan = 5) => {
  const tbody = document.querySelector(selector);
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="${colspan}">Carregando...</td></tr>`;
  }
};

const receitaPayloadFromForm = (form) => {
  const formData = new FormData(form);
  const status = formData.get("status") || "PENDENTE";
  const dataPagamento = formData.get("dataPagamento") || null;

  if (status === "PAGO" && !dataPagamento) {
    throw new Error("Informe a data de pagamento para registrar receita paga.");
  }

  return {
    atendimentoId: Number(formData.get("atendimentoId")),
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
          <td>#${escapeHtml(receita.atendimentoId ?? "-")}</td>
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
    renderReceitas(receitas);

    if (!canManageFinanceiro()) {
      renderResumoRestrito(receitas);
      renderDespesas([]);
      document.querySelector("#financeiroStatus").textContent = "atualizado";
      return;
    }

    const [despesas, relatorio] = await Promise.all([
      fetchJson(`/api/despesas?${query}`, {}, "Nao foi possivel carregar despesas."),
      fetchJson(`/api/relatorios/financeiro?${query}`, {}, "Nao foi possivel carregar o relatorio."),
    ]);

    renderDespesas(despesas);
    renderResumo(relatorio, receitas, despesas);
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

  document.querySelector("#receitaForm")?.addEventListener("submit", salvarReceita);
  document.querySelector("#despesaForm")?.addEventListener("submit", salvarDespesa);
  document.querySelector("#financeiroFiltroForm")?.addEventListener("submit", (event) => {
    event.preventDefault();
    carregarFinanceiro();
  });
  document.querySelector("#recarregarFinanceiroButton")?.addEventListener("click", carregarFinanceiro);

  carregarFinanceiro();
};
