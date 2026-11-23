// War Land 3D - Advanced Animation System v2.0
// Full FBX/GLB animation integration with state machine, retargeting & blending
// Integrates all animations from /animation folder (FBX → GLB converted via assets_loader)

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
            IDLE:              { priority: 0, loop: true,  blendIn: 0.25 },
            WALK:              { priority: 1, loop: true,  blendIn: 0.20 },
            WALK_BACK:         { priority: 1, loop: true,  blendIn: 0.20 },
            WALK_LEFT:         { priority: 1, loop: true,  blendIn: 0.20 },
            WALK_RIGHT:        { priority: 1, loop: true,  blendIn: 0.20 },
            RUN:               { priority: 2, loop: true,  blendIn: 0.18 },
            SPRINT:            { priority: 3, loop: true,  blendIn: 0.15 },
            // AIRBORNE
            JUMP:              { priority: 5, loop: false, blendIn: 0.15 },
            FALL:              { priority: 4, loop: true,  blendIn: 0.20 },
            LAND:              { priority: 5, loop: false, blendIn: 0.12 },
            // POSTURE
            CROUCH_IDLE:       { priority: 2, loop: true,  blendIn: 0.22 },
            CROUCH_WALK:       { priority: 3, loop: true,  blendIn: 0.20 },
            PRONE_IDLE:        { priority: 2, loop: true,  blendIn: 0.22 },
            PRONE_MOVE:        { priority: 3, loop: true,  blendIn: 0.20 },
            // SLIDING
            SLIDE:             { priority: 6, loop: false, blendIn: 0.12 },
            SLIDE_LOOP:        { priority: 6, loop: true,  blendIn: 0.10 },
            // COMBAT
            AIM_IDLE:          { priority: 4, loop: true,  blendIn: 0.18 },
            FIRE:              { priority: 7, loop: false, blendIn: 0.08 },
            RELOAD:            { priority: 6, loop: false, blendIn: 0.15 },
            GRENADE_THROW:     { priority: 6, loop: false, blendIn: 0.12 },
            // HIT / DEATH
            HIT:               { priority: 8, loop: false, blendIn: 0.08 },
            DEATH:             { priority: 9, loop: false, blendIn: 0.15 },
            DEATH_PRONE:       { priority: 9, loop: false, blendIn: 0.15 },
            // SWIMMING
            SWIM:              { priority: 3, loop: true,  blendIn: 0.25 },
            // SPECIAL
            SKYDIVE:           { priority: 4, loop: true,  blendIn: 0.20 },
            PARACHUTE:         { priority: 4, loop: true,  blendIn: 0.25 },
        };

        // Animation name mappings from downloaded FBX pack names
        // These clip names appear inside the anims_combat_pack.glb
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

        // Loaded animation clips keyed by state
        this.clips = {};
        this.isReady = false;
    }

    // ── LOAD ANIMATIONS FROM GLB ───────────────────────────────────────────────
    loadAnimationsFromGLB(gltfAnimations) {
        if (!gltfAnimations || gltfAnimations.length === 0) return;

        // Build lookup map: normalized name → clip
        const clipByName = {};
        for (const clip of gltfAnimations) {
            const normalName = clip.name.toLowerCase().trim();
            clipByName[normalName] = clip;
            clipByName[clip.name] = clip;
        }

        // Map each state to best matching clip
        for (const [state, candidates] of Object.entries(this.animNameMap)) {
            for (const candidateName of candidates) {
                const lower = candidateName.toLowerCase().trim();
                const found = clipByName[lower] || clipByName[candidateName];
                if (found) {
                    this.clips[state] = found;
                    break;
                }
            }
        }

        // Fallbacks: ensure critical states always have something
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
                this.clips[state] = clip;
            }
        }

        this.isReady = Object.keys(this.clips).length > 0;
        console.log(`[AnimSystem] Loaded ${Object.keys(this.clips).length} animation states:`, Object.keys(this.clips).join(', '));
    }

    // ── ATTACH TO A CHARACTER ──────────────────────────────────────────────────
    attachToCharacter(characterMesh, animationClips) {
        if (!characterMesh) return null;

        const mixer = new THREE.AnimationMixer(characterMesh);
        this.mixers.set(characterMesh, mixer);
        this.actions.set(characterMesh, {});
        this.currentStates.set(characterMesh, null);

        // Load clips from either provided array or our registry
        const clips = animationClips || Object.values(this.clips);

        const actionMap = this.actions.get(characterMesh);
        for (const clip of clips) {
            if (clip && clip.name) {
                try {
                    const action = mixer.clipAction(clip);
                    action.setEffectiveWeight(0);
                    actionMap[clip.name] = action;
                } catch(e) {
                    // skip bad clip
                }
            }
        }

        return mixer;
    }

    // ── TRANSITION TO STATE ────────────────────────────────────────────────────
    transitionTo(characterMesh, newState, options = {}) {
        if (!characterMesh) return;

        const mixer = this.mixers.get(characterMesh);
        if (!mixer) return;

        const currentState = this.currentStates.get(characterMesh);
        if (currentState === newState) return;

        const actionMap = this.actions.get(characterMesh);
        if (!actionMap) return;

        const clip = this.clips[newState];
        if (!clip) return;

        const stateDef = this.states[newState] || { loop: true, blendIn: 0.2 };
        const blendDuration = options.blendIn ?? stateDef.blendIn ?? 0.2;

        // Find or create new action
        let newAction = actionMap[clip.name];
        if (!newAction) {
            try {
                newAction = mixer.clipAction(clip);
                actionMap[clip.name] = newAction;
            } catch(e) { return; }
        }

        // Fade out current
        if (currentState && this.clips[currentState]) {
            const oldClip = this.clips[currentState];
            const oldAction = actionMap[oldClip.name];
            if (oldAction && oldAction !== newAction) {
                oldAction.fadeOut(blendDuration);
            }
        }

        // Configure loop
        newAction.loop = stateDef.loop ? THREE.LoopRepeat : THREE.LoopOnce;
        newAction.clampWhenFinished = !stateDef.loop;

        // Fade in new action
        newAction.reset();
        newAction.setEffectiveWeight(1);
        newAction.fadeIn(blendDuration);
        newAction.play();

        this.currentStates.set(characterMesh, newState);

        // For one-shot animations, listen for completion
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

    // ── EVALUATE CORRECT STATE FROM PLAYER ────────────────────────────────────
    evaluateState(player, isFiring, isAiming) {
        if (!player.isAlive) return 'DEATH';
        if (player.inPlane) return 'IDLE';

        if (player.isSkydiving && !player.isParachuteOpen) return 'SKYDIVE';
        if (player.isParachuteOpen) return 'PARACHUTE';

        if (player.isSwimming) return 'SWIM';

        const isMoving = player.velocity.clone().setY(0).length() > 0.5;

        if (!player.isGrounded && player.velocity.y < -1.5) return 'FALL';
        if (!player.isGrounded && player.velocity.y > 1.5) return 'JUMP';

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

        if (player.isSprinting) return 'SPRINT';

        // Directional check when aiming or moving relative to camera
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

        const hSpeed = Math.hypot(player.velocity.x, player.velocity.z);
        if (hSpeed > 8.0) return 'RUN';
        return 'WALK';
    }

    // ── TICK UPDATE ────────────────────────────────────────────────────────────
    update(dt, characterMesh, player, isFiring, isAiming) {
        const mixer = this.mixers.get(characterMesh);
        if (mixer) {
            mixer.update(dt);
        }

        const targetState = this.evaluateState(player, isFiring, isAiming);
        this.transitionTo(characterMesh, targetState);
    }

    // ── TRIGGER ONE-SHOT ACTIONS ───────────────────────────────────────────────
    triggerAction(characterMesh, state, onComplete) {
        this.transitionTo(characterMesh, state, { onComplete });
    }

    // ── CURRENT STATE ──────────────────────────────────────────────────────────
    getState(characterMesh) {
        return this.currentStates.get(characterMesh) || 'IDLE';
    }
}

window.animationSystem = new AnimationSystem();
