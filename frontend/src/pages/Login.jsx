import { useState } from "react";
import { api } from "../services/api";
import loginLogo from "../assets/images/A-O-GAMES-1-20-2026.png";

export default function LoginPage({ message = "", onAuthenticated }) {
  const [form, setForm] = useState({ login: "", senha: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const visibleError = error || message;

  function updateField(field, value) {
    setForm((current) => ({
      ...current,
      [field]: value
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const usuario = await api.post("/usuarios/login", {
        login: form.login.trim(),
        senha: form.senha
      });

      onAuthenticated(usuario);
    } catch (requestError) {
      setError(requestError.message || "Nao foi possivel entrar no sistema.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-shell">
      <section className="login-card" aria-label="Acesso ao sistema">
        <div className="login-brand">
          <img src={loginLogo} alt="Logo A-O Games" className="login-logo" />
          <div>
            <p>Sistema operacional da loja</p>
          </div>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <label className="field">
            <span>Usuario</span>
            <input
              autoFocus
              required
              autoComplete="username"
              value={form.login}
              onChange={(event) => updateField("login", event.target.value)}
              placeholder="Digite seu login"
            />
          </label>

          <label className="field">
            <span>Senha</span>
            <input
              required
              type={showPassword ? "text" : "password"}
              autoComplete="current-password"
              value={form.senha}
              onChange={(event) => updateField("senha", event.target.value)}
              placeholder="Digite sua senha"
            />
          </label>

          <label className="checkbox-filter login-check">
            <input
              type="checkbox"
              checked={showPassword}
              onChange={(event) => setShowPassword(event.target.checked)}
            />
            <span className="checkbox-control" aria-hidden="true" />
            Mostrar senha
          </label>

          {visibleError ? (
            <div className="login-error" role="alert">
              {visibleError}
            </div>
          ) : null}

          <button type="submit" className="primary-button login-submit" disabled={loading}>
            {loading ? "Entrando..." : "Entrar"}
          </button>
        </form>
      </section>
    </main>
  );
}
