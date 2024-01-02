/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.wsm
import org.jetbrains.kotlin.cli.common.arguments.JavaTypeEnhancementStateParser

//import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*

//@Serializable
class K2JVMCompilerArgumentsWsmWsm : CommonCompilerArgumentsWsm() {
    companion object {
        @JvmStatic
        private val serialVersionUID = 0L
    }

    var destination: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var classpath: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var includeRuntime = false
        set(value) {
            
            field = value
        }

    var jdkHome: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var noJdk = false
        set(value) {
            
            field = value
        }

    var noStdlib = false
        set(value) {
            
            field = value
        }

    var noReflect = false
        set(value) {
            
            field = value
        }

    var expression: String? = null
        set(value) {
            
            field = value
        }

    var scriptTemplates: Array<String>? = null
        set(value) {
            
            field = value
        }

    var moduleName: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var jvmTarget: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var javaParameters = false
        set(value) {
            
            field = value
        }

    // Advanced options

    var useOldBackend = false
        set(value) {
            
            field = value
        }

    var allowUnstableDependencies = false
        set(value) {
            
            field = value
        }

    var abiStability: String? = null
        set(value) {
            
            field = value
        }

    var doNotClearBindingContext = false
        set(value) {
            
            field = value
        }

    var backendThreads: String = "1"
        set(value) {
            
            field = value
        }

    var javaModulePath: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var additionalJavaModules: Array<String>? = null
        set(value) {
            
            field = value
        }

    var noCallAssertions = false
        set(value) {
            
            field = value
        }

    var noReceiverAssertions = false
        set(value) {
            
            field = value
        }

    var noParamAssertions = false
        set(value) {
            
            field = value
        }

    var noOptimize = false
        set(value) {
            
            field = value
        }

    var assertionsMode: String? = JVMAssertionsMode.DEFAULT.description
        set(value) {
            
            field = if (value.isNullOrEmpty()) JVMAssertionsMode.DEFAULT.description else value
        }

    var buildFile: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var inheritMultifileParts = false
        set(value) {
            
            field = value
        }

    var useTypeTable = false
        set(value) {
            
            field = value
        }

    var useOldClassFilesReading = false
        set(value) {
            
            field = value
        }

    var useFastJarFileSystem = false
        set(value) {
            
            field = value
        }

    var declarationsOutputPath: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var suppressMissingBuiltinsError = false
        set(value) {
            
            field = value
        }

    var scriptResolverEnvironment: Array<String>? = null
        set(value) {
            
            field = value
        }

    // Javac options
    var useJavac = false
        set(value) {
            
            field = value
        }

    var compileJava = false
        set(value) {
            
            field = value
        }

    var javacArguments: Array<String>? = null
        set(value) {
            
            field = value
        }


    var javaSourceRoots: Array<String>? = null
        set(value) {
            
            field = value
        }

    var javaPackagePrefix: String? = null
        set(value) {
            
            field = value
        }

    var jsr305: Array<String>? = null
        set(value) {
            
            field = value
        }

    var nullabilityAnnotations: Array<String>? = null
        set(value) {
            
            field = value
        }

    var supportCompatqualCheckerFrameworkAnnotations: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var jspecifyAnnotations: String? = null
        set(value) {
            
            field = value
        }

    var jvmDefault: String = JvmDefaultMode.DEFAULT.description
        set(value) {
            
            field = value
        }

    var defaultScriptExtension: String? = null
        set(value) {
            
            field = value
        }

    var disableStandardScript = false
        set(value) {
            
            field = value
        }

    var strictMetadataVersionSemantics = false
        set(value) {
            
            field = value
        }

    var sanitizeParentheses = false
        set(value) {
            
            field = value
        }

    var friendPaths: Array<String>? = null
        set(value) {
            
            field = value
        }

    var allowNoSourceFiles = false
        set(value) {
            
            field = value
        }

    var emitJvmTypeAnnotations = false
        set(value) {
            
            field = value
        }

    var stringConcat: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var jdkRelease: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }


    var samConversions: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var lambdas: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var klibLibraries: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var noOptimizedCallableReferences = false
        set(value) {
            
            field = value
        }

    var noKotlinNothingValueException = false
        set(value) {
            
            field = value
        }

    var noResetJarTimestamps = false
        set(value) {
            
            field = value
        }

    var noUnifiedNullChecks = false
        set(value) {
            
            field = value
        }

    var noSourceDebugExtension = false
        set(value) {
            
            field = value
        }

    var profileCompilerCommand: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var repeatCompileModules: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var useOldInlineClassesManglingScheme = false
        set(value) {
            
            field = value
        }

    var enableJvmPreview = false
        set(value) {
            
            field = value
        }

    var suppressDeprecatedJvmTargetWarning = false
        set(value) {
            
            field = value
        }

    var typeEnhancementImprovementsInStrictMode = false
        set(value) {
            
            field = value
        }

    var serializeIr: String = "none"
        set(value) {
            
            field = value
        }

    var validateIr = false
        set(value) {
            
            field = value
        }

    var validateBytecode = false
        set(value) {
            
            field = value
        }

    var enhanceTypeParameterTypesToDefNotNull = false
        set(value) {
            
            field = value
        }

    var linkViaSignatures = false
        set(value) {
            
            field = value
        }

    var enableDebugMode = false
        set(value) {
            
            field = value
        }

    var noNewJavaAnnotationTargets = false
        set(value) {
            
            field = value
        }

    var oldInnerClassesLogic = false
        set(value) {
            
            field = value
        }

    var valueClasses = false
        set(value) {
            
            field = value
        }

    var enableIrInliner: Boolean = false
        set(value) {
            
            field = value
        }

    var useKapt4 = false
        set(value) {
            
            field = value
        }

    override fun configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> {
        val result = super.configureAnalysisFlags(collector, languageVersion)
        result[JvmAnalysisFlags.strictMetadataVersionSemantics] = strictMetadataVersionSemantics
        result[JvmAnalysisFlags.javaTypeEnhancementState] = JavaTypeEnhancementStateParser(collector, languageVersion.toKotlinVersion())
            .parse(jsr305, supportCompatqualCheckerFrameworkAnnotations, jspecifyAnnotations, nullabilityAnnotations)
        result[AnalysisFlags.ignoreDataFlowInAssert] = JVMAssertionsMode.fromString(assertionsMode) != JVMAssertionsMode.LEGACY
        JvmDefaultMode.fromStringOrNull(jvmDefault)?.let {
            result[JvmAnalysisFlags.jvmDefaultMode] = it
        } ?: collector.report(
            CompilerMessageSeverity.ERROR,
            "Unknown -Xjvm-default mode: $jvmDefault, supported modes: ${
                JvmDefaultMode.values().mapNotNull { mode ->
                    mode.description.takeIf { JvmDefaultMode.fromStringOrNull(it) != null }
                }
            }"
        )
        result[JvmAnalysisFlags.inheritMultifileParts] = inheritMultifileParts
        result[JvmAnalysisFlags.sanitizeParentheses] = sanitizeParentheses
        result[JvmAnalysisFlags.suppressMissingBuiltinsError] = suppressMissingBuiltinsError
        result[JvmAnalysisFlags.enableJvmPreview] = enableJvmPreview
        result[AnalysisFlags.allowUnstableDependencies] = allowUnstableDependencies
        result[JvmAnalysisFlags.disableUltraLightClasses] = disableUltraLightClasses
        result[JvmAnalysisFlags.useIR] = !useOldBackend
        return result
    }

    override fun configureLanguageFeatures(collector: MessageCollector): MutableMap<LanguageFeature, LanguageFeature.State> {
        val result = super.configureLanguageFeatures(collector)
        if (typeEnhancementImprovementsInStrictMode) {
            result[LanguageFeature.TypeEnhancementImprovementsInStrictMode] = LanguageFeature.State.ENABLED
        }
        if (enhanceTypeParameterTypesToDefNotNull) {
            result[LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated] = LanguageFeature.State.ENABLED
        }
        if (JvmDefaultMode.fromStringOrNull(jvmDefault)?.forAllMethodsWithBody == true) {
            result[LanguageFeature.ForbidSuperDelegationToAbstractFakeOverride] = LanguageFeature.State.ENABLED
            result[LanguageFeature.AbstractClassMemberNotImplementedWithIntermediateAbstractClass] = LanguageFeature.State.ENABLED
        }
        if (valueClasses) {
            result[LanguageFeature.ValueClasses] = LanguageFeature.State.ENABLED
        }
        return result
    }

    override fun defaultLanguageVersion(collector: MessageCollector): LanguageVersion =
        if (useOldBackend) {
            if (!suppressVersionWarnings) {
                collector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Language version is automatically inferred to ${LanguageVersion.KOTLIN_1_5.versionString} when using " +
                            "the old JVM backend. Consider specifying -language-version explicitly, or remove -Xuse-old-backend"
                )
            }
            LanguageVersion.KOTLIN_1_5
        } else super.defaultLanguageVersion(collector)

    override fun checkPlatformSpecificSettings(languageVersionSettings: LanguageVersionSettings, collector: MessageCollector) {
        if (useOldBackend && languageVersionSettings.languageVersion >= LanguageVersion.KOTLIN_1_6) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "Old JVM backend does not support language version 1.6 or above. " +
                        "Please use language version 1.5 or below, or remove -Xuse-old-backend"
            )
        }
        if (oldInnerClassesLogic) {
            collector.report(
                CompilerMessageSeverity.WARNING,
                "The -Xuse-old-innerclasses-logic option is deprecated and will be deleted in future versions."
            )
        }
    }


}