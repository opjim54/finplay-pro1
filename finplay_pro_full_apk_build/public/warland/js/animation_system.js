// War Land 3D - Complete Animation Retargeting & State Machine System (v4.0)
// Integrates all 22 clips from anims_combat_pack.glb with automatic bone-prefix retargeting,
// root-motion drift elimination, speed-synced locomotion, and layered combat/action priority locks.

class AnimationSystem {
    constructor() {
        this.mixers = new WeakMap();
        this.actions = new WeakMap();
        this.currentStates = new WeakMap();
        this.actionLocks = new WeakMap(); // { state, until }
        this.retargetCache = new Map();   // prefix -> { stateName: AnimationClip }

        // Complete State Machine Configuration
        this.states = {
            // LOCOMOTION
            IDLE:              { priority: 0, loop: true,  blendIn: 0.20, timeScale: 1.0 },
            WALK:              { priority: 1, loop: true,  blendIn: 0.16, timeScale: 1.0, refSpeed: 4.5 },
            WALK_BACK:         { priority: 1, loop: true,  blendIn: 0.16, timeScale: 1.0, refSpeed: 4.0 },
            WALK_LEFT:         { priority: 1, loop: true,  blendIn: 0.16, timeScale: 1.0, refSpeed: 4.2 },
            WALK_RIGHT:        { priority: 1, loop: true,  blendIn: 0.16, timeScale: 1.0, refSpeed: 4.2 },
            RUN:               { priority: 2, loop: true,  blendIn: 0.14, timeScale: 1.05, refSpeed: 7.5 },
            SPRINT:            { priority: 3, loop: true,  blendIn: 0.12, timeScale: 1.15, refSpeed: 12.0 },
            // AIRBORNE
            JUMP:              { priority: 5, loop: false, blendIn: 0.08, timeScale: 1.15 },
            JUMP_BACK:         { priority: 5, loop: false, blendIn: 0.08, timeScale: 1.1 },
            FALL:              { priority: 4, loop: true,  blendIn: 0.14, timeScale: 0.9 },
            LAND:              { priority: 5, loop: false, blendIn: 0.06, timeScale: 1.4 },
            // POSTURE
            CROUCH_IDLE:       { priority: 2, loop: true,  blendIn: 0.18, timeScale: 0.04 }, // Hold crouched pose cleanly
            CROUCH_WALK:       { priority: 3, loop: true,  blendIn: 0.15, timeScale: 1.05, refSpeed: 3.8 },
            PRONE_IDLE:        { priority: 2, loop: true,  blendIn: 0.20, timeScale: 0.03 }, // Hold prone pose cleanly
            PRONE_MOVE:        { priority: 3, loop: true,  blendIn: 0.16, timeScale: 1.1, refSpeed: 2.0 },
            PRONE_FIRE:        { priority: 7, loop: true,  blendIn: 0.06, timeScale: 1.25 },
            // SLIDING & VAULTING
            SLIDE:             { priority: 6, loop: false, blendIn: 0.08, timeScale: 1.05 },
            SLIDE_LOOP:        { priority: 6, loop: true,  blendIn: 0.08, timeScale: 1.0 },
            WALL_CLIMB:        { priority: 6, loop: true,  blendIn: 0.12, timeScale: 1.1 },
            // COMBAT
            AIM_IDLE:          { priority: 4, loop: true,  blendIn: 0.14, timeScale: 0.85 },
            FIRE:              { priority: 7, loop: true,  blendIn: 0.05, timeScale: 1.35 },
            RELOAD:            { priority: 6, loop: false, blendIn: 0.10, timeScale: 1.1 },
            GRENADE_THROW:     { priority: 7, loop: false, blendIn: 0.08, timeScale: 1.25 },
            // HIT / DEATH
            HIT:               { priority: 8, loop: false, blendIn: 0.05, timeScale: 1.4 },
            DEATH:             { priority: 9, loop: false, blendIn: 0.10, timeScale: 1.0 },
            DEATH_PRONE:       { priority: 9, loop: false, blendIn: 0.10, timeScale: 1.0 },
            // SWIMMING & AERIAL DEPLOYMENT
            SWIM:              { priority: 3, loop: true,  blendIn: 0.22, timeScale: 1.1 },
            SWIM_IDLE:         { priority: 3, loop: true,  blendIn: 0.22, timeScale: 0.9 },
            SKYDIVE:           { priority: 4, loop: true,  blendIn: 0.18, timeScale: 0.8 },
            PARACHUTE:         { priority: 4, loop: true,  blendIn: 0.22, timeScale: 0.7 },
        };

        // Exact mappings to all 22 clips inside assets/animations/anims_combat_pack.glb
        this.animNameMap = {
            IDLE:          ['idle', 'rifle aiming idle', 'standing idle', 'armature|mixamo.com|layer0'],
            WALK:          ['walk', 'walking', 'rifle walk'],
            WALK_BACK:     ['walk_backwards', 'walking backwards', 'backwards rifle walk'],
            WALK_LEFT:     ['strafe_left', 'strafe left', 'left strafe'],
            WALK_RIGHT:    ['strafe_right', 'strafe right', 'right strafe'],
            RUN:           ['run', 'running', 'rifle run'],
            SPRINT:        ['sprint', 'running (1)', 'run'],
            JUMP:          ['jump_forward', 'jump', 'running jump', 'jumping'],
            JUMP_BACK:     ['jump_backward', 'jump'],
            FALL:          ['jump_backward', 'fall', 'falling', 'jump'],
            LAND:          ['idle', 'land', 'landing'],
            CROUCH_IDLE:   ['crouch_walk', 'crouch idle'],
            CROUCH_WALK:   ['crouch_walk', 'crouch walk'],
            PRONE_IDLE:    ['prone_forward', 'prone idle'],
            PRONE_MOVE:    ['prone_forward', 'prone move'],
            PRONE_FIRE:    ['prone_fire', 'fire'],
            SLIDE:         ['slide', 'sprint to backslide'],
            SLIDE_LOOP:    ['slide'],
            WALL_CLIMB:    ['wall_climb', 'climb'],
            AIM_IDLE:      ['idle', 'rifle aiming idle', 'aim idle'],
            FIRE:          ['fire', 'firing rifle'],
            RELOAD:        ['throw_grenade', 'reload', 'fire'],
            GRENADE_THROW: ['throw_grenade', 'throw grenade'],
            HIT:           ['jump_backward', 'hit'],
            DEATH:         ['death', 'standing death backward 01'],
            DEATH_PRONE:   ['death_prone', 'prone death', 'death'],
            SWIM:          ['swim', 'swimming'],
            SWIM_IDLE:     ['swim_idle', 'treading water', 'swim'],
            SKYDIVE:       ['jump_backward', 'fall', 'swim'],
            PARACHUTE:     ['swim_idle', 'idle'],
        };

        this.clips = {};
        this.isReady = false;
        this.registeredCharacters = new Set();
    }

    // ── LOAD & SANITIZE ALL CLIPS FROM anims_combat_pack.glb ───────────────────
    loadAnimationsFromGLB(gltfAnimations) {
        if (!gltfAnimations || gltfAnimations.length === 0) return;

        const clipByName = {};
        for (const clip of gltfAnimations) {
            const normalName = clip.name.toLowerCase().trim();
            clipByName[normalName] = clip;
            clipByName[clip.name] = clip;
        }

        // Map each state to its exact clip
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

        // Safety fallbacks so zero states are ever missing
        const fallbacks = {
            WALK:          this.clips.RUN || this.clips.IDLE,
            RUN:           this.clips.WALK || this.clips.IDLE,
            SPRINT:        this.clips.RUN || this.clips.WALK,
            WALK_BACK:     this.clips.WALK,
            WALK_LEFT:     this.clips.WALK,
            WALK_RIGHT:    this.clips.WALK,
            CROUCH_IDLE:   this.clips.CROUCH_WALK || this.clips.IDLE,
            CROUCH_WALK:   this.clips.WALK,
            PRONE_IDLE:    this.clips.PRONE_MOVE || this.clips.CROUCH_IDLE,
            PRONE_MOVE:    this.clips.CROUCH_WALK || this.clips.WALK,
            PRONE_FIRE:    this.clips.FIRE || this.clips.PRONE_IDLE,
            SLIDE:         this.clips.SPRINT || this.clips.RUN,
            SLIDE_LOOP:    this.clips.SLIDE,
            AIM_IDLE:      this.clips.IDLE,
            FIRE:          this.clips.IDLE,
            FALL:          this.clips.JUMP,
            LAND:          this.clips.IDLE,
            SWIM:          this.clips.IDLE,
            SWIM_IDLE:     this.clips.SWIM || this.clips.IDLE,
            SKYDIVE:       this.clips.FALL || this.clips.JUMP,
            PARACHUTE:     this.clips.SWIM_IDLE || this.clips.IDLE,
            DEATH_PRONE:   this.clips.DEATH,
        };

        for (const [state, clip] of Object.entries(fallbacks)) {
            if (!this.clips[state] && clip) {
                this.clips[state] = clip.clone();
                this.clips[state].name = state;
            }
        }

        // Neutralize progressive horizontal root-motion drift so characters stay locked to their physics collider!
        const stripDriftStates = [
            'IDLE', 'WALK', 'WALK_BACK', 'WALK_LEFT', 'WALK_RIGHT', 'RUN', 'SPRINT',
            'CROUCH_IDLE', 'CROUCH_WALK', 'PRONE_IDLE', 'PRONE_MOVE', 'PRONE_FIRE',
            'SLIDE', 'SLIDE_LOOP', 'JUMP', 'JUMP_BACK', 'FALL', 'SWIM', 'SWIM_IDLE',
            'WALL_CLIMB', 'FIRE', 'AIM_IDLE'
        ];

        for (const state of stripDriftStates) {
            const clip = this.clips[state];
            if (!clip || !clip.tracks) continue;

            for (const track of clip.tracks) {
                const lowerName = track.name.toLowerCase();
                if (lowerName.includes('hips') && lowerName.endsWith('.position')) {
                    const count = track.values.length / 3;
                    if (count > 1) {
                        const firstX = track.values[0];
                        const firstZ = track.values[2];
                        const lastX = track.values[(count - 1) * 3];
                        const lastZ = track.values[(count - 1) * 3 + 2];

                        const deltaX = lastX - firstX;
                        const deltaZ = lastZ - firstZ;

                        for (let i = 0; i < count; i++) {
                            const progress = i / (count - 1);
                            // Lock XZ root translation smoothly to origin (keeps natural Y hip bounce intact!)
                            if (Math.abs(deltaX) > 0.08) {
                                track.values[i * 3] -= deltaX * progress;
                            } else {
                                track.values[i * 3] = firstX + (track.values[i * 3] - firstX) * 0.25;
                            }
                            if (Math.abs(deltaZ) > 0.08) {
                                track.values[i * 3 + 2] -= deltaZ * progress;
                            } else {
                                track.values[i * 3 + 2] = firstZ + (track.values[i * 3 + 2] - firstZ) * 0.25;
                            }
                        }
                    }
                }
            }
        }

        this.retargetCache.clear();
        this.isReady = Object.keys(this.clips).length > 0;
        console.log(`[AnimSystem] Ready: ${Object.keys(this.clips).length} sanitized in-place states bound.`);

        for (const charMesh of this.registeredCharacters) {
            this.rebindCharacter(charMesh);
        }
    }

    // Detects the skeleton's bone naming prefix so clips retarget automatically to any Mixamo/generic rig
    _detectBonePrefix(characterMesh) {
        let prefix = 'mixamorig:';
        characterMesh.traverse((n) => {
            if (n.isBone) {
                if (n.name === 'mixamorig:Hips') prefix = 'mixamorig:';
                else if (n.name === 'mixamorigHips') prefix = 'mixamorig';
                else if (n.name === 'Hips') prefix = '';
            }
        });
        return prefix;
    }

    _getRetargetedClipsForPrefix(prefix) {
        if (prefix === 'mixamorig:') return this.clips;
        if (this.retargetCache.has(prefix)) return this.retargetCache.get(prefix);

        const mapped = {};
        for (const [state, clip] of Object.entries(this.clips)) {
            const cloned = clip.clone();
            cloned.tracks = cloned.tracks.map(t => {
                const nt = t.clone();
                if (nt.name.startsWith('mixamorig:')) {
                    nt.name = prefix + nt.name.slice('mixamorig:'.length);
                }
                return nt;
            });
            mapped[state] = cloned;
        }
        this.retargetCache.set(prefix, mapped);
        return mapped;
    }

    // ── ATTACH TO A SKINNED CHARACTER ──────────────────────────────────────────
    attachToCharacter(characterMesh) {
        if (!characterMesh) return null;
        if (characterMesh.userData && characterMesh.userData.hasSkinnedMesh === false) {
            return null; // Static character mesh: uses smooth procedural locomotion instead of AnimationMixer
        }

        let mixer = this.mixers.get(characterMesh);
        if (!mixer) {
            mixer = new THREE.AnimationMixer(characterMesh);
            this.mixers.set(characterMesh, mixer);
            this.actions.set(characterMesh, {});
            this.currentStates.set(characterMesh, null);
            this.registeredCharacters.add(characterMesh);
        }

        this.rebindCharacter(characterMesh);
        return mixer;
    }

    detachCharacter(characterMesh) {
        if (!characterMesh) return;
        const mixer = this.mixers.get(characterMesh);
        if (mixer) mixer.stopAllAction();
        this.registeredCharacters.delete(characterMesh);
    }

    rebindCharacter(characterMesh) {
        if (!characterMesh || !this.isReady) return;
        const mixer = this.mixers.get(characterMesh);
        if (!mixer) return;

        const prefix = this._detectBonePrefix(characterMesh);
        const clipSet = this._getRetargetedClipsForPrefix(prefix);
        const actionMap = {};
        this.actions.set(characterMesh, actionMap);

        for (const [stateName, clip] of Object.entries(clipSet)) {
            if (!clip) continue;
            try {
                const action = mixer.clipAction(clip);
                action.setEffectiveWeight(0);
                actionMap[stateName] = action;
            } catch (e) {
                // Ignore unsupported track bindings gracefully
            }
        }

        if (!this.currentStates.get(characterMesh) && actionMap['IDLE']) {
            this.transitionTo(characterMesh, 'IDLE', { blendIn: 0.1, force: true });
        }
    }

    // ── TRANSITION WITH SMOOTH CROSS-FADE ──────────────────────────────────────
    transitionTo(characterMesh, newState, options = {}) {
        if (!characterMesh) return;
        const mixer = this.mixers.get(characterMesh);
        if (!mixer) return;

        const currentState = this.currentStates.get(characterMesh);
        if (currentState === newState && !options.force) return;

        const actionMap = this.actions.get(characterMesh);
        if (!actionMap || !actionMap[newState]) return;

        const stateDef = this.states[newState] || { loop: true, blendIn: 0.18, timeScale: 1.0 };
        const blendDuration = options.blendIn ?? stateDef.blendIn ?? 0.16;
        const newAction = actionMap[newState];

        if (currentState && actionMap[currentState] && actionMap[currentState] !== newAction) {
            actionMap[currentState].fadeOut(blendDuration);
        }

        newAction.enabled = true;
        newAction.loop = stateDef.loop ? THREE.LoopRepeat : THREE.LoopOnce;
        newAction.clampWhenFinished = !stateDef.loop;
        newAction.timeScale = options.timeScale ?? stateDef.timeScale ?? 1.0;

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

    // ── EVALUATE FULL MOVEMENT + COMBAT STATE MACHINE ──────────────────────────
    evaluateState(entity, isFiring, isAiming) {
        if (!entity.isAlive) {
            return (entity.posture === 'PRONE') ? 'DEATH_PRONE' : 'DEATH';
        }
        if (entity.inPlane) return 'IDLE';

        if (entity.isSkydiving && !entity.isParachuteOpen) return 'SKYDIVE';
        if (entity.isParachuteOpen) return 'PARACHUTE';

        const hSpeed = Math.hypot(entity.velocity ? entity.velocity.x : 0, entity.velocity ? entity.velocity.z : 0);
        const isMoving = hSpeed > 0.4;

        if (entity.isSwimming) {
            return isMoving ? 'SWIM' : 'SWIM_IDLE';
        }

        // Respect short one-shot action locks (GRENADE_THROW, RELOAD, LAND, HIT)
        const lock = this.actionLocks.get(entity.blenderModel);
        if (lock && performance.now() < lock.until) {
            // Allow high-priority movement like SLIDE or JUMP to cancel RELOAD/LAND
            if (!entity.isSliding && entity.isGrounded) {
                return lock.state;
            }
        }

        if (entity.isSliding) return 'SLIDE';

        if (entity.isGrounded === false) {
            if (entity.velocity && entity.velocity.y < -2.2) return 'FALL';
            if (entity.velocity && entity.velocity.y > 1.2) return 'JUMP';
        }

        if (entity.posture === 'PRONE') {
            if (isFiring) return 'PRONE_FIRE';
            return isMoving ? 'PRONE_MOVE' : 'PRONE_IDLE';
        }

        if (entity.posture === 'CROUCH') {
            if (isFiring && !isMoving) return 'FIRE';
            return isMoving ? 'CROUCH_WALK' : 'CROUCH_IDLE';
        }

        if (!isMoving) {
            if (isFiring) return 'FIRE';
            return isAiming ? 'AIM_IDLE' : 'IDLE';
        }

        // Firing while slow-walking plays FIRE cleanly; while sprinting prioritizes SPRINT
        if (isFiring && hSpeed < 4.5) {
            return 'FIRE';
        }

        if (entity.isSprinting && hSpeed > 7.8) {
            return 'SPRINT';
        }

        // 8-way strafe & backpedal detection when aiming or in combat
        if (isAiming || isFiring) {
            const invLen = 1 / Math.max(0.001, hSpeed);
            const vx = entity.velocity.x * invLen;
            const vz = entity.velocity.z * invLen;
            const yaw = entity.rotationY || 0;
            const fwdX = -Math.sin(yaw);
            const fwdZ = -Math.cos(yaw);
            const rightX = Math.cos(yaw);
            const rightZ = -Math.sin(yaw);

            const dotFwd = vx * fwdX + vz * fwdZ;
            const dotRight = vx * rightX + vz * rightZ;

            if (dotFwd < -0.45) return 'WALK_BACK';
            if (dotRight > 0.55) return 'WALK_RIGHT';
            if (dotRight < -0.55) return 'WALK_LEFT';
            return (hSpeed > 5.5) ? 'RUN' : 'WALK';
        }

        if (hSpeed > 5.4) return 'RUN';
        return 'WALK';
    }

    // ── FRAME TICK WITH SPEED-SYNCED PLAYBACK (ELIMINATES FOOT SLIDING) ────────
    update(dt, characterMesh, entity, isFiring, isAiming) {
        if (!characterMesh || !this.isReady) return;
        const mixer = this.mixers.get(characterMesh);
        if (!mixer) return;

        const targetState = this.evaluateState(entity, isFiring, isAiming);
        this.transitionTo(characterMesh, targetState);

        // Dynamically sync locomotion clip playback speed with true ground speed
        const stateDef = this.states[targetState];
        const actionMap = this.actions.get(characterMesh);
        if (stateDef && stateDef.refSpeed && actionMap && actionMap[targetState]) {
            const hSpeed = Math.hypot(entity.velocity ? entity.velocity.x : 0, entity.velocity ? entity.velocity.z : 0);
            const synced = THREE.MathUtils.clamp((hSpeed / stateDef.refSpeed) * stateDef.timeScale, 0.55, 1.55);
            actionMap[targetState].timeScale = synced;
        }

        mixer.update(dt);
    }

    triggerAction(characterMesh, state, onComplete, lockDurationMs = 450) {
        if (!characterMesh || !this.isReady) return;
        if (lockDurationMs > 0) {
            this.actionLocks.set(characterMesh, {
                state: state,
                until: performance.now() + lockDurationMs
            });
        }
        this.transitionTo(characterMesh, state, { onComplete, force: true });
    }

    getState(characterMesh) {
        return this.currentStates.get(characterMesh) || 'IDLE';
    }
}

window.animationSystem = new AnimationSystem();
