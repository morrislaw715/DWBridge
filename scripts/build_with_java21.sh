#!/usr/bin/env bash
# Quick script to build the project with Java 21 on macOS (zsh/bash)
# Usage: ./scripts/build_with_java21.sh
# It will try to set JAVA_HOME to Java 21 using /usr/libexec/java_home and then run Gradle assembleDebug

set -euo pipefail

# Find Java 21 installation
JAVA_HOME_21=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
if [[ -z "$JAVA_HOME_21" ]]; then
  echo "Java 21 not found via /usr/libexec/java_home -v 21"
  echo "Please install Java 21 (e.g., Temurin 21) or set org.gradle.java.home in gradle.properties"
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_21"
echo "Using JAVA_HOME=$JAVA_HOME"

# Run Gradle assembleDebug
./gradlew assembleDebug

echo "Build finished. APK at app/build/outputs/apk/debug/app-debug.apk"
