const TOKEN_KEY = "clinica.auth.token";

const loginScreen = document.querySelector("#tela-login");
const dashboardScreen = document.querySelector("#tela-dashboard");
const form = document.querySelector("#loginForm");
const message = document.querySelector("#message");
const submitButton = document.querySelector("#submitButton");
const apiStatus = document.querySelector("#apiStatus");
const dashboardStatus = document.querySelector("#dashboardStatus");
const logoutButton = document.querySelector("#logoutButton");
const userName = document.querySelector("#userName");
const userRole = document.querySelector("#userRole");
const userInitials = document.querySelector("#userInitials");
const content = document.querySelector("#conteudo");
const pageTitle = document.querySelector("#pageTitle");
const navItems = document.querySelectorAll(".nav-item");
let currentProfile = { name: "Usuario", role: "Usuario" };

const pageTitles = {
  inicio: "Inicio",
  profissionais: "Profissionais",
  consultas: "Consultas",
  pacientes: "Pacientes",
  configuracoes: "Configuracoes",
};

const setMessage = (text, type = "") => {
  message.textContent = text;
  message.className = `message ${type}`.trim();
};

const decodeJwtPayload = (token) => {
  try {
    const payload = token.split(".")[1];
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
        atob(normalized)
            .split("")
            .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
            .join("")
    );

    return JSON.parse(json);
  } catch {
    return null;
  }
};

const isTokenExpired = (payload) => {
  if (!payload || !payload.exp) return false;
  return Date.now() >= payload.exp * 1000;
};

const getUserProfile = (payload) => {
  const name = payload?.nome || payload?.name || payload?.usuario || payload?.username || payload?.sub || payload?.email || "Usuario";

  const roleSource = payload?.perfil || payload?.role || payload?.cargo || payload?.roles || payload?.authorities || "Usuario";
  const role = Array.isArray(roleSource) ? roleSource.join(", ") : String(roleSource);

  return { name, role };
};

const getCurrentRole = () => String(currentProfile.role || "")
    .replace("ROLE_", "")
    .trim()
    .toUpperCase();

const hasAnyRole = (...roles) => roles.includes(getCurrentRole());

const getInitials = (name) => {
  return name
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0])
      .join("")
      .toUpperCase();
};

const getToken = () => localStorage.getItem(TOKEN_KEY);

const getAuthHeaders = () => ({
  "Content-Type": "application/json",
  Authorization: `Bearer ${getToken()}`,
});

const setPageMessage = (selector, text, type = "") => {
  const element = document.querySelector(selector);
  if (!element) return;
  element.textContent = text;
  element.className = `message ${type}`.trim();
};

const parseResponse = async (response) => {
  const text = await response.text();
  if (!text) return {};

  try {
    return JSON.parse(text);
  } catch {
    return { mensagem: text };
  }
};

const getErrorMessage = (data, fallback) => {
  if (!data) return fallback;
  return data.mensagem || data.erro || data.message || fallback;
};

const escapeHtml = (value) => {
  const map = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;",
  };

  return String(value ?? "-").replace(/[&<>"']/g, (char) => map[char]);
};

const showLogin = () => {
  loginScreen.hidden = false;
  dashboardScreen.hidden = true;
  document.title = "Clinica Odontologica | Login";
};

const showDashboard = (token) => {
  const payload = decodeJwtPayload(token);

  if (!payload || isTokenExpired(payload)) {
    localStorage.removeItem(TOKEN_KEY);
    showLogin();
    setMessage("Sessao expirada. Faca login novamente.", "error");
    return;
  }

  const profile = getUserProfile(payload);
  currentProfile = profile;
  userName.textContent = profile.name;
  userRole.textContent = profile.role;
  userInitials.textContent = getInitials(profile.name) || "U";

  loginScreen.hidden = true;
  dashboardScreen.hidden = false;
  setMessage("");
  renderPage("inicio");
};

const setSession = (token) => {
  if (!token) {
    localStorage.removeItem(TOKEN_KEY);
    showLogin();
    return;
  }

  localStorage.setItem(TOKEN_KEY, token);
  showDashboard(token);
};

const renderPage = (page) => {
  const template = document.querySelector(`#pagina-${page}`);
  if (!template) return;

  content.replaceChildren(template.content.cloneNode(true));
  pageTitle.textContent = pageTitles[page] || "Painel";
  document.title = `Clinica Odontologica | ${pageTitle.textContent}`;

  navItems.forEach((item) => {
    item.classList.toggle("active", item.dataset.page === page);
  });

  if (page === "profissionais") {
    initProfissionaisPage();
  }

  if (page === "pacientes") {
    initPacientesPage();
  }
};

const checkApi = async () => {
  try {
    const response = await fetch("/api/auth/health");
    if (!response.ok) throw new Error("API indisponivel");

    apiStatus.textContent = "online";
    apiStatus.className = "status-pill ok";
    dashboardStatus.textContent = "online";
    dashboardStatus.className = "status-pill ok";
  } catch {
    apiStatus.textContent = "offline";
    apiStatus.className = "status-pill offline";
    dashboardStatus.textContent = "offline";
    dashboardStatus.className = "status-pill offline";
  }
};

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

const initProfissionaisPage = () => {
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

const formatCpf = (cpf) => {
  const digits = String(cpf ?? "").replace(/\D/g, "");
  if (digits.length !== 11) return cpf || "-";
  return digits.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
};

const formatTelefone = (telefone) => {
  const digits = String(telefone ?? "").replace(/\D/g, "");
  if (digits.length === 10) {
    return digits.replace(/(\d{2})(\d{4})(\d{4})/, "($1) $2-$3");
  }
  if (digits.length === 11) {
    return digits.replace(/(\d{2})(\d{5})(\d{4})/, "($1) $2-$3");
  }
  return telefone || "-";
};

const formatData = (data) => {
  if (!data) return "-";
  const [ano, mes, dia] = data.split("-");
  return ano && mes && dia ? `${dia}/${mes}/${ano}` : data;
};

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

const initPacientesPage = () => {
  const formPaciente = document.querySelector("#pacienteForm");
  const reloadButton = document.querySelector("#recarregarPacientesButton");
  const cancelButton = document.querySelector("#cancelarEdicaoPacienteButton");
  const tbody = document.querySelector("#pacientesTabela");

  if (!formPaciente || formPaciente.dataset.ready === "true") return;

  formPaciente.dataset.ready = "true";
  formPaciente.addEventListener("submit", salvarPaciente);
  reloadButton.addEventListener("click", carregarPacientes);
  cancelButton.addEventListener("click", () => {
    limparPacienteForm();
    setPageMessage("#pacienteMessage", "");
  });
  tbody.addEventListener("click", handlePacienteAction);

  if (!canManagePacientes()) {
    formPaciente.querySelectorAll("input, textarea, button").forEach((element) => {
      element.disabled = true;
    });
    setPageMessage("#pacienteMessage", "Seu perfil permite apenas visualizar pacientes.");
  }

  carregarPacientes();
};

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  submitButton.disabled = true;
  setMessage("Autenticando...");

  const formData = new FormData(form);
  const payload = {
    email: formData.get("email"),
    senha: formData.get("senha"),
  };

  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.erro || "Falha no login");
    }

    setSession(data.token);
  } catch (error) {
    localStorage.removeItem(TOKEN_KEY);
    setMessage(error.message, "error");
  } finally {
    submitButton.disabled = false;
  }
});

logoutButton.addEventListener("click", () => {
  setSession(null);
  setMessage("Sessao encerrada.");
});

navItems.forEach((item) => {
  item.addEventListener("click", () => renderPage(item.dataset.page));
});

const savedToken = localStorage.getItem(TOKEN_KEY);
if (savedToken) {
  showDashboard(savedToken);
} else {
  showLogin();
}

checkApi();
