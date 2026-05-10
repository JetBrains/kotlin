/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputsBuilt
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilerResult
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.config.TsCompilationStrategy
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.JsIrArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.finalizePath
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerSecondStageFacade] for JS.
 */
class CustomJsCompilerSecondStageFacade(
    testServices: TestServices
) : CustomKlibCompilerSecondStageFacade<BinaryArtifacts.Js>(testServices) {

    override val outputKind get() = ArtifactKinds.Js

    override fun isMainModule(module: TestModule) = module == JsEnvironmentConfigurator.getMainModule(testServices)
    override fun collectDependencies(module: TestModule) = module.collectDependencies(testServices, CompilationStage.SECOND)

    override fun compileBinary(
        module: TestModule,
        customArgs: List<String>,
        mainLibrary: String,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): BinaryArtifacts.Js {
        val jsArtifactFile = File(
            JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name).finalizePath(
                JsEnvironmentConfigurator.getModuleKind(
                    testServices,
                    module
                )
            )
        )

        val compilerXmlOutput = ByteArrayOutputStream()

        val outputDir = jsArtifactFile.parentFile
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val regularAndFriendDependencies = regularDependencies + friendDependencies
            customJsCompilerSettings.customKlibCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    K2JSCompilerArguments::irProduceJs.cliArgument,
                    K2JSCompilerArguments::sourceMap.cliArgument,
                    K2JSCompilerArguments::sourceMapEmbedSources.cliArgument(SOURCE_MAP_SOURCE_CONTENT_NEVER),
                    K2JSCompilerArguments::includes.cliArgument(mainLibrary),

                    K2JSCompilerArguments::outputDir.cliArgument, outputDir.path,
                    K2JSCompilerArguments::moduleName.cliArgument, module.name,
                    CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument,
                ),
                runIf(regularAndFriendDependencies.isNotEmpty()) {
                    listOf(
                        K2JSCompilerArguments::libraries.cliArgument,
                        regularAndFriendDependencies.joinToString(File.pathSeparator),
                    )
                },
                runIf(friendDependencies.isNotEmpty()) {
                    listOf(K2JSCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator)))
                },
                customArgs,
            )
        }

        if (exitCode == ExitCode.OK) {
            // Successfully compiled. Return the artifact.
            File(outputDir.path, module.name + ".js")
                .renameFollowingTestInfraConvention(jsArtifactFile)

            return JsIrArtifact(
                outputFile = jsArtifactFile,
                compilerResult = CompilerResult(
                    listOf(
                        CompilationOutputsBuilt(
                            artifactConfiguration = WebArtifactConfiguration(
                                moduleKind = ModuleKind.PLAIN,
                                moduleName = module.name,
                                outputDirectory = outputDir,
                                outputName = module.name,
                                granularity = JsGenerationGranularity.WHOLE_PROGRAM,
                                tsCompilationStrategy = TsCompilationStrategy.NONE,
                                production = false,
                                minimizedMemberNames = false,
                            ),
                            rawJsCode = jsArtifactFile.readText(),
                            sourceMap = null,
                            tsDefinitions = null,
                            jsProgram = null,
                        )
                    )
                )
            )
        } else {
            // Throw an exception to abort further test execution.
            throw CustomKlibCompilerException(exitCode, compilerXmlOutput.toString(Charsets.UTF_8.name()))
        }
    }

    // CLI compilation creates an output file of different name than testInfra usually does.
    // A rename is needed, in order the testInfra runner can use its usual logic to run the test script.
    private fun File.renameFollowingTestInfraConvention(outputFile: File) {
        require(exists()) {
            "Internal testinfra error: Couldn't find expected generated js script ${absolutePath}"
        }

        renameTo(outputFile)

        // Move SourceMap file as well
        with(withExtension(".js.map")) {
            require(exists()) {
                "Internal testinfra error: Couldn't find expected generated js source map ${absolutePath}"
            }

            renameTo(outputFile.withExtension(".js.map"))
        }
    }
}
