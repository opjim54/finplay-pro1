// War Land 3D - Smart Tactical AI NPC Bot Engine (25 Bots) (v4.2)
// Diverse 3D Character Archetypes, Two-Handed Gun Mount, Realistic Combat & Patrol

const BOT_NAMES = [
    "Ghost_Sniper", "Viper_99", "Delta_Fox", "Shadow_Rider", "Cyber_Rex",
    "Blaze_Killa", "Titan_Alpha", "Frost_Byte", "Raven_X", "Havoc_Storm",
    "Iron_Fist", "Cobra_Strike", "Apex_Predator", "Phantom_Ops", "Rogue_Wave",
    "Bullet_Rain", "Savage_Beast", "Zero_Cool", "Night_Crawler", "Skull_Crusher",
    "Dead_Eye", "War_Machine", "Thunder_Bolt", "Eagle_Eye", "Wolf_Pack"
];

// Roster of Diverse 3D Character Archetypes (Female Operatives Only: Freya, Asian Agent, Agent Sophia)
const BOT_ARCHETYPES = ['freya', 'female', 'asian', 'sophia', 'freya', 'asian', 'sophia', 'freya'];

class BotAi {
    constructor(scene, map, id, name, spawnPos) {
        this.scene = scene;
        this.map = map;
        this.id = id;
        this.name = name;

        this.position = spawnPos.clone();
        this.targetGroundPos = spawnPos.clone();
        this.targetGroundPos.y = 0;
        this.velocity = new THREE.Vector3();
        this.rotationY = Math.random() * Math.PI * 2;
        this.hp = 100;
        this.maxHp = 100;
        this.isAlive = true;

        // Airdrop Flight & Parachute Deployment State
        this.inPlane = true;
        this.planeDropProgress = 0.06 + (id / 25) * 0.82;
        this.isSkydiving = false;
        this.isParachuteOpen = false;
        this.skydiveSpeed = -20.0 - Math.random() * 4.0;
        this.parachuteGlideSpeed = -5.0 - Math.random() * 1.5;
        this.chuteOpenAlt = 42.0 + Math.random() * 14.0;

        this.state = 'PATROL'; // 'PATROL', 'COMBAT', 'COVER'
        this.targetPos = spawnPos.clone();
        this.stateTimer = Math.random() * 4.0;
        this.fireCooldown = 0;
        this.aimReactionTimer = 0;
        this.strafeDir = 1;

        // Recoil, Grounding & Performance Throttling Variables
        this.recoilZ = 0;
        this.recoilPitch = 0;
        this.isGrounded = true;
        this.isDying = false;
        this.deathTimer = 0;
        this.animAccumDt = 0;
        this.animFrameCounter = id % 4;
        this.posture = 'STAND';

        // Visual Mesh (initially hidden inside plane)
        this.mesh = new THREE.Group();
        this.mesh.visible = false;
        this.buildBotMesh();
        this.mesh.position.copy(this.position);
        this.scene.add(this.mesh);
    }

    buildBotMesh() {
        // 1. Pick a unique character archetype from diverse roster
        const charType = BOT_ARCHETYPES[this.id % BOT_ARCHETYPES.length];
        let bModel = null;
        if (window.blenderAssetLoader) {
            bModel = window.blenderAssetLoader.createCharacterInstance(charType);
            if (bModel) {
                bModel.rotation.y = Math.PI; // Align CC3 model facing -Z forward
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
                this.blenderLegL = findBone(bModel, ['mixamorig:LeftUpLeg', 'CC_Base_L_Thigh', 'thigh.L', 'DEF-thigh.L', 'LeftUpLeg', 'Char_Leg_L']);
                this.blenderLegR = findBone(bModel, ['mixamorig:RightUpLeg', 'CC_Base_R_Thigh', 'thigh.R', 'DEF-thigh.R', 'RightUpLeg', 'Char_Leg_R']);

                if (window.animationSystem && bModel.userData && bModel.userData.hasSkinnedMesh) {
                    window.animationSystem.attachToCharacter(bModel);
                }
            }
        }

        // Fallback procedural geometry if 3D model failed
        if (!bModel) {
            const colors = [0x15803D, 0x1E3A8A, 0x374151, 0x7C2D12, 0x4C1D95];
            const shirtColor = colors[this.id % colors.length];
            const skinMat = new THREE.MeshLambertMaterial({ color: 0xFDE047 });
            const shirtMat = new THREE.MeshStandardMaterial({ color: shirtColor, roughness: 0.6 });
            const pantsMat = new THREE.MeshStandardMaterial({ color: 0x1F2937, roughness: 0.7 });

            this.torso = new THREE.Mesh(new THREE.CylinderGeometry(0.24, 0.22, 0.75, 10), shirtMat);
            this.torso.position.y = 1.35;
            this.mesh.add(this.torso);

            this.head = new THREE.Mesh(new THREE.SphereGeometry(0.20, 10, 10), skinMat);
            this.head.position.y = 1.9;
            this.mesh.add(this.head);

            this.leftLeg = new THREE.Mesh(new THREE.CylinderGeometry(0.09, 0.07, 0.85, 8), pantsMat);
            this.leftLeg.position.set(-0.16, 0.45, 0);
            this.mesh.add(this.leftLeg);

            this.rightLeg = new THREE.Mesh(new THREE.CylinderGeometry(0.09, 0.07, 0.85, 8), pantsMat);
            this.rightLeg.position.set(0.16, 0.45, 0);
            this.mesh.add(this.rightLeg);
        }

        // Parachute Canopy Mount
        this.parachuteMount = new THREE.Group();
        this.parachuteMount.position.set(0, 1.38, 0);
        this.parachuteMount.visible = false;
        this.mesh.add(this.parachuteMount);

        if (window.blenderAssetLoader) {
            const pModel = window.blenderAssetLoader.createParachuteInstance();
            if (pModel) {
                this.parachuteMount.add(pModel);
            }
        }

        // 2. Weapon Mount Socket (Anchored to Upper Chest, in front)
        this.weaponMount = new THREE.Group();
        this.weaponMount.position.set(0.12, 1.25, -0.32);
        this.mesh.add(this.weaponMount);

        // Assign weapon by bot specialization
        let weaponKey = 'GUN_AK47';
        if (this.id % 4 === 1) weaponKey = 'GUN_AWM';
        else if (this.id % 4 === 2) weaponKey = 'GUN_SPAS12';
        else if (this.id % 4 === 3) weaponKey = 'GUN_DEAGLE';

        if (window.blenderAssetLoader) {
            const gun = window.blenderAssetLoader.createWeaponInstance(weaponKey);
            if (gun) {
                gun.position.set(0, 0, 0);
                gun.rotation.set(Math.PI, Math.PI, Math.PI);
                this.weaponMount.add(gun);
            }
        }

        // Muzzle Flash Point
        this.muzzlePoint = new THREE.Object3D();
        this.muzzlePoint.position.set(0, 0.015, -0.66);
        this.weaponMount.add(this.muzzlePoint);

        const flashMat = new THREE.MeshBasicMaterial({ color: 0xFDE047, transparent: true, opacity: 0 });
        this.flashMesh = new THREE.Mesh(new THREE.OctahedronGeometry(0.10), flashMat);
        this.flashMesh.position.copy(this.muzzlePoint.position);
        this.weaponMount.add(this.flashMesh);
    }

    hearGunshot(shotPos) {
        if (!this.isAlive) return;
        const dist = this.position.distanceTo(shotPos);
        if (dist < 65) {
            const dir = new THREE.Vector3().subVectors(shotPos, this.position).normalize();
            this.rotationY = Math.atan2(dir.x, dir.z) + Math.PI;
            this.state = 'COMBAT';
            this.targetPos.copy(shotPos);
        }
    }

    takeDamage(amount, attackerName = "Player") {
        if (!this.isAlive) return;
        this.hp -= amount;

        this.state = 'COMBAT';
        this.stateTimer = 5.0;

        if (this.hp <= 0) {
            this.eliminate(attackerName);
        } else if (
            this.blenderModel &&
            this.blenderModel.userData &&
            this.blenderModel.userData.hasSkinnedMesh &&
            window.animationSystem &&
            window.animationSystem.isReady
        ) {
            window.animationSystem.triggerAction(this.blenderModel, 'HIT', null, 180);
        }
    }

    eliminate(attackerName) {
        if (!this.isAlive) return;
        this.isAlive = false;
        this.isDying = true;
        this.deathTimer = 1.45;
        this.velocity.set(0, 0, 0);

        // Play Death Animation cleanly before removing corpse & spawning Death Box
        const hasRig = !!(
            this.blenderModel &&
            this.blenderModel.userData &&
            this.blenderModel.userData.hasSkinnedMesh &&
            window.animationSystem &&
            window.animationSystem.isReady
        );
        if (hasRig) {
            window.animationSystem.triggerAction(this.blenderModel, 'DEATH', null, 1800);
        }

        if (window.hud) {
            window.hud.addKillFeedEntry(attackerName, this.name);
            window.hud.updateAliveCount();
        }

        const isPlayer = (attackerName === "Player" || attackerName === "You" || attackerName === "Warrior_Pro" || attackerName === window.playerIgn);
        if (isPlayer && window.player) {
            window.player.kills++;
            if (window.hud && window.hud.killText) window.hud.killText.textContent = window.player.kills;
            if (window.soundEngine) window.soundEngine.playKillSound();
        }
    }

    update(dt, player) {
        // Play Death Animation cleanly before removing mesh & dropping Death Box
        if (!this.isAlive) {
            if (this.isDying) {
                this.deathTimer -= dt;
                const hasRig = !!(
                    this.blenderModel &&
                    this.blenderModel.userData &&
                    this.blenderModel.userData.hasSkinnedMesh &&
                    window.animationSystem &&
                    window.animationSystem.isReady
                );
                if (hasRig) {
                    window.animationSystem.update(dt, this.blenderModel, this, false, false);
                } else {
                    // Fall backward smoothly for static models
                    this.mesh.rotation.x = THREE.MathUtils.lerp(this.mesh.rotation.x, -Math.PI / 2, dt * 6);
                }
                if (this.deathTimer <= 0) {
                    this.isDying = false;
                    if (window.animationSystem && this.blenderModel) {
                        window.animationSystem.detachCharacter(this.blenderModel);
                    }
                    this.scene.remove(this.mesh);
                    if (window.gameEngine && window.gameEngine.lootManager) {
                        window.gameEngine.lootManager.spawnDeathBox(this.position);
                    }
                }
            }
            return;
        }

        // Waiting inside Airdrop Airplane
        if (this.inPlane) return;

        const distToPlayer = player ? this.position.distanceTo(player.position) : 0;
        const hasRig = !!(
            this.blenderModel &&
            this.blenderModel.userData &&
            this.blenderModel.userData.hasSkinnedMesh &&
            window.animationSystem &&
            window.animationSystem.isReady
        );

        // Distance visibility culling for high FPS on large 520m map
        this.mesh.visible = distToPlayer < 245 || this.isSkydiving;

        // ── SKYDIVING & PARACHUTE DESCENT ────────────────────────────────────
        if (this.isSkydiving) {
            if (this.position.y <= this.chuteOpenAlt && !this.isParachuteOpen) {
                this.isParachuteOpen = true;
                this.parachuteMount.visible = true;
            }

            const vy = this.isParachuteOpen ? this.parachuteGlideSpeed : this.skydiveSpeed;
            this.position.y += vy * dt;

            // Drift toward ground target
            const toTgt = new THREE.Vector3().subVectors(this.targetPos, this.position);
            toTgt.y = 0;
            if (toTgt.length() > 1.0) {
                toTgt.normalize();
                const driftSpd = this.isParachuteOpen ? 4.5 : 8.0;
                this.position.x += toTgt.x * driftSpd * dt;
                this.position.z += toTgt.z * driftSpd * dt;
                this.rotationY = Math.atan2(toTgt.x, toTgt.z) + Math.PI;
            }

            const colResult = this.map.resolveCollision(this.position, this.position, 0.4, 1.8);
            if (this.position.y <= colResult.groundY + 0.1) {
                this.position.y = colResult.groundY;
                this.isSkydiving = false;
                this.isParachuteOpen = false;
                this.isGrounded = true;
                this.parachuteMount.visible = false;
            }

            this.mesh.position.copy(this.position);
            this.mesh.rotation.y = this.rotationY;

            if (hasRig && this.mesh.visible) {
                window.animationSystem.update(dt, this.blenderModel, this, false, false);
            }
            return;
        }

        // Distance-throttled animation update for massive CPU/GPU savings
        if (hasRig && this.mesh.visible) {
            this.animAccumDt += dt;
            this.animFrameCounter++;
            const stride = distToPlayer < 60 ? 1 : (distToPlayer < 130 ? 2 : 4);
            if (this.animFrameCounter % stride === 0) {
                window.animationSystem.update(this.animAccumDt, this.blenderModel, this, this.fireCooldown > 0.75, this.state === 'COMBAT');
                this.animAccumDt = 0;
            }
        }

        // Smooth recoil recovery
        this.recoilZ = THREE.MathUtils.lerp(this.recoilZ, 0, dt * 14);
        this.recoilPitch = THREE.MathUtils.lerp(this.recoilPitch, 0, dt * 16);

        this.stateTimer -= dt;
        this.fireCooldown -= dt;

        // ── COMBAT TARGET SELECTION (Player OR Rival Bots!) ──────────────────
        let bestTarget = null;
        let bestDist = 45.0;

        // Check Player
        if (player && player.isAlive && !player.inPlane && !player.isSkydiving) {
            const d = this.position.distanceTo(player.position);
            if (d < bestDist) {
                bestDist = d;
                bestTarget = { isPlayer: true, entity: player, position: player.position };
            }
        }

        // Check Rival Bots for bot-vs-bot firefights!
        if (window.gameEngine && window.gameEngine.botManager) {
            for (const other of window.gameEngine.botManager.bots) {
                if (other === this || !other.isAlive || other.inPlane || other.isSkydiving) continue;
                const d = this.position.distanceTo(other.position);
                if (d < bestDist) {
                    bestDist = d;
                    bestTarget = { isPlayer: false, entity: other, position: other.position };
                }
            }
        }

        if (bestTarget) {
            if (this.state !== 'COMBAT') {
                this.state = 'COMBAT';
                this.aimReactionTimer = 1.2 + Math.random() * 0.8; // Human-like reaction delay
            }
            this.aimReactionTimer = Math.max(0, this.aimReactionTimer - dt);
        } else if (this.stateTimer <= 0) {
            this.state = (Math.random() < 0.65) ? 'PATROL' : 'COVER';
            this.stateTimer = 3.5 + Math.random() * 4.5;
            const angle = Math.random() * Math.PI * 2;
            const radius = 35 + Math.random() * 340;
            this.targetPos.set(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
        }

        let moveDir = new THREE.Vector3();
        let speed = 4.2;

        if (this.state === 'COMBAT' && bestTarget) {
            const dirToTarget = new THREE.Vector3().subVectors(bestTarget.position, this.position);
            dirToTarget.y = 0;
            const dist = dirToTarget.length();
            dirToTarget.normalize();

            // Face target
            this.rotationY = Math.atan2(dirToTarget.x, dirToTarget.z) + Math.PI;

            // Tactical strafe & maintain spacing
            if (dist > 24) {
                moveDir.copy(dirToTarget);
            } else if (dist < 9) {
                moveDir.copy(dirToTarget).negate();
            } else {
                const right = new THREE.Vector3(-dirToTarget.z, 0, dirToTarget.x);
                moveDir.copy(right).multiplyScalar(this.strafeDir);
                if (Math.random() < 0.02) this.strafeDir *= -1;
            }

            // Fire burst at target after reaction delay
            if (this.aimReactionTimer <= 0 && this.fireCooldown <= 0 && dist < 45) {
                this.fireAtTarget(bestTarget);
                this.fireCooldown = 0.95 + Math.random() * 0.65;
            }
        } else {
            // Patrol to target
            const toTarget = new THREE.Vector3().subVectors(this.targetPos, this.position);
            toTarget.y = 0;
            if (toTarget.length() > 1.5) {
                moveDir.copy(toTarget.normalize());
                this.rotationY = Math.atan2(moveDir.x, moveDir.z) + Math.PI;
            }
        }

        // Movement velocity
        this.velocity.x = moveDir.x * speed;
        this.velocity.z = moveDir.z * speed;
        this.velocity.y -= 25.0 * dt;

        const nextPos = this.position.clone();
        nextPos.x += this.velocity.x * dt;
        nextPos.y += this.velocity.y * dt;
        nextPos.z += this.velocity.z * dt;

        const colResult = this.map.resolveCollision(this.position, nextPos, 0.4, 1.8);
        this.position.copy(colResult.position);

        if (this.position.y <= colResult.groundY) {
            this.position.y = colResult.groundY;
            this.velocity.y = 0;
        }

        this.mesh.position.copy(this.position);
        this.mesh.rotation.y = this.rotationY;

        // Limb walk animation & Two-Handed Gun Posing
        const isMoving = moveDir.length() > 0.1;
        const time = performance.now() * 0.008;

        if (isMoving) {
            const legAngle = Math.sin(time * 1.1) * 0.65;
            if (this.leftLeg) this.leftLeg.rotation.x = legAngle;
            if (this.rightLeg) this.rightLeg.rotation.x = -legAngle;
        } else {
            if (this.leftLeg) this.leftLeg.rotation.x = 0;
            if (this.rightLeg) this.rightLeg.rotation.x = 0;
        }

        if (this.weaponMount) {
            this.weaponMount.position.z = -0.32 + this.recoilZ;
            this.weaponMount.rotation.set(-this.recoilPitch, 0, 0);
        }
    }

    fireAtTarget(target) {
        // Recoil kick
        this.recoilZ = 0.06;
        this.recoilPitch = 0.10;

        // Muzzle flash
        if (this.flashMesh) {
            this.flashMesh.material.opacity = 0.9;
            setTimeout(() => {
                if (this.flashMesh) this.flashMesh.material.opacity = 0;
            }, 50);
        }

        // Sound
        if (window.soundEngine && window.player) {
            const dist = this.position.distanceTo(window.player.position);
            if (dist < 50) window.soundEngine.playShoot('AK47');
        }

        // Bullet tracer line from muzzle to target
        const startPoint = new THREE.Vector3();
        this.muzzlePoint.getWorldPosition(startPoint);

        const targetPoint = target.position.clone().add(new THREE.Vector3(
            (Math.random() - 0.5) * 0.5,
            1.2 + (Math.random() - 0.5) * 0.35,
            (Math.random() - 0.5) * 0.5
        ));

        // Line of sight obstacle check: cannot shoot through walls or Gloo Walls!
        const rayDir = new THREE.Vector3().subVectors(targetPoint, startPoint).normalize();
        const distToTgt = startPoint.distanceTo(targetPoint);
        let blocked = false;

        if (window.gameEngine) {
            // Check building colliders
            if (window.gameEngine.map) {
                for (const col of window.gameEngine.map.colliders) {
                    if (!col.isObstacle) continue;
                    const hitD = window.gameEngine.rayIntersectsAABB(startPoint, rayDir, col.min, col.max, distToTgt);
                    if (hitD !== null && hitD < distToTgt - 0.3) {
                        blocked = true;
                        window.gameEngine.spawnBulletTracer(startPoint, startPoint.clone().addScaledVector(rayDir, hitD));
                        break;
                    }
                }
            }

            // Check Gloo Walls
            if (!blocked && window.gameEngine.glooWalls) {
                for (const gw of window.gameEngine.glooWalls) {
                    if (gw.isDestroyed || !gw.collider) continue;
                    const hitD = window.gameEngine.rayIntersectsAABB(startPoint, rayDir, gw.collider.min, gw.collider.max, distToTgt);
                    if (hitD !== null && hitD < distToTgt - 0.3) {
                        blocked = true;
                        gw.takeDamage(12);
                        window.gameEngine.spawnBulletTracer(startPoint, startPoint.clone().addScaledVector(rayDir, hitD));
                        break;
                    }
                }
            }
        }

        if (!blocked) {
            // Human-like hit accuracy vs spread miss
            const hitChance = target.isPlayer ? 0.32 : 0.45;
            const isHit = Math.random() < hitChance;

            if (isHit) {
                if (window.gameEngine) {
                    window.gameEngine.spawnBulletTracer(startPoint, targetPoint, 0xFDE047);
                }

                if (target.isPlayer) {
                    // Balanced, non-lethal damage (3 to 6 damage per hit)
                    target.entity.takeDamage(3.0 + Math.random() * 3.0);
                } else {
                    // Damage against rival bot
                    target.entity.takeDamage(15.0 + Math.random() * 8.0, this.name);
                }
            } else {
                // Near miss tracer flying past target!
                const missPoint = targetPoint.clone().add(new THREE.Vector3(
                    (Math.random() - 0.5) * 4.2,
                    (Math.random() - 0.5) * 2.8,
                    (Math.random() - 0.5) * 4.2
                ));
                if (window.gameEngine) {
                    window.gameEngine.spawnBulletTracer(startPoint, missPoint, 0xF59E0B);
                }
            }
        }
    }
}

class BotManager {
    constructor(scene, map) {
        this.scene = scene;
        this.map = map;
        this.bots = [];
    }

    spawnBots(count = 25) {
        // Distributed across all districts of the expanded 520m WarLand island
        const spawns = [
            new THREE.Vector3(-45, 0, -55),
            new THREE.Vector3(-140, 0, -60),
            new THREE.Vector3(-220, 0, -140),
            new THREE.Vector3(-180, 0, -240),
            new THREE.Vector3(-140, 0, 80),
            new THREE.Vector3(-230, 0, 120),
            new THREE.Vector3(95, 0, -90),
            new THREE.Vector3(170, 0, -120),
            new THREE.Vector3(240, 0, -160),
            new THREE.Vector3(130, 0, -240),
            new THREE.Vector3(220, 0, -260),
            new THREE.Vector3(210, 0, 80),
            new THREE.Vector3(260, 0, 180),
            new THREE.Vector3(220, 0, 270),
            new THREE.Vector3(60, 0, 120),
            new THREE.Vector3(-75, 0, 165),
            new THREE.Vector3(-125, 0, 250),
            new THREE.Vector3(45, 0, 240),
            new THREE.Vector3(145, 0, 230),
            new THREE.Vector3(-280, 0, -40),
            new THREE.Vector3(310, 0, -40),
            new THREE.Vector3(0, 0, -260),
            new THREE.Vector3(0, 0, 260),
            new THREE.Vector3(-260, 0, 210),
            new THREE.Vector3(320, 0, 140)
        ];

        for (let i = 0; i < count; i++) {
            const spawnPos = spawns[i % spawns.length].clone();
            spawnPos.x += (Math.random() - 0.5) * 20;
            spawnPos.z += (Math.random() - 0.5) * 20;
            spawnPos.y = 140 + Math.random() * 28;

            const name = BOT_NAMES[i % BOT_NAMES.length];
            const bot = new BotAi(this.scene, this.map, i, name, spawnPos);
            bot.mesh.visible = false;
            this.bots.push(bot);
        }
    }

    update(dt, player) {
        for (const bot of this.bots) {
            bot.update(dt, player);
        }
    }

    updatePlaneDrops(flightProgress, planePos) {
        for (const bot of this.bots) {
            if (bot.inPlane && flightProgress >= bot.planeDropProgress) {
                bot.inPlane = false;
                bot.isSkydiving = true;
                bot.position.copy(planePos);
                bot.position.x += (Math.random() - 0.5) * 14;
                bot.position.z += (Math.random() - 0.5) * 14;
                bot.mesh.position.copy(bot.position);
                bot.mesh.visible = true;
            }
        }
    }

    notifyGunshot(shotPos) {
        for (const bot of this.bots) {
            bot.hearGunshot(shotPos);
        }
    }

    getAliveCount() {
        return this.bots.filter(b => b.isAlive).length;
    }

    getLivingBots() {
        return this.bots.filter(b => b.isAlive);
    }
}

window.BotManager = BotManager;
