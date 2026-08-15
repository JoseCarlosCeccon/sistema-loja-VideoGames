import React from "react";

const serviceOrderStatusConfig = {
  RECEBIDO: { label: "Recebido", tone: "received" },
  EM_ANALISE: { label: "Em análise", tone: "analysis" },
  AGUARDANDO_APROVACAO: { label: "Aguardando aprovação", tone: "waiting" },
  APROVADO: { label: "Aprovado", tone: "approved" },
  EM_MANUTENCAO: { label: "Em manutenção", tone: "progress" },
  PRONTO: { label: "Pronto", tone: "ready" },
  ENTREGUE: { label: "Entregue", tone: "delivered" },
  CANCELADO: { label: "Cancelado", tone: "cancelled" }
};

export default function ServiceOrderStatusBadge({ status }) {
  const normalizedStatus = String(status || "").toUpperCase();
  const config = serviceOrderStatusConfig[normalizedStatus] || {
    label: humanizeStatus(status),
    tone: "neutral"
  };

  return (
    <span className={`service-status-badge service-status-badge--${config.tone}`}>
      <span className="service-status-badge__dot" aria-hidden="true" />
      <span>{config.label}</span>
    </span>
  );
}

function humanizeStatus(status) {
  if (!status) {
    return "Não informado";
  }
  return String(status)
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}
