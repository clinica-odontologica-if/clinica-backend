import { TOKEN_KEY } from "./session.js";

export const getToken = () => localStorage.getItem(TOKEN_KEY);

export const getAuthHeaders = () => ({
  "Content-Type": "application/json",
  Authorization: `Bearer ${getToken()}`,
});

export const parseResponse = async (response) => {
  const text = await response.text();
  if (!text) return {};

  try {
    return JSON.parse(text);
  } catch {
    return { mensagem: text };
  }
};

export const getErrorMessage = (data, fallback) => {
  if (!data) return fallback;
  return data.mensagem || data.erro || data.message || fallback;
};

export const checkApi = async () => {
  const apiStatus = document.querySelector("#apiStatus");
  const dashboardStatus = document.querySelector("#dashboardStatus");

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
