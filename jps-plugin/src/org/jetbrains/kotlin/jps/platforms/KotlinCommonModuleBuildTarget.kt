/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.platforms

import com.intellij.util.containers.MultiMap
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.DirtyFilesHolder
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import com.intellij.openapi.compiler.CompileContext as JpsCompileContext
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.FSOperations
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.fs.CompilationRound
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.jps.build.FSOperationsHelper
import org.jetbrains.kotlin.jps.model.k2MetadataCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinCompilerSettings
import java.io.File

class KotlinCommonModuleBuildTarget(compileContext: CompileContext, jpsModuleBuildTarget: ModuleBuildTarget) :
    KotlinModuleBuilderTarget(compileContext, jpsModuleBuildTarget) {

    override fun compileModuleChunk(
        allCompiledFiles: MutableSet<File>,
        chunk: ModuleChunk,
        commonArguments: CommonCompilerArguments,
        dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
        environment: JpsCompilerEnvironment,
        filesToCompile: MultiMap<ModuleBuildTarget, File>,
        fsOperations: FSOperationsHelper
    ): Boolean {
        require(chunk.representativeTarget() == jpsModuleBuildTarget)
        if (reportAndSkipCircular(chunk, environment)) return false

        // Incremental compilation is not supported, so mark all dependents as dirty
        FSOperations.markDirtyRecursively(context, CompilationRound.CURRENT, chunk)

        JpsKotlinCompilerRunner().runK2MetadataCompiler(
            commonArguments,
            module.k2MetadataCompilerArguments,
            module.kotlinCompilerSettings,
            environment,
            sources
        )

        return true
    }
}