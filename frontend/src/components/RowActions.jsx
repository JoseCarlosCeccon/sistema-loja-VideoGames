import React, { useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

function availableActions(actions) {
  return (actions || []).filter(Boolean);
}

export default function RowActions({ primary, secondary, menuLabel = "Mais acoes" }) {
  const primaryActions = availableActions(primary);
  const secondaryActions = availableActions(secondary);
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const triggerRef = useRef(null);
  const menuRef = useRef(null);

  function closeMenu({ restoreFocus = false } = {}) {
    setOpen(false);
    if (restoreFocus) {
      requestAnimationFrame(() => triggerRef.current?.focus());
    }
  }

  function updatePosition() {
    const trigger = triggerRef.current;
    const menu = menuRef.current;
    if (!trigger || !menu) {
      return;
    }

    const triggerRect = trigger.getBoundingClientRect();
    const menuRect = menu.getBoundingClientRect();
    const gutter = 8;
    const top = triggerRect.bottom + gutter + menuRect.height <= window.innerHeight
      ? triggerRect.bottom + gutter
      : Math.max(gutter, triggerRect.top - menuRect.height - gutter);
    const left = Math.min(
      window.innerWidth - menuRect.width - gutter,
      Math.max(gutter, triggerRect.right - menuRect.width)
    );

    setPosition({ top, left });
  }

  useLayoutEffect(() => {
    if (open) {
      updatePosition();
    }
  }, [open]);

  useEffect(() => {
    if (!open) {
      return undefined;
    }

    function handlePointerDown(event) {
      if (!triggerRef.current?.contains(event.target) && !menuRef.current?.contains(event.target)) {
        closeMenu();
      }
    }

    function handleKeyDown(event) {
      if (event.key === "Escape") {
        closeMenu({ restoreFocus: true });
      }
    }

    function handleViewportChange() {
      updatePosition();
    }

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    window.addEventListener("resize", handleViewportChange);
    window.addEventListener("scroll", handleViewportChange, true);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("resize", handleViewportChange);
      window.removeEventListener("scroll", handleViewportChange, true);
    };
  }, [open]);

  function runAction(action) {
    closeMenu();
    action.onClick();
  }

  return (
    <div className="row-actions">
      {primaryActions.map((action) => (
        <button
          key={action.key || action.label}
          type="button"
          className={`table-action ${action.className || ""}`.trim()}
          onClick={action.onClick}
          aria-label={action.ariaLabel}
        >
          {action.label}
        </button>
      ))}

      {secondaryActions.length ? (
        <>
          <button
            ref={triggerRef}
            type="button"
            className="table-action row-actions__menu-trigger"
            aria-label={menuLabel}
            aria-haspopup="menu"
            aria-expanded={open}
            onClick={() => setOpen((current) => !current)}
            onKeyDown={(event) => {
              if (event.key === "ArrowDown") {
                event.preventDefault();
                setOpen(true);
                requestAnimationFrame(() => menuRef.current?.querySelector("button")?.focus());
              }
            }}
          >
            <span aria-hidden="true">...</span>
          </button>
          {open ? createPortal(
            <div
              ref={menuRef}
              className="row-actions-menu"
              role="menu"
              aria-label={menuLabel}
              style={{ top: position.top, left: position.left }}
            >
              {secondaryActions.map((action) => (
                <button
                  key={action.key || action.label}
                  type="button"
                  role="menuitem"
                  className={action.danger ? "row-actions-menu__danger" : ""}
                  onClick={() => runAction(action)}
                >
                  {action.label}
                </button>
              ))}
            </div>,
            document.body
          ) : null}
        </>
      ) : null}
    </div>
  );
}
