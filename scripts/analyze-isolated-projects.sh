#!/usr/bin/env bash
#
# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

set -euo pipefail

# Run Gradle with isolated projects and analyze the configuration-cache-report.
# Usage: ./scripts/analyze-isolated-projects.sh [--skip-build] [--report PATH]
#
# Options:
#   --skip-build    Skip the Gradle build and use an existing report
#   --report PATH   Path to an existing configuration-cache-report.html

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_DIR="$PROJECT_DIR/build/reports/isolated-projects-analysis"

SKIP_BUILD=false
REPORT_PATH=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-build) SKIP_BUILD=true; shift ;;
        --report) REPORT_PATH="$2"; shift 2 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

# Step 1: Run the build (unless skipped)
if [[ "$SKIP_BUILD" == false ]]; then
    echo "==> Running: ./gradlew compileAll -Dorg.gradle.unsafe.isolated-projects=true"
    BUILD_LOG="$PROJECT_DIR/build/isolated-projects-build.log"
    mkdir -p "$(dirname "$BUILD_LOG")"

    set +e
    "$PROJECT_DIR/gradlew" compileAll \
        -Dorg.gradle.unsafe.isolated-projects=true \
        2>&1 | tee "$BUILD_LOG"
    BUILD_EXIT=$?
    set -e

    echo ""
    echo "==> Build finished with exit code $BUILD_EXIT"

    # Step 2: Find the report URL in the output
    if [[ -z "$REPORT_PATH" ]]; then
        REPORT_URL=$(grep -oE 'file://[^ ]+configuration-cache-report\.html' "$BUILD_LOG" | tail -1 || true)
        if [[ -n "$REPORT_URL" ]]; then
            REPORT_PATH="${REPORT_URL#file://}"
            echo "==> Found report: $REPORT_PATH"
        else
            # Fallback: find the most recent configuration-cache-report.html
            REPORT_PATH=$(find "$PROJECT_DIR/build/reports/configuration-cache" -name "configuration-cache-report.html" -type f 2>/dev/null | xargs ls -t 2>/dev/null | head -1 || true)
            if [[ -n "$REPORT_PATH" && -f "$REPORT_PATH" ]]; then
                echo "==> Using most recent report: $REPORT_PATH"
            else
                echo "ERROR: Could not find configuration-cache-report."
                exit 1
            fi
        fi
    fi
else
    if [[ -z "$REPORT_PATH" ]]; then
        REPORT_PATH=$(find "$PROJECT_DIR/build/reports/configuration-cache" -name "configuration-cache-report.html" -type f 2>/dev/null | xargs ls -t 2>/dev/null | head -1 || true)
        if [[ -z "$REPORT_PATH" ]]; then
            echo "ERROR: No configuration-cache-report.html found under build/reports/configuration-cache/"
            exit 1
        fi
    fi
    echo "==> Skipping build, using report: $REPORT_PATH"
fi

if [[ ! -f "$REPORT_PATH" ]]; then
    echo "ERROR: Report file not found: $REPORT_PATH"
    exit 1
fi

# Step 3: Extract JSON, filter problems, generate markdown reports
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

python3 - "$REPORT_PATH" "$OUTPUT_DIR" <<'PYEOF'
import json
import re
import sys
import os
from collections import defaultdict

report_path = sys.argv[1]
output_dir = sys.argv[2]

# --- Extract JSON from HTML ---
with open(report_path, 'r') as f:
    content = f.read()

match = re.search(r'// begin-report-data\n(.*?)\n// end-report-data', content, re.DOTALL)
if not match:
    print("ERROR: Could not find report data in HTML")
    sys.exit(1)

data = json.loads(match.group(1))
diagnostics = data.get('diagnostics', [])

total_problem_count = data.get('totalProblemCount', '?')
build_name = data.get('buildName', 'N/A')
requested_tasks = data.get('requestedTasks', 'N/A')
cache_action = data.get('cacheAction', 'N/A')

# Diagnostics come in two flavors:
#   - "problem" entries: have 'problem' and 'error' keys (isolation violations)
#   - "input" entries: have 'input' key (configuration inputs / undeclared reads)
# Both have 'trace' for location.

problem_diags = [d for d in diagnostics if 'problem' in d]
input_diags = [d for d in diagnostics if 'input' in d]

print(f"==> Report: {report_path}")
print(f"==> Total diagnostics: {len(diagnostics)} (totalProblemCount: {total_problem_count})")
print(f"==> Problem entries (isolation violations): {len(problem_diags)}")
print(f"==> Input entries (configuration inputs): {len(input_diags)}")


# ============================================================
# Helpers
# ============================================================

def segments_to_text(segments):
    """Convert a list of {text:...} / {name:...} segments to plain text."""
    return ''.join(s.get('text', s.get('name', '')) for s in segments)


def segments_to_md(segments):
    """Convert segments to markdown: 'name' segments become `code`."""
    parts = []
    for s in segments:
        if 'name' in s:
            parts.append(f"`{s['name']}`")
        elif 'text' in s:
            parts.append(s['text'])
    return ''.join(parts)


def extract_trace_location(trace):
    """Extract project path and build-logic location from a trace list."""
    project = None
    location = None
    property_name = None
    for t in trace:
        kind = t.get('kind', '')
        if kind == 'Project':
            project = t.get('path')
        elif kind == 'BuildLogic':
            location = t.get('location')
        elif kind == 'BuildLogicClass':
            location = t.get('location', t.get('name'))
        elif kind == 'PropertyUsage':
            property_name = t.get('name')
    return project, location, property_name


def extract_error_location(error):
    """Extract a human-readable location from the error summary."""
    if not error:
        return None
    summary = error.get('summary', [])
    return segments_to_text(summary).strip()


def sanitize_filename(name):
    return re.sub(r'[^a-zA-Z0-9_-]', '_', name)[:80]


# ============================================================
# Parse problem diagnostics (isolation violations)
# ============================================================

def parse_problem(diag):
    problem_segments = diag.get('problem', [])
    problem_text = segments_to_text(problem_segments)
    problem_md = segments_to_md(problem_segments)
    trace = diag.get('trace', [])
    error = diag.get('error')

    project, build_location, prop_name = extract_trace_location(trace)
    error_location = extract_error_location(error)

    info = {
        'problem_text': problem_text,
        'problem_md': problem_md,
        'source_project': None,
        'target_project': None,
        'action': None,
        'group_key': None,
        'build_location': build_location,
        'error_location': error_location,
        'property_name': prop_name,
    }

    # Pattern 1: "Project <X> cannot <action> on another project <Y>"
    m = re.match(
        r'Project\s+(\S+)\s+cannot\s+(.*?)\s+on\s+another project\s+(\S+)',
        problem_text
    )
    if m:
        info['source_project'] = m.group(1)
        info['action'] = m.group(2).strip()
        info['target_project'] = m.group(3)
        info['group_key'] = _action_group(info['action'])
        return info

    # Pattern 2: "Project <X> cannot <action> in the parent project <Y>"
    m = re.match(
        r'Project\s+(\S+)\s+cannot\s+(.*?)\s+in the parent project\s+(\S+)',
        problem_text
    )
    if m:
        info['source_project'] = m.group(1)
        info['action'] = m.group(2).strip()
        info['target_project'] = m.group(3)
        info['group_key'] = _action_group(info['action'])
        return info

    # Pattern 3: "Project <X> cannot <action> on subprojects via <mechanism>"
    m = re.match(
        r'Project\s+(\S+)\s+cannot\s+(.*?)\s+on\s+subprojects\s+via\s+(\S+)',
        problem_text
    )
    if m:
        info['source_project'] = m.group(1)
        info['action'] = m.group(2).strip()
        info['target_project'] = f"(subprojects via {m.group(3)})"
        info['group_key'] = _action_group(info['action'])
        return info

    # Fallback
    info['source_project'] = project
    info['group_key'] = 'other problems'
    return info


def _action_group(action_text):
    m = re.search(r"access\s+(\S+)\s+functionality", action_text)
    if m:
        return f"cross-project access: {m.group(1)}"
    if 'dynamically look up a property' in action_text:
        return 'cross-project: dynamic property lookup'
    return f"cross-project: {action_text}"


# ============================================================
# Parse input diagnostics (configuration inputs)
# ============================================================

def parse_input(diag):
    input_segments = diag.get('input', [])
    input_text = segments_to_text(input_segments)
    input_md = segments_to_md(input_segments)
    trace = diag.get('trace', [])
    doc_link = diag.get('documentationLink')

    project, build_location, prop_name = extract_trace_location(trace)

    # Group key: extract the input type
    m = re.match(r'^(.*?)\s+\S+$', input_text)
    if m:
        group_key = m.group(1).strip()
    else:
        group_key = input_text[:40]

    return {
        'input_text': input_text,
        'input_md': input_md,
        'project': project,
        'build_location': build_location,
        'group_key': group_key,
        'documentation_link': doc_link,
    }


# ============================================================
# Process all diagnostics
# ============================================================

parsed_problems = [parse_problem(d) for d in problem_diags]
parsed_inputs = [parse_input(d) for d in input_diags]

# --- Group problems ---
problem_groups = defaultdict(list)
for p in parsed_problems:
    problem_groups[p['group_key']].append(p)
sorted_problem_groups = sorted(problem_groups.items(), key=lambda x: -len(x[1]))

# --- Group inputs ---
input_groups = defaultdict(list)
for inp in parsed_inputs:
    input_groups[inp['group_key']].append(inp)
sorted_input_groups = sorted(input_groups.items(), key=lambda x: -len(x[1]))


# ============================================================
# Generate markdown reports
# ============================================================

def write_problem_group_report(group_name, items, path):
    with open(path, 'w') as f:
        f.write(f"# {group_name}\n\n")
        f.write(f"**Count:** {len(items)}\n\n")

        by_pair = defaultdict(list)
        for item in items:
            src = item['source_project'] or '(unknown)'
            tgt = item['target_project'] or '(unknown)'
            by_pair[(src, tgt)].append(item)

        f.write("| Source Project | Target Project | Violation | Location |\n")
        f.write("|---|---|---|---|\n")
        for (src, tgt), pair_items in sorted(by_pair.items()):
            for item in pair_items:
                action = item['action'] or item['problem_text']
                loc = item['error_location'] or item['build_location'] or ''
                f.write(f"| `{src}` | `{tgt}` | {action} | {loc} |\n")
        f.write("\n")


def write_input_group_report(group_name, items, path):
    with open(path, 'w') as f:
        f.write(f"# {group_name}\n\n")
        f.write(f"**Count:** {len(items)}\n\n")

        by_input = defaultdict(list)
        for item in items:
            by_input[item['input_text']].append(item)

        f.write("| Input | Project | Location | Occurrences |\n")
        f.write("|---|---|---|---|\n")
        for input_text, group_items in sorted(by_input.items()):
            projects = sorted(set(i['project'] or '(unknown)' for i in group_items))
            locations = sorted(set(i['build_location'] or '(unknown)' for i in group_items))
            proj_col = ', '.join(f'`{p}`' for p in projects[:5])
            if len(projects) > 5:
                proj_col += f' ... (+{len(projects) - 5})'
            loc_col = locations[0] if locations else ''
            if len(locations) > 1:
                loc_col += f' ... (+{len(locations) - 1})'
            f.write(f"| {input_text} | {proj_col} | {loc_col} | {len(group_items)} |\n")
        f.write("\n")


# --- Write index ---
index_path = os.path.join(output_dir, "index.md")
with open(index_path, 'w') as idx:
    idx.write("# Isolated Projects - Configuration Cache Report Analysis\n\n")
    idx.write(f"**Build:** {build_name}\n")
    idx.write(f"**Tasks:** {requested_tasks}\n")
    idx.write(f"**Cache action:** {cache_action}\n")
    idx.write(f"**Total problem count:** {total_problem_count}\n")
    idx.write(f"**Problem entries (isolation violations):** {len(problem_diags)}\n")
    idx.write(f"**Input entries (configuration inputs):** {len(input_diags)}\n\n")

    # --- Problem groups ---
    idx.write("## Isolation Violation Groups\n\n")
    if sorted_problem_groups:
        idx.write("| # | Group | Count | Report |\n")
        idx.write("|---|---|---|---|\n")
        for i, (group_name, items) in enumerate(sorted_problem_groups, 1):
            fname = f"problem-{i:02d}-{sanitize_filename(group_name)}.md"
            fpath = os.path.join(output_dir, fname)
            write_problem_group_report(group_name, items, fpath)
            idx.write(f"| {i} | {group_name} | {len(items)} | [{fname}]({fname}) |\n")
        idx.write("\n")
    else:
        idx.write("No isolation violations found.\n\n")

    # --- Offending projects summary (problems) ---
    idx.write("## Offending Projects (Isolation Violations)\n\n")
    project_violations = defaultdict(int)
    for p in parsed_problems:
        src = p['source_project']
        if src:
            project_violations[src] += 1
    if project_violations:
        idx.write("| Project | Violation Count |\n")
        idx.write("|---|---|\n")
        for proj, count in sorted(project_violations.items(), key=lambda x: -x[1]):
            idx.write(f"| `{proj}` | {count} |\n")
        idx.write("\n")
    else:
        idx.write("None.\n\n")

    # --- Projects with most input reads ---
    idx.write("## Projects by Configuration Input Count\n\n")
    project_inputs = defaultdict(int)
    for inp in parsed_inputs:
        proj = inp['project']
        if proj:
            project_inputs[proj] += 1
    if project_inputs:
        idx.write("| Project | Input Count |\n")
        idx.write("|---|---|\n")
        for proj, count in sorted(project_inputs.items(), key=lambda x: -x[1])[:50]:
            idx.write(f"| `{proj}` | {count} |\n")
        if len(project_inputs) > 50:
            idx.write(f"| ... | ({len(project_inputs) - 50} more projects) |\n")
        idx.write("\n")
    else:
        idx.write("None.\n\n")

print(f"\n==> Reports generated in: {output_dir}")
print(f"    Index: {index_path}")
print(f"    Problem groups: {len(sorted_problem_groups)}")
print(f"    Input groups: {len(sorted_input_groups)}")
PYEOF

echo ""
echo "==> Done! Reports are in: $OUTPUT_DIR"
echo "    Open: $OUTPUT_DIR/index.md"
