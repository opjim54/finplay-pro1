// War Land 3D - HUD Customization & Visibility Settings System v2.0
// Full per-button opacity, visibility, reset, persistent storage

class HudCustomizer {
    constructor() {
        // Default settings for every HUD button
        this.defaultSettings = {
            btnFire:        { visible: true, opacity: 1.0 },
            btnJump:        { visible: true, opacity: 1.0 },
            btnCrouch:      { visible: true, opacity: 1.0 },
            btnProne:       { visible: true, opacity: 1.0 },
            btnSprint:      { visible: true, opacity: 1.0 },
            btnSlide:       { visible: true, opacity: 1.0 },
            btnScope:       { visible: true, opacity: 1.0 },
            btnReload:      { visible: true, opacity: 1.0 },
            btnGrenade:     { visible: true, opacity: 1.0 },
            btnMedkit:      { visible: true, opacity: 1.0 },
            btnGlooWall:    { visible: true, opacity: 1.0 },
            btnEmoteWheel:  { visible: true, opacity: 0.85 },
            btnShop:        { visible: true, opacity: 0.85 },
            btnSettings:    { visible: true, opacity: 1.0 },
            joystickZone:   { visible: true, opacity: 1.0 },
        };

        this.settings = this.load();
        this.apply();
    }

    // ── LOAD FROM localStorage ─────────────────────────────────────────────────
    load() {
        try {
            const raw = localStorage.getItem('warland_hud_settings');
            if (raw) {
                const parsed = JSON.parse(raw);
                // Merge with defaults (handle new buttons added later)
                return Object.assign({}, this.defaultSettings, parsed);
            }
        } catch(e) {}
        return JSON.parse(JSON.stringify(this.defaultSettings));
    }

    // ── SAVE TO localStorage ───────────────────────────────────────────────────
    save() {
        try {
            localStorage.setItem('warland_hud_settings', JSON.stringify(this.settings));
        } catch(e) {}
    }

    // ── APPLY SETTINGS TO DOM ──────────────────────────────────────────────────
    apply() {
        for (const [id, cfg] of Object.entries(this.settings)) {
            const el = document.getElementById(id);
            if (!el) continue;
            el.style.opacity    = cfg.opacity;
            el.style.display    = cfg.visible ? '' : 'none';
            el.style.visibility = cfg.visible ? '' : 'hidden';
            el.style.pointerEvents = cfg.visible && cfg.opacity > 0.05 ? '' : 'none';
        }
    }

    // ── SET VISIBILITY ─────────────────────────────────────────────────────────
    setVisible(id, visible) {
        if (!this.settings[id]) this.settings[id] = { ...this.defaultSettings[id] || { visible: true, opacity: 1 } };
        this.settings[id].visible = visible;
        this.apply();
        this.save();
    }

    // ── SET OPACITY ────────────────────────────────────────────────────────────
    setOpacity(id, opacity) {
        if (!this.settings[id]) this.settings[id] = { ...this.defaultSettings[id] || { visible: true, opacity: 1 } };
        this.settings[id].opacity = Math.max(0, Math.min(1, opacity));
        this.apply();
        this.save();
    }

    // ── RESET SINGLE BUTTON ────────────────────────────────────────────────────
    resetButton(id) {
        if (this.defaultSettings[id]) {
            this.settings[id] = { ...this.defaultSettings[id] };
            this.apply();
            this.save();
        }
    }

    // ── RESET ALL TO DEFAULTS ──────────────────────────────────────────────────
    resetAll() {
        this.settings = JSON.parse(JSON.stringify(this.defaultSettings));
        this.apply();
        this.save();
    }

    // ── OPEN HUD CUSTOMIZER MODAL ──────────────────────────────────────────────
    openModal() {
        const existing = document.getElementById('hudCustomizerModal');
        if (existing) {
            existing.style.display = 'flex';
            this._refreshModal();
            return;
        }

        const buttonDefs = [
            { id: 'btnFire',       label: '🎯 Fire Button' },
            { id: 'btnJump',       label: '⬆️ Jump Button' },
            { id: 'btnCrouch',     label: '🦵 Crouch Button' },
            { id: 'btnProne',      label: '🛌 Prone Button' },
            { id: 'btnSprint',     label: '🏃 Sprint Button' },
            { id: 'btnSlide',      label: '🌊 Slide Button' },
            { id: 'btnScope',      label: '🔭 Scope/ADS Button' },
            { id: 'btnReload',     label: '🔄 Reload Button' },
            { id: 'btnGrenade',    label: '💣 Grenade Button' },
            { id: 'btnMedkit',     label: '💊 Medkit Button' },
            { id: 'btnGlooWall',   label: '🧱 Gloo Wall Button' },
            { id: 'btnEmoteWheel', label: '😂 Emote Button' },
            { id: 'btnShop',       label: '🛒 Shop Button' },
            { id: 'btnSettings',   label: '⚙️ Settings Button' },
            { id: 'joystickZone',  label: '🕹️ Joystick' },
        ];

        const modal = document.createElement('div');
        modal.id = 'hudCustomizerModal';
        modal.style.cssText = `
            position:fixed; inset:0; background:rgba(5,10,20,0.95);
            z-index:9999; display:flex; flex-direction:column;
            align-items:center; overflow-y:auto; padding:16px;
            font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;
        `;

        const header = document.createElement('div');
        header.style.cssText = `
            width:100%; max-width:520px; display:flex; justify-content:space-between;
            align-items:center; margin-bottom:16px; position:sticky; top:0;
            background:rgba(5,10,20,0.98); padding:8px 0; z-index:1;
        `;
        header.innerHTML = `
            <span style="color:#FACC15;font-size:18px;font-weight:900;letter-spacing:1px;">
                🎮 HUD CUSTOMIZATION
            </span>
            <div style="display:flex;gap:8px;">
                <button id="hudResetAllBtn" style="
                    background:rgba(239,68,68,0.15);border:1.5px solid #EF4444;
                    color:#FCA5A5;padding:6px 14px;border-radius:12px;cursor:pointer;
                    font-size:12px;font-weight:700;">↺ Reset All</button>
                <button id="hudCloseBtn" style="
                    background:rgba(100,116,139,0.2);border:1.5px solid #64748B;
                    color:#94A3B8;padding:6px 14px;border-radius:12px;cursor:pointer;
                    font-size:12px;font-weight:700;">✕ Close</button>
            </div>
        `;
        modal.appendChild(header);

        // Info tip
        const tip = document.createElement('div');
        tip.style.cssText = `
            background:rgba(14,165,233,0.12);border:1px solid rgba(14,165,233,0.3);
            border-radius:12px;padding:10px 16px;color:#7DD3FC;font-size:12px;
            max-width:520px;width:100%;margin-bottom:12px;text-align:center;
        `;
        tip.textContent = 'Adjust opacity to make buttons translucent, or hide buttons you don\'t need. Changes are saved automatically.';
        modal.appendChild(tip);

        // Build button rows
        const grid = document.createElement('div');
        grid.id = 'hudCustomizerGrid';
        grid.style.cssText = `
            width:100%;max-width:520px;display:flex;flex-direction:column;gap:8px;
        `;

        for (const def of buttonDefs) {
            const cfg = this.settings[def.id] || this.defaultSettings[def.id] || { visible: true, opacity: 1 };
            const row = document.createElement('div');
            row.setAttribute('data-btn-id', def.id);
            row.style.cssText = `
                background:rgba(15,23,42,0.85);border:1.5px solid rgba(255,255,255,0.1);
                border-radius:14px;padding:12px 16px;display:flex;align-items:center;gap:12px;
            `;
            row.innerHTML = `
                <span style="color:#E2E8F0;font-size:13px;font-weight:600;flex:1;">${def.label}</span>
                <div style="display:flex;align-items:center;gap:10px;">
                    <label style="display:flex;align-items:center;gap:5px;cursor:pointer;">
                        <input type="checkbox" class="hud-vis-check" data-id="${def.id}"
                            ${cfg.visible ? 'checked' : ''}
                            style="width:16px;height:16px;accent-color:#22C55E;cursor:pointer;">
                        <span style="color:#94A3B8;font-size:11px;">Show</span>
                    </label>
                    <div style="display:flex;align-items:center;gap:6px;">
                        <span style="color:#94A3B8;font-size:11px;">Opacity</span>
                        <input type="range" class="hud-opacity-slider" data-id="${def.id}"
                            min="0.1" max="1.0" step="0.05" value="${cfg.opacity}"
                            style="width:80px;accent-color:#38BDF8;cursor:pointer;">
                        <span class="hud-opacity-val" style="color:#38BDF8;font-size:11px;width:28px;text-align:right;">
                            ${Math.round(cfg.opacity * 100)}%
                        </span>
                    </div>
                    <button class="hud-reset-btn" data-id="${def.id}" style="
                        background:transparent;border:1px solid rgba(100,116,139,0.4);
                        color:#64748B;padding:4px 8px;border-radius:8px;cursor:pointer;
                        font-size:10px;">↺</button>
                </div>
            `;
            grid.appendChild(row);
        }

        modal.appendChild(grid);
        document.body.appendChild(modal);

        // Bind events
        document.getElementById('hudCloseBtn').addEventListener('click', () => {
            modal.style.display = 'none';
        });
        document.getElementById('hudResetAllBtn').addEventListener('click', () => {
            this.resetAll();
            this._refreshModal();
        });

        modal.addEventListener('change', (e) => {
            const id = e.target.getAttribute('data-id');
            if (!id) return;
            if (e.target.classList.contains('hud-vis-check')) {
                this.setVisible(id, e.target.checked);
            }
            if (e.target.classList.contains('hud-opacity-slider')) {
                const val = parseFloat(e.target.value);
                this.setOpacity(id, val);
                // Update label
                const row = e.target.closest('[data-btn-id]');
                if (row) {
                    const lbl = row.querySelector('.hud-opacity-val');
                    if (lbl) lbl.textContent = Math.round(val * 100) + '%';
                }
            }
        });

        modal.addEventListener('click', (e) => {
            if (e.target.classList.contains('hud-reset-btn')) {
                const id = e.target.getAttribute('data-id');
                if (id) {
                    this.resetButton(id);
                    this._refreshModal();
                }
            }
        });
    }

    _refreshModal() {
        const grid = document.getElementById('hudCustomizerGrid');
        if (!grid) return;
        grid.querySelectorAll('[data-btn-id]').forEach(row => {
            const id = row.getAttribute('data-btn-id');
            const cfg = this.settings[id] || { visible: true, opacity: 1 };
            const check = row.querySelector('.hud-vis-check');
            const slider = row.querySelector('.hud-opacity-slider');
            const lbl = row.querySelector('.hud-opacity-val');
            if (check) check.checked = cfg.visible;
            if (slider) slider.value = cfg.opacity;
            if (lbl) lbl.textContent = Math.round(cfg.opacity * 100) + '%';
        });
    }

    closeModal() {
        const modal = document.getElementById('hudCustomizerModal');
        if (modal) modal.style.display = 'none';
    }
}

window.hudCustomizer = new HudCustomizer();
