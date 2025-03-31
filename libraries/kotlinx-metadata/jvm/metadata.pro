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
-keep class kotlin.metadata.internal.common.BuiltInsMetadataExtensions

# Required for protobuf java lite mode: https://github.com/protocolbuffers/protobuf/issues/6463
-keepclassmembers class * extends org.jetbrains.kotlin.protobuf.AbstractMessageLite { <fields>; }