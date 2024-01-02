/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass

@Serializable
class K2JSCompilerArguments : CommonCompilerArguments() {
    companion object {
        @JvmStatic private val serialVersionUID = 0L
    }

    @GradleDeprecatedOption(
        message = "Only for legacy backend. For IR backend please use task.destinationDirectory and moduleName",
        level = DeprecationLevel.WARNING,
        removeAfter = "1.9.0"
    )
    var outputFile: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var outputDir: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var moduleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var noStdlib = false
        set(value) {
            checkFrozen()
            field = value
        }

    var libraries: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var sourceMap = false
        set(value) {
            checkFrozen()
            field = value
        }

    var sourceMapPrefix: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var sourceMapBaseDirs: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    /**
     * SourceMapEmbedSources should be null by default, since it has effect only when source maps are enabled.
     * When sourceMapEmbedSources are not null and source maps is disabled warning is reported.
     */
    var sourceMapEmbedSources: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var sourceMapNamesPolicy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var metaInfo = false
        set(value) {
            checkFrozen()
            field = value
        }

    var target: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var irKeep: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var moduleKind: String? = MODULE_PLAIN
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) MODULE_PLAIN else value
        }

    var main: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var outputPrefix: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var outputPostfix: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    // Advanced options

    var irProduceKlibDir = false
        set(value) {
            checkFrozen()
            field = value
        }

    var irProduceKlibFile = false
        set(value) {
            checkFrozen()
            field = value
        }

    var irProduceJs = false
        set(value) {
            checkFrozen()
            field = value
        }

    var irDce = false
        set(value) {
            checkFrozen()
            field = value
        }

    var irDceRuntimeDiagnostic: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var irDcePrintReachabilityInfo = false
        set(value) {
            checkFrozen()
            field = value
        }

    var irDceDumpReachabilityInfoToFile: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var irDceDumpDeclarationIrSizesToFile: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var irPropertyLazyInitialization = true
        set(value) {
            checkFrozen()
            field = value
        }

    var irMinimizedMemberNames = false
        set(value) {
            checkFrozen()
            field = value
        }

    var irOnly = false
        set(value) {
            checkFrozen()
            field = value
        }

    var irModuleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var irBaseClassInMetadata = false
        set(value) {
            checkFrozen()
            field = value
        }

    var irSafeExternalBoolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var irSafeExternalBooleanDiagnostic: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var irPerModule = false
        set(value) {
            checkFrozen()
            field = value
        }

    var irPerModuleOutputName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var irPerFile = false
        set(value) {
            checkFrozen()
            field = value
        }

    var irNewIr2Js = true
        set(value) {
            checkFrozen()
            field = value
        }

    var irGenerateInlineAnonymousFunctions = false
        set(value) {
            checkFrozen()
            field = value
        }

    var includes: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var cacheDirectory: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var irBuildCache = false
        set(value) {
            checkFrozen()
            field = value
        }

    var generateDts = false
        set(value) {
            checkFrozen()
            field = value
        }

    var generatePolyfills = true
        set(value) {
            checkFrozen()
            field = value
        }

    var strictImplicitExportType = false
        set(value) {
            checkFrozen()
            field = value
        }

    var useEsClasses = false
        set(value) {
            checkFrozen()
            field = value
        }

    var platformArgumentsProviderJsExpression: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var useEsGenerators = false
        set(value) {
            checkFrozen()
            field = value
        }

    var typedArrays = true
        set(value) {
            checkFrozen()
            field = value
        }

    var friendModulesDisabled = false
        set(value) {
            checkFrozen()
            field = value
        }

    var friendModules: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var extensionFunctionsInExternals = false
        set(value) {
            checkFrozen()
            field = value
        }

    var metadataOnly = false
        set(value) {
            checkFrozen()
            field = value
        }

    var enableJsScripting = false
        set(value) {
            checkFrozen()
            field = value
        }

    var fakeOverrideValidator = false
        set(value) {
            checkFrozen()
            field = value
        }

    var errorTolerancePolicy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var partialLinkageMode: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var partialLinkageLogLevel: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var wasm = false
        set(value) {
            checkFrozen()
            field = value
        }

    var wasmDebug = true
        set(value) {
            checkFrozen()
            field = value
        }

    var wasmKClassFqn = false
        set(value) {
            checkFrozen()
            field = value
        }

    var wasmEnableArrayRangeChecks = false
        set(value) {
            checkFrozen()
            field = value
        }

    var wasmEnableAsserts = false
        set(value) {
            checkFrozen()
            field = value
        }

    var wasmGenerateWat = false
        set(value) {
            checkFrozen()
            field = value
        }

    var wasmTarget: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var wasmUseTrapsInsteadOfExceptions = false
        set(value) {
            checkFrozen()
            field = value
        }

    var forceDeprecatedLegacyCompilerUsage = false
        set(value) {
            checkFrozen()
            field = value
        }

    var optimizeGeneratedJs = true
        set(value) {
            checkFrozen()
            field = value
        }

    private fun MessageCollector.deprecationWarn(value: Boolean, defaultValue: Boolean, name: String) {
        if (value != defaultValue) {
            report(CompilerMessageSeverity.WARNING, "'$name' is deprecated and ignored, it will be removed in a future release")
        }
    }

    override fun configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> {
        // TODO: 'enableJsScripting' is used in intellij tests
        //   Drop it after removing the usage from the intellij repository:
        //   https://github.com/JetBrains/intellij-community/blob/master/plugins/kotlin/gradle/gradle-java/tests/test/org/jetbrains/kotlin/gradle/CompilerArgumentsCachingTest.kt#L329
        collector.deprecationWarn(enableJsScripting, false, "-Xenable-js-scripting")
        collector.deprecationWarn(irBaseClassInMetadata, false, "-Xir-base-class-in-metadata")
        collector.deprecationWarn(irNewIr2Js, true, "-Xir-new-ir2js")

        if (irPerFile && moduleKind != MODULE_ES) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "Per-file compilation can't be used with any `moduleKind` except `es` (ECMAScript Modules)"
            )
        }

        return super.configureAnalysisFlags(collector, languageVersion).also {
            it[allowFullyQualifiedNameInKClass] = wasm && wasmKClassFqn //Only enabled WASM BE supports this flag
        }
    }

    override fun checkIrSupport(languageVersionSettings: LanguageVersionSettings, collector: MessageCollector) {
        if (!isIrBackendEnabled()) return

        if (languageVersionSettings.languageVersion < LanguageVersion.KOTLIN_1_4
            || languageVersionSettings.apiVersion < ApiVersion.KOTLIN_1_4
        ) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "IR backend cannot be used with language or API version below 1.4"
            )
        }
    }

    override fun configureLanguageFeatures(collector: MessageCollector): MutableMap<LanguageFeature, LanguageFeature.State> {
        return super.configureLanguageFeatures(collector).apply {
            if (extensionFunctionsInExternals) {
                this[LanguageFeature.JsEnableExtensionFunctionInExternals] = LanguageFeature.State.ENABLED
            }
            if (!isIrBackendEnabled()) {
                this[LanguageFeature.JsAllowInvalidCharsIdentifiersEscaping] = LanguageFeature.State.DISABLED
            }
            if (isIrBackendEnabled()) {
                this[LanguageFeature.JsAllowValueClassesInExternals] = LanguageFeature.State.ENABLED
            }
            if (wasm) {
                this[LanguageFeature.JsAllowImplementingFunctionInterface] = LanguageFeature.State.ENABLED
            }
        }
    }

    override fun copyOf(): Freezable = copyK2JSCompilerArguments(this, K2JSCompilerArguments())
}

fun K2JSCompilerArguments.isPreIrBackendDisabled(): Boolean =
    irOnly || irProduceJs || irProduceKlibFile || irBuildCache || useK2

fun K2JSCompilerArguments.isIrBackendEnabled(): Boolean =
    irProduceKlibDir || irProduceJs || irProduceKlibFile || wasm || irBuildCache || useK2