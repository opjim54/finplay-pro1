// War Land 3D - Master Blender Asset Loader & Instancer (v5 Realistic & High-Fidelity)
// Loads realistic 3D character models from user's 3dmodels folder (Zane Ryder, Realistic Female Agent, Freya)
// and high-detail PBR architectural buildings, weapons, and props

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

        const modelFiles = {
            // Realistic Humanoid 3D Characters (Tactical Female Operatives)
            char_freya:       'assets/models/character_freya_tactical.glb',     // Freya Tactical Operative (Mixamo Rigged)
            char_female:      'assets/models/character_freya_tactical.glb',
            char_asian:       'assets/models/character_asian_agent.glb',
            char_sophia:      'assets/models/character_agent_sophia.glb',
            char_commando:    'assets/models/character_freya_tactical.glb',
            char_mercenary:   'assets/models/character_freya_tactical.glb',
            char_medic:       'assets/models/character_asian_agent.glb',
            char_cyborg:      'assets/models/character_freya_tactical.glb',
            char_eva:         'assets/models/character_asian_agent.glb',
            character:        'assets/models/character_freya_tactical.glb',     // Default female operative

            // ── COMBAT ANIMATION PACK ──────────────────────────────────────────
            // Contains all movement/combat animations from the user's /animation folder
            anims_combat:     'assets/animations/anims_combat_pack.glb',

            // Specialized 3D Architectural Buildings (PBR Stucco, Terracotta, Concrete, Steel)
            building_villa:      'assets/models/building_villa_terracotta.glb',
            building_townhouse:  'assets/models/building_tactical_townhouse.glb',
            building_barracks:   'assets/models/building_military_barracks.glb',
            building_hangar:     'assets/models/building_hangar_depot.glb',
            building_city:       'assets/models/building_city_blocks.glb',
            building_modern:     'assets/models/building_modern_houses.glb',
            building_multistory: 'assets/models/building_multistory.glb',

            // Free Fire Gloo Wall & Environment Props
            gloowall:           'assets/models/prop_gloowall.glb',
            tree_oak:           'assets/models/prop_tree_stylized.glb',
            tree_oak_detailed:  'assets/models/prop_tree_oak_detailed.glb',
            tree_palm:          'assets/models/prop_palm_tree.glb',
            rock:               'assets/models/prop_rock_boulder.glb',
            cooling_tower:   'assets/models/prop_cooling_tower.glb',
            watchtower:      'assets/models/prop_watchtower.glb',
            airdrop_crate:   'assets/models/prop_airdrop_crate.glb',
            container:       'assets/models/prop_shipping_container.glb',

            // Weapons
            gun_ak47:    'assets/models/gun_ak47.glb',
            gun_awm:     'assets/models/gun_awm.glb',
            gun_shotgun: 'assets/models/gun_shotgun.glb',
            gun_deagle:  'assets/models/gun_deagle.glb',
            parachute:   'assets/models/prop_parachute.glb'
        };

        const keys = Object.keys(modelFiles);
        let loadedCount = 0;

        const checkCompletion = () => {
            loadedCount++;
            if (loadedCount >= keys.length) {
                this.isLoaded = true;
                console.log(`[BlenderLoader] All 3D assets ready (${Object.keys(this.models).length} loaded).`);
                if (onComplete) onComplete();
            }
        };

        // Safety fallback: ensure models have ample time to parse over localhost/webview
        setTimeout(() => {
            if (!this.isLoaded) {
                console.warn("[BlenderLoader] Starting game while background assets stream in...");
                this.isLoaded = true;
                if (onComplete) onComplete();
            }
        }, 15000);

        keys.forEach(key => {
            const path = modelFiles[key];
            this.loader.load(
                path,
                (gltf) => {
                    const isFoliageOrRock = key.includes('tree') || key.includes('rock');
                    gltf.scene.traverse((child) => {
                        if (child.isMesh) {
                            child.castShadow = !isFoliageOrRock;
                            child.receiveShadow = true;
                            child.frustumCulled = true;
                            if (child.material) {
                                const mats = Array.isArray(child.material) ? child.material : [child.material];
                                mats.forEach(m => {
                                    if (m.name && (m.name.includes('Hair') || m.name.includes('Bushy') || m.name.includes('Stubble') || m.name.includes('Eyelash'))) {
                                        m.transparent = true;
                                        m.alphaTest = 0.35;
                                        m.depthWrite = true;
                                    }
                                });
                            }
                        }
                    });
                    this.models[key] = gltf.scene;
                    if (gltf.animations && gltf.animations.length > 0) {
                        this.animations[key] = gltf.animations;

                        // ── Load animations into animation system when combat pack loads ──
                        if (key === 'anims_combat' || key.includes('anim') || gltf.animations.length > 5) {
                            if (window.animationSystem) {
                                window.animationSystem.loadAnimationsFromGLB(gltf.animations);
                                console.log(`[BlenderLoader] Loaded ${gltf.animations.length} animation clips into AnimationSystem from ${key}`);
                            }
                        }
                    }
                    checkCompletion();
                },
                undefined,
                (err) => {
                    console.warn(`[BlenderLoader] Model skip/fallback for ${path}:`, err);
                    checkCompletion();
                }
            );
        });
    }

    _safeClone(src) {
        if (!src) return null;
        
        // Proper Three.js SkinnedMesh cloning with skeleton re-binding
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

        clone.traverse(node => {
            if (node.isSkinnedMesh) {
                const sourceMesh = sourceLookup.get(node);
                if (sourceMesh && sourceMesh.skeleton) {
                    const sourceBones = sourceMesh.skeleton.bones;
                    const clonedBones = [];
                    for (let i = 0; i < sourceBones.length; i++) {
                        clonedBones.push(cloneLookup.get(sourceBones[i]) || sourceBones[i]);
                    }
                    node.bind(new THREE.Skeleton(clonedBones, sourceMesh.skeleton.boneInverses), sourceMesh.bindMatrix);
                }
            }
            if (node.isMesh && node.material) {
                if (Array.isArray(node.material)) {
                    node.material = node.material.map(m => m.clone());
                } else {
                    node.material = node.material.clone();
                }
            }
        });

        return clone;
    }

    // ── REALISTIC FEMALE CHARACTER INSTANCE ──────────────────────────────────
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

        const clone = this._safeClone(src);

        // Normalize character bounds so it is exactly in human scale (~1.72m)
        const b = new THREE.Box3().setFromObject(clone);
        const h = b.max.y - b.min.y;
        if (h > 0.001 && h < 0.25) {
            clone.scale.set(100, 100, 100);
        } else if (h > 2.2) {
            const targetH = 1.72;
            const s = targetH / h;
            clone.scale.multiplyScalar(s);
        }

        clone.updateMatrixWorld(true);
        const postBox = new THREE.Box3().setFromObject(clone);
        if (Math.abs(postBox.min.y) > 0.01) {
            clone.position.y -= postBox.min.y;
        }

        const combatClips = this.animations['anims_combat'] || [];
        const charClips = this.animations[key] || this.animations['char_freya'] || [];
        const allClips = [...charClips, ...combatClips];
        clone.userData.animations = allClips;

        if (window.animationSystem) {
            if (combatClips.length > 0 && !window.animationSystem.isReady) {
                window.animationSystem.loadAnimationsFromGLB(allClips);
            }
            window.animationSystem.attachToCharacter(clone, allClips);
        }

        return clone;
    }

    // ── BUILDING INSTANCE ────────────────────────────────────────────────────
    createBuildingInstance(type, posX, posZ, rotY) {
        const keyMap = {
            'villa':      'building_villa',
            'townhouse':  'building_townhouse',
            'barracks':   'building_barracks',
            'hangar':     'building_hangar'
        };
        const key = keyMap[type] || 'building_barracks';
        const src = this.models[key];
        if (!src) return null;

        const g = this._safeClone(src);
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

        const group = new THREE.Group();
        const clone = this._safeClone(foundMesh);
        clone.position.set(0, 0, 0);
        clone.rotation.set(0, 0, 0);
        group.add(clone);
        group.position.set(posX || 0, 0, posZ || 0);
        if (rotY) group.rotation.y = rotY;
        if (scale !== 1.0) group.scale.multiplyScalar(scale);
        return group;
    }

    createModernHouseInstance(houseIndex, posX, posZ, rotY, scale = 1.0) {
        const src = this.models['building_modern'];
        if (!src) return null;
        const houseNum = ((houseIndex || 0) % 20) + 1;
        const houseName = `house${houseNum}`;
        const foundHouse = src.getObjectByName(houseName);
        if (!foundHouse) return null;

        const group = new THREE.Group();
        const clone = this._safeClone(foundHouse);
        clone.position.set(0, 0, 0);
        clone.rotation.set(0, 0, 0);
        group.add(clone);
        group.position.set(posX || 0, 0, posZ || 0);
        if (rotY) group.rotation.y = rotY;
        if (scale !== 1.0) group.scale.multiplyScalar(scale);
        return group;
    }

    createCoolingTowerInstance(posX, posZ) {
        const src = this.models['cooling_tower'];
        if (!src) return null;
        const g = this._safeClone(src);
        g.position.set(posX || 0, 0, posZ || 0);
        return g;
    }

    createWatchtowerInstance(posX, posZ) {
        const src = this.models['watchtower'];
        if (!src) return null;
        const g = this._safeClone(src);
        g.position.set(posX || 0, 0, posZ || 0);
        return g;
    }

    createContainerInstance(posX, posY, posZ, colorHex) {
        const src = this.models['container'];
        if (!src) return null;
        const g = this._safeClone(src);
        g.position.set(posX || 0, posY || 0, posZ || 0);
        if (colorHex) {
            g.traverse(c => {
                if (c.isMesh && c.material) {
                    c.material.color.setHex(colorHex);
                }
            });
        }
        return g;
    }

    createMcLarenInstance(posX, posY, posZ, rotY) {
        const src = this.models['mclaren'];
        if (!src) return null;
        const g = this._safeClone(src);
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
        if (treeType === 'palm') {
            key = 'tree_palm';
        } else if (treeType === 'oak_detailed' || treeType === 'oak') {
            key = this.models['tree_oak_detailed'] ? 'tree_oak_detailed' : 'tree_oak';
        } else {
            key = 'tree_oak';
        }
        const src = this.models[key] || this.models['tree_palm'] || this.models['tree_oak'];
        if (!src) return null;

        const g = this._safeClone(src);
        g.position.set(x, 0, z);
        if (s !== 1.0) g.scale.multiplyScalar(s);
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

        const g = this._safeClone(src);
        g.position.set(x, 0, z);
        if (s !== 1.0) g.scale.multiplyScalar(s);
        return g;
    }

    createGlooWallInstance(posX, posY, posZ, rotY) {
        const src = this.models['gloowall'];
        if (!src) return null;
        const g = this._safeClone(src);
        if (posX !== undefined) g.position.set(posX, posY || 0, posZ || 0);
        if (rotY !== undefined) g.rotation.y = rotY;
        return g;
    }

    createAirdropInstance(posX, posY, posZ) {
        const src = this.models['airdrop_crate'];
        if (!src) return null;
        const g = this._safeClone(src);
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
        const clone = this._safeClone(src);

        // Weapon proportions optimization (realistic human hand/torso scale)
        if (key === 'gun_ak47') clone.scale.set(0.80, 0.80, 0.80);
        else if (key === 'gun_awm') clone.scale.set(0.85, 0.85, 0.85);
        else if (key === 'gun_shotgun') clone.scale.set(0.80, 0.80, 0.80);
        else if (key === 'gun_deagle') clone.scale.set(0.85, 0.85, 0.85);

        return clone;
    }

    createParachuteInstance() {
        const src = this.models['parachute'];
        if (!src) return null;
        return this._safeClone(src);
    }
}

window.blenderAssetLoader = new BlenderAssetLoader();
