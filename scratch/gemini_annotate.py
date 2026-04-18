import os
import re
import subprocess
import sys

# This script processes a test error log and uses Gemini to add descriptions.
# It expects a file with '# testName()' headers followed by stack traces.

file_path = "jklib_test_errors.md"
out_path = "jklib_test_errors_gemini.md"

if len(sys.argv) > 1:
    file_path = sys.argv[1]
if len(sys.argv) > 2:
    out_path = sys.argv[2]

try:
    with open(file_path, "r") as f:
        content = f.read()
except FileNotFoundError:
    print(f"Error: File not found at {file_path}")
    sys.exit(1)

# Split by header
entries = re.split(r"\n(?=# )", content)
if entries and not entries[0].startswith("#"):
    entries.pop(0) # Remove intro if any

batch_size = 30

def process_batch(batch):
    prompt = (
        "You are an expert Kotlin compiler engineer analyzing test failure logs. "
        "For each test failure, provide a concise, human-readable error description of no more than 2 lines. "
        "Focus on identifying missing annotations (circular dependencies), IR/Kotlin text mismatches, or infrastructure issues.\n\n"
        "Here are the test failures:\n"
    )

    for entry in batch:
        lines = entry.split("\n")
        header = lines[0].strip()
        # Truncate stack trace to avoid huge prompts
        stack = "\n".join(lines[1:20]).strip()
        prompt += f"=== TEST: {header} ===\n{stack}\n\n"

    prompt += (
        "Respond ONLY in this format:\n"
        "=== RESULT: testName ===\n"
        "Description line 1\n"
        "Description line 2 (optional)\n"
        "...\n"
        "Do not include any other text or markdown."
    )

    print(f"Calling Gemini for batch of {len(batch)} entries...")
    try:
        result = subprocess.run(
            ["gemini", "--prompt", prompt], capture_output=True, text=True
        )
        return result.stdout
    except Exception as e:
        print(f"Error calling gemini: {e}")
        return ""

results = {}

# Process in batches
for i in range(0, len(entries), batch_size):
    batch = entries[i : i + batch_size]
    llm_output = process_batch(batch)

    # Parse results
    current_test = None
    current_desc = []
    for line in llm_output.split("\n"):
        if line.startswith("=== RESULT:"):
            if current_test and current_desc:
                results[current_test] = "\n".join(current_desc).strip()
            current_test = line.replace("=== RESULT:", "").replace("===", "").strip()
            current_desc = []
        elif current_test:
            current_desc.append(line)
    if current_test and current_desc:
        results[current_test] = "\n".join(current_desc).strip()

# Reconstruct file
updated_content = []
for entry in entries:
    lines = entry.split("\n")
    header = lines[0].strip()
    test_name = header.replace("# ", "").strip()

    updated_content.append(header)
    if test_name in results:
        updated_content.append(results[test_name])
        updated_content.append("")  # Blank line

    # Append the rest of the lines
    updated_content.extend(lines[1:])
    updated_content.append("")  # Blank line between entries

with open(out_path, "w") as f:
    f.write("\n".join(updated_content))

print(f"Wrote updated file to {out_path}")
