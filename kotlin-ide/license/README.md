The Apache 2 license (given in full in [LICENSE.txt](LICENSE.txt)) applies to all code in this repository which is copyright
by JetBrains. The following sections of the repository contain third-party code, to which different licenses
may apply:

## Kotlin Plugin

The following modules contain third-party code and are incorporated into the Kotlin IntelliJ IDEA plugin:

 - Path: jvm-debugger/eval4j/src/org/jetbrains/eval4j/interpreterLoop.kt
     - License: BSD ([license/third-party/asm-license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

## Kotlin Test Data

The following source code is used for testing the Kotlin IntelliJ IDEA plugin and is not incorporated into
any distributions of the compiler, libraries or plugin:

 - Path: idea/testData/android/lintQuickfix/requiresApi/RequiresApi.java
      - License: Apache 2 ([license/third-party/aosp-license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: idea/testData/android/lint/IntRange.java
      - License: Apache 2 ([license/third-party/aosp-license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: idea/testData/android/lint/RequiresPermission.java
      - License: Apache 2 ([license/third-party/aosp-license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

[aosp]: third-party/aosp-license.txt
[asm]: third-party/asm-license.txt
[javaslang]: third-party/javaslang-license.txt
[kotlinx.collections.immutable]: third-party/kotlinx.collections.immutable-license.txt
