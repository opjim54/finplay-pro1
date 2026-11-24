// War Land 3D - Weapons, Ballistics, Grenade Physics & Loot System

const WEAPON_TYPES = {
    FISTS: {
        id: 'FISTS',
        name: 'Bare Fists',
        slot: 0,
        damage: 25,
        headshotMultiplier: 1.5,
        fireRate: 0.35,
        magSize: 0,
        maxReserveAmmo: 0,
        reloadTime: 0,
        range: 2.2,
        spread: 0,
        colorHex: 0xF59E0B,
        sound: 'playPunch',
        isMelee: true
    },
    AK47: {
        id: 'AK47',
        name: 'Dragon AK-47',
        slot: 1,
        damage: 34,
        headshotMultiplier: 2.0,
        fireRate: 0.11, // seconds per round
        magSize: 30,
        maxReserveAmmo: 180,
        reloadTime: 2.0,
        range: 150,
        spread: 0.024,
        colorHex: 0xEA580C,
        sound: 'playRifleShot',
        isPrimary: true
    },
    AWM: {
        id: 'AWM',
        name: 'Golden AWM Sniper',
        slot: 2,
        damage: 155,
        headshotMultiplier: 2.5,
        fireRate: 1.2,
        magSize: 5,
        maxReserveAmmo: 30,
        reloadTime: 2.8,
        range: 400,
        spread: 0.002,
        colorHex: 0xF59E0B,
        sound: 'playSniperShot',
        hasScope: true,
        scopeZoomFov: 20,
        isPrimary: true
    },
    SPAS12: {
        id: 'SPAS12',
        name: 'Cyber Shotgun',
        slot: 1,
        damage: 20, // per pellet (8 pellets)
        pellets: 8,
        headshotMultiplier: 1.5,
        fireRate: 0.65,
        magSize: 8,
        maxReserveAmmo: 48,
        reloadTime: 2.3,
        range: 45,
        spread: 0.085,
        colorHex: 0x3B82F6,
        sound: 'playShotgunShot',
        isPrimary: true
    },
    DEAGLE: {
        id: 'DEAGLE',
        name: 'Desert Eagle',
        slot: 3,
        damage: 54,
        headshotMultiplier: 2.0,
        fireRate: 0.28,
        magSize: 7,
        maxReserveAmmo: 42,
        reloadTime: 1.5,
        range: 80,
        spread: 0.035,
        colorHex: 0x94A3B8,
        sound: 'playPistolShot',
        isSecondary: true
    }
};

class WeaponInventory {
    constructor() {
        // Slot 0: Fists (always equipped)
        // Slot 1: Primary Weapon 1 (starts empty)
        // Slot 2: Primary Weapon 2 (starts empty)
        // Slot 3: Pistol / Secondary (starts empty)
        this.weapons = {
            0: { ...WEAPON_TYPES.FISTS, damage: this.getPunchDamage() },
            1: null,
            2: null,
            3: null
        };
        this.activeSlot = 0; // Starts with fists! Empty hands at match start
        this.grenadeCount = 3;
        this.medkitCount = 2;
        this.isReloading = false;
        this.reloadTimer = 0;
        this.lastFireTime = 0;
        this.isAiming = false;
    }

    getPunchDamage() {
        const pLevel = localStorage.getItem('warland_punch_power') || 'default';
        if (pLevel === 'god_fist') return 75;
        if (pLevel === 'cyber_knuckle') return 50;
        if (pLevel === 'iron_fist') return 35;
        return 25;
    }

    getActiveWeapon() {
        return this.weapons[this.activeSlot] || this.weapons[0];
    }

    switchSlot(slot) {
        if (slot === 0 || this.weapons[slot]) {
            this.activeSlot = slot;
            this.isReloading = false;
            this.isAiming = false;
            if (window.player) window.player.updateHolsteredWeapons();
            if (window.hud) window.hud.updateWeaponUi();
        }
    }

    pickupWeapon(itemType) {
        let typeDef = null;
        if (itemType === 'GUN_AK47' || itemType === 'AK47') typeDef = WEAPON_TYPES.AK47;
        else if (itemType === 'GUN_AWM' || itemType === 'AWM') typeDef = WEAPON_TYPES.AWM;
        else if (itemType === 'GUN_SPAS12' || itemType === 'SPAS12') typeDef = WEAPON_TYPES.SPAS12;
        else if (itemType === 'GUN_DEAGLE' || itemType === 'DEAGLE') typeDef = WEAPON_TYPES.DEAGLE;

        if (!typeDef) return false;

        if (typeDef.isSecondary) {
            // Pistol goes to Slot 3
            if (this.weapons[3] && this.weapons[3].id === typeDef.id) {
                this.weapons[3].reserveAmmo = Math.min(this.weapons[3].maxReserveAmmo, this.weapons[3].reserveAmmo + typeDef.magSize * 2);
            } else {
                this.weapons[3] = { ...typeDef, ammoInMag: typeDef.magSize, reserveAmmo: typeDef.magSize * 3 };
            }
            if (this.activeSlot === 0) {
                this.activeSlot = 3;
            }
        } else {
            // Primary weapon (Slots 1 & 2)
            if (!this.weapons[1]) {
                this.weapons[1] = { ...typeDef, ammoInMag: typeDef.magSize, reserveAmmo: typeDef.magSize * 3 };
                if (this.activeSlot === 0) this.activeSlot = 1;
            } else if (!this.weapons[2]) {
                this.weapons[2] = { ...typeDef, ammoInMag: typeDef.magSize, reserveAmmo: typeDef.magSize * 3 };
                if (this.activeSlot === 0) this.activeSlot = 2;
            } else {
                // If already carrying this weapon, add ammo
                if (this.weapons[1].id === typeDef.id) {
                    this.weapons[1].reserveAmmo = Math.min(this.weapons[1].maxReserveAmmo, this.weapons[1].reserveAmmo + typeDef.magSize * 2);
                } else if (this.weapons[2].id === typeDef.id) {
                    this.weapons[2].reserveAmmo = Math.min(this.weapons[2].maxReserveAmmo, this.weapons[2].reserveAmmo + typeDef.magSize * 2);
                } else {
                    // Replace active slot if 1 or 2, else replace slot 1
                    const targetSlot = (this.activeSlot === 2) ? 2 : 1;
                    this.weapons[targetSlot] = { ...typeDef, ammoInMag: typeDef.magSize, reserveAmmo: typeDef.magSize * 3 };
                    this.activeSlot = targetSlot;
                }
            }
        }

        if (window.player) window.player.updateHolsteredWeapons();
        if (window.hud) window.hud.updateWeaponUi();
        return true;
    }

    startReload() {
        const weapon = this.getActiveWeapon();
        if (!weapon || weapon.isMelee) return false;
        if (this.isReloading || weapon.ammoInMag >= weapon.magSize || weapon.reserveAmmo <= 0) {
            return false;
        }
        this.isReloading = true;
        this.reloadTimer = weapon.reloadTime;
        if (window.soundEngine) window.soundEngine.playReload();
        if (window.player && typeof window.player.onReloadStarted === 'function') {
            window.player.onReloadStarted(weapon.reloadTime);
        }
        return true;
    }

    update(dt) {
        if (this.isReloading) {
            this.reloadTimer -= dt;
            if (this.reloadTimer <= 0) {
                const weapon = this.getActiveWeapon();
                if (weapon && !weapon.isMelee) {
                    const needed = weapon.magSize - weapon.ammoInMag;
                    const toAdd = Math.min(needed, weapon.reserveAmmo);
                    weapon.ammoInMag += toAdd;
                    weapon.reserveAmmo -= toAdd;
                }
                this.isReloading = false;
                if (window.hud) window.hud.updateWeaponUi();
            }
        }
    }
}

// Active Grenade in flight
class ActiveGrenade {
    constructor(scene, startPos, velocity) {
        this.scene = scene;
        this.position = startPos.clone();
        this.velocity = velocity.clone();
        this.fuseTimer = 2.8;
        this.exploded = false;

        // Visual 3D Grenade Mesh
        const geom = new THREE.DodecahedronGeometry(0.25, 1);
        const mat = new THREE.MeshLambertMaterial({ color: 0x166534, wireframe: false });
        this.mesh = new THREE.Mesh(geom, mat);
        this.mesh.position.copy(this.position);
        this.mesh.castShadow = true;
        this.scene.add(this.mesh);
    }

    update(dt, colliders, onExplode) {
        if (this.exploded) return;
        this.fuseTimer -= dt;

        // Apply Gravity
        this.velocity.y -= 19.6 * dt;
        this.position.addScaledVector(this.velocity, dt);

        // Ground / Obstacle bounce
        if (this.position.y <= 0.25) {
            this.position.y = 0.25;
            this.velocity.y = -this.velocity.y * 0.45;
            this.velocity.x *= 0.75;
            this.velocity.z *= 0.75;
        }

        this.mesh.position.copy(this.position);
        this.mesh.rotation.x += this.velocity.z * dt * 2;
        this.mesh.rotation.z -= this.velocity.x * dt * 2;

        if (this.fuseTimer <= 0) {
            this.explode(onExplode);
        }
    }

    explode(onExplode) {
        this.exploded = true;
        this.scene.remove(this.mesh);
        if (window.soundEngine) window.soundEngine.playExplosion();

        // Spawn visual explosion fireball particles
        if (window.gameEngine) {
            window.gameEngine.spawnExplosionEffect(this.position);
        }
        if (onExplode) {
            onExplode(this.position, 12, 180);
        }
    }
}

// Free Fire Deployable Gloo Wall Shield
class GlooWallInstance {
    constructor(scene, pos, rotY) {
        this.scene = scene;
        this.position = pos.clone();
        this.rotY = rotY;
        this.hp = 350;
        this.maxHp = 350;
        this.lifetime = 30.0;
        this.isDestroyed = false;

        let mesh = null;
        if (window.blenderAssetLoader && window.blenderAssetLoader.models['gloowall']) {
            mesh = window.blenderAssetLoader.createGlooWallInstance(pos.x, pos.y, pos.z, rotY);
        }

        if (!mesh) {
            // Procedural ice curved shield fallback
            const geom = new THREE.CylinderGeometry(3.0, 3.0, 2.6, 16, 1, false, -Math.PI * 0.35, Math.PI * 0.7);
            const mat = new THREE.MeshStandardMaterial({
                color: 0x06B6D4,
                roughness: 0.15,
                metalness: 0.3,
                transparent: true,
                opacity: 0.85,
                side: THREE.DoubleSide
            });
            mesh = new THREE.Mesh(geom, mat);
            mesh.position.set(pos.x, pos.y + 1.3, pos.z);
            mesh.rotation.y = rotY;
            mesh.castShadow = true;
        }

        this.mesh = mesh;
        this.scene.add(this.mesh);

        // Register solid physical obstacle collider
        this.collider = {
            min: new THREE.Vector3(pos.x - 2.4, pos.y, pos.z - 1.2),
            max: new THREE.Vector3(pos.x + 2.4, pos.y + 2.8, pos.z + 1.2),
            isStairs: false,
            topY: pos.y + 2.8
        };

        if (window.gameEngine && window.gameEngine.map) {
            window.gameEngine.map.colliders.push(this.collider);
        }
    }

    takeDamage(dmg) {
        this.hp -= dmg;
        // Visual flash or crack
        if (this.mesh) {
            this.mesh.traverse(child => {
                if (child.isMesh && child.material) {
                    const originalColor = child.material.color ? child.material.color.getHex() : 0x06B6D4;
                    child.material.color.setHex(0xFFFFFF);
                    setTimeout(() => {
                        if (!this.isDestroyed && child.material) child.material.color.setHex(originalColor);
                    }, 80);
                }
            });
        }
        if (this.hp <= 0 && !this.isDestroyed) {
            this.destroy();
        }
    }

    update(dt) {
        this.lifetime -= dt;
        if (this.lifetime <= 0 && !this.isDestroyed) {
            this.destroy();
        }
    }

    destroy() {
        if (this.isDestroyed) return;
        this.isDestroyed = true;
        this.scene.remove(this.mesh);
        if (window.gameEngine) {
            window.gameEngine.spawnExplosionEffect(this.position.clone().add(new THREE.Vector3(0, 1.4, 0)));
            if (this.map || window.gameEngine.map) {
                const map = window.gameEngine.map;
                const idx = map.colliders.indexOf(this.collider);
                if (idx !== -1) map.colliders.splice(idx, 1);
            }
        }
    }
}

// Ground Loot & Death Boxes
class LootManager {
    constructor(scene) {
        this.scene = scene;
        this.lootItems = [];
    }

    spawnGroundItem(type, pos) {
        const group = new THREE.Group();
        group.position.copy(pos);
        group.position.y = Math.max(0.3, pos.y);

        let mesh = null;
        let color = 0xF59E0B;

        if (type.startsWith('GUN_') && window.blenderAssetLoader) {
            mesh = window.blenderAssetLoader.createWeaponInstance(type);
            if (mesh) {
                mesh.position.set(0, 0.15, 0);
                mesh.scale.set(1.4, 1.4, 1.4);
                group.add(mesh);
            }
        } else if (type === 'GLOO_WALL') {
            color = 0x06B6D4;
            const canGeom = new THREE.CylinderGeometry(0.2, 0.2, 0.55, 12);
            const canMat = new THREE.MeshStandardMaterial({
                color: 0x06B6D4,
                emissive: 0x0891B2,
                emissiveIntensity: 0.5,
                metalness: 0.6,
                roughness: 0.2
            });
            mesh = new THREE.Mesh(canGeom, canMat);
            mesh.position.y = 0.28;
            group.add(mesh);
        } else if (type === 'GRENADE') {
            color = 0x10B981;
            const grGroup = new THREE.Group();
            const bodyGeom = new THREE.SphereGeometry(0.22, 10, 10);
            bodyGeom.scale(1.0, 1.35, 1.0);
            const bodyMat = new THREE.MeshStandardMaterial({
                color: 0x15803D,
                metalness: 0.6,
                roughness: 0.4
            });
            const bodyMesh = new THREE.Mesh(bodyGeom, bodyMat);
            bodyMesh.position.y = 0.28;
            bodyMesh.castShadow = true;
            grGroup.add(bodyMesh);

            const pinGeom = new THREE.CylinderGeometry(0.06, 0.06, 0.14, 8);
            const pinMat = new THREE.MeshStandardMaterial({ color: 0xF59E0B, metalness: 0.8 });
            const pinMesh = new THREE.Mesh(pinGeom, pinMat);
            pinMesh.position.y = 0.48;
            grGroup.add(pinMesh);

            mesh = grGroup;
            group.add(mesh);
        }

        if (!mesh) {
            let geom, mat;
            if (type.startsWith('GUN_')) {
                geom = new THREE.BoxGeometry(0.8, 0.2, 0.25);
                color = 0xF59E0B;
            } else if (type === 'AMMO') {
                geom = new THREE.BoxGeometry(0.4, 0.3, 0.4);
                color = 0x10B981;
            } else if (type === 'MEDKIT') {
                geom = new THREE.BoxGeometry(0.45, 0.35, 0.45);
                color = 0xEF4444;
            } else if (type === 'ARMOR') {
                geom = new THREE.CylinderGeometry(0.3, 0.3, 0.4, 6);
                color = 0x6366F1;
            } else {
                geom = new THREE.BoxGeometry(0.35, 0.35, 0.35);
                color = 0xEC4899;
            }

            mat = new THREE.MeshStandardMaterial({
                color: color,
                emissive: color,
                emissiveIntensity: 0.35,
                roughness: 0.3
            });
            const box = new THREE.Mesh(geom, mat);
            box.castShadow = true;
            group.add(box);
        }

        // Ambient vertical beacon ring
        const ringGeom = new THREE.RingGeometry(0.4, 0.55, 16);
        const ringMat = new THREE.MeshBasicMaterial({ color: color, side: THREE.DoubleSide });
        const ring = new THREE.Mesh(ringGeom, ringMat);
        ring.rotation.x = -Math.PI / 2;
        ring.position.y = 0.05;
        group.add(ring);

        this.scene.add(group);
        this.lootItems.push({
            type: type,
            mesh: group,
            pos: group.position,
            initialY: group.position.y,
            hoverOffset: Math.random() * Math.PI * 2
        });
    }

    // Spawn classic Free Fire Death Box when an enemy dies
    spawnDeathBox(pos, killerName = "") {
        const group = new THREE.Group();
        group.position.copy(pos);
        group.position.y = Math.max(0.4, pos.y);

        let blenderBox = null;
        if (window.blenderAssetLoader && window.blenderAssetLoader.models['airdrop_crate']) {
            blenderBox = window.blenderAssetLoader.createAirdropInstance(0, 0, 0);
            if (blenderBox) {
                blenderBox.scale.set(0.65, 0.65, 0.65);
                group.add(blenderBox);
            }
        }

        if (!blenderBox) {
            // Golden ornate death crate
            const boxGeom = new THREE.BoxGeometry(0.9, 0.7, 0.9);
            const boxMat = new THREE.MeshStandardMaterial({
                color: 0xD97706,
                metalness: 0.8,
                roughness: 0.25,
                emissive: 0x78350F,
                emissiveIntensity: 0.4
            });
            const box = new THREE.Mesh(boxGeom, boxMat);
            box.castShadow = true;
            group.add(box);
        }

        // Vertical glowing golden light beam
        const beamGeom = new THREE.CylinderGeometry(0.12, 0.4, 18, 12, 1, true);
        const beamMat = new THREE.MeshBasicMaterial({
            color: 0xFDE047,
            transparent: true,
            opacity: 0.45,
            side: THREE.DoubleSide
        });
        const beam = new THREE.Mesh(beamGeom, beamMat);
        beam.position.y = 9;
        group.add(beam);

        this.scene.add(group);
        this.lootItems.push({
            type: 'DEATH_BOX',
            mesh: group,
            pos: group.position,
            initialY: group.position.y,
            hoverOffset: 0
        });
    }

    // Spawn high-tier Parachute Airdrop Box
    spawnAirdrop(pos) {
        const group = new THREE.Group();
        group.position.copy(pos);

        let blenderCrate = null;
        if (window.blenderAssetLoader && window.blenderAssetLoader.models['airdrop_crate']) {
            blenderCrate = window.blenderAssetLoader.createAirdropInstance(0, 0, 0);
            if (blenderCrate) {
                blenderCrate.scale.set(1.2, 1.2, 1.2);
                group.add(blenderCrate);
            }
        }

        if (!blenderCrate) {
            // Red military supply crate with yellow warning stripes
            const crateGeom = new THREE.BoxGeometry(1.6, 1.4, 1.6);
            const crateMat = new THREE.MeshStandardMaterial({
                color: 0x991B1B,
                roughness: 0.4,
                metalness: 0.3
            });
            const crate = new THREE.Mesh(crateGeom, crateMat);
            crate.castShadow = true;
            group.add(crate);
        }

        // High vertical yellow laser beam piercing sky
        const beamGeom = new THREE.CylinderGeometry(0.2, 0.5, 45, 12, 1, true);
        const beamMat = new THREE.MeshBasicMaterial({
            color: 0xFACC15,
            transparent: true,
            opacity: 0.65,
            side: THREE.DoubleSide
        });
        const beam = new THREE.Mesh(beamGeom, beamMat);
        beam.position.y = 22.5;
        group.add(beam);

        this.scene.add(group);
        this.lootItems.push({
            type: 'AIRDROP',
            mesh: group,
            pos: group.position,
            initialY: group.position.y,
            hoverOffset: 0
        });
    }

    update(dt, playerPos, onPickup) {
        const now = performance.now() * 0.002;
        for (let i = this.lootItems.length - 1; i >= 0; i--) {
            const item = this.lootItems[i];
            // Gentle hovering animation for loose loot
            if (item.type !== 'DEATH_BOX' && item.type !== 'AIRDROP') {
                item.mesh.rotation.y += 1.5 * dt;
                item.mesh.position.y = item.initialY + Math.sin(now + item.hoverOffset) * 0.12;
            }

            // Proximity pickup check (1.8m radius)
            const dist = playerPos.distanceTo(item.pos);
            if (dist < 2.0) {
                if (onPickup) {
                    const consumed = onPickup(item.type);
                    if (consumed) {
                        this.scene.remove(item.mesh);
                        this.lootItems.splice(i, 1);
                        if (window.soundEngine) window.soundEngine.playReload();
                    }
                }
            }
        }
    }
}

window.WEAPON_TYPES = WEAPON_TYPES;
window.WeaponInventory = WeaponInventory;
window.ActiveGrenade = ActiveGrenade;
window.LootManager = LootManager;
window.GlooWallInstance = GlooWallInstance;
