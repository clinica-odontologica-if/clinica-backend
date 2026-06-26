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