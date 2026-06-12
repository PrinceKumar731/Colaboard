const TOOLS = [
  {
    id: 'select',
    label: 'Cursor',
    shortcut: 'V',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M5 3l13 9-7 1.2L8 20 5 3z" />
      </svg>
    ),
  },
  {
    id: 'pen',
    label: 'Pencil',
    shortcut: 'P',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 16.5V20h3.5L18.1 9.4l-3.5-3.5L4 16.5z" />
        <path d="M15.8 4.7l1.1-1.1a1.7 1.7 0 012.4 0l1.1 1.1a1.7 1.7 0 010 2.4l-1.1 1.1-3.5-3.5z" />
      </svg>
    ),
  },
  {
    id: 'line',
    label: 'Line',
    shortcut: 'L',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M5 19L19 5" />
      </svg>
    ),
  },
  {
    id: 'arrow',
    label: 'Arrow',
    shortcut: 'A',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 12h15" />
        <path d="M14 7l5 5-5 5" />
      </svg>
    ),
  },
  {
    id: 'rectangle',
    label: 'Rectangle',
    shortcut: 'R',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M5 7h14v10H5z" />
      </svg>
    ),
  },
  {
    id: 'ellipse',
    label: 'Ellipse',
    shortcut: 'E',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 12a8 6.5 0 1016 0 8 6.5 0 10-16 0z" />
      </svg>
    ),
  },
  {
    id: 'diamond',
    label: 'Diamond',
    shortcut: 'D',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 4l8 8-8 8-8-8 8-8z" />
      </svg>
    ),
  },
  {
    id: 'eraser',
    label: 'Eraser',
    shortcut: 'X',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4.8 14.8l6.8-6.8a2 2 0 012.8 0l4.1 4.1a2 2 0 010 2.8l-4.3 4.3H8.8l-4-4.4z" />
        <path d="M8.8 19.2H20" />
        <path d="M10.8 9.2l5 5" />
      </svg>
    ),
  },
];

const STROKE_COLORS = ['#1f2937', '#d14c3f', '#5f9d55', '#4b77be', '#df912f', '#242424'];
const FILL_CHOICES = [
  { id: 'transparent', label: 'Transparent' },
  { id: 'solid', label: 'Solid' },
];

export default function Toolbar({
  tool,
  color,
  fillStyle,
  lineWidth,
  onToolChange,
  onColorChange,
  onFillStyleChange,
  onLineWidthChange,
  onClear,
}) {
  return (
    <>
      <nav className="top-toolbar">
        <div className="top-toolbar-group">
          {TOOLS.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`icon-tool${tool === item.id ? ' active' : ''}`}
              onClick={() => onToolChange(item.id)}
              title={`${item.label} (${item.shortcut})`}
              aria-label={item.label}
            >
              <span className="icon-tool-glyph">{item.icon}</span>
            </button>
          ))}
        </div>
      </nav>

      <aside className="style-dock">
        <div className="dock-section">
          <span className="dock-label">Stroke</span>
          <div className="swatch-row compact">
            {STROKE_COLORS.map((swatch) => (
              <button
                key={swatch}
                type="button"
                className={`swatch${color === swatch ? ' active' : ''}`}
                style={{ backgroundColor: swatch }}
                onClick={() => onColorChange(swatch)}
                title={swatch}
              />
            ))}
          </div>
        </div>

        <div className="dock-section">
          <span className="dock-label">Fill</span>
          <div className="compact-pill-row">
            {FILL_CHOICES.map((choice) => (
              <button
                key={choice.id}
                type="button"
                className={`choice-pill compact${fillStyle === choice.id ? ' active' : ''}`}
                onClick={() => onFillStyleChange(choice.id)}
              >
                {choice.label}
              </button>
            ))}
          </div>
        </div>

        <div className="dock-section">
          <span className="dock-label">Width</span>
          <input
            className="size-slider compact"
            type="range"
            min="1"
            max="18"
            value={lineWidth}
            onChange={(event) => onLineWidthChange(Number(event.target.value))}
          />
        </div>

        <div className="dock-actions">
          <button type="button" className="mini-action danger" onClick={onClear} title="Clear board">
            Clear
          </button>
        </div>
      </aside>
    </>
  );
}
