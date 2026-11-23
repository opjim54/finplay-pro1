// War Land 3D - Dynamic Environmental Weather & Atmospheric VFX System
// Clear Tropical Day, Monsoon Storm (Rain + Thunder + Lightning), and Sunset Dusk

class WarLandWeather {
    constructor(scene, sunLight, hemiLight, skyDome, camera) {
        this.scene = scene;
        this.sunLight = sunLight;
        this.hemiLight = hemiLight;
        this.skyDome = skyDome;
        this.camera = camera;

        this.currentMode = 'SUNNY'; // 'SUNNY', 'RAIN', 'SUNSET'
        this.weatherTimer = 0;
        this.autoCycleTime = 180; // 3 minutes per cycle in matches

        // Rain Particle System
        this.rainCount = 1600;
        this.rainBoxSize = 75; // 75m box around camera
        this.rainGeometry = null;
        this.rainMaterial = null;
        this.rainLines = null;
        this.rainPositions = null;
        this.rainSpeed = 28.0; // falling speed m/s

        // Lightning Flash
        this.lightningTimer = 0;
        this.lightningNext = 6.0 + Math.random() * 8.0;
        this.lightningMesh = null;
        this.lightningFlashTime = 0;

        // Sunset & Fog settings
        this.targetSunColor = new THREE.Color(0xFFFBEB);
        this.targetHemiSky = new THREE.Color(0xFFFFFF);
        this.targetHemiGround = new THREE.Color(0x334155);
        this.targetSunIntensity = 1.35;
        this.targetFogDensity = 0.0018;
        this.targetFogColor = new THREE.Color(0x38BDF8);

        this.initRainSystem();
        this.initLightningLight();
    }

    initRainSystem() {
        const lineCount = this.rainCount;
        const positions = new Float32Array(lineCount * 6); // 2 vertices per line streak

        for (let i = 0; i < lineCount; i++) {
            const x = (Math.random() - 0.5) * this.rainBoxSize;
            const y = Math.random() * this.rainBoxSize;
            const z = (Math.random() - 0.5) * this.rainBoxSize;
            const streakLen = 0.85 + Math.random() * 0.45;

            const idx = i * 6;
            // Top vertex
            positions[idx] = x;
            positions[idx + 1] = y;
            positions[idx + 2] = z;
            // Bottom vertex (slanted slightly along wind -X)
            positions[idx + 3] = x - 0.15;
            positions[idx + 4] = y - streakLen;
            positions[idx + 5] = z;
        }

        this.rainPositions = positions;
        this.rainGeometry = new THREE.BufferGeometry();
        this.rainGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));

        this.rainMaterial = new THREE.LineBasicMaterial({
            color: 0x93C5FD,
            transparent: true,
            opacity: 0.0, // Hidden in sunny weather
            depthWrite: false
        });

        this.rainLines = new THREE.LineSegments(this.rainGeometry, this.rainMaterial);
        this.rainLines.frustumCulled = false;
        this.scene.add(this.rainLines);
    }

    initLightningLight() {
        this.lightningLight = new THREE.DirectionalLight(0xE0F2FE, 0.0);
        this.lightningLight.position.set(20, 150, 20);
        this.scene.add(this.lightningLight);
    }

    setWeather(mode) {
        this.currentMode = mode;
        this.weatherTimer = 0;

        const skyCanvas = document.createElement('canvas');
        skyCanvas.width = 16;
        skyCanvas.height = 256;
        const sctx = skyCanvas.getContext('2d');
        const grad = sctx.createLinearGradient(0, 0, 0, 256);

        if (mode === 'SUNNY') {
            grad.addColorStop(0.0, '#0369A1');
            grad.addColorStop(0.55, '#38BDF8');
            grad.addColorStop(0.82, '#BAE6FD');
            grad.addColorStop(1.0, '#FEF08A');
            this.targetSunColor.set(0xFFFBEB);
            this.targetHemiSky.set(0xFFFFFF);
            this.targetHemiGround.set(0x334155);
            this.targetSunIntensity = 1.35;
            this.targetFogDensity = 0.0016;
            this.targetFogColor.set(0x38BDF8);
            if (this.rainMaterial) this.rainMaterial.opacity = 0.0;
        } else if (mode === 'RAIN') {
            grad.addColorStop(0.0, '#0F172A');
            grad.addColorStop(0.5, '#334155');
            grad.addColorStop(0.8, '#475569');
            grad.addColorStop(1.0, '#64748B');
            this.targetSunColor.set(0x94A3B8);
            this.targetHemiSky.set(0x475569);
            this.targetHemiGround.set(0x1E293B);
            this.targetSunIntensity = 0.65;
            this.targetFogDensity = 0.0040;
            this.targetFogColor.set(0x475569);
            if (this.rainMaterial) this.rainMaterial.opacity = 0.70;
            if (window.soundEngine && window.soundEngine.playThunder) {
                window.soundEngine.playThunder();
            }
        } else if (mode === 'SUNSET') {
            grad.addColorStop(0.0, '#4C1D95'); // Deep twilight violet
            grad.addColorStop(0.35, '#C2410C'); // Fiery amber
            grad.addColorStop(0.70, '#EA580C'); // Vibrant orange
            grad.addColorStop(1.0, '#FBBF24'); // Golden glow
            this.targetSunColor.set(0xFB923C);
            this.targetHemiSky.set(0xFDBA74);
            this.targetHemiGround.set(0x431407);
            this.targetSunIntensity = 1.10;
            this.targetFogDensity = 0.0022;
            this.targetFogColor.set(0xC2410C);
            if (this.rainMaterial) this.rainMaterial.opacity = 0.0;
        }

        sctx.fillStyle = grad;
        sctx.fillRect(0, 0, 16, 256);

        if (this.skyDome && this.skyDome.material && this.skyDome.material.map) {
            this.skyDome.material.map.dispose();
            this.skyDome.material.map = new THREE.CanvasTexture(skyCanvas);
            this.skyDome.material.map.needsUpdate = true;
        }

        // Update HUD weather button text
        const btnWeather = document.getElementById('btnWeather');
        if (btnWeather) {
            if (mode === 'SUNNY') btnWeather.textContent = '☀️';
            else if (mode === 'RAIN') btnWeather.textContent = '🌧️';
            else if (mode === 'SUNSET') btnWeather.textContent = '🌅';
        }
    }

    cycleWeather() {
        if (this.currentMode === 'SUNNY') this.setWeather('RAIN');
        else if (this.currentMode === 'RAIN') this.setWeather('SUNSET');
        else this.setWeather('SUNNY');
    }

    update(dt, playerPos) {
        this.weatherTimer += dt;
        if (this.weatherTimer >= this.autoCycleTime) {
            this.cycleWeather();
        }

        // Smooth lighting and fog interpolation
        if (this.sunLight) {
            this.sunLight.color.lerp(this.targetSunColor, dt * 2.0);
            this.sunLight.intensity = THREE.MathUtils.lerp(this.sunLight.intensity, this.targetSunIntensity, dt * 2.0);
        }
        if (this.hemiLight) {
            this.hemiLight.color.lerp(this.targetHemiSky, dt * 2.0);
            this.hemiLight.groundColor.lerp(this.targetHemiGround, dt * 2.0);
        }
        if (this.scene.fog) {
            this.scene.fog.color.lerp(this.targetFogColor, dt * 2.0);
            this.scene.fog.density = THREE.MathUtils.lerp(this.scene.fog.density, this.targetFogDensity, dt * 2.0);
        }

        // Update Rain Particles
        if (this.currentMode === 'RAIN' && this.rainLines && playerPos) {
            const posAttr = this.rainGeometry.attributes.position;
            const arr = posAttr.array;
            const halfBox = this.rainBoxSize * 0.5;
            const fallDist = this.rainSpeed * dt;

            for (let i = 0; i < this.rainCount; i++) {
                const idx = i * 6;
                // Update Y
                arr[idx + 1] -= fallDist;
                arr[idx + 4] -= fallDist;

                // Center around player
                const dy = arr[idx + 1] - playerPos.y;
                if (dy < -2.0) {
                    // Wrap back to top of box above player
                    arr[idx + 1] = playerPos.y + halfBox;
                    arr[idx + 4] = arr[idx + 1] - (0.85 + Math.random() * 0.45);
                    arr[idx] = playerPos.x + (Math.random() - 0.5) * this.rainBoxSize;
                    arr[idx + 3] = arr[idx] - 0.15;
                    arr[idx + 2] = playerPos.z + (Math.random() - 0.5) * this.rainBoxSize;
                    arr[idx + 5] = arr[idx + 2];
                }
            }
            posAttr.needsUpdate = true;

            // Lightning Thunder Flash
            this.lightningTimer += dt;
            if (this.lightningTimer >= this.lightningNext) {
                this.lightningTimer = 0;
                this.lightningNext = 8.0 + Math.random() * 12.0;
                this.triggerLightning();
            }
        }

        // Fade lightning flash
        if (this.lightningFlashTime > 0) {
            this.lightningFlashTime -= dt;
            if (this.lightningLight) {
                this.lightningLight.intensity = Math.max(0, this.lightningFlashTime * 8.0);
            }
        }
    }

    triggerLightning() {
        this.lightningFlashTime = 0.28;
        if (this.lightningLight) {
            this.lightningLight.intensity = 2.5;
        }
        if (window.soundEngine && window.soundEngine.playThunder) {
            window.soundEngine.playThunder();
        }
    }
}

window.WarLandWeather = WarLandWeather;
