const COLORS = [
  '#000000', '#374151', '#ef4444', '#f97316',
  '#eab308', '#22c55e', '#3b82f6', '#8b5cf6',
  '#ec4899', '#ffffff'
];

export default function Toolbar({ tool, color, lineWidth, onToolChange, onColorChange, onLineWidthChange, onClear }) {
  return (
    <div className="toolbar">
      <div className="tool-section">
        <span className="section-label">Tool</span>
        <button
          className={`tool-btn${tool === 'pen' ? ' active' : ''}`}
          onClick={() => onToolChange('pen')}
        >
          Pen
        </button>
        <button
          className={`tool-btn${tool === 'eraser' ? ' active' : ''}`}
          onClick={() => onToolChange('eraser')}
        >
          Eraser
        </button>
      </div>

      <div className="tool-section">
        <span className="section-label">Color</span>
        <div className="color-grid">
          {COLORS.map((c) => (
            <button
              key={c}
              className={`color-swatch${color === c ? ' active' : ''}`}
              style={{ background: c, border: c === '#ffffff' ? '1.5px solid #d1d5db' : 'none' }}
              onClick={() => onColorChange(c)}
              title={c}
            />
          ))}
        </div>
      </div>

      <div className="tool-section">
        <span className="section-label">Size: {lineWidth}px</span>
        <input
          type="range"
          min="1"
          max="40"
          value={lineWidth}
          onChange={(e) => onLineWidthChange(Number(e.target.value))}
          className="size-range"
        />
        <div
          className="size-dot"
          style={{ width: Math.min(lineWidth, 36), height: Math.min(lineWidth, 36) }}
        />
      </div>

      <div className="tool-section">
        <button className="clear-btn" onClick={onClear}>
          Clear All
        </button>
      </div>
    </div>
  );
}
