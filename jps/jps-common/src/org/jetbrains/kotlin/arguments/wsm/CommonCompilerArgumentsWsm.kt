/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.annotations.Transient
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.utils.IDEAPlatforms
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

@SuppressWarnings("WeakerAccess")
@Serializable
abstract class CommonCompilerArguments : CommonToolArguments() {
    companion object {
        @JvmStatic
        private val serialVersionUID = 0L

        const val PLUGIN_OPTION_FORMAT = "plugin:<pluginId>:<optionName>=<value>"
        const val PLUGIN_DECLARATION_FORMAT = "<path>[=<optionName>=<value>]"

        const val WARN = "warn"
        const val ERROR = "error"
        const val ENABLE = "enable"
        const val DEFAULT = "default"
    }

    @get:Transient
    var autoAdvanceLanguageVersion = true
        set(value) {
            checkFrozen()
            field = value
        }

    var languageVersion: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @get:Transient
    var autoAdvanceApiVersion = true
        set(value) {
            checkFrozen()
            field = value
        }

    var apiVersion: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var kotlinHome: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var progressiveMode = false
        set(value) {
            checkFrozen()
            field = value
        }

    var script = false
        set(value) {
            checkFrozen()
            field = value
        }

    var optIn: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    // Advanced options

    var noInline = false
        set(value) {
            checkFrozen()
            field = value
        }

    var skipMetadataVersionCheck = false
        set(value) {
            checkFrozen()
            field = value
        }

    var skipPrereleaseCheck = false
        set(value) {
            checkFrozen()
            field = value
        }

    var allowKotlinPackage = false
        set(value) {
            checkFrozen()
            field = value
        }

    var reportOutputFiles = false
        set(value) {
            checkFrozen()
            field = value
        }

    var pluginClasspaths: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var pluginOptions: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var pluginConfigurations: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var multiPlatform = false
        set(value) {
            checkFrozen()
            field = value
        }

    var noCheckActual = false
        set(value) {
            checkFrozen()
            field = value
        }

    var intellijPluginRoot: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var newInference = false
        set(value) {
            checkFrozen()
            field = value
        }

    var inlineClasses = false
        set(value) {
            checkFrozen()
            field = value
        }

    var legacySmartCastAfterTry = false
        set(value) {
            checkFrozen()
            field = value
        }

    var effectSystem = false
        set(value) {
            checkFrozen()
            field = value
        }

    var readDeserializedContracts = false
        set(value) {
            checkFrozen()
            field = value
        }

    @IDEAPluginsCompatibilityAPI(
        IDEAPlatforms._212, // maybe 211 AS used it too
        IDEAPlatforms._213,
        message = "Please migrate to -opt-in",
        plugins = "Android"
    )
    var experimental: Array<String>? = null

    @IDEAPluginsCompatibilityAPI(
        IDEAPlatforms._212, // maybe 211 AS used it too
        IDEAPlatforms._213,
        message = "Please migrate to -opt-in",
        plugins = "Android"
    )
    var useExperimental: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var properIeee754Comparisons = false
        set(value) {
            checkFrozen()
            field = value
        }

    var reportPerf = false
        set(value) {
            checkFrozen()
            field = value
        }

    var dumpPerf: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var metadataVersion: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var commonSources: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var allowResultReturnType = false
        set(value) {
            checkFrozen()
            field = value
        }

    var listPhases = false
        set(value) {
            checkFrozen()
            field = value
        }

    var disablePhases: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var verbosePhases: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var phasesToDumpBefore: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var phasesToDumpAfter: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var phasesToDump: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var dumpDirectory: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var dumpOnlyFqName: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var phasesToValidateBefore: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var phasesToValidateAfter: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var phasesToValidate: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var profilePhases = false
        set(value) {
            checkFrozen()
            field = value
        }

    var checkPhaseConditions = false
        set(value) {
            checkFrozen()
            field = value
        }

    var checkStickyPhaseConditions = false
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleDeprecatedOption(
        message = "Compiler flag -Xuse-k2 is deprecated; please use language version 2.0 instead",
        level = DeprecationLevel.WARNING,
        removeAfter = "2.0.0",
    )
    var useK2 = false
        set(value) {
            checkFrozen()
            field = value
        }

    var useFirExtendedCheckers = false
        set(value) {
            checkFrozen()
            field = value
        }

    var useFirIC = false
        set(value) {
            checkFrozen()
            field = value
        }

    var useFirLT = true
        set(value) {
            checkFrozen()
            field = value
        }

    var useIrFakeOverrideBuilder = false
        set(value) {
            checkFrozen()
            field = value
        }

    var disableUltraLightClasses = false
        set(value) {
            checkFrozen()
            field = value
        }

    var useMixedNamedArguments = false
        set(value) {
            checkFrozen()
            field = value
        }

    var metadataKlib: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var disableDefaultScriptingPlugin = false
        set(value) {
            checkFrozen()
            field = value
        }

    var explicitApi: String = ExplicitApiMode.DISABLED.state
        set(value) {
            checkFrozen()
            field = value
        }

    var inferenceCompatibility = false
        set(value) {
            checkFrozen()
            field = value
        }

    var suppressVersionWarnings = false
        set(value) {
            checkFrozen()
            field = value
        }

    var extendedCompilerChecks = false
        set(value) {
            checkFrozen()
            field = value
        }

    var builtInsFromSources = false
        set(value) {
            checkFrozen()
            field = value
        }

    var expectActualClasses = false
        set(value) {
            checkFrozen()
            field = value
        }

    var unrestrictedBuilderInference = false
        set(value) {
            checkFrozen()
            field = value
        }

    var enableBuilderInference = false
        set(value) {
            checkFrozen()
            field = value
        }

    var selfUpperBoundInference = false
        set(value) {
            checkFrozen()
            field = value
        }

    var contextReceivers = false
        set(value) {
            checkFrozen()
            field = value
        }

    var relativePathBases: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var normalizeAbsolutePath = false
        set(value) {
            checkFrozen()
            field = value
        }

    var enableSignatureClashChecks = true
        set(value) {
            checkFrozen()
            field = value
        }

    var incrementalCompilation: Boolean? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var renderInternalDiagnosticNames = false
        set(value) {
            checkFrozen()
            field = value
        }

    var allowAnyScriptsInSourceRoots = false
        set(value) {
            checkFrozen()
            field = value
        }

    var fragments: Array<String>? = null

    var fragmentSources: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var fragmentRefines: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    var ignoreConstOptimizationErrors = false
        set(value) {
            checkFrozen()
            field = value
        }

    var dontWarnOnErrorSuppression = false
        set(value) {
            checkFrozen()
            field = value
        }

    @OptIn(IDEAPluginsCompatibilityAPI::class)
    open fun configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> {
        return HashMap<AnalysisFlag<*>, Any>().apply {
            put(AnalysisFlags.skipMetadataVersionCheck, skipMetadataVersionCheck)
            put(AnalysisFlags.skipPrereleaseCheck, skipPrereleaseCheck || skipMetadataVersionCheck)
            put(AnalysisFlags.multiPlatformDoNotCheckActual, noCheckActual)
            val useExperimentalFqNames = useExperimental?.toList().orEmpty()
            if (useExperimentalFqNames.isNotEmpty()) {
                collector.report(
                    WARNING, "'-Xuse-experimental' is deprecated and will be removed in a future release, please use -opt-in instead"
                )
            }
            put(AnalysisFlags.optIn, useExperimentalFqNames + optIn?.toList().orEmpty())
            put(AnalysisFlags.skipExpectedActualDeclarationChecker, metadataKlib)
            put(AnalysisFlags.explicitApiVersion, apiVersion != null)
            put(AnalysisFlags.allowResultReturnType, allowResultReturnType)
            ExplicitApiMode.fromString(explicitApi)?.also { put(AnalysisFlags.explicitApiMode, it) } ?: collector.report(
                CompilerMessageSeverity.ERROR,
                "Unknown value for parameter -Xexplicit-api: '$explicitApi'. Value should be one of ${ExplicitApiMode.availableValues()}"
            )
            put(AnalysisFlags.extendedCompilerChecks, extendedCompilerChecks)
            put(AnalysisFlags.allowKotlinPackage, allowKotlinPackage)
            put(AnalysisFlags.builtInsFromSources, builtInsFromSources)
            put(AnalysisFlags.muteExpectActualClassesWarning, expectActualClasses)
            put(AnalysisFlags.allowFullyQualifiedNameInKClass, true)
            put(AnalysisFlags.dontWarnOnErrorSuppression, dontWarnOnErrorSuppression)
        }
    }

    open fun configureLanguageFeatures(collector: MessageCollector): MutableMap<LanguageFeature, LanguageFeature.State> =
        HashMap<LanguageFeature, LanguageFeature.State>().apply {
            if (multiPlatform) {
                put(LanguageFeature.MultiPlatformProjects, LanguageFeature.State.ENABLED)
            }

            if (unrestrictedBuilderInference) {
                put(LanguageFeature.UnrestrictedBuilderInference, LanguageFeature.State.ENABLED)
            }

            if (enableBuilderInference) {
                put(LanguageFeature.UseBuilderInferenceWithoutAnnotation, LanguageFeature.State.ENABLED)
            }

            if (selfUpperBoundInference) {
                put(LanguageFeature.TypeInferenceOnCallsWithSelfTypes, LanguageFeature.State.ENABLED)
            }

            if (newInference) {
                put(LanguageFeature.NewInference, LanguageFeature.State.ENABLED)
                put(LanguageFeature.SamConversionPerArgument, LanguageFeature.State.ENABLED)
                put(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType, LanguageFeature.State.ENABLED)
                put(LanguageFeature.DisableCompatibilityModeForNewInference, LanguageFeature.State.ENABLED)
            }

            if (contextReceivers) {
                put(LanguageFeature.ContextReceivers, LanguageFeature.State.ENABLED)
            }

            if (inlineClasses) {
                put(LanguageFeature.InlineClasses, LanguageFeature.State.ENABLED)
            }

            if (legacySmartCastAfterTry) {
                put(LanguageFeature.SoundSmartCastsAfterTry, LanguageFeature.State.DISABLED)
            }

            if (effectSystem) {
                put(LanguageFeature.UseCallsInPlaceEffect, LanguageFeature.State.ENABLED)
                put(LanguageFeature.UseReturnsEffect, LanguageFeature.State.ENABLED)
            }

            if (readDeserializedContracts) {
                put(LanguageFeature.ReadDeserializedContracts, LanguageFeature.State.ENABLED)
            }

            if (properIeee754Comparisons) {
                put(LanguageFeature.ProperIeee754Comparisons, LanguageFeature.State.ENABLED)
            }

            if (useMixedNamedArguments) {
                put(LanguageFeature.MixedNamedArgumentsInTheirOwnPosition, LanguageFeature.State.ENABLED)
            }

            if (inferenceCompatibility) {
                put(LanguageFeature.InferenceCompatibility, LanguageFeature.State.ENABLED)
            }

            if (progressiveMode) {
                LanguageFeature.entries.filter { it.enabledInProgressiveMode }.forEach {
                    // Don't overwrite other settings: users may want to turn off some particular
                    // breaking change manually instead of turning off whole progressive mode
                    if (!contains(it)) put(it, LanguageFeature.State.ENABLED)
                }
            }

            if (useK2) {
                // TODO: remove when K2 compilation will mean LV 2.0
                put(LanguageFeature.SkipStandaloneScriptsInSourceRoots, LanguageFeature.State.ENABLED)
            } else if (allowAnyScriptsInSourceRoots) {
                put(LanguageFeature.SkipStandaloneScriptsInSourceRoots, LanguageFeature.State.DISABLED)
            }

            // Internal arguments should go last, because it may be useful to override
            // some feature state via -XX (even if some -X flags were passed)
            if (internalArguments.isNotEmpty()) {
                configureLanguageFeaturesFromInternalArgs(collector)
            }

            configureExtraLanguageFeatures(this)
        }

    protected open fun configureExtraLanguageFeatures(map: HashMap<LanguageFeature, LanguageFeature.State>) {}

    private fun HashMap<LanguageFeature, LanguageFeature.State>.configureLanguageFeaturesFromInternalArgs(collector: MessageCollector) {
        val featuresThatForcePreReleaseBinaries = mutableListOf<LanguageFeature>()
        val disabledFeaturesFromUnsupportedVersions = mutableListOf<LanguageFeature>()

        var standaloneSamConversionFeaturePassedExplicitly = false
        var functionReferenceWithDefaultValueFeaturePassedExplicitly = false
        for ((feature, state) in internalArguments.filterIsInstance<ManualLanguageFeatureSetting>()) {
            put(feature, state)
            if (state == LanguageFeature.State.ENABLED && feature.forcesPreReleaseBinariesIfEnabled()) {
                featuresThatForcePreReleaseBinaries += feature
            }

            if (state == LanguageFeature.State.DISABLED && feature.sinceVersion?.isUnsupported == true) {
                disabledFeaturesFromUnsupportedVersions += feature
            }

            when (feature) {
                LanguageFeature.SamConversionPerArgument ->
                    standaloneSamConversionFeaturePassedExplicitly = true

                LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType ->
                    functionReferenceWithDefaultValueFeaturePassedExplicitly = true

                else -> {}
            }
        }

        if (this[LanguageFeature.NewInference] == LanguageFeature.State.ENABLED) {
            if (!standaloneSamConversionFeaturePassedExplicitly)
                put(LanguageFeature.SamConversionPerArgument, LanguageFeature.State.ENABLED)

            if (!functionReferenceWithDefaultValueFeaturePassedExplicitly)
                put(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType, LanguageFeature.State.ENABLED)

            put(LanguageFeature.DisableCompatibilityModeForNewInference, LanguageFeature.State.ENABLED)
        }

        if (featuresThatForcePreReleaseBinaries.isNotEmpty()) {
            collector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Following manually enabled features will force generation of pre-release binaries: ${featuresThatForcePreReleaseBinaries.joinToString()}"
            )
        }

        if (disabledFeaturesFromUnsupportedVersions.isNotEmpty()) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "The following features cannot be disabled manually, because the version they first appeared in is no longer " +
                        "supported:\n${disabledFeaturesFromUnsupportedVersions.joinToString()}"
            )
        }
    }

    fun toLanguageVersionSettings(collector: MessageCollector): LanguageVersionSettings {
        return toLanguageVersionSettings(collector, emptyMap())
    }

    fun toLanguageVersionSettings(
        collector: MessageCollector,
        additionalAnalysisFlags: Map<AnalysisFlag<*>, Any>
    ): LanguageVersionSettings {
        val languageVersion = parseOrConfigureLanguageVersion(collector)
        // If only "-language-version" is specified, API version is assumed to be equal to the language version
        // (API version cannot be greater than the language version)
        val apiVersion = ApiVersion.createByLanguageVersion(parseVersion(collector, apiVersion, "API") ?: languageVersion)

        checkApiVersionIsNotGreaterThenLanguageVersion(languageVersion, apiVersion, collector)

        val languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion,
            apiVersion,
            configureAnalysisFlags(collector, languageVersion) + additionalAnalysisFlags,
            configureLanguageFeatures(collector)
        )

        checkLanguageVersionIsStable(languageVersion, collector)
        checkOutdatedVersions(languageVersion, apiVersion, collector)
        checkProgressiveMode(languageVersion, collector)

        checkIrSupport(languageVersionSettings, collector)

        checkPlatformSpecificSettings(languageVersionSettings, collector)

        return languageVersionSettings
    }

    private fun checkApiVersionIsNotGreaterThenLanguageVersion(
        languageVersion: LanguageVersion,
        apiVersion: ApiVersion,
        collector: MessageCollector
    ) {
        if (apiVersion > ApiVersion.createByLanguageVersion(languageVersion)) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "-api-version (${apiVersion.versionString}) cannot be greater than -language-version (${languageVersion.versionString})"
            )
        }
    }

    fun checkLanguageVersionIsStable(languageVersion: LanguageVersion, collector: MessageCollector) {
        if (!languageVersion.isStable && !suppressVersionWarnings) {
            collector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Language version ${languageVersion.versionString} is experimental, there are no backwards compatibility guarantees for " +
                        "new language and library features"
            )
        }
    }

    private fun checkOutdatedVersions(language: LanguageVersion, api: ApiVersion, collector: MessageCollector) {
        val (version, supportedVersion, versionKind) = findOutdatedVersion(language, api) ?: return
        when {
            version.isUnsupported -> {
                collector.report(
                    CompilerMessageSeverity.ERROR,
                    "${versionKind.text} version ${version.versionString} is no longer supported; " +
                            "please, use version ${supportedVersion!!.versionString} or greater."
                )
            }
            version.isDeprecated && !suppressVersionWarnings -> {
                collector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "${versionKind.text} version ${version.versionString} is deprecated " +
                            "and its support will be removed in a future version of Kotlin"
                )
            }
        }
    }

    private fun findOutdatedVersion(
        language: LanguageVersion,
        api: ApiVersion
    ): Triple<LanguageOrApiVersion, LanguageOrApiVersion?, VersionKind>? {
        return when {
            language.isUnsupported -> Triple(language, LanguageVersion.FIRST_SUPPORTED, VersionKind.LANGUAGE)
            api.isUnsupported -> Triple(api, ApiVersion.FIRST_SUPPORTED, VersionKind.API)
            language.isDeprecated -> Triple(language, null, VersionKind.LANGUAGE)
            api.isDeprecated -> Triple(api, null, VersionKind.API)
            else -> null
        }
    }

    private fun checkProgressiveMode(languageVersion: LanguageVersion, collector: MessageCollector) {
        if (progressiveMode && languageVersion < LanguageVersion.LATEST_STABLE && !suppressVersionWarnings) {
            collector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "'-progressive' is meaningful only for the latest language version (${LanguageVersion.LATEST_STABLE}), " +
                        "while this build uses $languageVersion\n" +
                        "Compiler behavior in such mode is undefined; please, consider moving to the latest stable version " +
                        "or turning off progressive mode."
            )
        }
    }

    protected open fun defaultLanguageVersion(collector: MessageCollector): LanguageVersion =
        LanguageVersion.LATEST_STABLE

    protected open fun checkPlatformSpecificSettings(languageVersionSettings: LanguageVersionSettings, collector: MessageCollector) {
    }

    protected open fun checkIrSupport(languageVersionSettings: LanguageVersionSettings, collector: MessageCollector) {
        // backend-specific
    }

    private enum class VersionKind(val text: String) {
        LANGUAGE("Language"), API("API")
    }

    private fun parseOrConfigureLanguageVersion(collector: MessageCollector): LanguageVersion {
        // If only "-api-version" is specified, language version is assumed to be the latest stable (or 2.0 with -Xuse-k2)
        val explicitVersion = parseVersion(collector, languageVersion, "language")
        val explicitOrDefaultVersion = explicitVersion ?: defaultLanguageVersion(collector)
        if (useK2) {
            val message = when (explicitVersion?.usesK2) {
                true ->
                    "Deprecated compiler flag -Xuse-k2 is redundant because of \"-language-version $explicitVersion\" and should be removed"
                false ->
                    "Deprecated compiler flag -Xuse-k2 overrides \"-language-version $explicitVersion\" to 2.0;" +
                            " please remove -Xuse-k2 and use -language-version to select either $explicitVersion or 2.0"
                null ->
                    "Compiler flag -Xuse-k2 is deprecated; please use \"-language-version 2.0\" instead"
            }
            collector.report(CompilerMessageSeverity.STRONG_WARNING, message)
        }
        return if (useK2 && !explicitOrDefaultVersion.usesK2) LanguageVersion.KOTLIN_2_0
        else explicitOrDefaultVersion
    }

    private fun parseVersion(collector: MessageCollector, value: String?, versionOf: String): LanguageVersion? =
        if (value == null) null
        else LanguageVersion.fromVersionString(value)
            ?: run {
                val versionStrings = LanguageVersion.values().filterNot(LanguageVersion::isUnsupported).map(LanguageVersion::description)
                val message = "Unknown $versionOf version: $value\nSupported $versionOf versions: ${versionStrings.joinToString(", ")}"
                collector.report(CompilerMessageSeverity.ERROR, message, null)
                null
            }

    // Used only for serialize and deserialize settings. Don't use in other places!
    @Serializable
    class DummyImpl : CommonCompilerArguments() {
        override fun copyOf(): Freezable = copyCommonCompilerArguments(this, DummyImpl())
    }
}