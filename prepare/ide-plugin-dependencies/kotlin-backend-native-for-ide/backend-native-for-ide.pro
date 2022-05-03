-target 11
-dontoptimize
-dontobfuscate

-keepdirectories META-INF/**

-dontnote **
-dontwarn llvm.**
-dontwarn kotlin.reflect.full.KClasses

-keep public class !org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport,org.jetbrains.kotlin.backend.konan.objcexport.** { public *; }
-keep public class org.jetbrains.kotlin.backend.konan.ObjCInteropKt { public *; }
-keep public class org.jetbrains.kotlin.backend.konan.ObjCOverridabilityCondition { public *; }
-keep public class org.jetbrains.kotlin.cli.bc.K2NativeCompilerArguments* { public *; }
-keep public class org.jetbrains.kotlin.cli.bc.K2NativeKt { public *** parse*(...); }
-keep public class org.jetbrains.kotlin.cli.bc.BinaryOptionWithValue { public *; }
-keep public class org.jetbrains.kotlin.backend.konan.BinaryOptions { public *; }
