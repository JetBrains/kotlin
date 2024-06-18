-target 1.8
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata
-dontpreverify
-verbose

# Ignore classpath duplication from JVM
-dontnote sun.**

# These are IDE specific and we probably don't care
-dontwarn org.jetbrains.kotlin.com.intellij.openapi.util.Iconable$IconFlags
-dontwarn org.jetbrains.kotlin.com.intellij.openapi.util.NlsSafe

# Keep everything from Swift Export standalone
-keep public class org.jetbrains.kotlin.swiftexport.standalone.** { public *; }