/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.analysis.FqNameMatcher
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityConfigParser
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import androidx.compose.compiler.plugins.kotlin.k1.*
import androidx.compose.compiler.plugins.kotlin.k2.ComposeFirExtensionRegistrar
import androidx.compose.compiler.plugins.kotlin.lower.ClassStabilityFieldSerializationPlugin
import androidx.compose.compiler.plugins.kotlin.lower.hiddenfromobjc.AddHiddenFromObjCSerializationPlugin
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.serialization.DescriptorSerializerPlugin

object ComposeConfiguration {
    val LIVE_LITERALS_ENABLED_KEY =
        CompilerConfigurationKey<Boolean>("Enable Live Literals code generation")
    val LIVE_LITERALS_V2_ENABLED_KEY =
        CompilerConfigurationKey<Boolean>(
            "Enable Live Literals code generation (with per-file enabled flags)"
        )
    val GENERATE_FUNCTION_KEY_META_CLASSES_KEY =
        CompilerConfigurationKey<Boolean>(
            "Generate function key meta classes"
        )
    val SOURCE_INFORMATION_ENABLED_KEY =
        CompilerConfigurationKey<Boolean>("Include source information in generated code")
    val METRICS_DESTINATION_KEY =
        CompilerConfigurationKey<String>("Directory to save compose build metrics")
    val REPORTS_DESTINATION_KEY =
        CompilerConfigurationKey<String>("Directory to save compose build reports")
    val INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_KEY =
        CompilerConfigurationKey<Boolean>("Enable optimization to treat remember as an intrinsic")
    val NON_SKIPPING_GROUP_OPTIMIZATION_ENABLED_KEY =
        CompilerConfigurationKey<Boolean>(
            "Enabled optimization to remove groups around non-skipping functions"
        )
    val SUPPRESS_KOTLIN_VERSION_COMPATIBILITY_CHECK = CompilerConfigurationKey<String?>(
        "Deprecated. Version of Kotlin for which version compatibility check should be suppressed"
    )
    val DECOYS_ENABLED_KEY =
        CompilerConfigurationKey<Boolean>("Generate decoy methods in IR transform")
    val STRONG_SKIPPING_ENABLED_KEY =
        CompilerConfigurationKey<Boolean>("Enable strong skipping mode")
    val STABILITY_CONFIG_PATH_KEY =
        CompilerConfigurationKey<List<String>>(
            "Path to stability configuration file"
        )
    val TEST_STABILITY_CONFIG_KEY =
        CompilerConfigurationKey<Set<String>>(
            "Set of stable classes to be merged with configuration file, used for testing."
        )
    val TRACE_MARKERS_ENABLED_KEY =
        CompilerConfigurationKey<Boolean>("Include composition trace markers in generated code")
    val FEATURE_FLAGS =
        CompilerConfigurationKey<List<String>>(
            "A list of features to enable."
        )
    val SKIP_IR_LOWERING_IF_RUNTIME_NOT_FOUND_KEY =
        CompilerConfigurationKey<Boolean>("Skip IR lowering transformation when finding Compose runtime fails")
}

@OptIn(ExperimentalCompilerApi::class)
class ComposeCommandLineProcessor : CommandLineProcessor {
    companion object {
        val PLUGIN_ID = "androidx.compose.compiler.plugins.kotlin"
        val LIVE_LITERALS_ENABLED_OPTION = CliOption(
            "liveLiterals",
            "<true|false>",
            "Enable Live Literals code generation",
            required = false,
            allowMultipleOccurrences = false
        )
        val LIVE_LITERALS_V2_ENABLED_OPTION = CliOption(
            "liveLiteralsEnabled",
            "<true|false>",
            "Enable Live Literals code generation (with per-file enabled flags)",
            required = false,
            allowMultipleOccurrences = false
        )
        val GENERATE_FUNCTION_KEY_META_CLASSES_OPTION = CliOption(
            "generateFunctionKeyMetaClasses",
            "<true|false>",
            "Generate function key meta classes with annotations indicating the " +
                "functions and their group keys. Generally used for tooling.",
            required = false,
            allowMultipleOccurrences = false
        )
        val SOURCE_INFORMATION_ENABLED_OPTION = CliOption(
            "sourceInformation",
            "<true|false>",
            "Include source information in generated code",
            required = false,
            allowMultipleOccurrences = false
        )
        val METRICS_DESTINATION_OPTION = CliOption(
            "metricsDestination",
            "<path>",
            "Save compose build metrics to this folder",
            required = false,
            allowMultipleOccurrences = false
        )
        val REPORTS_DESTINATION_OPTION = CliOption(
            "reportsDestination",
            "<path>",
            "Save compose build reports to this folder",
            required = false,
            allowMultipleOccurrences = false
        )
        val FEATURE_FLAG_OPTION = CliOption(
            "featureFlag",
            "<feature name>",
            "The name of the feature to enable",
            required = false,
            allowMultipleOccurrences = true
        )
        val INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_OPTION = CliOption(
            "intrinsicRemember",
            "<true|false>",
            "Include source information in generated code. Deprecated. Use ${
                useFeatureFlagInsteadMessage(FeatureFlag.IntrinsicRemember)
            }",
            required = false,
            allowMultipleOccurrences = false
        )
        val NON_SKIPPING_GROUP_OPTIMIZATION_ENABLED_OPTION = CliOption(
            optionName = "nonSkippingGroupOptimization",
            valueDescription = "<true|false>",
            description = "Remove groups around non-skipping composable functions. " +
                "Deprecated. ${
                    useFeatureFlagInsteadMessage(FeatureFlag.OptimizeNonSkippingGroups)
                }",
            required = false,
            allowMultipleOccurrences = false
        )
        val SUPPRESS_KOTLIN_VERSION_CHECK_ENABLED_OPTION = CliOption(
            "suppressKotlinVersionCompatibilityCheck",
            "<kotlin_version>",
            "Deprecated. Suppress Kotlin version compatibility check",
            required = false,
            allowMultipleOccurrences = false
        )
        val DECOYS_ENABLED_OPTION = CliOption(
            "generateDecoys",
            "<true|false>",
            "Generate decoy methods in IR transform",
            required = false,
            allowMultipleOccurrences = false
        )
        val STRONG_SKIPPING_OPTION = CliOption(
            "strongSkipping",
            "<true|false>",
            "Enable strong skipping mode. " +
                "Deprecated. ${useFeatureFlagInsteadMessage(FeatureFlag.StrongSkipping)}",
            required = false,
            allowMultipleOccurrences = false
        )
        val EXPERIMENTAL_STRONG_SKIPPING_OPTION = CliOption(
            "experimentalStrongSkipping",
            "<true|false>",
            "Deprecated. ${
                useFeatureFlagInsteadMessage(FeatureFlag.StrongSkipping)
            }",
            required = false,
            allowMultipleOccurrences = false
        )
        val STABLE_CONFIG_PATH_OPTION = CliOption(
            "stabilityConfigurationPath",
            "<path>",
            "Path to stability configuration file",
            required = false,
            allowMultipleOccurrences = true
        )
        val TRACE_MARKERS_OPTION = CliOption(
            "traceMarkersEnabled",
            "<true|false>",
            "Include composition trace markers in generate code",
            required = false,
            allowMultipleOccurrences = false
        )
        val SKIP_IR_LOWERING_IF_RUNTIME_NOT_FOUND_OPTION = CliOption(
            "skipIrLoweringIfRuntimeNotFound",
            "<true|false>",
            "Skip IR lowering transformation when finding Compose runtime fails",
            required = false,
            allowMultipleOccurrences = false
        )
    }

    override val pluginId = PLUGIN_ID
    override val pluginOptions = listOf(
        LIVE_LITERALS_ENABLED_OPTION,
        LIVE_LITERALS_V2_ENABLED_OPTION,
        GENERATE_FUNCTION_KEY_META_CLASSES_OPTION,
        SOURCE_INFORMATION_ENABLED_OPTION,
        METRICS_DESTINATION_OPTION,
        REPORTS_DESTINATION_OPTION,
        INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_OPTION,
        NON_SKIPPING_GROUP_OPTIMIZATION_ENABLED_OPTION,
        SUPPRESS_KOTLIN_VERSION_CHECK_ENABLED_OPTION,
        DECOYS_ENABLED_OPTION,
        EXPERIMENTAL_STRONG_SKIPPING_OPTION,
        STRONG_SKIPPING_OPTION,
        STABLE_CONFIG_PATH_OPTION,
        TRACE_MARKERS_OPTION,
        FEATURE_FLAG_OPTION,
        SKIP_IR_LOWERING_IF_RUNTIME_NOT_FOUND_OPTION,
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = when (option) {
        LIVE_LITERALS_ENABLED_OPTION -> configuration.put(
            ComposeConfiguration.LIVE_LITERALS_ENABLED_KEY,
            value == "true"
        )
        LIVE_LITERALS_V2_ENABLED_OPTION -> configuration.put(
            ComposeConfiguration.LIVE_LITERALS_V2_ENABLED_KEY,
            value == "true"
        )
        GENERATE_FUNCTION_KEY_META_CLASSES_OPTION -> configuration.put(
            ComposeConfiguration.GENERATE_FUNCTION_KEY_META_CLASSES_KEY,
            value == "true"
        )
        SOURCE_INFORMATION_ENABLED_OPTION -> configuration.put(
            ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY,
            value == "true"
        )
        METRICS_DESTINATION_OPTION -> configuration.put(
            ComposeConfiguration.METRICS_DESTINATION_KEY,
            value
        )
        REPORTS_DESTINATION_OPTION -> configuration.put(
            ComposeConfiguration.REPORTS_DESTINATION_KEY,
            value
        )
        INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_OPTION -> {
            oldOptionDeprecationWarning(
                configuration,
                INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_OPTION,
                FeatureFlag.IntrinsicRemember
            )
            configuration.put(
                ComposeConfiguration.INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_KEY,
                value == "true"
            )
        }
        NON_SKIPPING_GROUP_OPTIMIZATION_ENABLED_OPTION -> {
            oldOptionDeprecationWarning(
                configuration,
                NON_SKIPPING_GROUP_OPTIMIZATION_ENABLED_OPTION,
                FeatureFlag.OptimizeNonSkippingGroups
            )
            configuration.put(
                ComposeConfiguration.NON_SKIPPING_GROUP_OPTIMIZATION_ENABLED_KEY,
                value == "true"
            )
        }
        SUPPRESS_KOTLIN_VERSION_CHECK_ENABLED_OPTION -> configuration.put(
            ComposeConfiguration.SUPPRESS_KOTLIN_VERSION_COMPATIBILITY_CHECK,
            value
        )
        DECOYS_ENABLED_OPTION -> configuration.put(
            ComposeConfiguration.DECOYS_ENABLED_KEY,
            value == "true"
        )
        EXPERIMENTAL_STRONG_SKIPPING_OPTION -> {
            oldOptionDeprecationWarning(
                configuration,
                EXPERIMENTAL_STRONG_SKIPPING_OPTION,
                FeatureFlag.StrongSkipping
            )
            configuration.put(
                ComposeConfiguration.STRONG_SKIPPING_ENABLED_KEY,
                value == "true"
            )
        }
        STRONG_SKIPPING_OPTION -> {
            oldOptionDeprecationWarning(
                configuration,
                EXPERIMENTAL_STRONG_SKIPPING_OPTION,
                FeatureFlag.StrongSkipping
            )
            configuration.put(
                ComposeConfiguration.STRONG_SKIPPING_ENABLED_KEY,
                value == "true"
            )
        }
        STABLE_CONFIG_PATH_OPTION -> configuration.appendList(
            ComposeConfiguration.STABILITY_CONFIG_PATH_KEY,
            value
        )
        TRACE_MARKERS_OPTION -> configuration.put(
            ComposeConfiguration.TRACE_MARKERS_ENABLED_KEY,
            value == "true"
        )
        FEATURE_FLAG_OPTION -> {
            validateFeatureFlag(configuration, value)
            configuration.appendList(
                ComposeConfiguration.FEATURE_FLAGS,
                value
            )
        }
        SKIP_IR_LOWERING_IF_RUNTIME_NOT_FOUND_OPTION -> configuration.put(
            ComposeConfiguration.SKIP_IR_LOWERING_IF_RUNTIME_NOT_FOUND_KEY,
            value == "true"
        )
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}

/**
 * A list of features that can be enabled through the "featureFlags" option.
 *
 * Features should be added to this list if they are intended to eventually become the default
 * behavior of the compiler. This is intended to allow progressive roll-out of a feature to
 * facilitate coordinating the runtime and compiler changes. New features should be disabled
 * by default until it is validated to be ready for production after testing with the corresponding
 * changes needed in the runtime. Using this technique does not remove the need to feature detect
 * for the version of runtime and is only intended to disable the feature even if the feature is
 * detected in the runtime.
 *
 * If a feature default is `true` the feature is reported as known by the command-line processor
 * but will generate a warning that the option is no longer necessary as it is the default. If
 * the feature is not in this list a warning is produced instead of an error to facilitate moving
 * compiler versions without having to always remove features unknown to older versions of the
 * plugin.
 *
 * A feature flag enum value can be used in the transformers that derive from
 * AbstractComposeLowering by using the FeatureFlag.enabled extension property. For example
 * testing if StrongSkipping is enabled can be checked by checking
 *
 *   FeatureFlag.StrongSkipping.enabled
 *
 * The `default` field is the source of truth for the default of the property. Turning it
 * to `true` here will make it default on even if the value was previous enabled through
 * a deprecated explicit option.
 *
 * A feature can be explicitly disabled by prefixing the feature name with "-" even if
 * the feature is enabled by default.
 *
 * @param featureName The name of the feature that is used with featureFlags to enable or disable
 *   the feature.
 * @param default True if the feature is enabled by default or false if it is not.
 */
enum class FeatureFlag(val featureName: String, val default: Boolean) {
    StrongSkipping("StrongSkipping", default = false),
    IntrinsicRemember("IntrinsicRemember", default = true),
    OptimizeNonSkippingGroups("OptimizeNonSkippingGroups", default = false);

    val disabledName get() = "-$featureName"
    fun name(enabled: Boolean) = if (enabled) featureName else disabledName

    companion object {
        fun fromString(featureName: String): Pair<FeatureFlag?, Boolean> {
            val (featureToSearch, enabled) = when {
                featureName.startsWith("+") -> featureName.substring(1) to true
                featureName.startsWith("-") -> featureName.substring(1) to false
                else -> featureName to true
            }
            return FeatureFlag.values().firstOrNull {
                featureToSearch.trim().compareTo(it.featureName, ignoreCase = true) == 0
            } to enabled
        }
    }
}

class FeatureFlags(featureConfiguration: List<String> = emptyList()) {
    private val setForCompatibility = mutableSetOf<FeatureFlag>()
    private val duplicate = mutableSetOf<FeatureFlag>()
    private val enabledFeatures = mutableSetOf<FeatureFlag>()
    private val disabledFeatures = mutableSetOf<FeatureFlag>()

    init {
        processConfigurationList(featureConfiguration)
    }

    private fun enableFeature(feature: FeatureFlag) {
        if (feature in disabledFeatures) {
            duplicate.add(feature)
            disabledFeatures.remove(feature)
        }
        enabledFeatures.add(feature)
    }

    private fun disableFeature(feature: FeatureFlag) {
        if (feature in enabledFeatures) {
            duplicate.add(feature)
            enabledFeatures.remove(feature)
        }
        disabledFeatures.add(feature)
    }

    fun setFeature(feature: FeatureFlag, value: Boolean) {
        if (feature.default != value) {
            setForCompatibility.add(feature)
            if (value) enableFeature(feature) else disableFeature(feature)
        }
    }

    fun isEnabled(feature: FeatureFlag) = feature in enabledFeatures || (feature.default &&
        feature !in disabledFeatures)

    private fun processConfigurationList(featuresNames: List<String>) {
        for (featureName in featuresNames) {
            val (feature, enabled) = FeatureFlag.fromString(featureName)
            if (feature != null) {
                if (enabled) enableFeature(feature) else disableFeature(feature)
            }
        }
    }

    fun validateFeatureFlags(configuration: CompilerConfiguration) {
        val msgCollector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        if (msgCollector != null) {
            val reported = mutableSetOf<FeatureFlag>()
            fun report(feature: FeatureFlag, message: String) {
                if (feature !in reported) {
                    reported.add(feature)
                    msgCollector.report(
                        CompilerMessageSeverity.WARNING,
                        message
                    )
                }
            }
            val configured = enabledFeatures + disabledFeatures
            val oldAndNewSet = setForCompatibility.intersect(configured)
            for (feature in oldAndNewSet) {
                report(
                    feature,
                    "Feature ${featureFlagName()}=${feature.featureName} is using featureFlags " +
                        "and is set using the deprecated option. It is recommended to only use " +
                        "featureFlag. ${currentState(feature)}"
                )
            }
            for (feature in duplicate) {
                if (feature !in reported) {
                    report(
                        feature,
                        "Feature ${featureFlagName()}=${feature.featureName} was both enabled " +
                            "and disabled. ${currentState(feature)}"
                    )
                }
            }
            for (feature in disabledFeatures) {
                if (!feature.default) {
                    report(
                        feature,
                        "The feature ${featureFlagName()}=${feature.featureName} is disabled " +
                        "by default and specifying this option explicitly is not necessary."
                    )
                }
            }
            for (feature in enabledFeatures) {
                if (feature.default) {
                    report(
                        feature,
                        "The feature ${featureFlagName()}=${feature.featureName} is enabled " +
                        "by default and specifying this option explicitly is not necessary."
                    )
                }
            }
        }
    }

    private fun currentState(feature: FeatureFlag): String =
        "With the given options set, the feature is ${
            if (isEnabled(feature)) "enabled" else "disabled"
        }"
}

fun featureFlagName() =
    "plugin:${ComposeCommandLineProcessor.PLUGIN_ID}:${
        ComposeCommandLineProcessor.FEATURE_FLAG_OPTION.optionName
    }"

fun useFeatureFlagInsteadMessage(feature: FeatureFlag) = "Use " +
    "${featureFlagName()}=${feature.featureName} instead"

fun oldOptionDeprecationWarning(
    configuration: CompilerConfiguration,
    oldOption: AbstractCliOption,
    feature: FeatureFlag
) {
    configuration.messageCollector.report(
        CompilerMessageSeverity.WARNING,
        "${oldOption.optionName} is deprecated. ${useFeatureFlagInsteadMessage(feature)}"
    )
}

fun validateFeatureFlag(
    configuration: CompilerConfiguration,
    value: String
) {
    val (feature, _) = FeatureFlag.fromString(value)
    if (feature == null) {
        configuration.messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "${featureFlagName()} contains an unrecognized feature name: $value."
        )
    }
}

@Suppress("DEPRECATION") // CompilerPluginRegistrar does not expose project (or disposable) causing
                         // memory leaks, see: https://youtrack.jetbrains.com/issue/KT-60952
@OptIn(ExperimentalCompilerApi::class)
class ComposePluginRegistrar : org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar {
    override val supportsK2: Boolean
        get() = true

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        if (checkCompilerVersion(configuration)) {
            val usesK2 = configuration.languageVersionSettings.languageVersion.usesK2
            val descriptorSerializerContext =
                if (usesK2) null
                else ComposeDescriptorSerializerContext()

            registerCommonExtensions(project, descriptorSerializerContext)

            IrGenerationExtension.registerExtension(
                project,
                createComposeIrExtension(
                    configuration,
                    descriptorSerializerContext
                )
            )

            if (!usesK2) {
                registerNativeExtensions(project, descriptorSerializerContext!!)
            }
        }
    }

    companion object {
        fun checkCompilerVersion(configuration: CompilerConfiguration): Boolean {
            val msgCollector = configuration.messageCollector
            val suppressKotlinVersionCheck = configuration.get(ComposeConfiguration.SUPPRESS_KOTLIN_VERSION_COMPATIBILITY_CHECK)
            if (suppressKotlinVersionCheck != null) {
                msgCollector.report(
                    CompilerMessageSeverity.WARNING,
                    "suppressKotlinVersionCompatibilityCheck flag is deprecated for Compose compiler bundled with Kotlin releases."
                )
            }

            val decoysEnabled =
                configuration.get(ComposeConfiguration.DECOYS_ENABLED_KEY, false)
            if (decoysEnabled) {
                msgCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Decoys generation should be disabled for Compose Multiplatform projects"
                )
                return false
            }
            return true
        }

        fun registerCommonExtensions(
            project: Project,
            composeDescriptorSerializerContext: ComposeDescriptorSerializerContext? = null
        ) {
            StorageComponentContainerContributor.registerExtension(
                project,
                ComposableCallChecker()
            )
            StorageComponentContainerContributor.registerExtension(
                project,
                ComposableDeclarationChecker()
            )
            StorageComponentContainerContributor.registerExtension(
                project,
                ComposableTargetChecker()
            )
            DiagnosticSuppressor.registerExtension(project, ComposeDiagnosticSuppressor())
            @Suppress("OPT_IN_USAGE_ERROR")
            TypeResolutionInterceptor.registerExtension(
                project,
                ComposeTypeResolutionInterceptorExtension()
            )
            DescriptorSerializerPlugin.registerExtension(
                project,
                ClassStabilityFieldSerializationPlugin(
                    composeDescriptorSerializerContext?.classStabilityInferredCollection
                )
            )
            FirExtensionRegistrarAdapter.registerExtension(project, ComposeFirExtensionRegistrar())
        }

        fun registerNativeExtensions(
            project: Project,
            composeDescriptorSerializerContext: ComposeDescriptorSerializerContext
        ) {
            DescriptorSerializerPlugin.registerExtension(
                project,
                AddHiddenFromObjCSerializationPlugin(
                    composeDescriptorSerializerContext.hideFromObjCDeclarationsSet
                )
            )
        }

        fun createComposeIrExtension(
            configuration: CompilerConfiguration,
            descriptorSerializerContext: ComposeDescriptorSerializerContext? = null,
            moduleMetricsFactory: ((StabilityInferencer) -> ModuleMetrics)? = null
        ): ComposeIrGenerationExtension {
            val liveLiteralsEnabled = configuration.getBoolean(
                ComposeConfiguration.LIVE_LITERALS_ENABLED_KEY,
            )
            val liveLiteralsV2Enabled = configuration.getBoolean(
                ComposeConfiguration.LIVE_LITERALS_V2_ENABLED_KEY,
            )
            val generateFunctionKeyMetaClasses = configuration.getBoolean(
                ComposeConfiguration.GENERATE_FUNCTION_KEY_META_CLASSES_KEY,
            )
            val sourceInformationEnabled = configuration.getBoolean(
                ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY,
            )
            val intrinsicRememberEnabled = configuration.get(
                ComposeConfiguration.INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_KEY,
                FeatureFlag.IntrinsicRemember.default
            )
            val nonSkippingGroupOptimizationEnabled = configuration.get(
                ComposeConfiguration.NON_SKIPPING_GROUP_OPTIMIZATION_ENABLED_KEY,
                FeatureFlag.OptimizeNonSkippingGroups.default
            )
            val decoysEnabled = configuration.getBoolean(
                ComposeConfiguration.DECOYS_ENABLED_KEY,
            )
            val metricsDestination = configuration.get(
                ComposeConfiguration.METRICS_DESTINATION_KEY,
                ""
            ).ifBlank { null }
            val reportsDestination = configuration.get(
                ComposeConfiguration.REPORTS_DESTINATION_KEY,
                ""
            ).ifBlank { null }
            val irVerificationMode = configuration.get(CommonConfigurationKeys.VERIFY_IR, IrVerificationMode.NONE)

            val useK2 = configuration.languageVersionSettings.languageVersion.usesK2

            val strongSkippingEnabled = configuration.get(
                ComposeConfiguration.STRONG_SKIPPING_ENABLED_KEY,
                FeatureFlag.StrongSkipping.default
            )

            val stabilityConfigPaths = configuration.getList(
                ComposeConfiguration.STABILITY_CONFIG_PATH_KEY
            )
            val traceMarkersEnabled = configuration.get(
                ComposeConfiguration.TRACE_MARKERS_ENABLED_KEY,
                true
            )

            val skipIrLoweringIfRuntimeNotFound = configuration.getBoolean(
                ComposeConfiguration.SKIP_IR_LOWERING_IF_RUNTIME_NOT_FOUND_KEY,
            )

            val featureFlags = FeatureFlags(
                configuration.get(
                    ComposeConfiguration.FEATURE_FLAGS, emptyList()
                )
            )
            featureFlags.validateFeatureFlags(configuration)

            // Compatibility with older features configuration options
            // New features should not create a explicit option
            featureFlags.setFeature(FeatureFlag.IntrinsicRemember, intrinsicRememberEnabled)
            featureFlags.setFeature(FeatureFlag.StrongSkipping, strongSkippingEnabled)
            featureFlags.setFeature(
                FeatureFlag.OptimizeNonSkippingGroups,
                nonSkippingGroupOptimizationEnabled
            )

            val stableTypeMatchers = mutableSetOf<FqNameMatcher>()
            for (i in stabilityConfigPaths.indices) {
                val path = stabilityConfigPaths[i]
                val matchers = try {
                    StabilityConfigParser.fromFile(path).stableTypeMatchers
                } catch (e: Exception) {
                    configuration.messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        e.message ?: "Error parsing stability configuration at $path"
                    )
                    emptySet()
                }
                stableTypeMatchers.addAll(matchers)
            }
            val testingMatchers = configuration.get(ComposeConfiguration.TEST_STABILITY_CONFIG_KEY)
                ?.map { FqNameMatcher(it) }
                ?: emptySet()
            stableTypeMatchers.addAll(testingMatchers)

            return ComposeIrGenerationExtension(
                liveLiteralsEnabled = liveLiteralsEnabled,
                liveLiteralsV2Enabled = liveLiteralsV2Enabled,
                generateFunctionKeyMetaClasses = generateFunctionKeyMetaClasses,
                sourceInformationEnabled = sourceInformationEnabled,
                traceMarkersEnabled = traceMarkersEnabled,
                decoysEnabled = decoysEnabled,
                metricsDestination = metricsDestination,
                reportsDestination = reportsDestination,
                irVerificationMode = irVerificationMode,
                useK2 = useK2,
                stableTypeMatchers = stableTypeMatchers,
                moduleMetricsFactory = moduleMetricsFactory,
                descriptorSerializerContext = descriptorSerializerContext,
                featureFlags = featureFlags,
                skipIfRuntimeNotFound = skipIrLoweringIfRuntimeNotFound,
                messageCollector = configuration.messageCollector,
            )
        }
    }
}
