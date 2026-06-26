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
