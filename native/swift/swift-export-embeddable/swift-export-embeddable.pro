-target 1.8
-dontoptimize
-dontobfuscate
-dontprocesskotlinmetadata
-dontpreverify
-verbose

# Ignore classpath duplication from JVM
-dontnote sun.**

# These are IDE specific and shouldn't be reachable by Swift Export
-dontwarn org.jetbrains.kotlin.com.intellij.openapi.util.Iconable$IconFlags
-dontwarn org.jetbrains.kotlin.com.intellij.openapi.project.IndexNotReadyException
# These annotations are not retained at runtime, so these also shouldn't be reachable
-dontwarn org.jetbrains.kotlin.com.intellij.openapi.util.NlsSafe
-dontwarn org.jetbrains.kotlin.com.intellij.util.concurrency.annotations.RequiresReadLock

# Keep everything from Swift Export standalone
-keep public class org.jetbrains.kotlin.swiftexport.standalone.** { public *; }

# JDK Flight Recorder (jdk.jfr.*) is used by LLFlightRecorder in analysis:low-level-api-fir
# for optional performance tracing. It is available on JDK 9+ but absent from the JDK 8 rt.jar
# that ProGuard uses as its library jar, so these references will always be unresolvable here.
# The LL*Event classes extend jdk.jfr.Event; suppressing jdk.jfr.** causes cascading "can't find
# referenced method" warnings on the LL* program classes themselves, so we suppress those too.
-dontwarn jdk.jfr.**
-dontwarn org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFlightRecorder
-dontwarn org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLAbstractPhaseEvent
-dontwarn org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLPartialBodyAnalysisEvent
-dontwarn org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLPhaseEvent
-dontwarn org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLPhaseWithTraceEvent
-dontwarn org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLPhaseSuspensionEvent
-dontwarn org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLReadyPhaseEvent
-dontwarn org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLStopWorldInvalidation

# The IntelliJ-patched kotlinx-coroutines-core-jvm is embedded into the fat jar (to avoid publishing
# a JetBrains-internal artifact as a transitive dependency). This causes ProGuard to treat coroutines
# classes as program classes. The IntelliJ platform classes in kotlin-compiler-embeddable (library
# classes) reference coroutines types that are now program classes. This is correct at runtime.
# Suppress the "library class depends on program class" warnings for these specific platform classes.
-dontwarn org.jetbrains.kotlin.com.intellij.codeWithMe.ClientIdContextElement
-dontwarn org.jetbrains.kotlin.com.intellij.mock.MockApplication
-dontwarn org.jetbrains.kotlin.com.intellij.mock.MockProject
-dontwarn org.jetbrains.kotlin.com.intellij.mock.MockProject$ChildScope
-dontwarn org.jetbrains.kotlin.com.intellij.util.messages.MessageBus
# Annotation-only references from coroutines classes to relocated/Android annotations not in rt.jar.
# Identical suppressions exist in analysis-api.pro for the same reasons.
-dontwarn android.annotation.SuppressLint
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn org.jetbrains.kotlin.org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Pre-existing: these Analysis API classes reference IntelliJ platform methods or classes that are
# absent from the platform version embedded in kotlin-compiler-embeddable (platform version skew).
# The methods exist in newer platform releases used at runtime; ProGuard sees the older embedded copy.
-dontwarn org.jetbrains.kotlin.analysis.api.fir.KaFirSessionProvider
-dontwarn org.jetbrains.kotlin.analysis.api.fir.components.KaFirDirectoryBasedCompiledCodeProvider
-dontwarn org.jetbrains.kotlin.analysis.api.fir.references.KotlinFirKDocResolutionStrategyProviderService
-dontwarn org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISessionBuilderKt
-dontwarn org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
-dontwarn org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder
-dontwarn org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.FirPartialBodyExpressionResolveTransformer$**
-dontwarn org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedJavaCollectionStubMethod

