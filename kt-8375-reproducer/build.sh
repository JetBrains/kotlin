#!/usr/bin/env bash
# Build KT-8375 reproducer and dump generated class/method names.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"
SRC="$ROOT/src/Kt8375Reproducer.kt"

# Prefer compiler dist built from this Kotlin repo; allow override via KOTLINC.
if [[ -z "${KOTLINC:-}" ]]; then
  if [[ -x "$ROOT/../dist/kotlinc/bin/kotlinc" ]]; then
    KOTLINC="$ROOT/../dist/kotlinc/bin/kotlinc"
  elif command -v kotlinc >/dev/null 2>&1; then
    KOTLINC="$(command -v kotlinc)"
  else
    echo "error: kotlinc not found." >&2
    echo "Build the repo dist first:  (from repo root) ./gradlew :kotlin-compiler:distKotlinc" >&2
    echo "Or set KOTLINC=/path/to/kotlinc" >&2
    exit 1
  fi
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in \
    /usr/lib/jvm/amazon-corretto-21* \
    /usr/lib/jvm/java-21* \
    /usr/lib/jvm/jdk-21* \
    /usr/lib/jvm/amazon-corretto-17* \
    /usr/lib/jvm/java-17*; do
    if [[ -x "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

rm -rf "$OUT"
mkdir -p "$OUT"

echo "==> Compiler: $KOTLINC"
"$KOTLINC" -version 2>&1 || true
echo "==> Compiling $SRC -> $OUT"
"$KOTLINC" "$SRC" -d "$OUT"

echo
echo "=== Generated .class files ==="
find "$OUT" -name '*.class' | sort

echo
echo "=== javap -p (all classes) ==="
while IFS= read -r classfile; do
  echo
  echo "----- $classfile -----"
  javap -p -c "$classfile"
done < <(find "$OUT" -name '*.class' | sort)

echo
echo "=== Name check (KT-8375) ==="
CLASSES="$(find "$OUT" -name '*.class' -printf '%f\n' | sort | tr '\n' ' ')"
echo "Class files: $CLASSES"

if echo "$CLASSES" | grep -q 'kotlinName\$1'; then
  echo "Case 1 (class-lambda): REPRODUCES — found *kotlinName\$1* (expected *jvmName\$1*)"
else
  echo "Case 1 (class-lambda): does NOT match known failing pattern"
fi

if javap -p "$OUT/_1Kt.class" 2>/dev/null | grep -q 'kotlinName2\$lambda\$0'; then
  echo "Case 2 (indy lambda):  REPRODUCES — found kotlinName2\$lambda\$0 (expected jvmName2\$lambda\$0)"
else
  echo "Case 2 (indy lambda):  does NOT match known failing pattern"
fi

echo
echo "Done. Output directory: $OUT"
