import React from "react";

export default function EntityCell({
  title,
  subtitle,
  iconSrc,
  imageSrc,
  imageAlt = "",
  tone = "default"
}) {
  return (
    <div className="entity-cell">
      <span className={`entity-cell__visual entity-cell__visual--${tone}`} aria-hidden={!imageSrc}>
        {imageSrc ? (
          <img src={imageSrc} alt={imageAlt} loading="lazy" />
        ) : (
          <span
            className="svg-icon"
            style={{
              "--icon-url": `url("${iconSrc}")`,
              "--icon-size": "17px"
            }}
            aria-hidden="true"
          />
        )}
      </span>
      <span className="entity-cell__copy">
        <strong title={title}>{title}</strong>
        {subtitle ? <small title={subtitle}>{subtitle}</small> : null}
      </span>
    </div>
  );
}
