// War Land 3D - Master WebGL Engine, Game Loop & Systems Coordinator

class GameEngine {
    constructor() {
        this.container = document.getElementById('gameContainer');
        this.canvas = document.getElementById('gameCanvas');

        this.scene = null;
        this.camera = null;
        this.renderer = null;

        this.clock = new THREE.Clock();
        this.activeGrenades = [];
        this.bulletTracers = [];
        this.explosionParticles = [];

        // Safe Zone Circle (Active ONLY in Tournament & Custom / Friends modes)
        const urlParams = new URLSearchParams(window.location.search);
        this.gameMode = urlParams.get('mode') || window.gameMode || 'PRACTICE';
        this.isZoneActive = (this.gameMode === 'TOURNAMENT' || this.gameMode === 'FRIENDS' || this.gameMode === 'CUSTOM');
        this.safeZoneRadius = 360;
        this.safeZoneCenter = new THREE.Vector2(0, 0);
        this.safeZoneShrinkSpeed = 0.8; // meters per second

        // Free Fire Gloo Wall Deployable Shield System
        this.glooWalls = [];
        this.playerGlooWallCount = 3;

        this.initThree();
        if (window.blenderAssetLoader) {
            window.blenderAssetLoader.loadAllModels(() => {
                this.initSystems();
                this.initMinimap();
                this.animate();
            });
        } else {
            this.initSystems();
            this.initMinimap();
            this.animate();
        }
    }

    initThree() {
        // 1. Scene & Tropical Fog
        this.scene = new THREE.Scene();
        this.scene.background = new THREE.Color(0x38BDF8); // Vibrant tropical sky
        this.scene.fog = new THREE.FogExp2(0x38BDF8, 0.00085);

        // Atmospheric Sky Dome with vibrant tropical gradient
        const skyCanvas = document.createElement('canvas');
        skyCanvas.width = 16;
        skyCanvas.height = 256;
        const sctx = skyCanvas.getContext('2d');
        const grad = sctx.createLinearGradient(0, 0, 0, 256);
        grad.addColorStop(0.0, '#0369A1'); // Deep Azure Blue sky top
        grad.addColorStop(0.55, '#38BDF8'); // Vibrant Sky Blue
        grad.addColorStop(0.82, '#BAE6FD'); // Soft horizon cyan
        grad.addColorStop(1.0, '#FEF08A'); // Golden sunset haze at horizon
        sctx.fillStyle = grad;
        sctx.fillRect(0, 0, 16, 256);

        const skyTex = new THREE.CanvasTexture(skyCanvas);
        const skyGeom = new THREE.SphereGeometry(1500, 32, 24);
        const skyMat = new THREE.MeshBasicMaterial({ map: skyTex, side: THREE.BackSide, depthWrite: false });
        const skyDome = new THREE.Mesh(skyGeom, skyMat);
        this.scene.add(skyDome);
        this.skyDome = skyDome;

        // 2. Camera (far plane 2400 to show majestic distant mountains)
        const aspect = window.innerWidth / window.innerHeight;
        this.camera = new THREE.PerspectiveCamera(65, aspect, 0.1, 2400);

        // 3. Renderer with Optimized Performance
        this.renderer = new THREE.WebGLRenderer({
            canvas: this.canvas,
            antialias: true,
            powerPreference: 'high-performance'
        });
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        this.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5));
        this.renderer.shadowMap.enabled = true;
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
        this.renderer.toneMappingExposure = 1.15;

        // 4. Lighting (Sun & Ambient)
        const hemiLight = new THREE.HemisphereLight(0xFFFFFF, 0x334155, 0.65);
        this.scene.add(hemiLight);
        this.hemiLight = hemiLight;

        const sunLight = new THREE.DirectionalLight(0xFFFBEB, 1.3);
        sunLight.position.set(80, 120, 60);
        sunLight.castShadow = true;
        this.sunLight = sunLight;
        sunLight.shadow.mapSize.width = 1024;
        sunLight.shadow.mapSize.height = 1024;
        sunLight.shadow.camera.near = 10;
        sunLight.shadow.camera.far = 300;
        sunLight.shadow.bias = -0.0005;
        sunLight.shadow.normalBias = 0.03;
        const d = 140;
        sunLight.shadow.camera.left = -d;
        sunLight.shadow.camera.right = d;
        sunLight.shadow.camera.top = d;
        sunLight.shadow.camera.bottom = -d;
        this.scene.add(sunLight);
        this.scene.add(sunLight.target);

        window.addEventListener('resize', () => {
            this.camera.aspect = window.innerWidth / window.innerHeight;
            this.camera.updateProjectionMatrix();
            this.renderer.setSize(window.innerWidth, window.innerHeight);
        });
    }

    initSystems() {
        // 1. Map Builder
        this.map = new WarLandMap(this.scene);
        this.map.buildMap();

        // 2. Player
        this.player = new PlayerController(this.scene, this.camera, this.map);
        window.player = this.player;

        // 3. Weapons & Inventory
        this.inventory = new WeaponInventory();
        window.inventory = this.inventory;

        // 4. Loot Manager
        this.lootManager = new LootManager(this.scene);
        this.spawnInitialLoot();

        // 5. Bot AI Manager (25 Bots)
        this.botManager = new BotManager(this.scene, this.map);
        this.botManager.spawnBots(25);

        // 6. Multiplayer P2P & Spectator
        this.multiplayer = new MultiplayerP2p();
        window.multiplayer = this.multiplayer;

        // 7. HUD Controller
        this.hud = new WarLandHud();
        window.hud = this.hud;

        this.hud.updateHealthUi();
        this.hud.updateWeaponUi();
        this.hud.updateAliveCount();

        // Bind spectator buttons
        document.getElementById('btnSpectatePrev')?.addEventListener('click', () => {
            const living = this.botManager.getLivingBots();
            this.multiplayer.prevSpectateTarget(living);
        });
        document.getElementById('btnSpectateNext')?.addEventListener('click', () => {
            const living = this.botManager.getLivingBots();
            this.multiplayer.nextSpectateTarget(living);
        });

        // 8. Dynamic Weather System
        if (window.WarLandWeather) {
            this.weather = new WarLandWeather(this.scene, this.sunLight, this.hemiLight, this.skyDome, this.camera);
            window.weather = this.weather;
            document.getElementById('btnWeather')?.addEventListener('click', () => {
                if (this.weather) this.weather.cycleWeather();
            });
        }

        // 9. Battle Royale Airplane Airdrop System
        this.initAirdropPlane();
    }

    initAirdropPlane() {
        this.planeStart = new THREE.Vector3(-360, 190, -360);
        this.planeEnd = new THREE.Vector3(360, 190, 360);
        this.planeFlightDuration = 28.0;
        this.planeTimer = 0;
        this.isPlaneActive = true;

        // 3D Military Cargo Aircraft (C-130 Hercules style)
        const plane = new THREE.Group();
        plane.position.copy(this.planeStart);

        const camoMat = new THREE.MeshStandardMaterial({ color: 0x334155, roughness: 0.5, metalness: 0.3 });
        const metalMat = new THREE.MeshStandardMaterial({ color: 0x475569, roughness: 0.4, metalness: 0.6 });

        // Fuselage Main Body
        const bodyGeom = new THREE.CylinderGeometry(3.5, 3.2, 38, 16);
        bodyGeom.rotateX(Math.PI / 2);
        const body = new THREE.Mesh(bodyGeom, camoMat);
        plane.add(body);

        // Cockpit Radome Nose
        const noseGeom = new THREE.ConeGeometry(3.5, 9, 16);
        noseGeom.rotateX(-Math.PI / 2);
        const nose = new THREE.Mesh(noseGeom, metalMat);
        nose.position.set(0, 0, -23.5);
        plane.add(nose);

        // Wings (52m wingspan)
        const wingGeom = new THREE.BoxGeometry(52, 0.6, 6.5);
        const wings = new THREE.Mesh(wingGeom, camoMat);
        wings.position.set(0, 1.2, -2.0);
        plane.add(wings);

        // 4 Turboprop Engine Pods
        for (const ex of [-14, -7, 7, 14]) {
            const engGeom = new THREE.CylinderGeometry(1.2, 1.2, 6.0, 12);
            engGeom.rotateX(Math.PI / 2);
            const eng = new THREE.Mesh(engGeom, metalMat);
            eng.position.set(ex, 0.4, -3.0);
            plane.add(eng);

            const propGeom = new THREE.CircleGeometry(2.4, 8);
            const propMat = new THREE.MeshBasicMaterial({ color: 0xF8FAFC, transparent: true, opacity: 0.45, side: THREE.DoubleSide });
            const prop = new THREE.Mesh(propGeom, propMat);
            prop.position.set(ex, 0.4, -6.1);
            plane.add(prop);
        }

        // T-Tail
        const tailGeom = new THREE.BoxGeometry(0.5, 11, 7);
        const tail = new THREE.Mesh(tailGeom, camoMat);
        tail.position.set(0, 6.5, 16);
        plane.add(tail);

        const hTailGeom = new THREE.BoxGeometry(18, 0.4, 4.5);
        const hTail = new THREE.Mesh(hTailGeom, camoMat);
        hTail.position.set(0, 11.5, 17);
        plane.add(hTail);

        const dir = new THREE.Vector3().subVectors(this.planeEnd, this.planeStart).normalize();
        plane.rotation.y = Math.atan2(dir.x, dir.z) + Math.PI;

        this.scene.add(plane);
        this.airdropPlane = plane;

        // Show HUD Flight Banner
        const planeHud = document.getElementById('planeFlightHud');
        if (planeHud) planeHud.classList.remove('hidden');

        // Bind Jump Button
        document.getElementById('btnPlaneJump')?.addEventListener('click', () => {
            if (this.player && this.player.inPlane) {
                this.player.ejectFromPlane();
            }
        });

        document.getElementById('btnDeployChute')?.addEventListener('click', () => {
            if (this.player && this.player.isSkydiving && !this.player.isParachuteOpen) {
                this.player.deployParachute();
            }
        });
    }

    spawnInitialLoot() {
        // Spawn Ground Weapons & Medkits across building floors, courtyards, and rooftops
        const lootSpawns = [
            // Interior Villa & Townhouse Loot (Ground, 2nd floor, and rooftops!)
            { t: 'GUN_AK47', p: new THREE.Vector3(28, 4.2, -32) },    // Villa 2nd floor!
            { t: 'GUN_AWM', p: new THREE.Vector3(28, 8.0, -32) },     // Villa Rooftop!
            { t: 'MEDKIT', p: new THREE.Vector3(26, 0.4, -30) },      // Villa Living room
            { t: 'ARMOR', p: new THREE.Vector3(-32, 4.2, -45) },      // Townhouse 2nd floor
            { t: 'GUN_DEAGLE', p: new THREE.Vector3(-32, 8.0, -45) }, // Townhouse 3rd floor
            { t: 'GUN_SPAS12', p: new THREE.Vector3(-32, 11.8, -45) },// Townhouse Rooftop
            { t: 'AMMO', p: new THREE.Vector3(-65, 4.2, -75) },
            { t: 'GUN_AK47', p: new THREE.Vector3(-85, 0.4, -55) },
            { t: 'MEDKIT', p: new THREE.Vector3(120, 0.4, -80) },     // Inside Hangar
            { t: 'GUN_AWM', p: new THREE.Vector3(120, 8.8, -80) },     // Hangar roof
            // Center Town Square & Roads
            { t: 'GUN_AK47', p: new THREE.Vector3(8, 0.4, 8) },
            { t: 'GUN_DEAGLE', p: new THREE.Vector3(-8, 0.4, 12) },
            { t: 'GUN_SPAS12', p: new THREE.Vector3(12, 0.4, -15) },
            { t: 'AMMO', p: new THREE.Vector3(10, 0.4, 10) },
            { t: 'ARMOR', p: new THREE.Vector3(0, 0.4, 0) },

            // Military Barracks & Rooftops
            { t: 'GUN_AK47', p: new THREE.Vector3(-45, 8.2, -55) },
            { t: 'GUN_AWM', p: new THREE.Vector3(-80, 12.0, -35) },
            { t: 'AMMO', p: new THREE.Vector3(-42, 8.2, -52) },
            { t: 'MEDKIT', p: new THREE.Vector3(-82, 12.0, -32) },
            { t: 'ARMOR', p: new THREE.Vector3(-50, 12.0, 45) },

            // Industrial Depot & Hangar
            { t: 'GUN_SPAS12', p: new THREE.Vector3(-100, 0.5, -60) },
            { t: 'GUN_DEAGLE', p: new THREE.Vector3(-115, 0.5, -45) },
            { t: 'AMMO', p: new THREE.Vector3(-98, 0.5, -58) },

            // Watchtower & Sniper Posts
            { t: 'GUN_AWM', p: new THREE.Vector3(-20, 12.4, -115) },
            { t: 'AMMO', p: new THREE.Vector3(-19, 12.4, -114) },
            { t: 'MEDKIT', p: new THREE.Vector3(110, 12.4, -70) },

            // Villas & Townhouses
            { t: 'GUN_DEAGLE', p: new THREE.Vector3(90, 3.6, 25) },
            { t: 'GUN_AK47', p: new THREE.Vector3(85, 0.4, 45) },
            { t: 'MEDKIT', p: new THREE.Vector3(50, 0.4, 80) },
            { t: 'GUN_SPAS12', p: new THREE.Vector3(-60, 0.4, 105) },
            { t: 'ARMOR', p: new THREE.Vector3(125, 0.4, 15) },

            // Modern Luxury Residential District & Downtown City Blocks
            { t: 'GUN_AK47', p: new THREE.Vector3(55, 0.4, -45) },
            { t: 'GUN_AWM', p: new THREE.Vector3(95, 0.4, -45) },
            { t: 'GUN_DEAGLE', p: new THREE.Vector3(135, 0.4, -50) },
            { t: 'GUN_SPAS12', p: new THREE.Vector3(55, 0.4, -90) },
            { t: 'MEDKIT', p: new THREE.Vector3(95, 0.4, -95) },
            { t: 'ARMOR', p: new THREE.Vector3(145, 0.4, -100) },
            { t: 'GUN_AK47', p: new THREE.Vector3(-60, 0.4, -30) },
            { t: 'GUN_AWM', p: new THREE.Vector3(-95, 0.4, -40) },
            { t: 'MEDKIT', p: new THREE.Vector3(-140, 0.4, -30) },
            { t: 'AMMO', p: new THREE.Vector3(-105, 0.4, -95) }
        ];

        lootSpawns.forEach(item => {
            this.lootManager.spawnGroundItem(item.t, item.p);
        });

        // Spawn Free Fire Gloo Wall Pickups
        const glooSpawns = [
            new THREE.Vector3(5, 0.4, 5),
            new THREE.Vector3(-25, 0.4, -25),
            new THREE.Vector3(45, 0.4, 35),
            new THREE.Vector3(-45, 8.2, -50),
            new THREE.Vector3(75, 0.4, -60)
        ];
        glooSpawns.forEach(pos => {
            this.lootManager.spawnGroundItem('GLOO_WALL', pos);
        });

        // Spawn Ground Grenades across key tactical positions
        const grenadeSpawns = [
            new THREE.Vector3(12, 0.4, 18),
            new THREE.Vector3(-15, 0.4, 25),
            new THREE.Vector3(28, 4.2, -30),   // Villa 2nd floor
            new THREE.Vector3(-32, 4.2, -43),  // Townhouse 2nd floor
            new THREE.Vector3(-110, 0.4, -68), // Barracks entrance
            new THREE.Vector3(118, 0.4, -75),  // Hangar interior
            new THREE.Vector3(-85, 0.4, -30)
        ];
        grenadeSpawns.forEach(pos => {
            this.lootManager.spawnGroundItem('GRENADE', pos);
        });

        // Spawn Initial Parachuting Airdrop Crate with laser beacon
        this.lootManager.spawnAirdrop(new THREE.Vector3(15, 0.7, -20));
    }

    initMinimap() {
        this.minimapCanvas = document.getElementById('minimapCanvas');
        if (this.minimapCanvas) {
            this.minimapCtx = this.minimapCanvas.getContext('2d');
        }
    }

    throwGrenade() {
        if (!this.player || !this.inventory || this.inventory.grenadeCount <= 0) return;
        this.inventory.grenadeCount--;
        if (this.hud) this.hud.updateGrenadeUi(this.inventory.grenadeCount);

        const startPos = this.player.position.clone();
        startPos.y += 1.6;

        // Throw direction along camera view
        const dir = new THREE.Vector3(
            Math.sin(this.player.rotationY),
            Math.tan(-this.player.pitch) + 0.35, // arc upward
            Math.cos(this.player.rotationY)
        ).normalize();

        const velocity = dir.multiplyScalar(22);
        const grenade = new ActiveGrenade(this.scene, startPos, velocity);
        this.activeGrenades.push(grenade);

        if (window.soundEngine) window.soundEngine.playReload();
    }

    rayIntersectsAABB(orig, dir, min, max, maxDist = Infinity) {
        let tmin = (min.x - orig.x) / (dir.x || 1e-6);
        let tmax = (max.x - orig.x) / (dir.x || 1e-6);
        if (tmin > tmax) [tmin, tmax] = [tmax, tmin];

        let tymin = (min.y - orig.y) / (dir.y || 1e-6);
        let tymax = (max.y - orig.y) / (dir.y || 1e-6);
        if (tymin > tymax) [tymin, tymax] = [tymax, tymin];

        if ((tmin > tymax) || (tymin > tmax)) return null;
        if (tymin > tmin) tmin = tymin;
        if (tymax < tmax) tmax = tymax;

        let tzmin = (min.z - orig.z) / (dir.z || 1e-6);
        let tzmax = (max.z - orig.z) / (dir.z || 1e-6);
        if (tzmin > tzmax) [tzmin, tzmax] = [tzmax, tzmin];

        if ((tmin > tzmax) || (tzmin > tmax)) return null;
        if (tzmin > tmin) tmin = tzmin;
        if (tzmax < tmax) tmax = tzmax;

        if (tmin < 0) {
            if (tmax < 0) return null;
            tmin = tmax;
        }
        return (tmin <= maxDist) ? tmin : null;
    }

    spawnImpactSparks(pos, colorHex = 0xF59E0B) {
        for (let i = 0; i < 6; i++) {
            const spark = new THREE.Mesh(
                new THREE.SphereGeometry(0.06, 4, 4),
                new THREE.MeshBasicMaterial({ color: colorHex })
            );
            spark.position.copy(pos);
            const vel = new THREE.Vector3(
                (Math.random() - 0.5) * 4,
                Math.random() * 3 + 1,
                (Math.random() - 0.5) * 4
            );
            this.scene.add(spark);
            this.explosionParticles.push({ mesh: spark, vel: vel, life: 0.25 });
        }
    }

    fireActiveWeapon() {
        if (!this.player || !this.player.isAlive || this.player.inPlane || this.player.isSkydiving) return;
        const weapon = this.inventory.getActiveWeapon();
        if (!weapon) return;

        // Fists Melee Punch Attack
        if (weapon.isMelee) {
            this.player.onPunchTriggered();
            return;
        }

        if (this.inventory.isReloading) return;

        const now = performance.now() * 0.001;
        if (now - this.inventory.lastFireTime < weapon.fireRate) return;
        if (weapon.ammoInMag <= 0) {
            this.inventory.startReload();
            return;
        }

        weapon.ammoInMag--;
        this.inventory.lastFireTime = now;
        this.hud.updateWeaponUi();

        // Audio Trigger
        if (window.soundEngine && typeof window.soundEngine[weapon.sound] === 'function') {
            window.soundEngine[weapon.sound]();
        }

        // Notify nearby Bots about gunshot sound
        this.botManager.notifyGunshot(this.player.position);

        // Raycasting Ballistics
        const origin = this.player.position.clone();
        origin.y += this.player.cameraHeightOffset;

        const forward = new THREE.Vector3(
            -Math.sin(this.player.rotationY),
            Math.tan(-this.player.pitch),
            -Math.cos(this.player.rotationY)
        ).normalize();

        // Apply weapon spread jitter
        forward.x += (Math.random() - 0.5) * weapon.spread;
        forward.y += (Math.random() - 0.5) * weapon.spread;
        forward.z += (Math.random() - 0.5) * weapon.spread;
        forward.normalize();

        // 1. Line-of-sight raycast against Building Walls
        let closestWallDist = Infinity;
        if (this.map && this.map.colliders) {
            for (const col of this.map.colliders) {
                if (!col.isObstacle) continue;
                const d = this.rayIntersectsAABB(origin, forward, col.min, col.max, weapon.range);
                if (d !== null && d > 0.35 && d < closestWallDist) {
                    closestWallDist = d;
                }
            }
        }

        // 2. Line-of-sight raycast against Gloo Walls
        let closestGlooDist = Infinity;
        let hitGloo = null;
        for (const gw of this.glooWalls) {
            if (gw.isDestroyed || !gw.collider) continue;
            const d = this.rayIntersectsAABB(origin, forward, gw.collider.min, gw.collider.max, weapon.range);
            if (d !== null && d > 0.35 && d < closestGlooDist) {
                closestGlooDist = d;
                hitGloo = gw;
            }
        }

        const earliestBlockDist = Math.min(closestWallDist, closestGlooDist);

        let hitBot = null;
        let hitBotDist = Infinity;
        let isHeadshot = false;

        // Check hits against living bots
        for (const bot of this.botManager.bots) {
            if (!bot.isAlive || bot.inPlane || bot.isSkydiving) continue;
            const dist = origin.distanceTo(bot.position);
            if (dist > weapon.range || dist > earliestBlockDist) continue; // Blocked by wall or Gloo Wall!

            const toBot = new THREE.Vector3().subVectors(bot.position, origin);
            const projection = toBot.dot(forward);

            if (projection > 0 && projection < earliestBlockDist) {
                const closestPoint = origin.clone().addScaledVector(forward, projection);
                const perpDist = closestPoint.distanceTo(bot.position);

                // Hit detection cylinder around bot (0.75m radius, 2.0m height)
                if (perpDist < 0.75 && closestPoint.y >= bot.position.y && closestPoint.y <= bot.position.y + 2.0) {
                    if (projection < hitBotDist) {
                        hitBotDist = projection;
                        hitBot = bot;
                        isHeadshot = closestPoint.y > bot.position.y + 1.5;
                    }
                }
            }
        }

        if (hitBot) {
            // Hit enemy bot!
            const dmg = isHeadshot ? weapon.damage * weapon.headshotMultiplier : weapon.damage;
            hitBot.takeDamage(dmg, window.playerIgn || "You");
            this.spawnBulletTracer(origin, hitBot.position.clone().add(new THREE.Vector3(0, 1.2, 0)), 0xFDE047);
        } else if (closestGlooDist < closestWallDist && closestGlooDist <= weapon.range) {
            // Hit Gloo Wall! Bullet is absorbed, Gloo Wall takes damage!
            const hitPos = origin.clone().addScaledVector(forward, closestGlooDist);
            this.spawnBulletTracer(origin, hitPos, 0x06B6D4);
            this.spawnImpactSparks(hitPos, 0x38BDF8);
            if (hitGloo) {
                hitGloo.takeDamage(weapon.damage);
            }
        } else if (closestWallDist <= weapon.range) {
            // Hit building wall! Bullet cannot pass through!
            const hitPos = origin.clone().addScaledVector(forward, closestWallDist);
            this.spawnBulletTracer(origin, hitPos, 0xF59E0B);
            this.spawnImpactSparks(hitPos, 0x94A3B8);
        } else {
            // Open air / long range
            const endPoint = origin.clone().addScaledVector(forward, weapon.range);
            this.spawnBulletTracer(origin, endPoint, 0xFDE047);
        }
    }

    spawnBulletTracer(start, end, colorHex = 0xFDE047) {
        const geom = new THREE.BufferGeometry().setFromPoints([start, end]);
        const mat = new THREE.LineBasicMaterial({ color: colorHex, linewidth: 2 });
        const line = new THREE.Line(geom, mat);
        this.scene.add(line);
        this.bulletTracers.push({ line: line, life: 0.08 });
    }

    spawnExplosionEffect(pos) {
        // 1. Expanding High-Heat Fireball Dome
        const fireballGeom = new THREE.SphereGeometry(1.2, 16, 16);
        const fireballMat = new THREE.MeshBasicMaterial({
            color: 0xF59E0B,
            transparent: true,
            opacity: 0.95
        });
        const fireball = new THREE.Mesh(fireballGeom, fireballMat);
        fireball.position.copy(pos);
        this.scene.add(fireball);

        // 2. Expanding Ground Shockwave Blast Ring
        const ringGeom = new THREE.RingGeometry(0.6, 1.4, 32);
        const ringMat = new THREE.MeshBasicMaterial({
            color: 0xEA580C,
            transparent: true,
            opacity: 0.85,
            side: THREE.DoubleSide
        });
        const ring = new THREE.Mesh(ringGeom, ringMat);
        ring.rotation.x = -Math.PI / 2;
        ring.position.set(pos.x, pos.y + 0.05, pos.z);
        this.scene.add(ring);

        // 3. Dynamic Flash Light
        const flashLight = new THREE.PointLight(0xF59E0B, 7.0, 28);
        flashLight.position.copy(pos);
        flashLight.position.y += 1.0;
        this.scene.add(flashLight);
        setTimeout(() => {
            this.scene.remove(flashLight);
        }, 180);

        // 4. Fiery Shrapnel & Smoke Debris Particles
        for (let i = 0; i < 32; i++) {
            const geom = new THREE.SphereGeometry(0.3 + Math.random() * 0.45, 6, 6);
            const mat = new THREE.MeshBasicMaterial({
                color: (i % 3 === 0 ? 0xEF4444 : (i % 3 === 1 ? 0xF59E0B : 0x475569)),
                transparent: true,
                opacity: 0.95
            });
            const p = new THREE.Mesh(geom, mat);
            p.position.copy(pos);

            const vel = new THREE.Vector3(
                (Math.random() - 0.5) * 18,
                Math.random() * 14 + 3,
                (Math.random() - 0.5) * 18
            );
            this.scene.add(p);
            this.explosionParticles.push({ mesh: p, vel: vel, life: 0.85, maxLife: 0.85 });
        }

        // Track fireball & shockwave in particle updates
        this.explosionParticles.push({
            mesh: fireball,
            vel: new THREE.Vector3(0, 1.2, 0),
            life: 0.55,
            maxLife: 0.55,
            isFireball: true
        });
        this.explosionParticles.push({
            mesh: ring,
            vel: new THREE.Vector3(0, 0, 0),
            life: 0.5,
            maxLife: 0.5,
            isShockwave: true
        });

        // 5. Camera Trauma Shake
        if (this.player && this.player.isAlive) {
            const dist = this.player.position.distanceTo(pos);
            if (dist < 38) {
                const shake = Math.max(0.12, (1 - dist / 38) * 0.6);
                if (typeof this.player.addCameraTrauma === 'function') {
                    this.player.addCameraTrauma(shake);
                } else {
                    this.player.recoilPitch = shake * 0.4;
                }
            }
        }

        // 6. Sound Effect
        if (window.soundEngine && typeof window.soundEngine.playExplosion === 'function') {
            window.soundEngine.playExplosion();
        }
    }

    onPlayerEliminated() {
        const rank = this.botManager.getAliveCount() + 1;
        const livingBots = this.botManager.getLivingBots();

        // Switch to Spectator Mode
        this.multiplayer.startSpectating(livingBots);

        // Show match over modal after 2.5s
        setTimeout(() => {
            this.hud.showMatchOverModal(false, rank, this.player.kills);
        }, 2500);
    }

    checkWinCondition() {
        if (!this.player.isAlive) return;
        const aliveBots = this.botManager.getAliveCount();
        if (aliveBots === 0) {
            // Player won! BOOYAH!
            this.hud.showMatchOverModal(true, 1, this.player.kills);
        }
    }

    updateMinimap() {
        if (!this.minimapCtx) return;
        const ctx = this.minimapCtx;
        const w = this.minimapCanvas.width;
        const h = this.minimapCanvas.height;
        const cx = w / 2;
        const cy = h / 2;
        const scale = 0.40; // Scale factor for radar radius

        ctx.clearRect(0, 0, w, h);

        // Circular clipping region for circular radar HUD
        ctx.save();
        ctx.beginPath();
        ctx.arc(cx, cy, cx - 2, 0, Math.PI * 2);
        ctx.clip();

        // 1. Water background
        ctx.fillStyle = '#0369A1';
        ctx.fillRect(0, 0, w, h);

        // 2. Island terrain circle (Center is at 0, 0 in world coords)
        const islandScreenX = cx + (0 - this.player.position.x) * scale;
        const islandScreenY = cy + (0 - this.player.position.z) * scale;
        const islandRadius = 360 * scale;

        // Sand Shoreline Ring
        ctx.beginPath();
        ctx.arc(islandScreenX, islandScreenY, islandRadius + 8 * scale, 0, Math.PI * 2);
        ctx.fillStyle = '#FDE047';
        ctx.fill();

        // Lush Green Grass Island
        ctx.beginPath();
        ctx.arc(islandScreenX, islandScreenY, islandRadius, 0, Math.PI * 2);
        ctx.fillStyle = '#15803D';
        ctx.fill();

        // 3. Roads & Runway
        ctx.fillStyle = '#1E293B';
        // North-South Highway (width 16, length 520 centered at 0, 0)
        ctx.fillRect(islandScreenX - 8 * scale, islandScreenY - 260 * scale, 16 * scale, 520 * scale);
        // East-West Road (length 520, width 16 centered at 0, 0)
        ctx.fillRect(islandScreenX - 260 * scale, islandScreenY - 8 * scale, 520 * scale, 16 * scale);
        // Residential Suburb Boulevard
        ctx.fillRect(islandScreenX + (105 - 6) * scale, islandScreenY + (-100 - 90) * scale, 12 * scale, 180 * scale);
        // Airport Runway (at x=145, z=50, size 34 x 280)
        const rwX = islandScreenX + (145 - 17) * scale;
        const rwY = islandScreenY + (50 - 140) * scale;
        ctx.fillStyle = '#0F172A';
        ctx.fillRect(rwX, rwY, 34 * scale, 280 * scale);

        // 4. Buildings Footprints
        if (this.map && this.map.buildingConfigs) {
            for (const b of this.map.buildingConfigs) {
                const bx = cx + (b.x - this.player.position.x) * scale;
                const by = cy + (b.z - this.player.position.z) * scale;
                let bw = 12 * scale;
                let bh = 14 * scale;
                let bColor = '#EA580C'; // Villa terracotta
                if (b.type === 'townhouse') {
                    bw = 14 * scale; bh = 16 * scale; bColor = '#3B82F6';
                } else if (b.type === 'barracks') {
                    bw = 20 * scale; bh = 10 * scale; bColor = '#4ADE80';
                } else if (b.type === 'hangar') {
                    bw = 18 * scale; bh = 26 * scale; bColor = '#64748B';
                } else if (b.type === 'city') {
                    bw = 16 * scale; bh = 18 * scale; bColor = '#06B6D4'; // City commercial block
                } else if (b.type === 'modern') {
                    bw = 13 * scale; bh = 13 * scale; bColor = '#10B981'; // Modern luxury villa
                }

                ctx.save();
                ctx.translate(bx, by);
                if (b.rot) ctx.rotate(-b.rot);
                ctx.fillStyle = bColor;
                ctx.fillRect(-bw / 2, -bh / 2, bw, bh);
                ctx.strokeStyle = '#FFFFFF';
                ctx.lineWidth = 1;
                ctx.strokeRect(-bw / 2, -bh / 2, bw, bh);
                ctx.restore();
            }
        }

        // 5. Plane Flight Trajectory (if plane active)
        if (this.isPlaneActive && this.planeStart && this.planeEnd) {
            const pStartX = cx + (this.planeStart.x - this.player.position.x) * scale;
            const pStartY = cy + (this.planeStart.z - this.player.position.z) * scale;
            const pEndX = cx + (this.planeEnd.x - this.player.position.x) * scale;
            const pEndY = cy + (this.planeEnd.z - this.player.position.z) * scale;

            ctx.beginPath();
            ctx.setLineDash([4, 4]);
            ctx.moveTo(pStartX, pStartY);
            ctx.lineTo(pEndX, pEndY);
            ctx.strokeStyle = '#FACC15';
            ctx.lineWidth = 2;
            ctx.stroke();
            ctx.setLineDash([]);

            // Plane marker icon
            if (this.airdropPlane) {
                const px = cx + (this.airdropPlane.position.x - this.player.position.x) * scale;
                const py = cy + (this.airdropPlane.position.z - this.player.position.z) * scale;
                ctx.beginPath();
                ctx.arc(px, py, 4.5, 0, Math.PI * 2);
                ctx.fillStyle = '#FFFFFF';
                ctx.fill();
                ctx.strokeStyle = '#F59E0B';
                ctx.lineWidth = 2;
                ctx.stroke();
            }
        }

        // 6. Safe Zone Circle
        if (this.isZoneActive) {
            ctx.beginPath();
            const szX = cx + (this.safeZoneCenter.x - this.player.position.x) * scale;
            const szY = cy + (this.safeZoneCenter.y - this.player.position.z) * scale;
            ctx.arc(szX, szY, Math.max(5, this.safeZoneRadius * scale), 0, Math.PI * 2);
            ctx.strokeStyle = '#22C55E';
            ctx.lineWidth = 2.5;
            ctx.stroke();
        }

        // 7. Ground Loot Blips
        if (this.lootManager && this.lootManager.lootItems) {
            for (const item of this.lootManager.lootItems) {
                const lx = cx + (item.pos.x - this.player.position.x) * scale;
                const ly = cy + (item.pos.z - this.player.position.z) * scale;
                const dist = Math.hypot(lx - cx, ly - cy);
                if (dist < cx - 4) {
                    ctx.beginPath();
                    ctx.arc(lx, ly, 2.2, 0, Math.PI * 2);
                    ctx.fillStyle = (item.type === 'AIRDROP' || item.type === 'DEATH_BOX') ? '#FACC15' : 
                                    (item.type === 'GLOO_WALL' ? '#06B6D4' : '#FDE047');
                    ctx.fill();
                }
            }
        }

        // 8. Living Bots (Red markers)
        if (this.botManager && this.botManager.bots) {
            ctx.fillStyle = '#EF4444';
            for (const bot of this.botManager.bots) {
                if (!bot.isAlive || bot.inPlane) continue;
                const bx = cx + (bot.position.x - this.player.position.x) * scale;
                const by = cy + (bot.position.z - this.player.position.z) * scale;
                const dist = Math.hypot(bx - cx, by - cy);
                if (dist < cx - 4) {
                    ctx.beginPath();
                    ctx.arc(bx, by, 3.2, 0, Math.PI * 2);
                    ctx.fill();
                    ctx.strokeStyle = '#7F1D1D';
                    ctx.lineWidth = 1;
                    ctx.stroke();
                }
            }
        }

        // 9. Active Gloo Walls (Cyan shield markers)
        for (const gw of this.glooWalls) {
            if (gw.isDestroyed) continue;
            const gx = cx + (gw.position.x - this.player.position.x) * scale;
            const gy = cy + (gw.position.z - this.player.position.z) * scale;
            const dist = Math.hypot(gx - cx, gy - cy);
            if (dist < cx - 4) {
                ctx.beginPath();
                ctx.arc(gx, gy, 3.0, 0, Math.PI * 2);
                ctx.fillStyle = '#06B6D4';
                ctx.fill();
            }
        }

        // 10. Player Heading Arrow in Center
        ctx.save();
        ctx.translate(cx, cy);
        ctx.rotate(-this.player.rotationY);
        ctx.fillStyle = '#38BDF8';
        ctx.strokeStyle = '#FFFFFF';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.moveTo(0, -8);
        ctx.lineTo(6, 7);
        ctx.lineTo(0, 3.5);
        ctx.lineTo(-6, 7);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();
        ctx.restore();

        // Restore clipping
        ctx.restore();

        // Outer Compass / Radar Ring
        ctx.beginPath();
        ctx.arc(cx, cy, cx - 2, 0, Math.PI * 2);
        ctx.strokeStyle = '#F59E0B';
        ctx.lineWidth = 3;
        ctx.stroke();

        // North indicator 'N'
        ctx.save();
        ctx.translate(cx, cy);
        const northDist = cx - 10;
        ctx.fillStyle = '#EF4444';
        ctx.font = 'bold 11px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('N', 0, -northDist);
        ctx.restore();
    }

    animate() {
        requestAnimationFrame(() => this.animate());
        const dt = Math.min(this.clock.getDelta(), 0.05);

        // Update Map Dynamics
        this.map.update(dt);

        // Update Battle Royale Airplane Flight
        if (this.isPlaneActive && this.airdropPlane) {
            this.planeTimer += dt;
            const progress = Math.min(1.0, this.planeTimer / this.planeFlightDuration);
            this.airdropPlane.position.lerpVectors(this.planeStart, this.planeEnd, progress);

            // Update bots dropping along plane flight path
            if (this.botManager) {
                this.botManager.updatePlaneDrops(progress, this.airdropPlane.position);
            }

            // Follow airplane with 360-degree free orbital camera while player is inside
            if (this.player && this.player.inPlane) {
                this.player.position.copy(this.airdropPlane.position);
                this.player.position.y -= 1.0;

                // 360-degree free orbit camera around cargo airplane
                const orbitDist = 28.0;
                const camPitch = Math.max(-0.65, Math.min(0.75, this.player.pitch));
                const horizDist = Math.cos(camPitch) * orbitDist;
                const camY = this.airdropPlane.position.y + Math.sin(camPitch) * orbitDist + 4.0;
                const camX = this.airdropPlane.position.x + Math.sin(this.player.rotationY) * horizDist;
                const camZ = this.airdropPlane.position.z + Math.cos(this.player.rotationY) * horizDist;

                this.camera.position.set(camX, Math.max(camY, 15), camZ);
                this.camera.lookAt(this.airdropPlane.position);

                // Auto-eject player near end of flight route if not jumped yet
                if (progress >= 0.88) {
                    this.player.ejectFromPlane();
                }
            }

            if (progress >= 1.0) {
                this.scene.remove(this.airdropPlane);
                this.isPlaneActive = false;
                const planeHud = document.getElementById('planeFlightHud');
                if (planeHud) planeHud.classList.add('hidden');
            }
        }

        // Update Safe Zone (ONLY in Tournament, Custom, or Friends modes)
        if (this.isZoneActive) {
            if (this.safeZoneRadius > 25) {
                this.safeZoneRadius -= this.safeZoneShrinkSpeed * dt;
            }
            // Zone border tick damage
            const p2D = new THREE.Vector2(this.player.position.x, this.player.position.z);
            if (p2D.distanceTo(this.safeZoneCenter) > this.safeZoneRadius) {
                this.player.takeDamage(4.0 * dt);
            }
        }

        // Update Active Gloo Walls
        for (let i = this.glooWalls.length - 1; i >= 0; i--) {
            const gw = this.glooWalls[i];
            gw.update(dt);
            if (gw.isDestroyed) {
                this.glooWalls.splice(i, 1);
            }
        }

        // Handle Player Combat Input
        if (this.hud.isFiring) {
            this.fireActiveWeapon();
        }

        // Update Inventory & Reload
        this.inventory.update(dt);

        // Update Player Controller
        this.player.update(dt, this.hud.inputVector, this.hud.isFiring, this.hud.isAiming, this.inventory.getActiveWeapon());

        // Update Spectator Camera if Eliminated
        if (this.multiplayer.isSpectating) {
            const living = this.botManager.getLivingBots();
            const target = living[this.multiplayer.spectateIndex];
            if (target) {
                const targetLook = target.position.clone().add(new THREE.Vector3(0, 1.4, 0));
                this.camera.position.set(
                    target.position.x - Math.sin(target.rotationY) * 5.0,
                    target.position.y + 2.5,
                    target.position.z - Math.cos(target.rotationY) * 5.0
                );
                this.camera.lookAt(targetLook);
            }
        }

        // Update 25 AI Bots
        this.botManager.update(dt, this.player);

        // Update Active Grenades
        for (let i = this.activeGrenades.length - 1; i >= 0; i--) {
            const g = this.activeGrenades[i];
            g.update(dt, this.map.colliders, (pos, radius, damage) => {
                // Radial damage to bots
                for (const bot of this.botManager.bots) {
                    if (!bot.isAlive) continue;
                    const dist = bot.position.distanceTo(pos);
                    if (dist < radius) {
                        const falloff = 1 - (dist / radius);
                        bot.takeDamage(damage * falloff, "Player Grenade");
                    }
                }
                // Damage to player if in blast radius
                const pDist = this.player.position.distanceTo(pos);
                if (pDist < radius) {
                    const falloff = 1 - (pDist / radius);
                    this.player.takeDamage(damage * falloff);
                }
            });
            if (g.exploded) {
                this.activeGrenades.splice(i, 1);
            }
        }

        // Update Bullet Tracers
        for (let i = this.bulletTracers.length - 1; i >= 0; i--) {
            const tracer = this.bulletTracers[i];
            tracer.life -= dt;
            if (tracer.life <= 0) {
                this.scene.remove(tracer.line);
                this.bulletTracers.splice(i, 1);
            }
        }

        // Update Explosion Particles
        for (let i = this.explosionParticles.length - 1; i >= 0; i--) {
            const ep = this.explosionParticles[i];
            ep.life -= dt;
            const progress = 1.0 - Math.max(0, ep.life / (ep.maxLife || 0.7));

            if (ep.isFireball) {
                const scale = 1.0 + progress * 2.8;
                ep.mesh.scale.set(scale, scale, scale);
                if (ep.mesh.material) {
                    ep.mesh.material.opacity = Math.max(0, 1.0 - progress);
                }
            } else if (ep.isShockwave) {
                const scale = 1.0 + progress * 6.5;
                ep.mesh.scale.set(scale, scale, 1.0);
                if (ep.mesh.material) {
                    ep.mesh.material.opacity = Math.max(0, 0.85 * (1.0 - progress));
                }
            } else {
                ep.mesh.position.addScaledVector(ep.vel, dt);
                ep.vel.y -= 16.0 * dt; // gravity
                ep.mesh.scale.multiplyScalar(0.97);
            }

            if (ep.life <= 0) {
                this.scene.remove(ep.mesh);
                this.explosionParticles.splice(i, 1);
            }
        }

        // Update Ground Loot Pickups
        this.lootManager.update(dt, this.player.position, (itemType) => {
            if (itemType === 'GLOO_WALL') {
                this.playerGlooWallCount = Math.min(6, this.playerGlooWallCount + 2);
                this.hud.updateGlooUi(this.playerGlooWallCount);
                if (window.soundEngine) window.soundEngine.playLootPickup();
                return true;
            } else if (itemType.startsWith('GUN_')) {
                this.inventory.pickupWeapon(itemType);
                this.hud.updateWeaponUi();
                if (window.soundEngine) window.soundEngine.playLootPickup();
                return true;
            } else if (itemType === 'AMMO') {
                const w = this.inventory.getActiveWeapon();
                w.reserveAmmo += w.magSize * 2;
                this.hud.updateWeaponUi();
                return true;
            } else if (itemType === 'GRENADE') {
                this.inventory.grenadeCount = Math.min(5, this.inventory.grenadeCount + 1);
                this.hud.updateGrenadeUi(this.inventory.grenadeCount);
                if (window.soundEngine) window.soundEngine.playLootPickup();
                return true;
            } else if (itemType === 'MEDKIT') {
                this.inventory.medkitCount++;
                return true;
            } else if (itemType === 'ARMOR') {
                this.player.armor = this.player.maxArmor;
                this.hud.updateHealthUi();
                return true;
            } else if (itemType === 'DEATH_BOX' || itemType === 'AIRDROP') {
                // Free Fire loot crate scavenged
                const w = this.inventory.getActiveWeapon();
                w.reserveAmmo += w.magSize * 2;
                this.player.armor = this.player.maxArmor;
                this.playerGlooWallCount = Math.min(6, this.playerGlooWallCount + 2);
                this.inventory.grenadeCount = Math.min(5, this.inventory.grenadeCount + 1);
                this.hud.updateGrenadeUi(this.inventory.grenadeCount);
                this.hud.updateGlooUi(this.playerGlooWallCount);
                this.hud.updateWeaponUi();
                this.hud.updateHealthUi();
                return true;
            }
            return false;
        });

        // Update Minimap
        this.updateMinimap();

        // Update Weather VFX (Rain particles, lightning & dynamic sky)
        if (this.weather) {
            this.weather.update(dt, this.player ? this.player.position : null);
        }

        // Check Match Over / Booyah!
        this.checkWinCondition();

        // Update Sun & Shadow focus centered dynamically on player
        if (this.sunLight && this.player) {
            this.sunLight.position.set(
                this.player.position.x + 80,
                this.player.position.y + 120,
                this.player.position.z + 60
            );
            this.sunLight.target.position.copy(this.player.position);
            this.sunLight.target.updateMatrixWorld();
        }

        // Render Scene
        this.renderer.render(this.scene, this.camera);
    }

    deployGlooWall() {
        if (!this.player || !this.player.isAlive) return;
        if (this.playerGlooWallCount <= 0) return;

        this.playerGlooWallCount--;
        this.hud.updateGlooUi(this.playerGlooWallCount);

        const fwd = new THREE.Vector3(0, 0, -1).applyAxisAngle(new THREE.Vector3(0, 1, 0), this.player.rotationY);
        const dropPos = this.player.position.clone().addScaledVector(fwd, 2.8);
        dropPos.y = Math.max(0, this.player.position.y);

        const glooWall = new GlooWallInstance(this.scene, dropPos, this.player.rotationY);
        this.glooWalls.push(glooWall);

        if (window.soundEngine) window.soundEngine.playGlooWallDeploy();
    }

    scavengeNearbyLoot() {
        if (!this.player || !this.player.isAlive) return;
        const pPos = this.player.position;
        for (let i = this.lootManager.lootItems.length - 1; i >= 0; i--) {
            const item = this.lootManager.lootItems[i];
            if (pPos.distanceTo(item.pos) < 4.0) {
                if (item.type === 'GLOO_WALL') {
                    this.playerGlooWallCount = Math.min(6, this.playerGlooWallCount + 2);
                    this.hud.updateGlooUi(this.playerGlooWallCount);
                } else if (item.type.startsWith('GUN_')) {
                    this.inventory.pickupWeapon(item.type);
                    this.hud.updateWeaponUi();
                } else if (item.type === 'AMMO') {
                    const w = this.inventory.getActiveWeapon();
                    w.reserveAmmo += w.magSize * 2;
                    this.hud.updateWeaponUi();
                } else if (item.type === 'MEDKIT') {
                    this.inventory.medkitCount++;
                } else if (item.type === 'ARMOR') {
                    this.player.armor = this.player.maxArmor;
                    this.hud.updateHealthUi();
                }
                this.scene.remove(item.mesh);
                this.lootManager.lootItems.splice(i, 1);
                if (window.soundEngine) window.soundEngine.playLootPickup();
                break;
            }
        }
    }
}

window.addEventListener('DOMContentLoaded', () => {
    window.gameEngine = new GameEngine();
});
