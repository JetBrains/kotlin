package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.backend.jvm.extensions.IrLoweringExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.CallResolutionInterceptorExtension
import org.jetbrains.kotlin.extensions.KtxControlFlowExtension
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.parsing.KtxParsingExtension
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.r4a.frames.analysis.FrameModelChecker
import org.jetbrains.kotlin.r4a.frames.analysis.FramePackageAnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

class R4ACommandLineProcessor : CommandLineProcessor {

    companion object {
        val PLUGIN_ID = "org.jetbrains.kotlin.r4a"
    }

    override val pluginId = PLUGIN_ID
    override val pluginOptions = emptyList<CliOption>()

    @Suppress("OverridingDeprecatedMember")
    override fun processOption(
        option: CliOption,
        value: String,
        configuration: CompilerConfiguration
    ) =
        throw CliOptionProcessingException("Unknown option: ${option.optionName}")
}

class R4AComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        registerProjectExtensions(project as Project, configuration)
    }

    companion object {
        val COMPOSABLE_CHECKER_MODE_KEY =
            CompilerConfigurationKey<ComposableAnnotationChecker.Mode>(
                "@composable checker mode"
            )

        fun registerProjectExtensions(project: Project, configuration: CompilerConfiguration) {
            StorageComponentContainerContributor.registerExtension(
                project,
                ComponentsClosedDeclarationChecker()
            )
            StorageComponentContainerContributor.registerExtension(
                project,
                ComposableAnnotationChecker(
                    configuration.get(COMPOSABLE_CHECKER_MODE_KEY,
                        ComposableAnnotationChecker.DEFAULT_MODE
                    )
                )
            )
            StorageComponentContainerContributor.registerExtension(
                project,
                UnionAnnotationCheckerProvider()
            )
            KtxParsingExtension.registerExtension(project, R4aKtxParsingExtension())
            KtxTypeResolutionExtension.registerExtension(project, R4aKtxTypeResolutionExtension())
            KtxControlFlowExtension.registerExtension(project, R4aKtxControlFlowExtension())
            R4aDiagnosticSuppressor.registerExtension(project, R4aDiagnosticSuppressor())
            TypeResolutionInterceptorExtension.registerExtension(
                project,
                R4aTypeResolutionInterceptorExtension()
            )
            SyntheticIrExtension.registerExtension(project, R4ASyntheticIrExtension())
            IrLoweringExtension.registerExtension(project, R4aIrLoweringExtension())
            CallResolutionInterceptorExtension.registerExtension(
                project,
                R4aCallResolutionInterceptorExtension()
            )

            StorageComponentContainerContributor.registerExtension(project, FrameModelChecker())
            AnalysisHandlerExtension.registerExtension(
                project,
                FramePackageAnalysisHandlerExtension()
            )
        }
    }
}
