#!/usr/bin/env bash
# ----------------------------------------------------------------------
#   Recipe Manager - debug launcher (with console, macOS / Linux)
#   Use this when you need stdout/stderr for troubleshooting.
# ----------------------------------------------------------------------
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# プロジェクトルートに移動 (このスクリプトは scripts/unix/ にある)
cd "$SCRIPT_DIR/../.."

# JAR が無い、またはソースが JAR より新しい場合はビルド
if [ ! -f "RecipeManager.jar" ] || [ -n "$(find src -name '*.java' -newer RecipeManager.jar | head -n 1)" ]; then
    echo "RecipeManager.jar is missing or older than the sources. Running build first..."
    bash "$SCRIPT_DIR/build.command" nopause
fi

if ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] java not found. Please install JRE/JDK 17 or later."
    read -n 1 -s -r -p "Press any key to exit..."
    exit 1
fi

echo "=== Launching with console attached. Close the GUI to exit. ==="
java -jar RecipeManager.jar

echo
echo "=== Application exited. ==="
read -n 1 -s -r -p "Press any key to close..."
