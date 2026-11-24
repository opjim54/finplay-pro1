// War Land 3D - Master Blender Asset Loader & Instancer (v6 Optimized & Grounded)
// Deduplicated GLB loading, shared PBR materials for 60fps, exact Box3 grounding & weapon scaling
// Preserves all user-supplied 3D characters, buildings, weapons, trees, palms & McLaren supercar

class BlenderAssetLoader {
    constructor() {
        this.models = {};
        this.animations = {};
        this.isLoaded = false;
        this.loader = (typeof THREE !== 'undefined' && typeof THREE.GLTFLoader !== 'undefined') ? new THREE.GLTFLoader() : null;
    }

    loadAllModels(onComplete) {
        if (!this.loader) {
            console.warn("[BlenderLoader] THREE.GLTFLoader not available, using procedural fallback");
            this.isLoaded = true;
            if (onComplete) onComplete();
            return;
        }

        // Canonical logical key -> asset path (deduplicated by path below so each GLB loads ONCE!)
        const modelFiles = {
            // Existing User 3D Characters (PRESERVED - never replaced!)
            char_freya:       'assets/models/character_freya_tactical.glb',
            char_female:      'assets/models/character_freya_tactical.glb',
            char_asian:       'assets/models/character_asian_agent.glb',
            char_sophia:      'assets/models/character_agent_sophia.glb',
            char_commando:    'assets/models/character_freya_tactical.glb',
            char_mercenary:   'assets/models/character_freya_tactical.glb',
            char_medic:       'assets/models/character_asian_agent.glb',
            char_cyborg:      'assets/models/character_freya_tactical.glb',
            char_eva:         'assets/models/character_asian_agent.glb',
            character:        'assets/models/character_freya_tactical.glb',

            // Combat & Movement Animation Pack (22 Mixamo Clips)
            anims_combat:     'assets/animations/anims_combat_pack.glb',

            // Supplied Architectural Buildings
            building_villa:      'assets/models/building_villa_terracotta.glb',
            building_townhouse:  'assets/models/building_tactical_townhouse.glb',
            building_barracks:   'assets/models/building_military_barracks.glb',
            building_hangar:     'assets/models/building_hangar_depot.glb',
            building_city:       'assets/models/building_city_blocks.glb',
            building_modern:     'assets/models/building_modern_houses.glb',
            building_multistory: 'assets/models/building_multistory.glb',

            // Supplied Props, Foliage & Vehicles
            gloowall:           'assets/models/prop_gloowall.glb',
            tree_oak:           'assets/models/prop_tree_stylized.glb',
            tree_oak_detailed:  'assets/models/prop_tree_oak_detailed.glb',
            tree_palm:          'assets/models/prop_palm_tree.glb',
            rock:               'assets/models/prop_rock_boulder.glb',
            cooling_tower:      'assets/models/prop_cooling_tower.glb',
            watchtower:         'assets/models/prop_watchtower.glb',
            airdrop_crate:      'assets/models/prop_airdrop_crate.glb',
            container:          'assets/models/prop_shipping_container.glb',
            mclaren:            'assets/models/mclaren_p1_gtr.glb',

            // Supplied Weapons & Parachute
            gun_ak47:    'assets/models/gun_ak47.glb',
            gun_awm:     'assets/models/gun_awm.glb',
            gun_shotgun: 'assets/models/gun_shotgun.glb',
            gun_deagle:  'assets/models/gun_deagle.glb',
            parachute:   'assets/models/prop_parachute.glb'
        };

        // Group aliases by unique GLB URL so every file is fetched & parsed exactly ONCE
        const pathToKeys = new Map();
        for (const [key, path] of Object.entries(modelFiles)) {
            if (!pathToKeys.has(path)) pathToKeys.set(path, []);
            pathToKeys.get(path).push(key);
        }

        const uniqueEntries = Array.from(pathToKeys.entries());
        let loadedCount = 0;

        const checkCompletion = () => {
            loadedCount++;
            if (typeof window.onWarLandLoadProgress === 'function') {
                window.onWarLandLoadProgress(loadedCount, uniqueEntries.length);
            }
            if (loadedCount >= uniqueEntries.length && !this.isLoaded) {
                this.isLoaded = true;
                console.log(`[BlenderLoader] Ready: ${uniqueEntries.length} unique GLBs (${Object.keys(this.models).length} aliases).`);
                if (onComplete) onComplete();
            }
        };

        // Safety timeout so gameplay never stalls on slow devices
        setTimeout(() => {
            if (!this.isLoaded) {
                console.warn("[BlenderLoader] Starting game while remaining assets finish streaming...");
                this.isLoaded = true;
                if (onComplete) onComplete();
            }
        }, 16000);

        uniqueEntries.forEach(([path, aliases]) => {
            const primaryKey = aliases[0];
            const isFoliageOrRock = primaryKey.includes('tree') || primaryKey.includes('rock');
            const isBuilding = primaryKey.includes('building') || primaryKey.includes('cooling_tower');
            const isModern = primaryKey === 'building_modern';

            this.loader.load(
                path,
                (gltf) => {
                    gltf.scene.traverse((child) => {
                        if (child.isMesh) {
                            // Keep shadows crisp on characters, buildings, vehicles; skip expensive foliage shadow passes
                            child.castShadow = !isFoliageOrRock && !isBuilding;
                            child.receiveShadow = true;
                            child.frustumCulled = true;

                            if (child.material) {
                                const mats = Array.isArray(child.material) ? child.material : [child.material];
                                mats.forEach(m => {
                                    const mName = (m.name || '').toLowerCase();
                                    if (
                                        mName.includes('hair') || mName.includes('bushy') ||
                                        mName.includes('stubble') || mName.includes('eyelash') ||
                                        mName.includes('leaf') || mName.includes('foliage') ||
                                        mName.includes('palm') || mName.includes('banana')
                                    ) {
                                        m.transparent = false;
                                        m.alphaTest = 0.35;
                                        m.depthWrite = true;
                                        m.side = THREE.DoubleSide;
                                    } else if (isModern) {
                                        m.side = THREE.DoubleSide;
                                    }
                                });
                            }
                        }
                    });

                    aliases.forEach((key) => {
                        this.models[key] = gltf.scene;
                        if (gltf.animations && gltf.animations.length > 0) {
                            this.animations[key] = gltf.animations;
                        }
                    });

                    // Register combat pack animations immediately when loaded
                    if (
                        gltf.animations && gltf.animations.length > 0 &&
                        (aliases.includes('anims_combat') || path.includes('anim'))
                    ) {
                        if (window.animationSystem) {
                            window.animationSystem.loadAnimationsFromGLB(gltf.animations);
                            console.log(`[BlenderLoader] Loaded ${gltf.animations.length} combat/movement clips into AnimationSystem`);
                        }
                    }

                    checkCompletion();
                },
                undefined,
                (err) => {
                    console.warn(`[BlenderLoader] Skip/fallback for ${path}:`, err);
                    checkCompletion();
                }
            );
        });
    }

    // Properly clones meshes + SkinnedMesh skeletons; shares static materials by default for high FPS
    _safeClone(src, cloneMaterials = false) {
        if (!src) return null;

        const sourceLookup = new Map();
        const cloneLookup = new Map();
        const clone = src.clone();

        function parallelTraverse(a, b) {
            sourceLookup.set(b, a);
            cloneLookup.set(a, b);
            for (let i = 0; i < a.children.length; i++) {
                parallelTraverse(a.children[i], b.children[i]);
            }
        }
        parallelTraverse(src, clone);

        let hasSkinnedMesh = false;
        clone.traverse(node => {
            if (node.isSkinnedMesh) {
                hasSkinnedMesh = true;
                node.frustumCulled = false; // Prevent skinned animation bounds popping
                const sourceMesh = sourceLookup.get(node);
                if (sourceMesh && sourceMesh.skeleton) {
                    const sourceBones = sourceMesh.skeleton.bones;
                    const clonedBones = new Array(sourceBones.length);
                    for (let i = 0; i < sourceBones.length; i++) {
                        clonedBones[i] = cloneLookup.get(sourceBones[i]) || sourceBones[i];
                    }
                    node.bind(new THREE.Skeleton(clonedBones, sourceMesh.skeleton.boneInverses), sourceMesh.bindMatrix);
                }
            }
            if (cloneMaterials && node.isMesh && node.material) {
                if (Array.isArray(node.material)) {
                    node.material = node.material.map(m => m.clone());
                } else {
                    node.material = node.material.clone();
                }
            }
        });

        clone.userData.hasSkinnedMesh = hasSkinnedMesh;
        return clone;
    }

    // Grounds a cloned mesh inside a pivot Group so its base sits cleanly at Y = 0 and X/Z are centered
    _wrapAndGround(rawClone, targetHeight = null, maxExtent = null, centerXZ = true) {
        const wrapper = new THREE.Group();
        rawClone.position.set(0, 0, 0);
        wrapper.add(rawClone);
        wrapper.updateMatrixWorld(true);

        let box = new THREE.Box3().setFromObject(rawClone);
        let size = new THREE.Vector3();
        box.getSize(size);

        if (targetHeight && size.y > 0.0001) {
            const s = targetHeight / size.y;
            rawClone.scale.multiplyScalar(s);
            wrapper.updateMatrixWorld(true);
            box = new THREE.Box3().setFromObject(rawClone);
            box.getSize(size);
        } else if (maxExtent) {
            const maxDim = Math.max(size.x, size.y, size.z);
            if (maxDim > 0.0001) {
                const s = maxExtent / maxDim;
                rawClone.scale.multiplyScalar(s);
                wrapper.updateMatrixWorld(true);
                box = new THREE.Box3().setFromObject(rawClone);
                box.getSize(size);
            }
        }

        const center = new THREE.Vector3();
        box.getCenter(center);

        if (centerXZ) {
            rawClone.position.x -= center.x;
            rawClone.position.z -= center.z;
        }
        rawClone.position.y -= box.min.y;

        wrapper.updateMatrixWorld(true);
        const finalBox = new THREE.Box3().setFromObject(wrapper);
        const finalSize = new THREE.Vector3();
        finalBox.getSize(finalSize);
        wrapper.userData.bboxSize = finalSize;
        return wrapper;
    }

    // ── EXISTING CHARACTER INSTANCE (PRESERVES ORIGINAL MODELS & SKINS) ──────
    createCharacterInstance(charType) {
        const keyMap = {
            'freya':        'char_freya',
            'female':       'char_freya',
            'asian':        'char_asian',
            'sophia':       'char_sophia',
            'commando':     'char_freya',
            'mercenary':    'char_freya',
            'medic':        'char_asian',
            'cyborg':       'char_freya',
            'eva':          'char_asian',
            'zane':         'char_freya',
            'default':      'char_freya'
        };
        const key = keyMap[charType] || 'char_freya';
        const src = this.models[key] || this.models['char_freya'] || this.models['char_asian'];
        if (!src) return null;

        const clone = this._safeClone(src, false);

        // Normalize character height accurately to realistic ~1.72m without altering proportions
        clone.updateMatrixWorld(true);
        const b = new THREE.Box3().setFromObject(clone);
        const h = b.max.y - b.min.y;
        if (h > 0.001 && (h < 1.35 || h > 2.05)) {
            const s = 1.72 / h;
            clone.scale.multiplyScalar(s);
        }

        clone.updateMatrixWorld(true);
        const postBox = new THREE.Box3().setFromObject(clone);
        if (Math.abs(postBox.min.y) > 0.005) {
            clone.position.y -= postBox.min.y;
        }

        const combatClips = this.animations['anims_combat'] || [];
        const charClips = this.animations[key] || [];
        const allClips = [...charClips, ...combatClips];
        clone.userData.animations = allClips;
        clone.userData.characterKey = key;

        if (window.animationSystem && clone.userData.hasSkinnedMesh) {
            if (combatClips.length > 0 && !window.animationSystem.isReady) {
                window.animationSystem.loadAnimationsFromGLB(combatClips);
            }
            window.animationSystem.attachToCharacter(clone);
        }

        return clone;
    }

    // ── SUPPLIED BUILDING INSTANCES (GROUNDED WITH EXACT BBOX METADATA) ──────
    createBuildingInstance(type, posX, posZ, rotY) {
        const keyMap = {
            'villa':      'building_villa',
            'townhouse':  'building_townhouse',
            'barracks':   'building_barracks',
            'hangar':     'building_hangar',
            'multistory': 'building_multistory'
        };
        const key = keyMap[type] || 'building_barracks';
        const src = this.models[key];
        if (!src) return null;

        const raw = this._safeClone(src, false);
        const g = this._wrapAndGround(raw, null, null, true);
        g.position.set(posX || 0, 0, posZ || 0);
        if (rotY) g.rotation.y = rotY;
        return g;
    }

    createCityBuildingInstance(binaIndex, posX, posZ, rotY, scale = 1.0) {
        const src = this.models['building_city'];
        if (!src) return null;
        const binaNames = [
            'bina', 'bina.001', 'bina.002', 'bina.003', 'bina.004', 'bina.005',
            'bina.006', 'bina.007', 'bina.008', 'bina.009', 'bina.010', 'bina.012',
            'bina.013', 'bina.014', 'bina.015', 'bina.036'
        ];
        const targetName = (typeof binaIndex === 'string') ? binaIndex : binaNames[binaIndex % binaNames.length];
        const foundMesh = src.getObjectByName(targetName);
        if (!foundMesh) return null;

        const clone = this._safeClone(foundMesh, false);
        clone.rotation.set(0, 0, 0);
        const group = this._wrapAndGround(clone, null, null, true);

        // Clamp absurd outlier scales to realistic city skyscraper dimensions (14m..56m tall)
        const sz = group.userData.bboxSize;
        if (sz) {
            const maxHoriz = Math.max(sz.x, sz.z);
            if (maxHoriz > 55) {
                const fix = 34 / maxHoriz;
                group.scale.multiplyScalar(fix);
            } else if (maxHoriz < 6 && sz.y < 8) {
                const fix = 18 / Math.max(maxHoriz, sz.y, 1);
                group.scale.multiplyScalar(fix);
            }
        }

        if (scale !== 1.0) group.scale.multiplyScalar(scale);
        group.position.set(posX || 0, 0, posZ || 0);
        if (rotY) group.rotation.y = rotY;

        group.updateMatrixWorld(true);
        const wb = new THREE.Box3().setFromObject(group);
        group.position.y -= wb.min.y;
        const ws = new THREE.Vector3();
        wb.getSize(ws);
        group.userData.worldBoxSize = ws;
        return group;
    }

    createModernHouseInstance(houseIndex, posX, posZ, rotY, scale = 1.0) {
        const src = this.models['building_modern'];
        if (!src) return null;
        // Skip negative-scaled house12/house13 to avoid inverted-normal artifacts
        const safeHouses = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14, 15, 16, 17, 18, 19, 20];
        const houseNum = safeHouses[Math.abs(houseIndex || 0) % safeHouses.length];
        const houseName = `house${houseNum}`;
        const foundHouse = src.getObjectByName(houseName);
        if (!foundHouse) return null;

        const clone = this._safeClone(foundHouse, false);
        clone.rotation.set(0, 0, 0);
        // Ensure positive scale
        clone.scale.set(Math.abs(clone.scale.x), Math.abs(clone.scale.y), Math.abs(clone.scale.z));

        const group = this._wrapAndGround(clone, null, null, true);
        if (scale !== 1.0) group.scale.multiplyScalar(scale);
        group.position.set(posX || 0, 0, posZ || 0);
        if (rotY) group.rotation.y = rotY;

        group.updateMatrixWorld(true);
        const wb = new THREE.Box3().setFromObject(group);
        group.position.y -= wb.min.y;
        const ws = new THREE.Vector3();
        wb.getSize(ws);
        group.userData.worldBoxSize = ws;
        return group;
    }

    createCoolingTowerInstance(posX, posZ) {
        const src = this.models['cooling_tower'];
        if (!src) return null;
        const g = this._wrapAndGround(this._safeClone(src, false), 46, null, true);
        g.position.set(posX || 0, 0, posZ || 0);
        return g;
    }

    createWatchtowerInstance(posX, posZ) {
        const src = this.models['watchtower'];
        if (!src) return null;
        const g = this._wrapAndGround(this._safeClone(src, false), 14.5, null, true);
        g.position.set(posX || 0, 0, posZ || 0);
        return g;
    }

    createContainerInstance(posX, posY, posZ, colorHex) {
        const src = this.models['container'];
        if (!src) return null;
        const raw = this._safeClone(src, !!colorHex);
        const g = this._wrapAndGround(raw, 2.6, null, true);
        g.position.set(posX || 0, posY || 0, posZ || 0);
        if (colorHex) {
            g.traverse(c => {
                if (c.isMesh && c.material && c.material.color) {
                    c.material.color.setHex(colorHex);
                }
            });
        }
        return g;
    }

    createMcLarenInstance(posX, posY, posZ, rotY) {
        const src = this.models['mclaren'];
        if (!src) return null;
        const raw = this._safeClone(src, false);
        // Normalize McLaren P1 GTR to realistic supercar length (~4.7m) and ground cleanly on wheels
        const g = this._wrapAndGround(raw, null, 4.7, true);
        g.position.set(posX || 0, posY || 0, posZ || 0);
        if (rotY) g.rotation.y = rotY;
        return g;
    }

    createTreeInstance(posX, posZ, type, scale) {
        let x = 0, z = 0, treeType = 'oak', s = 1.0;
        if (typeof posX === 'string') {
            treeType = posX;
        } else {
            x = posX || 0;
            z = posZ || 0;
            treeType = type || 'oak';
            s = scale || 1.0;
        }

        let key = 'tree_oak_detailed';
        let targetH = 9.5;
        if (treeType === 'palm') {
            key = 'tree_palm';
            targetH = 7.8;
        } else if (treeType === 'oak_detailed' || treeType === 'oak') {
            key = this.models['tree_oak_detailed'] ? 'tree_oak_detailed' : 'tree_oak';
            targetH = 9.5;
        } else {
            key = 'tree_oak';
            targetH = 8.0;
        }
        const src = this.models[key] || this.models['tree_oak'] || this.models['tree_palm'];
        if (!src) return null;

        const raw = this._safeClone(src, false);
        // Ground accurately! (Fixes prop_tree_oak_detailed's -35m Y offset!)
        const g = this._wrapAndGround(raw, targetH * s, null, true);
        g.position.set(x, 0, z);
        g.rotation.y = Math.random() * Math.PI * 2;
        return g;
    }

    createRockInstance(posX, posZ, scale) {
        let x = 0, z = 0, s = 1.0;
        if (typeof posX === 'number' && typeof posZ === 'number') {
            x = posX;
            z = posZ;
            s = scale || 1.0;
        }
        const src = this.models['rock'];
        if (!src) return null;

        const raw = this._safeClone(src, false);
        const g = this._wrapAndGround(raw, 2.0 * s, null, true);
        g.position.set(x, -0.15, z); // Embed slightly into soil so boulders look natural
        g.rotation.y = Math.random() * Math.PI * 2;
        return g;
    }

    createGlooWallInstance(posX, posY, posZ, rotY) {
        const src = this.models['gloowall'];
        if (!src) return null;
        const raw = this._safeClone(src, true); // Needs cloned material for hit flash
        const g = this._wrapAndGround(raw, 2.7, null, true);
        if (posX !== undefined) g.position.set(posX, posY || 0, posZ || 0);
        if (rotY !== undefined) g.rotation.y = rotY;
        return g;
    }

    createAirdropInstance(posX, posY, posZ) {
        const src = this.models['airdrop_crate'];
        if (!src) return null;
        const raw = this._safeClone(src, false);
        const g = this._wrapAndGround(raw, 1.5, null, true);
        if (posX !== undefined) g.position.set(posX, posY || 0, posZ || 0);
        return g;
    }

    createWeaponInstance(weaponKey) {
        const keyMap = {
            'GUN_AK47':   'gun_ak47',
            'GUN_AWM':    'gun_awm',
            'GUN_SPAS12': 'gun_shotgun',
            'GUN_DEAGLE': 'gun_deagle',
            'AK47':       'gun_ak47',
            'AWM':        'gun_awm',
            'SPAS12':     'gun_shotgun',
            'DEAGLE':     'gun_deagle'
        };
        const key = keyMap[weaponKey] || 'gun_ak47';
        const src = this.models[key];
        if (!src) return null;

        const raw = this._safeClone(src, false);
        const targetLengths = {
            'gun_ak47':    0.88,
            'gun_awm':     1.14,
            'gun_shotgun': 0.94,
            'gun_deagle':  0.30
        };
        const targetLen = targetLengths[key] || 0.88;

        // Normalize weapon scale & center pivot cleanly (fixes giant 66-unit AWM GLB!)
        const group = new THREE.Group();
        raw.position.set(0, 0, 0);
        group.add(raw);
        group.updateMatrixWorld(true);

        const b = new THREE.Box3().setFromObject(raw);
        const sz = new THREE.Vector3();
        b.getSize(sz);
        const maxDim = Math.max(sz.x, sz.y, sz.z);
        if (maxDim > 0.0001) {
            const s = targetLen / maxDim;
            raw.scale.multiplyScalar(s);
        }

        // Rotate X-aligned gun models (AK47, AWM) so barrel points along Z axis consistently
        group.updateMatrixWorld(true);
        const b2 = new THREE.Box3().setFromObject(raw);
        const sz2 = new THREE.Vector3();
        b2.getSize(sz2);
        if (sz2.x > sz2.z * 1.4) {
            raw.rotation.y += Math.PI / 2;
        }

        group.updateMatrixWorld(true);
        const b3 = new THREE.Box3().setFromObject(raw);
        const c3 = new THREE.Vector3();
        b3.getCenter(c3);
        raw.position.sub(c3);

        return group;
    }

    createParachuteInstance() {
        const src = this.models['parachute'];
        if (!src) return null;
        return this._wrapAndGround(this._safeClone(src, false), 2.8, null, true);
    }
}

window.blenderAssetLoader = new BlenderAssetLoader();
