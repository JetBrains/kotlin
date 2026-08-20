-dontoptimize
-dontobfuscate
-dontpreverify
-dontprocesskotlinmetadata

# Keep everything - we only want the classpath completeness check.
-keep class ** { *; }

-dontnote **

# Broken references are currently expected (e.g., while K1 is being removed).
# The warning output is compared verbatim against 'proguard-warnings.txt'.
#
# Only classes that are provably irrelevant to the IDE plugin may be suppressed with
# '-dontwarn' below (unrelated third-party libraries, backends that don't run in the IDE).
# Everything else must stay visible in the golden file to be reviewed.
-ignorewarnings

# The JS and WASM backends are not shipped to the IDE and never run there.
-dontwarn org.jetbrains.kotlin.backend.wasm.**
-dontwarn org.jetbrains.kotlin.ir.backend.js.**
-dontwarn org.jetbrains.kotlin.js.config.**
-dontwarn org.jetbrains.kotlin.js.parser.**
-dontwarn org.jetbrains.kotlin.wasm.ir.**

# Compiler plugins are shipped to the IDE as separate artifacts and are not checked here
# (the shipped artifacts mix K1 and K2 implementations).
-dontwarn org.jetbrains.kotlin.assignment.plugin.**
-dontwarn org.jetbrains.kotlin.powerassert.**

# Incremental compilation runners never run in the IDE context.
-dontwarn org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunnerBase

# The LightTree parser is deliberately excluded from the IDE artifacts (KT-86408);
# the dangling reference from 'fir:entrypoint' is a known, separate problem.
-dontwarn org.jetbrains.kotlin.fir.lightTree.**

# Some IntelliJ platform classes are duplicated in the compiler artifacts, splitting class
# hierarchies between the program and library classpaths; other platform classes are missing
# from the 'intellij-core' artifact surface (e.g., 'com.intellij.openapi.vfs.LocalFileSystem').
# Both are known problems, unrelated to the compiler artifact integrity checked here.
-dontwarn com.intellij.**

# ProGuard false positive: 'MethodHandle.invoke()' is a polymorphic signature method.
-dontwarn java.lang.invoke.MethodHandle

# Third-party libraries not bundled with the IDE plugin; only used on unrelated code paths.
-dontwarn io.vavr.**
-dontwarn org.fusesource.jansi.**
-dontwarn org.jline.**

# The IntelliJ platform bundles a different version of the library than the compiler
# is built against; the affected members are not used on IDE code paths.
-dontwarn kotlinx.collections.immutable.**

# Annotation-only dependencies; annotations are not resolved at runtime.
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn kotlin.annotations.jvm.**
