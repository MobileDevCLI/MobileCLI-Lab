#!/bin/sh
# Gradle Wrapper Script - downloads and runs the correct Gradle version
APP_NAME="Gradle"
GRADLE_HOME="${HOME}/.gradle"
GRADLE_VERSION="8.4"
GRADLE_DIST="gradle-${GRADLE_VERSION}"
GRADLE_ZIP="${GRADLE_DIST}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/${GRADLE_ZIP}"
GRADLE_DIR="${GRADLE_HOME}/wrapper/dists/${GRADLE_DIST}"
GRADLE_BIN="${GRADLE_DIR}/bin/gradle"

# Save current directory
PROJECT_DIR="$(pwd)"

# Create directories
mkdir -p "${GRADLE_HOME}/wrapper/dists"

# Download if needed
if [ ! -x "${GRADLE_BIN}" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    DOWNLOAD_DIR="${GRADLE_HOME}/wrapper/dists"
    cd "${DOWNLOAD_DIR}"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL -o "${GRADLE_ZIP}" "${GRADLE_URL}"
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "${GRADLE_ZIP}" "${GRADLE_URL}"
    else
        echo "Error: curl or wget required"
        exit 1
    fi
    unzip -q "${GRADLE_ZIP}"
    rm "${GRADLE_ZIP}"
    cd "${PROJECT_DIR}"
fi

# Run Gradle from project directory
cd "${PROJECT_DIR}"
exec "${GRADLE_BIN}" "$@"
