-target 1.8
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata
-dontpreverify
-verbose

-keep class org.jetbrains.kotlin.swiftexport.standalone.** { *; }

# FIXME: ??? These bring :kotlin-scripting-compiler-embeddable and :kotlin-assignment-compiler-plugin.embeddable ???
#-dontwarn org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.FirScriptingCompilerExtensionIdeRegistrar
#-dontwarn org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirAbstractSessionFactory
#-dontwarn org.jetbrains.kotlin.analysis.api.fir.components.KaFirPsiTypeProvider