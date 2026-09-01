const serviceOrderStatusLabels = {
  RECEBIDO: "Recebido",
  EM_ANALISE: "Em análise",
  AGUARDANDO_APROVACAO: "Aguardando aprovação",
  APROVADO: "Aprovado",
  EM_MANUTENCAO: "Em manutenção",
  PRONTO: "Pronto",
  ENTREGUE: "Entregue",
  CANCELADO: "Não autorizado"
};

function normalizeStatus(status) {
  return String(status || "").trim().toUpperCase();
}

export function serviceOrderStatusLabel(status) {
  const normalized = normalizeStatus(status);
  if (serviceOrderStatusLabels[normalized]) {
    return serviceOrderStatusLabels[normalized];
  }

  if (!normalized) {
    return "Não informado";
  }

  return normalized
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

export function isServiceOrderWorkflowClosed(status) {
  return ["ENTREGUE", "CANCELADO"].includes(normalizeStatus(status));
}

export function canEditServiceOrder(status) {
  return normalizeStatus(status) !== "ENTREGUE";
}
