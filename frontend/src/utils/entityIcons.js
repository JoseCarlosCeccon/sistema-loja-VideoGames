import fileTextIcon from "../assets/icons/file-text.svg";
import gamepadIcon from "../assets/icons/gamepad.svg";
import packageIcon from "../assets/icons/package.svg";

function normalize(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .toLowerCase();
}

export function productCategoryIcon(category) {
  return normalize(category) === "servico" ? fileTextIcon : packageIcon;
}

export function equipmentTypeIcon(type) {
  return ["console", "controle"].includes(normalize(type)) ? gamepadIcon : packageIcon;
}
