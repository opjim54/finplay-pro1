// War Land 3D - Android Kotlin Javascript Bridge & Communication Layer

class WarLandBridge {
    constructor() {
        this.playerName = "Warrior_Pro";
        this.userProfilePic = null;
        this.equippedSkin = "war_char_commando";
        this.equippedGunSkin = "war_gun_dragon_ak";
        this.setupBridge();
    }

    setupBridge() {
        // Exposed methods called from Android Kotlin WebView
        window.setPlayerData = (name, profilePic, charSkin, gunSkin) => {
            if (name) {
                this.playerName = name;
                window.playerIgn = name;
                const nameEl = document.getElementById('playerIgnDisplay');
                if (nameEl) nameEl.textContent = name;
            }
            if (profilePic) {
                this.userProfilePic = profilePic;
                const picEl = document.getElementById('playerProfileImg');
                if (picEl) {
                    picEl.src = profilePic;
                    picEl.style.display = 'block';
                }
            }
            if (charSkin) {
                this.equippedSkin = charSkin;
                if (window.player && typeof window.player.applySkin === 'function') {
                    window.player.applySkin(charSkin);
                }
            }
            if (gunSkin) {
                this.equippedGunSkin = gunSkin;
                if (window.player && typeof window.player.applyWeaponSkin === 'function') {
                    window.player.applyWeaponSkin(gunSkin);
                }
            }
        };

        window.exitGameToArena = () => {
            if (window.AndroidBridge && typeof window.AndroidBridge.exitToArena === 'function') {
                window.AndroidBridge.exitToArena();
            } else {
                console.log("[Bridge] Exit clicked (Browser Preview Mode)");
                window.history.back();
            }
        };

        window.onMatchCompleted = (kills, rank, coins) => {
            if (window.AndroidBridge && typeof window.AndroidBridge.onMatchResult === 'function') {
                window.AndroidBridge.onMatchResult(kills, rank, coins);
            } else {
                console.log(`[Bridge] Match finished: Kills=${kills}, Rank=${rank}, Won=${coins} coins`);
            }
        };
    }
}

window.warLandBridge = new WarLandBridge();
