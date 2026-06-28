import { checkApi } from "./api.js";
import { setupAuthentication } from "./auth.js";
import { renderPage, setupNavigation } from "./navigation.js";

setupNavigation();
setupAuthentication({ renderPage });
checkApi();
