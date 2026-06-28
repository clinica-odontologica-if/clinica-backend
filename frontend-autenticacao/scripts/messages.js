export const setMessage = (element, text, type = "") => {
  if (!element) return;
  element.textContent = text;
  element.className = `message ${type}`.trim();
};

export const setPageMessage = (selector, text, type = "") => {
  setMessage(document.querySelector(selector), text, type);
};
