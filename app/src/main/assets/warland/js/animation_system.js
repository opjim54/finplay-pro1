// War Land 3D - Advanced Animation System v3.0 (Smooth In-Place Locomotion)
// Full FBX/GLB animation integration with state machine, retargeting & blending
// Eliminates root-motion drift for silky smooth 60fps running without snapping

class AnimationSystem {
    constructor() {
        // Animation clip registry: maps state → animation name in GLB file
        this.clipRegistry = {};

        // Runtime mixer per character instance
        this.mixers = new WeakMap();
        this.actions = new WeakMap();
        this.currentStates = new WeakMap();
        this.blendWeights = new WeakMap();

        // Animation state machine definition
        this.states = {
            // LOCOMOTION
            IDLE:              { priority: 0, loop: true,  blendIn: 0.22, timeScale: 1.0 },
            WALK:              { priority: 1, loop: true,  blendIn: 0.18, timeScale: 1.05 },
            WALK_BACK:         { priority: 1, loop: true,  blendIn: 0.18, timeScale: 1.05 },
            WALK_LEFT:         { priority: 1, loop: true,  blendIn: 0.18, timeScale: 1.05 },
            WALK_RIGHT:        { priority: 1, loop: true,  blendIn: 0.18, timeScale: 1.05 },
            RUN:               { priority: 2, loop: true,  blendIn: 0.15, timeScale: 1.15 },
            SPRINT:            { priority: 3, loop: true,  blendIn: 0.12, timeScale: 1.30 },
            // AIRBORNE
            JUMP:              { priority: 5, loop: false, blendIn: 0.10, timeScale: 1.2 },
            FALL:              { priority: 4, loop: true,  blendIn: 0.15, timeScale: 1.0 },
            LAND:              { priority: 5, loop: false, blendIn: 0.10, timeScale: 1.2 },
            // POSTURE
            CROUCH_IDLE:       { priority: 2, loop: true,  blendIn: 0.20, timeScale: 1.0 },
            CROUCH_WALK:       { priority: 3, loop: true,  blendIn: 0.18, timeScale: 1.1 },
            PRONE_IDLE:        { priority: 2, loop: true,  blendIn: 0.20, timeScale: 1.0 },
            PRONE_MOVE:        { priority: 3, loop: true,  blendIn: 0.18, timeScale: 1.1 },
            // SLIDING
            SLIDE:             { priority: 6, loop: false, blendIn: 0.10, timeScale: 1.2 },
            SLIDE_LOOP:        { priority: 6, loop: true,  blendIn: 0.10, timeScale: 1.0 },
            // COMBAT
            AIM_IDLE:          { priority: 4, loop: true,  blendIn: 0.15, timeScale: 1.0 },
            FIRE:              { priority: 7, loop: false, blendIn: 0.05, timeScale: 1.4 },
            RELOAD:            { priority: 6, loop: false, blendIn: 0.12, timeScale: 1.1 },
            GRENADE_THROW:     { priority: 6, loop: false, blendIn: 0.10, timeScale: 1.2 },
            // HIT / DEATH
            HIT:               { priority: 8, loop: false, blendIn: 0.06, timeScale: 1.3 },
            DEATH:             { priority: 9, loop: false, blendIn: 0.12, timeScale: 1.0 },
            DEATH_PRONE:       { priority: 9, loop: false, blendIn: 0.12, timeScale: 1.0 },
            // SWIMMING
            SWIM:              { priority: 3, loop: true,  blendIn: 0.22, timeScale: 1.1 },
            // SPECIAL
            SKYDIVE:           { priority: 4, loop: true,  blendIn: 0.20, timeScale: 1.0 },
            PARACHUTE:         { priority: 4, loop: true,  blendIn: 0.25, timeScale: 1.0 },
        };

        // Animation name mappings from downloaded FBX pack names
        this.animNameMap = {
            IDLE:          ['idle', 'rifle aiming idle', 'Standing Idle', 'Idle', 'T-Pose'],
            WALK:          ['walk', 'Walking', 'walking', 'start walking', 'Rifle Walk'],
            WALK_BACK:     ['walk_backwards', 'walking backwards', 'Backwards Rifle Walk', 'run backwards'],
            WALK_LEFT:     ['strafe_left', 'strafe', 'Strafe Left', 'Left Strafe'],
            WALK_RIGHT:    ['strafe_right', 'strafe (2)', 'Strafe Right', 'Right Strafe'],
            RUN:           ['run', 'Running', 'Run', 'running', 'rifle run'],
            SPRINT:        ['sprint', 'Sprint', 'Running (1)', 'Idle To Running', 'Turn To Running'],
            JUMP:          ['jump', 'jump_forward', 'Running Jump', 'Jump', 'Jumping'],
            FALL:          ['jump_backward', 'fall', 'Fall', 'Falling'],
            LAND:          ['land', 'Landing', 'stop walking', 'idle'],
            CROUCH_IDLE:   ['crouch_walk', 'Rifle Crouch Walk To Kneel', 'Crouch Idle'],
            CROUCH_WALK:   ['crouch_walk', 'Rifle Kneel To Prone', 'Crouch Walk'],
            PRONE_IDLE:    ['prone_forward', 'Prone Forward', 'Prone Idle'],
            PRONE_MOVE:    ['prone_forward', 'Moving Backward In Prone Position'],
            SLIDE:         ['slide', 'Sprint To Backslide', 'Slide'],
            SLIDE_LOOP:    ['slide', 'Slide Loop'],
            AIM_IDLE:      ['idle', 'rifle aiming idle', 'Aim Idle', 'Pistol Walk Backward'],
            FIRE:          ['fire', 'firing rifle', 'prone_fire', 'Fire'],
            RELOAD:        ['Grab And Put Back Rifle', 'Reload', 'reload'],
            GRENADE_THROW: ['throw_grenade', 'Throw Grenade', 'Throw'],
            HIT:           ['hit', 'Hit', 'Take Damage'],
            DEATH:         ['death', 'Standing Death Backward 01', 'Death'],
            DEATH_PRONE:   ['death_prone', 'Prone Death', 'Death Prone'],
            SWIM:          ['swim', 'Swimming', 'Swim'],
            SKYDIVE:       ['jump_backward', 'fall', 'Skydive'],
            PARACHUTE:     ['swim_idle', 'treading water', 'Parachute'],
        };

        this.clips = {};
        this.isReady = false;
        this.registeredCharacters = new Set();
    }

    // ── LOAD ANIMATIONS FROM GLB & REMOVE ROOT MOTION DRIFT ───────────────────
    loadAnimationsFromGLB(gltfAnimations) {
        if (!gltfAnimations || gltfAnimations.length === 0) return;

        // Build lookup map: normalized name → clip
        const clipByName = {};
        for (const clip of gltfAnimations) {
            const normalName = clip.name.toLowerCase().trim();
            clipByName[normalName] = clip;
            clipByName[clip.name] = clip;
        }

        // Map each state to best matching clip (clone to sanitize tracks independently)
        for (const [state, candidates] of Object.entries(this.animNameMap)) {
            for (const candidateName of candidates) {
                const lower = candidateName.toLowerCase().trim();
                const found = clipByName[lower] || clipByName[candidateName];
                if (found) {
                    this.clips[state] = found.clone();
                    this.clips[state].name = state;
                    break;
                }
            }
        }

        // Fallbacks: ensure critical states always have a valid clip
        const fallbacks = {
            WALK:        this.clips.RUN,
            RUN:         this.clips.WALK,
            SPRINT:      this.clips.RUN || this.clips.WALK,
            CROUCH_IDLE: this.clips.IDLE,
            CROUCH_WALK: this.clips.WALK,
            PRONE_IDLE:  this.clips.CROUCH_IDLE || this.clips.IDLE,
            PRONE_MOVE:  this.clips.CROUCH_WALK || this.clips.WALK,
            SLIDE:       this.clips.SPRINT || this.clips.RUN,
            SLIDE_LOOP:  this.clips.SLIDE,
            AIM_IDLE:    this.clips.IDLE,
            FALL:        this.clips.JUMP,
            LAND:        this.clips.WALK,
            SWIM:        this.clips.IDLE,
            SKYDIVE:     this.clips.FALL || this.clips.JUMP,
            PARACHUTE:   this.clips.IDLE,
        };

        for (const [state, clip] of Object.entries(fallbacks)) {
            if (!this.clips[state] && clip) {
                this.clips[state] = clip.clone();
                this.clips[state].name = state;
            }
        }

        // ── CRITICAL: Neutralize Progressive Root Motion Drift on Locomotion Clips! ──
        // This stops the character from leaving the collider and snapping back on loop!
        const inPlaceStates = ['WALK', 'WALK_BACK', 'WALK_LEFT', 'WALK_RIGHT', 'RUN', 'SPRINT', 'CROUCH_WALK', 'PRONE_MOVE'];
        for (const state of inPlaceStates) {
            const clip = this.clips[state];
            if (!clip || !clip.tracks) continue;

            for (const track of clip.tracks) {
                if (track.name.toLowerCase().includes('hips') && track.name.endsWith('.position')) {
                    const count = track.values.length / 3;
                    if (count > 1) {
                        const firstX = track.values[0];
                        const lastX = track.values[(count - 1) * 3];
                        const firstY = track.values[1];
                        const lastY = track.values[(count - 1) * 3 + 1];
                        const firstZ = track.values[2];
                        const lastZ = track.values[(count - 1) * 3 + 2];

                        const deltaX = lastX - firstX;
                        const deltaY = lastY - firstY;
                        const deltaZ = lastZ - firstZ;

                        for (let i = 0; i < count; i++) {
                            const progress = i / (count - 1);
                            // Subtract progressive forward/lateral drift so start and end align perfectly
                            if (Math.abs(deltaY) > 0.4) {
                                track.values[i * 3 + 1] -= (deltaY * progress);
                            }
                            if (Math.abs(deltaZ) > 0.4) {
                                track.values[i * 3 + 2] -= (deltaZ * progress);
                            }
                            if (Math.abs(deltaX) > 0.25) {
                                track.values[i * 3] -= (deltaX * progress);
                            }
                        }
                    }
                }
            }
        }

        this.isReady = Object.keys(this.clips).length > 0;
        console.log(`[AnimSystem] Ready: ${Object.keys(this.clips).length} in-place clips loaded.`);

        // Re-bind to existing live characters
        for (const charMesh of this.registeredCharacters) {
            this.rebindCharacter(charMesh);
        }
    }

    // ── ATTACH TO A CHARACTER ──────────────────────────────────────────────────
    attachToCharacter(characterMesh, animationClips) {
        if (!characterMesh) return null;

        let mixer = this.mixers.get(characterMesh);
        if (!mixer) {
            mixer = new THREE.AnimationMixer(characterMesh);
            this.mixers.set(characterMesh, mixer);
            this.actions.set(characterMesh, {});
            this.currentStates.set(characterMesh, null);
            this.registeredCharacters.add(characterMesh);
        }

        this.rebindCharacter(characterMesh, animationClips);
        return mixer;
    }

    rebindCharacter(characterMesh, extraClips = null) {
        if (!characterMesh) return;
        const mixer = this.mixers.get(characterMesh);
        if (!mixer) return;

        const actionMap = this.actions.get(characterMesh) || {};
        this.actions.set(characterMesh, actionMap);

        const allClips = extraClips || Object.values(this.clips);
        for (const clip of allClips) {
            if (clip && clip.name && !actionMap[clip.name]) {
                try {
                    const action = mixer.clipAction(clip);
                    action.setEffectiveWeight(0);
                    actionMap[clip.name] = action;
                } catch(e) {
                    // skip incompatible clip
                }
            }
        }

        // If no state playing yet, start IDLE
        if (!this.currentStates.get(characterMesh) && this.clips['IDLE']) {
            this.transitionTo(characterMesh, 'IDLE', { blendIn: 0.1 });
        }
    }

    // ── TRANSITION TO STATE ────────────────────────────────────────────────────
    transitionTo(characterMesh, newState, options = {}) {
        if (!characterMesh) return;

        const mixer = this.mixers.get(characterMesh);
        if (!mixer) return;

        const currentState = this.currentStates.get(characterMesh);
        if (currentState === newState && !options.force) return;

        const actionMap = this.actions.get(characterMesh);
        if (!actionMap) return;

        const clip = this.clips[newState];
        if (!clip) return;

        const stateDef = this.states[newState] || { loop: true, blendIn: 0.2, timeScale: 1.0 };
        const blendDuration = options.blendIn ?? stateDef.blendIn ?? 0.18;

        // Find or create new action
        let newAction = actionMap[clip.name];
        if (!newAction) {
            try {
                newAction = mixer.clipAction(clip);
                actionMap[clip.name] = newAction;
            } catch(e) { return; }
        }

        // Fade out current action
        if (currentState && this.clips[currentState]) {
            const oldClip = this.clips[currentState];
            const oldAction = actionMap[oldClip.name];
            if (oldAction && oldAction !== newAction) {
                oldAction.fadeOut(blendDuration);
            }
        }

        // Configure loop & time scale
        newAction.loop = stateDef.loop ? THREE.LoopRepeat : THREE.LoopOnce;
        newAction.clampWhenFinished = !stateDef.loop;
        newAction.timeScale = stateDef.timeScale || 1.0;

        // Fade in new action
        newAction.reset();
        newAction.setEffectiveWeight(1);
        newAction.fadeIn(blendDuration);
        newAction.play();

        this.currentStates.set(characterMesh, newState);

        if (!stateDef.loop && options.onComplete) {
            const onFinished = (e) => {
                if (e.action === newAction) {
                    mixer.removeEventListener('finished', onFinished);
                    options.onComplete();
                }
            };
            mixer.addEventListener('finished', onFinished);
        }
    }

    // ── EVALUATE STATE FROM PLAYER PHYSICS & COMBAT ────────────────────────────
    evaluateState(player, isFiring, isAiming) {
        if (!player.isAlive) return 'DEATH';
        if (player.inPlane) return 'IDLE';

        if (player.isSkydiving && !player.isParachuteOpen) return 'SKYDIVE';
        if (player.isParachuteOpen) return 'PARACHUTE';

        if (player.isSwimming) return 'SWIM';

        const hSpeed = Math.hypot(player.velocity.x, player.velocity.z);
        const isMoving = hSpeed > 0.45;

        if (!player.isGrounded && player.velocity.y < -1.8) return 'FALL';
        if (!player.isGrounded && player.velocity.y > 1.8) return 'JUMP';

        if (player.isSliding) return 'SLIDE';

        if (player.posture === 'PRONE') {
            return isMoving ? 'PRONE_MOVE' : 'PRONE_IDLE';
        }
        if (player.posture === 'CROUCH') {
            return isMoving ? 'CROUCH_WALK' : 'CROUCH_IDLE';
        }

        if (!isMoving) {
            if (isFiring) return 'FIRE';
            return isAiming ? 'AIM_IDLE' : 'IDLE';
        }

        // Sprinting priority
        if (player.isSprinting && hSpeed > 6.0) return 'SPRINT';

        // Directional checks when aiming
        if (isAiming) {
            const hVel = new THREE.Vector2(player.velocity.x, player.velocity.z).normalize();
            const fwd = new THREE.Vector2(-Math.sin(player.rotationY), -Math.cos(player.rotationY)).normalize();
            const right = new THREE.Vector2(Math.cos(player.rotationY), -Math.sin(player.rotationY)).normalize();
            const dotFwd = hVel.dot(fwd);
            const dotRight = hVel.dot(right);

            if (dotFwd < -0.45) return 'WALK_BACK';
            if (dotRight > 0.55) return 'WALK_RIGHT';
            if (dotRight < -0.55) return 'WALK_LEFT';
        }

        if (hSpeed > 6.5) return 'RUN';
        return 'WALK';
    }

    // ── MAIN TICK UPDATE ───────────────────────────────────────────────────────
    update(dt, characterMesh, player, isFiring, isAiming) {
        const mixer = this.mixers.get(characterMesh);
        if (mixer) {
            mixer.update(dt);
        }

        const targetState = this.evaluateState(player, isFiring, isAiming);
        this.transitionTo(characterMesh, targetState);
    }

    triggerAction(characterMesh, state, onComplete) {
        this.transitionTo(characterMesh, state, { onComplete, force: true });
    }

    getState(characterMesh) {
        return this.currentStates.get(characterMesh) || 'IDLE';
    }
}

window.animationSystem = new AnimationSystem();
