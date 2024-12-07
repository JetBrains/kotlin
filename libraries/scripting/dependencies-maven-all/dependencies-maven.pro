-target 1.6
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata
-keep class kotlin.Metadata
# -dontshrink

-keepdirectories META-INF/**

-dontnote **
-dontwarn org.jetbrains.kotlin.**
-dontwarn kotlin.script.experimental.**
-dontwarn junit.framework.**
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**
-dontwarn org.testng.**
-dontwarn org.osgi.**
-dontwarn javax.el.**
-dontwarn javax.crypto.**
-dontwarn javax.interceptor.**
-dontwarn org.eclipse.sisu.**
-dontwarn org.slf4j.**

-keep class kotlin.script.experimental.** { *; }

-keep class org.eclipse.sisu.** { *; }
-keep class org.jetbrains.kotlin.org.eclipse.sisu.** { *; }

-keep class com.google.inject.** { *; }
-keep class org.jetbrains.kotlin.com.google.inject.** { *; }

-keep class org.jetbrains.kotlin.script.util.impl.PathUtilKt { *; }

-keep class com.google.common.** { *; }
-keep class org.jetbrains.kotlin.com.google.common.** { *; }

-keep class org.apache.maven.wagon.providers.** { *; }
-keep class org.jetbrains.kotlin.org.apache.maven.wagon.providers.** { *; }

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    **[] values();
}