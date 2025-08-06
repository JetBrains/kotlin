/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.ir.backend.js.CompilerResult
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputsBuilt
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.test.converters.finalizePath
import org.jetbrains.kotlin.js.test.converters.kind
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondPhaseFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerSecondPhaseFacade] for JS.
 */
class CustomJsCompilerSecondPhaseFacade(
    testServices: TestServices
) : CustomKlibCompilerSecondPhaseFacade<BinaryArtifacts.Js>(testServices) {

    override val outputKind get() = ArtifactKinds.Js

    override fun isMainModule(module: TestModule) = module == JsEnvironmentConfigurator.getMainModule(testServices)
    override fun collectDependencies(module: TestModule) = module.collectDependencies(testServices)

    override fun compileBinary(
        module: TestModule,
        customArgs: List<String>,
        mainLibrary: String,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): BinaryArtifacts.Js {
        val jsArtifactFile = File(JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name).finalizePath(module.kind))

        val compilerXmlOutput = ByteArrayOutputStream()

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            customJsCompilerSettings.customCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    K2JSCompilerArguments::irProduceJs.cliArgument,
                    K2JSCompilerArguments::includes.cliArgument(mainLibrary),

                    K2JSCompilerArguments::outputDir.cliArgument, jsArtifactFile.parentFile.path,
                    K2JSCompilerArguments::moduleName.cliArgument, jsArtifactFile.nameWithoutExtension,
                ),
                runIf(regularDependencies.isNotEmpty()) {
                    listOf(
                        K2JSCompilerArguments::libraries.cliArgument,
                        regularDependencies.joinToString(File.pathSeparator),
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
            return BinaryArtifacts.Js.JsIrArtifact(
                outputFile = jsArtifactFile,
                compilerResult = CompilerResult(
                    mapOf(
                        TranslationMode.FULL_DEV to CompilationOutputsBuilt(
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
}
