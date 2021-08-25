/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.compiler

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.project.modelx.*

/**
 * API surface of KPM for Build System tool that works with CLI Compiler Arguments
 * This adapter is considered to be aware of context of some [KotlinModule]
 */
interface KpmBuildSystemAdapter {
    /**
     * The [KotlinModule] for which the given instance of [KpmBuildSystemAdapter] is configured
     */
    val module: KotlinModule

    /**
     *  Some BuildSystems can provide its own VariantMatching functionalities.
     *  They have to respect Kotlin [Attribute] relations anyway.
     *
     *  By default [DefaultVariantMatcher] is used
     */
    val variantMatcher: KpmDependencyExpansion.VariantMatcher get() = DefaultVariantMatcher(module, this::dependencyModule)

    /**
     * Build system adapter should be able to locate and extract dependency [KotlinModule] by its [moduleId]
     *
     * Invariants:
     *  * [moduleId] must be present in [Fragment.moduleDependencies] of at least one [Fragment] from [KotlinModule.fragments]
     */
    fun dependencyModule(moduleId: ModuleId): KotlinModule

    /**
     * Returns source paths of a fragment
     */
    fun fragmentSources(fragmentId: FragmentId): List<String>

    /**
     * Returns classpath to metadata compilation output of given [fragmentId]
     */
    fun fragmentMetadataClasspath(fragmentId: FragmentId): String

    /**
     * Returns classpath to metadata classes of external [fragmentDependency]
     */
    fun fragmentDependencyMetadataArtifacts(fragmentDependency: FragmentDependency): List<String>

    /**
     * Returns classpath to complete variant artifact and its complete dependencies
     */
    fun variantDependencyArtifacts(fragmentDependency: FragmentDependency): List<String>
}

/**
 * TODO: maybe interface?
 */
class Compilers(
    val compileMetadata: (K2MetadataCompilerArguments) -> ExitCode,
    val compileJvm: (K2JVMCompilerArguments) -> ExitCode,
    val compileJs: (K2JSCompilerArguments) -> ExitCode,
    // val compileNative: (K2NativeCompilerArguments) -> ExitCode,
)

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