/**
 * drawing-common.js
 * キャンバス描画の共通ロジック。
 * drawing-new.html / drawing-edit.html から読み込んで使用する。
 *
 * 【使い方】
 *   1. <canvas id="canvas"> を用意する。
 *   2. このファイルを読み込んだあと DrawingCanvas を new する。
 *      const dc = new DrawingCanvas('canvas');
 *   3. 必要に応じて dc.loadStrokes(strokesArray) で既存データを流し込む。
 *   4. 送信時は dc.getStrokes() でストローク配列を取得する。
 */

class DrawingCanvas {
  /**
   * @param {string} canvasId  - <canvas> の id
   * @param {object} [options]
   * @param {number} [options.lineWidth=2]      - 通常ペンの太さ
   * @param {number} [options.eraserWidth=20]   - 消しゴムの太さ
   */
  constructor(canvasId, options = {}) {
    this.canvas = document.getElementById(canvasId);
    this.ctx    = this.canvas.getContext('2d');

    this.lineWidth   = options.lineWidth   ?? 2;
    this.eraserWidth = options.eraserWidth ?? 20;

    this.strokes     = [];   // 確定済みストローク: { points, eraser }[]
    this.current     = null; // 描画中ストローク

    this.isEraser    = false; // 現在のモード
    this._drawing    = false;

    this._bindEvents();
  }

  /* ─────────────────────────────────────────
   * 公開 API
   * ───────────────────────────────────────── */

  /** 保存済みのストローク配列をキャンバスに読み込む */
  loadStrokes(strokes) {
    this.strokes = strokes.map(s => ({
      points: s.points ?? s, // 旧形式（points配列直接）も許容
      eraser: s.eraser ?? false,
    }));
    this._redraw();
  }

  /** 現在のストローク配列を返す（JSON保存用） */
  getStrokes() {
    return this.strokes;
  }

  /** ペンモードに切り替え */
  setPen() {
    this.isEraser = false;
    this.canvas.style.cursor = 'crosshair';
  }

  /** 消しゴムモードに切り替え */
  setEraser() {
    this.isEraser = true;
    this.canvas.style.cursor = `url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="${this.eraserWidth}" height="${this.eraserWidth}"><rect width="${this.eraserWidth}" height="${this.eraserWidth}" rx="3" fill="white" stroke="%23999" stroke-width="1.5"/></svg>') ${this.eraserWidth/2} ${this.eraserWidth/2}, cell`;
  }

  /** 1ストローク戻す */
  undo() {
    if (this.strokes.length === 0) return;
    this.strokes.pop();
    this._redraw();
  }

  /** キャンバスを全消し */
  clear() {
    this.strokes = [];
    this._redraw();
  }

  /* ─────────────────────────────────────────
   * 内部処理
   * ───────────────────────────────────────── */

  _getPoint(e) {
    const rect = this.canvas.getBoundingClientRect();
    // タッチ対応
    const src = e.touches ? e.touches[0] : e;
    return {
      x: src.clientX - rect.left,
      y: src.clientY - rect.top,
    };
  }

  _startDraw(e) {
    e.preventDefault();
    this._drawing = true;
    const pt = this._getPoint(e);
    this.current = { points: [pt], eraser: this.isEraser };
    // リアルタイム描画開始
    this._applyStyle(this.ctx, this.current);
    this.ctx.beginPath();
    this.ctx.moveTo(pt.x, pt.y);
  }

  _moveDraw(e) {
    e.preventDefault();
    if (!this._drawing) return;
    const pt = this._getPoint(e);
    this.current.points.push(pt);
    this.ctx.lineTo(pt.x, pt.y);
    this.ctx.stroke();
  }

  _endDraw(e) {
    if (!this._drawing) return;
    this._drawing = false;
    if (this.current && this.current.points.length > 0) {
      this.strokes.push(this.current);
    }
    this.current = null;
  }

  _applyStyle(ctx, stroke) {
    if (stroke.eraser) {
      ctx.globalCompositeOperation = 'destination-out';
      ctx.strokeStyle = 'rgba(0,0,0,1)';
      ctx.lineWidth   = this.eraserWidth;
    } else {
      ctx.globalCompositeOperation = 'source-over';
      ctx.strokeStyle = '#000000';
      ctx.lineWidth   = this.lineWidth;
    }
    ctx.lineCap  = 'round';
    ctx.lineJoin = 'round';
  }

  _drawStroke(stroke) {
    const pts = stroke.points;
    if (!pts || pts.length === 0) return;
    this._applyStyle(this.ctx, stroke);
    this.ctx.beginPath();
    this.ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length; i++) {
      this.ctx.lineTo(pts[i].x, pts[i].y);
    }
    this.ctx.stroke();
  }

  _redraw() {
    this.ctx.globalCompositeOperation = 'source-over';
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    for (const stroke of this.strokes) {
      this._drawStroke(stroke);
    }
    // 描画後は必ず source-over に戻す
    this.ctx.globalCompositeOperation = 'source-over';
  }

  _bindEvents() {
    // マウス
    this.canvas.addEventListener('mousedown',  e => this._startDraw(e));
    this.canvas.addEventListener('mousemove',  e => this._moveDraw(e));
    this.canvas.addEventListener('mouseup',    e => this._endDraw(e));
    this.canvas.addEventListener('mouseleave', e => this._endDraw(e));
    // タッチ（スマホ・タブレット対応）
    this.canvas.addEventListener('touchstart', e => this._startDraw(e), { passive: false });
    this.canvas.addEventListener('touchmove',  e => this._moveDraw(e),  { passive: false });
    this.canvas.addEventListener('touchend',   e => this._endDraw(e));
  }
}