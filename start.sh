#!/usr/bin/env bash
set -euo pipefail

echo ""
echo " ============================================"
echo "  ArtEvolver v3.1 - Launcher"
echo " ============================================"
echo ""

# -------------------------------------------------------------------
# Check Java
# -------------------------------------------------------------------
if ! command -v java &>/dev/null; then
    echo " [ERROR] Java not found on PATH."
    echo "         Install Java 17+ from https://adoptium.net/"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
echo " [OK] Java found: $JAVA_VER"

# -------------------------------------------------------------------
# Check Maven
# -------------------------------------------------------------------
if ! command -v mvn &>/dev/null; then
    echo " [ERROR] Maven not found on PATH."
    echo "         Install Maven 3.6+ from https://maven.apache.org/"
    exit 1
fi
echo " [OK] Maven found"

# -------------------------------------------------------------------
# Build (skip if already compiled)
# -------------------------------------------------------------------
if [ "$1" = "--rebuild" ] 2>/dev/null || \
   [ ! -f "artevolver-core/target/classes/com/rndmodgames/evolver/ArtEvolver.class" ]; then
    echo ""
    echo " Building ArtEvolver..."
    echo ""
    mvn compile -pl artevolver-core -q || {
        echo " [ERROR] Build failed. Trying full rebuild..."
        mvn clean compile -pl artevolver-core || {
            echo " [ERROR] Build failed. Check errors above."
            exit 1
        }
    }
    echo " [OK] Build complete"
else
    echo " [OK] Build exists (use --rebuild to force)"
fi

# -------------------------------------------------------------------
# Run
# -------------------------------------------------------------------
echo ""
echo " Starting ArtEvolver..."
echo " (Close the window or press Ctrl+C to stop)"
echo ""

mvn exec:java -pl artevolver-core \
    -Dexec.mainClass="com.rndmodgames.evolver.ArtEvolver" -q
