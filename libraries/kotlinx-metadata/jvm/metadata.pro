-target 1.8
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata
-keep class kotlin.Metadata
# -dontshrink

-dontnote **

-keepdirectories META-INF/**

-keep public class kotlin.metadata.* { public protected *; }
-keep public class kotlin.metadata.jvm.* { public protected *; }
-keep class kotlin.metadata.jvm.internal.JvmMetadataExtensions

# Used to load .kotlin_builtins files, in kotlinx-reflect-lite and will be used in kotlin-reflect after KT-75463
-keep class kotlin.metadata.internal.common.* { *; }

# Required for protobuf java lite mode: https://github.com/protocolbuffers/protobuf/issues/6463
-keepclassmembers class * extends org.jetbrains.kotlin.protobuf.AbstractMessageLite { <fields>; }