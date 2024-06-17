-target 1.8
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata
-dontpreverify
-verbose

# Ignore classpath duplication from JVM
-dontnote sun.**

# Keep everything from Swift Export standalone
-keep class org.jetbrains.kotlin.swiftexport.standalone.** { *; }