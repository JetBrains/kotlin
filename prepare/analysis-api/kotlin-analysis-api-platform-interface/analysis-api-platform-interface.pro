-target 11
-dontoptimize
-dontobfuscate
-dontshrink
-dontprocesskotlinmetadata
-dontpreverify

# Keep everything — we only want the classpath completeness check
-keep class ** { *; }

-dontnote **

# The IntelliJ API components are not complete on their own: they rely on the IntelliJ implementation components
# (util-rt, jdom, coroutines, fastutil, ...) and their libraries, which 'kotlin-analysis-api-syntax-implementation'
# ships. References from and to them are ignored, so the check covers the Kotlin classes of the artifact only.
-dontwarn com.intellij.**
-dontwarn org.jdom.**
-dontwarn kotlinx.**
-dontwarn it.unimi.dsi.fastutil.**
-dontwarn com.google.common.**
-dontwarn org.codehaus.stax2.**
-dontwarn com.fasterxml.aalto.**
-dontwarn net.jpountz.lz4.**
-dontwarn org.apache.oro.**
-dontwarn io.opentelemetry.**

# The IntelliJ 'util' JAR bundles 'org.apache.log4j.Level' alone; the rest of log4j is an implementation component as well
-dontwarn org.apache.log4j.**
