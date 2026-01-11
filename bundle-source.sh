#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI Source Bundler
# Creates a source archive to include in APK for self-modification

set -e

# Get absolute path
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SOURCE_DIR="$SCRIPT_DIR"
OUTPUT_DIR="$SOURCE_DIR/app/src/main/assets"
OUTPUT_FILE="$OUTPUT_DIR/mobilecli-source.tar.gz"

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "=== MobileCLI Source Bundler ==="
echo "Source: $SOURCE_DIR"
echo "Output: $OUTPUT_FILE"
echo ""

# Create temporary directory for clean source
TEMP_DIR=$(mktemp -d)
CLEAN_SOURCE="$TEMP_DIR/MobileCLI-source"
mkdir -p "$CLEAN_SOURCE"

echo "Copying source files..."

# Copy essential files only (no build artifacts, no secrets)
cp -r "$SOURCE_DIR/app" "$CLEAN_SOURCE/"
cp -r "$SOURCE_DIR/engine-core" "$CLEAN_SOURCE/" 2>/dev/null || true
cp -r "$SOURCE_DIR/engine-terminal" "$CLEAN_SOURCE/" 2>/dev/null || true
cp -r "$SOURCE_DIR/engine-intents" "$CLEAN_SOURCE/" 2>/dev/null || true
cp -r "$SOURCE_DIR/engine-packages" "$CLEAN_SOURCE/" 2>/dev/null || true
cp -r "$SOURCE_DIR/engine-ai" "$CLEAN_SOURCE/" 2>/dev/null || true
cp -r "$SOURCE_DIR/engine-selfmod" "$CLEAN_SOURCE/" 2>/dev/null || true
cp -r "$SOURCE_DIR/gradle" "$CLEAN_SOURCE/"
cp "$SOURCE_DIR/build.gradle.kts" "$CLEAN_SOURCE/"
cp "$SOURCE_DIR/settings.gradle.kts" "$CLEAN_SOURCE/"
cp "$SOURCE_DIR/gradle.properties" "$CLEAN_SOURCE/"
cp "$SOURCE_DIR/gradlew" "$CLEAN_SOURCE/"
cp "$SOURCE_DIR/.gitignore" "$CLEAN_SOURCE/" 2>/dev/null || true

# Remove build artifacts from copied source
find "$CLEAN_SOURCE" -name "build" -type d -exec rm -rf {} + 2>/dev/null || true
find "$CLEAN_SOURCE" -name ".gradle" -type d -exec rm -rf {} + 2>/dev/null || true
find "$CLEAN_SOURCE" -name "*.apk" -delete 2>/dev/null || true
find "$CLEAN_SOURCE" -name "*.keystore" -delete 2>/dev/null || true
find "$CLEAN_SOURCE" -name "local.properties" -delete 2>/dev/null || true

# Remove secrets and personal files
rm -f "$CLEAN_SOURCE/.git-credentials" 2>/dev/null || true
rm -rf "$CLEAN_SOURCE/.git" 2>/dev/null || true

# Create local.properties template
cat > "$CLEAN_SOURCE/local.properties.template" << 'EOF'
# MobileCLI Local Properties Template
# Copy this to local.properties and update paths

sdk.dir=/data/data/com.termux/files/home/android-sdk

# Optional: If you have a custom aapt2
# android.aapt2FromMavenOverride=/path/to/aapt2
EOF

echo "Creating archive..."
cd "$TEMP_DIR"
tar -czf source.tar.gz MobileCLI-source
mv source.tar.gz "$OUTPUT_FILE"

# Cleanup
rm -rf "$TEMP_DIR"

# Show result
SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
echo ""
echo "SUCCESS: Source bundled"
echo "  File: $OUTPUT_FILE"
echo "  Size: $SIZE"
echo ""
echo "This will be included in the APK."
echo "Users can extract with: extract-source.sh"
