/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.targets

import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.kotlin.build.BuildMetaInfo
import org.jetbrains.kotlin.build.BuildMetaInfoFactory
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.jps.build.KotlinCompileContext
import org.jetbrains.kotlin.jps.build.KotlinDirtySourceFilesHolder
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalCache
import org.jetbrains.kotlin.jps.model.platform
import org.jetbrains.kotlin.platform.idePlatformKind

class KotlinUnsupportedModuleBuildTarget(
    kotlinContext: KotlinCompileContext,
    jpsModuleBuildTarget: ModuleBuildTarget
) : KotlinModuleBuildTarget<BuildMetaInfo>(kotlinContext, jpsModuleBuildTarget) {
    val kind = module.platform?.idePlatformKind?.name

    private fun shouldNotBeCalled(): Nothing = error("Should not be called")

    override fun isEnabled(chunkCompilerArguments: CommonCompilerArguments): Boolean {
        return false
    }

    override val isIncrementalCompilationEnabled: Boolean
        get() = false

    override val hasCaches: Boolean
        get() = false

    override val globalLookupCacheId: String
        get() = shouldNotBeCalled()

    override fun compileModuleChunk(
        commonArguments: CommonCompilerArguments,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        environment: JpsCompilerEnvironment
    ): Boolean {
        shouldNotBeCalled()
    }

    override fun createCacheStorage(paths: BuildDataPaths): JpsIncrementalCache {
        shouldNotBeCalled()
    }

    override val buildMetaInfoFactory: BuildMetaInfoFactory<BuildMetaInfo>
        get() = shouldNotBeCalled()

    override val buildMetaInfoFileName: String
        get() = shouldNotBeCalled()
}