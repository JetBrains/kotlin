-target 1.8
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata
-keep class kotlin.Metadata
# -dontshrink

-keepdirectories META-INF/**

-dontnote **

-keep class @org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
-keep @org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader class *
-keepclassmembers class * {
    @org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader *;
}

-keep class org.slf4j.** { *; }
-keep class org.jetbrains.kotlin.org.slf4j.** { *; }
