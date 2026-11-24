// War Land 3D - Free Fire Style Custom HUD, Touch Virtual Joystick & Settings

class WarLandHud {
    constructor() {
        this.inputVector = new THREE.Vector2(); // x: -1..1, y: -1..1
        this.isFiring = false;
        this.isAiming = false;
        this.cameraTouchId = null;
        this.lastTouchX = 0;
        this.lastTouchY = 0;

        this.touchSensitivity = 0.0035;
        this.aliveCount = 26;
        this.killCount = 0;
        this.isCustomizingHud = false;
        this.keyboardSprintActive = false;

        this.initDomElements();
        this.bindTouchEvents();
        this.bindMouseControls();
        this.bindKeyboardFallback();
    }

    // Zero-lag tap binding for mobile + desktop: prevents touchstart+click double-fire AND stops cameraZone bleed
    _bindTapButton(id, handler) {
        const el = document.getElementById(id);
        if (!el) return null;
        let lastTouchTime = 0;

        el.addEventListener('touchstart', (e) => {
            e.preventDefault();
            e.stopPropagation();
            lastTouchTime = performance.now();
            handler(e, el);
        }, { passive: false });

        el.addEventListener('click', (e) => {
            if (performance.now() - lastTouchTime < 450) return;
            e.stopPropagation();
            handler(e, el);
        });
        return el;
    }

    initDomElements() {
        this.hpFill = document.getElementById('hpFill');
        this.hpText = document.getElementById('hpText');
        this.armorFill = document.getElementById('armorFill');
        this.armorText = document.getElementById('armorText');
        this.aliveText = document.getElementById('aliveText');
        this.killText = document.getElementById('killText');
        this.ammoInMagText = document.getElementById('ammoInMag');
        this.ammoReserveText = document.getElementById('ammoReserve');
        this.weaponNameText = document.getElementById('weaponName');
        this.scopeOverlay = document.getElementById('scopeOverlay');
        this.killFeedContainer = document.getElementById('killFeed');
        this.emoteWheel = document.getElementById('emoteWheel');
        this.settingsModal = document.getElementById('settingsModal');
        this.spectateBanner = document.getElementById('spectateBanner');
        this.matchOverModal = document.getElementById('matchOverModal');

        // Joystick elements
        this.joystickZone = document.getElementById('joystickZone');
        this.joystickThumb = document.getElementById('joystickThumb');
        this.joystickBase = document.getElementById('joystickBase');
    }

    bindTouchEvents() {
        let joystickTouchId = null;
        this.isJoystickDragging = false;

        const updateJoystickFromPointer = (clientX, clientY) => {
            const rect = this.joystickZone.getBoundingClientRect();
            const centerX = rect.left + rect.width * 0.5;
            const centerY = rect.top + rect.height * 0.5;

            const dx = clientX - centerX;
            const dy = clientY - centerY;
            const dist = Math.hypot(dx, dy);
            const maxRadius = 45;

            const clampedDist = Math.min(dist, maxRadius);
            const angle = Math.atan2(dy, dx);

            const clampedX = Math.cos(angle) * clampedDist;
            const clampedY = Math.sin(angle) * clampedDist;

            this.joystickThumb.style.transform = `translate(${clampedX}px, ${clampedY}px)`;
            this.inputVector.x = clampedX / maxRadius;
            this.inputVector.y = -clampedY / maxRadius; // Forward is negative Y in screen coordinates
        };

        const resetJoystick = () => {
            this.isJoystickDragging = false;
            joystickTouchId = null;
            this.inputVector.set(0, 0);
            this.joystickThumb.style.transform = 'translate(0px, 0px)';
        };

        // 1. Mobile Touch Joystick
        this.joystickZone.addEventListener('touchstart', (e) => {
            e.preventDefault();
            const touch = e.changedTouches[0];
            joystickTouchId = touch.identifier;
            this.isJoystickDragging = true;
            updateJoystickFromPointer(touch.clientX, touch.clientY);
        }, { passive: false });

        window.addEventListener('touchmove', (e) => {
            for (let i = 0; i < e.changedTouches.length; i++) {
                const touch = e.changedTouches[i];
                if (touch.identifier === joystickTouchId) {
                    updateJoystickFromPointer(touch.clientX, touch.clientY);
                } else if (touch.identifier === this.cameraTouchId && window.player) {
                    const dx = touch.clientX - this.lastTouchX;
                    const dy = touch.clientY - this.lastTouchY;
                    this.lastTouchX = touch.clientX;
                    this.lastTouchY = touch.clientY;

                    // Rotate Player Horizontally and Camera Vertically
                    window.player.rotationY -= dx * this.touchSensitivity;
                    window.player.pitch = Math.max(-0.45, Math.min(0.85, window.player.pitch + dy * this.touchSensitivity));
                }
            }
        }, { passive: false });

        const endJoystickTouch = (e) => {
            for (let i = 0; i < e.changedTouches.length; i++) {
                if (e.changedTouches[i].identifier === joystickTouchId) {
                    resetJoystick();
                }
                if (e.changedTouches[i].identifier === this.cameraTouchId) {
                    this.cameraTouchId = null;
                }
            }
        };
        window.addEventListener('touchend', endJoystickTouch);
        window.addEventListener('touchcancel', endJoystickTouch);

        // Desktop Mouse Drag Support for Virtual Joystick
        this.joystickZone.addEventListener('mousedown', (e) => {
            e.preventDefault();
            this.isJoystickDragging = true;
            updateJoystickFromPointer(e.clientX, e.clientY);
        });

        window.addEventListener('mousemove', (e) => {
            if (this.isJoystickDragging && joystickTouchId === null) {
                updateJoystickFromPointer(e.clientX, e.clientY);
            }
        });

        window.addEventListener('mouseup', () => {
            if (this.isJoystickDragging && joystickTouchId === null) {
                resetJoystick();
            }
        });

        // 2. Camera Touch Pan Zone (Right Side Screen Pan)
        const cameraTouchZone = document.getElementById('cameraZone');
        if (cameraTouchZone) {
            cameraTouchZone.addEventListener('touchstart', (e) => {
                const touch = e.changedTouches[0];
                this.cameraTouchId = touch.identifier;
                this.lastTouchX = touch.clientX;
                this.lastTouchY = touch.clientY;
            });
        }

        // 3. Fire Button (touchstart + stopPropagation so firing never rotates camera accidentally)
        const fireBtn = document.getElementById('btnFire');
        if (fireBtn) {
            fireBtn.addEventListener('touchstart', (e) => { e.preventDefault(); e.stopPropagation(); this.isFiring = true; }, { passive: false });
            fireBtn.addEventListener('touchend', (e) => { e.preventDefault(); e.stopPropagation(); this.isFiring = false; }, { passive: false });
            fireBtn.addEventListener('touchcancel', () => { this.isFiring = false; });
            fireBtn.addEventListener('mousedown', (e) => { e.stopPropagation(); this.isFiring = true; });
            window.addEventListener('mouseup', () => { this.isFiring = false; });
        }

        // 4. Action Mobility & Combat Buttons (instant touch response, zero double-fire, zero camera bleed)
        this._bindTapButton('btnJump', () => {
            if (window.player) window.player.jump();
        });

        this._bindTapButton('btnCrouch', () => {
            if (!window.player) return;
            window.player.posture = (window.player.posture === 'CROUCH') ? 'STAND' : 'CROUCH';
            document.getElementById('btnCrouch')?.classList.toggle('active', window.player.posture === 'CROUCH');
            document.getElementById('btnProne')?.classList.remove('active');
        });

        this._bindTapButton('btnProne', () => {
            if (!window.player) return;
            window.player.posture = (window.player.posture === 'PRONE') ? 'STAND' : 'PRONE';
            document.getElementById('btnProne')?.classList.toggle('active', window.player.posture === 'PRONE');
            document.getElementById('btnCrouch')?.classList.remove('active');
        });

        // Dedicated Run / Sprint Toggle Button
        this._bindTapButton('btnSprint', (_e, sprintBtn) => {
            if (!window.player) return;
            window.player.isSprinting = !window.player.isSprinting;
            if (window.player.isSprinting && window.player.posture !== 'STAND') {
                window.player.posture = 'STAND';
                document.getElementById('btnCrouch')?.classList.remove('active');
                document.getElementById('btnProne')?.classList.remove('active');
            }
            sprintBtn.classList.toggle('active', window.player.isSprinting);
        });

        // Dedicated Tactical Slide Button
        this._bindTapButton('btnSlide', () => {
            if (window.player) window.player.startSlide();
        });

        this._bindTapButton('btnScope', () => {
            this.toggleScope();
        });

        this._bindTapButton('btnReload', () => {
            if (window.inventory) window.inventory.startReload();
        });

        this._bindTapButton('btnGrenade', () => {
            if (window.gameEngine) window.gameEngine.throwGrenade();
        });

        this._bindTapButton('btnMedkit', () => {
            if (window.player) window.player.healMedkit();
        });

        // 5. Emote Wheel Triggers
        document.getElementById('btnEmoteWheel')?.addEventListener('click', () => {
            this.emoteWheel.classList.toggle('hidden');
        });

        document.querySelectorAll('.emote-option').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const emote = btn.getAttribute('data-emote');
                if (window.player) window.player.triggerEmote(emote);
                this.emoteWheel.classList.add('hidden');
            });
        });

        // 6. Weapon Slots (0: Fists, 1: Primary 1, 2: Primary 2, 3: Pistol)
        for (let i = 0; i <= 3; i++) {
            document.getElementById(`slot${i}`)?.addEventListener('click', () => {
                if (window.inventory) window.inventory.switchSlot(i);
            });
        }

        // Parachute Deploy Button Handler
        const chuteBtn = document.getElementById('btnDeployChute');
        if (chuteBtn) {
            chuteBtn.addEventListener('click', () => {
                if (window.player) window.player.deployParachute();
                chuteBtn.style.display = 'none';
            });
        }

        // 7. Settings Modal
        document.getElementById('btnSettings')?.addEventListener('click', () => {
            this.settingsModal.classList.remove('hidden');
        });
        document.getElementById('btnCloseSettings')?.addEventListener('click', () => {
            this.settingsModal.classList.add('hidden');
        });

        // 8. Game Shop Modal & Purchases
        const shopModal = document.getElementById('shopModal');
        const coinsVal = document.getElementById('shopCoinsVal');
        const updateCoinsDisplay = () => {
            let coins = parseInt(localStorage.getItem('warland_coins') || '250', 10);
            if (coinsVal) coinsVal.textContent = coins;
        };

        document.getElementById('btnShop')?.addEventListener('click', () => {
            updateCoinsDisplay();
            shopModal?.classList.remove('hidden');
        });
        document.getElementById('btnCloseShop')?.addEventListener('click', () => {
            shopModal?.classList.add('hidden');
        });

        // Shop Tabs
        document.querySelectorAll('.shop-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                document.querySelectorAll('.shop-tab').forEach(t => t.classList.remove('active'));
                tab.classList.add('active');
                const target = tab.getAttribute('data-tab');
                document.querySelectorAll('.shop-content-pane').forEach(p => p.classList.add('hidden'));
                document.getElementById(target)?.classList.remove('hidden');
            });
        });

        // Shop Item Purchases (Punch Power, Gloo Walls, Ammo, Skins)
        document.querySelectorAll('.shop-buy-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const bType = btn.getAttribute('data-type');
                const bId = btn.getAttribute('data-id');
                const bCost = parseInt(btn.getAttribute('data-cost') || '0', 10);
                let coins = parseInt(localStorage.getItem('warland_coins') || '250', 10);

                if (bType === 'punch') {
                    if (coins >= bCost) {
                        coins -= bCost;
                        localStorage.setItem('warland_coins', coins);
                        localStorage.setItem('warland_punch_power', bId);
                        if (window.inventory && window.inventory.weapons[0]) {
                            window.inventory.weapons[0].damage = window.inventory.getPunchDamage();
                        }
                        btn.textContent = 'EQUIPPED';
                        btn.classList.add('equipped');
                        updateCoinsDisplay();
                        this.updateWeaponUi();
                        if (window.soundEngine) window.soundEngine.playLootPickup();
                    } else {
                        alert('Not enough coins! Eliminate enemies or win matches to earn coins.');
                    }
                } else if (bType === 'gloo') {
                    if (coins >= bCost) {
                        coins -= bCost;
                        localStorage.setItem('warland_coins', coins);
                        if (window.gameEngine) {
                            window.gameEngine.playerGlooWallCount = Math.min(6, window.gameEngine.playerGlooWallCount + 3);
                            this.updateGlooUi(window.gameEngine.playerGlooWallCount);
                        }
                        updateCoinsDisplay();
                        if (window.soundEngine) window.soundEngine.playLootPickup();
                    }
                } else if (bType === 'ammo') {
                    if (coins >= bCost) {
                        coins -= bCost;
                        localStorage.setItem('warland_coins', coins);
                        if (window.inventory) {
                            for (let s = 1; s <= 3; s++) {
                                if (window.inventory.weapons[s]) {
                                    window.inventory.weapons[s].reserveAmmo += 60;
                                }
                            }
                            this.updateWeaponUi();
                        }
                        updateCoinsDisplay();
                        if (window.soundEngine) window.soundEngine.playLootPickup();
                    }
                } else if (bType === 'skin') {
                    window.activeSkinId = bId;
                    localStorage.setItem('warland_skin', bId);
                    btn.textContent = 'EQUIPPED';
                    btn.classList.add('equipped');
                    alert(`Operative model selected: ${bId.toUpperCase()}`);
                }
            });
        });

        document.getElementById('sensSlider')?.addEventListener('input', (e) => {
            this.touchSensitivity = 0.0015 + (e.target.value / 100) * 0.006;
        });

        document.getElementById('volSlider')?.addEventListener('input', (e) => {
            if (window.soundEngine) window.soundEngine.masterVolume = e.target.value / 100;
        });

        // 7b. Master HUD Opacity Slider inside #settingsModal
        const masterOpacitySlider = document.getElementById('hudMasterOpacitySlider');
        const masterOpacityVal = document.getElementById('hudMasterOpacityVal');
        const syncMasterOpacityUi = (pct) => {
            const clampedPct = Math.max(15, Math.min(100, Math.round(pct)));
            if (masterOpacitySlider) masterOpacitySlider.value = String(clampedPct);
            if (masterOpacityVal) masterOpacityVal.textContent = `${clampedPct}%`;
        };
        if (window.hudCustomizer && typeof window.hudCustomizer.getMasterOpacity === 'function') {
            syncMasterOpacityUi(window.hudCustomizer.getMasterOpacity() * 100);
        }
        masterOpacitySlider?.addEventListener('input', (e) => {
            const pct = Number(e.target.value);
            syncMasterOpacityUi(pct);
            if (window.hudCustomizer && typeof window.hudCustomizer.setMasterOpacity === 'function') {
                window.hudCustomizer.setMasterOpacity(pct / 100);
            }
        });

        // 7c. Minimap Visibility Toggle Button on top of #minimapWrapper
        const minimapWrapper = document.getElementById('minimapWrapper');
        const btnToggleMinimap = document.getElementById('btnToggleMinimap');
        const applyMinimapCollapsed = (collapsed) => {
            if (!minimapWrapper || !btnToggleMinimap) return;
            minimapWrapper.classList.toggle('minimap-collapsed', !!collapsed);
            btnToggleMinimap.textContent = collapsed ? '🗺️ Map' : '🗺️ Hide';
            btnToggleMinimap.setAttribute('aria-pressed', collapsed ? 'false' : 'true');
            try {
                localStorage.setItem('warland_minimap_collapsed', collapsed ? '1' : '0');
            } catch (err) {}
        };
        try {
            const savedCollapsed = localStorage.getItem('warland_minimap_collapsed') === '1';
            applyMinimapCollapsed(savedCollapsed);
        } catch (err) {}
        this._bindTapButton('btnToggleMinimap', () => {
            const isCurrentlyCollapsed = minimapWrapper?.classList.contains('minimap-collapsed');
            applyMinimapCollapsed(!isCurrentlyCollapsed);
        });

        // 8. Custom HUD Toggle
        document.getElementById('btnCustomizeHud')?.addEventListener('click', () => {
            if (window.hudCustomizer) {
                window.hudCustomizer.openModal();
            } else {
                this.toggleCustomizeHud();
            }
        });

        // 9. Free Fire Gloo Wall Deploy Button
        this._bindTapButton('btnGlooWall', () => {
            if (window.gameEngine) window.gameEngine.deployGlooWall();
        });
    }

    bindMouseControls() {
        const canvas = document.getElementById('gameCanvas');
        if (!canvas) return;

        let isMouseDragging = false;
        let lastMouseX = 0;
        let lastMouseY = 0;
        let mouseStartX = 0;
        let mouseStartY = 0;

        // Pointer Lock & Drag Look initiation
        window.addEventListener('mousedown', (e) => {
            // Ignore if clicking UI buttons, modals, joystick, or top bar
            if (e.target.closest('button, .hud-action-btn, #joystickZone, .game-modal, #topBar, .inventory-slot, .emote-option')) return;

            isMouseDragging = true;
            lastMouseX = e.clientX;
            lastMouseY = e.clientY;
            mouseStartX = e.clientX;
            mouseStartY = e.clientY;

            if (e.button === 0) {
                // If pointer lock is already active, left click fires
                if (document.pointerLockElement) {
                    this.isFiring = true;
                }
            } else if (e.button === 2) {
                e.preventDefault();
                this.toggleScope();
            }
        });

        // Mouse movement (works BOTH in Pointer Lock AND normal mouse drag!)
        window.addEventListener('mousemove', (e) => {
            if (!window.player) return;

            if (document.pointerLockElement) {
                // 1. Pointer Lock Mouse Look (Desktop full-screen 360 view)
                const sens = 0.0024 * (this.touchSensitivity / 0.0035);
                window.player.rotationY -= e.movementX * sens;
                window.player.pitch = Math.max(-0.65, Math.min(0.85, window.player.pitch + e.movementY * sens));
            } else if (isMouseDragging) {
                // 2. Direct Mouse Drag Look (works smoothly without needing Pointer Lock!)
                const dx = e.clientX - lastMouseX;
                const dy = e.clientY - lastMouseY;
                lastMouseX = e.clientX;
                lastMouseY = e.clientY;

                const sens = 0.0042 * (this.touchSensitivity / 0.0035);
                window.player.rotationY -= dx * sens;
                window.player.pitch = Math.max(-0.65, Math.min(0.85, window.player.pitch + dy * sens));
            }
        });

        window.addEventListener('mouseup', (e) => {
            if (e.button === 0) {
                this.isFiring = false;
            }

            if (isMouseDragging) {
                const distMoved = Math.hypot(e.clientX - mouseStartX, e.clientY - mouseStartY);
                isMouseDragging = false;

                // If user did a quick click without dragging across the screen
                if (distMoved < 6 && e.button === 0) {
                    if (!document.pointerLockElement) {
                        canvas.requestPointerLock().catch(() => {});
                    }
                    if (window.gameEngine) {
                        window.gameEngine.fireActiveWeapon();
                    }
                }
            }
        });

        window.addEventListener('contextmenu', (e) => {
            e.preventDefault();
        });
    }

    updateGlooUi(count) {
        const badge = document.getElementById('glooCountBadge');
        if (badge) badge.innerText = count;
    }

    bindKeyboardFallback() {
        const keys = {};
        window.addEventListener('keydown', (e) => {
            keys[e.key.toLowerCase()] = true;
            if (e.key === ' ' && window.player) window.player.jump();
            if (e.key.toLowerCase() === 'c' && window.player) {
                window.player.posture = (window.player.posture === 'CROUCH') ? 'STAND' : 'CROUCH';
            }
            if (e.key.toLowerCase() === 'z' && window.player) {
                window.player.posture = (window.player.posture === 'PRONE') ? 'STAND' : 'PRONE';
            }
            // Slide on Left Ctrl or X key
            if ((e.key === 'Control' || e.key.toLowerCase() === 'x') && window.player) {
                window.player.startSlide();
            }
            if (e.key.toLowerCase() === 'r' && window.inventory) window.inventory.startReload();
            if (e.key.toLowerCase() === 'g' && window.gameEngine) window.gameEngine.deployGlooWall();
            if (e.key.toLowerCase() === 't' && window.gameEngine) window.gameEngine.throwGrenade();
            if (e.key.toLowerCase() === 'f' && window.gameEngine) window.gameEngine.scavengeNearbyLoot();
            if (e.key.toLowerCase() === 'h' && window.player) window.player.healMedkit();
            if (['0', '1', '2', '3'].includes(e.key) && window.inventory) {
                window.inventory.switchSlot(parseInt(e.key));
            }
            this.updateKeyboardInput(keys);
        });

        window.addEventListener('keyup', (e) => {
            keys[e.key.toLowerCase()] = false;
            this.updateKeyboardInput(keys);
        });
    }

    updateKeyboardInput(keys) {
        let x = 0;
        let y = 0;
        if (keys['w']) y += 1;
        if (keys['s']) y -= 1;
        if (keys['a']) x -= 1;
        if (keys['d']) x += 1;

        // Arrow keys rotate camera smoothly 360 degrees!
        if (window.player) {
            if (keys['arrowleft']) window.player.rotationY += 0.04;
            if (keys['arrowright']) window.player.rotationY -= 0.04;
            if (keys['arrowup']) window.player.pitch = Math.max(-0.65, window.player.pitch - 0.03);
            if (keys['arrowdown']) window.player.pitch = Math.min(0.85, window.player.pitch + 0.03);
        }

        if (keys['shift']) {
            this.keyboardSprintActive = true;
            if (window.player) {
                window.player.isSprinting = true;
                document.getElementById('btnSprint')?.classList.add('active');
            }
        } else if (this.keyboardSprintActive) {
            this.keyboardSprintActive = false;
            if (window.player) {
                window.player.isSprinting = false;
                document.getElementById('btnSprint')?.classList.remove('active');
            }
        }

        if (x !== 0 || y !== 0) {
            this.inputVector.set(x, y).normalize();
            if (this.joystickThumb && !this.isJoystickDragging) {
                this.joystickThumb.style.transform = `translate(${x * 32}px, ${-y * 32}px)`;
            }
        } else {
            this.inputVector.set(0, 0);
            if (this.joystickThumb && !this.isJoystickDragging) {
                this.joystickThumb.style.transform = 'translate(0px, 0px)';
            }
        }
    }

    toggleScope() {
        this.isAiming = !this.isAiming;
        const weapon = window.inventory ? window.inventory.getActiveWeapon() : null;

        if (this.isAiming && weapon && weapon.hasScope) {
            this.scopeOverlay.classList.remove('hidden');
        } else {
            this.scopeOverlay.classList.add('hidden');
        }

        document.getElementById('btnScope')?.classList.toggle('active', this.isAiming);
    }

    toggleCustomizeHud() {
        this.isCustomizingHud = !this.isCustomizingHud;
        const hudElements = document.querySelectorAll('.hud-draggable');

        hudElements.forEach(el => {
            if (this.isCustomizingHud) {
                el.classList.add('customizing-border');
                this.makeDraggable(el);
            } else {
                el.classList.remove('customizing-border');
            }
        });
    }

    makeDraggable(el) {
        let isDragging = false;
        let startX, startY, initX, initY;

        const onStart = (e) => {
            isDragging = true;
            const touch = e.touches ? e.touches[0] : e;
            startX = touch.clientX;
            startY = touch.clientY;
            initX = el.offsetLeft;
            initY = el.offsetTop;
        };

        const onMove = (e) => {
            if (!isDragging) return;
            const touch = e.touches ? e.touches[0] : e;
            const dx = touch.clientX - startX;
            const dy = touch.clientY - startY;
            el.style.left = `${initX + dx}px`;
            el.style.top = `${initY + dy}px`;
            el.style.bottom = 'auto';
            el.style.right = 'auto';
        };

        const onEnd = () => { isDragging = false; };

        el.addEventListener('mousedown', onStart);
        el.addEventListener('touchstart', onStart, { passive: true });
        window.addEventListener('mousemove', onMove);
        window.addEventListener('touchmove', onMove, { passive: true });
        window.addEventListener('mouseup', onEnd);
        window.addEventListener('touchend', onEnd);
    }

    updateHealthUi() {
        if (!window.player) return;
        const hpPercent = (window.player.hp / window.player.maxHp) * 100;
        this.hpFill.style.width = `${Math.max(0, hpPercent)}%`;
        this.hpText.textContent = `${Math.ceil(window.player.hp)} / ${window.player.maxHp}`;

        const armorPercent = (window.player.armor / window.player.maxArmor) * 100;
        this.armorFill.style.width = `${Math.max(0, armorPercent)}%`;
        this.armorText.textContent = `${Math.ceil(window.player.armor)} / ${window.player.maxArmor}`;
    }

    updateWeaponUi() {
        if (!window.inventory) return;
        const weapon = window.inventory.getActiveWeapon();
        const activeSlot = window.inventory.activeSlot;

        const fireBtn = document.getElementById('btnFire');
        if (weapon.isMelee) {
            this.weaponNameText.textContent = `🥊 ${weapon.name}`;
            this.ammoInMagText.textContent = `PWR ${weapon.damage}`;
            this.ammoReserveText.textContent = '∞';
            if (fireBtn) {
                fireBtn.innerHTML = '🥊';
                fireBtn.style.background = 'radial-gradient(circle, rgba(245,158,11,0.9) 0%, rgba(180,83,9,0.9) 100%)';
                fireBtn.style.borderColor = '#F59E0B';
            }
        } else {
            this.weaponNameText.textContent = weapon.name;
            this.ammoInMagText.textContent = weapon.ammoInMag;
            this.ammoReserveText.textContent = weapon.reserveAmmo;
            if (fireBtn) {
                fireBtn.innerHTML = '🎯';
                fireBtn.style.background = 'radial-gradient(circle, rgba(220,38,38,0.85) 0%, rgba(15,23,42,0.95) 100%)';
                fireBtn.style.borderColor = '#EF4444';
            }
        }

        // Slots: 0 (Fists), 1 (Primary 1), 2 (Primary 2), 3 (Pistol)
        const slotDefs = [
            { id: 'slot0', name: '🥊 FISTS', data: window.inventory.weapons[0] },
            { id: 'slot1', name: window.inventory.weapons[1] ? window.inventory.weapons[1].name.split(' ')[0] : 'EMPTY 1', data: window.inventory.weapons[1] },
            { id: 'slot2', name: window.inventory.weapons[2] ? window.inventory.weapons[2].name.split(' ')[0] : 'EMPTY 2', data: window.inventory.weapons[2] },
            { id: 'slot3', name: window.inventory.weapons[3] ? 'DEAGLE' : 'PISTOL', data: window.inventory.weapons[3] }
        ];

        slotDefs.forEach((slot, idx) => {
            const el = document.getElementById(slot.id);
            if (el) {
                el.textContent = slot.name;
                el.classList.toggle('active-slot', activeSlot === idx);
                el.classList.toggle('empty-slot', !slot.data);
            }
        });

        // Update Dynamic Crosshair mode
        const crosshair = document.getElementById('combatCrosshair');
        if (crosshair) {
            if (weapon.isMelee) {
                crosshair.classList.add('melee-mode');
            } else {
                crosshair.classList.remove('melee-mode');
            }
        }
        this.updateGrenadeUi();
    }

    updateGrenadeUi(count) {
        const badge = document.getElementById('grenadeCountBadge');
        if (badge) {
            const val = (count !== undefined) ? count : (window.inventory ? window.inventory.grenadeCount : 0);
            badge.textContent = val;
            badge.style.display = (val > 0) ? 'flex' : 'none';
        }
    }

    setCrosshairState(moving, firing, crouch, targetLocked) {
        const ch = document.getElementById('combatCrosshair');
        if (!ch) return;
        ch.classList.toggle('spread-moving', !!moving && !firing);
        ch.classList.toggle('spread-firing', !!firing);
        ch.classList.toggle('crouch-tight', !!crouch && !moving && !firing);
        ch.classList.toggle('target-locked', !!targetLocked);
    }

    hideParachutePrompt() {
        const hud = document.getElementById('chuteDeployHud');
        if (hud) hud.classList.add('hidden');
        const btn = document.getElementById('btnDeployChute');
        if (btn) {
            btn.style.display = 'none';
            btn.classList.add('hidden');
        }
    }

    updateAliveCount() {
        if (window.gameEngine && window.gameEngine.botManager) {
            this.aliveCount = window.gameEngine.botManager.getAliveCount() + (window.player && window.player.isAlive ? 1 : 0);
            this.aliveText.textContent = this.aliveCount;
        }
    }

    addKillFeedEntry(killer, victim) {
        const item = document.createElement('div');
        item.className = 'kill-feed-item';
        item.innerHTML = `<span class="killer">${killer}</span> <span class="skull">☠️</span> <span class="victim">${victim}</span>`;
        this.killFeedContainer.prepend(item);

        setTimeout(() => {
            item.remove();
        }, 4500);

        if (killer === 'You' || killer === (window.playerIgn || 'Player')) {
            this.killCount++;
            this.killText.textContent = this.killCount;
            if (window.soundEngine) window.soundEngine.playHitMarker(true);
        }
    }

    _spawnVictoryConfetti(container) {
        if (!container) return;
        container.innerHTML = '';
        const palette = ['#FACC15', '#F59E0B', '#34D399', '#38BDF8', '#FB7185', '#FFFFFF'];
        for (let i = 0; i < 28; i++) {
            const piece = document.createElement('span');
            piece.className = 'confetti-piece';
            piece.style.left = `${4 + Math.random() * 92}%`;
            piece.style.backgroundColor = palette[i % palette.length];
            piece.style.setProperty('--dur', `${1.9 + Math.random() * 1.4}s`);
            piece.style.setProperty('--delay', `${(Math.random() * 0.55).toFixed(2)}s`);
            piece.style.setProperty('--drift', `${Math.round((Math.random() - 0.5) * 140)}px`);
            container.appendChild(piece);
        }
    }

    _animateCoinCount(targetCoins) {
        const counterEl = document.getElementById('coinsWonCounter');
        const badgeEl = document.getElementById('coinsWonBadge');
        if (!counterEl || !badgeEl) return;

        if (this._coinAnimFrame) {
            cancelAnimationFrame(this._coinAnimFrame);
            this._coinAnimFrame = null;
        }

        const safeTarget = Math.max(0, Math.round(Number(targetCoins) || 0));
        counterEl.textContent = '0';
        badgeEl.classList.remove('coin-landed');
        badgeEl.classList.add('coin-counting');

        const durationMs = Math.min(1800, Math.max(850, safeTarget * 16));
        const startTime = performance.now();

        const step = (now) => {
            const elapsed = now - startTime;
            const progress = Math.min(1, elapsed / durationMs);
            // Smooth cubic ease-out
            const eased = 1 - Math.pow(1 - progress, 3);
            const currentVal = Math.round(safeTarget * eased);
            counterEl.textContent = String(currentVal);

            if (progress < 1) {
                this._coinAnimFrame = requestAnimationFrame(step);
            } else {
                counterEl.textContent = String(safeTarget);
                badgeEl.classList.remove('coin-counting');
                badgeEl.classList.add('coin-landed');
                this._coinAnimFrame = null;
            }
        };

        this._coinAnimFrame = requestAnimationFrame(step);
    }

    showMatchOverModal(isWinner, rank, kills) {
        const modal = this.matchOverModal;
        if (!modal) return;

        modal.classList.remove('hidden', 'outcome-victory', 'outcome-loss');
        // Force reflow so CSS keyframe banner animations re-trigger cleanly
        void modal.offsetWidth;
        modal.classList.add(isWinner ? 'outcome-victory' : 'outcome-loss');

        const emblemEl = document.getElementById('matchOutcomeEmblem');
        const kickerEl = document.getElementById('matchOutcomeKicker');
        const titleEl = document.getElementById('matchOutcomeTitle');
        const descEl = document.getElementById('matchOutcomeDesc');
        const statRankEl = document.getElementById('matchStatRank');
        const statKillsEl = document.getElementById('matchStatKills');
        const confettiLayer = document.getElementById('matchConfettiLayer');

        if (emblemEl) emblemEl.textContent = isWinner ? '🏆' : '💀';
        if (kickerEl) kickerEl.textContent = isWinner ? 'VICTORY' : 'DEFEAT';
        if (titleEl) {
            titleEl.textContent = isWinner
                ? 'BOOYAH! #1 CHAMPION'
                : `LOSS • ELIMINATED (#${rank})`;
        }
        if (descEl) {
            descEl.textContent = isWinner
                ? `Dominated WarLand with ${kills} eliminations!`
                : `Finished Rank #${rank} with ${kills} eliminations • Try again!`;
        }
        if (statRankEl) statRankEl.textContent = `#${rank}`;
        if (statKillsEl) statKillsEl.textContent = String(kills);

        if (isWinner) {
            this._spawnVictoryConfetti(confettiLayer);
        } else if (confettiLayer) {
            confettiLayer.innerHTML = '';
        }

        const coinsEarned = (kills * 5) + (isWinner ? 50 : (rank <= 3 ? 30 : 5));
        this._animateCoinCount(coinsEarned);

        if (isWinner && window.soundEngine) {
            window.soundEngine.playVictory();
        }

        // Bridge to Android Kotlin WebView
        if (typeof window.onMatchCompleted === 'function') {
            window.onMatchCompleted(kills, rank, coinsEarned);
        } else if (window.WarLandBridge && typeof window.WarLandBridge.onMatchCompleted === 'function') {
            window.WarLandBridge.onMatchCompleted(kills, rank, coinsEarned);
        }
    }
}

window.WarLandHud = WarLandHud;
