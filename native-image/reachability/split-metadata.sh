#!/bin/bash
#
# Splits a unified reachability-metadata.json into separate
# GraalVM native-image configuration files.
#
# Usage: splitMetadata.sh <input.json> <output-dir>

set -euo pipefail

INPUT="$1"
OUTPUT_DIR="$2"

mkdir -p "$OUTPUT_DIR"

python3 - "$INPUT" "$OUTPUT_DIR" <<'PYEOF'
import json, sys, os, re

input_path = sys.argv[1]
output_dir = sys.argv[2]

with open(input_path) as f:
    data = json.load(f)


def to_jvm_type_name(name):
    """Convert 'Foo[]' to '[LFoo;' JVM array type descriptor."""
    if name.endswith("[]"):
        return "[L" + name[:-2] + ";"
    return name


def glob_to_resource_pattern(glob_str):
    """Convert a glob string to a \\Q...\\E quoted regex pattern."""
    return "\\Q" + glob_str + "\\E"


reflection_entries = []
proxy_entries = []
jni_entries = []

for entry in data.get("reflection", []):
    t = entry.get("type")

    # Non-string type entries (proxy, lambda, etc.)
    if isinstance(t, dict):
        if "proxy" in t:
            proxy_entries.append({"interfaces": t["proxy"]})
        # Skip lambda and other non-standard entries
        continue

    # JNI entries: have "jniAccessible" flag
    if entry.get("jniAccessible"):
        e = dict(entry)
        e["name"] = to_jvm_type_name(e.pop("type"))
        del e["jniAccessible"]
        jni_entries.append(e)
        continue

    # Regular reflection entry
    e = dict(entry)
    if "type" in e:
        e["name"] = to_jvm_type_name(e.pop("type"))
    reflection_entries.append(e)

# Resources: convert {"glob": "..."} to {"resources": {"includes": [{"pattern": "\\Q...\\E"}]}}
resource_includes = []
for entry in data.get("resources", []):
    if "glob" in entry:
        resource_includes.append({"pattern": glob_to_resource_pattern(entry["glob"])})
resource_config = {"resources": {"includes": resource_includes}}

# Pick up any top-level proxy/jni sections too
for p in data.get("proxy", []):
    if isinstance(p, list):
        proxy_entries.append({"interfaces": p})
    elif isinstance(p, dict):
        proxy_entries.append(p)

jni_entries.extend(data.get("jni", []))

serialization_config = {"types": [], "lambdaCapturingTypes": [], "proxies": []}
predefined_classes_config = [{"type": "agent-extracted", "classes": []}]

outputs = {
    "reflect-config.json": reflection_entries,
    "resource-config.json": resource_config,
    "jni-config.json": jni_entries,
    "proxy-config.json": proxy_entries,
    "serialization-config.json": serialization_config,
    "predefined-classes-config.json": predefined_classes_config,
}

for filename, content in outputs.items():
    path = os.path.join(output_dir, filename)
    with open(path, "w") as out:
        json.dump(content, out, indent=2)
    if isinstance(content, list):
        print(f"Written {path} ({len(content)} entries)")
    else:
        print(f"Written {path}")
PYEOF
