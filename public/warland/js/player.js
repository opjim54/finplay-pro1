// War Land 3D - Advanced Third-Person Tactical Combat Controller (v5.0)
// 360-Degree Fluid Movement, Multi-Weapon Back/Hip Holstering, Fists Punch Melee & Parachute Skydive

class PlayerController {
    constructor(scene, camera, map) {
        this.scene = scene;
        this.camera = camera;
        this.map = map;

        // Player Stats
        this.maxHp = 200;
        this.hp = 200;
        this.maxArmor = 100;
        this.armor = 100;
        this.kills = 0;
        this.isAlive = true;

        // Skydiving & Deployment State (Starts inside military cargo aircraft)
        this.inPlane = true;
        this.isSkydiving = false;
        this.isParachuteOpen = false;
        this.skydiveSpeed = -24.0; // Realistic aerofoil freefall dive m/s
        this.parachuteGlideSpeed = -5.0; // Gentle parachute glide m/s
        this.position = new THREE.Vector3(-360, 190.0, -360);
        this.velocity = new THREE.Vector3();

        // 360-Degree Character Heading & Camera Orbit
        this.rotationY = 0;          // Horizontal Camera Orbit Yaw
        this.pitch = 0.04;           // Vertical Camera Pitch
        this.characterYaw = 0;       // Smooth 360-Degree Model Facing Yaw
        this.posture = 'STAND';      // 'STAND', 'CROUCH', 'PRONE'
        this.isSprinting = false;
        this.isGrounded = false;

        // ── SLIDING SYSTEM ───────────────────────────────────────────────────
        this.isSliding = false;
        this.slideTimer = 0;
        this.slideDuration = 1.1;    // seconds of slide
        this.slideSpeed = 14.5;      // peak slide speed m/s
        this.slideDirection = new THREE.Vector3();
        this.slideDecay = 0.85;      // speed decay per second
        this.slideCooldown = 0;      // prevent spam re-slide
        this.slideCooldownMax = 1.2;

        // Camera Framing Offsets
        this.cameraDistance = 3.6;
        this.cameraHeightOffset = 1.62;
        this.cameraTrauma = 0;
        this.isSwimming = false;
        this.swimTimer = 0;

        // Dynamic Recoil, ADS & Punch Melee
        this.recoilZ = 0;
        this.recoilPitch = 0;
        this.recoilYaw = 0;
        this.adsWeight = 0;
        this.currentWeaponId = 'FISTS';

        this.punchTimer = 0;
        this.punchArm = 0; // 0 = right arm, 1 = left arm
        this.lastPunchTime = 0;

        // Active Emote
        this.activeEmote = null;
        this.emoteTimer = 0;

        // Sound Timing
        this.footstepTimer = 0;

        // Build 3D Character Mesh & Multi-Weapon Sockets
        this.mesh = new THREE.Group();
        this.buildHumanoidModel();
        this.scene.add(this.mesh);

        // Initial holstered weapon setup
        setTimeout(() => {
            this.updateHolsteredWeapons();
        }, 150);
    }

    buildHumanoidModel() {
        const urlParams = new URLSearchParams(window.location.search);
        let charSkin = urlParams.get('skin') || localStorage.getItem('warland_skin') || localStorage.getItem('warland_equipped_skin') || window.activeSkinId || 'freya';
        if (charSkin === 'zane') charSkin = 'freya';
        let bModel = null;
        if (window.blenderAssetLoader) {
            bModel = window.blenderAssetLoader.createCharacterInstance(charSkin);
            if (bModel) {
                bModel.rotation.y = Math.PI; // Align CC3/Mixamo model facing -Z forward
                this.blenderModel = bModel;
                this.mesh.add(bModel);

                // Clean up any procedural limbs so they NEVER block the camera or float
                if (this.torso) { this.mesh.remove(this.torso); this.torso = null; }
                if (this.head) { this.mesh.remove(this.head); this.head = null; }
                if (this.armL) { this.mesh.remove(this.armL); this.armL = null; }
                if (this.armR) { this.mesh.remove(this.armR); this.armR = null; }
                if (this.leftLeg) { this.mesh.remove(this.leftLeg); this.leftLeg = null; }
                if (this.rightLeg) { this.mesh.remove(this.rightLeg); this.rightLeg = null; }

                const findBone = (m, names) => {
                    if (!m) return null;
                    for (const n of names) {
                        const b = m.getObjectByName(n);
                        if (b) return b;
                    }
                    return null;
                };

                this.blenderPelvis = findBone(bModel, ['mixamorig:Hips', 'CC_Base_Pelvis', 'pelvis', 'Hips', 'DEF-spine', 'Char_Pelvis']);
                this.blenderTorso = findBone(bModel, ['mixamorig:Spine1', 'mixamorig:Spine', 'CC_Base_Waist', 'CC_Base_Spine01', 'spine', 'Spine', 'DEF-spine.002', 'Char_Torso']);
                this.blenderHead = findBone(bModel, ['mixamorig:Head', 'CC_Base_Head', 'head', 'Head', 'DEF-spine.006', 'Char_Head']);
                this.blenderArmL = findBone(bModel, ['mixamorig:LeftArm', 'CC_Base_L_Upperarm', 'upper_arm.L', 'DEF-upper_arm.L', 'LeftArm', 'Char_Arm_L']);
                this.blenderArmR = findBone(bModel, ['mixamorig:RightArm', 'CC_Base_R_Upperarm', 'upper_arm.R', 'DEF-upper_arm.R', 'RightArm', 'Char_Arm_R']);
                this.blenderForearmL = findBone(bModel, ['mixamorig:LeftForeArm', 'CC_Base_L_Forearm', 'forearm.L', 'DEF-forearm.L', 'LeftForeArm', 'Char_Forearm_L']);
                this.blenderForearmR = findBone(bModel, ['mixamorig:RightForeArm', 'CC_Base_R_Forearm', 'forearm.R', 'DEF-forearm.R', 'RightForeArm', 'Char_Forearm_R']);
                this.blenderLegL = findBone(bModel, ['mixamorig:LeftUpLeg', 'CC_Base_L_Thigh', 'thigh.L', 'DEF-thigh.L', 'LeftUpLeg', 'Char_Leg_L']);
                this.blenderLegR = findBone(bModel, ['mixamorig:RightUpLeg', 'CC_Base_R_Thigh', 'thigh.R', 'DEF-thigh.R', 'RightUpLeg', 'Char_Leg_R']);
                this.blenderCalfL = findBone(bModel, ['mixamorig:LeftLeg', 'CC_Base_L_Calf', 'calf.L', 'DEF-calf.L', 'LeftLeg', 'Char_Calf_L']);
                this.blenderCalfR = findBone(bModel, ['mixamorig:RightLeg', 'CC_Base_R_Calf', 'calf.R', 'DEF-calf.R', 'RightLeg', 'Char_Calf_R']);

                const allBones = [
                    this.blenderPelvis, this.blenderTorso, this.blenderHead,
                    this.blenderArmL, this.blenderArmR, this.blenderForearmL, this.blenderForearmR,
                    this.blenderLegL, this.blenderLegR, this.blenderCalfL, this.blenderCalfR
                ];
                allBones.forEach(b => {
                    if (b) {
                        b.initialQuaternion = b.quaternion.clone();
                    }
                });

                if (bModel.userData && bModel.userData.animations && bModel.userData.animations.length > 0) {
                    const clip = bModel.userData.animations[0];
                    if (clip && clip.duration > 0.25) {
                        this.mixer = new THREE.AnimationMixer(bModel);
                        const action = this.mixer.clipAction(clip);
                        action.play();
                    }
                }
            }
        }

        // Procedural fallback if 3D model wasn't loaded
        if (!bModel) {
            const skinMat = new THREE.MeshLambertMaterial({ color: 0xFBBF24 });
            const shirtMat = new THREE.MeshStandardMaterial({ color: 0xDC2626, roughness: 0.6 });
            const pantsMat = new THREE.MeshStandardMaterial({ color: 0x1E293B, roughness: 0.7 });

            this.torso = new THREE.Mesh(new THREE.CylinderGeometry(0.24, 0.22, 0.75, 10), shirtMat);
            this.torso.position.y = 1.35;
            this.mesh.add(this.torso);

            this.head = new THREE.Mesh(new THREE.SphereGeometry(0.20, 12, 12), skinMat);
            this.head.position.y = 1.95;
            this.mesh.add(this.head);

            this.armL = new THREE.Mesh(new THREE.CylinderGeometry(0.07, 0.06, 0.60, 8), shirtMat);
            this.armL.position.set(-0.35, 1.35, 0);
            this.mesh.add(this.armL);

            this.armR = new THREE.Mesh(new THREE.CylinderGeometry(0.07, 0.06, 0.60, 8), shirtMat);
            this.armR.position.set(0.35, 1.35, 0);
            this.mesh.add(this.armR);

            this.leftLeg = new THREE.Mesh(new THREE.CylinderGeometry(0.09, 0.07, 0.85, 8), pantsMat);
            this.leftLeg.position.set(-0.16, 0.45, 0);
            this.mesh.add(this.leftLeg);

            this.rightLeg = new THREE.Mesh(new THREE.CylinderGeometry(0.09, 0.07, 0.85, 8), pantsMat);
            this.rightLeg.position.set(0.16, 0.45, 0);
            this.mesh.add(this.rightLeg);
        }

        // ── WEAPON & MOUNT SOCKETS ───────────────────────────────────────────
        // 1. Hand Mount (active weapon in 2 hands)
        this.weaponMount = new THREE.Group();
        this.weaponMount.position.set(0.12, 1.25, -0.32);
        this.mesh.add(this.weaponMount);

        // 2. Back Mount 1 (Primary Gun 1 holstered on back diagonally)
        this.backMount1 = new THREE.Group();
        this.backMount1.position.set(-0.06, 1.30, 0.16);
        this.backMount1.rotation.set(-0.25, 0, 0.72); // Barrel pointing up over right shoulder
        this.mesh.add(this.backMount1);

        // 3. Back Mount 2 (Primary Gun 2 holstered on back opposing diagonal)
        this.backMount2 = new THREE.Group();
        this.backMount2.position.set(0.06, 1.30, 0.16);
        this.backMount2.rotation.set(-0.25, 0, -0.72); // Barrel pointing up over left shoulder
        this.mesh.add(this.backMount2);

        // 4. Hip Mount (Pistol in side holster bag on right hip)
        this.holsterMount = new THREE.Group();
        this.holsterMount.position.set(0.20, 0.86, 0.04);
        this.holsterMount.rotation.set(0, 0, -0.15); // Snug against right hip
        this.mesh.add(this.holsterMount);

        // 5. Parachute Canopy Mount (above shoulders)
        this.parachuteMount = new THREE.Group();
        this.parachuteMount.position.set(0, 1.38, 0);
        this.parachuteMount.visible = false;
        this.mesh.add(this.parachuteMount);

        // Attach Parachute 3D Model
        if (window.blenderAssetLoader) {
            const pModel = window.blenderAssetLoader.createParachuteInstance();
            if (pModel) {
                this.parachuteMount.add(pModel);
            }
        }

        // Overhead Emote Bubble Banner
        this.emoteBubble = new THREE.Group();
        this.emoteBubble.position.set(0, 2.3, 0);
        this.emoteBubble.visible = false;

        const bubbleCanvas = document.createElement('canvas');
        bubbleCanvas.width = 128;
        bubbleCanvas.height = 128;
        this.bubbleCtx = bubbleCanvas.getContext('2d');
        this.bubbleTexture = new THREE.CanvasTexture(bubbleCanvas);

        const bubbleMat = new THREE.SpriteMaterial({ map: this.bubbleTexture, transparent: true });
        this.bubbleSprite = new THREE.Sprite(bubbleMat);
        this.bubbleSprite.scale.set(1.2, 1.2, 1.2);
        this.emoteBubble.add(this.bubbleSprite);
        this.mesh.add(this.emoteBubble);
    }

    applySkin(charSkin) {
        if (!charSkin || charSkin === 'zane') charSkin = 'freya';
        window.activeSkinId = charSkin;
        localStorage.setItem('warland_skin', charSkin);

        if (this.blenderModel) {
            this.mesh.remove(this.blenderModel);
            this.blenderModel = null;
        }

        // Clean up any procedural fallback meshes so they never coexist with 3D model
        if (this.torso) { this.mesh.remove(this.torso); this.torso = null; }
        if (this.head) { this.mesh.remove(this.head); this.head = null; }
        if (this.armL) { this.mesh.remove(this.armL); this.armL = null; }
        if (this.armR) { this.mesh.remove(this.armR); this.armR = null; }
        if (this.leftLeg) { this.mesh.remove(this.leftLeg); this.leftLeg = null; }
        if (this.rightLeg) { this.mesh.remove(this.rightLeg); this.rightLeg = null; }

        if (window.blenderAssetLoader) {
            const bModel = window.blenderAssetLoader.createCharacterInstance(charSkin);
            if (bModel) {
                bModel.rotation.y = Math.PI;
                this.blenderModel = bModel;
                this.mesh.add(bModel);

                const findBone = (m, names) => {
                    if (!m) return null;
                    for (const n of names) {
                        const b = m.getObjectByName(n);
                        if (b) return b;
                    }
                    return null;
                };

                this.blenderPelvis = findBone(bModel, ['mixamorig:Hips', 'CC_Base_Pelvis', 'pelvis', 'Hips', 'DEF-spine', 'Char_Pelvis']);
                this.blenderTorso = findBone(bModel, ['mixamorig:Spine1', 'mixamorig:Spine', 'CC_Base_Waist', 'CC_Base_Spine01', 'spine', 'Spine', 'DEF-spine.002', 'Char_Torso']);
                this.blenderHead = findBone(bModel, ['mixamorig:Head', 'CC_Base_Head', 'head', 'Head', 'DEF-spine.006', 'Char_Head']);
                this.blenderArmL = findBone(bModel, ['mixamorig:LeftArm', 'CC_Base_L_Upperarm', 'upper_arm.L', 'DEF-upper_arm.L', 'LeftArm', 'Char_Arm_L']);
                this.blenderArmR = findBone(bModel, ['mixamorig:RightArm', 'CC_Base_R_Upperarm', 'upper_arm.R', 'DEF-upper_arm.R', 'RightArm', 'Char_Arm_R']);
                this.blenderForearmL = findBone(bModel, ['mixamorig:LeftForeArm', 'CC_Base_L_Forearm', 'forearm.L', 'DEF-forearm.L', 'LeftForeArm', 'Char_Forearm_L']);
                this.blenderForearmR = findBone(bModel, ['mixamorig:RightForeArm', 'CC_Base_R_Forearm', 'forearm.R', 'DEF-forearm.R', 'RightForeArm', 'Char_Forearm_R']);
                this.blenderLegL = findBone(bModel, ['mixamorig:LeftUpLeg', 'CC_Base_L_Thigh', 'thigh.L', 'DEF-thigh.L', 'LeftUpLeg', 'Char_Leg_L']);
                this.blenderLegR = findBone(bModel, ['mixamorig:RightUpLeg', 'CC_Base_R_Thigh', 'thigh.R', 'DEF-thigh.R', 'RightUpLeg', 'Char_Leg_R']);
                this.blenderCalfL = findBone(bModel, ['mixamorig:LeftLeg', 'CC_Base_L_Calf', 'calf.L', 'DEF-calf.L', 'LeftLeg', 'Char_Calf_L']);
                this.blenderCalfR = findBone(bModel, ['mixamorig:RightLeg', 'CC_Base_R_Calf', 'calf.R', 'DEF-calf.R', 'RightLeg', 'Char_Calf_R']);

                const allBones = [
                    this.blenderPelvis, this.blenderTorso, this.blenderHead,
                    this.blenderArmL, this.blenderArmR, this.blenderForearmL, this.blenderForearmR,
                    this.blenderLegL, this.blenderLegR, this.blenderCalfL, this.blenderCalfR
                ];
                allBones.forEach(b => {
                    if (b) {
                        b.initialQuaternion = b.quaternion.clone();
                    }
                });

                if (bModel.userData && bModel.userData.animations && bModel.userData.animations.length > 0) {
                    const clip = bModel.userData.animations[0];
                    if (clip && clip.duration > 0.25) {
                        this.mixer = new THREE.AnimationMixer(bModel);
                        const action = this.mixer.clipAction(clip);
                        action.play();
                    }
                }
            }
        }
        this.updateHolsteredWeapons();
    }

    applyWeaponSkin(gunSkin) {
        if (!gunSkin) return;
        localStorage.setItem('warland_weapon_skin', gunSkin);
        this.updateHolsteredWeapons();
    }

    // ── MULTI-WEAPON HOLSTERING & DYNAMIC MESH UPDATES ────────────────────────
    updateHolsteredWeapons() {
        if (!window.inventory) return;

        // Clear all mount groups
        while (this.weaponMount.children.length > 0) this.weaponMount.remove(this.weaponMount.children[0]);
        while (this.backMount1.children.length > 0) this.backMount1.remove(this.backMount1.children[0]);
        while (this.backMount2.children.length > 0) this.backMount2.remove(this.backMount2.children[0]);
        while (this.holsterMount.children.length > 0) this.holsterMount.remove(this.holsterMount.children[0]);

        const loader = window.blenderAssetLoader;
        const activeSlot = window.inventory.activeSlot;
        const w0 = window.inventory.weapons[0]; // Fists
        const w1 = window.inventory.weapons[1]; // Primary 1
        const w2 = window.inventory.weapons[2]; // Primary 2
        const w3 = window.inventory.weapons[3]; // Pistol

        // 1. Active Weapon in Hand (Slot 1, 2, or 3)
        if (activeSlot !== 0) {
            const activeWeapon = window.inventory.getActiveWeapon();
            if (activeWeapon && loader) {
                const gunMesh = loader.createWeaponInstance(activeWeapon.id);
                if (gunMesh) {
                    this.gunMesh = gunMesh;
                    gunMesh.position.set(0, 0, 0);
                    gunMesh.rotation.set(Math.PI, Math.PI, Math.PI);
                    this.weaponMount.add(gunMesh);

                    // Muzzle Point & Dynamic Flash at tip of barrel
                    this.muzzlePoint = new THREE.Object3D();
                    const muzzleZ = (activeWeapon.id === 'DEAGLE') ? -0.28 : (activeWeapon.id === 'AWM' ? -0.85 : -0.66);
                    this.muzzlePoint.position.set(0, 0.015, muzzleZ);
                    this.weaponMount.add(this.muzzlePoint);

                    const flashMat = new THREE.MeshBasicMaterial({ color: 0xFDE047, transparent: true, opacity: 0 });
                    this.flashMesh = new THREE.Mesh(new THREE.OctahedronGeometry(0.12), flashMat);
                    this.flashMesh.position.copy(this.muzzlePoint.position);
                    this.weaponMount.add(this.flashMesh);

                    this.muzzleLight = new THREE.PointLight(0xF59E0B, 0, 10);
                    this.muzzleLight.position.copy(this.muzzlePoint.position);
                    this.weaponMount.add(this.muzzleLight);
                }
            }
        } else {
            // Slot 0 (FISTS): No weapon in hand!
            this.gunMesh = null;
            this.muzzlePoint = null;
            this.flashMesh = null;
            this.muzzleLight = null;
        }

        // 2. Primary 1 Holstered on Back (when not active)
        if (w1 && activeSlot !== 1 && loader) {
            const backMesh1 = loader.createWeaponInstance(w1.id);
            if (backMesh1) {
                backMesh1.position.set(0, 0, 0);
                backMesh1.rotation.set(0, 0, 0);
                this.backMount1.add(backMesh1);
            }
        }

        // 3. Primary 2 Holstered on Back (when not active)
        if (w2 && activeSlot !== 2 && loader) {
            const backMesh2 = loader.createWeaponInstance(w2.id);
            if (backMesh2) {
                backMesh2.position.set(0, 0, 0);
                backMesh2.rotation.set(0, 0, 0);
                this.backMount2.add(backMesh2);
            }
        }

        // 4. Pistol Holstered on Right Hip (when not active)
        if (w3 && activeSlot !== 3 && loader) {
            const holsterMesh = loader.createWeaponInstance(w3.id);
            if (holsterMesh) {
                holsterMesh.position.set(0, 0, 0);
                holsterMesh.rotation.set(Math.PI, 0, 0);
                this.holsterMount.add(holsterMesh);
            }
        }
    }

    // ── COMBAT / MELEE PUNCH ACTION ──────────────────────────────────────────
    onFireTriggered() {
        const weapon = window.inventory ? window.inventory.getActiveWeapon() : null;
        if (weapon && weapon.isMelee) {
            this.onPunchTriggered();
            return;
        }

        // Gun Recoil Kickback
        this.recoilZ = 0.07;
        this.recoilPitch = 0.12;
        this.recoilYaw = (Math.random() - 0.5) * 0.03;

        // Flash Muzzle
        if (this.flashMesh && this.muzzleLight) {
            this.flashMesh.material.opacity = 0.95;
            this.flashMesh.rotation.z = Math.random() * Math.PI * 2;
            this.muzzleLight.intensity = 3.5;
            setTimeout(() => {
                if (this.flashMesh) this.flashMesh.material.opacity = 0;
                if (this.muzzleLight) this.muzzleLight.intensity = 0;
            }, 55);
        }
    }

    onPunchTriggered() {
        const now = performance.now() * 0.001;
        if (now - this.lastPunchTime < 0.28) return;
        this.lastPunchTime = now;

        this.punchTimer = 0.28;
        this.punchCombo = ((this.punchCombo || 0) + 1) % 3;
        this.punchArm = (this.punchCombo === 1) ? 1 : 0; // Alternate right / left punch; 3rd is heavy hook

        if (window.soundEngine) window.soundEngine.playPunch();

        // Punch Melee Hit Detection in Front (aligned with 360-deg character heading)
        if (window.gameEngine && window.gameEngine.botManager) {
            const facingYaw = (this.characterYaw !== undefined) ? this.characterYaw : this.rotationY;
            const fwd = new THREE.Vector3(0, 0, -1).applyAxisAngle(new THREE.Vector3(0, 1, 0), facingYaw);

            const myPos = this.position.clone();
            const baseDamage = (window.inventory && window.inventory.weapons[0]) ? window.inventory.weapons[0].damage : 25;
            const punchDamage = (this.punchCombo === 2) ? Math.round(baseDamage * 1.5) : baseDamage;

            for (const bot of window.gameEngine.botManager.bots) {
                if (!bot.isAlive) continue;
                const dist = myPos.distanceTo(bot.position);
                if (dist < 2.8) {
                    const toBot = new THREE.Vector3().subVectors(bot.position, myPos);
                    toBot.y = 0;
                    toBot.normalize();
                    const dot = fwd.dot(toBot);
                    if (dot > 0.35) { // In front within 140-degree tactical melee cone
                        bot.takeDamage(punchDamage, window.playerIgn || "Warrior_Pro");
                        // Knockback bot backward slightly
                        bot.position.addScaledVector(fwd, 0.45);

                        if (window.soundEngine) window.soundEngine.playPunchHit();
                        if (window.gameEngine) {
                            window.gameEngine.spawnExplosionEffect(bot.position.clone().add(new THREE.Vector3(0, 1.2, 0)));
                        }
                        this.recoilPitch = 0.05; // Game-feel micro punch impact kick
                        break;
                    }
                }
            }
        }
    }

    triggerEmote(emoteType) {
        this.activeEmote = emoteType;
        this.emoteTimer = 3.5;
        this.emoteBubble.visible = true;

        const glyphs = {
            emote_wave: '👋',
            emote_heart: '❤️',
            emote_laugh: '😂',
            emote_dance: '🕺'
        };
        const glyph = glyphs[emoteType] || '⭐';

        this.bubbleCtx.clearRect(0, 0, 128, 128);
        this.bubbleCtx.fillStyle = 'rgba(15, 23, 42, 0.85)';
        this.bubbleCtx.beginPath();
        this.bubbleCtx.arc(64, 64, 56, 0, Math.PI * 2);
        this.bubbleCtx.fill();
        this.bubbleCtx.lineWidth = 4;
        this.bubbleCtx.strokeStyle = '#F59E0B';
        this.bubbleCtx.stroke();

        this.bubbleCtx.font = '64px sans-serif';
        this.bubbleCtx.textAlign = 'center';
        this.bubbleCtx.textBaseline = 'middle';
        this.bubbleCtx.fillText(glyph, 64, 64);
        this.bubbleTexture.needsUpdate = true;
    }

    takeDamage(amount, isHeadshot = false) {
        if (!this.isAlive) return;
        let actualDamage = isHeadshot ? amount * 1.5 : amount;

        if (this.armor > 0) {
            const absorbed = actualDamage * 0.5;
            this.armor = Math.max(0, this.armor - absorbed);
            actualDamage -= absorbed;
        }

        this.hp = Math.max(0, this.hp - actualDamage);
        if (window.soundEngine) window.soundEngine.playHitMarker(isHeadshot);

        if (this.hp <= 0) {
            this.isAlive = false;
            if (window.gameEngine) window.gameEngine.onPlayerEliminated();
        }
        if (window.hud) window.hud.updateHealthUi();
    }

    healMedkit() {
        if (this.hp >= this.maxHp) return false;
        this.hp = Math.min(this.maxHp, this.hp + 75);
        if (window.soundEngine) window.soundEngine.playMedkit();
        if (window.hud) window.hud.updateHealthUi();
        return true;
    }

    // ── SLIDE ACTION ─────────────────────────────────────────────────────────
    startSlide() {
        if (!this.isGrounded) return;
        if (this.isSliding) return;
        if (this.isSwimming) return;
        if (this.posture === 'PRONE') return;
        if (this.slideCooldown > 0) return;

        // Must be moving reasonably fast to slide
        const hSpeed = Math.hypot(this.velocity.x, this.velocity.z);
        if (hSpeed < 3.0) return;

        this.isSliding = true;
        this.slideTimer = this.slideDuration;
        this.posture = 'CROUCH'; // Crouches during slide

        // Slide in current movement direction
        if (hSpeed > 0.1) {
            this.slideDirection.set(this.velocity.x / hSpeed, 0, this.velocity.z / hSpeed);
        } else {
            const fwd = new THREE.Vector3(0, 0, -1).applyAxisAngle(new THREE.Vector3(0, 1, 0), this.rotationY);
            this.slideDirection.copy(fwd);
        }

        if (window.soundEngine) window.soundEngine.playSlide && window.soundEngine.playSlide();

        // Trigger slide animation
        if (window.animationSystem && this.blenderModel) {
            window.animationSystem.triggerAction(this.blenderModel, 'SLIDE', () => {
                // After slide ends, go to slide loop or end
            });
        }

        // Update Sprint button visual if present
        const slideBtn = document.getElementById('btnSlide');
        if (slideBtn) slideBtn.classList.add('active');
    }

    endSlide() {
        if (!this.isSliding) return;
        this.isSliding = false;
        this.slideTimer = 0;
        this.slideCooldown = this.slideCooldownMax;
        this.posture = 'STAND';

        const slideBtn = document.getElementById('btnSlide');
        if (slideBtn) slideBtn.classList.remove('active');
    }

    jump() {
        if (this.inPlane) {
            this.ejectFromPlane();
            return;
        }
        if (this.isSkydiving && !this.isParachuteOpen) {
            this.deployParachute();
            return;
        }
        // Cancel slide on jump
        if (this.isSliding) this.endSlide();
        if (this.isGrounded && this.posture !== 'PRONE') {
            this.velocity.y = 8.5;
            this.isGrounded = false;
            if (this.posture === 'CROUCH') this.posture = 'STAND';
        }
    }

    ejectFromPlane() {
        if (!this.inPlane) return;
        this.inPlane = false;
        this.isSkydiving = true;
        this.isParachuteOpen = false;
        this.mesh.visible = true;
        this.velocity.set(0, -22.0, 0);

        if (window.gameEngine && window.gameEngine.planeEnd && window.gameEngine.planeStart) {
            const dir = new THREE.Vector3().subVectors(window.gameEngine.planeEnd, window.gameEngine.planeStart).normalize();
            this.rotationY = Math.atan2(dir.x, dir.z);
            this.characterYaw = this.rotationY;
        }

        document.getElementById('planeFlightHud')?.classList.add('hidden');
        document.getElementById('chuteDeployHud')?.classList.remove('hidden');
    }

    deployParachute() {
        if (!this.isSkydiving || this.isParachuteOpen) return;
        this.isParachuteOpen = true;
        this.parachuteMount.visible = true;
        this.velocity.y = this.parachuteGlideSpeed;

        const chuteHud = document.getElementById('chuteDeployHud');
        if (chuteHud) chuteHud.classList.add('hidden');
        const btn = document.getElementById('btnDeployChute');
        if (btn) {
            btn.style.display = 'none';
            btn.classList.add('hidden');
        }

        if (window.soundEngine) window.soundEngine.playParachuteOpen();
        if (window.hud) window.hud.hideParachutePrompt();
    }

    landOnGround() {
        this.isSkydiving = false;
        this.isParachuteOpen = false;
        this.parachuteMount.visible = false;
        this.isGrounded = true;
        this.velocity.set(0, 0, 0);
        this.mesh.rotation.x = 0;

        document.getElementById('planeFlightHud')?.classList.add('hidden');
        const chuteHud = document.getElementById('chuteDeployHud');
        if (chuteHud) chuteHud.classList.add('hidden');
        const btn = document.getElementById('btnDeployChute');
        if (btn) {
            btn.style.display = 'none';
            btn.classList.add('hidden');
        }

        if (window.soundEngine) window.soundEngine.playLandingThud();
        this.updateHolsteredWeapons();
        if (window.hud) {
            window.hud.updateWeaponUi();
            window.hud.hideParachutePrompt();
        }
    }

    // ── MAIN TICK UPDATE LOOP ────────────────────────────────────────────────
    update(dt, inputVector, isFiring, isAiming, activeWeapon) {
        if (!this.isAlive) return;

        // In-Plane Waiting Phase
        if (this.inPlane) {
            const pAlt = document.getElementById('planeAltText');
            if (pAlt) pAlt.textContent = Math.round(this.position.y) + 'm';
            return;
        }

        // Recoil spring recovery
        this.recoilZ = THREE.MathUtils.lerp(this.recoilZ, 0, dt * 14);
        this.recoilPitch = THREE.MathUtils.lerp(this.recoilPitch, 0, dt * 16);
        this.recoilYaw = THREE.MathUtils.lerp(this.recoilYaw, 0, dt * 16);

        // ADS transition weight
        const targetAds = isAiming ? 1.0 : 0.0;
        this.adsWeight = THREE.MathUtils.lerp(this.adsWeight, targetAds, dt * 12);

        // Punch cooldown / timer tick
        if (this.punchTimer > 0) {
            this.punchTimer = Math.max(0, this.punchTimer - dt);
        }

        // Emote timer tick
        if (this.activeEmote) {
            this.emoteTimer -= dt;
            if (this.emoteTimer <= 0) {
                this.activeEmote = null;
                this.emoteBubble.visible = false;
            }
        }

        // Slide cooldown tick
        if (this.slideCooldown > 0) {
            this.slideCooldown = Math.max(0, this.slideCooldown - dt);
        }

        // Slide timer tick
        if (this.isSliding) {
            this.slideTimer -= dt;
            if (this.slideTimer <= 0) {
                this.endSlide();
            }
        }

        // Convert 2D input relative to camera orbit yaw
        const forward = new THREE.Vector3(0, 0, -1).applyAxisAngle(new THREE.Vector3(0, 1, 0), this.rotationY);
        const right = new THREE.Vector3(1, 0, 0).applyAxisAngle(new THREE.Vector3(0, 1, 0), this.rotationY);
        const moveDir = new THREE.Vector3();

        if (inputVector.length() > 0.05) {
            moveDir.addScaledVector(forward, inputVector.y);
            moveDir.addScaledVector(right, inputVector.x);
            moveDir.normalize();
        }

        // ── 1. SKYDIVING & PARACHUTING MOVEMENT ──────────────────────────────
        if (this.isSkydiving) {
            const chuteAlt = document.getElementById('skydiveAltText');
            if (chuteAlt) chuteAlt.textContent = Math.round(this.position.y) + 'm';

            // Auto open parachute at altitude 36m
            if (this.position.y <= 36.0 && !this.isParachuteOpen) {
                this.deployParachute();
            }

            // Air steering
            const airSpeed = this.isParachuteOpen ? 9.5 : 18.0;
            this.velocity.x = moveDir.x * airSpeed;
            this.velocity.z = moveDir.z * airSpeed;

            // Descent speed
            const targetVy = this.isParachuteOpen ? this.parachuteGlideSpeed : this.skydiveSpeed;
            this.velocity.y = THREE.MathUtils.lerp(this.velocity.y, targetVy, dt * 4);

            this.position.x += this.velocity.x * dt;
            this.position.y += this.velocity.y * dt;
            this.position.z += this.velocity.z * dt;

            // Smooth heading towards camera or steering direction
            this.characterYaw = this.rotationY;
            this.mesh.rotation.y = this.characterYaw;
            this.mesh.position.copy(this.position);

            // Aerodynamic tilt
            if (!this.isParachuteOpen) {
                this.mesh.rotation.x = -0.55; // Aerodynamic forward dive toward island
            } else {
                this.mesh.rotation.x = -0.05; // Upright under canopy
            }

            // Ground touchdown check
            const colResult = this.map.resolveCollision(this.position, this.position, 0.45, 1.8);
            if (this.position.y <= colResult.groundY + 0.1) {
                this.position.y = colResult.groundY;
                this.landOnGround();
            }

            this.updateCamera(dt, isAiming, activeWeapon);
            return;
        }

        // ── WATER & SWIMMING DETECTION ──────────────────────────────────────
        const distFromCenter = Math.hypot(this.position.x, this.position.z);
        const inWater = (distFromCenter > 340 || this.position.y < -0.1) && this.position.y <= 0.25;
        if (inWater) {
            this.isSwimming = true;
            this.isGrounded = false;
            this.position.y = -0.32; // Floating at water surface
            this.velocity.y = 0;
            if (this.isSkydiving) {
                this.landOnGround();
            }
        } else if (distFromCenter <= 335 && this.position.y >= 0.0) {
            this.isSwimming = false;
        }

        // ── 2. GROUNDED TACTICAL COMBAT & 360-DEGREE MOVEMENT ─────────────────
        let baseSpeed = 7.5;
        if (this.isSwimming) baseSpeed = 3.4;
        else if (this.posture === 'CROUCH') baseSpeed = 3.8;
        else if (this.posture === 'PRONE') baseSpeed = 2.0;
        else if (this.isSprinting && inputVector.length() > 0.1) baseSpeed = 12.0;

        if (isAiming) baseSpeed *= 0.55;

        // ── SLIDING MOVEMENT ─────────────────────────────────────────────────
        if (this.isSliding) {
            // Sliding uses decaying slide direction, ignoring joystick
            const slideProgress = 1.0 - (this.slideTimer / this.slideDuration);
            const currentSlideSpeed = this.slideSpeed * Math.pow(this.slideDecay, slideProgress * 6);
            this.velocity.x = this.slideDirection.x * currentSlideSpeed;
            this.velocity.z = this.slideDirection.z * currentSlideSpeed;

            // Slide character yaw locks to slide direction
            const targetHeading = Math.atan2(-this.slideDirection.x, -this.slideDirection.z);
            let angleDiff = (targetHeading - this.characterYaw) % (Math.PI * 2);
            if (angleDiff < -Math.PI) angleDiff += Math.PI * 2;
            if (angleDiff > Math.PI) angleDiff -= Math.PI * 2;
            this.characterYaw += angleDiff * Math.min(1.0, dt * 20);
        } else if (moveDir.length() > 0.05) {
            this.velocity.x = moveDir.x * baseSpeed;
            this.velocity.z = moveDir.z * baseSpeed;

            // 360-Degree Fluid Model Heading
            let targetHeading;
            if (isAiming || isFiring) {
                // When aiming or shooting, lock character directly to crosshair
                targetHeading = this.rotationY;
            } else {
                // When running freely, model turns 360 degrees to face moving direction!
                targetHeading = Math.atan2(-moveDir.x, -moveDir.z);
            }

            let angleDiff = (targetHeading - this.characterYaw) % (Math.PI * 2);
            if (angleDiff < -Math.PI) angleDiff += Math.PI * 2;
            if (angleDiff > Math.PI) angleDiff -= Math.PI * 2;
            this.characterYaw += angleDiff * Math.min(1.0, dt * 14);

            // Footstep audio
            this.footstepTimer += dt;
            const stepInterval = this.isSprinting ? 0.28 : 0.42;
            if (this.footstepTimer >= stepInterval && this.isGrounded && !this.isSwimming) {
                this.footstepTimer = 0;
                if (window.soundEngine) window.soundEngine.playFootstep();
            }
        } else {
            this.velocity.x = THREE.MathUtils.lerp(this.velocity.x, 0, dt * 15);
            this.velocity.z = THREE.MathUtils.lerp(this.velocity.z, 0, dt * 15);
            this.footstepTimer = 0;

            // When stationary, smoothly turn character to face where camera looks
            const targetHeading = this.rotationY;
            let angleDiff = (targetHeading - this.characterYaw) % (Math.PI * 2);
            if (angleDiff < -Math.PI) angleDiff += Math.PI * 2;
            if (angleDiff > Math.PI) angleDiff -= Math.PI * 2;
            const turnRate = (isAiming || isFiring) ? 18.0 : 8.0;
            this.characterYaw += angleDiff * Math.min(1.0, dt * turnRate);
        }

        // Gravity (only on dry land)
        if (!this.isSwimming) {
            this.velocity.y -= 25.0 * dt;
        }

        // Map Collision
        const nextPos = this.position.clone();
        nextPos.x += this.velocity.x * dt;
        nextPos.y += this.velocity.y * dt;
        nextPos.z += this.velocity.z * dt;

        const colResult = this.map.resolveCollision(this.position, nextPos, 0.45, 1.8);
        this.position.copy(colResult.position);

        // Ceiling collision prevention when jumping inside buildings
        if (colResult.ceilingY && (this.position.y + 1.8) >= colResult.ceilingY) {
            this.position.y = colResult.ceilingY - 1.82;
            if (this.velocity.y > 0) this.velocity.y = 0;
        }

        if (!this.isSwimming) {
            if (this.position.y <= colResult.groundY) {
                this.position.y = colResult.groundY;
                this.velocity.y = 0;
                this.isGrounded = true;
            } else {
                this.isGrounded = false;
            }
        }

        // Update mesh position and realistic skeletal orientation (NO squashing!)
        this.mesh.scale.set(1.0, 1.0, 1.0);
        this.mesh.position.copy(this.position);

        if (this.isSwimming) {
            // Horizontal floating breaststroke orientation
            this.mesh.position.y = -0.32;
            this.mesh.rotation.set(-1.25, this.characterYaw, 0);
            this.cameraHeightOffset = 0.82;
        } else if (this.isSliding) {
            // Slide: crouch-height body lean forward
            this.mesh.position.y = this.position.y - 0.52;
            this.mesh.rotation.set(0.18, this.characterYaw, 0); // slight forward lean
            this.cameraHeightOffset = 0.90;
        } else if (this.posture === 'PRONE') {
            // Lie flat horizontally along ground belly-down!
            this.mesh.position.y = this.position.y + 0.16;
            this.mesh.rotation.set(-Math.PI / 2 + 0.08, this.characterYaw, 0);
            this.cameraHeightOffset = 0.45;
        } else if (this.posture === 'CROUCH') {
            // Real crouching hip drop
            this.mesh.position.y = this.position.y - 0.42;
            this.mesh.rotation.set(0, this.characterYaw, 0);
            this.cameraHeightOffset = 1.1;
        } else {
            // Natural upright posture
            this.mesh.rotation.set(0, this.characterYaw, 0);
            this.cameraHeightOffset = 1.58;
        }

        if (this.mixer) {
            this.mixer.update(dt);
        }

        // ── ANIMATION SYSTEM UPDATE (GLB clip-based animation state machine) ──
        if (window.animationSystem && this.blenderModel && window.animationSystem.isReady) {
            window.animationSystem.update(dt, this.blenderModel, this, isFiring, isAiming);
            this.updateWeaponMount(isFiring, isAiming, activeWeapon, moveDir.length() > 0.05);
        } else {
            // Fallback: original procedural skeletal animation if 3D model or clip pack is missing
            this.animateLimbs(dt, moveDir.length() > 0.05, isFiring, isAiming, activeWeapon);
        }

        // Update Camera
        this.updateCamera(dt, isAiming, activeWeapon);
    }

    updateWeaponMount(isFiring, isAiming, activeWeapon, isMoving) {
        const time = performance.now() * 0.008;
        const isFistActive = !activeWeapon || activeWeapon.isMelee;
        if (!isFistActive && this.weaponMount) {
            const isPistol = (activeWeapon && activeWeapon.id === 'DEAGLE');
            const hipX = isPistol ? 0.08 : 0.12;
            const hipY = 1.22;
            const hipZ = isPistol ? -0.28 : -0.34;

            const adsX = 0.00;
            const adsY = 1.28;
            const adsZ = isPistol ? -0.32 : -0.40;

            const currentX = THREE.MathUtils.lerp(hipX, adsX, this.adsWeight);
            const currentY = THREE.MathUtils.lerp(hipY, adsY, this.adsWeight);
            const currentZ = THREE.MathUtils.lerp(hipZ, adsZ, this.adsWeight);

            let swayX = 0, swayY = 0;
            if (isMoving && this.isGrounded) {
                const swayDamp = (1.0 - this.adsWeight * 0.8);
                swayX = Math.sin(time * 0.8) * 0.02 * swayDamp;
                swayY = Math.abs(Math.cos(time * 1.6)) * 0.015 * swayDamp;
            }

            this.weaponMount.position.set(
                currentX + swayX + this.recoilYaw,
                currentY + swayY,
                currentZ + this.recoilZ
            );
            this.weaponMount.rotation.set(this.pitch - this.recoilPitch, Math.PI, 0);
        }
    }

    animateLimbs(dt, isMoving, isFiring, isAiming, activeWeapon) {
        const time = performance.now() * 0.008;

        // 1. Emote Overrides
        if (this.activeEmote) {
            if (this.activeEmote === 'emote_wave') {
                if (this.blenderArmR && this.blenderArmR.initialQuaternion) {
                    const qW = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(0, 0, 1), 1.6 + Math.sin(time * 6) * 0.35);
                    this.blenderArmR.quaternion.copy(this.blenderArmR.initialQuaternion).multiply(qW);
                } else if (this.armR) {
                    this.armR.rotation.set(-0.5, 0, 1.9 + Math.sin(time * 6) * 0.4);
                }
            } else if (this.activeEmote === 'emote_dance') {
                const legSwing = Math.sin(time * 5) * 0.5;
                if (this.leftLeg) this.leftLeg.rotation.x = legSwing;
                if (this.rightLeg) this.rightLeg.rotation.x = -legSwing;
            }
            return;
        }

        // 1b. Sliding Overrides (legs tucked low)
        if (this.isSliding) {
            if (this.blenderLegL && this.blenderLegL.initialQuaternion && this.blenderLegR && this.blenderLegR.initialQuaternion) {
                const qSlideL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), 1.2);
                const qSlideR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), 1.0);
                this.blenderLegL.quaternion.copy(this.blenderLegL.initialQuaternion).multiply(qSlideL);
                this.blenderLegR.quaternion.copy(this.blenderLegR.initialQuaternion).multiply(qSlideR);
                if (this.blenderCalfL && this.blenderCalfL.initialQuaternion) {
                    const qCalf = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -1.4);
                    this.blenderCalfL.quaternion.copy(this.blenderCalfL.initialQuaternion).multiply(qCalf);
                }
                if (this.blenderCalfR && this.blenderCalfR.initialQuaternion) {
                    const qCalf = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -1.3);
                    this.blenderCalfR.quaternion.copy(this.blenderCalfR.initialQuaternion).multiply(qCalf);
                }
            }
            if (this.blenderTorso && this.blenderTorso.initialQuaternion) {
                const qSlide = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), 0.35);
                this.blenderTorso.quaternion.copy(this.blenderTorso.initialQuaternion).multiply(qSlide);
            }
            if (this.blenderArmR && this.blenderArmR.initialQuaternion && this.blenderArmL && this.blenderArmL.initialQuaternion) {
                const qArmSlide = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -0.5);
                this.blenderArmL.quaternion.copy(this.blenderArmL.initialQuaternion).multiply(qArmSlide);
                this.blenderArmR.quaternion.copy(this.blenderArmR.initialQuaternion).multiply(qArmSlide);
            }
            return; // Skip remaining anim logic during slide
        }

        // 2. Swimming Animation Overrides
        if (this.isSwimming) {
            const swimPhase = time * 3.5;
            const armForward = Math.sin(swimPhase);
            const armOut = Math.cos(swimPhase);

            if (this.blenderArmR && this.blenderArmR.initialQuaternion && this.blenderArmL && this.blenderArmL.initialQuaternion) {
                const qSwR = new THREE.Quaternion().setFromEuler(new THREE.Euler(-1.0 + armForward * 0.45, 0.35 * armOut, -0.25));
                const qSwL = new THREE.Quaternion().setFromEuler(new THREE.Euler(-1.0 - armForward * 0.45, -0.35 * armOut, 0.25));
                this.blenderArmR.quaternion.copy(this.blenderArmR.initialQuaternion).multiply(qSwR);
                this.blenderArmL.quaternion.copy(this.blenderArmL.initialQuaternion).multiply(qSwL);
            }

            if (this.blenderLegL && this.blenderLegL.initialQuaternion && this.blenderLegR && this.blenderLegR.initialQuaternion) {
                const kickPhase = Math.sin(time * 4.0) * 0.35;
                const qLegL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), kickPhase);
                const qLegR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -kickPhase);
                this.blenderLegL.quaternion.copy(this.blenderLegL.initialQuaternion).multiply(qLegL);
                this.blenderLegR.quaternion.copy(this.blenderLegR.initialQuaternion).multiply(qLegR);
            }

            if (this.leftLeg && this.rightLeg) {
                this.leftLeg.rotation.x = Math.sin(time * 4.0) * 0.4;
                this.rightLeg.rotation.x = -Math.sin(time * 4.0) * 0.4;
            }
            if (this.weaponMount) {
                this.weaponMount.position.set(0.12, 0.95, -0.2);
                this.weaponMount.rotation.set(-1.25, Math.PI, 0);
            }
            return;
        }

        const isFistActive = !activeWeapon || activeWeapon.isMelee;

        // 3. Dynamic Locomotion & Posture System (Blender Model Bones)
        if (this.blenderLegL && this.blenderLegL.initialQuaternion && this.blenderLegR && this.blenderLegR.initialQuaternion) {
            if (this.posture === 'CROUCH') {
                const crouchThigh = 1.05;
                const crouchKnee = -1.35;
                if (isMoving && this.isGrounded) {
                    const stride = Math.sin(time * 1.3) * 0.25;
                    const qL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), crouchThigh + stride);
                    const qR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), crouchThigh - stride);
                    this.blenderLegL.quaternion.copy(this.blenderLegL.initialQuaternion).multiply(qL);
                    this.blenderLegR.quaternion.copy(this.blenderLegR.initialQuaternion).multiply(qR);

                    if (this.blenderCalfL && this.blenderCalfL.initialQuaternion && this.blenderCalfR && this.blenderCalfR.initialQuaternion) {
                        const qkL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), crouchKnee - Math.max(0, stride * 0.8));
                        const qkR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), crouchKnee - Math.max(0, -stride * 0.8));
                        this.blenderCalfL.quaternion.copy(this.blenderCalfL.initialQuaternion).multiply(qkL);
                        this.blenderCalfR.quaternion.copy(this.blenderCalfR.initialQuaternion).multiply(qkR);
                    }
                } else if (this.isGrounded) {
                    const qL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), crouchThigh);
                    const qR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), crouchThigh + 0.1);
                    this.blenderLegL.quaternion.copy(this.blenderLegL.initialQuaternion).multiply(qL);
                    this.blenderLegR.quaternion.copy(this.blenderLegR.initialQuaternion).multiply(qR);

                    if (this.blenderCalfL && this.blenderCalfL.initialQuaternion && this.blenderCalfR && this.blenderCalfR.initialQuaternion) {
                        const qkL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), crouchKnee);
                        const qkR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), crouchKnee - 0.08);
                        this.blenderCalfL.quaternion.copy(this.blenderCalfL.initialQuaternion).multiply(qkL);
                        this.blenderCalfR.quaternion.copy(this.blenderCalfR.initialQuaternion).multiply(qkR);
                    }
                }
            } else if (this.posture === 'PRONE') {
                if (isMoving) {
                    const crawl = Math.sin(time * 3.0) * 0.32;
                    const qL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), crawl);
                    const qR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -crawl);
                    this.blenderLegL.quaternion.copy(this.blenderLegL.initialQuaternion).multiply(qL);
                    this.blenderLegR.quaternion.copy(this.blenderLegR.initialQuaternion).multiply(qR);
                } else {
                    this.blenderLegL.quaternion.copy(this.blenderLegL.initialQuaternion);
                    this.blenderLegR.quaternion.copy(this.blenderLegR.initialQuaternion);
                }
            } else if (isMoving && this.isGrounded) {
                const freq = this.isSprinting ? 1.8 : 1.25;
                const stride = this.isSprinting ? 0.65 : 0.42;
                const legAngle = Math.sin(time * freq) * stride;

                // Thigh Swings
                const qL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), legAngle);
                this.blenderLegL.quaternion.copy(this.blenderLegL.initialQuaternion).multiply(qL);

                const qR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -legAngle);
                this.blenderLegR.quaternion.copy(this.blenderLegR.initialQuaternion).multiply(qR);

                // Knee Bends (bends backward when leg is back)
                if (this.blenderCalfL && this.blenderCalfL.initialQuaternion) {
                    const kneeL = Math.max(0, -legAngle * 1.35);
                    const qkL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), kneeL);
                    this.blenderCalfL.quaternion.copy(this.blenderCalfL.initialQuaternion).multiply(qkL);
                }
                if (this.blenderCalfR && this.blenderCalfR.initialQuaternion) {
                    const kneeR = Math.max(0, legAngle * 1.35);
                    const qkR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), kneeR);
                    this.blenderCalfR.quaternion.copy(this.blenderCalfR.initialQuaternion).multiply(qkR);
                }
            } else if (this.isGrounded) {
                // Standing Idle - restore clean natural rest pose
                this.blenderLegL.quaternion.slerp(this.blenderLegL.initialQuaternion, dt * 12);
                this.blenderLegR.quaternion.slerp(this.blenderLegR.initialQuaternion, dt * 12);
                if (this.blenderCalfL && this.blenderCalfL.initialQuaternion) {
                    this.blenderCalfL.quaternion.slerp(this.blenderCalfL.initialQuaternion, dt * 12);
                }
                if (this.blenderCalfR && this.blenderCalfR.initialQuaternion) {
                    this.blenderCalfR.quaternion.slerp(this.blenderCalfR.initialQuaternion, dt * 12);
                }
            }

            // Jump in air tuck
            if (!this.isGrounded && !this.isSkydiving && !this.isSwimming) {
                const qJump = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), 0.35);
                this.blenderLegL.quaternion.copy(this.blenderLegL.initialQuaternion).multiply(qJump);
                this.blenderLegR.quaternion.copy(this.blenderLegR.initialQuaternion).multiply(qJump);
                if (this.blenderCalfL && this.blenderCalfL.initialQuaternion) {
                    const qCalf = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), 0.45);
                    this.blenderCalfL.quaternion.copy(this.blenderCalfL.initialQuaternion).multiply(qCalf);
                }
                if (this.blenderCalfR && this.blenderCalfR.initialQuaternion) {
                    const qCalf = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), 0.45);
                    this.blenderCalfR.quaternion.copy(this.blenderCalfR.initialQuaternion).multiply(qCalf);
                }
            }
        }

        // Procedural Fallback Legs
        if (this.leftLeg && this.rightLeg) {
            if (this.posture === 'CROUCH') {
                this.leftLeg.rotation.x = 1.05;
                this.rightLeg.rotation.x = 1.15;
            } else if (this.posture === 'PRONE') {
                this.leftLeg.rotation.x = 0;
                this.rightLeg.rotation.x = 0;
            } else if (isMoving && this.isGrounded) {
                const freq = this.isSprinting ? 1.8 : 1.25;
                const legAngle = Math.sin(time * freq) * (this.isSprinting ? 0.75 : 0.50);
                this.leftLeg.rotation.x = legAngle;
                this.rightLeg.rotation.x = -legAngle;
                if (this.torso) this.torso.rotation.x = this.isSprinting ? 0.15 : 0.08;
            } else {
                this.leftLeg.rotation.x = 0;
                this.rightLeg.rotation.x = 0;
                if (this.torso) this.torso.rotation.x = Math.sin(time * 0.4) * 0.02;
            }
            if (!this.isGrounded && !this.isSwimming) {
                this.leftLeg.rotation.x = 0.35;
                this.rightLeg.rotation.x = 0.45;
            }
        }

        // 4. Torso Breathing & Lean
        if (this.blenderTorso && this.blenderTorso.initialQuaternion) {
            if (this.posture === 'CROUCH') {
                const qTorso = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), 0.22);
                this.blenderTorso.quaternion.copy(this.blenderTorso.initialQuaternion).multiply(qTorso);
            } else if (this.posture === 'PRONE') {
                this.blenderTorso.quaternion.copy(this.blenderTorso.initialQuaternion);
            } else {
                const freq = this.isSprinting ? 1.8 : 1.25;
                const bounce = (isMoving && this.isGrounded) ? Math.abs(Math.sin(time * freq)) * 0.04 : Math.sin(time * 0.3) * 0.015;
                const lean = (this.isSprinting && isMoving) ? 0.12 : ((isMoving) ? 0.05 : 0);
                const qTorso = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), lean + bounce);
                this.blenderTorso.quaternion.copy(this.blenderTorso.initialQuaternion).multiply(qTorso);
            }
        }

        // 4. Arms & Combat Stance
        if (this.blenderArmR && this.blenderArmR.initialQuaternion && this.blenderArmL && this.blenderArmL.initialQuaternion) {
            if (isFistActive) {
                if (this.punchTimer > 0) {
                    // Powerful punch thrust forward
                    const punchProg = this.punchTimer / 0.28;
                    const punchThrust = Math.sin(punchProg * Math.PI) * 0.7;

                    if (this.punchArm === 0) {
                        const qP = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -1.1 - punchThrust);
                        this.blenderArmR.quaternion.copy(this.blenderArmR.initialQuaternion).multiply(qP);
                        const qG = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -0.6);
                        this.blenderArmL.quaternion.copy(this.blenderArmL.initialQuaternion).multiply(qG);
                    } else {
                        const qP = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -1.1 - punchThrust);
                        this.blenderArmL.quaternion.copy(this.blenderArmL.initialQuaternion).multiply(qP);
                        const qG = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -0.6);
                        this.blenderArmR.quaternion.copy(this.blenderArmR.initialQuaternion).multiply(qG);
                    }
                } else if (isMoving && this.isGrounded) {
                    // Natural arm swing while running with bare fists
                    const freq = this.isSprinting ? 1.8 : 1.25;
                    const armSwing = Math.sin(time * freq) * (this.isSprinting ? 0.45 : 0.28);
                    const qArmL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), armSwing - 0.15);
                    const qArmR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -armSwing - 0.15);
                    this.blenderArmL.quaternion.copy(this.blenderArmL.initialQuaternion).multiply(qArmL);
                    this.blenderArmR.quaternion.copy(this.blenderArmR.initialQuaternion).multiply(qArmR);
                } else {
                    // Idle relaxed natural stance
                    const idleBob = Math.sin(time * 0.3) * 0.02;
                    const qIdleL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -0.1 + idleBob);
                    const qIdleR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -0.1 - idleBob);
                    this.blenderArmL.quaternion.copy(this.blenderArmL.initialQuaternion).multiply(qIdleL);
                    this.blenderArmR.quaternion.copy(this.blenderArmR.initialQuaternion).multiply(qIdleR);
                }
            } else {
                // Tactical Two-Handed Gun Grip
                const pitchAngle = -this.pitch * 0.4;
                const qGunR = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -0.9 + pitchAngle);
                const qGunL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -0.85 + pitchAngle);
                this.blenderArmR.quaternion.copy(this.blenderArmR.initialQuaternion).multiply(qGunR);
                this.blenderArmL.quaternion.copy(this.blenderArmL.initialQuaternion).multiply(qGunL);

                if (this.blenderForearmL && this.blenderForearmL.initialQuaternion) {
                    const qForeL = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(0, 1, 0), 0.3);
                    this.blenderForearmL.quaternion.copy(this.blenderForearmL.initialQuaternion).multiply(qForeL);
                }
            }
        }

        // Procedural Fallback Arms & Gun Mount
        if (!isFistActive) {
            const isPistol = (activeWeapon && activeWeapon.id === 'DEAGLE');
            const hipX = isPistol ? 0.08 : 0.12;
            const hipY = 1.22;
            const hipZ = isPistol ? -0.28 : -0.34;

            const adsX = 0.00;
            const adsY = 1.28;
            const adsZ = isPistol ? -0.32 : -0.40;

            const currentX = THREE.MathUtils.lerp(hipX, adsX, this.adsWeight);
            const currentY = THREE.MathUtils.lerp(hipY, adsY, this.adsWeight);
            const currentZ = THREE.MathUtils.lerp(hipZ, adsZ, this.adsWeight);

            let swayX = 0, swayY = 0;
            if (isMoving && this.isGrounded) {
                const swayDamp = (1.0 - this.adsWeight * 0.8);
                swayX = Math.sin(time * 0.8) * 0.02 * swayDamp;
                swayY = Math.abs(Math.cos(time * 1.6)) * 0.015 * swayDamp;
            }

            this.weaponMount.position.set(
                currentX + swayX + this.recoilYaw,
                currentY + swayY,
                currentZ + this.recoilZ
            );
            this.weaponMount.rotation.set(this.pitch - this.recoilPitch, Math.PI, 0);

            if (this.armR) {
                const rArmX = THREE.MathUtils.lerp(-1.18, -1.35, this.adsWeight) + this.recoilPitch * 0.5;
                this.armR.rotation.set(rArmX, -0.12, 0.15);
            }
            if (this.armL) {
                const lArmX = THREE.MathUtils.lerp(-1.08, -1.25, this.adsWeight);
                this.armL.rotation.set(lArmX, 0.38, -0.25);
            }
        } else if (this.armR && this.armL) {
            if (this.punchTimer > 0) {
                const punchProg = this.punchTimer / 0.28;
                const punchThrust = Math.sin(punchProg * Math.PI) * 1.5;
                if (this.punchArm === 0) {
                    this.armR.rotation.set(-1.4 - punchThrust * 0.3, 0.2, 0.4);
                } else {
                    this.armL.rotation.set(-1.4 - punchThrust * 0.3, -0.2, -0.4);
                }
            } else {
                this.armR.rotation.set(-0.9, -0.3, 0.4);
                this.armL.rotation.set(-0.9, 0.3, -0.4);
            }
        }

        // 5. Head smoothly tracks pitch
        if (this.blenderHead && this.blenderHead.initialQuaternion) {
            const headPitch = (this.posture === 'PRONE') ? -0.65 : ((this.posture === 'CROUCH') ? -0.15 : 0);
            const qHead = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), headPitch - this.pitch * 0.5);
            this.blenderHead.quaternion.copy(this.blenderHead.initialQuaternion).multiply(qHead);
        } else if (this.head) {
            this.head.rotation.x = -this.pitch * 0.8;
        }
    }

    addCameraTrauma(amount) {
        this.cameraTrauma = Math.min(1.0, this.cameraTrauma + amount);
    }

    updateCamera(dt, isAiming, activeWeapon) {
        let targetDist = isAiming ? 2.0 : (this.isSkydiving ? 5.2 : (this.isSwimming ? 3.6 : 3.6));
        if (isAiming && activeWeapon && activeWeapon.hasScope) {
            targetDist = 1.0;
        }
        this.cameraDistance = THREE.MathUtils.lerp(this.cameraDistance, targetDist, dt * 10);

        // Trauma camera shake
        let shakeX = 0, shakeY = 0, shakeZ = 0;
        if (this.cameraTrauma > 0.001) {
            this.cameraTrauma = Math.max(0, this.cameraTrauma - dt * 2.2);
            const traumaSq = this.cameraTrauma * this.cameraTrauma;
            shakeX = (Math.random() - 0.5) * traumaSq * 0.45;
            shakeY = (Math.random() - 0.5) * traumaSq * 0.45;
            shakeZ = (Math.random() - 0.5) * traumaSq * 0.45;
        }

        const horizDist = Math.cos(this.pitch) * this.cameraDistance;
        const camY = this.position.y + this.cameraHeightOffset + Math.sin(this.pitch) * this.cameraDistance;

        const shoulderOffset = (this.isSkydiving || this.isSwimming ? 0 : 0.52 * (1.0 - this.adsWeight * 0.85));
        const shoulderVec = new THREE.Vector3(shoulderOffset, 0, 0).applyAxisAngle(new THREE.Vector3(0, 1, 0), this.rotationY);

        const camX = this.position.x + Math.sin(this.rotationY) * horizDist + shoulderVec.x;
        const camZ = this.position.z + Math.cos(this.rotationY) * horizDist + shoulderVec.z;

        this.camera.position.set(camX + shakeX, Math.max(0.3, camY + shakeY), camZ + shakeZ);

        const targetDistAhead = 25.0;
        const lookTarget = new THREE.Vector3(
            this.position.x - Math.sin(this.rotationY) * targetDistAhead + shoulderVec.x * 0.35,
            this.position.y + this.cameraHeightOffset - Math.tan(this.pitch) * targetDistAhead,
            this.position.z - Math.cos(this.rotationY) * targetDistAhead + shoulderVec.z * 0.35
        );
        this.camera.lookAt(lookTarget);

        // Fast Aim-Raycast for Dynamic Tactical Crosshair (Turns RED when hovering over enemy bot!)
        if (window.gameEngine && window.gameEngine.botManager && window.hud) {
            let targetLocked = false;
            const camForward = new THREE.Vector3(
                Math.sin(this.rotationY),
                Math.tan(-this.pitch),
                Math.cos(this.rotationY)
            ).normalize();

            const origin = this.camera.position;
            const maxLockDist = (activeWeapon && activeWeapon.range) ? Math.min(80, activeWeapon.range) : 45;

            for (const bot of window.gameEngine.botManager.bots) {
                if (!bot.isAlive || bot.inPlane || bot.isSkydiving) continue;
                const toBot = new THREE.Vector3().subVectors(bot.position, origin);
                const proj = toBot.dot(camForward);
                if (proj > 2.0 && proj < maxLockDist) {
                    const closestPt = origin.clone().addScaledVector(camForward, proj);
                    const lateralDist = closestPt.distanceTo(bot.position);
                    if (lateralDist < 1.1 && closestPt.y >= bot.position.y - 0.2 && closestPt.y <= bot.position.y + 2.2) {
                        targetLocked = true;
                        break;
                    }
                }
            }
            const isMoving = this.velocity.clone().setY(0).length() > 0.5;
            const isFiring = (performance.now() * 0.001 - (window.inventory ? window.inventory.lastFireTime : 0)) < 0.15;
            const isCrouch = (this.posture === 'CROUCH');
            window.hud.setCrosshairState(isMoving, isFiring, isCrouch, targetLocked);
        }
    }
}

window.PlayerController = PlayerController;
