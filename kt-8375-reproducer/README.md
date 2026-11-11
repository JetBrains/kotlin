# KT-8375 reproducer

**Issue:** [@JvmName](https://youtrack.jetbrains.com/issue/KT-8375) on a function does **not** propagate to names of auto-generated classes / lambda methods it creates.

**Status on this repo's compiler (verified):** **still reproduces**

- Compiler used: `dist/kotlinc` built from this repository via `./gradlew :kotlin-compiler:distKotlinc`
- Version observed: `kotlinc-jvm 2.5.255-SNAPSHOT`

## Source

See [`src/Kt8375Reproducer.kt`](src/Kt8375Reproducer.kt):

```kotlin
@file:JvmName("_1Kt")

import kotlin.jvm.JvmSerializableLambda

@JvmName("jvmName")
fun kotlinName() = @JvmSerializableLambda {}

@JvmName("jvmName2")
fun kotlinName2() = {}
```

## How to build & inspect

### Option A — using the compiler dist from this monorepo (recommended)

From the Kotlin repository root:

```bash
# one-time: build compiler distribution (needs JDK on PATH / JAVA_HOME)
export JAVA_HOME=/path/to/jdk-21   # or 17
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :kotlin-compiler:distKotlinc

# run the reproducer
./kt-8375-reproducer/build.sh
```

### Option B — any kotlinc

```bash
export KOTLINC=/path/to/kotlinc
./kt-8375-reproducer/build.sh
```

### Manual equivalent

```bash
mkdir -p out
"$KOTLINC" src/Kt8375Reproducer.kt -d out
find out -name '*.class' | sort
javap -p -c out/_1Kt.class
javap -p -c out/_1Kt\$kotlinName\$1.class
```

## Observed results (this repo, 2.5.255-SNAPSHOT)

### Generated class files

| File | Role |
|------|------|
| `_1Kt.class` | File facade (`@file:JvmName("_1Kt")`) |
| `_1Kt$kotlinName$1.class` | Class-lambda for `kotlinName` / `@JvmSerializableLambda` |

### Case 1 — class-lambda (`@JvmSerializableLambda`)

| | Name |
|--|------|
| **Expected** | `_1Kt$jvmName$1` (use `@JvmName("jvmName")`) |
| **Actual** | `_1Kt$kotlinName$1` (Kotlin source name) |

Notes from bytecode:

- Method on facade is correctly renamed: `public static final Function0 jvmName()`
- Generated lambda class still embeds the **Kotlin** name: `_1Kt$kotlinName$1`
- `EnclosingMethod` points at `_1Kt.jvmName` (JVM name), but the **class name** still uses `kotlinName`

### Case 2 — plain indy lambda

| | Name |
|--|------|
| **Expected** | `jvmName2$lambda$0` (use `@JvmName("jvmName2")`) |
| **Actual** | `kotlinName2$lambda$0` (Kotlin source name) |

Notes from bytecode:

- Method on facade is correctly renamed: `public static final Function0 jvmName2()`
- Implementation is `invokedynamic` → bootstrap MH:
  - `private static final Unit kotlinName2$lambda$0()`
- Lambda **method** name uses the **Kotlin** name `kotlinName2`, not `jvmName2`

### Verdict

```
reproduces: yes

Case 1 (class-lambda):
  actual:   _1Kt$kotlinName$1.class
  expected: _1Kt$jvmName$1.class

Case 2 (indy lambda):
  actual:   _1Kt.kotlinName2$lambda$0
  expected: _1Kt.jvmName2$lambda$0
```

## Why this matters

Stack traces, ProGuard/R8 keep rules, and tooling that key off synthetic class/method names see the Kotlin source name even when the callable’s JVM name was explicitly overridden with `@JvmName`.
