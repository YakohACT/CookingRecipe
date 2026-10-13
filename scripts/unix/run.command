#!/usr/bin/env bash
# ----------------------------------------------------------------------
#   Recipe Manager - launcher (macOS / Linux)
#   - Auto-builds the JAR on first run, or when sources are newer than the JAR
#   - Launches the GUI in the background
# ----------------------------------------------------------------------
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# プロジェクトルートに移動 (このスクリプトは scripts/unix/ にある)
cd "$SCRIPT_DIR/../.."

# 初回実行時、またはソースが JAR より新しい場合にビルド
if [ ! -f "RecipeManager.jar" ] || [ -n "$(find src -name '*.java' -newer RecipeManager.jar | head -n 1)" ]; then
    echo "Building RecipeManager.jar..."
    bash "$SCRIPT_DIR/build.command" nopause
fi

# JRE/JDK チェック
if ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] java not found. Please install JRE/JDK 17 or later."
    read -n 1 -s -r -p "Press any key to exit..."
    exit 1
fi

# GUI をバックグラウンド起動 (端末を閉じても継続)
# nohup でハングアップ無視、stdout/stderr は破棄
nohup java -jar RecipeManager.jar >/dev/null 2>&1 &
disown 2>/dev/null || true
