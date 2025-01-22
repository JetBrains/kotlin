-target 1.8
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata
-dontpreverify
-verbose

# Ignore classpath duplication from JVM
-dontnote sun.**

# These are IDE specific and shouldn't be reachable by Swift Export
-dontwarn org.jetbrains.kotlin.com.intellij.openapi.util.Iconable$IconFlags
-dontwarn org.jetbrains.kotlin.com.intellij.openapi.project.IndexNotReadyException
# These annotations are not retained at runtime, so these also shouldn't be reachable
-dontwarn org.jetbrains.kotlin.com.intellij.openapi.util.NlsSafe
-dontwarn org.jetbrains.kotlin.com.intellij.util.concurrency.annotations.RequiresReadLock
# See KT-73438
-dontwarn org.jetbrains.kotlin.com.intellij.util.concurrency.AppExecutorUtil

# Keep everything from Swift Export standalone
-keep public class org.jetbrains.kotlin.swiftexport.standalone.** { public *; }