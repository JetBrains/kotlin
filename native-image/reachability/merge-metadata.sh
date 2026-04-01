#!/bin/bash
#
# Merges multiple reachability-metadata.json files into one.
# Entries are deduplicated and sorted by type name.
#
# Usage: mergeMetadata.sh <output.json> <file1.json> [file2.json ...]

set -euo pipefail

OUTPUT="$1"
shift
INPUT_FILES=("$@")

python3 - "$OUTPUT" "${INPUT_FILES[@]}" <<'PYEOF'
import json, sys

output = sys.argv[1]
input_files = sys.argv[2:]


def entry_key(entry):
    """Stable key for deduplication and sorting."""
    # reflection/jni entries use "type", resource entries use "glob"
    t = entry.get("type", entry.get("glob", ""))
    if isinstance(t, dict):
        return json.dumps(t, sort_keys=True)
    return t


def merge_entries(all_data, key):
    """Merge arrays from all files, dedup by type, sort by type name."""
    seen = {}
    for data in all_data:
        for entry in data.get(key, []):
            k = entry_key(entry)
            if k in seen:
                existing = seen[k]
                for attr in ("fields", "methods"):
                    if attr in entry:
                        existing_items = {json.dumps(x, sort_keys=True) for x in existing.get(attr, [])}
                        merged = list(existing.get(attr, []))
                        for item in entry[attr]:
                            if json.dumps(item, sort_keys=True) not in existing_items:
                                merged.append(item)
                        existing[attr] = merged
                for attr in ("jniAccessible", "allDeclaredFields", "allDeclaredMethods",
                             "allPublicFields", "allPublicMethods", "allDeclaredConstructors",
                             "allPublicConstructors", "unsafeAllocated"):
                    if entry.get(attr):
                        existing[attr] = True
            else:
                seen[k] = dict(entry)

    result = list(seen.values())
    result.sort(key=lambda e: entry_key(e))
    return result


all_data = []
for path in input_files:
    with open(path) as f:
        all_data.append(json.load(f))

all_keys = set()
for data in all_data:
    all_keys.update(data.keys())

merged = {}
for key in sorted(all_keys):
    merged[key] = merge_entries(all_data, key)

with open(output, "w") as f:
    json.dump(merged, f, indent=2)

total = sum(len(v) for v in merged.values())
print(f"Merged {len(input_files)} files into {output} ({total} total entries across {len(merged)} sections)")
PYEOF
