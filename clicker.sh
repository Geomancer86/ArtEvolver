#!/usr/bin/env bash
set -euo pipefail

# Extract version from POM
PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null | grep -E '^[0-9]' || true)
if [ -z "$PROJECT_VERSION" ]; then
    PROJECT_VERSION=$(grep -m1 '<version>' pom.xml 2>/dev/null | sed 's/.*<version>//;s/<\/version>.*//' || echo "unknown")
fi
[ -z "$PROJECT_VERSION" ] && PROJECT_VERSION="unknown"

echo ""
echo " ============================================"
echo "  ArtEvolver $PROJECT_VERSION - Evolution Clicker"
echo " ============================================"
echo ""
echo " Browser-based clicker game. No Java UI needed."
echo " Drop an image in the browser and start evolving!"
echo ""

# -------------------------------------------------------------------
# Check Java
# -------------------------------------------------------------------
if ! command -v java &>/dev/null; then
    echo " [ERROR] Java not found on PATH."
    echo "         Install Java 21+ from https://adoptium.net/"
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
# Build (skip if already compiled for this version)
# -------------------------------------------------------------------
NEED_BUILD=0
[ ! -f "artevolver-core/target/classes/com/rndmodgames/evolver/ArtEvolver.class" ] && NEED_BUILD=1
CACHED_VERSION=""
[ -f "artevolver-core/target/.build-version" ] && CACHED_VERSION=$(cat "artevolver-core/target/.build-version")
[ "$CACHED_VERSION" != "$PROJECT_VERSION" ] && NEED_BUILD=1

if [ "$NEED_BUILD" = "1" ]; then
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
    echo "$PROJECT_VERSION" > "artevolver-core/target/.build-version"
    echo " [OK] Build complete"
else
    echo " [OK] Build exists"
fi

# -------------------------------------------------------------------
# Launch clicker mode
# -------------------------------------------------------------------
echo ""
echo " Starting Evolution Clicker..."
echo " The game will open in your browser automatically."
echo " (Press Ctrl+C to stop the server)"
echo ""

mvn exec:java -pl artevolver-core \
    -Dexec.mainClass="com.rndmodgames.evolver.ArtEvolver" \
    -Dexec.args="--clicker" -q
