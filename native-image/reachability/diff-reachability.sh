#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OLD_JSON="${1:-$SCRIPT_DIR/reachability-metadata-old.json}"
NEW_JSON="${2:-$SCRIPT_DIR/reachability-metadata-new.json}"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 1
fi

if [[ ! -f "$OLD_JSON" ]]; then
  echo "Missing file: $OLD_JSON" >&2
  exit 1
fi

if [[ ! -f "$NEW_JSON" ]]; then
  echo "Missing file: $NEW_JSON" >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

canonicalize_section() {
  local input_file="$1"
  local section="$2"
  local output_file="$3"
  jq -cS --arg section "$section" '.[$section][]' "$input_file" | LC_ALL=C sort > "$output_file"
}

count_lines() {
  local input_file="$1"
  wc -l < "$input_file" | tr -d ' '
}

show_json_stream() {
  local input_file="$1"
  if [[ ! -s "$input_file" ]]; then
    echo "[]"
  else
    jq -s '.' "$input_file"
  fi
}

show_section_diff() {
  local section="$1"
  local old_items="$TMP_DIR/${section}.old"
  local new_items="$TMP_DIR/${section}.new"
  local removed_items="$TMP_DIR/${section}.removed"
  local added_items="$TMP_DIR/${section}.added"

  canonicalize_section "$OLD_JSON" "$section" "$old_items"
  canonicalize_section "$NEW_JSON" "$section" "$new_items"

  comm -23 "$old_items" "$new_items" > "$removed_items"
  comm -13 "$old_items" "$new_items" > "$added_items"

  local old_count new_count removed_count added_count
  old_count="$(count_lines "$old_items")"
  new_count="$(count_lines "$new_items")"
  removed_count="$(count_lines "$removed_items")"
  added_count="$(count_lines "$added_items")"

  printf '%s\n' "== $section =="
  printf 'old: %s, new: %s, removed: %s, added: %s\n' \
    "$old_count" "$new_count" "$removed_count" "$added_count"
  printf '%s\n' "-- removed from old"
  show_json_stream "$removed_items"
  printf '%s\n' "-- added in new"
  show_json_stream "$added_items"
  printf '\n'
}

show_section_diff "reflection"
show_section_diff "resources"
