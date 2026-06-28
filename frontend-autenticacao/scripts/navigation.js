import { initPacientesPage } from "./pages/pacientes.js";
import { initProfissionaisPage } from "./pages/profissionais.js";

const pageTitles = {
  inicio: "Inicio",
  profissionais: "Profissionais",
  consultas: "Consultas",
  pacientes: "Pacientes",
  configuracoes: "Configuracoes",
};

export const renderPage = (page) => {
  const content = document.querySelector("#conteudo");
  const pageTitle = document.querySelector("#pageTitle");
  const navItems = document.querySelectorAll(".nav-item");
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

export const setupNavigation = () => {
  document.querySelectorAll(".nav-item").forEach((item) => {
    item.addEventListener("click", () => renderPage(item.dataset.page));
  });
};
