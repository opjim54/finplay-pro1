// War Land 3D - High-Fidelity Coastal Military Island Map (v5 Enhanced)
// Detailed PBR architecture, realistic asphalt highways, sandy shores, rolling horizon mountains,
// animated ocean waves, supply airdrop beacons, and floating "WARLAND" blimp!

class WarLandMap {
    constructor(scene) {
        this.scene = scene;
        this.colliders = []; // AABB { min: Vector3, max: Vector3, isStairs: bool, topY: float, isObstacle: bool }
        this.walkableSurfaces = []; // Y-planes { y, minX, maxX, minZ, maxZ }
        this.walkableRamps = []; // Ramps { minX, maxX, minZ, maxZ, yStart, yEnd, dir: 1|-1, axis: 'z'|'x' }
        this.spatialGrid = new Map(); // Spatial partition for 60fps fast collision
        this.gridCellSize = 25;
        this.buildingConfigs = [];
        this.buildingFootprints = []; // { minX, maxX, minZ, maxZ } for zero-overlap prop placement
        this.roadRects = [];          // { minX, maxX, minZ, maxZ } for zero-overlap tree/rock placement
        this.spawnPoints = [];
        this.blimp = null;
        this.water = null;
        this.blimpAngle = 0;
        this.waterTime = 0;
        this.islandRadius = 520;
    }

    registerFootprint(cx, cz, halfW, halfD) {
        this.buildingFootprints.push({
            minX: cx - halfW,
            maxX: cx + halfW,
            minZ: cz - halfD,
            maxZ: cz + halfD
        });
    }

    registerRoadRect(cx, cz, width, length) {
        this.roadRects.push({
            minX: cx - width / 2,
            maxX: cx + width / 2,
            minZ: cz - length / 2,
            maxZ: cz + length / 2
        });
    }

    isClearForProp(x, z, pad = 3.5) {
        // Keep inside walkable island shore
        if (Math.hypot(x, z) > 495) return false;
        for (let i = 0; i < this.roadRects.length; i++) {
            const r = this.roadRects[i];
            if (x >= r.minX - pad && x <= r.maxX + pad && z >= r.minZ - pad && z <= r.maxZ + pad) {
                return false;
            }
        }
        for (let i = 0; i < this.buildingFootprints.length; i++) {
            const b = this.buildingFootprints[i];
            if (x >= b.minX - pad && x <= b.maxX + pad && z >= b.minZ - pad && z <= b.maxZ + pad) {
                return false;
            }
        }
        return true;
    }

    addWalkableSurface(surf) {
        this.walkableSurfaces.push(surf);
    }

    addWalkableRamp(minX, maxX, minZ, maxZ, yStart, yEnd, dir = 1, axis = 'z') {
        this.walkableRamps.push({
            minX, maxX, minZ, maxZ, yStart, yEnd, dir, axis
        });
    }

    getNearbyColliders(x, z, radius = 2.0) {
        const minCellX = Math.floor((x - radius) / this.gridCellSize);
        const maxCellX = Math.floor((x + radius) / this.gridCellSize);
        const minCellZ = Math.floor((z - radius) / this.gridCellSize);
        const maxCellZ = Math.floor((z + radius) / this.gridCellSize);
        const nearby = [];
        const seen = new Set();
        for (let cx = minCellX; cx <= maxCellX; cx++) {
            for (let cz = minCellZ; cz <= maxCellZ; cz++) {
                const list = this.spatialGrid.get(`${cx},${cz}`);
                if (list) {
                    for (let i = 0; i < list.length; i++) {
                        const c = list[i];
                        if (!seen.has(c)) {
                            seen.add(c);
                            nearby.push(c);
                        }
                    }
                }
            }
        }
        return nearby;
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
        // 1. Vast Ocean Water Plane with Sun Specular (3600m x 3600m)
        const waterGeom = new THREE.PlaneGeometry(3600, 3600, 32, 32);
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
        groundTex.repeat.set(62, 62);

        // Main Island Ground Surface (Expanded 520m Radius, Top sits at Y = 0.0)
        const islandGeom = new THREE.CylinderGeometry(520, 555, 3.0, 72);
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
            minX: -510,
            maxX: 510,
            minZ: -510,
            maxZ: 510
        });

        // 3. Sandy Shoreline Ring (475m to 530m, sits at Y = 0.02)
        const sandGeom = new THREE.RingGeometry(475, 530, 72);
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

        const makeRoadMat = (repeatY) => {
            const tex = new THREE.CanvasTexture(roadCanvas);
            tex.wrapS = THREE.RepeatWrapping;
            tex.wrapT = THREE.RepeatWrapping;
            tex.repeat.set(1, repeatY);
            return new THREE.MeshStandardMaterial({ map: tex, roughness: 0.75, metalness: 0.1 });
        };

        const addRoad = (cx, cz, width, length, isEW = false, yPos = 0.04) => {
            const geom = new THREE.PlaneGeometry(width, length);
            const mat = makeRoadMat(Math.max(4, Math.round(length / 22)));
            const mesh = new THREE.Mesh(geom, mat);
            if (isEW) {
                mesh.rotation.set(-Math.PI / 2, 0, Math.PI / 2);
                this.registerRoadRect(cx, cz, length, width);
            } else {
                mesh.rotation.x = -Math.PI / 2;
                this.registerRoadRect(cx, cz, width, length);
            }
            mesh.position.set(cx, yPos, cz);
            mesh.receiveShadow = true;
            this.scene.add(mesh);
        };

        // 1. Main North-South Highway (860m long across island)
        addRoad(0, 0, 16, 860, false, 0.04);

        // 2. Main East-West Cross Highway (860m long)
        addRoad(0, 0, 16, 860, true, 0.045);

        // 3. West Downtown Commercial Avenue & Cross Street
        addRoad(-185, -30, 14, 420, false, 0.05);
        addRoad(-155, -140, 12, 310, true, 0.052);

        // 4. North-East Modern Suburb Boulevard & Cross Street
        addRoad(165, -165, 12, 330, false, 0.05);
        addRoad(155, -170, 12, 310, true, 0.052);

        // 5. South Industrial Access Road
        addRoad(-20, 210, 12, 440, true, 0.05);

        // 6. East Airbase & Supercar Runway (36m wide, 300m long at x=255, z=30..330)
        const runwayMat = new THREE.MeshStandardMaterial({ color: 0x0F172A, roughness: 0.65, metalness: 0.2 });
        const runwayGeom = new THREE.PlaneGeometry(36, 300);
        const runway = new THREE.Mesh(runwayGeom, runwayMat);
        runway.rotation.x = -Math.PI / 2;
        runway.position.set(255, 0.06, 180);
        runway.receiveShadow = true;
        this.scene.add(runway);
        this.registerRoadRect(255, 180, 46, 310);

        // Runway Yellow Dashed Center Line & Threshold Stripes
        const rwLineMat = new THREE.MeshBasicMaterial({ color: 0xF59E0B });
        const rwLine = new THREE.Mesh(new THREE.PlaneGeometry(1.8, 286), rwLineMat);
        rwLine.rotation.x = -Math.PI / 2;
        rwLine.position.set(255, 0.07, 180);
        this.scene.add(rwLine);

        // 7. Coastal Bridge extending over eastern ocean (x: 420..560)
        const bridgeGeom = new THREE.BoxGeometry(140, 1.4, 18);
        const bridgeMat = new THREE.MeshStandardMaterial({ color: 0x475569, metalness: 0.6, roughness: 0.35 });
        const bridge = new THREE.Mesh(bridgeGeom, bridgeMat);
        bridge.position.set(490, 0.7, 0);
        bridge.castShadow = true;
        bridge.receiveShadow = true;
        this.scene.add(bridge);
        this.registerRoadRect(490, 0, 145, 22);

        this.addBoxCollider(bridge.position, new THREE.Vector3(140, 1.4, 18), true, 1.4);
        this.walkableSurfaces.push({
            y: 1.4,
            minX: 420, maxX: 560,
            minZ: -9, maxZ: 9
        });
    }

    // ── WEST DOWNTOWN COMMERCIAL DISTRICT (building_city_blocks.glb + building_multistory.glb) ──
    createCityBlocksDistrict() {
        if (!window.blenderAssetLoader) return;
        const cityConfigs = [
            // North-West Commercial Blocks (between x=-100..-310, z=-250..-35, clear of West Ave x=-185 & Cross z=-140)
            { name: 'bina',     x: -115, z: -55,  rot: 0 },
            { name: 'bina.001', x: -115, z: -102, rot: Math.PI / 2 },
            { name: 'bina.002', x: -115, z: -185, rot: 0 },
            { name: 'bina.003', x: -115, z: -235, rot: 0 },
            { name: 'bina.004', x: -245, z: -55,  rot: Math.PI / 2 },
            { name: 'bina.005', x: -255, z: -105, rot: 0 },
            { name: 'bina.006', x: -245, z: -185, rot: 0 },
            { name: 'bina.007', x: -255, z: -235, rot: Math.PI / 2 },
            { name: 'bina.008', x: -305, z: -90,  rot: 0 },
            // South-West Commercial Plaza (west of West Avenue, z=+45..+145)
            { name: 'bina.009', x: -240, z: 55,   rot: 0 },
            { name: 'bina.010', x: -295, z: 65,   rot: Math.PI / 2 },
            { name: 'bina.014', x: -245, z: 125,  rot: 0 }
        ];

        cityConfigs.forEach(cfg => {
            const b = window.blenderAssetLoader.createCityBuildingInstance(cfg.name, cfg.x, cfg.z, cfg.rot);
            if (b) {
                this.scene.add(b);
                const ws = b.userData.worldBoxSize || new THREE.Vector3(24, 36, 24);
                this.addBoxCollider(new THREE.Vector3(cfg.x, ws.y / 2, cfg.z), ws, true, ws.y);
                this.registerFootprint(cfg.x, cfg.z, ws.x / 2 + 2.5, ws.z / 2 + 2.5);
                this.buildingConfigs.push({ x: cfg.x, z: cfg.z, w: ws.x, d: ws.z, type: 'city', rot: 0 });
            }
        });

        // Supplied Multi-Story Commercial Apartment Towers (building_multistory.glb)
        const multiConfigs = [
            { x: -140, z: 55,   rot: 0 },
            { x: -140, z: 115,  rot: Math.PI / 2 },
            { x: -305, z: -185, rot: 0 },
            { x: -295, z: 135,  rot: Math.PI }
        ];
        multiConfigs.forEach(cfg => {
            const m = window.blenderAssetLoader.createBuildingInstance('multistory', cfg.x, cfg.z, cfg.rot);
            if (m) {
                this.scene.add(m);
                m.updateMatrixWorld(true);
                const wb = new THREE.Box3().setFromObject(m);
                const ws = new THREE.Vector3();
                wb.getSize(ws);
                this.addBoxCollider(new THREE.Vector3(cfg.x, ws.y / 2, cfg.z), ws, true, ws.y);
                this.registerFootprint(cfg.x, cfg.z, ws.x / 2 + 2.5, ws.z / 2 + 2.5);
                this.buildingConfigs.push({ x: cfg.x, z: cfg.z, w: ws.x, d: ws.z, type: 'city', rot: 0 });
            }
        });
    }

    // ── NORTH-EAST MODERN RESIDENTIAL SUBURB (building_modern_houses.glb) ────
    createModernResidentialDistrict() {
        if (!window.blenderAssetLoader) return;
        // Cleanly laid out around Suburb Boulevard (x=165) & Cross Street (z=-170) with ZERO overlaps
        const modernConfigs = [
            // South-West Quadrant of NE Suburb (x: 80..135, z: -55..-140)
            { idx: 0,  x: 92,  z: -62,  rot: 0 },
            { idx: 1,  x: 132, z: -62,  rot: 0 },
            { idx: 2,  x: 92,  z: -118, rot: Math.PI },
            { idx: 3,  x: 132, z: -118, rot: Math.PI },
            // South-East Quadrant of NE Suburb (x: 200..295, z: -62..-125)
            { idx: 4,  x: 205, z: -62,  rot: 0 },
            { idx: 5,  x: 255, z: -62,  rot: 0 },
            { idx: 6,  x: 205, z: -118, rot: Math.PI },
            { idx: 7,  x: 255, z: -118, rot: Math.PI },
            { idx: 8,  x: 300, z: -90,  rot: -Math.PI / 2 },
            // North-West Quadrant of NE Suburb (x: 85..135, z: -210..-285)
            { idx: 9,  x: 92,  z: -215, rot: 0 },
            { idx: 10, x: 132, z: -215, rot: 0 },
            { idx: 11, x: 112, z: -275, rot: Math.PI },
            // North-East Quadrant of NE Suburb (x: 205..270, z: -215..-275)
            { idx: 12, x: 205, z: -215, rot: 0 },
            { idx: 13, x: 258, z: -215, rot: -Math.PI / 4 },
            { idx: 14, x: 225, z: -275, rot: Math.PI }
        ];

        modernConfigs.forEach(cfg => {
            const h = window.blenderAssetLoader.createModernHouseInstance(cfg.idx, cfg.x, cfg.z, cfg.rot, 1.25);
            if (h) {
                this.scene.add(h);
                const ws = h.userData.worldBoxSize || new THREE.Vector3(15, 9, 13);
                this.addBoxCollider(new THREE.Vector3(cfg.x, ws.y / 2, cfg.z), ws, true, ws.y);
                this.registerFootprint(cfg.x, cfg.z, ws.x / 2 + 2.5, ws.z / 2 + 2.5);
                this.buildingConfigs.push({ x: cfg.x, z: cfg.z, w: ws.x, d: ws.z, type: 'modern', rot: 0 });
            }
        });
    }

    createMultiStoryBuildings() {
        // Enterable multi-floor buildings with interior stairs, balconies & rooftops — zero overlap with GLB districts!
        const buildingConfigs = [
            // Central Crossroads Tactical Hub (near 0,0)
            { x: 38,  z: -38,  type: 'villa',     rot: 0, w: 16, d: 14 },
            { x: -42, z: -44,  type: 'townhouse', rot: 0, w: 12, d: 12 },
            { x: 42,  z: 44,   type: 'barracks',  rot: 0, w: 22, d: 10 },
            { x: -42, z: 42,   type: 'villa',     rot: 0, w: 16, d: 14 },

            // North Hills Compound (along NS Highway North)
            { x: -42, z: -180, type: 'villa',     rot: 0, w: 16, d: 14 },
            { x: 40,  z: -195, type: 'townhouse', rot: 0, w: 12, d: 12 },
            { x: -38, z: -275, type: 'barracks',  rot: 0, w: 22, d: 10 },
            { x: 42,  z: -285, type: 'villa',     rot: 0, w: 16, d: 14 },

            // South-Central Military Garrison (between EW highway and South Industrial road)
            { x: -72, z: 95,   type: 'townhouse', rot: 0, w: 12, d: 12 },
            { x: -65, z: 155,  type: 'barracks',  rot: 0, w: 22, d: 10 },
            { x: 48,  z: 135,  type: 'townhouse', rot: 0, w: 12, d: 12 },
            { x: 105, z: 90,   type: 'villa',     rot: 0, w: 16, d: 14 },
            { x: 105, z: 160,  type: 'barracks',  rot: 0, w: 22, d: 10 },

            // East Airbase Hangars (flanking West side of Runway x=255)
            { x: 198, z: 105,  type: 'hangar',    rot: 0, w: 20, d: 28 },
            { x: 198, z: 225,  type: 'hangar',    rot: 0, w: 20, d: 28 }
        ];

        this.buildingConfigs.push(...buildingConfigs);
        buildingConfigs.forEach(cfg => {
            this.buildAccessibleBuilding(cfg.x, cfg.z, cfg.type, cfg.rot);
            this.registerFootprint(cfg.x, cfg.z, (cfg.w || 16) / 2 + 2.5, (cfg.d || 16) / 2 + 2.5);
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

        this.walkableSurfaces.push({
            y: 0.1,
            minX: px - halfW, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Stone Base Plinth Around Exterior
        const plinth = new THREE.Mesh(new THREE.BoxGeometry(w + 0.5, 0.2, d + 0.5), mats.brick);
        plinth.position.set(0, 0.1, 0);
        bGroup.add(plinth);

        // FLOOR 1 EXTERIOR WALLS (Doorway: 3.2m wide, X: -1.6 to +1.6)
        const fwL = new THREE.Mesh(new THREE.BoxGeometry(5.2, h1, 0.4), mats.stucco);
        fwL.position.set(-4.2, h1 / 2, halfD);
        fwL.castShadow = true; bGroup.add(fwL);
        this.addBoxCollider(new THREE.Vector3(px - 4.2, h1 / 2, pz + halfD), new THREE.Vector3(5.2, h1, 0.4), true, h1);

        const fwR = new THREE.Mesh(new THREE.BoxGeometry(5.2, h1, 0.4), mats.stucco);
        fwR.position.set(4.2, h1 / 2, halfD);
        fwR.castShadow = true; bGroup.add(fwR);
        this.addBoxCollider(new THREE.Vector3(px + 4.2, h1 / 2, pz + halfD), new THREE.Vector3(5.2, h1, 0.4), true, h1);

        // Doorway Lintel Header (above 2.7m)
        const doorLintel = new THREE.Mesh(new THREE.BoxGeometry(3.6, h1 - 2.7, 0.45), mats.trim);
        doorLintel.position.set(0, 2.7 + (h1 - 2.7) / 2, halfD);
        bGroup.add(doorLintel);
        this.addBoxCollider(new THREE.Vector3(px, 2.7 + (h1 - 2.7) / 2, pz + halfD), new THREE.Vector3(3.6, h1 - 2.7, 0.45), true, h1);

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

        // STAIRCASE 1: GROUND TO FLOOR 2 (Inner left wall, front to back)
        const numSteps = 12;
        const stepWidth = 2.0;
        const stepRun = 5.0;
        const stepDepth = stepRun / numSteps;
        const stepHeight = h1 / numSteps;
        const stairStartX = -halfW + 0.6 + stepWidth / 2;
        const stairStartZ = -halfD + 1.2;

        for (let i = 0; i < numSteps; i++) {
            const sy = (i + 0.5) * stepHeight;
            const sz = stairStartZ + (i + 0.5) * stepDepth;
            const stepMesh = new THREE.Mesh(new THREE.BoxGeometry(stepWidth, stepHeight, stepDepth + 0.04), mats.trim);
            stepMesh.position.set(stairStartX, sy, sz);
            bGroup.add(stepMesh);
            this.addBoxCollider(new THREE.Vector3(px + stairStartX, sy, pz + sz), new THREE.Vector3(stepWidth, stepHeight, stepDepth), false, (i + 1) * stepHeight, true);
        }

        // Smooth elevation ramp for Staircase 1
        this.addWalkableRamp(
            px + stairStartX - stepWidth / 2 - 0.2,
            px + stairStartX + stepWidth / 2 + 0.2,
            pz + stairStartZ - 0.2,
            pz + stairStartZ + stepRun + 0.4,
            0.1, h1, 1, 'z'
        );

        // FLOOR 2 SLAB (Y = 3.8, leaves left stair opening clear!)
        const f2Right = new THREE.Mesh(new THREE.BoxGeometry(w - 3.0, 0.25, d), mats.wood);
        f2Right.position.set(1.5, h1, 0);
        f2Right.receiveShadow = true;
        bGroup.add(f2Right);

        this.walkableSurfaces.push({
            y: h1,
            minX: px - halfW + 2.8, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Floor 2 back landing connecting top of stairs
        const f2BackLanding = new THREE.Mesh(new THREE.BoxGeometry(3.0, 0.25, d - stepRun - 1.2), mats.wood);
        f2BackLanding.position.set(-halfW + 1.5, h1, halfD - (d - stepRun - 1.2) / 2);
        bGroup.add(f2BackLanding);

        this.walkableSurfaces.push({
            y: h1,
            minX: px - halfW, maxX: px - halfW + 3.0,
            minZ: pz + halfD - (d - stepRun - 1.2), maxZ: pz + halfD
        });

        // FLOOR 2 WALLS & BALCONY (Y = 3.8 to 7.6)
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

        const railing = new THREE.Mesh(new THREE.BoxGeometry(6.5, 0.9, 0.15), mats.trim);
        railing.position.set(0, h1 + 0.45, halfD + 2.45);
        bGroup.add(railing);
        this.addBoxCollider(new THREE.Vector3(px, h1 + 0.45, pz + halfD + 2.45), new THREE.Vector3(6.5, 0.9, 0.15), true, h1 + 0.9);

        // Floor 2 Perimeter Walls
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

        // STAIRCASE 2: FLOOR 2 TO ROOFTOP (Along inner right wall, back to front)
        const stair2StartX = halfW - 0.6 - stepWidth / 2;
        const stair2StartZ = halfD - 1.2;

        for (let i = 0; i < numSteps; i++) {
            const sy = h1 + (i + 0.5) * stepHeight;
            const sz = stair2StartZ - (i + 0.5) * stepDepth;
            const stepMesh = new THREE.Mesh(new THREE.BoxGeometry(stepWidth, stepHeight, stepDepth + 0.04), mats.trim);
            stepMesh.position.set(stair2StartX, sy, sz);
            bGroup.add(stepMesh);
            this.addBoxCollider(new THREE.Vector3(px + stair2StartX, sy, pz + sz), new THREE.Vector3(stepWidth, stepHeight, stepDepth), false, h1 + (i + 1) * stepHeight, true);
        }

        // Walkable ramp for Staircase 2
        this.addWalkableRamp(
            px + stair2StartX - stepWidth / 2 - 0.2,
            px + stair2StartX + stepWidth / 2 + 0.2,
            pz + stair2StartZ - stepRun - 0.4,
            pz + stair2StartZ + 0.2,
            h1, totalH, -1, 'z'
        );

        // ROOFTOP SLAB WITH OPEN STAIRWELL (Y = 7.6)
        const roofLeft = new THREE.Mesh(new THREE.BoxGeometry(w - 3.0, 0.35, d), mats.roof);
        roofLeft.position.set(-1.5, totalH, 0);
        roofLeft.receiveShadow = true;
        bGroup.add(roofLeft);

        this.walkableSurfaces.push({
            y: totalH,
            minX: px - halfW, maxX: px + halfW - 2.8,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Front roof section forward of the stair exit:
        const roofFrontRight = new THREE.Mesh(new THREE.BoxGeometry(3.0, 0.35, d - stepRun - 1.2), mats.roof);
        roofFrontRight.position.set(halfW - 1.5, totalH, -halfD + (d - stepRun - 1.2) / 2);
        bGroup.add(roofFrontRight);

        this.walkableSurfaces.push({
            y: totalH,
            minX: px + halfW - 3.0, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz - halfD + (d - stepRun - 1.2)
        });

        // 1.0m High Parapet Walls with Sniper Slits on Rooftop
        const parapetFront = new THREE.Mesh(new THREE.BoxGeometry(w, 1.0, 0.25), mats.brick);
        parapetFront.position.set(0, totalH + 0.5, halfD);
        bGroup.add(parapetFront);
        this.addBoxCollider(new THREE.Vector3(px, totalH + 0.5, pz + halfD), new THREE.Vector3(w, 1.0, 0.25), true, totalH + 1.0);

        const parapetBack = new THREE.Mesh(new THREE.BoxGeometry(w, 1.0, 0.25), mats.brick);
        parapetBack.position.set(0, totalH + 0.5, -halfD);
        bGroup.add(parapetBack);
        this.addBoxCollider(new THREE.Vector3(px, totalH + 0.5, pz - halfD), new THREE.Vector3(w, 1.0, 0.25), true, totalH + 1.0);

        const parapetLeft = new THREE.Mesh(new THREE.BoxGeometry(0.25, 1.0, d), mats.brick);
        parapetLeft.position.set(-halfW, totalH + 0.5, 0);
        bGroup.add(parapetLeft);
        this.addBoxCollider(new THREE.Vector3(px - halfW, totalH + 0.5, pz), new THREE.Vector3(0.25, 1.0, d), true, totalH + 1.0);

        const parapetRight = new THREE.Mesh(new THREE.BoxGeometry(0.25, 1.0, d), mats.brick);
        parapetRight.position.set(halfW, totalH + 0.5, 0);
        bGroup.add(parapetRight);
        this.addBoxCollider(new THREE.Vector3(px + halfW, totalH + 0.5, pz), new THREE.Vector3(0.25, 1.0, d), true, totalH + 1.0);

        // Tactical Rooftop Sniper Crate (High ground vantage)
        const crateGeom = new THREE.BoxGeometry(1.2, 0.8, 1.2);
        const crateMat = new THREE.MeshStandardMaterial({ color: 0x78350F, roughness: 0.8 });
        const roofCrate = new THREE.Mesh(crateGeom, crateMat);
        roofCrate.position.set(-3.5, totalH + 0.4, -2.5);
        bGroup.add(roofCrate);
        this.addBoxCollider(new THREE.Vector3(px - 3.5, totalH + 0.4, pz - 2.5), new THREE.Vector3(1.2, 0.8, 1.2), true, totalH + 0.8);

        this.scene.add(bGroup);
    }

    // ── 2. ENTERABLE 3-STORY TACTICAL TOWNHOUSE ──────────────────────────────
    buildEnterableTownhouse(px, pz, mats) {
        const bGroup = new THREE.Group();
        bGroup.position.set(px, 0, pz);

        const w = 10, d = 10, fH = 3.8, totalH = 11.4;
        const halfW = w / 2, halfD = d / 2;

        // Base Floor
        const baseFloor = new THREE.Mesh(new THREE.BoxGeometry(w, 0.2, d), mats.concrete);
        baseFloor.position.set(0, 0.1, 0);
        bGroup.add(baseFloor);

        this.walkableSurfaces.push({
            y: 0.1,
            minX: px - halfW, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Floor 1 Walls with open double doorway (Doorway: 3.2m wide, X: -1.6 to +1.6)
        const fwL = new THREE.Mesh(new THREE.BoxGeometry(3.4, fH, 0.4), mats.brick);
        fwL.position.set(-3.3, fH / 2, halfD);
        bGroup.add(fwL);
        this.addBoxCollider(new THREE.Vector3(px - 3.3, fH / 2, pz + halfD), new THREE.Vector3(3.4, fH, 0.4), true, fH);

        const fwR = new THREE.Mesh(new THREE.BoxGeometry(3.4, fH, 0.4), mats.brick);
        fwR.position.set(3.3, fH / 2, halfD);
        bGroup.add(fwR);
        this.addBoxCollider(new THREE.Vector3(px + 3.3, fH / 2, pz + halfD), new THREE.Vector3(3.4, fH, 0.4), true, fH);

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

        // STAIRS 1: GROUND TO FLOOR 2 (Left wall, front to back)
        const numSteps = 10;
        const stepW = 1.8;
        const stepRun = 4.8;
        const stepD = stepRun / numSteps;
        const stepH = fH / numSteps;
        const s1X = -halfW + 0.5 + stepW / 2;
        const s1Z = -halfD + 1.0;

        for (let i = 0; i < numSteps; i++) {
            const sy = (i + 0.5) * stepH;
            const sz = s1Z + (i + 0.5) * stepD;
            const step = new THREE.Mesh(new THREE.BoxGeometry(stepW, stepH, stepD + 0.04), mats.trim);
            step.position.set(s1X, sy, sz);
            bGroup.add(step);
            this.addBoxCollider(new THREE.Vector3(px + s1X, sy, pz + sz), new THREE.Vector3(stepW, stepH, stepD), false, (i + 1) * stepH, true);
        }

        this.addWalkableRamp(
            px + s1X - stepW / 2 - 0.2,
            px + s1X + stepW / 2 + 0.2,
            pz + s1Z - 0.2,
            pz + s1Z + stepRun + 0.4,
            0.1, fH, 1, 'z'
        );

        // FLOOR 2 SLAB (Y = 3.8)
        const f2Slab = new THREE.Mesh(new THREE.BoxGeometry(w - 2.5, 0.25, d), mats.concrete);
        f2Slab.position.set(1.25, fH, 0);
        bGroup.add(f2Slab);

        this.walkableSurfaces.push({
            y: fH,
            minX: px - halfW + 2.3, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Floor 2 back landing:
        const f2BackLanding = new THREE.Mesh(new THREE.BoxGeometry(2.5, 0.25, d - stepRun - 1.0), mats.concrete);
        f2BackLanding.position.set(-halfW + 1.25, fH, halfD - (d - stepRun - 1.0) / 2);
        bGroup.add(f2BackLanding);

        this.walkableSurfaces.push({
            y: fH,
            minX: px - halfW, maxX: px - halfW + 2.5,
            minZ: pz + halfD - (d - stepRun - 1.0), maxZ: pz + halfD
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

        // STAIRS 2: FLOOR 2 TO FLOOR 3 (Right wall, back to front)
        const s2X = halfW - 0.5 - stepW / 2;
        const s2Z = halfD - 1.0;

        for (let i = 0; i < numSteps; i++) {
            const sy = fH + (i + 0.5) * stepH;
            const sz = s2Z - (i + 0.5) * stepD;
            const step = new THREE.Mesh(new THREE.BoxGeometry(stepW, stepH, stepD + 0.04), mats.trim);
            step.position.set(s2X, sy, sz);
            bGroup.add(step);
            this.addBoxCollider(new THREE.Vector3(px + s2X, sy, pz + sz), new THREE.Vector3(stepW, stepH, stepD), false, fH + (i + 1) * stepH, true);
        }

        this.addWalkableRamp(
            px + s2X - stepW / 2 - 0.2,
            px + s2X + stepW / 2 + 0.2,
            pz + s2Z - stepRun - 0.4,
            pz + s2Z + 0.2,
            fH, fH * 2, -1, 'z'
        );

        // FLOOR 3 SLAB (Y = 7.6)
        const f3Slab = new THREE.Mesh(new THREE.BoxGeometry(w - 2.5, 0.25, d), mats.concrete);
        f3Slab.position.set(-1.25, fH * 2, 0);
        bGroup.add(f3Slab);

        this.walkableSurfaces.push({
            y: fH * 2,
            minX: px - halfW, maxX: px + halfW - 2.3,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        const f3FrontLanding = new THREE.Mesh(new THREE.BoxGeometry(2.5, 0.25, d - stepRun - 1.0), mats.concrete);
        f3FrontLanding.position.set(halfW - 1.25, fH * 2, -halfD + (d - stepRun - 1.0) / 2);
        bGroup.add(f3FrontLanding);

        this.walkableSurfaces.push({
            y: fH * 2,
            minX: px + halfW - 2.5, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz - halfD + (d - stepRun - 1.0)
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

        // STAIRS 3: FLOOR 3 TO ROOFTOP OBSERVATION DECK (Left wall, front to back)
        for (let i = 0; i < numSteps; i++) {
            const sy = fH * 2 + (i + 0.5) * stepH;
            const sz = s1Z + (i + 0.5) * stepD;
            const step = new THREE.Mesh(new THREE.BoxGeometry(stepW, stepH, stepD + 0.04), mats.trim);
            step.position.set(s1X, sy, sz);
            bGroup.add(step);
            this.addBoxCollider(new THREE.Vector3(px + s1X, sy, pz + sz), new THREE.Vector3(stepW, stepH, stepD), false, fH * 2 + (i + 1) * stepH, true);
        }

        this.addWalkableRamp(
            px + s1X - stepW / 2 - 0.2,
            px + s1X + stepW / 2 + 0.2,
            pz + s1Z - 0.2,
            pz + s1Z + stepRun + 0.4,
            fH * 2, totalH, 1, 'z'
        );

        // ROOFTOP SLAB (Y = 11.4) WITH STAIR OPENING
        const roofSlabRight = new THREE.Mesh(new THREE.BoxGeometry(w - 2.5, 0.35, d), mats.concrete);
        roofSlabRight.position.set(1.25, totalH, 0);
        bGroup.add(roofSlabRight);

        this.walkableSurfaces.push({
            y: totalH,
            minX: px - halfW + 2.3, maxX: px + halfW,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        const roofBackLanding = new THREE.Mesh(new THREE.BoxGeometry(2.5, 0.35, d - stepRun - 1.0), mats.concrete);
        roofBackLanding.position.set(-halfW + 1.25, totalH, halfD - (d - stepRun - 1.0) / 2);
        bGroup.add(roofBackLanding);

        this.walkableSurfaces.push({
            y: totalH,
            minX: px - halfW, maxX: px - halfW + 2.5,
            minZ: pz + halfD - (d - stepRun - 1.0), maxZ: pz + halfD
        });

        // Rooftop Sniper Parapet (1.0m high) on all 4 edges
        const pFront = new THREE.Mesh(new THREE.BoxGeometry(w, 1.0, 0.3), mats.brick);
        pFront.position.set(0, totalH + 0.5, halfD);
        bGroup.add(pFront);
        this.addBoxCollider(new THREE.Vector3(px, totalH + 0.5, pz + halfD), new THREE.Vector3(w, 1.0, 0.3), true, totalH + 1.0);

        const pBack = new THREE.Mesh(new THREE.BoxGeometry(w, 1.0, 0.3), mats.brick);
        pBack.position.set(0, totalH + 0.5, -halfD);
        bGroup.add(pBack);
        this.addBoxCollider(new THREE.Vector3(px, totalH + 0.5, pz - halfD), new THREE.Vector3(w, 1.0, 0.3), true, totalH + 1.0);

        const pLeft = new THREE.Mesh(new THREE.BoxGeometry(0.3, 1.0, d), mats.brick);
        pLeft.position.set(-halfW, totalH + 0.5, 0);
        bGroup.add(pLeft);
        this.addBoxCollider(new THREE.Vector3(px - halfW, totalH + 0.5, pz), new THREE.Vector3(0.3, 1.0, d), true, totalH + 1.0);

        const pRight = new THREE.Mesh(new THREE.BoxGeometry(0.3, 1.0, d), mats.brick);
        pRight.position.set(halfW, totalH + 0.5, 0);
        bGroup.add(pRight);
        this.addBoxCollider(new THREE.Vector3(px + halfW, totalH + 0.5, pz), new THREE.Vector3(0.3, 1.0, d), true, totalH + 1.0);

        // Tactical Sniper Deck Box on Roof
        const deckCrate = new THREE.Mesh(new THREE.BoxGeometry(1.4, 0.8, 1.4), mats.concrete);
        deckCrate.position.set(2.0, totalH + 0.4, 2.0);
        bGroup.add(deckCrate);
        this.addBoxCollider(new THREE.Vector3(px + 2.0, totalH + 0.4, pz + 2.0), new THREE.Vector3(1.4, 0.8, 1.4), true, totalH + 0.8);

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

        // Exterior Steel Stairs to Roof (Along right end wall)
        const bStepNum = 12;
        const bStepW = 1.6;
        const bStepRun = 5.0;
        const bStepD = bStepRun / bStepNum;
        const bStepH = h / bStepNum;
        const bStairX = halfW + 0.2 + bStepW / 2;
        const bStairZ = -halfD + 1.2;

        for (let i = 0; i < bStepNum; i++) {
            const sy = (i + 0.5) * bStepH;
            const sz = bStairZ + (i + 0.5) * bStepD;
            const step = new THREE.Mesh(new THREE.BoxGeometry(bStepW, bStepH, bStepD + 0.04), mats.trim);
            step.position.set(bStairX, sy, sz);
            bGroup.add(step);
            this.addBoxCollider(new THREE.Vector3(px + bStairX, sy, pz + sz), new THREE.Vector3(bStepW, bStepH, bStepD), false, (i + 1) * bStepH, true);
        }

        this.addWalkableRamp(
            px + bStairX - bStepW / 2 - 0.2,
            px + bStairX + bStepW / 2 + 0.2,
            pz + bStairZ - 0.2,
            pz + bStairZ + bStepRun + 0.4,
            0.1, h, 1, 'z'
        );

        // Walkable Flat Barracks Roof (Y = 4.2)
        const roofSlab = new THREE.Mesh(new THREE.BoxGeometry(w, 0.35, d), mats.concrete);
        roofSlab.position.set(0, h, 0);
        bGroup.add(roofSlab);

        this.walkableSurfaces.push({
            y: h,
            minX: px - halfW, maxX: px + halfW + bStepW + 0.4,
            minZ: pz - halfD, maxZ: pz + halfD
        });

        // Parapet around roof
        const bParapetF = new THREE.Mesh(new THREE.BoxGeometry(w, 0.9, 0.25), mats.brick);
        bParapetF.position.set(0, h + 0.45, halfD);
        bGroup.add(bParapetF);
        this.addBoxCollider(new THREE.Vector3(px, h + 0.45, pz + halfD), new THREE.Vector3(w, 0.9, 0.25), true, h + 0.9);

        const bParapetB = new THREE.Mesh(new THREE.BoxGeometry(w, 0.9, 0.25), mats.brick);
        bParapetB.position.set(0, h + 0.45, -halfD);
        bGroup.add(bParapetB);
        this.addBoxCollider(new THREE.Vector3(px, h + 0.45, pz - halfD), new THREE.Vector3(w, 0.9, 0.25), true, h + 0.9);

        this.scene.add(bGroup);
    }
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
        // 1. Massive Cooling Tower in South Industrial Power Plant
        if (window.blenderAssetLoader && window.blenderAssetLoader.models['cooling_tower']) {
            const blenderTower = window.blenderAssetLoader.createCoolingTowerInstance(-95, 275);
            if (blenderTower) {
                this.scene.add(blenderTower);
                const sz = blenderTower.userData.bboxSize || new THREE.Vector3(34, 46, 34);
                this.addBoxCollider(new THREE.Vector3(-95, sz.y / 2, 275), sz, true, sz.y);
                this.registerFootprint(-95, 275, sz.x / 2 + 4, sz.z / 2 + 4);
            }
        }

        // 2. Telecommunication / Radio Tower with blinking beacon (South-West Industrial Ridge)
        const radioGroup = new THREE.Group();
        radioGroup.position.set(-165, 0, 270);

        const mastGeom = new THREE.CylinderGeometry(0.8, 4.5, 56, 4);
        const mastMat = new THREE.MeshStandardMaterial({ color: 0xDC2626, metalness: 0.7, roughness: 0.3 });
        const mast = new THREE.Mesh(mastGeom, mastMat);
        mast.position.y = 28;
        mast.castShadow = true;
        radioGroup.add(mast);

        const beaconGeom = new THREE.SphereGeometry(0.8, 8, 8);
        const beaconMat = new THREE.MeshBasicMaterial({ color: 0xEF4444 });
        const beacon = new THREE.Mesh(beaconGeom, beaconMat);
        beacon.position.y = 56.5;
        radioGroup.add(beacon);

        this.scene.add(radioGroup);
        this.addBoxCollider(new THREE.Vector3(-165, 28, 270), new THREE.Vector3(6, 56, 6), true, 56);
        this.registerFootprint(-165, 270, 8, 8);
    }

    createMilitaryHangarsAndContainers() {
        // Multi-colored Shipping Containers Cluster (South Port & Airbase Apron)
        const containerColors = [0xDC2626, 0x2563EB, 0xEAB308, 0x16A34A, 0x475569];
        const containerSpawns = [
            // South Industrial Port Yard
            { x: -35, y: 1.3, z: 255, c: containerColors[0] },
            { x: -35, y: 1.3, z: 262, c: containerColors[1] },
            { x: -35, y: 3.9, z: 258, c: containerColors[2] }, // Stacked
            { x: -24, y: 1.3, z: 258, c: containerColors[3] },
            // East Airbase Cargo Apron
            { x: 168, y: 1.3, z: 160, c: containerColors[0] },
            { x: 168, y: 1.3, z: 167, c: containerColors[1] },
            { x: 168, y: 3.9, z: 163, c: containerColors[4] },
            { x: 168, y: 1.3, z: 280, c: containerColors[2] },
            { x: 168, y: 1.3, z: 287, c: containerColors[3] }
        ];

        containerSpawns.forEach(cs => {
            if (window.blenderAssetLoader && window.blenderAssetLoader.models['container']) {
                const bCont = window.blenderAssetLoader.createContainerInstance(cs.x, cs.y - 1.3, cs.z, cs.c);
                if (bCont) this.scene.add(bCont);
            }
            this.addBoxCollider(new THREE.Vector3(cs.x, cs.y, cs.z), new THREE.Vector3(6.5, 2.6, 2.8), true, cs.y + 1.3);
            this.registerFootprint(cs.x, cs.z, 4.5, 3.0);
        });
    }

    createAircraftWreckageAndVehicles() {
        // Gleaming McLaren P1 GTR Supercar parked on the East Airbase Runway!
        if (window.blenderAssetLoader && window.blenderAssetLoader.models['mclaren']) {
            const mclaren = window.blenderAssetLoader.createMcLarenInstance(255, 0.08, 125, Math.PI / 14);
            if (mclaren) {
                this.scene.add(mclaren);
                this.addBoxCollider(new THREE.Vector3(255, 0.7, 125), new THREE.Vector3(2.3, 1.4, 4.8), true, 1.4);
                this.registerFootprint(255, 125, 4.5, 5.5);
            }
        }
    }

    createWatchtowersAndBunkers() {
        const towerLocs = [
            { x: 0,    z: -340 }, // Northern Highway Checkpoint
            { x: -210, z: 245 },  // South-West Coast Overlook
            { x: 155,  z: 295 },  // South-East Airbase Overlook
            { x: -330, z: 0 }     // Western Highway Outpost
        ];

        towerLocs.forEach(loc => {
            if (window.blenderAssetLoader && window.blenderAssetLoader.models['watchtower']) {
                const bTower = window.blenderAssetLoader.createWatchtowerInstance(loc.x, loc.z);
                if (bTower) this.scene.add(bTower);
            }

            // 4 Corner Post colliders (leaves center open to walk through)
            const postDist = 1.8;
            this.addBoxCollider(new THREE.Vector3(loc.x - postDist, 6.0, loc.z - postDist), new THREE.Vector3(0.5, 12.0, 0.5), true, 12.0);
            this.addBoxCollider(new THREE.Vector3(loc.x + postDist, 6.0, loc.z - postDist), new THREE.Vector3(0.5, 12.0, 0.5), true, 12.0);
            this.addBoxCollider(new THREE.Vector3(loc.x - postDist, 6.0, loc.z + postDist), new THREE.Vector3(0.5, 12.0, 0.5), true, 12.0);
            this.addBoxCollider(new THREE.Vector3(loc.x + postDist, 6.0, loc.z + postDist), new THREE.Vector3(0.5, 12.0, 0.5), true, 12.0);

            // Access ramp / stairs to top platform:
            this.addWalkableRamp(loc.x - 1.0, loc.x + 1.0, loc.z - 6.5, loc.z - 1.2, 0.1, 12.0, 1, 'z');

            // Top observation platform at Y = 12.0
            this.walkableSurfaces.push({
                y: 12.0,
                minX: loc.x - 2.5, maxX: loc.x + 2.5,
                minZ: loc.z - 2.5, maxZ: loc.z + 2.5
            });
            this.registerFootprint(loc.x, loc.z, 5.0, 8.0);
        });
    }

    createFoliageAndProps() {
        if (!window.blenderAssetLoader) return;

        // Deterministic seeded PRNG so island forests look hand-crafted and consistent every match
        let seed = 42197;
        const nextRand = () => {
            seed = (seed * 1664525 + 1013904223) >>> 0;
            return seed / 4294967296;
        };

        // 1. Detailed Oak Trees across parklands, hillsides & forest belts (strict road & building clearance!)
        let oaksPlaced = 0;
        for (let attempt = 0; attempt < 260 && oaksPlaced < 72; attempt++) {
            const angle = nextRand() * Math.PI * 2;
            const dist = 55 + nextRand() * 405;
            const px = Math.cos(angle) * dist;
            const pz = Math.sin(angle) * dist;

            if (!this.isClearForProp(px, pz, 5.5)) continue;

            const scale = 0.85 + nextRand() * 0.4;
            const tree = window.blenderAssetLoader.createTreeInstance(px, pz, 'oak', scale);
            if (tree) {
                this.scene.add(tree);
                // Solid trunk collider so players & bullets interact cleanly with forest cover
                this.addBoxCollider(new THREE.Vector3(px, 2.5, pz), new THREE.Vector3(0.95, 5.0, 0.95), true, 5.0);
                this.registerFootprint(px, pz, 3.2, 3.2);
                oaksPlaced++;
            }
        }

        // 2. Coastal Tropical Palm Ring (46 palms lining golden sandy beaches, skipping Eastern Bridge)
        for (let i = 0; i < 46; i++) {
            const angle = (i / 46) * Math.PI * 2;
            const dist = 468 + (i % 4) * 7;
            const px = Math.cos(angle) * dist;
            const pz = Math.sin(angle) * dist;

            if (!this.isClearForProp(px, pz, 4.0)) continue;

            const palm = window.blenderAssetLoader.createTreeInstance(px, pz, 'palm', 1.0 + nextRand() * 0.35);
            if (palm) {
                this.scene.add(palm);
                this.addBoxCollider(new THREE.Vector3(px, 2.5, pz), new THREE.Vector3(0.8, 5.0, 0.8), true, 5.0);
            }
        }

        // 3. Modern Suburb & Boulevard Accent Palms
        const resPalms = [
            { x: 75,  z: -62 },  { x: 152, z: -62 },  { x: 178, z: -62 },
            { x: 152, z: -120 }, { x: 178, z: -120 }, { x: 152, z: -215 },
            { x: 178, z: -215 }, { x: 14,  z: -24 },  { x: -14, z: -24 },
            { x: 14,  z: 70 },   { x: -14, z: 70 },   { x: 14,  z: -135 },
            { x: -14, z: -135 }, { x: 14,  z: 175 },  { x: -14, z: 175 }
        ];
        resPalms.forEach(p => {
            if (!this.isClearForProp(p.x, p.z, 1.8)) return;
            const palm = window.blenderAssetLoader.createTreeInstance(p.x, p.z, 'palm', 1.12);
            if (palm) {
                this.scene.add(palm);
                this.addBoxCollider(new THREE.Vector3(p.x, 2.5, p.z), new THREE.Vector3(0.75, 5.0, 0.75), true, 5.0);
            }
        });

        // 4. Natural Rock Boulders Across Open Fields (tactical hard cover with exact colliders)
        let rocksPlaced = 0;
        for (let attempt = 0; attempt < 150 && rocksPlaced < 34; attempt++) {
            const angle = nextRand() * Math.PI * 2;
            const dist = 60 + nextRand() * 395;
            const px = Math.cos(angle) * dist;
            const pz = Math.sin(angle) * dist;

            if (!this.isClearForProp(px, pz, 4.5)) continue;

            const scale = 0.95 + nextRand() * 0.65;
            const rock = window.blenderAssetLoader.createRockInstance(px, pz, scale);
            if (rock) {
                this.scene.add(rock);
                const sz = rock.userData.bboxSize || new THREE.Vector3(2.6 * scale, 1.8 * scale, 2.6 * scale);
                this.addBoxCollider(new THREE.Vector3(px, sz.y / 2, pz), new THREE.Vector3(sz.x * 0.8, sz.y, sz.z * 0.8), true, sz.y);
                this.registerFootprint(px, pz, sz.x / 2 + 1.5, sz.z / 2 + 1.5);
                rocksPlaced++;
            }
        }

        // 5. Iconic Roadside Supply Airdrop Crate (FIXED: grounded cleanly at Y=0, pos (18, 0, -24))
        const dropCrate = window.blenderAssetLoader.createAirdropInstance(18, 0, -24);
        if (dropCrate) {
            this.scene.add(dropCrate);
            this.addBoxCollider(new THREE.Vector3(18, 0.75, -24), new THREE.Vector3(1.6, 1.5, 1.6), true, 1.5);
        }

        // Towering Golden Light Pillar shooting into the sky from the airdrop
        const beamGeom = new THREE.CylinderGeometry(0.45, 1.15, 80, 14);
        const beamMat = new THREE.MeshBasicMaterial({
            color: 0xFACC15,
            transparent: true,
            opacity: 0.45,
            side: THREE.DoubleSide,
            depthWrite: false
        });
        const lightBeam = new THREE.Mesh(beamGeom, beamMat);
        lightBeam.position.set(18, 40, -24);
        this.scene.add(lightBeam);
    }

    createHorizonMountains() {
        // Grand 360-degree majestic mountain range on the far horizon (720m to 960m ring)
        const mountGroup = new THREE.Group();

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
            color: 0x14532D,
            roughness: 0.85,
            metalness: 0.05,
            flatShading: true
        });

        const count = 32;
        for (let i = 0; i < count; i++) {
            const angle = (i / count) * Math.PI * 2;
            const dist = 740 + (i % 4) * 65;
            const mx = Math.cos(angle) * dist;
            const mz = Math.sin(angle) * dist;

            const height = 180 + (i % 5) * 36 + (i % 2) * 28;
            const baseRadius = 150 + (i % 3) * 40;

            const mGeom = new THREE.ConeGeometry(baseRadius, height, 12, 2, false);
            const posAttr = mGeom.attributes.position;
            for (let v = 0; v < posAttr.count; v++) {
                const py = posAttr.getY(v);
                if (py < height / 2 - 5) {
                    const px = posAttr.getX(v);
                    const pz = posAttr.getZ(v);
                    const noise = Math.sin(px * 0.04) * Math.cos(pz * 0.04) * 18;
                    posAttr.setX(v, px + noise);
                    posAttr.setZ(v, pz + noise);
                }
            }
            mGeom.computeVertexNormals();

            const mat = (i % 3 === 1) ? lushMat : rockMat;
            const peak = new THREE.Mesh(mGeom, mat);
            peak.position.set(mx, height / 2 - 14, mz);
            peak.rotation.y = (i * 1.37);
            mountGroup.add(peak);

            if (height >= 215) {
                const capHeight = height * 0.28;
                const capRadius = baseRadius * 0.32;
                const capGeom = new THREE.ConeGeometry(capRadius, capHeight, 10);
                const capMesh = new THREE.Mesh(capGeom, snowMat);
                capMesh.position.set(mx, height - 14 - capHeight / 2, mz);
                mountGroup.add(capMesh);
            }
        }
        this.scene.add(mountGroup);
    }

    createFloatingBlimp() {
        this.blimp = new THREE.Group();
        this.blimp.position.set(0, 95, -260);

        const blimpBodyMat = new THREE.MeshStandardMaterial({ color: 0xF59E0B, metalness: 0.4, roughness: 0.3 });
        const blimpGeom = new THREE.SphereGeometry(15, 20, 16);
        blimpGeom.scale(1.0, 0.7, 2.6);
        const blimpMesh = new THREE.Mesh(blimpGeom, blimpBodyMat);
        this.blimp.add(blimpMesh);

        const stripeMat = new THREE.MeshStandardMaterial({ color: 0x1E3A8A, roughness: 0.4 });
        const stripeGeom = new THREE.CylinderGeometry(15.2, 15.2, 4.2, 24, 1, true);
        stripeGeom.scale(1.0, 2.4, 0.65);
        stripeGeom.rotateZ(Math.PI / 2);
        const stripe = new THREE.Mesh(stripeGeom, stripeMat);
        this.blimp.add(stripe);

        const gondolaMat = new THREE.MeshStandardMaterial({ color: 0x0F172A, roughness: 0.5 });
        const gondolaGeom = new THREE.BoxGeometry(3.8, 2.6, 12.5);
        const gondola = new THREE.Mesh(gondolaGeom, gondolaMat);
        gondola.position.y = -11.0;
        this.blimp.add(gondola);

        this.scene.add(this.blimp);
    }

    update(dt) {
        if (this.blimp) {
            this.blimpAngle += dt * 0.04;
            const bx = Math.cos(this.blimpAngle) * 300;
            const bz = Math.sin(this.blimpAngle) * 300;
            this.blimp.position.x = bx;
            this.blimp.position.z = bz;
            this.blimp.rotation.y = -this.blimpAngle + Math.PI / 2;
        }

        if (this.water) {
            this.waterTime += dt * 1.5;
            this.water.position.y = -0.55 + Math.sin(this.waterTime) * 0.08;
        }
    }

    addBoxCollider(pos, size, isObstacle = true, topY = 0, isStairs = false) {
        const col = {
            min: new THREE.Vector3(pos.x - size.x / 2, pos.y - size.y / 2, pos.z - size.z / 2),
            max: new THREE.Vector3(pos.x + size.x / 2, pos.y + size.y / 2, pos.z + size.z / 2),
            isObstacle: isObstacle,
            topY: topY || (pos.y + size.y / 2),
            isStairs: isStairs
        };
        this.colliders.push(col);

        // Register into Spatial Hash Grid
        const minCellX = Math.floor(col.min.x / this.gridCellSize);
        const maxCellX = Math.floor(col.max.x / this.gridCellSize);
        const minCellZ = Math.floor(col.min.z / this.gridCellSize);
        const maxCellZ = Math.floor(col.max.z / this.gridCellSize);
        for (let cx = minCellX; cx <= maxCellX; cx++) {
            for (let cz = minCellZ; cz <= maxCellZ; cz++) {
                const key = `${cx},${cz}`;
                if (!this.spatialGrid.has(key)) this.spatialGrid.set(key, []);
                this.spatialGrid.get(key).push(col);
            }
        }
    }

    resolveCollision(currentPos, nextPos, radius = 0.45, height = 1.8) {
        let finalPos = nextPos.clone();
        let groundY = 0.0;
        let ceilingY = 999.0;

        // 1. Walkable Flat Surfaces (Ground, Floor 1, Floor 2, Floor 3, Rooftops, Bridges, Watchtowers)
        for (let i = 0; i < this.walkableSurfaces.length; i++) {
            const surf = this.walkableSurfaces[i];
            if (finalPos.x >= surf.minX && finalPos.x <= surf.maxX &&
                finalPos.z >= surf.minZ && finalPos.z <= surf.maxZ) {
                if (currentPos.y >= surf.y - 0.75 && currentPos.y <= surf.y + 4.2) {
                    groundY = Math.max(groundY, surf.y);
                }
            }
        }

        // 2. Walkable Stair Ramps (Continuous smooth incline for effortless stair climbing)
        for (let i = 0; i < this.walkableRamps.length; i++) {
            const ramp = this.walkableRamps[i];
            if (finalPos.x >= ramp.minX && finalPos.x <= ramp.maxX &&
                finalPos.z >= ramp.minZ && finalPos.z <= ramp.maxZ) {
                let t = 0;
                if (ramp.axis === 'x') {
                    const span = ramp.maxX - ramp.minX;
                    t = span > 0.001 ? (finalPos.x - ramp.minX) / span : 0;
                } else {
                    const span = ramp.maxZ - ramp.minZ;
                    t = span > 0.001 ? (finalPos.z - ramp.minZ) / span : 0;
                }
                t = Math.max(0, Math.min(1, t));
                const rampY = (ramp.dir > 0) ? (ramp.yStart + t * (ramp.yEnd - ramp.yStart)) : (ramp.yEnd - t * (ramp.yEnd - ramp.yStart));
                if (currentPos.y >= rampY - 0.85 && currentPos.y <= rampY + 3.0) {
                    groundY = Math.max(groundY, rampY);
                }
            }
        }

        // 3. Fast Spatial Grid Colliders (Only checking nearby cells for 60fps buttery smoothness!)
        const collidersToCheck = this.getNearbyColliders(finalPos.x, finalPos.z, radius + 0.8);
        for (let i = 0; i < collidersToCheck.length; i++) {
            const col = collidersToCheck[i];

            // Ceiling collision directly overhead
            if (finalPos.x >= col.min.x && finalPos.x <= col.max.x &&
                finalPos.z >= col.min.z && finalPos.z <= col.max.z) {
                if (col.min.y > currentPos.y + 0.6 && col.min.y <= currentPos.y + height + 0.3) {
                    ceilingY = Math.min(ceilingY, col.min.y);
                }
            }

            // If stairs, ramp elevation handled groundY; do not block horizontally!
            if (col.isStairs) continue;

            // Step-up / curb auto climbing (if obstacle top is within 0.45m stepOffset)
            if (col.topY && col.topY <= currentPos.y + 0.45 && currentPos.y >= col.topY - 0.55) {
                if (finalPos.x >= col.min.x && finalPos.x <= col.max.x &&
                    finalPos.z >= col.min.z && finalPos.z <= col.max.z) {
                    groundY = Math.max(groundY, col.topY);
                }
                continue;
            }

            // Headroom clearance (doorway lintels, arches, high roofs)
            if (col.min.y >= currentPos.y + height - 0.15) {
                continue;
            }

            // Below feet clearance
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
        for (let i = 0; i < 48; i++) {
            const angle = (i / 48) * Math.PI * 2;
            const r = 40 + Math.random() * 380;
            const sx = Math.cos(angle) * r;
            const sz = Math.sin(angle) * r;
            if (this.isClearForProp(sx, sz, 2.0)) {
                this.spawnPoints.push(new THREE.Vector3(sx, 0, sz));
            }
        }
    }
}

window.WarLandMap = WarLandMap;
