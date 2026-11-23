// War Land 3D - Procedural WebAudio Sound Synthesizer
// Zero external audio files needed; ultra-low latency & 100% offline reliable.

class SoundEngine {
    constructor() {
        this.ctx = null;
        this.enabled = true;
        this.masterVolume = 0.8;
        this.sfxVolume = 1.0;
        this.initOnUserGesture();
    }

    initOnUserGesture() {
        const unlock = () => {
            if (!this.ctx) {
                const AudioCtx = window.AudioContext || window.webkitAudioContext;
                if (AudioCtx) {
                    this.ctx = new AudioCtx();
                }
            }
            if (this.ctx && this.ctx.state === 'suspended') {
                this.ctx.resume();
            }
            window.removeEventListener('pointerdown', unlock);
            window.removeEventListener('keydown', unlock);
        };
        window.addEventListener('pointerdown', unlock);
        window.addEventListener('keydown', unlock);
    }

    getGain(vol = 1.0) {
        if (!this.ctx || !this.enabled) return null;
        const gain = this.ctx.createGain();
        gain.gain.value = vol * this.masterVolume * this.sfxVolume;
        gain.connect(this.ctx.destination);
        return gain;
    }

    // Play Assault Rifle (AK-47 / Scar)
    playRifleShot() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.7);
        if (!gain) return;

        // Punch noise
        const bufferSize = this.ctx.sampleRate * 0.12;
        const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (this.ctx.sampleRate * 0.02));
        }
        const noise = this.ctx.createBufferSource();
        noise.buffer = buffer;

        // Bandpass filter for military crack
        const filter = this.ctx.createBiquadFilter();
        filter.type = 'bandpass';
        filter.frequency.setValueAtTime(1400, now);
        filter.Q.setValueAtTime(2.5, now);

        // Low oscillator for kick
        const osc = this.ctx.createOscillator();
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(160, now);
        osc.frequency.exponentialRampToValueAtTime(30, now + 0.08);

        const oscGain = this.ctx.createGain();
        oscGain.gain.setValueAtTime(1.0, now);
        oscGain.gain.exponentialRampToValueAtTime(0.01, now + 0.09);

        noise.connect(filter);
        filter.connect(gain);
        osc.connect(oscGain);
        oscGain.connect(gain);

        noise.start(now);
        osc.start(now);
        noise.stop(now + 0.12);
        osc.stop(now + 0.09);
    }

    // Play Sniper (AWM)
    playSniperShot() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(1.0);
        if (!gain) return;

        // Massive crack
        const bufferSize = this.ctx.sampleRate * 0.45;
        const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (this.ctx.sampleRate * 0.09));
        }
        const noise = this.ctx.createBufferSource();
        noise.buffer = buffer;

        const filter = this.ctx.createBiquadFilter();
        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(3200, now);
        filter.frequency.exponentialRampToValueAtTime(200, now + 0.4);

        const sub = this.ctx.createOscillator();
        sub.type = 'sine';
        sub.frequency.setValueAtTime(120, now);
        sub.frequency.exponentialRampToValueAtTime(20, now + 0.35);

        const subGain = this.ctx.createGain();
        subGain.gain.setValueAtTime(1.2, now);
        subGain.gain.exponentialRampToValueAtTime(0.01, now + 0.35);

        noise.connect(filter);
        filter.connect(gain);
        sub.connect(subGain);
        subGain.connect(gain);

        noise.start(now);
        sub.start(now);
        noise.stop(now + 0.45);
        sub.stop(now + 0.35);
    }

    // Play Shotgun (SPAS-12)
    playShotgunShot() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.85);
        if (!gain) return;

        const bufferSize = this.ctx.sampleRate * 0.25;
        const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (this.ctx.sampleRate * 0.04));
        }
        const noise = this.ctx.createBufferSource();
        noise.buffer = buffer;

        const osc = this.ctx.createOscillator();
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(90, now);
        osc.frequency.exponentialRampToValueAtTime(25, now + 0.2);

        const oscGain = this.ctx.createGain();
        oscGain.gain.setValueAtTime(1.0, now);
        oscGain.gain.exponentialRampToValueAtTime(0.01, now + 0.2);

        noise.connect(gain);
        osc.connect(oscGain);
        oscGain.connect(gain);

        noise.start(now);
        osc.start(now);
        noise.stop(now + 0.25);
        osc.stop(now + 0.2);
    }

    // Play Pistol (Desert Eagle)
    playPistolShot() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.6);
        if (!gain) return;

        const osc = this.ctx.createOscillator();
        osc.type = 'square';
        osc.frequency.setValueAtTime(450, now);
        osc.frequency.exponentialRampToValueAtTime(60, now + 0.07);

        const oscGain = this.ctx.createGain();
        oscGain.gain.setValueAtTime(0.8, now);
        oscGain.gain.exponentialRampToValueAtTime(0.01, now + 0.07);

        osc.connect(oscGain);
        oscGain.connect(gain);

        osc.start(now);
        osc.stop(now + 0.08);
    }

    // Hit Marker (Ding / Thud)
    playHitMarker(headshot = false) {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.75);
        if (!gain) return;

        const osc = this.ctx.createOscillator();
        osc.type = headshot ? 'triangle' : 'sine';
        const startFreq = headshot ? 1760 : 880;
        osc.frequency.setValueAtTime(startFreq, now);
        osc.frequency.exponentialRampToValueAtTime(headshot ? 2200 : 550, now + 0.08);

        const oscGain = this.ctx.createGain();
        oscGain.gain.setValueAtTime(0.9, now);
        oscGain.gain.exponentialRampToValueAtTime(0.01, now + 0.09);

        osc.connect(oscGain);
        oscGain.connect(gain);

        osc.start(now);
        osc.stop(now + 0.09);
    }

    // Weapon Reload Click
    playReload() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.5);
        if (!gain) return;

        [0, 0.14, 0.32].forEach((offset, idx) => {
            const osc = this.ctx.createOscillator();
            osc.type = 'triangle';
            osc.frequency.setValueAtTime(600 + idx * 150, now + offset);
            osc.frequency.exponentialRampToValueAtTime(300, now + offset + 0.04);

            const g = this.ctx.createGain();
            g.gain.setValueAtTime(0.5, now + offset);
            g.gain.exponentialRampToValueAtTime(0.01, now + offset + 0.04);

            osc.connect(g);
            g.connect(gain);

            osc.start(now + offset);
            osc.stop(now + offset + 0.05);
        });
    }

    // Grenade Explosion
    playExplosion() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(1.0);
        if (!gain) return;

        const bufferSize = this.ctx.sampleRate * 1.2;
        const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (this.ctx.sampleRate * 0.3));
        }
        const noise = this.ctx.createBufferSource();
        noise.buffer = buffer;

        const filter = this.ctx.createBiquadFilter();
        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(450, now);
        filter.frequency.exponentialRampToValueAtTime(40, now + 1.1);

        const sub = this.ctx.createOscillator();
        sub.type = 'sine';
        sub.frequency.setValueAtTime(90, now);
        sub.frequency.exponentialRampToValueAtTime(20, now + 1.0);

        const subGain = this.ctx.createGain();
        subGain.gain.setValueAtTime(1.5, now);
        subGain.gain.exponentialRampToValueAtTime(0.01, now + 1.0);

        noise.connect(filter);
        filter.connect(gain);
        sub.connect(subGain);
        subGain.connect(gain);

        noise.start(now);
        sub.start(now);
        noise.stop(now + 1.2);
        sub.stop(now + 1.0);
    }

    // Footstep Sound
    playFootstep(surface = 'grass') {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.25);
        if (!gain) return;

        const osc = this.ctx.createOscillator();
        osc.type = surface === 'concrete' ? 'square' : 'triangle';
        osc.frequency.setValueAtTime(surface === 'concrete' ? 180 : 80, now);
        osc.frequency.exponentialRampToValueAtTime(30, now + 0.05);

        const g = this.ctx.createGain();
        g.gain.setValueAtTime(0.3, now);
        g.gain.exponentialRampToValueAtTime(0.01, now + 0.05);

        osc.connect(g);
        g.connect(gain);

        osc.start(now);
        osc.stop(now + 0.06);
    }

    // Medkit Healed
    playMedkit() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.6);
        if (!gain) return;

        const freqs = [330, 440, 550, 660];
        freqs.forEach((freq, idx) => {
            const osc = this.ctx.createOscillator();
            osc.type = 'sine';
            osc.frequency.setValueAtTime(freq, now + idx * 0.08);

            const g = this.ctx.createGain();
            g.gain.setValueAtTime(0.4, now + idx * 0.08);
            g.gain.exponentialRampToValueAtTime(0.01, now + idx * 0.08 + 0.15);

            osc.connect(g);
            g.connect(gain);

            osc.start(now + idx * 0.08);
            osc.stop(now + idx * 0.08 + 0.16);
        });
    }

    // Victory Fanfare (Booyah!)
    playVictory() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.8);
        if (!gain) return;

        const melody = [
            { f: 523.25, d: 0.15, t: 0.0 },   // C5
            { f: 523.25, d: 0.15, t: 0.18 },  // C5
            { f: 523.25, d: 0.15, t: 0.36 },  // C5
            { f: 659.25, d: 0.45, t: 0.54 },  // E5
            { f: 783.99, d: 0.60, t: 1.05 }   // G5
        ];

        melody.forEach(note => {
            const osc = this.ctx.createOscillator();
            osc.type = 'triangle';
            osc.frequency.setValueAtTime(note.f, now + note.t);

            const g = this.ctx.createGain();
            g.gain.setValueAtTime(0.6, now + note.t);
            g.gain.exponentialRampToValueAtTime(0.01, now + note.t + note.d);

            osc.connect(g);
            g.connect(gain);

            osc.start(now + note.t);
            osc.stop(now + note.t + note.d + 0.02);
        });
    }

    // Free Fire Gloo Wall Ice Erection Sound
    playGlooWallDeploy() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.9);
        if (!gain) return;

        // Ice rise sweep
        const osc = this.ctx.createOscillator();
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(180, now);
        osc.frequency.exponentialRampToValueAtTime(850, now + 0.22);

        const filter = this.ctx.createBiquadFilter();
        filter.type = 'bandpass';
        filter.frequency.setValueAtTime(900, now);
        filter.Q.setValueAtTime(3.0, now);

        const g = this.ctx.createGain();
        g.gain.setValueAtTime(0.7, now);
        g.gain.exponentialRampToValueAtTime(0.01, now + 0.35);

        osc.connect(filter);
        filter.connect(g);
        g.connect(gain);

        osc.start(now);
        osc.stop(now + 0.36);
    }

    playShoot(weaponId = 'AK47') {
        const id = (weaponId || '').toUpperCase();
        if (id.includes('AWM') || id.includes('SNIPER')) {
            this.playSniperShot();
        } else if (id.includes('SPAS') || id.includes('SHOTGUN')) {
            this.playShotgunShot();
        } else if (id.includes('DEAGLE') || id.includes('PISTOL')) {
            this.playPistolShot();
        } else {
            this.playRifleShot();
        }
    }

    playKillSound() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.95);
        if (!gain) return;

        // Triumphant Kill Chime (Free Fire / CS style)
        [523.25, 659.25, 783.99, 1046.50].forEach((freq, idx) => {
            const osc = this.ctx.createOscillator();
            osc.type = 'sine';
            osc.frequency.setValueAtTime(freq, now + idx * 0.05);

            const g = this.ctx.createGain();
            g.gain.setValueAtTime(0.6, now + idx * 0.05);
            g.gain.exponentialRampToValueAtTime(0.01, now + idx * 0.05 + 0.25);

            osc.connect(g);
            g.connect(gain);
            osc.start(now + idx * 0.05);
            osc.stop(now + idx * 0.05 + 0.26);
        });
    }

    playLootPickup() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.7);
        if (!gain) return;

        const osc = this.ctx.createOscillator();
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(587.33, now); // D5
        osc.frequency.exponentialRampToValueAtTime(880.00, now + 0.09); // A5

        const g = this.ctx.createGain();
        g.gain.setValueAtTime(0.5, now);
        g.gain.exponentialRampToValueAtTime(0.01, now + 0.12);

        osc.connect(g);
        g.connect(gain);
        osc.start(now);
        osc.stop(now + 0.13);
    }

    // Play Melee Punch Air Whoosh
    playPunch() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.7);
        if (!gain) return;

        const osc = this.ctx.createOscillator();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(140, now);
        osc.frequency.exponentialRampToValueAtTime(45, now + 0.12);

        const g = this.ctx.createGain();
        g.gain.setValueAtTime(0.8, now);
        g.gain.exponentialRampToValueAtTime(0.01, now + 0.14);

        osc.connect(g);
        g.connect(gain);
        osc.start(now);
        osc.stop(now + 0.15);
    }

    // Play Melee Punch Impact Hit on Enemy
    playPunchHit() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.9);
        if (!gain) return;

        // Heavy flesh smack
        const bufferSize = this.ctx.sampleRate * 0.15;
        const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (this.ctx.sampleRate * 0.02));
        }
        const noise = this.ctx.createBufferSource();
        noise.buffer = buffer;

        const filter = this.ctx.createBiquadFilter();
        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(650, now);

        const osc = this.ctx.createOscillator();
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(180, now);
        osc.frequency.exponentialRampToValueAtTime(40, now + 0.10);

        const oscGain = this.ctx.createGain();
        oscGain.gain.setValueAtTime(1.2, now);
        oscGain.gain.exponentialRampToValueAtTime(0.01, now + 0.12);

        noise.connect(filter);
        filter.connect(gain);
        osc.connect(oscGain);
        oscGain.connect(gain);

        noise.start(now);
        osc.start(now);
        noise.stop(now + 0.15);
        osc.stop(now + 0.13);
    }

    // Play Parachute Canopy Deploy
    playParachuteOpen() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.85);
        if (!gain) return;

        const bufferSize = this.ctx.sampleRate * 0.6;
        const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = (Math.random() * 2 - 1) * Math.sin(i / (this.ctx.sampleRate * 0.05)) * Math.exp(-i / (this.ctx.sampleRate * 0.2));
        }
        const noise = this.ctx.createBufferSource();
        noise.buffer = buffer;

        const filter = this.ctx.createBiquadFilter();
        filter.type = 'bandpass';
        filter.frequency.setValueAtTime(500, now);
        filter.Q.setValueAtTime(1.5, now);

        noise.connect(filter);
        filter.connect(gain);
        noise.start(now);
        noise.stop(now + 0.6);
    }

    // Play Landing Thud on Ground
    playLandingThud() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.9);
        if (!gain) return;

        const osc = this.ctx.createOscillator();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(90, now);
        osc.frequency.exponentialRampToValueAtTime(25, now + 0.22);

        const g = this.ctx.createGain();
        g.gain.setValueAtTime(1.0, now);
        g.gain.exponentialRampToValueAtTime(0.01, now + 0.24);

        osc.connect(g);
        g.connect(gain);
        osc.start(now);
        osc.stop(now + 0.25);
    }

    // Play Atmospheric Thunder Rumble
    playThunder() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.75);
        if (!gain) return;

        const osc = this.ctx.createOscillator();
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(75, now);
        osc.frequency.exponentialRampToValueAtTime(22, now + 1.8);

        const filter = this.ctx.createBiquadFilter();
        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(140, now);

        const g = this.ctx.createGain();
        g.gain.setValueAtTime(0.01, now);
        g.gain.linearRampToValueAtTime(0.85, now + 0.15);
        g.gain.exponentialRampToValueAtTime(0.001, now + 1.95);

        osc.connect(filter);
        filter.connect(g);
        g.connect(gain);
        osc.start(now);
        osc.stop(now + 2.0);
    }

    // Play Ground Friction Slide Whoosh
    playSlide() {
        if (!this.ctx || !this.enabled) return;
        const now = this.ctx.currentTime;
        const gain = this.getGain(0.65);
        if (!gain) return;

        // Friction noise buffer
        const bufferSize = Math.floor(this.ctx.sampleRate * 0.45);
        const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (this.ctx.sampleRate * 0.18));
        }
        const noise = this.ctx.createBufferSource();
        noise.buffer = buffer;

        const filter = this.ctx.createBiquadFilter();
        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(800, now);
        filter.frequency.exponentialRampToValueAtTime(280, now + 0.45);

        const g = this.ctx.createGain();
        g.gain.setValueAtTime(0.01, now);
        g.gain.linearRampToValueAtTime(0.9, now + 0.05);
        g.gain.exponentialRampToValueAtTime(0.01, now + 0.45);

        noise.connect(filter);
        filter.connect(g);
        g.connect(gain);

        noise.start(now);
        noise.stop(now + 0.45);
    }
}

window.soundEngine = new SoundEngine();
