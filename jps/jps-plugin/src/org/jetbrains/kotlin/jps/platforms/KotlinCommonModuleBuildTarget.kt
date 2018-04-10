/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.platforms

import com.intellij.util.containers.MultiMap
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.DirtyFilesHolder
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.jps.model.k2MetadataCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinCompilerSettings
import java.io.File

class KotlinCommonModuleBuildTarget(jpsModuleBuildTarget: ModuleBuildTarget) :
    KotlinModuleBuilderTarget(jpsModuleBuildTarget) {

    override fun compileModuleChunk(
        allCompiledFiles: MutableSet<File>,
        chunk: ModuleChunk,
        commonArguments: CommonCompilerArguments,
        context: CompileContext,
        dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
        environment: JpsCompilerEnvironment,
        filesToCompile: MultiMap<ModuleBuildTarget, File>
    ): Boolean {
        require(chunk.representativeTarget() == jpsModuleBuildTarget)
        if (reportAndSkipCircular(chunk, environment)) return false

        JpsKotlinCompilerRunner().runK2MetadataCompiler(
            commonArguments,
            module.k2MetadataCompilerArguments,
            module.kotlinCompilerSettings,
            environment,
            sources
        )

        // TODO: Compile metadata
        return false
    }
}