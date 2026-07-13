-target 11
-dontoptimize
-dontobfuscate
-dontshrink
-dontprocesskotlinmetadata
-dontpreverify

# Keep everything — we only want the classpath completeness check
-keep class ** { *; }

-dontnote **

# Ignore certain dependencies of IntelliJ like the compiler does
-dontwarn com.intellij.util.diff.*
-dontwarn com.intellij.util.CompressionUtil
-dontwarn com.intellij.util.io.Compressor$Tar
-dontwarn com.intellij.util.io.Decompressor*
-dontwarn com.intellij.platform.diagnostic.telemetry.**

-dontwarn dk.brics.automaton.*
-dontwarn org.jdom.xpath.jaxen.*
-dontwarn gnu.trove.TObjectHashingStrategy
-dontwarn kotlinx.coroutines.debug.DebugProbes

# Section after upgrade to SDK 261
# Some JDK 1.8 intellij modules illegally use List.removeLast()
# com.intellij.java.syntax.parser.JavaDocParser was hijacked to fix exceptions in some CliTestGenerated
# for the remaining two is not known, if the usages are dangerous
# com.intellij.ide.plugins.PluginDescriptorLoader, com.intellij.platform.eel.path.ArrayListEelAbsolutePath
-dontwarn java.util.List
# Used by some intellij classes, e.g. by com.intellij.platform.syntax.extensions.impl.ExtensionRegistryHolderJvmKt
# In fact we don't need this one in runtime
-dontwarn fleet.util.multiplatform.Actual

# Used in script compilation (refineCompilationConfiguration.kt), requires intellij-analysis
-dontwarn com.intellij.openapi.vfs.LocalFileSystem

# Used in REPL
-dontwarn org.jline.**

# Warnings in Guava on broken 'MethodHandle's
-dontwarn com.google.common.hash.Hashing$Crc32cMethodHandles
-dontwarn com.google.common.hash.ChecksumHashFunction$ChecksumMethodHandles

# From LLFirAbstractSessionFactory (KT-85503)
-dontwarn org.jetbrains.kotlin.assignment.plugin.**

# From ScriptingIrExplainGenerationExtension backend extension (disabled by default)
-dontwarn org.jetbrains.kotlin.powerassert.diagram.**

# From CliCompilerUtils.kt, Javac mode (disabled by default & obsolete)
-dontwarn org.jetbrains.kotlin.javac.**

# From JvmFrontendPipelinePhase, used by CommonConfigurationKeys.DUMP_MODEL
-dontwarn org.jetbrains.kotlin.compilerRunner.ArgumentUtils

# From org.apache.log4j.or.jms.MessageRenderer
-dontwarn javax.jms.Message
-dontwarn javax.jms.JMSException

# Unrelated annotations on kotlinx.coroutines
-dontwarn android.annotation.SuppressLint
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Other
-dontwarn java.lang.invoke.MethodHandle
