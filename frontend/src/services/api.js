const API_URL = import.meta.env.VITE_API_URL || "/api";
const DEFAULT_TIMEOUT_MS = 15000;

async function request(path, options = {}) {
  const { timeoutMs = DEFAULT_TIMEOUT_MS, ...fetchOptions } = options;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(`${API_URL}${path}`, {
      ...fetchOptions,
      signal: controller.signal,
      headers: {
        "Content-Type": "application/json",
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
      throw new Error(message);
    }

    return payload;
  } catch (error) {
    if (error.name === "AbortError") {
      throw new Error("A API demorou para responder. Reinicie o backend e tente novamente.");
    }

    if (error instanceof TypeError || error.message === "Failed to fetch") {
      throw new Error("Nao foi possivel conectar a API. Verifique se o backend esta em execucao e reinicie o servidor do frontend para ativar o proxy.");
    }

    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

export const api = {
  fileUrl(path) {
    return `${API_URL}${path}`;
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
  delete(path) {
    return request(path, {
      method: "DELETE"
    });
  }
};
