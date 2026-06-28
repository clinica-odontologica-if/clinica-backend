export const decodeJwtPayload = (token) => {
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

export const isTokenExpired = (payload) => {
  if (!payload || !payload.exp) return false;
  return Date.now() >= payload.exp * 1000;
};

export const getUserProfile = (payload) => {
  const name = payload?.nome
    || payload?.name
    || payload?.usuario
    || payload?.username
    || payload?.sub
    || payload?.email
    || "Usuario";

  const roleSource = payload?.perfil
    || payload?.role
    || payload?.cargo
    || payload?.roles
    || payload?.authorities
    || "Usuario";
  const role = Array.isArray(roleSource) ? roleSource.join(", ") : String(roleSource);

  return { name, role };
};

export const getInitials = (name) => name
  .split(" ")
  .filter(Boolean)
  .slice(0, 2)
  .map((part) => part[0])
  .join("")
  .toUpperCase();

export const escapeHtml = (value) => {
  const map = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;",
  };

  return String(value ?? "-").replace(/[&<>"']/g, (char) => map[char]);
};

export const formatCpf = (cpf) => {
  const digits = String(cpf ?? "").replace(/\D/g, "");
  if (digits.length !== 11) return cpf || "-";
  return digits.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
};

export const formatTelefone = (telefone) => {
  const digits = String(telefone ?? "").replace(/\D/g, "");
  if (digits.length === 10) {
    return digits.replace(/(\d{2})(\d{4})(\d{4})/, "($1) $2-$3");
  }
  if (digits.length === 11) {
    return digits.replace(/(\d{2})(\d{5})(\d{4})/, "($1) $2-$3");
  }
  return telefone || "-";
};

export const formatData = (data) => {
  if (!data) return "-";
  const [ano, mes, dia] = data.split("-");
  return ano && mes && dia ? `${dia}/${mes}/${ano}` : data;
};
