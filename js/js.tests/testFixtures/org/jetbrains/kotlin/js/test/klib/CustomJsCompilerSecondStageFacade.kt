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
        val moduleKind = JsEnvironmentConfigurator.getModuleKind(testServices, module)

        val finalJsArtifactFile = File(JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name).finalizePath(moduleKind))
        val outputDir: File = finalJsArtifactFile.parentFile
        val tempJsArtifactFile = outputDir.resolve(module.name.finalizePath(moduleKind))

        val compilerXmlOutput = ByteArrayOutputStream()

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val regularAndFriendDependencies = regularDependencies + friendDependencies
            customJsCompilerSettings.customKlibCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    K2JSCompilerArguments::irProduceJs.cliArgument,
                    K2JSCompilerArguments::sourceMap.cliArgument,
                    K2JSCompilerArguments::sourceMapEmbedSources.cliArgument(SOURCE_MAP_SOURCE_CONTENT_NEVER),
                    K2JSCompilerArguments::includes.cliArgument(mainLibrary),

                    K2JSCompilerArguments::moduleKind.cliArgument, moduleKind.type,
                    K2JSCompilerArguments::outputDir.cliArgument, outputDir.path,
                    K2JSCompilerArguments::moduleName.cliArgument, tempJsArtifactFile.nameWithoutExtension,
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
            tempJsArtifactFile.renameTo(finalJsArtifactFile)

            return JsIrArtifact(
                outputFile = finalJsArtifactFile,
                compilerResult = CompilerResult(
                    listOf(
                        CompilationOutputsBuilt(
                            artifactConfiguration = WebArtifactConfiguration(
                                moduleKind = moduleKind,
                                moduleName = module.name,
                                outputDirectory = outputDir,
                                outputName = finalJsArtifactFile.nameWithoutExtension,
                                granularity = JsGenerationGranularity.WHOLE_PROGRAM,
                                tsCompilationStrategy = TsCompilationStrategy.NONE,
                                production = false,
                                minimizedMemberNames = false,
                            ),
                            rawJsCode = finalJsArtifactFile.readText(),
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
}
