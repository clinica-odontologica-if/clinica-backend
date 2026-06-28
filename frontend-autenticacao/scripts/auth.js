import { parseResponse } from "./api.js";
import { setMessage } from "./messages.js";
import { setCurrentProfile, TOKEN_KEY } from "./session.js";
import { decodeJwtPayload, getInitials, getUserProfile, isTokenExpired } from "./utils.js";

let renderInitialPage = () => {};

const loginScreen = document.querySelector("#tela-login");
const dashboardScreen = document.querySelector("#tela-dashboard");
const form = document.querySelector("#loginForm");
const message = document.querySelector("#message");
const submitButton = document.querySelector("#submitButton");
const logoutButton = document.querySelector("#logoutButton");
const userName = document.querySelector("#userName");
const userRole = document.querySelector("#userRole");
const userInitials = document.querySelector("#userInitials");

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
    setMessage(message, "Sessao expirada. Faca login novamente.", "error");
    return;
  }

  const profile = getUserProfile(payload);
  setCurrentProfile(profile);
  userName.textContent = profile.name;
  userRole.textContent = profile.role;
  userInitials.textContent = getInitials(profile.name) || "U";

  loginScreen.hidden = true;
  dashboardScreen.hidden = false;
  setMessage(message, "");
  renderInitialPage("inicio");
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

const handleLogin = async (event) => {
  event.preventDefault();
  submitButton.disabled = true;
  setMessage(message, "Autenticando...");

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
    const data = await parseResponse(response);

    if (!response.ok) {
      throw new Error(data.erro || "Falha no login");
    }

    setSession(data.token);
  } catch (error) {
    localStorage.removeItem(TOKEN_KEY);
    setMessage(message, error.message, "error");
  } finally {
    submitButton.disabled = false;
  }
};

export const setupAuthentication = ({ renderPage }) => {
  renderInitialPage = renderPage;

  form.addEventListener("submit", handleLogin);
  logoutButton.addEventListener("click", () => {
    setSession(null);
    setMessage(message, "Sessao encerrada.");
  });

  const savedToken = localStorage.getItem(TOKEN_KEY);
  if (savedToken) {
    showDashboard(savedToken);
  } else {
    showLogin();
  }
};
