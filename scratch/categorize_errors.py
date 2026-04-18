import sys
from collections import defaultdict

file_path = "/Users/joseefort/Code/kotlin-worktrees/kotlin2/jklib_test_errors.md"

if len(sys.argv) > 1:
    file_path = sys.argv[1]

errors = defaultdict(list)
current_header = None
current_desc = []
in_desc = False

try:
    with open(file_path, "r") as f:
        for line in f:
            stripped = line.strip()
            if stripped.startswith("# ") and stripped.endswith("()"):
                if current_header and current_desc:
                    test_name = current_header.replace("# ", "").strip()
                    errors[" ".join(current_desc)].append(test_name)
                current_header = stripped
                current_desc = []
                in_desc = True
            elif in_desc:
                if stripped == "":
                    in_desc = False
                    if current_header and current_desc:
                        test_name = current_header.replace("# ", "").strip()
                        errors[" ".join(current_desc)].append(test_name)
                        current_header = None
                        current_desc = []
                else:
                    current_desc.append(stripped)
except FileNotFoundError:
    print(f"Error: File not found at {file_path}")
    sys.exit(1)

# Add the last one if still in desc
if current_header and current_desc:
    test_name = current_header.replace("# ", "").strip()
    errors[" ".join(current_desc)].append(test_name)

# Print results
print("Error Counts:")
sorted_errors = sorted(errors.items(), key=lambda item: len(item[1]), reverse=True)

def clean_test_name(name):
    clean = name.strip()
    if clean.startswith("test"):
        clean = clean[4:]
    if clean.endswith("()"):
        clean = clean[:-2]
    if clean:
        clean = clean[0].lower() + clean[1:]
    return clean

# Summary
for desc, tests in sorted_errors:
    print(f"{len(tests)}: {desc}")

print("\nDetails:")
for desc, tests in sorted_errors:
    print(f"{len(tests)}: {desc}")
    clean_tests = [clean_test_name(test) for test in tests]
    for test in clean_tests:
        print(f"'{test}',")
    print()
