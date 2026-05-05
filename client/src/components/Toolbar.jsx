const TOOLS = [
  { id: 'pen', label: 'Pencil', shortcut: 'P' },
  { id: 'line', label: 'Line', shortcut: 'L' },
  { id: 'arrow', label: 'Arrow', shortcut: 'A' },
  { id: 'rectangle', label: 'Rectangle', shortcut: 'R' },
  { id: 'ellipse', label: 'Ellipse', shortcut: 'E' },
  { id: 'diamond', label: 'Diamond', shortcut: 'D' },
  { id: 'eraser', label: 'Eraser', shortcut: 'X' },
];

const STROKE_COLORS = ['#1f2937', '#0f766e', '#2563eb', '#dc2626', '#ea580c', '#9333ea', '#111827'];
const FILL_CHOICES = [
  { id: 'transparent', label: 'No fill' },
  { id: 'solid', label: 'Soft fill' },
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
  onExport,
}) {
  return (
    <>
      <aside className="toolbelt">
        <div className="toolbelt-group">
          {TOOLS.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`toolbelt-btn${tool === item.id ? ' active' : ''}`}
              onClick={() => onToolChange(item.id)}
              title={`${item.label} (${item.shortcut})`}
            >
              <span className="toolbelt-btn-label">{item.label}</span>
              <span className="toolbelt-btn-shortcut">{item.shortcut}</span>
            </button>
          ))}
        </div>
      </aside>

      <section className="style-panel">
        <div className="style-panel-section">
          <div className="panel-heading">
            <span>Stroke</span>
            <strong>{color}</strong>
          </div>
          <div className="swatch-row">
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

        <div className="style-panel-section">
          <div className="panel-heading">
            <span>Fill</span>
          </div>
          <div className="choice-row">
            {FILL_CHOICES.map((choice) => (
              <button
                key={choice.id}
                type="button"
                className={`choice-pill${fillStyle === choice.id ? ' active' : ''}`}
                onClick={() => onFillStyleChange(choice.id)}
              >
                {choice.label}
              </button>
            ))}
          </div>
        </div>

        <div className="style-panel-section">
          <div className="panel-heading">
            <span>Stroke width</span>
            <strong>{lineWidth}px</strong>
          </div>
          <input
            className="size-slider"
            type="range"
            min="1"
            max="18"
            value={lineWidth}
            onChange={(event) => onLineWidthChange(Number(event.target.value))}
          />
        </div>

        <div className="style-panel-actions">
          <button type="button" className="secondary-action" onClick={onExport}>
            Export PNG
          </button>
          <button type="button" className="danger-action" onClick={onClear}>
            Clear board
          </button>
        </div>
      </section>
    </>
  );
}
