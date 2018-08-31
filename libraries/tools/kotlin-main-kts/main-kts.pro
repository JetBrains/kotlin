-target 1.8
-dontoptimize
-dontobfuscate
# -dontshrink

-dontnote **
-dontwarn org.sonatype.aether.**
-dontwarn org.jetbrains.kotlin.**
-dontwarn org.apache.commons.**
-dontwarn com.jcraft.**
-dontwarn org.apache.tools.ant.**
-dontwarn org.apache.oro.text.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.ivy.ant.**
-dontwarn kotlin.annotations.jvm.**

-keep class org.jetbrains.kotlin.mainKts.** { *; }
-keep class kotlin.script.experimental.** { *; }
-keep class org.jetbrains.kotlin.script.util.impl.PathUtilKt { *; }

