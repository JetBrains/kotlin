/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.targets

import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.build.CommonBuildMetaInfo
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.jps.build.KotlinCompileContext
import org.jetbrains.kotlin.jps.build.KotlinDirtySourceFilesHolder
import org.jetbrains.kotlin.jps.build.ModuleBuildTarget
import org.jetbrains.kotlin.jps.model.k2MetadataCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinCompilerSettings

private const val COMMON_BUILD_META_INFO_FILE_NAME = "common-build-meta-info.txt"

class KotlinCommonModuleBuildTarget(kotlinContext: KotlinCompileContext, jpsModuleBuildTarget: ModuleBuildTarget) :
    KotlinModuleBuildTarget<CommonBuildMetaInfo>(kotlinContext, jpsModuleBuildTarget) {

    override fun isEnabled(chunkCompilerArguments: CommonCompilerArguments): Boolean {
        val k2MetadataArguments = module.k2MetadataCompilerArguments
        return k2MetadataArguments.enabledInJps || (chunkCompilerArguments as? K2MetadataCompilerArguments)?.enabledInJps == true
    }

    override val isIncrementalCompilationEnabled: Boolean
        get() = false

    override val buildMetaInfoFactory
        get() = CommonBuildMetaInfo

    override val buildMetaInfoFileName
        get() = COMMON_BUILD_META_INFO_FILE_NAME

    override val globalLookupCacheId: String
        get() = "metadata-compiler"

    override fun compileModuleChunk(
        commonArguments: CommonCompilerArguments,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        environment: JpsCompilerEnvironment
    ): Boolean {
        require(chunk.representativeTarget == this)

        reportAndSkipCircular(environment)

        JpsKotlinCompilerRunner().runK2MetadataCompiler(
            commonArguments,
            module.k2MetadataCompilerArguments,
            module.kotlinCompilerSettings,
            environment,
            destination,
            dependenciesOutputDirs + libraryFiles,
            sourceFiles // incremental K2MetadataCompiler not supported yet
        )

        return true
    }

    private val libraryFiles: List<String>
        get() = mutableListOf<String>().also { result ->
            for (library in allDependencies.libraries) {
                for (root in library.getRoots(JpsOrderRootType.COMPILED)) {
                    result.add(JpsPathUtil.urlToPath(root.url))
                }
            }
        }

    private val dependenciesOutputDirs: List<String>
        get() = mutableListOf<String>().also { result ->
            allDependencies.processModules { module ->
                if (isTests) addDependencyMetaFile(module, result, isTests = true)

                // note: production targets should be also added as dependency to test targets
                addDependencyMetaFile(module, result, isTests = false)
            }
        }

    val destination: String
        get() = module.k2MetadataCompilerArguments.destination ?: outputDir.absolutePath

    private fun addDependencyMetaFile(
        module: JpsModule,
        result: MutableList<String>,
        isTests: Boolean
    ) {
        val dependencyBuildTarget = kotlinContext.targetsBinding[ModuleBuildTarget(module, isTests)]

        if (dependencyBuildTarget != this@KotlinCommonModuleBuildTarget &&
            dependencyBuildTarget is KotlinCommonModuleBuildTarget &&
            dependencyBuildTarget.sources.isNotEmpty()
        ) {
            result.add(dependencyBuildTarget.destination)
        }
    }

    override val hasCaches: Boolean
        get() = false

    override fun createCacheStorage(paths: BuildDataPaths) =
        error("incremental K2MetadataCompiler not supported yet, createCacheStorage() should not be called")
}