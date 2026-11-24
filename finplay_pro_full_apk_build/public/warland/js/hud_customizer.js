// War Land 3D - HUD Customization & Visibility Settings System v3.0
// Per-player persistence, live real-time opacity preview, quick presets,
// per-button hide/show & reset, and lockout-safe Settings button protection.

class HudCustomizer {
    constructor() {
        // Default settings for every interactive HUD element
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
            joystickZone:   { visible: true, opacity: 1.0 },
            weaponStrip:    { visible: true, opacity: 1.0 },
            minimapWrapper: { visible: true, opacity: 1.0 },
            btnSettings:    { visible: true, opacity: 1.0 }
        };

        // Passive combat HUD containers that also respect Master HUD Opacity
        this.passiveHudIds = ['healthArmorContainer', 'ammoBadge', 'killFeed'];

        this.masterOpacity = this.loadMasterOpacity();
        this.settings = this.load();
        if (document.readyState === 'loading') {
            window.addEventListener('DOMContentLoaded', () => this.apply());
        } else {
            this.apply();
        }
    }

    _getMasterOpacityStorageKey() {
        return `${this._getStorageKey()}_master_opacity`;
    }

    loadMasterOpacity() {
        try {
            const raw = localStorage.getItem(this._getMasterOpacityStorageKey())
                || localStorage.getItem('warland_hud_master_opacity');
            if (raw !== null) {
                const parsed = Number(raw);
                if (Number.isFinite(parsed)) {
                    return Math.max(0.15, Math.min(1.0, parsed));
                }
            }
        } catch (e) {}
        return 1.0;
    }

    saveMasterOpacity() {
        try {
            const val = String(this.masterOpacity);
            localStorage.setItem(this._getMasterOpacityStorageKey(), val);
            localStorage.setItem('warland_hud_master_opacity', val);
        } catch (e) {}
    }

    getMasterOpacity() {
        return this.masterOpacity;
    }

    setMasterOpacity(opacity) {
        const clamped = Math.max(0.15, Math.min(1.0, Number(opacity) || 1.0));
        this.masterOpacity = clamped;
        this.apply();
        this.saveMasterOpacity();
    }

    _getStorageKey() {
        const ign = (window.playerIgn || localStorage.getItem('warland_player_ign') || 'default')
            .toString()
            .trim()
            .toLowerCase()
            .replace(/[^a-z0-9_-]/g, '_');
        return `warland_hud_settings_${ign || 'default'}`;
    }

    // ── LOAD FROM localStorage (per-player with legacy fallback) ───────────────
    load() {
        const merged = JSON.parse(JSON.stringify(this.defaultSettings));
        try {
            const raw = localStorage.getItem(this._getStorageKey()) || localStorage.getItem('warland_hud_settings');
            if (raw) {
                const parsed = JSON.parse(raw);
                for (const key of Object.keys(merged)) {
                    if (parsed[key] && typeof parsed[key] === 'object') {
                        merged[key].visible = parsed[key].visible !== false;
                        const op = Number(parsed[key].opacity);
                        if (Number.isFinite(op)) {
                            merged[key].opacity = Math.max(0.1, Math.min(1.0, op));
                        }
                    }
                }
            }
        } catch (e) {}

        // Lockout safeguard: Settings button can NEVER be permanently hidden or invisible
        merged.btnSettings.visible = true;
        merged.btnSettings.opacity = Math.max(0.35, merged.btnSettings.opacity);
        return merged;
    }

    // ── SAVE TO localStorage ───────────────────────────────────────────────────
    save() {
        try {
            const payload = JSON.stringify(this.settings);
            localStorage.setItem(this._getStorageKey(), payload);
            localStorage.setItem('warland_hud_settings', payload);
        } catch (e) {}
    }

    // ── APPLY SETTINGS TO DOM ──────────────────────────────────────────────────
    apply() {
        const master = Math.max(0.15, Math.min(1.0, this.masterOpacity || 1.0));
        for (const [id, cfg] of Object.entries(this.settings)) {
            const el = document.getElementById(id);
            if (!el) continue;

            // Always keep btnSettings accessible
            const isProtected = (id === 'btnSettings');
            const visible = isProtected ? true : cfg.visible !== false;
            const baseOpacity = isProtected ? Math.max(0.35, cfg.opacity) : Math.max(0.1, Math.min(1.0, cfg.opacity));
            const effectiveOpacity = isProtected
                ? Math.max(0.35, baseOpacity * master)
                : Math.max(0.12, Math.min(1.0, baseOpacity * master));

            el.style.opacity = String(effectiveOpacity);
            el.style.display = visible ? '' : 'none';
            el.style.visibility = visible ? '' : 'hidden';
            el.style.pointerEvents = visible ? '' : 'none';
        }

        // Apply master opacity to passive combat HUD elements & top-left stats strip
        for (const passiveId of this.passiveHudIds) {
            const el = document.getElementById(passiveId);
            if (el) el.style.opacity = String(master);
        }
        const topLeft = document.querySelector('#topBar .top-left-group');
        if (topLeft) topLeft.style.opacity = String(master);
    }

    // ── SET VISIBILITY ─────────────────────────────────────────────────────────
    setVisible(id, visible) {
        if (id === 'btnSettings') return; // Prevent lockout
        if (!this.settings[id]) this.settings[id] = { ...(this.defaultSettings[id] || { visible: true, opacity: 1 }) };
        this.settings[id].visible = !!visible;
        this.apply();
        this.save();
    }

    // ── SET OPACITY ────────────────────────────────────────────────────────────
    setOpacity(id, opacity) {
        if (!this.settings[id]) this.settings[id] = { ...(this.defaultSettings[id] || { visible: true, opacity: 1 }) };
        const minOp = (id === 'btnSettings') ? 0.35 : 0.1;
        this.settings[id].opacity = Math.max(minOp, Math.min(1.0, opacity));
        this.apply();
        this.save();
    }

    // ── APPLY QUICK PRESETS ────────────────────────────────────────────────────
    applyPreset(presetName) {
        if (presetName === 'default') {
            this.resetAll();
            return;
        }
        const next = JSON.parse(JSON.stringify(this.defaultSettings));
        if (presetName === 'pro') {
            // Pro Translucent: 65% opacity across combat HUD for maximum visibility
            for (const key of Object.keys(next)) {
                next[key].visible = true;
                next[key].opacity = (key === 'btnFire' || key === 'joystickZone') ? 0.75 : 0.65;
            }
        } else if (presetName === 'minimal') {
            // Minimal HUD: Hides non-essential buttons, sets crisp 55% opacity
            for (const key of Object.keys(next)) {
                next[key].opacity = (key === 'btnFire') ? 0.8 : 0.55;
            }
            next.btnEmoteWheel.visible = false;
            next.btnProne.visible = false;
        }
        next.btnSettings.visible = true;
        next.btnSettings.opacity = Math.max(0.45, next.btnSettings.opacity);
        this.settings = next;
        this.apply();
        this.save();
        this._refreshModal();
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
        this.masterOpacity = 1.0;
        this.settings = JSON.parse(JSON.stringify(this.defaultSettings));
        this.apply();
        this.save();
        this.saveMasterOpacity();
        const masterSlider = document.getElementById('hudMasterOpacitySlider');
        const masterVal = document.getElementById('hudMasterOpacityVal');
        if (masterSlider) masterSlider.value = '100';
        if (masterVal) masterVal.textContent = '100%';
    }

    // ── OPEN HUD CUSTOMIZER MODAL ──────────────────────────────────────────────
    openModal() {
        // Close parent settings modal so live HUD changes are visible behind translucent backdrop
        document.getElementById('settingsModal')?.classList.add('hidden');

        const existing = document.getElementById('hudCustomizerModal');
        if (existing) {
            existing.style.display = 'flex';
            this._refreshModal();
            return;
        }

        const buttonDefs = [
            { id: 'btnFire',        label: '🎯 Fire Button' },
            { id: 'btnSprint',      label: '🏃 Run / Sprint Button' },
            { id: 'btnSlide',       label: '🌊 Slide Button' },
            { id: 'btnJump',        label: '🦘 Jump Button' },
            { id: 'btnCrouch',      label: '🧎 Crouch Button' },
            { id: 'btnProne',       label: '🛌 Prone Button' },
            { id: 'btnScope',       label: '🔭 Scope / ADS Button' },
            { id: 'btnReload',      label: '🔄 Reload Button' },
            { id: 'btnGrenade',     label: '💣 Grenade Button' },
            { id: 'btnGlooWall',    label: '🛡️ Gloo Wall Button' },
            { id: 'btnMedkit',      label: '➕ Medkit Button' },
            { id: 'btnEmoteWheel',  label: '🎭 Emote Button' },
            { id: 'joystickZone',   label: '🕹️ Virtual Joystick' },
            { id: 'weaponStrip',    label: '🔫 Weapon Slots Strip' },
            { id: 'minimapWrapper', label: '🗺️ Radar Minimap' },
            { id: 'btnSettings',    label: '⚙️ Settings Button (Always Visible)', protected: true }
        ];

        const modal = document.createElement('div');
        modal.id = 'hudCustomizerModal';
        modal.style.cssText = `
            position:fixed; inset:0; background:rgba(5,10,20,0.72);
            backdrop-filter:blur(4px);
            z-index:9999; display:flex; flex-direction:column;
            align-items:center; overflow-y:auto; padding:16px;
            font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;
        `;

        const header = document.createElement('div');
        header.style.cssText = `
            width:100%; max-width:540px; display:flex; justify-content:space-between;
            align-items:center; margin-bottom:10px; position:sticky; top:0;
            background:rgba(5,10,20,0.92); padding:10px 12px; border-radius:14px;
            border:1px solid rgba(250,204,21,0.35); z-index:2;
        `;
        header.innerHTML = `
            <span style="color:#FACC15;font-size:16px;font-weight:900;letter-spacing:0.8px;">
                🎮 CUSTOM HUD SETTINGS
            </span>
            <div style="display:flex;gap:8px;">
                <button id="hudResetAllBtn" style="
                    background:rgba(239,68,68,0.18);border:1.5px solid #EF4444;
                    color:#FCA5A5;padding:6px 12px;border-radius:10px;cursor:pointer;
                    font-size:12px;font-weight:700;">↺ Reset All</button>
                <button id="hudCloseBtn" style="
                    background:#F59E0B;border:none;
                    color:#0F172A;padding:6px 14px;border-radius:10px;cursor:pointer;
                    font-size:12px;font-weight:800;">✓ Done</button>
            </div>
        `;
        modal.appendChild(header);

        // Quick Presets Bar
        const presetBar = document.createElement('div');
        presetBar.style.cssText = `
            width:100%; max-width:540px; display:flex; gap:8px; margin-bottom:10px;
        `;
        presetBar.innerHTML = `
            <button class="hud-preset-btn" data-preset="default" style="
                flex:1;background:rgba(30,41,59,0.9);border:1.5px solid rgba(148,163,184,0.4);
                color:#E2E8F0;padding:8px;border-radius:10px;cursor:pointer;font-size:12px;font-weight:700;">
                ✨ Default (100%)</button>
            <button class="hud-preset-btn" data-preset="pro" style="
                flex:1;background:rgba(14,165,233,0.18);border:1.5px solid #38BDF8;
                color:#BAE6FD;padding:8px;border-radius:10px;cursor:pointer;font-size:12px;font-weight:700;">
                🎯 Pro Translucent (65%)</button>
            <button class="hud-preset-btn" data-preset="minimal" style="
                flex:1;background:rgba(16,185,129,0.18);border:1.5px solid #10B981;
                color:#A7F3D0;padding:8px;border-radius:10px;cursor:pointer;font-size:12px;font-weight:700;">
                🧹 Clean Minimal</button>
        `;
        modal.appendChild(presetBar);

        const tip = document.createElement('div');
        tip.style.cssText = `
            background:rgba(14,165,233,0.14);border:1px solid rgba(14,165,233,0.35);
            border-radius:10px;padding:8px 14px;color:#7DD3FC;font-size:11.5px;
            max-width:540px;width:100%;margin-bottom:10px;text-align:center;
        `;
        tip.textContent = 'Drag sliders for instant real-time transparency preview or toggle Show/Hide per control. Saved automatically.';
        modal.appendChild(tip);

        // Build button rows
        const grid = document.createElement('div');
        grid.id = 'hudCustomizerGrid';
        grid.style.cssText = `
            width:100%;max-width:540px;display:flex;flex-direction:column;gap:8px;padding-bottom:24px;
        `;

        for (const def of buttonDefs) {
            const cfg = this.settings[def.id] || this.defaultSettings[def.id] || { visible: true, opacity: 1 };
            const minOp = def.protected ? 0.35 : 0.1;
            const row = document.createElement('div');
            row.setAttribute('data-btn-id', def.id);
            row.style.cssText = `
                background:rgba(15,23,42,0.88);border:1.5px solid rgba(255,255,255,0.12);
                border-radius:12px;padding:10px 14px;display:flex;align-items:center;gap:10px;flex-wrap:wrap;
            `;
            row.innerHTML = `
                <span style="color:#E2E8F0;font-size:13px;font-weight:700;flex:1;min-width:140px;">${def.label}</span>
                <div style="display:flex;align-items:center;gap:10px;">
                    <label style="display:flex;align-items:center;gap:5px;cursor:${def.protected ? 'not-allowed' : 'pointer'};opacity:${def.protected ? '0.6' : '1'};">
                        <input type="checkbox" class="hud-vis-check" data-id="${def.id}"
                            ${cfg.visible ? 'checked' : ''} ${def.protected ? 'disabled' : ''}
                            style="width:16px;height:16px;accent-color:#22C55E;cursor:${def.protected ? 'not-allowed' : 'pointer'};">
                        <span style="color:#94A3B8;font-size:11px;">Show</span>
                    </label>
                    <div style="display:flex;align-items:center;gap:6px;">
                        <span style="color:#94A3B8;font-size:11px;">Opacity</span>
                        <input type="range" class="hud-opacity-slider" data-id="${def.id}"
                            min="${minOp}" max="1.0" step="0.05" value="${cfg.opacity}"
                            style="width:86px;accent-color:#38BDF8;cursor:pointer;">
                        <span class="hud-opacity-val" style="color:#38BDF8;font-size:11px;font-weight:700;width:32px;text-align:right;">
                            ${Math.round(cfg.opacity * 100)}%
                        </span>
                    </div>
                    <button class="hud-reset-btn" data-id="${def.id}" title="Reset this button" style="
                        background:transparent;border:1px solid rgba(100,116,139,0.45);
                        color:#94A3B8;padding:4px 8px;border-radius:8px;cursor:pointer;
                        font-size:11px;font-weight:700;">↺</button>
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

        // Live real-time opacity preview on slider drag!
        modal.addEventListener('input', (e) => {
            if (e.target.classList.contains('hud-opacity-slider')) {
                const id = e.target.getAttribute('data-id');
                if (!id) return;
                const val = parseFloat(e.target.value);
                this.setOpacity(id, val);
                const row = e.target.closest('[data-btn-id]');
                const lbl = row?.querySelector('.hud-opacity-val');
                if (lbl) lbl.textContent = Math.round(val * 100) + '%';
            }
        });

        modal.addEventListener('change', (e) => {
            const id = e.target.getAttribute('data-id');
            if (!id) return;
            if (e.target.classList.contains('hud-vis-check')) {
                this.setVisible(id, e.target.checked);
            }
        });

        modal.addEventListener('click', (e) => {
            if (e.target.classList.contains('hud-reset-btn')) {
                const id = e.target.getAttribute('data-id');
                if (id) {
                    this.resetButton(id);
                    this._refreshModal();
                }
            } else if (e.target.classList.contains('hud-preset-btn')) {
                const preset = e.target.getAttribute('data-preset');
                if (preset) this.applyPreset(preset);
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
