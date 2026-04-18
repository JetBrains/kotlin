import sys
import subprocess

file_path = "errors.txt"
if len(sys.argv) > 1:
    file_path = sys.argv[1]

try:
    with open(file_path, "r") as f:
        content = f.read()
except FileNotFoundError:
    print(f"Error: File not found at {file_path}")
    sys.exit(1)

prompt = (
    "You are an expert Kotlin compiler engineer. I have a list of categorized test failures with counts and test names. "
    "Some categories are very similar and should be grouped together (e.g., slight variations in wording for IR dump mismatches or silent CLI failures). "
    "Please analyze the list and group similar categories together. "
    "For each group, provide a new unified description, the total count of tests in that group, and the list of all tests.\n\n"
    "Crucially, for the list of tests in each group, maintain the format they have in the input (each test is on a new line, enclosed in single quotes and followed by a comma, without brackets `[` or `]`). "
    "Do not change the test names.\n"
    "Example:\n"
    "    'javaWildcardType',\n"
    "    'intersectionWithMappedSignature',\n\n"
    "Order the groups by total count descending.\n\n"
    "Here is the list of errors:\n"
    f"{content}\n\n"
    "Respond with the grouped results in this format."
)

print("Calling Gemini to group similar errors...")
try:
    result = subprocess.run(
        ["gemini", "--prompt", prompt], capture_output=True, text=True
    )
    print(result.stdout)
    print("\n=== Original Categorized Errors (from errors.txt) ===")
    print(content)
except Exception as e:
    print(f"Error calling gemini: {e}")
