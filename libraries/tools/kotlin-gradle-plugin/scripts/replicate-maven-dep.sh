#!/bin/bash
#
# Replicates a Maven dependency into the local mirror directory, preserving
# metadata (POM, Gradle Module Metadata) but replacing binary files (JARs,
# klibs, AARs) with zero-size stubs.
#
# Usage:
#   ./replicate-maven-dep.sh group:artifact:version [--with-transitives]
#
# Example:
#   ./replicate-maven-dep.sh com.arkivanov.mvikotlin:mvikotlin:3.0.2 --with-transitives
#
# Output goes to ../src/functionalTest/resources/mavenRepoMirror/

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESOURCES_DIR="$SCRIPT_DIR/../src/functionalTest/resources"
ARCHIVE="$RESOURCES_DIR/mavenRepoMirror.tar.gz"
MAVEN_BASE="https://cache-redirector.jetbrains.com/maven-central"
# OUTPUT_DIR is set after temp dir creation in main
# Empty valid ZIP (minimal end-of-central-directory record)
EMPTY_JAR_HEX="504b05060000000000000000000000000000"

WITH_TRANSITIVES=false
PROCESSED=()

usage() {
    echo "Usage: $0 group:artifact:version [--with-transitives]"
    echo ""
    echo "Replicates a Maven dependency preserving metadata but stubbing binaries."
    echo "Automatically extracts and re-compresses mavenRepoMirror.tar.gz."
    exit 1
}

# Check if coordinate was already processed (avoid infinite loops)
already_processed() {
    local coord="$1"
    for p in "${PROCESSED[@]+"${PROCESSED[@]}"}"; do
        if [ "$p" = "$coord" ]; then
            return 0
        fi
    done
    return 1
}

# Download a file from Maven Central, return 0 on success
download() {
    local url="$1"
    local dest="$2"
    local http_code
    http_code=$(curl -sL -o "$dest" -w "%{http_code}" "$url")
    if [ "$http_code" = "200" ]; then
        return 0
    else
        rm -f "$dest"
        return 1
    fi
}

# Create an empty stub file (valid empty ZIP for .jar, AAR with classes.jar for .aar, empty for .klib)
create_stub() {
    local dest="$1"
    local ext="${dest##*.}"
    if [ "$ext" = "aar" ]; then
        # AAR must contain classes.jar for AGP's artifact transform
        python3 -c "
import zipfile, io
buf = io.BytesIO()
with zipfile.ZipFile(buf, 'w') as outer:
    inner = io.BytesIO()
    with zipfile.ZipFile(inner, 'w') as jar:
        pass
    outer.writestr('classes.jar', inner.getvalue())
open('$dest', 'wb').write(buf.getvalue())
"
    elif [ "$ext" = "jar" ]; then
        echo "$EMPTY_JAR_HEX" | xxd -r -p > "$dest"
    else
        : > "$dest"
    fi
}

# Replicate a single artifact
replicate_artifact() {
    local group="$1"
    local artifact="$2"
    local version="$3"
    local coord="$group:$artifact:$version"

    if already_processed "$coord"; then
        return
    fi
    PROCESSED+=("$coord")

    local group_path="${group//\.//}"
    local base_url="$MAVEN_BASE/$group_path/$artifact/$version"
    local dest_dir="$OUTPUT_DIR/$group_path/$artifact/$version"

    mkdir -p "$dest_dir"

    echo "Replicating $coord ..."

    # Download POM
    if download "$base_url/$artifact-$version.pom" "$dest_dir/$artifact-$version.pom"; then
        echo "  POM ok"
    else
        echo "  POM not found (skipping)"
        return
    fi

    # Download Gradle Module Metadata (.module)
    if download "$base_url/$artifact-$version.module" "$dest_dir/$artifact-$version.module"; then
        echo "  .module ok"
    else
        echo "  .module not found (POM-only artifact)"
    fi

    # For each file referenced in the .module, create stubs (or download real metadata JARs)
    local module_file="$dest_dir/$artifact-$version.module"
    if [ -f "$module_file" ]; then
        # Extract file URLs and identify which are metadata JARs (need real content)
        local files
        files=$(python3 -c "
import json, sys
try:
    d = json.load(open('$module_file'))
    # Collect files from metadata variants (these need real downloads, not stubs)
    metadata_variants = {'metadataApiElements', 'commonMainMetadataElements',
                         'metadataApiElements-published', 'commonMainMetadataElements-published'}
    metadata_files = set()
    for v in d.get('variants', []):
        vname = v.get('name', '')
        is_metadata = vname in metadata_variants or 'MetadataElements' in vname
        for f in v.get('files', []):
            url = f.get('url', '')
            if url:
                if is_metadata and url.endswith('.jar'):
                    print('METADATA_JAR:' + url)
                    metadata_files.add(url)
                else:
                    print(url)
        # Follow available-at references
        aa = v.get('available-at', {})
        if aa:
            print('AVAILABLE_AT:' + aa.get('group','') + ':' + aa.get('module','') + ':' + aa.get('version',''))
except:
    pass
" 2>/dev/null || true)

        for f in $files; do
            if [[ "$f" == AVAILABLE_AT:* ]]; then
                # Parse available-at reference and replicate that sub-module
                local ref="${f#AVAILABLE_AT:}"
                local ref_group="${ref%%:*}"
                local ref_rest="${ref#*:}"
                local ref_artifact="${ref_rest%%:*}"
                local ref_version="${ref_rest#*:}"
                if [ -n "$ref_group" ] && [ -n "$ref_artifact" ] && [ -n "$ref_version" ]; then
                    replicate_artifact "$ref_group" "$ref_artifact" "$ref_version"
                fi
            elif [[ "$f" == METADATA_JAR:* ]]; then
                # Download real metadata JAR (IdeTransformedMetadataDependencyResolver reads its content)
                local meta_url="${f#METADATA_JAR:}"
                local meta_path="$dest_dir/$meta_url"
                if [ ! -f "$meta_path" ]; then
                    if download "$base_url/$meta_url" "$meta_path"; then
                        echo "  metadata jar: $meta_url (real)"
                    else
                        create_stub "$meta_path"
                        echo "  metadata jar: $meta_url (stub, download failed)"
                    fi
                fi
            elif [ -n "$f" ]; then
                # Create stub for referenced file
                local stub_path="$dest_dir/$f"
                if [ ! -f "$stub_path" ]; then
                    create_stub "$stub_path"
                    echo "  stub: $f"
                fi
            fi
        done
    fi

    # Also create a stub JAR if not already created (for POM-only artifacts)
    if [ ! -f "$dest_dir/$artifact-$version.jar" ]; then
        create_stub "$dest_dir/$artifact-$version.jar"
        echo "  stub: $artifact-$version.jar"
    fi

    # Create sources JAR stub (Gradle resolves -sources.jar via classifier convention)
    if [ ! -f "$dest_dir/$artifact-$version-sources.jar" ]; then
        create_stub "$dest_dir/$artifact-$version-sources.jar"
        echo "  stub: $artifact-$version-sources.jar"
    fi

    # If --with-transitives, parse POM and .module for dependencies
    if [ "$WITH_TRANSITIVES" = true ]; then
        local deps
        deps=$(python3 -c "
import json, sys, xml.etree.ElementTree as ET

deps = set()

# From .module file
try:
    d = json.load(open('$module_file'))
    for v in d.get('variants', []):
        for dep in v.get('dependencies', []):
            g = dep.get('group', '')
            m = dep.get('module', '')
            ver = dep.get('version', {}).get('requires', '') or dep.get('version', {}).get('prefers', '')
            if g and m and ver:
                deps.add(f'{g}:{m}:{ver}')
except:
    pass

# From POM file
try:
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    tree = ET.parse('$dest_dir/$artifact-$version.pom')
    root = tree.getroot()
    for dep in root.findall('.//m:dependency', ns) + root.findall('.//dependency'):
        g = (dep.find('m:groupId', ns) or dep.find('groupId'))
        a = (dep.find('m:artifactId', ns) or dep.find('artifactId'))
        v = (dep.find('m:version', ns) or dep.find('version'))
        if g is not None and a is not None and v is not None and g.text and a.text and v.text:
            deps.add(f'{g.text}:{a.text}:{v.text}')
except:
    pass

for d in sorted(deps):
    print(d)
" 2>/dev/null || true)

        for dep in $deps; do
            local dep_group="${dep%%:*}"
            local dep_rest="${dep#*:}"
            local dep_artifact="${dep_rest%%:*}"
            local dep_version="${dep_rest#*:}"
            replicate_artifact "$dep_group" "$dep_artifact" "$dep_version"
        done
    fi
}

# --- Main ---

if [ $# -lt 1 ]; then
    usage
fi

COORD="$1"
shift

while [ $# -gt 0 ]; do
    case "$1" in
        --with-transitives) WITH_TRANSITIVES=true ;;
        *) echo "Unknown option: $1"; usage ;;
    esac
    shift
done

# Parse coordinate
IFS=':' read -r GROUP ARTIFACT VERSION <<< "$COORD"
if [ -z "$GROUP" ] || [ -z "$ARTIFACT" ] || [ -z "$VERSION" ]; then
    echo "Error: Invalid coordinate '$COORD'. Expected format: group:artifact:version"
    exit 1
fi

WORK_DIR=$(mktemp -d)
OUTPUT_DIR="$WORK_DIR/mavenRepoMirror"
trap 'rm -rf "$WORK_DIR"' EXIT

# Extract existing archive
if [ -f "$ARCHIVE" ]; then
    echo "Extracting $ARCHIVE ..."
    tar xzf "$ARCHIVE" -C "$WORK_DIR"
fi

mkdir -p "$OUTPUT_DIR"

replicate_artifact "$GROUP" "$ARTIFACT" "$VERSION"

# Re-compress
echo ""
echo "Re-compressing archive ..."
tar czf "$ARCHIVE" -C "$WORK_DIR" mavenRepoMirror

echo "Done. Archive updated: $ARCHIVE"
