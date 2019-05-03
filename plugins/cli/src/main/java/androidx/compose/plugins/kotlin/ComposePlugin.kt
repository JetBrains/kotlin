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

package androidx.compose.plugins.kotlin

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.backend.jvm.extensions.IrLoweringExtension
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.CallResolutionInterceptorExtension
import org.jetbrains.kotlin.extensions.KtxControlFlowExtension
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.parsing.KtxParsingExtension
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import androidx.compose.plugins.kotlin.ComposeConfigurationKeys.COMPOSABLE_CHECKER_MODE_KEY
import androidx.compose.plugins.kotlin.frames.analysis.FrameModelChecker
import androidx.compose.plugins.kotlin.frames.analysis.FramePackageAnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

object ComposeConfigurationKeys {
    val COMPOSABLE_CHECKER_MODE_KEY = CompilerConfigurationKey<ComposableAnnotationChecker.Mode>("@composable checker mode")
}

class ComposeCommandLineProcessor : CommandLineProcessor {

    companion object {
        val PLUGIN_ID = "androidx.compose.plugins.kotlin"
        val SYNTAX_OPTION = CliOption("syntax", "<ktx_checked|ktx_strict|ktx_pedantic|fcs>", "@composable syntax checker mode",
                                      required = false, allowMultipleOccurrences = false)
    }

    override val pluginId =
        PLUGIN_ID
    override val pluginOptions = listOf(SYNTAX_OPTION)

    @Suppress("OverridingDeprecatedMember")
    override fun processOption(
        option: CliOption,
        value: String,
        configuration: CompilerConfiguration
    ) =
        throw CliOptionProcessingException("Unknown option: ${option.optionName}")

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        SYNTAX_OPTION -> configuration.put(COMPOSABLE_CHECKER_MODE_KEY, ComposableAnnotationChecker.Mode.valueOf(value.toUpperCase()))
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

        fun registerProjectExtensions(project: Project, configuration: CompilerConfiguration) {
            StorageComponentContainerContributor.registerExtension(
                project,
                ComponentsClosedDeclarationChecker()
            )
            StorageComponentContainerContributor.registerExtension(
                project,
                ComposableAnnotationChecker(
                    configuration.get(
                        COMPOSABLE_CHECKER_MODE_KEY,
                        ComposableAnnotationChecker.DEFAULT_MODE
                    )
                )
            )
            StorageComponentContainerContributor.registerExtension(
                project,
                UnionAnnotationCheckerProvider()
            )
            KtxParsingExtension.registerExtension(project,
                ComposeKtxParsingExtension()
            )
            KtxTypeResolutionExtension.registerExtension(project,
                ComposeKtxTypeResolutionExtension()
            )
            KtxControlFlowExtension.registerExtension(project,
                ComposeKtxControlFlowExtension()
            )
            ComposeDiagnosticSuppressor.registerExtension(
                project,
                ComposeDiagnosticSuppressor()
            )
            TypeResolutionInterceptorExtension.registerExtension(
                project,
                ComposeTypeResolutionInterceptorExtension()
            )
            SyntheticIrExtension.registerExtension(project,
                ComposeSyntheticIrExtension()
            )
            IrLoweringExtension.registerExtension(project,
                ComposeIrLoweringExtension()
            )
            CallResolutionInterceptorExtension.registerExtension(
                project,
                ComposeCallResolutionInterceptorExtension()
            )

            StorageComponentContainerContributor.registerExtension(project,
                FrameModelChecker()
            )
            AnalysisHandlerExtension.registerExtension(
                project,
                FramePackageAnalysisHandlerExtension()
            )
        }
    }
}
