-target 1.8
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata
-keep class kotlin.Metadata
# -dontshrink

-keepdirectories META-INF/**

-dontnote **

-keep class org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
-keep @org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader class *
-keepclassmembers class * {
    @org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader *;
}

-keep class org.slf4j.** { *; }
-keep class org.jetbrains.kotlin.org.slf4j.** { *; }


#-dontwarn com.intellij.**
-dontwarn org.jetbrains.kotlin.io.opentelemetry.**
-dontwarn org.jetbrains.kotlin.org.jdom.**
-dontwarn org.jetbrains.annotations.**
-dontwarn org.jetbrains.kotlin.com.google.common.hash.**
-dontwarn org.jetbrains.org.objectweb.asm.**
-dontwarn org.jetbrains.kotlin.org.apache.log4j.**
-dontwarn org.jetbrains.kotlin.cli.**
-dontwarn org.jetbrains.kotlin.psi.**
-dontwarn org.jetbrains.kotlin.platform.**