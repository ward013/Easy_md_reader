#!/usr/bin/env bash

set -euo pipefail

APP_NAME="EasyMDReader"
APP_VERSION="1.0.0"
MAIN_CLASS="com.example.mdreader.Main"
MAIN_JAR="md-reader-0.1.0-SNAPSHOT.jar"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/target"
PACKAGE_INPUT_DIR="$BUILD_DIR/package-input"
DIST_DIR="$ROOT_DIR/dist"

if ! command -v mvn >/dev/null 2>&1; then
  echo "未找到 mvn，请先安装 Maven。"
  exit 1
fi

if ! command -v jpackage >/dev/null 2>&1; then
  echo "未找到 jpackage，请先使用带 jpackage 的 JDK（建议 JDK 21）。"
  exit 1
fi

cd "$ROOT_DIR"

echo "==> 构建项目"
mvn -DskipTests clean package dependency:copy-dependencies

echo "==> 准备打包输入目录"
rm -rf "$PACKAGE_INPUT_DIR" "$DIST_DIR"
mkdir -p "$PACKAGE_INPUT_DIR" "$DIST_DIR"

cp "$BUILD_DIR/$MAIN_JAR" "$PACKAGE_INPUT_DIR/"
cp "$BUILD_DIR"/dependency/*.jar "$PACKAGE_INPUT_DIR/"

echo "==> 生成 .app"
jpackage \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input "$PACKAGE_INPUT_DIR" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --type app-image \
  --dest "$DIST_DIR"

echo "==> 生成 .dmg"
jpackage \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input "$PACKAGE_INPUT_DIR" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --type dmg \
  --dest "$DIST_DIR"

echo "打包完成，产物目录：$DIST_DIR"
