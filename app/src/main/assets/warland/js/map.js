// War Land 3D - High-Fidelity Coastal Military Island Map (v5 Enhanced)
// Detailed PBR architecture, realistic asphalt highways, sandy shores, rolling horizon mountains,
// animated ocean waves, supply airdrop beacons, and floating "WARLAND" blimp!

class WarLandMap {
    constructor(scene) {
        this.scene = scene;
        this.colliders = []; // AABB { min: Vector3, max: Vector3, isStairs: bool, topY: float }
        this.walkableSurfaces = []; // Y-planes { y, minX, maxX, minZ, maxZ }
        this.buildingConfigs = [];
        this.spawnPoints = [];
        this.blimp = null;
        this.water = null;
        this.blimpAngle = 0;
        this.waterTime = 0;
    }

    buildMap() {
        this.createTerrainAndOcean();
        this.createRoadsAndBridges();
        this.createCityBlocksDistrict();
        this.createModernResidentialDistrict();
        this.createMultiStoryBuildings();
        this.createIndustrialLandmarks();
        this.createMilitaryHangarsAndContainers();
        this.createAircraftWreckageAndVehicles();
        this.createWatchtowersAndBunkers();
        this.createFoliageAndProps();
        this.createHorizonMountains();
        this.createFloatingBlimp();
        this.generateSpawnPoints();
    }

    createTerrainAndOcean() {
        // 1. Vast Ocean Water Plane with Sun Specular (2800m x 2800m)
        const waterGeom = new THREE.PlaneGeometry(2800, 2800, 32, 32);
        const waterMat = new THREE.MeshStandardMaterial({
            color: 0x0284C7,
            roughness: 0.15,
            metalness: 0.8,
            transparent: true,
            opacity: 0.85
        });
        this.water = new THREE.Mesh(waterGeom, waterMat);
        this.water.rotation.x = -Math.PI / 2;
        this.water.position.y = -0.55;
        this.scene.add(this.water);

        // 2. High-Vibrancy Tropical Grass Texture (512x512)
        const groundCanvas = document.createElement('canvas');
        groundCanvas.width = 512;
        groundCanvas.height = 512;
        const gctx = groundCanvas.getContext('2d');

        // Radiant tropical emerald grass base
        gctx.fillStyle = '#16A34A';
        gctx.fillRect(0, 0, 512, 512);

        // Sunlit green and lime grass blades
        const grassHues = ['#15803D', '#22C55E', '#4ADE80', '#166534', '#86EFAC'];
        for (let i = 0; i < 9000; i++) {
            const gx = Math.random() * 512;
            const gy = Math.random() * 512;
            const radius = Math.random() * 4.5 + 1.2;
            gctx.fillStyle = grassHues[Math.floor(Math.random() * grassHues.length)];
            gctx.beginPath();
            gctx.arc(gx, gy, radius, 0, Math.PI * 2);
            gctx.fill();
        }

        const groundTex = new THREE.CanvasTexture(groundCanvas);
        groundTex.wrapS = THREE.RepeatWrapping;
        groundTex.wrapT = THREE.RepeatWrapping;
        groundTex.repeat.set(42, 42);

        // Main Island Ground Surface (Radius 360m, Top sits at Y = 0.0)
        const islandGeom = new THREE.CylinderGeometry(360, 395, 3.0, 64);
        const islandMat = new THREE.MeshStandardMaterial({
            map: groundTex,
            roughness: 0.75,
            metalness: 0.05
        });
        const island = new THREE.Mesh(islandGeom, islandMat);
        island.position.y = -1.5; // Top face sits at Y = 0.0!
        island.receiveShadow = true;
        this.scene.add(island);

        // Walkable ground plane at Y = 0
        this.walkableSurfaces.push({
            y: 0.0,
            minX: -350,
            maxX: 350,
            minZ: -350,
            maxZ: 350
        });

        // 3. Sandy Shoreline Ring (325m to 370m, sits at Y = 0.02)
        const sandGeom = new THREE.RingGeometry(325, 370, 64);
        const sandMat = new THREE.MeshStandardMaterial({
            color: 0xFDE047,
            roughness: 0.9,
            metalness: 0.05,
            side: THREE.DoubleSide
        });
        const sand = new THREE.Mesh(sandGeom, sandMat);
        sand.rotation.x = -Math.PI / 2;
        sand.position.y = 0.02;
        sand.receiveShadow = true;
        this.scene.add(sand);
    }

    createRoadsAndBridges() {
        // High-Quality Asphalt Road Texture with Markings
        const roadCanvas = document.createElement('canvas');
        roadCanvas.width = 128;
        roadCanvas.height = 512;
        const rctx = roadCanvas.getContext('2d');

        // Dark Asphalt
        rctx.fillStyle = '#1E293B';
        rctx.fillRect(0, 0, 128, 512);

        // Yellow Edge Lines
        rctx.fillStyle = '#F59E0B';
        rctx.fillRect(4, 0, 4, 512);
        rctx.fillRect(120, 0, 4, 512);

        // White Dashed Center Divider Line
        rctx.fillStyle = '#F8FAFC';
        for (let y = 0; y < 512; y += 48) {
            rctx.fillRect(62, y, 4, 28);
        }

        const roadTex = new THREE.CanvasTexture(roadCanvas);
        roadTex.wrapS = THREE.RepeatWrapping;
        roadTex.wrapT = THREE.RepeatWrapping;
        roadTex.repeat.set(1, 24);

        const roadMat = new THREE.MeshStandardMaterial({
            map: roadTex,
            roughness: 0.75,
            metalness: 0.1
        });

        // 1. Main North-South Highway (520m long, Y = 0.04 to cleanly sit above grass)
        const roadNSGeom = new THREE.PlaneGeometry(16, 520);
        const roadNS = new THREE.Mesh(roadNSGeom, roadMat);
        roadNS.rotation.x = -Math.PI / 2;
        roadNS.position.set(0, 0.04, 0);
        roadNS.receiveShadow = true;
        this.scene.add(roadNS);

        // 2. East-West Cross Road (520m long, rotated 90 deg so road markings align cleanly along X)
        const roadEWGeom = new THREE.PlaneGeometry(16, 520);
        const roadEW = new THREE.Mesh(roadEWGeom, roadMat);
        roadEW.rotation.set(-Math.PI / 2, 0, Math.PI / 2);
        roadEW.position.set(0, 0.045, 0);
        roadEW.receiveShadow = true;
        this.scene.add(roadEW);

        // 3. Residential Suburb Boulevard (Connecting North-East Modern Villas)
        const resRoadGeom = new THREE.PlaneGeometry(12, 180);
        const resRoad = new THREE.Mesh(resRoadGeom, roadMat);
        resRoad.rotation.x = -Math.PI / 2;
        resRoad.position.set(105, 0.05, -100);
        resRoad.receiveShadow = true;
        this.scene.add(resRoad);

        // 4. Airport Supercar Runway (34m wide, 280m long)
        const runwayMat = new THREE.MeshStandardMaterial({ color: 0x0F172A, roughness: 0.65, metalness: 0.2 });
        const runwayGeom = new THREE.PlaneGeometry(34, 280);
        const runway = new THREE.Mesh(runwayGeom, runwayMat);
        runway.rotation.x = -Math.PI / 2;
        runway.position.set(145, 0.06, 50);
        runway.receiveShadow = true;
        this.scene.add(runway);

        // Runway Yellow Dashed Center Line
        const rwLineGeom = new THREE.PlaneGeometry(1.8, 270);
        const rwLineMat = new THREE.MeshBasicMaterial({ color: 0xF59E0B });
        const rwLine = new THREE.Mesh(rwLineGeom, rwLineMat);
        rwLine.rotation.x = -Math.PI / 2;
        rwLine.position.set(145, 0.07, 50);
        this.scene.add(rwLine);

        // 5. Coastal Bridge extending over eastern ocean
        const bridgeGeom = new THREE.BoxGeometry(120, 1.4, 18);
        const bridgeMat = new THREE.MeshStandardMaterial({ color: 0x475569, metalness: 0.6, roughness: 0.35 });
        const bridge = new THREE.Mesh(bridgeGeom, bridgeMat);
        bridge.position.set(290, 0.7, 0);
        bridge.castShadow = true;
        bridge.receiveShadow = true;
        this.scene.add(bridge);

        this.addBoxCollider(bridge.position, new THREE.Vector3(120, 1.4, 18), true, 1.4);
        this.walkableSurfaces.push({
            y: 1.4,
            minX: 230, maxX: 350,
            minZ: -9, maxZ: 9
        });
    }

    // ── DOWNTOWN COMMERCIAL DISTRICT (From user's Buildings.blend) ───────────
    createCityBlocksDistrict() {
        if (!window.blenderAssetLoader) return;
        const cityConfigs = [
            // West & North-West Commercial City Corridor
            { name: 'bina',     x: -60,  z: -35, rot: 0,             size: new THREE.Vector3(24, 39, 26) },
            { name: 'bina.001', x: -95,  z: -45, rot: Math.PI / 2,   size: new THREE.Vector3(35, 40, 22) },
            { name: 'bina.002', x: -140, z: -35, rot: 0,             size: new THREE.Vector3(13, 42, 20) },
            { name: 'bina.003', x: -60,  z: -95, rot: 0,             size: new THREE.Vector3(24, 50, 25) },
            { name: 'bina.004', x: -105, z: -100, rot: Math.PI / 2,  size: new THREE.Vector3(25, 13, 29) },
            { name: 'bina.005', x: -155, z: -90, rot: 0,             size: new THREE.Vector3(44, 18, 38) },
            { name: 'bina.006', x: -60,  z: -155, rot: 0,            size: new THREE.Vector3(20, 54, 20) },
            { name: 'bina.007', x: -110, z: -160, rot: Math.PI / 2,  size: new THREE.Vector3(30, 47, 32) },
            { name: 'bina.008', x: -165, z: -155, rot: 0,            size: new THREE.Vector3(48, 20, 26) },
            { name: 'bina.009', x: -60,  z: 60,  rot: 0,             size: new THREE.Vector3(19, 38, 15) },
            { name: 'bina.010', x: -105, z: 65,  rot: Math.PI / 2,   size: new THREE.Vector3(24, 50, 25) },
            { name: 'bina.014', x: -150, z: 70,  rot: 0,             size: new THREE.Vector3(22, 25, 31) }
        ];

        cityConfigs.forEach(cfg => {
            const b = window.blenderAssetLoader.createCityBuildingInstance(cfg.name, cfg.x, cfg.z, cfg.rot);
            if (b) {
                this.scene.add(b);
                this.addBoxCollider(new THREE.Vector3(cfg.x, cfg.size.y / 2, cfg.z), cfg.size, false);
                this.buildingConfigs.push({ x: cfg.x, z: cfg.z, type: 'city', rot: cfg.rot });
            }
        });
    }

    // ── MODERN RESIDENTIAL SUBURB (From user's Modern House Collection) ──────
    createModernResidentialDistrict() {
        if (!window.blenderAssetLoader) return;
        const modernConfigs = [
            // North-East Luxury Residential Neighborhood
            { idx: 1,  x: 55,  z: -50,  rot: 0,              size: new THREE.Vector3(14, 8, 12) },
            { idx: 2,  x: 95,  z: -50,  rot: 0,              size: new THREE.Vector3(15, 8, 13) },
            { idx: 3,  x: 135, z: -55,  rot: Math.PI / 4,    size: new THREE.Vector3(16, 9, 14) },
            { idx: 4,  x: 55,  z: -95,  rot: Math.PI,        size: new THREE.Vector3(14, 8, 12) },
            { idx: 5,  x: 95,  z: -100, rot: Math.PI,        size: new THREE.Vector3(15, 9, 13) },
            { idx: 6,  x: 145, z: -105, rot: -Math.PI / 4,   size: new THREE.Vector3(16, 8, 14) },
            { idx: 7,  x: 65,  z: -145, rot: 0,              size: new THREE.Vector3(15, 8, 12) },
            { idx: 8,  x: 110, z: -150, rot: 0,              size: new THREE.Vector3(16, 9, 13) },
            { idx: 9,  x: 155, z: -155, rot: -Math.PI / 2,   size: new THREE.Vector3(15, 8, 14) },
            { idx: 10, x: 195, z: -120, rot: -Math.PI / 2,   size: new THREE.Vector3(16, 9, 14) }
        ];

        modernConfigs.forEach(cfg => {
            const h = window.blenderAssetLoader.createModernHouseInstance(cfg.idx, cfg.x, cfg.z, cfg.rot, 1.25);
            if (h) {
                this.scene.add(h);
                this.addBoxCollider(new THREE.Vector3(cfg.x, cfg.size.y / 2, cfg.z), cfg.size, false);
                this.buildingConfigs.push({ x: cfg.x, z: cfg.z, type: 'modern', rot: cfg.rot });
            }
        });
    }

    createMultiStoryBuildings() {
        const buildingConfigs = [
            // Prominent Roadside Landmarks (Visible from spawn, matching Reference Image 1)
            { x: 28,  z: -32, type: 'villa', rot: 0 },
            { x: -32, z: -45, type: 'townhouse', rot: 0 },
            { x: 32,  z: 38,  type: 'barracks', rot: 0 },
            { x: -35, z: 32,  type: 'villa', rot: 0 },

            // North-West Residential Sector (Mediterranean Terracotta Villas)
            { x: -65, z: -75, type: 'villa', rot: 0 },
            { x: -85, z: -55, type: 'villa', rot: 0 },
            { x: -50, z: -115, type: 'villa', rot: 0 },

            // Central Tactical Sniping Zone (3-Story Modern Townhouses with rooftop access)
            { x: -85, z: -35, type: 'townhouse', rot: 0 },
            { x: -60, z: 55,  type: 'townhouse', rot: 0 },
            { x: 45,  z: -75, type: 'townhouse', rot: 0 },
            { x: -105, z: 25, type: 'townhouse', rot: 0 },

            // South-West Military Outpost (Barracks)
            { x: -110, z: -70, type: 'barracks', rot: 0 },
            { x: -125, z: -45, type: 'barracks', rot: 0 },
            { x: -75,  z: 105, type: 'barracks', rot: 0 },

            // Eastern Industrial Hangars
            { x: 120, z: -80, type: 'hangar', rot: 0 },
            { x: 125, z: 55,  type: 'hangar', rot: 0 }
        ];

        this.buildingConfigs.push(...buildingConfigs);
        buildingConfigs.forEach(cfg => {
            this.buildAccessibleBuilding(cfg.x, cfg.z, cfg.type, cfg.rot);
        });
    }

    initBuildingTextures() {
        if (this._buildingMats) return this._buildingMats;

        // 1. Warm Mediterranean Stucco Plaster Texture
        const stuccoCanvas = document.createElement('canvas');
        stuccoCanvas.width = 256; stuccoCanvas.height = 256;
        const sctx = stuccoCanvas.getContext('2d');
        sctx.fillStyle = '#F5EFE6';
        sctx.fillRect(0, 0, 256, 256);
        sctx.fillStyle = '#E8DEC8';
        for (let i = 0; i < 4000; i++) {
            sctx.fillRect(Math.random() * 256, Math.random() * 256, 1.5, 1.5);
        }
        const stuccoTex = new THREE.CanvasTexture(stuccoCanvas);
        stuccoTex.wrapS = THREE.RepeatWrapping; stuccoTex.wrapT = THREE.RepeatWrapping;
        stuccoTex.repeat.set(3, 3);

        // 2. Cobblestone / Brick Plinth Foundation Texture
        const brickCanvas = document.createElement('canvas');
        brickCanvas.width = 256; brickCanvas.height = 256;
        const bctx = brickCanvas.getContext('2d');
        bctx.fillStyle = '#64748B';
        bctx.fillRect(0, 0, 256, 256);
        bctx.strokeStyle = '#334155';
        bctx.lineWidth = 2;
        for (let y = 0; y < 256; y += 16) {
            bctx.beginPath(); bctx.moveTo(0, y); bctx.lineTo(256, y); bctx.stroke();
            const offset = (y % 32 === 0) ? 0 : 16;
            for (let x = offset; x < 256; x += 32) {
                bctx.beginPath(); bctx.moveTo(x, y); bctx.lineTo(x, y + 16); bctx.stroke();
            }
        }
        const brickTex = new THREE.CanvasTexture(brickCanvas);
        brickTex.wrapS = THREE.RepeatWrapping; brickTex.wrapT = THREE.RepeatWrapping;
        brickTex.repeat.set(4, 4);

        // 3. Parquet Hardwood Interior Floor Texture
        const woodCanvas = document.createElement('canvas');
        woodCanvas.width = 256; woodCanvas.height = 256;
        const wctx = woodCanvas.getContext('2d');
        wctx.fillStyle = '#92400E';
        wctx.fillRect(0, 0, 256, 256);
        wctx.fillStyle = '#78350F';
        for (let y = 0; y < 256; y += 24) {
            wctx.fillRect(0, y, 256, 1.5);
            wctx.fillStyle = (y % 48 === 0) ? '#B45309' : '#78350F';
            wctx.fillRect(0, y + 1.5, 256, 22.5);
        }
        const woodTex = new THREE.CanvasTexture(woodCanvas);
        woodTex.wrapS = THREE.RepeatWrapping; woodTex.wrapT = THREE.RepeatWrapping;
        woodTex.repeat.set(3, 3);

        // 4. Terracotta Spanish Clay Roof Tile Texture
        const roofCanvas = document.createElement('canvas');
        roofCanvas.width = 256; roofCanvas.height = 256;
        const rctx = roofCanvas.getContext('2d');
        rctx.fillStyle = '#C2410C';
        rctx.fillRect(0, 0, 256, 256);
        for (let y = 0; y < 256; y += 16) {
            rctx.fillStyle = '#9A3412';
            rctx.fillRect(0, y, 256, 3);
            rctx.fillStyle = '#EA580C';
            rctx.fillRect(0, y + 3, 256, 13);
        }
        const roofTex = new THREE.CanvasTexture(roofCanvas);
        roofTex.wrapS = THREE.RepeatWrapping; roofTex.wrapT = THREE.RepeatWrapping;
        roofTex.repeat.set(4, 4);

        this._buildingMats = {
            stucco: new THREE.MeshStandardMaterial({ map: stuccoTex, roughness: 0.75, metalness: 0.05 }),
            brick: new THREE.MeshStandardMaterial({ map: brickTex, roughness: 0.85, metalness: 0.1 }),
            wood: new THREE.MeshStandardMaterial({ map: woodTex, roughness: 0.65, metalness: 0.05 }),
            roof: new THREE.MeshStandardMaterial({ map: roofTex, roughness: 0.6, metalness: 0.05 }),
            glass: new THREE.MeshStandardMaterial({ color: 0x38BDF8, roughness: 0.1, metalness: 0.8, transparent: true, opacity: 0.5 }),
            trim: new THREE.MeshStandardMaterial({ color: 0x451A03, roughness: 0.5, metalness: 0.1 }),
            concrete: new THREE.MeshStandardMaterial({ color: 0x94A3B8, roughness: 0.8, metalness: 0.1 })
        };
        return this._buildingMats;
    }

    buildAccessibleBuilding(posX, posZ, type, rotation = 0) {
        const mats = this.initBuildingTextures();

        if (type === 'villa') {
            this.buildEnterableVilla(posX, posZ, mats);
        } else if (type === 'townhouse') {
            this.buildEnterableTownhouse(posX, posZ, mats);
        } else if (type === 'barracks') {
            this.buildEnterableBarracks(posX, posZ, mats);
        } else if (type === 'hangar') {
            this.buildEnterableHangar(posX, posZ, mats);
        }
    }

    // ── 1. ENTERABLE 2-STORY MEDITERRANEAN VILLA ─────────────────────────────
    buildEnterableVilla(px, pz, mats) {
        const bGroup = new THREE.Group();
        bGroup.position.set(px, 0, pz);

        const w = 14, d = 10, h1 = 3.8, h2 = 3.8, totalH = 7.6;
        const halfW = w / 2, halfD = d / 2;

        // Ground Floor Parquet Slab
        const floor1 = new THREE.Mesh(new THREE.BoxGeometry(w, 0.2, d), mats.wood);
        floor1.position.set(0, 0.1, 0);
        floor1.receiveShadow = true;
        bGroup.add(floor1);

        // Walkable surface for Ground Floor
        this.walkableSurfaces.push({
            y: 0.1,
            minX: px - halfW, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Stone Base Plinth Around Exterior (Sits flush at ground level so doorway is 100% open)
        const plinth = new THREE.Mesh(new THREE.BoxGeometry(w + 0.5, 0.2, d + 0.5), mats.brick);
        plinth.position.set(0, 0.1, 0);
        bGroup.add(plinth);

        // ── FLOOR 1 EXTERIOR WALLS (With Open Doorway: 3.2m wide) ────────────
        // Front Wall Left
        const fwL = new THREE.Mesh(new THREE.BoxGeometry(5.2, h1, 0.4), mats.stucco);
        fwL.position.set(-4.2, h1 / 2, halfD);
        fwL.castShadow = true; bGroup.add(fwL);
        this.addBoxCollider(new THREE.Vector3(px - 4.2, h1 / 2, pz + halfD), new THREE.Vector3(5.2, h1, 0.4), true, h1);

        // Front Wall Right
        const fwR = new THREE.Mesh(new THREE.BoxGeometry(5.2, h1, 0.4), mats.stucco);
        fwR.position.set(4.2, h1 / 2, halfD);
        fwR.castShadow = true; bGroup.add(fwR);
        this.addBoxCollider(new THREE.Vector3(px + 4.2, h1 / 2, pz + halfD), new THREE.Vector3(5.2, h1, 0.4), true, h1);

        // Doorway Lintel Header (Open Doorway from X = -1.6 to +1.6, Height = 2.6m)
        const doorLintel = new THREE.Mesh(new THREE.BoxGeometry(3.6, h1 - 2.6, 0.45), mats.trim);
        doorLintel.position.set(0, 2.6 + (h1 - 2.6) / 2, halfD);
        bGroup.add(doorLintel);
        this.addBoxCollider(new THREE.Vector3(px, 2.6 + (h1 - 2.6) / 2, pz + halfD), new THREE.Vector3(3.6, h1 - 2.6, 0.45), true, h1);

        // Back Wall
        const bw = new THREE.Mesh(new THREE.BoxGeometry(w, h1, 0.4), mats.stucco);
        bw.position.set(0, h1 / 2, -halfD);
        bw.castShadow = true; bGroup.add(bw);
        this.addBoxCollider(new THREE.Vector3(px, h1 / 2, pz - halfD), new THREE.Vector3(w, h1, 0.4), true, h1);

        // Left & Right Walls
        const lw = new THREE.Mesh(new THREE.BoxGeometry(0.4, h1, d), mats.stucco);
        lw.position.set(-halfW, h1 / 2, 0);
        lw.castShadow = true; bGroup.add(lw);
        this.addBoxCollider(new THREE.Vector3(px - halfW, h1 / 2, pz), new THREE.Vector3(0.4, h1, d), true, h1);

        const rw = new THREE.Mesh(new THREE.BoxGeometry(0.4, h1, d), mats.stucco);
        rw.position.set(halfW, h1 / 2, 0);
        rw.castShadow = true; bGroup.add(rw);
        this.addBoxCollider(new THREE.Vector3(px + halfW, h1 / 2, pz), new THREE.Vector3(0.4, h1, d), true, h1);

        // ── STAIRCASE TO FLOOR 2 (Steps from Y=0 to Y=3.8 along inner left wall)
        const numSteps = 10;
        const stepWidth = 1.8;
        const stepDepth = 0.55;
        const stepHeight = h1 / numSteps;
        for (let i = 0; i < numSteps; i++) {
            const sy = (i + 0.5) * stepHeight;
            const sz = -halfD + 1.2 + i * stepDepth;
            const stepMesh = new THREE.Mesh(new THREE.BoxGeometry(stepWidth, stepHeight, stepDepth), mats.trim);
            stepMesh.position.set(-halfW + 1.0 + stepWidth / 2, sy, sz);
            stepMesh.castShadow = true;
            bGroup.add(stepMesh);

            this.addBoxCollider(
                new THREE.Vector3(px - halfW + 1.0 + stepWidth / 2, sy, pz + sz),
                new THREE.Vector3(stepWidth, stepHeight, stepDepth),
                true,
                (i + 1) * stepHeight
            );
        }

        // ── FLOOR 2 SLAB (With stairwell opening on left) ────────────────────
        const f2Right = new THREE.Mesh(new THREE.BoxGeometry(w - 2.8, 0.25, d), mats.wood);
        f2Right.position.set(1.4, h1, 0);
        f2Right.receiveShadow = true;
        bGroup.add(f2Right);

        // Walkable surface for Floor 2
        this.walkableSurfaces.push({
            y: h1,
            minX: px - halfW + 2.4, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // ── FLOOR 2 WALLS & BALCONY (Y = 3.8 to 7.6) ─────────────────────────
        // Front Wall with Balcony Doorway & Windows
        const f2FwL = new THREE.Mesh(new THREE.BoxGeometry(5.2, h2, 0.4), mats.stucco);
        f2FwL.position.set(-4.2, h1 + h2 / 2, halfD);
        bGroup.add(f2FwL);
        this.addBoxCollider(new THREE.Vector3(px - 4.2, h1 + h2 / 2, pz + halfD), new THREE.Vector3(5.2, h2, 0.4), true, totalH);

        const f2FwR = new THREE.Mesh(new THREE.BoxGeometry(5.2, h2, 0.4), mats.stucco);
        f2FwR.position.set(4.2, h1 + h2 / 2, halfD);
        bGroup.add(f2FwR);
        this.addBoxCollider(new THREE.Vector3(px + 4.2, h1 + h2 / 2, pz + halfD), new THREE.Vector3(5.2, h2, 0.4), true, totalH);

        // Front Balcony Terrace (Extending 2.5m forward from 2nd floor)
        const balconyFloor = new THREE.Mesh(new THREE.BoxGeometry(6.5, 0.25, 2.5), mats.brick);
        balconyFloor.position.set(0, h1, halfD + 1.25);
        bGroup.add(balconyFloor);
        this.walkableSurfaces.push({
            y: h1,
            minX: px - 3.25, maxX: px + 3.25,
            minZ: pz + halfD, maxZ: pz + halfD + 2.5
        });

        // Balcony Safety Railing (0.9m high)
        const railing = new THREE.Mesh(new THREE.BoxGeometry(6.5, 0.9, 0.15), mats.trim);
        railing.position.set(0, h1 + 0.45, halfD + 2.45);
        bGroup.add(railing);
        this.addBoxCollider(new THREE.Vector3(px, h1 + 0.45, pz + halfD + 2.45), new THREE.Vector3(6.5, 0.9, 0.15), true, h1 + 0.9);

        // Floor 2 Rear & Side Walls
        const f2Bw = new THREE.Mesh(new THREE.BoxGeometry(w, h2, 0.4), mats.stucco);
        f2Bw.position.set(0, h1 + h2 / 2, -halfD);
        bGroup.add(f2Bw);
        this.addBoxCollider(new THREE.Vector3(px, h1 + h2 / 2, pz - halfD), new THREE.Vector3(w, h2, 0.4), true, totalH);

        const f2Lw = new THREE.Mesh(new THREE.BoxGeometry(0.4, h2, d), mats.stucco);
        f2Lw.position.set(-halfW, h1 + h2 / 2, 0);
        bGroup.add(f2Lw);
        this.addBoxCollider(new THREE.Vector3(px - halfW, h1 + h2 / 2, pz), new THREE.Vector3(0.4, h2, d), true, totalH);

        const f2Rw = new THREE.Mesh(new THREE.BoxGeometry(0.4, h2, d), mats.stucco);
        f2Rw.position.set(halfW, h1 + h2 / 2, 0);
        bGroup.add(f2Rw);
        this.addBoxCollider(new THREE.Vector3(px + halfW, h1 + h2 / 2, pz), new THREE.Vector3(0.4, h2, d), true, totalH);

        // ── STAIRS TO ROOFTOP (From Y=3.8 to Y=7.6) ──────────────────────────
        for (let i = 0; i < numSteps; i++) {
            const sy = h1 + (i + 0.5) * stepHeight;
            const sz = halfD - 1.2 - i * stepDepth;
            const stepMesh = new THREE.Mesh(new THREE.BoxGeometry(stepWidth, stepHeight, stepDepth), mats.trim);
            stepMesh.position.set(halfW - 1.0 - stepWidth / 2, sy, sz);
            bGroup.add(stepMesh);

            this.addBoxCollider(
                new THREE.Vector3(px + halfW - 1.0 - stepWidth / 2, sy, pz + sz),
                new THREE.Vector3(stepWidth, stepHeight, stepDepth),
                true,
                h1 + (i + 1) * stepHeight
            );
        }

        // ── ROOFTOP SLAB & SNIPER PARAPET (Y = 7.6) ──────────────────────────
        const roofSlab = new THREE.Mesh(new THREE.BoxGeometry(w, 0.35, d), mats.roof);
        roofSlab.position.set(0, totalH, 0);
        roofSlab.receiveShadow = true;
        bGroup.add(roofSlab);

        this.walkableSurfaces.push({
            y: totalH,
            minX: px - halfW, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // 0.9m High Parapet Walls with Sniper Slits on Rooftop
        const parapetFront = new THREE.Mesh(new THREE.BoxGeometry(w, 0.9, 0.25), mats.brick);
        parapetFront.position.set(0, totalH + 0.45, halfD);
        bGroup.add(parapetFront);
        this.addBoxCollider(new THREE.Vector3(px, totalH + 0.45, pz + halfD), new THREE.Vector3(w, 0.9, 0.25), true, totalH + 0.9);

        const parapetBack = new THREE.Mesh(new THREE.BoxGeometry(w, 0.9, 0.25), mats.brick);
        parapetBack.position.set(0, totalH + 0.45, -halfD);
        bGroup.add(parapetBack);
        this.addBoxCollider(new THREE.Vector3(px, totalH + 0.45, pz - halfD), new THREE.Vector3(w, 0.9, 0.25), true, totalH + 0.9);

        const parapetLeft = new THREE.Mesh(new THREE.BoxGeometry(0.25, 0.9, d), mats.brick);
        parapetLeft.position.set(-halfW, totalH + 0.45, 0);
        bGroup.add(parapetLeft);
        this.addBoxCollider(new THREE.Vector3(px - halfW, totalH + 0.45, pz), new THREE.Vector3(0.25, 0.9, d), true, totalH + 0.9);

        const parapetRight = new THREE.Mesh(new THREE.BoxGeometry(0.25, 0.9, d), mats.brick);
        parapetRight.position.set(halfW, totalH + 0.45, 0);
        bGroup.add(parapetRight);
        this.addBoxCollider(new THREE.Vector3(px + halfW, totalH + 0.45, pz), new THREE.Vector3(0.25, 0.9, d), true, totalH + 0.9);

        this.scene.add(bGroup);
    }

    // ── 2. ENTERABLE 3-STORY TACTICAL TOWNHOUSE ──────────────────────────────
    buildEnterableTownhouse(px, pz, mats) {
        const bGroup = new THREE.Group();
        bGroup.position.set(px, 0, pz);

        const w = 10, d = 10, fH = 3.8, totalH = 11.4;
        const halfW = w / 2, halfD = d / 2;

        // Concrete Base Slab
        const baseFloor = new THREE.Mesh(new THREE.BoxGeometry(w, 0.2, d), mats.concrete);
        baseFloor.position.set(0, 0.1, 0);
        bGroup.add(baseFloor);

        // Walkable surface for Ground Floor
        this.walkableSurfaces.push({
            y: 0.1,
            minX: px - halfW, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Floor 1 Walls with open double doorway
        const fwL = new THREE.Mesh(new THREE.BoxGeometry(3.6, fH, 0.4), mats.brick);
        fwL.position.set(-3.0, fH / 2, halfD);
        bGroup.add(fwL);
        this.addBoxCollider(new THREE.Vector3(px - 3.0, fH / 2, pz + halfD), new THREE.Vector3(3.6, fH, 0.4), true, fH);

        const fwR = new THREE.Mesh(new THREE.BoxGeometry(3.6, fH, 0.4), mats.brick);
        fwR.position.set(3.0, fH / 2, halfD);
        bGroup.add(fwR);
        this.addBoxCollider(new THREE.Vector3(px + 3.0, fH / 2, pz + halfD), new THREE.Vector3(3.6, fH, 0.4), true, fH);

        const bw = new THREE.Mesh(new THREE.BoxGeometry(w, fH, 0.4), mats.brick);
        bw.position.set(0, fH / 2, -halfD);
        bGroup.add(bw);
        this.addBoxCollider(new THREE.Vector3(px, fH / 2, pz - halfD), new THREE.Vector3(w, fH, 0.4), true, fH);

        const lw = new THREE.Mesh(new THREE.BoxGeometry(0.4, fH, d), mats.brick);
        lw.position.set(-halfW, fH / 2, 0);
        bGroup.add(lw);
        this.addBoxCollider(new THREE.Vector3(px - halfW, fH / 2, pz), new THREE.Vector3(0.4, fH, d), true, fH);

        const rw = new THREE.Mesh(new THREE.BoxGeometry(0.4, fH, d), mats.brick);
        rw.position.set(halfW, fH / 2, 0);
        bGroup.add(rw);
        this.addBoxCollider(new THREE.Vector3(px + halfW, fH / 2, pz), new THREE.Vector3(0.4, fH, d), true, fH);

        // Stairs from Ground to Floor 2
        for (let i = 0; i < 9; i++) {
            const sy = (i + 0.5) * (fH / 9);
            const sz = -halfD + 1.0 + i * 0.6;
            const step = new THREE.Mesh(new THREE.BoxGeometry(1.6, fH / 9, 0.6), mats.trim);
            step.position.set(-halfW + 0.8 + 0.8, sy, sz);
            bGroup.add(step);
            this.addBoxCollider(new THREE.Vector3(px - halfW + 1.6, sy, pz + sz), new THREE.Vector3(1.6, fH / 9, 0.6), true, (i + 1) * (fH / 9));
        }

        // Floor 2 Slab at Y = 3.8
        const f2Slab = new THREE.Mesh(new THREE.BoxGeometry(w - 2.2, 0.25, d), mats.concrete);
        f2Slab.position.set(1.1, fH, 0);
        bGroup.add(f2Slab);
        this.walkableSurfaces.push({
            y: fH,
            minX: px - halfW + 2.0, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Floor 2 Perimeter Walls & Balcony
        const f2bw = new THREE.Mesh(new THREE.BoxGeometry(w, fH, 0.4), mats.stucco);
        f2bw.position.set(0, fH + fH / 2, -halfD);
        bGroup.add(f2bw);
        this.addBoxCollider(new THREE.Vector3(px, fH + fH / 2, pz - halfD), new THREE.Vector3(w, fH, 0.4), true, fH * 2);

        const f2lw = new THREE.Mesh(new THREE.BoxGeometry(0.4, fH, d), mats.stucco);
        f2lw.position.set(-halfW, fH + fH / 2, 0);
        bGroup.add(f2lw);
        this.addBoxCollider(new THREE.Vector3(px - halfW, fH + fH / 2, pz), new THREE.Vector3(0.4, fH, d), true, fH * 2);

        const f2rw = new THREE.Mesh(new THREE.BoxGeometry(0.4, fH, d), mats.stucco);
        f2rw.position.set(halfW, fH + fH / 2, 0);
        bGroup.add(f2rw);
        this.addBoxCollider(new THREE.Vector3(px + halfW, fH + fH / 2, pz), new THREE.Vector3(0.4, fH, d), true, fH * 2);

        // Stairs from Floor 2 to Floor 3
        for (let i = 0; i < 9; i++) {
            const sy = fH + (i + 0.5) * (fH / 9);
            const sz = halfD - 1.0 - i * 0.6;
            const step = new THREE.Mesh(new THREE.BoxGeometry(1.6, fH / 9, 0.6), mats.trim);
            step.position.set(halfW - 1.6, sy, sz);
            bGroup.add(step);
            this.addBoxCollider(new THREE.Vector3(px + halfW - 1.6, sy, pz + sz), new THREE.Vector3(1.6, fH / 9, 0.6), true, fH + (i + 1) * (fH / 9));
        }

        // Floor 3 Slab at Y = 7.6
        const f3Slab = new THREE.Mesh(new THREE.BoxGeometry(w - 2.2, 0.25, d), mats.concrete);
        f3Slab.position.set(-1.1, fH * 2, 0);
        bGroup.add(f3Slab);
        this.walkableSurfaces.push({
            y: fH * 2,
            minX: px - halfW, maxX: px + halfW - 2.0,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Floor 3 Walls
        const f3bw = new THREE.Mesh(new THREE.BoxGeometry(w, fH, 0.4), mats.stucco);
        f3bw.position.set(0, fH * 2 + fH / 2, -halfD);
        bGroup.add(f3bw);
        this.addBoxCollider(new THREE.Vector3(px, fH * 2 + fH / 2, pz - halfD), new THREE.Vector3(w, fH, 0.4), true, totalH);

        const f3lw = new THREE.Mesh(new THREE.BoxGeometry(0.4, fH, d), mats.stucco);
        f3lw.position.set(-halfW, fH * 2 + fH / 2, 0);
        bGroup.add(f3lw);
        this.addBoxCollider(new THREE.Vector3(px - halfW, fH * 2 + fH / 2, pz), new THREE.Vector3(0.4, fH, d), true, totalH);

        const f3rw = new THREE.Mesh(new THREE.BoxGeometry(0.4, fH, d), mats.stucco);
        f3rw.position.set(halfW, fH * 2 + fH / 2, 0);
        bGroup.add(f3rw);
        this.addBoxCollider(new THREE.Vector3(px + halfW, fH * 2 + fH / 2, pz), new THREE.Vector3(0.4, fH, d), true, totalH);

        // Rooftop Slab at Y = 11.4
        const roofSlab = new THREE.Mesh(new THREE.BoxGeometry(w, 0.35, d), mats.concrete);
        roofSlab.position.set(0, totalH, 0);
        bGroup.add(roofSlab);
        this.walkableSurfaces.push({
            y: totalH,
            minX: px - halfW, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Rooftop Sniper Parapet (1.0m high)
        const pFront = new THREE.Mesh(new THREE.BoxGeometry(w, 1.0, 0.3), mats.brick);
        pFront.position.set(0, totalH + 0.5, halfD);
        bGroup.add(pFront);
        this.addBoxCollider(new THREE.Vector3(px, totalH + 0.5, pz + halfD), new THREE.Vector3(w, 1.0, 0.3), true, totalH + 1.0);

        const pBack = new THREE.Mesh(new THREE.BoxGeometry(w, 1.0, 0.3), mats.brick);
        pBack.position.set(0, totalH + 0.5, -halfD);
        bGroup.add(pBack);
        this.addBoxCollider(new THREE.Vector3(px, totalH + 0.5, pz - halfD), new THREE.Vector3(w, 1.0, 0.3), true, totalH + 1.0);

        this.scene.add(bGroup);
    }

    // ── 3. ENTERABLE MILITARY BARRACKS ───────────────────────────────────────
    buildEnterableBarracks(px, pz, mats) {
        const bGroup = new THREE.Group();
        bGroup.position.set(px, 0, pz);

        const w = 20, d = 8, h = 4.2;
        const halfW = w / 2, halfD = d / 2;

        // Base Floor
        const floor = new THREE.Mesh(new THREE.BoxGeometry(w, 0.2, d), mats.concrete);
        floor.position.set(0, 0.1, 0);
        bGroup.add(floor);

        // Walkable surface for Barracks Ground Floor
        this.walkableSurfaces.push({
            y: 0.1,
            minX: px - halfW, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Front Wall Left & Right (Leaving open entrance in middle: 3.2m wide)
        const fwL = new THREE.Mesh(new THREE.BoxGeometry(8.2, h, 0.35), mats.brick);
        fwL.position.set(-5.6, h / 2, halfD);
        bGroup.add(fwL);
        this.addBoxCollider(new THREE.Vector3(px - 5.6, h / 2, pz + halfD), new THREE.Vector3(8.2, h, 0.35), true, h);

        const fwR = new THREE.Mesh(new THREE.BoxGeometry(8.2, h, 0.35), mats.brick);
        fwR.position.set(5.6, h / 2, halfD);
        bGroup.add(fwR);
        this.addBoxCollider(new THREE.Vector3(px + 5.6, h / 2, pz + halfD), new THREE.Vector3(8.2, h, 0.35), true, h);

        // Rear Wall with back exit opening
        const bwL = new THREE.Mesh(new THREE.BoxGeometry(8.2, h, 0.35), mats.brick);
        bwL.position.set(-5.6, h / 2, -halfD);
        bGroup.add(bwL);
        this.addBoxCollider(new THREE.Vector3(px - 5.6, h / 2, pz - halfD), new THREE.Vector3(8.2, h, 0.35), true, h);

        const bwR = new THREE.Mesh(new THREE.BoxGeometry(8.2, h, 0.35), mats.brick);
        bwR.position.set(5.6, h / 2, -halfD);
        bGroup.add(bwR);
        this.addBoxCollider(new THREE.Vector3(px + 5.6, h / 2, pz - halfD), new THREE.Vector3(8.2, h, 0.35), true, h);

        // Left & Right end walls
        const lw = new THREE.Mesh(new THREE.BoxGeometry(0.35, h, d), mats.brick);
        lw.position.set(-halfW, h / 2, 0);
        bGroup.add(lw);
        this.addBoxCollider(new THREE.Vector3(px - halfW, h / 2, pz), new THREE.Vector3(0.35, h, d), true, h);

        const rw = new THREE.Mesh(new THREE.BoxGeometry(0.35, h, d), mats.brick);
        rw.position.set(halfW, h / 2, 0);
        bGroup.add(rw);
        this.addBoxCollider(new THREE.Vector3(px + halfW, h / 2, pz), new THREE.Vector3(0.35, h, d), true, h);

        // Pitched Corrugated Metal Roof
        const roof = new THREE.Mesh(new THREE.BoxGeometry(w + 1.2, 0.3, d + 1.0), mats.roof);
        roof.position.set(0, h + 0.15, 0);
        bGroup.add(roof);

        this.walkableSurfaces.push({
            y: h + 0.3,
            minX: px - halfW - 0.5, maxX: px + halfW + 0.5,
            minZ: pz - halfD - 0.5, maxZ: pz + halfD + 0.5
        });

        this.scene.add(bGroup);
    }

    // ── 4. ENTERABLE INDUSTRIAL VEHICLE HANGAR ────────────────────────────────
    buildEnterableHangar(px, pz, mats) {
        const bGroup = new THREE.Group();
        bGroup.position.set(px, 0, pz);

        const w = 18, d = 26, h = 8.5;
        const halfW = w / 2, halfD = d / 2;

        // Concrete Floor Slab
        const floor = new THREE.Mesh(new THREE.BoxGeometry(w, 0.2, d), mats.concrete);
        floor.position.set(0, 0.1, 0);
        bGroup.add(floor);

        // Walkable surface for Hangar Ground Floor
        this.walkableSurfaces.push({
            y: 0.1,
            minX: px - halfW, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Front Wall Left & Right Pillars (Massive Open Hangar Gate in center: 10m wide!)
        const fwL = new THREE.Mesh(new THREE.BoxGeometry(3.8, h, 0.5), mats.brick);
        fwL.position.set(-halfW + 1.9, h / 2, halfD);
        bGroup.add(fwL);
        this.addBoxCollider(new THREE.Vector3(px - halfW + 1.9, h / 2, pz + halfD), new THREE.Vector3(3.8, h, 0.5), true, h);

        const fwR = new THREE.Mesh(new THREE.BoxGeometry(3.8, h, 0.5), mats.brick);
        fwR.position.set(halfW - 1.9, h / 2, halfD);
        bGroup.add(fwR);
        this.addBoxCollider(new THREE.Vector3(px + halfW - 1.9, h / 2, pz + halfD), new THREE.Vector3(3.8, h, 0.5), true, h);

        // Rear Wall Left & Right Pillars (Open Rear Gate: 10m wide)
        const bwL = new THREE.Mesh(new THREE.BoxGeometry(3.8, h, 0.5), mats.brick);
        bwL.position.set(-halfW + 1.9, h / 2, -halfD);
        bGroup.add(bwL);
        this.addBoxCollider(new THREE.Vector3(px - halfW + 1.9, h / 2, pz - halfD), new THREE.Vector3(3.8, h, 0.5), true, h);

        const bwR = new THREE.Mesh(new THREE.BoxGeometry(3.8, h, 0.5), mats.brick);
        bwR.position.set(halfW - 1.9, h / 2, -halfD);
        bGroup.add(bwR);
        this.addBoxCollider(new THREE.Vector3(px + halfW - 1.9, h / 2, pz - halfD), new THREE.Vector3(3.8, h, 0.5), true, h);

        // Left & Right Solid Industrial Walls
        const lw = new THREE.Mesh(new THREE.BoxGeometry(0.5, h, d), mats.stucco);
        lw.position.set(-halfW, h / 2, 0);
        bGroup.add(lw);
        this.addBoxCollider(new THREE.Vector3(px - halfW, h / 2, pz), new THREE.Vector3(0.5, h, d), true, h);

        const rw = new THREE.Mesh(new THREE.BoxGeometry(0.5, h, d), mats.stucco);
        rw.position.set(halfW, h / 2, 0);
        bGroup.add(rw);
        this.addBoxCollider(new THREE.Vector3(px + halfW, h / 2, pz), new THREE.Vector3(0.5, h, d), true, h);

        // Vaulted Industrial Roof Arch
        const roofGeom = new THREE.CylinderGeometry(halfW + 0.5, halfW + 0.5, d + 1.0, 16, 1, false, 0, Math.PI);
        roofGeom.rotateZ(Math.PI / 2);
        roofGeom.rotateY(Math.PI / 2);
        const roofMesh = new THREE.Mesh(roofGeom, mats.roof);
        roofMesh.position.set(0, h, 0);
        bGroup.add(roofMesh);

        this.walkableSurfaces.push({
            y: h + 1.5,
            minX: px - halfW, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        this.scene.add(bGroup);
    }

    createIndustrialLandmarks() {
        // 1. Massive Nuclear Cooling Tower (from Reference Image 1)
        if (window.blenderAssetLoader && window.blenderAssetLoader.models['cooling_tower']) {
            const blenderTower = window.blenderAssetLoader.createCoolingTowerInstance(25, -95);
            if (blenderTower) {
                this.scene.add(blenderTower);
                this.addBoxCollider(new THREE.Vector3(25, 24, -95), new THREE.Vector3(36, 48, 36), false);
            }
        }

        // 2. Telecommunication / Radio Tower with blinking beacon
        const radioGroup = new THREE.Group();
        radioGroup.position.set(65, 0, -100);

        const mastGeom = new THREE.CylinderGeometry(0.8, 4.5, 52, 4);
        const mastMat = new THREE.MeshStandardMaterial({ color: 0xDC2626, metalness: 0.7, roughness: 0.3 });
        const mast = new THREE.Mesh(mastGeom, mastMat);
        mast.position.y = 26;
        mast.castShadow = true;
        radioGroup.add(mast);

        const beaconGeom = new THREE.SphereGeometry(0.7, 8, 8);
        const beaconMat = new THREE.MeshBasicMaterial({ color: 0xEF4444 });
        const beacon = new THREE.Mesh(beaconGeom, beaconMat);
        beacon.position.y = 52.5;
        radioGroup.add(beacon);

        this.scene.add(radioGroup);
        this.addBoxCollider(radioGroup.position, new THREE.Vector3(6, 52, 6), false);
    }

    createMilitaryHangarsAndContainers() {
        // Multi-colored Shipping Containers Cluster (Red, Blue, Yellow, Green)
        const containerColors = [0xDC2626, 0x2563EB, 0xEAB308, 0x16A34A, 0x475569];
        const containerSpawns = [
            { x: -125, y: 1.3, z: -55, c: containerColors[0] },
            { x: -125, y: 1.3, z: -48, c: containerColors[1] },
            { x: -125, y: 3.9, z: -51, c: containerColors[2] }, // Stacked
            { x: -115, y: 1.3, z: -52, c: containerColors[3] },
            { x: 85,   y: 1.3, z: -80, c: containerColors[0] },
            { x: 85,   y: 1.3, z: -73, c: containerColors[1] },
            { x: 85,   y: 3.9, z: -76, c: containerColors[4] }
        ];

        containerSpawns.forEach(cs => {
            if (window.blenderAssetLoader && window.blenderAssetLoader.models['container']) {
                const bCont = window.blenderAssetLoader.createContainerInstance(cs.x, cs.y - 1.3, cs.z, cs.c);
                if (bCont) this.scene.add(bCont);
            }
            this.addBoxCollider(new THREE.Vector3(cs.x, cs.y, cs.z), new THREE.Vector3(6.5, 2.6, 2.8), true, cs.y + 1.3);
        });
    }

    createAircraftWreckageAndVehicles() {
        // McLaren Supercar on Runway
        if (window.blenderAssetLoader && window.blenderAssetLoader.models['mclaren']) {
            const mclaren = window.blenderAssetLoader.createMcLarenInstance(75, 0.08, 25, Math.PI / 12);
            if (mclaren) {
                this.scene.add(mclaren);
                this.addBoxCollider(new THREE.Vector3(75, 0.7, 25), new THREE.Vector3(4.8, 1.4, 2.2), false);
            }
        }
    }

    createWatchtowersAndBunkers() {
        const towerLocs = [
            { x: -20, z: -115 }, // Northern Perimeter
            { x: -120, z: 80 },  // South-West Coast
            { x: 120, z: 90 }    // South-East Pier
        ];

        towerLocs.forEach(loc => {
            if (window.blenderAssetLoader && window.blenderAssetLoader.models['watchtower']) {
                const bTower = window.blenderAssetLoader.createWatchtowerInstance(loc.x, loc.z);
                if (bTower) this.scene.add(bTower);
            }
            this.addBoxCollider(new THREE.Vector3(loc.x, 6.2, loc.z), new THREE.Vector3(4.5, 12.4, 4.5), true, 12.4);
            this.walkableSurfaces.push({
                y: 12.4,
                minX: loc.x - 2.2, maxX: loc.x + 2.2,
                minZ: loc.z - 2.2, maxZ: loc.z + 2.2
            });
        });
    }

    createFoliageAndProps() {
        if (!window.blenderAssetLoader) return;

        // 1. High-Quality Oak Trees (Detailed Forest & Parklands from user's Tree.fbx)
        for (let i = 0; i < 75; i++) {
            const angle = Math.random() * Math.PI * 2;
            const dist = 35 + Math.random() * 270;
            const px = Math.cos(angle) * dist;
            const pz = Math.sin(angle) * dist;

            // Don't spawn on main highways
            if (Math.abs(px) < 14 || Math.abs(pz) < 14) continue;
            // Don't spawn inside airport runway or commercial dense block
            if (px > 125 && px < 165 && pz > 30 && pz < 190) continue;
            if (px < -50 && px > -170 && pz < -30 && pz > -170) continue;

            const tree = window.blenderAssetLoader.createTreeInstance(px, pz, 'oak', 0.85 + Math.random() * 0.4);
            if (tree) this.scene.add(tree);
        }

        // 2. Coastal Banana Palms (PBR Tropical Foliage from user's Banana_Plant_01.fbx)
        // Replacing the bad procedural cone palm tree with pristine high-fidelity tropical palms
        for (let i = 0; i < 55; i++) {
            const angle = (i / 55) * Math.PI * 2;
            const dist = 315 + (i % 5) * 8;
            const px = Math.cos(angle) * dist;
            const pz = Math.sin(angle) * dist;

            const palm = window.blenderAssetLoader.createTreeInstance(px, pz, 'palm', 1.0 + Math.random() * 0.35);
            if (palm) this.scene.add(palm);
        }

        // 3. Residential Villa Front Yard Palms
        const resPalms = [
            { x: 45, z: -40 }, { x: 80, z: -40 }, { x: 125, z: -45 },
            { x: 45, z: -105 }, { x: 80, z: -110 }, { x: 135, z: -115 },
            { x: 175, z: -110 }, { x: 100, z: -140 }, { x: 140, z: -145 }
        ];
        resPalms.forEach(p => {
            const palm = window.blenderAssetLoader.createTreeInstance(p.x, p.z, 'palm', 1.15);
            if (palm) this.scene.add(palm);
        });

        // 4. Rock Boulders Across Open Fields
        for (let i = 0; i < 40; i++) {
            const angle = Math.random() * Math.PI * 2;
            const dist = 45 + Math.random() * 250;
            const px = Math.cos(angle) * dist;
            const pz = Math.sin(angle) * dist;

            if (Math.abs(px) < 14 || Math.abs(pz) < 14) continue;
            if (px > 125 && px < 165 && pz > 30 && pz < 190) continue;

            const rock = window.blenderAssetLoader.createRockInstance(px, pz, 1.0 + Math.random() * 0.7);
            if (rock) this.scene.add(rock);
        }

        // 5. Iconic Roadside Supply Airdrop Crate with Radiant Yellow Light Beam
        const dropCrate = window.blenderAssetLoader.createAirdropInstance(14, -22);
        if (dropCrate) this.scene.add(dropCrate);

        // Towering Golden Light Pillar shooting into the sky from the airdrop
        const beamGeom = new THREE.CylinderGeometry(0.5, 1.2, 80, 16);
        const beamMat = new THREE.MeshBasicMaterial({
            color: 0xFACC15,
            transparent: true,
            opacity: 0.55,
            side: THREE.DoubleSide
        });
        const lightBeam = new THREE.Mesh(beamGeom, beamMat);
        lightBeam.position.set(14, 40, -22);
        this.scene.add(lightBeam);

        // Roadside Tropical Palm Trees along Highway
        const roadPalms = [
            { x: 12, z: -15 }, { x: -12, z: -25 }, { x: 13, z: -55 },
            { x: -13, z: 25 }, { x: 12, z: 50 },  { x: -13, z: 75 },
            { x: 12, z: 120 }, { x: -12, z: 150 }, { x: 12, z: -120 }
        ];
        roadPalms.forEach(p => {
            const palm = window.blenderAssetLoader.createTreeInstance(p.x, p.z, 'palm', 1.1 + Math.random() * 0.25);
            if (palm) this.scene.add(palm);
        });
    }

    createHorizonMountains() {
        // Grand 360-degree majestic mountain range on the far horizon
        const mountGroup = new THREE.Group();

        // 1. Textures for Mountain slopes (Rocky cliffs & snow-capped peaks)
        const rockCanvas = document.createElement('canvas');
        rockCanvas.width = 256;
        rockCanvas.height = 256;
        const rctx = rockCanvas.getContext('2d');
        rctx.fillStyle = '#334155';
        rctx.fillRect(0, 0, 256, 256);
        for (let y = 0; y < 256; y += 8) {
            rctx.fillStyle = (y % 16 === 0) ? '#1E293B' : '#475569';
            rctx.fillRect(0, y, 256, 4);
        }
        const rockTex = new THREE.CanvasTexture(rockCanvas);
        rockTex.wrapS = THREE.RepeatWrapping;
        rockTex.wrapT = THREE.RepeatWrapping;
        rockTex.repeat.set(4, 8);

        const rockMat = new THREE.MeshStandardMaterial({
            map: rockTex,
            roughness: 0.88,
            metalness: 0.1,
            flatShading: true
        });

        const snowMat = new THREE.MeshStandardMaterial({
            color: 0xF8FAFC,
            roughness: 0.65,
            metalness: 0.05,
            flatShading: true
        });

        const lushMat = new THREE.MeshStandardMaterial({
            color: 0x14532D, // Lush dark pine green
            roughness: 0.85,
            metalness: 0.05,
            flatShading: true
        });

        // 32 Panoramic Mountain Formations around the horizon (520m to 780m radius)
        const count = 32;
        for (let i = 0; i < count; i++) {
            const angle = (i / count) * Math.PI * 2;
            const dist = 540 + (i % 4) * 60;
            const mx = Math.cos(angle) * dist;
            const mz = Math.sin(angle) * dist;

            const height = 150 + (i % 5) * 32 + (i % 2) * 25; // 150m to 280m soaring peaks!
            const baseRadius = 120 + (i % 3) * 35;

            // Mountain cone body with natural asymmetric ridge distortion
            const mGeom = new THREE.ConeGeometry(baseRadius, height, 12, 2, false);
            const posAttr = mGeom.attributes.position;
            for (let v = 0; v < posAttr.count; v++) {
                const py = posAttr.getY(v);
                if (py < height / 2 - 5) {
                    const px = posAttr.getX(v);
                    const pz = posAttr.getZ(v);
                    const noise = Math.sin(px * 0.05) * Math.cos(pz * 0.05) * 16;
                    posAttr.setX(v, px + noise);
                    posAttr.setZ(v, pz + noise);
                }
            }
            mGeom.computeVertexNormals();

            const mat = (i % 3 === 0) ? rockMat : ((i % 3 === 1) ? lushMat : rockMat);
            const peak = new THREE.Mesh(mGeom, mat);
            peak.position.set(mx, height / 2 - 12, mz);
            peak.rotation.y = (i * 1.37);
            mountGroup.add(peak);

            // Snow-capped summits on tallest mountains (height >= 185m)
            if (height >= 185) {
                const capHeight = height * 0.28;
                const capRadius = baseRadius * 0.32;
                const capGeom = new THREE.ConeGeometry(capRadius, capHeight, 10);
                const capMesh = new THREE.Mesh(capGeom, snowMat);
                capMesh.position.set(mx, height - 12 - capHeight / 2, mz);
                mountGroup.add(capMesh);
            }
        }
        this.scene.add(mountGroup);
    }

    createFloatingBlimp() {
        // Floating "WARLAND" Airship (Reference Image 1)
        this.blimp = new THREE.Group();
        this.blimp.position.set(0, 85, -140);

        const blimpBodyMat = new THREE.MeshStandardMaterial({ color: 0xF59E0B, metalness: 0.4, roughness: 0.3 });
        const blimpGeom = new THREE.SphereGeometry(14, 20, 16);
        blimpGeom.scale(1.0, 0.7, 2.6);
        const blimpMesh = new THREE.Mesh(blimpGeom, blimpBodyMat);
        this.blimp.add(blimpMesh);

        // Navy Blue side stripe
        const stripeMat = new THREE.MeshStandardMaterial({ color: 0x1E3A8A, roughness: 0.4 });
        const stripeGeom = new THREE.CylinderGeometry(14.2, 14.2, 4.0, 24, 1, true);
        stripeGeom.scale(1.0, 2.4, 0.65);
        stripeGeom.rotateZ(Math.PI / 2);
        const stripe = new THREE.Mesh(stripeGeom, stripeMat);
        this.blimp.add(stripe);

        // Gondola under blimp
        const gondolaMat = new THREE.MeshStandardMaterial({ color: 0x0F172A, roughness: 0.5 });
        const gondolaGeom = new THREE.BoxGeometry(3.5, 2.5, 12);
        const gondola = new THREE.Mesh(gondolaGeom, gondolaMat);
        gondola.position.y = -10.5;
        this.blimp.add(gondola);

        this.scene.add(this.blimp);
    }

    update(dt) {
        // 1. Circling Airship Blimp
        if (this.blimp) {
            this.blimpAngle += dt * 0.05;
            const bx = Math.cos(this.blimpAngle) * 200;
            const bz = Math.sin(this.blimpAngle) * 200;
            this.blimp.position.x = bx;
            this.blimp.position.z = bz;
            this.blimp.rotation.y = -this.blimpAngle + Math.PI / 2;
        }

        // 2. Subtle Ocean Wave Motion
        if (this.water) {
            this.waterTime += dt * 1.5;
            this.water.position.y = -0.55 + Math.sin(this.waterTime) * 0.08;
        }
    }

    addBoxCollider(pos, size, isObstacle = true, topY = 0) {
        this.colliders.push({
            min: new THREE.Vector3(pos.x - size.x / 2, pos.y - size.y / 2, pos.z - size.z / 2),
            max: new THREE.Vector3(pos.x + size.x / 2, pos.y + size.y / 2, pos.z + size.z / 2),
            isObstacle: isObstacle,
            topY: topY || (pos.y + size.y / 2)
        });
    }

    resolveCollision(currentPos, nextPos, radius = 0.45, height = 1.8) {
        let finalPos = nextPos.clone();
        let groundY = 0.0;
        let ceilingY = 999.0;

        // Check Walkable Surfaces (Floor 1, Floor 2, Floor 3, Rooftops, Bridges, Watchtowers)
        for (const surf of this.walkableSurfaces) {
            if (finalPos.x >= surf.minX && finalPos.x <= surf.maxX &&
                finalPos.z >= surf.minZ && finalPos.z <= surf.maxZ) {
                if (currentPos.y >= surf.y - 0.45 && currentPos.y <= surf.y + 3.8) {
                    groundY = Math.max(groundY, surf.y);
                }
            }
        }

        // Check Obstacle Colliders (Walls, Stairs, Roofs)
        for (const col of this.colliders) {
            // Check Ceiling Collision directly overhead
            if (finalPos.x >= col.min.x && finalPos.x <= col.max.x &&
                finalPos.z >= col.min.z && finalPos.z <= col.max.z) {
                if (col.min.y > currentPos.y + 0.5 && col.min.y <= currentPos.y + height + 0.3) {
                    ceilingY = Math.min(ceilingY, col.min.y);
                }
            }

            // Step onto stairs or top of obstacle if walking onto it
            if (currentPos.y >= col.topY - 0.55) {
                if (finalPos.x >= col.min.x && finalPos.x <= col.max.x &&
                    finalPos.z >= col.min.z && finalPos.z <= col.max.z) {
                    groundY = Math.max(groundY, col.topY);
                }
                continue;
            }

            // Vertical clearance checks:
            // 1. If obstacle is completely above player's head (doorway lintel, ceiling, arch), player walks freely underneath!
            if (col.min.y >= currentPos.y + height - 0.15) {
                continue;
            }
            // 2. If obstacle is completely below player's feet, ignore horizontal blocking
            if (col.max.y <= currentPos.y + 0.08) {
                continue;
            }

            // Cylinder-AABB horizontal collision
            const closestX = Math.max(col.min.x, Math.min(finalPos.x, col.max.x));
            const closestZ = Math.max(col.min.z, Math.min(finalPos.z, col.max.z));

            const distX = finalPos.x - closestX;
            const distZ = finalPos.z - closestZ;
            const distSq = distX * distX + distZ * distZ;

            if (distSq < radius * radius) {
                const dist = Math.sqrt(distSq);
                if (dist > 0.001) {
                    const overlap = radius - dist;
                    finalPos.x += (distX / dist) * overlap;
                    finalPos.z += (distZ / dist) * overlap;
                } else {
                    finalPos.x = currentPos.x;
                    finalPos.z = currentPos.z;
                }
            }
        }

        return { position: finalPos, groundY: groundY, ceilingY: ceilingY };
    }

    generateSpawnPoints() {
        for (let i = 0; i < 40; i++) {
            const angle = (i / 40) * Math.PI * 2;
            const r = 30 + Math.random() * 260;
            this.spawnPoints.push(new THREE.Vector3(Math.cos(angle) * r, 0, Math.sin(angle) * r));
        }
    }
}

window.WarLandMap = WarLandMap;
