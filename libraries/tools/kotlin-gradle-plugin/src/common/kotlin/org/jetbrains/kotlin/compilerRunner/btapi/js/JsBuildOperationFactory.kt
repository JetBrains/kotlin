/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi.js

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.btapi.BuildOperationFactory
import org.jetbrains.kotlin.compilerRunner.btapi.extractSourceFiles
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import kotlin.io.path.Path

internal class JsKlibBuildOperationFactory(private val compilerArgs: List<String>) :
    BuildOperationFactory<JsKlibCompilationOperation.Builder> {
    override fun createOperation(kotlinToolchains: KotlinToolchains): JsKlibCompilationOperation.Builder {
        /*
         * GradleCompilerRunner.runCompilerAsync transforms arguments adding the freeArgs separator (`--`)
         * This way, even incorrect arguments are surviving the `parseCommandLineArguments` call (by staying in `args.freeArgs`)
         * and can be passed to BTA.
         * If you rework this, please make sure that the freeArgs separator is not reaching BTA.
         */
        val args: K2JSCompilerArguments = parseCommandLineArguments(compilerArgs)
        val destination = Path(requireNotNull(args.outputDir))
        val compilationOperationBuilder =
            kotlinToolchains.js.jsKlibCompilationOperationBuilder(extractSourceFiles(args.freeArgs), destination)

        args.outputDir = null
        @Suppress("DEPRECATION")
        args.irProduceKlibDir = null
        @Suppress("DEPRECATION")
        args.irProduceKlibFile = null

        compilationOperationBuilder.compilerArguments.applyArgumentStrings(
            args.toArgumentStrings(
                allowArgFileInValues = false
            )
        )
        return compilationOperationBuilder
    }
}

internal class JsLinkingBuildOperationFactory(private val compilerArgs: List<String>) : BuildOperationFactory<JsLinkingOperation.Builder> {
    override fun createOperation(kotlinToolchains: KotlinToolchains): JsLinkingOperation.Builder {
        /*
         * GradleCompilerRunner.runCompilerAsync transforms arguments adding the freeArgs separator (`--`)
         * This way, even incorrect arguments are surviving the `parseCommandLineArguments` call (by staying in `args.freeArgs`)
         * and can be passed to BTA.
         * If you rework this, please make sure that the freeArgs separator is not reaching BTA.
         */
        val args: K2JSCompilerArguments = parseCommandLineArguments(compilerArgs)
        val destination = Path(requireNotNull(args.outputDir))
        val includes = Path(requireNotNull(args.includes))
        val compilationOperationBuilder = kotlinToolchains.js.jsLinkingOperationBuilder(includes, destination)

        args.outputDir = null
        args.irProduceJs = K2JSCompilerArguments().irProduceJs
        args.includes = null

        compilationOperationBuilder.compilerArguments.applyArgumentStrings(
            args.toArgumentStrings(
                allowArgFileInValues = false
            )
        )
        return compilationOperationBuilder
    }
}
