-target 11
-dontoptimize
-dontobfuscate
-dontshrink
-dontprocesskotlinmetadata
-dontpreverify

# Keep everything — we only want the classpath completeness check
-keep class ** { *; }

-dontnote **

# The IntelliJ API components are not complete on their own: they rely on the IntelliJ implementation components
# (util-rt, jdom, coroutines, fastutil, ...) and their libraries, which 'kotlin-analysis-api-syntax-implementation'
# ships. References from and to them are ignored, so the check covers the Kotlin classes of the artifact only.
-dontwarn com.intellij.**
-dontwarn org.jdom.**
-dontwarn kotlinx.**
-dontwarn it.unimi.dsi.fastutil.**
-dontwarn com.google.common.**
-dontwarn org.codehaus.stax2.**
-dontwarn com.fasterxml.aalto.**
-dontwarn net.jpountz.lz4.**
-dontwarn org.apache.oro.**
-dontwarn io.opentelemetry.**

# The IntelliJ 'util' JAR bundles 'org.apache.log4j.Level' alone; the rest of log4j is an implementation component as well
-dontwarn org.apache.log4j.**

# ======================================================================================================================
# Known problems
#
# Sic! Don't add more entries here. The entries below have to be fixed and removed.
# ======================================================================================================================

# The generated diagnostics expose compiler types which are shipped in 'kotlin-analysis-api-implementation'.
# That artifact depends on this one, so the classes are missing from the runtime classpath.
-dontwarn org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
-dontwarn org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
-dontwarn org.jetbrains.kotlin.descriptors.ClassKind
-dontwarn org.jetbrains.kotlin.descriptors.EffectiveVisibility
-dontwarn org.jetbrains.kotlin.descriptors.RelationToType
-dontwarn org.jetbrains.kotlin.descriptors.Visibility
-dontwarn org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
-dontwarn org.jetbrains.kotlin.fir.FirModuleData
-dontwarn org.jetbrains.kotlin.fir.declarations.FirDeprecationInfo
-dontwarn org.jetbrains.kotlin.fir.expressions.FirAnnotation
-dontwarn org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
-dontwarn org.jetbrains.kotlin.metadata.deserialization.VersionRequirement$Version
-dontwarn org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
-dontwarn org.jetbrains.kotlin.resolve.ReturnValueStatus
-dontwarn org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
-dontwarn org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
-dontwarn org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility$Mismatch
-dontwarn org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
