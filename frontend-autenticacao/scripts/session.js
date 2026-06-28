export const TOKEN_KEY = "clinica.auth.token";

let currentProfile = { name: "Usuario", role: "Usuario" };

export const setCurrentProfile = (profile) => {
  currentProfile = profile;
};

export const getCurrentProfile = () => currentProfile;

export const getCurrentRole = () => String(currentProfile.role || "")
  .replace("ROLE_", "")
  .trim()
  .toUpperCase();

export const hasAnyRole = (...roles) => roles.includes(getCurrentRole());
