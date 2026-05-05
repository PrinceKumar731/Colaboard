const TOOLS = [
  { id: 'pen', label: 'Pencil', shortcut: 'P', icon: '✎' },
  { id: 'line', label: 'Line', shortcut: 'L', icon: '／' },
  { id: 'arrow', label: 'Arrow', shortcut: 'A', icon: '→' },
  { id: 'rectangle', label: 'Rectangle', shortcut: 'R', icon: '▭' },
  { id: 'ellipse', label: 'Ellipse', shortcut: 'E', icon: '◯' },
  { id: 'diamond', label: 'Diamond', shortcut: 'D', icon: '◇' },
  { id: 'eraser', label: 'Eraser', shortcut: 'X', icon: '⌫' },
];

const STROKE_COLORS = ['#1f2937', '#d14c3f', '#5f9d55', '#4b77be', '#df912f', '#242424'];
const FILL_CHOICES = [
  { id: 'transparent', label: 'Transparent' },
  { id: 'solid', label: 'Solid' },
];

const WIDTH_CHOICES = [2, 4, 8];

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
  onExport,
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
              <span className="icon-tool-shortcut">{item.shortcut}</span>
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
          <div className="width-chip-row">
            {WIDTH_CHOICES.map((width) => (
              <button
                key={width}
                type="button"
                className={`width-chip${lineWidth === width ? ' active' : ''}`}
                onClick={() => onLineWidthChange(width)}
                aria-label={`Stroke width ${width}`}
              >
                <span style={{ height: Math.max(2, width) }} />
              </button>
            ))}
          </div>
        </div>

        <div className="dock-actions">
          <button type="button" className="mini-action" onClick={onExport} title="Export PNG">
            ⤓
          </button>
          <button type="button" className="mini-action danger" onClick={onClear} title="Clear board">
            ✕
          </button>
        </div>
      </aside>
    </>
  );
}
