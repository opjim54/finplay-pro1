#!/usr/bin/env bash

# FinPlay Pro - One-Click APK Builder for Linux & macOS
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "====================================================================="
echo "          FINPLAY PRO - ANDROID APK BUILD SYSTEM (V1.0)"
echo "====================================================================="
echo ""

# Make gradlew executable
chmod +x ./gradlew 2>/dev/null || true

echo "Select an option:"
echo " [1] Build DEBUG APK (Fastest, ready to test)"
echo " [2] Build RELEASE APK"
echo " [3] Clean & Rebuild"
echo " [4] Run Local Node.js Server"
echo " [0] Exit"
read -p "Enter choice [1-4, 0]: " choice

case "$choice" in
    1)
        echo "Building FinPlay Pro DEBUG APK..."
        ./gradlew assembleDebug
        echo "Debug APK location: app/build/outputs/apk/debug/app-debug.apk"
        ;;
    2)
        echo "Building FinPlay Pro RELEASE APK..."
        ./gradlew assembleRelease
        echo "Release APK location: app/build/outputs/apk/release/"
        ;;
    3)
        echo "Cleaning and rebuilding..."
        ./gradlew clean assembleDebug
        ;;
    4)
        echo "Starting local game server on port 3000..."
        node server.js
        ;;
    0)
        exit 0
        ;;
    *)
        echo "Building Debug APK by default..."
        ./gradlew assembleDebug
        ;;
esac
