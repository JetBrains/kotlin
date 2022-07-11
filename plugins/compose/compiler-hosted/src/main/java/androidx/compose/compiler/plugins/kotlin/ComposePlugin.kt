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

import androidx.compose.compiler.plugins.kotlin.lower.ClassStabilityFieldSerializationPlugin
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptor
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
    val SUPPRESS_KOTLIN_VERSION_COMPATIBILITY_CHECK =
        CompilerConfigurationKey<Boolean>("Suppress Kotlin version compatibility check")
    val DECOYS_ENABLED_KEY =
        CompilerConfigurationKey<Boolean>("Generate decoy methods in IR transform")
}

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
        val INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_OPTION = CliOption(
            "intrinsicRemember",
            "<true|false>",
            "Include source information in generated code",
            required = false,
            allowMultipleOccurrences = false
        )
        val SUPPRESS_KOTLIN_VERSION_CHECK_ENABLED_OPTION = CliOption(
            "suppressKotlinVersionCompatibilityCheck",
            "<true|false>",
            "Suppress Kotlin version compatibility check",
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
        SUPPRESS_KOTLIN_VERSION_CHECK_ENABLED_OPTION,
        DECOYS_ENABLED_OPTION,
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
        INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_OPTION -> configuration.put(
            ComposeConfiguration.INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_KEY,
            value == "true"
        )
        SUPPRESS_KOTLIN_VERSION_CHECK_ENABLED_OPTION -> configuration.put(
            ComposeConfiguration.SUPPRESS_KOTLIN_VERSION_COMPATIBILITY_CHECK,
            value == "true"
        )
        DECOYS_ENABLED_OPTION -> configuration.put(
            ComposeConfiguration.DECOYS_ENABLED_KEY,
            value == "true"
        )
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}

class ComposeComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        registerProjectExtensions(
            project as Project,
            configuration
        )
    }

    companion object {

        @Suppress("UNUSED_PARAMETER")
        fun registerProjectExtensions(
            project: Project,
            configuration: CompilerConfiguration
        ) {
            val KOTLIN_VERSION_EXPECTATION = "1.7.10"
            KotlinCompilerVersion.getVersion()?.let { version ->
                val suppressKotlinVersionCheck = configuration.get(
                    ComposeConfiguration.SUPPRESS_KOTLIN_VERSION_COMPATIBILITY_CHECK,
                    false
                )
                if (!suppressKotlinVersionCheck && version != KOTLIN_VERSION_EXPECTATION) {
                    val msgCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
                    msgCollector?.report(
                        CompilerMessageSeverity.ERROR,
                        "This version (${VersionChecker.compilerVersion}) of the Compose" +
                            " Compiler requires Kotlin version $KOTLIN_VERSION_EXPECTATION but" +
                            " you appear to be using Kotlin version $version which is not known" +
                            " to be compatible.  Please fix your configuration (or" +
                            " `suppressKotlinVersionCompatibilityCheck` but don't say I didn't" +
                            " warn you!)."
                    )

                    // Return without registering the Compose plugin because the registration
                    // APIs may have changed and thus throw an exception during registration,
                    // preventing the diagnostic from being emitted.
                    return
                }
            }

            val liveLiteralsEnabled = configuration.get(
                ComposeConfiguration.LIVE_LITERALS_ENABLED_KEY,
                false
            )
            val liveLiteralsV2Enabled = configuration.get(
                ComposeConfiguration.LIVE_LITERALS_V2_ENABLED_KEY,
                false
            )
            val generateFunctionKeyMetaClasses = configuration.get(
                ComposeConfiguration.GENERATE_FUNCTION_KEY_META_CLASSES_KEY,
                false
            )
            val sourceInformationEnabled = configuration.get(
                ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY,
                false
            )
            val intrinsicRememberEnabled = configuration.get(
                ComposeConfiguration.INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_KEY,
                false
            )
            val decoysEnabled = configuration.get(
                ComposeConfiguration.DECOYS_ENABLED_KEY,
                false
            )
            val metricsDestination = configuration.get(
                ComposeConfiguration.METRICS_DESTINATION_KEY,
                ""
            ).let {
                if (it.isBlank()) null else it
            }
            val reportsDestination = configuration.get(
                ComposeConfiguration.REPORTS_DESTINATION_KEY,
                ""
            ).let {
                if (it.isBlank()) null else it
            }

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
            ComposeDiagnosticSuppressor.registerExtension(
                project,
                ComposeDiagnosticSuppressor()
            )
            @Suppress("OPT_IN_USAGE_ERROR")
            TypeResolutionInterceptor.registerExtension(
                project,
                @Suppress("IllegalExperimentalApiUsage")
                ComposeTypeResolutionInterceptorExtension()
            )
            IrGenerationExtension.registerExtension(
                project,
                ComposeIrGenerationExtension(
                    configuration = configuration,
                    liveLiteralsEnabled = liveLiteralsEnabled,
                    liveLiteralsV2Enabled = liveLiteralsV2Enabled,
                    generateFunctionKeyMetaClasses = generateFunctionKeyMetaClasses,
                    sourceInformationEnabled = sourceInformationEnabled,
                    intrinsicRememberEnabled = intrinsicRememberEnabled,
                    decoysEnabled = decoysEnabled,
                    metricsDestination = metricsDestination,
                    reportsDestination = reportsDestination,
                )
            )
            DescriptorSerializerPlugin.registerExtension(
                project,
                ClassStabilityFieldSerializationPlugin()
            )
        }
    }
}
