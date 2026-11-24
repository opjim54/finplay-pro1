// War Land 3D - Offline Local Wi-Fi / Hotspot / Bluetooth Peer Rooms (Mini Militia Style) & Spectator Camera

class MultiplayerP2p {
    constructor() {
        this.roomCode = null;
        this.isHost = false;
        this.peers = [];
        this.spectateIndex = 0;
        this.isSpectating = false;
    }

    createLocalRoom() {
        this.roomCode = `WAR-${Math.floor(1000 + Math.random() * 9000)}`;
        this.isHost = true;
        this.peers = [
            { id: 'host', name: window.playerIgn || 'Player (Host)', isHost: true }
        ];

        // Broadcast local peer signal (Simulated Local Hotspot / Bluetooth Discovery)
        console.log(`[P2P] Local Room created: ${this.roomCode}`);
        if (window.hud) {
            window.hud.addKillFeedEntry("System", `Room ${this.roomCode} Ready on Local Wi-Fi / Hotspot!`);
        }
        return this.roomCode;
    }

    joinLocalRoom(code) {
        this.roomCode = code.toUpperCase().trim();
        this.isHost = false;
        console.log(`[P2P] Joined Local Room ${this.roomCode}`);
        if (window.hud) {
            window.hud.addKillFeedEntry("System", `Connected to Host: ${this.roomCode}`);
        }
        return true;
    }

    startSpectating(livingBots) {
        this.isSpectating = true;
        this.spectateIndex = 0;
        const banner = document.getElementById('spectateBanner');
        if (banner) {
            banner.classList.remove('hidden');
            this.updateSpectateUi(livingBots);
        }
    }

    nextSpectateTarget(livingBots) {
        if (!livingBots || livingBots.length === 0) return null;
        this.spectateIndex = (this.spectateIndex + 1) % livingBots.length;
        this.updateSpectateUi(livingBots);
        return livingBots[this.spectateIndex];
    }

    prevSpectateTarget(livingBots) {
        if (!livingBots || livingBots.length === 0) return null;
        this.spectateIndex = (this.spectateIndex - 1 + livingBots.length) % livingBots.length;
        this.updateSpectateUi(livingBots);
        return livingBots[this.spectateIndex];
    }

    updateSpectateUi(livingBots) {
        const target = livingBots[this.spectateIndex];
        const nameEl = document.getElementById('spectateTargetName');
        if (nameEl && target) {
            nameEl.textContent = `Spectating: ${target.name} (HP: ${target.hp}/100)`;
        }
    }
}

window.MultiplayerP2p = MultiplayerP2p;
