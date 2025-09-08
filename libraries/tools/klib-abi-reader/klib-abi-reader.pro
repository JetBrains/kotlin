-target 1.8
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata
-keep class kotlin.Metadata
# -dontshrink

-keepdirectories META-INF/**

-dontnote **

-keep class org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
-keep @org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader class * {
    public protected *;
}
-keepclassmembers class * {
    @org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader *;
}

-keep class org.slf4j.** { *; }
-keep class org.jetbrains.kotlin.org.slf4j.** { *; }


# There are a bunch of compiler fragments and libraries that are referenced, and which themself have dependencies that are missing at
# compile time, but are not required for the purpose of this library (reading declarations from klibs). This could be avoided by a proper
# separation of modules within the compiler, but that is feasible at the moment... we have quite a mess. OOTH, supplying the complete
# runtime classpath expected by the compiler to ProGuard is also challenging, as well as redundant, in that we need only a small portion
# of its functionality and everything else would be stripped anyway. Hence, here we suppress warnings coming from such code in hope it
# is not actually needed at runtime.
-dontwarn org.jetbrains.kotlin.psi.**
-dontwarn org.jetbrains.kotlin.kdoc.psi.**
-dontwarn org.jetbrains.kotlin.type.MapPsiToAsmDesc
-dontwarn org.jetbrains.kotlin.lexer.**
-dontwarn org.jetbrains.kotlin.parsing.**
-dontwarn org.jetbrains.kotlin.diagnostics.PositioningStrategies*
-dontwarn org.jetbrains.kotlin.library.metadata.impl.**
-dontwarn org.jetbrains.kotlin.backend.common.serialization.metadata.**
-dontwarn com.intellij.**
-dontwarn org.jetbrains.kotlin.com.google.common.hash.**
-dontwarn org.jetbrains.kotlin.org.apache.log4j.**
