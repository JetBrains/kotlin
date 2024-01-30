-target 11
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata

-keep class kotlin.Metadata

-dontnote **

-keep public class !org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport,org.jetbrains.kotlin.backend.konan.objcexport.** { public *; }
-keep public class org.jetbrains.kotlin.backend.konan.ObjC* { public *; }
-keep public class org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArg* { public *; }
-keep public class org.jetbrains.kotlin.backend.konan.KonanConfig* { public *; }
-keep public class org.jetbrains.kotlin.backend.konan.SetupConfig* { public *; }
-keep public class org.jetbrains.kotlin.backend.konan.BinaryOpt* { public *; }
-keep public class org.jetbrains.kotlin.backend.konan.Depend* { public *; }

# legacy
-keep public class org.jetbrains.kotlin.cli.bc.K2NativeCompilerArg* { public *; }
-keep public class org.jetbrains.kotlin.cli.bc.K2NativeKt { public *** parse*(...); }
-keep public class org.jetbrains.kotlin.cli.bc.BinaryOpt* { public *; }