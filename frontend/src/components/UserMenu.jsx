import React, { useEffect, useId, useRef, useState } from "react";

export default function UserMenu({ name, profile, onLogout }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);
  const triggerRef = useRef(null);
  const logoutRef = useRef(null);
  const menuId = useId();

  useEffect(() => {
    if (!open) {
      return undefined;
    }

    function handlePointerDown(event) {
      if (!rootRef.current?.contains(event.target)) {
        setOpen(false);
      }
    }

    function handleKeyDown(event) {
      if (event.key === "Escape") {
        setOpen(false);
        triggerRef.current?.focus();
      }
    }

    document.addEventListener("pointerdown", handlePointerDown, true);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown, true);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  function openFromKeyboard(event) {
    if (event.key !== "ArrowDown") {
      return;
    }
    event.preventDefault();
    setOpen(true);
    window.requestAnimationFrame(() => logoutRef.current?.focus());
  }

  return (
    <div
      className="user-menu"
      ref={rootRef}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) {
          setOpen(false);
        }
      }}
    >
      <button
        type="button"
        ref={triggerRef}
        className={`user-menu__trigger ${open ? "user-menu__trigger--open" : ""}`}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        aria-label={`${open ? "Fechar" : "Abrir"} menu de ${name}`}
        onClick={() => setOpen((current) => !current)}
        onKeyDown={openFromKeyboard}
      >
        <span className="user-menu__avatar" aria-hidden="true">{userInitials(name)}</span>
        <span className="user-menu__name">{name}</span>
        <span className="user-menu__chevron" aria-hidden="true" />
      </button>

      {open ? (
        <div id={menuId} className="user-menu__dropdown" role="menu" aria-label="Menu do usuário">
          <div className="user-menu__identity" role="presentation">
            <strong>{name}</strong>
            <span>{profile}</span>
          </div>
          <button
            type="button"
            ref={logoutRef}
            className="user-menu__logout"
            role="menuitem"
            onClick={onLogout}
          >
            Sair
          </button>
        </div>
      ) : null}
    </div>
  );
}

function userInitials(name) {
  const parts = String(name || "Usuário").trim().split(/\s+/).filter(Boolean);
  if (parts.length <= 1) {
    return (parts[0] || "US").slice(0, 2).toUpperCase();
  }
  return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
}
