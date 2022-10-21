/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.compiler

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.project.modelx.*

/**
 * TODO: maybe interface?
 */
class Compilers(
    val compileMetadata: (K2MetadataCompilerArguments) -> ExitCode,
    val compileJvm: (K2JVMCompilerArguments) -> ExitCode,
    val compileJs: (K2JSCompilerArguments) -> ExitCode,
    // val compileNative: (K2NativeCompilerArguments) -> ExitCode,
)

/**
 * Implementation of [KpmCompiler] that work on top of K2*Compiler's
 * provided via [compilers]
 */
class KpmCompiler(
    private val compilers: Compilers,
    private val compilationProcessor: KpmCompilationProcessor,
    private val argumentsMapper: KPMCompilerArgumentsMapper
) {
    /**
     * Returns [null] when compilation is avoided
     */
    fun compileMetadata(fragmentId: FragmentId): ExitCode? {
        val compilationData = compilationProcessor.metadataCompilation(fragmentId) ?: return null
        val compilerArguments = argumentsMapper.metadataArguments(compilationData)
        return compilers.compileMetadata(compilerArguments)
    }

    fun compileVariant(variantId: String): ExitCode {
        val data = compilationProcessor.compileVariant(variantId)
        val compilerArguments = argumentsMapper.arguments(data)

        return when (compilerArguments) {
            is K2JVMCompilerArguments -> compilers.compileJvm(compilerArguments)
            is K2JSCompilerArguments -> compilers.compileJs(compilerArguments)
            else -> error("$compilerArguments is not supported")
        }
    }
}