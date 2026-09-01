import React from "react";
import { serviceOrderStatusLabel } from "../utils/serviceOrderStatus";

const serviceOrderStatusConfig = {
  RECEBIDO: "received",
  EM_ANALISE: "analysis",
  AGUARDANDO_APROVACAO: "waiting",
  APROVADO: "approved",
  EM_MANUTENCAO: "progress",
  PRONTO: "ready",
  ENTREGUE: "delivered",
  CANCELADO: "cancelled"
};

export default function ServiceOrderStatusBadge({ status }) {
  const normalizedStatus = String(status || "").toUpperCase();
  const tone = serviceOrderStatusConfig[normalizedStatus] || "neutral";

  return (
    <span className={`service-status-badge service-status-badge--${tone}`}>
      <span className="service-status-badge__dot" aria-hidden="true" />
      <span>{serviceOrderStatusLabel(status)}</span>
    </span>
  );
}
