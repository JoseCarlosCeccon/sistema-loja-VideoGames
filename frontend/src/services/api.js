const API_URL = import.meta.env.VITE_API_URL || "/api";
const DEFAULT_TIMEOUT_MS = 15000;
export const AUTH_STORAGE_KEY = "acao-games-auth-user";
export const AUTH_REJECTED_EVENT = "acao-games-auth-rejected";

function readAuthToken() {
  if (typeof window === "undefined") {
    return "";
  }

  try {
    const stored = window.localStorage.getItem(AUTH_STORAGE_KEY);
    const usuario = stored ? JSON.parse(stored) : null;
    return usuario?.token || "";
  } catch {
    return "";
  }
}

async function request(path, options = {}) {
  const { timeoutMs = DEFAULT_TIMEOUT_MS, ...fetchOptions } = options;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
  const isFormData = fetchOptions.body instanceof FormData;
  const token = readAuthToken();

  try {
    const response = await fetch(`${API_URL}${path}`, {
      ...fetchOptions,
      signal: controller.signal,
      headers: {
        ...(isFormData ? {} : { "Content-Type": "application/json" }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(fetchOptions.headers || {})
      }
    });

    if (response.status === 204) {
      return null;
    }

    const contentType = response.headers.get("content-type") || "";
    const isJson = contentType.includes("application/json");
    const payload = isJson ? await response.json() : null;

    if (!response.ok) {
      const message = payload?.details || payload?.message || "Falha ao processar a requisicao.";
      if (path !== "/usuarios/login" && (response.status === 401 || response.status === 403)) {
        notifyAuthRejected(response.status, message);
      }

      const requestError = new Error(message);
      requestError.status = response.status;
      requestError.payload = payload;
      throw requestError;
    }

    return payload;
  } catch (error) {
    if (error.name === "AbortError") {
      throw new Error("A API demorou para responder. Reinicie o backend e tente novamente.");
    }

    if (error instanceof TypeError || error.message === "Failed to fetch") {
      throw new Error("Nao foi possivel conectar a API. Verifique se o backend esta em execucao em http://localhost:8080.");
    }

    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

function notifyAuthRejected(status, message) {
  if (typeof window === "undefined") {
    return;
  }

  window.dispatchEvent(new CustomEvent(AUTH_REJECTED_EVENT, {
    detail: {
      status,
      message: message || "Sua sessao expirou. Entre novamente."
    }
  }));
}

export const api = {
  fileUrl(path) {
    const token = readAuthToken();
    const separator = path.includes("?") ? "&" : "?";
    return token ? `${API_URL}${path}${separator}access_token=${encodeURIComponent(token)}` : `${API_URL}${path}`;
  },
  get(path) {
    return request(path);
  },
  post(path, body) {
    return request(path, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  put(path, body) {
    return request(path, {
      method: "PUT",
      body: JSON.stringify(body)
    });
  },
  upload(path, formData) {
    return request(path, {
      method: "POST",
      body: formData,
      timeoutMs: 30000
    });
  },
  delete(path) {
    return request(path, {
      method: "DELETE"
    });
  }
};
