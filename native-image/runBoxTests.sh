#!/bin/bash

NATIVE_IMAGE_BIN="./kotlinc-native"
TESTS_DIR="compiler/testData/codegen/boxJvm"
NATIVE_IMAGE_ARGS="-Djava.home=$JAVA_HOME -Dkotlin.home=dist/kotlinc"
STOP_ON_FAILURE=false

for arg in "$@"; do
  case $arg in
    --nativeImage=*)
      NATIVE_IMAGE_BIN="${arg#*=}"
      ;;
    --testsDir=*)
      TESTS_DIR="${arg#*=}"
      ;;
    --nativeImageArgs=*)
      NATIVE_IMAGE_ARGS="${arg#*=}"
      ;;
    --stopOnFailure)
      STOP_ON_FAILURE=true
      ;;
    *)
      echo "Unknown argument: $arg"
      echo "Usage: $0 [--nativeImage=PATH] [--testsDir=PATH] [--nativeImageArgs=ARGS] [--stopOnFailure]"
      exit 1
      ;;
  esac
done

TMP_DIR=$(mktemp -d)
COMPILER_LOG=$(mktemp)
OUT_DIR=$(mktemp -d)
trap 'rm -rf $TMP_DIR $COMPILER_LOG $OUT_DIR' EXIT

KOTLIN_TEST_JAR="dist/kotlinc/lib/kotlin-test.jar"

PASSED=0
FAILED=0
SKIPPED=0

for file in $(find $TESTS_DIR -name "*.kt"); do
  # --- Skip non-JVM tests ---
  if grep -qE '// TARGET_BACKEND: (JS|JS_IR|JS_IR_ES6|WASM|NATIVE)$' "$file"; then
    continue
  fi
  if grep -q '// DONT_TARGET_EXACT_BACKEND: JVM_IR$' "$file"; then
    continue
  fi

  # --- Skip tests requiring expect/actual (multiplatform) ---
  if grep -q "^// LANGUAGE:.*+MultiPlatformProjects" "$file"; then
    continue
  fi

  # --- Skip tests that import helpers (test infra dependency not available) ---
  if grep -q '^import helpers\.' "$file" || grep -q '^import helpers\.\*' "$file"; then
    continue
  fi

  echo "--- Running $file ---"

  # --- Parse LANGUAGE directives into -XXLanguage: flags ---
  LANGUAGE_FLAGS=""
  LANGUAGE_LINE=$(grep '^// LANGUAGE:' "$file" | head -1)
  if [ -n "$LANGUAGE_LINE" ]; then
    # Extract +Feature and -Feature tokens
    LANGUAGE_FLAGS=$(echo "$LANGUAGE_LINE" | sed 's|// LANGUAGE:||' | tr ',' '\n' | sed 's/^ *//;s/ *$//' | while read feature; do
      if [ -n "$feature" ]; then
        echo "-XXLanguage:$feature"
      fi
    done | tr '\n' ' ')
  fi

  # --- Build extra classpath for compilation ---
  EXTRA_CP=""
  if grep -q 'import kotlin\.test' "$file"; then
    EXTRA_CP="-cp $KOTLIN_TEST_JAR"
  fi

  # --- Handle multi-file tests (// FILE: markers) ---
  if grep -q '^// FILE:' "$file"; then
    # Split into separate files
    rm -rf "${TMP_DIR:?}"/*
    current_file=""
    while IFS= read -r line; do
      if echo "$line" | grep -qE '^// FILE: '; then
        current_file=$(echo "$line" | sed 's|^// FILE: ||')
        mkdir -p "$TMP_DIR/$(dirname "$current_file")"
        : > "$TMP_DIR/$current_file"
      elif [ -n "$current_file" ]; then
        echo "$line" >> "$TMP_DIR/$current_file"
      fi
    done < "$file"

    # Find the file containing fun box() and append main
    BOX_FILE=$(grep -rl 'fun box()' "$TMP_DIR" | head -1)
    if [ -z "$BOX_FILE" ]; then
      SKIPPED=$((SKIPPED + 1))
      continue
    fi
    printf '\n fun main() { println(box()) } \n' >> "$BOX_FILE"

    # Collect all .kt files
    SOURCE_FILES=$(find "$TMP_DIR" -name "*.kt" | tr '\n' ' ')

    $NATIVE_IMAGE_BIN $NATIVE_IMAGE_ARGS $EXTRA_CP $LANGUAGE_FLAGS -d "$OUT_DIR" $SOURCE_FILES &> $COMPILER_LOG
  else
    # --- Single-file test ---
    TMP_FILE="$TMP_DIR/blackBox.kt"
    cat "$file" > "$TMP_FILE"
    printf '\n fun main() { println(box()) } \n' >> "$TMP_FILE"

    $NATIVE_IMAGE_BIN $NATIVE_IMAGE_ARGS $EXTRA_CP $LANGUAGE_FLAGS -d "$OUT_DIR" "$TMP_FILE" &> $COMPILER_LOG
  fi

  if [ $? -ne 0 ]; then
    echo "COMPILATION FAILED: $file"
    cat $COMPILER_LOG
    SKIPPED=$((SKIPPED + 1))
    if [ "$STOP_ON_FAILURE" = true ]; then break; fi
    rm -rf "${OUT_DIR:?}"/*
    continue
  fi

  # --- Detect package and class name to build qualified main class ---
  if grep -q '^// FILE:' "$file"; then
    BOX_SOURCE="$BOX_FILE"
  else
    BOX_SOURCE="$TMP_FILE"
  fi

  PACKAGE=$(grep '^package ' "$BOX_SOURCE" | head -1 | sed 's/package //;s/[[:space:]]//g')
  # Class name is derived from the source filename: foo.kt -> FooKt, 1.kt -> _1Kt
  BOX_BASENAME=$(basename "$BOX_SOURCE" .kt)
  FIRST_CHAR="${BOX_BASENAME:0:1}"
  if echo "$FIRST_CHAR" | grep -q '[0-9]'; then
    CLASS_NAME="_${BOX_BASENAME}Kt"
  else
    CLASS_NAME="$(echo "$FIRST_CHAR" | tr '[:lower:]' '[:upper:]')${BOX_BASENAME:1}Kt"
  fi
  if [ -n "$PACKAGE" ]; then
    MAIN_CLASS="${PACKAGE}.${CLASS_NAME}"
  else
    MAIN_CLASS="$CLASS_NAME"
  fi

  RESULT=$(java -cp "$OUT_DIR:dist/kotlinc/lib/*" "$MAIN_CLASS" 2>&1)
  if [ "$RESULT" = "OK" ]; then
    echo "PASSED: $file"
    PASSED=$((PASSED + 1))
  else
    echo "FAILED: $file (expected 'OK', got '$RESULT')"
    FAILED=$((FAILED + 1))
    if [ "$STOP_ON_FAILURE" = true ]; then break; fi
  fi

  rm -rf "${OUT_DIR:?}"/*
done

echo ""
echo "=== Summary: $PASSED passed, $FAILED failed, $SKIPPED skipped ==="
if [ $FAILED -gt 0 ] || [ $SKIPPED -gt 0 ]; then
  exit 1
fi
