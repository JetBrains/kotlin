-target 1.8
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata
-keep class kotlin.Metadata
# -dontshrink

-keepdirectories META-INF/**

-dontnote **
-dontwarn org.jetbrains.kotlin.**
-dontwarn org.apache.commons.**
-dontwarn org.eclipse.sisu.**
-dontwarn org.checkerframework.**
-dontwarn afu.org.checkerframework.**
-dontwarn org.sonatype.plexus.components.**
-dontwarn org.codehaus.plexus.PlexusTestCase
-dontwarn javax.enterprise.inject.**
-dontwarn kotlin.annotations.jvm.**
# hopefully temporarily, for coroutines
-dontwarn kotlin.time.**

-keep class org.jetbrains.kotlin.mainKts.** { *; }
-keep class kotlin.script.experimental.** { *; }
-keep class org.apache.ivy.plugins.** { *; }

-keep class org.eclipse.sisu.** { *; }
-keep class org.jetbrains.kotlin.org.eclipse.sisu.** { *; }

-keep class com.google.inject.** { *; }
-keep class org.jetbrains.kotlin.com.google.inject.** { *; }

-keep class com.google.common.** { *; }
-keep class org.jetbrains.kotlin.com.google.common.** { *; }

-keep class org.apache.maven.wagon.providers.** { *; }
-keep class org.jetbrains.kotlin.org.apache.maven.wagon.providers.** { *; }

-keep class org.slf4j.** { *; }
-keep class org.jetbrains.kotlin.org.slf4j.** { *; }

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    **[] values();
}
