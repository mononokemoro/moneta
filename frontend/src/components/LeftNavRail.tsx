const RAIL_ITEMS = ["⌂", "▤", "▥", "◍", "◷", "⌁", "⚙"];

export function LeftNavRail() {
  return (
    <aside className="cb-rail" aria-label="기능 메뉴">
      <button type="button" className="cb-rail__profile" aria-label="프로필">
        ●
      </button>
      <div className="cb-rail__menu">
        {RAIL_ITEMS.map((icon, idx) => (
          <button key={idx} type="button" className={`cb-rail__btn${idx === 0 ? " is-active" : ""}`} aria-label={`menu-${idx}`}>
            {icon}
          </button>
        ))}
      </div>
      <button type="button" className="cb-rail__profile" aria-label="사용자">
        ◉
      </button>
    </aside>
  );
}

