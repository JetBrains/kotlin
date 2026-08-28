/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi.wasm

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain.Companion.wasm
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmLinkingOperation
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.btapi.BuildOperationFactory
import org.jetbrains.kotlin.compilerRunner.btapi.extractSourceFiles
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import kotlin.io.path.Path

internal class WasmKlibBuildOperationFactory(private val compilerArgs: List<String>) :
    BuildOperationFactory<WasmKlibCompilationOperation.Builder> {
    override fun createOperation(kotlinToolchains: KotlinToolchains): WasmKlibCompilationOperation.Builder {
        /*
         * GradleCompilerRunner.runCompilerAsync transforms arguments adding the freeArgs separator (`--`)
         * This way, even incorrect arguments are surviving the `parseCommandLineArguments` call and can be passed to BTA.
         * If you rework this, please make sure that the freeArgs separator is not reaching BTA.
         */
        val args: KotlinWasmCompilerArguments = parseCommandLineArguments(compilerArgs)
        val destination = Path(requireNotNull(args.outputDir))
        val compilationOperationBuilder =
            kotlinToolchains.wasm.wasmKlibCompilationOperationBuilder(extractSourceFiles(args.freeArgs), destination)
        compilationOperationBuilder.compilerArguments.applyArgumentStrings(
            args.toArgumentStrings(
                allowArgFileInValues = false
            )
        )
        return compilationOperationBuilder
    }
}

internal class WasmLinkingBuildOperationFactory(private val compilerArgs: List<String>) : BuildOperationFactory<WasmLinkingOperation.Builder> {
    override fun createOperation(kotlinToolchains: KotlinToolchains): WasmLinkingOperation.Builder {
        /*
         * GradleCompilerRunner.runCompilerAsync transforms arguments adding the freeArgs separator (`--`)
         * This way, even incorrect arguments are surviving the `parseCommandLineArguments` call (by staying in `args.freeArgs`)
         * and can be passed to BTA.
         * If you rework this, please make sure that the freeArgs separator is not reaching BTA.
         */
        val args: KotlinWasmCompilerArguments = parseCommandLineArguments(compilerArgs)
        val destination = Path(requireNotNull(args.outputDir))
        val includes = Path(requireNotNull(args.includes))
        val compilationOperationBuilder = kotlinToolchains.wasm.wasmLinkingOperationBuilder(includes, destination)
        compilationOperationBuilder.compilerArguments.applyArgumentStrings(
            args.toArgumentStrings(
                allowArgFileInValues = false
            )
        )
        return compilationOperationBuilder
    }
}
