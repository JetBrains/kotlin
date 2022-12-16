## Largely the same rules as in core/reflection.jvm/reflection.pro

-dontnote **

-target 1.8
-dontoptimize
-dontobfuscate
# -dontshrink

-keep public class kotlinx.metadata.* { *; }
-keep public class kotlinx.metadata.jvm.* { *; }
-keep public class kotlinx.metadata.jvm.internal.JvmMetadataExtensions { *; }
-keep public class kotlinx.metadata.internal.extensions.MetadataExtensions { *; }

-keepattributes SourceFile,LineNumberTable,InnerClasses,Signature,Deprecated,*Annotation*,EnclosingMethod

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * {
    ** toString();
}

# TODO: test if this proguard issue is still not fixed
# This is needed because otherwise ProGuard strips generic signature of this class (even though we pass `-keepattributes Signature` above)
# See KT-23962 and https://sourceforge.net/p/proguard/bugs/482/
-keep class kotlin.reflect.jvm.internal.impl.protobuf.GeneratedMessageLite$ExtendableMessageOrBuilder
