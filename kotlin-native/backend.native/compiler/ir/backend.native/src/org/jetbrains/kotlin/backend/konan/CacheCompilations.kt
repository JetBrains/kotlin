/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

/**
 * A compilation of the caches of one or several libraries, configured like a spawned compilation by [setupConfiguration].
 * [onBuilt] records what has been built, [onFailure] cleans up after a failed build and reports the failure.
 */
internal class CacheBuild(
        val setupConfiguration: CompilerConfiguration.() -> Unit,
        val onBuilt: () -> Unit = {},
        val onFailure: (Throwable) -> Nothing,
)

/**
 * Runs the cache builds of the compilation of [config], in order. With [NativeSecondStageCompilationConfig.mergesIcCacheBuild],
 * the last one is not spawned if it can be merged: the caller compiles it in the IR of the output, see [takeDeferredBuild].
 */
internal class CacheCompilations(private val config: NativeSecondStageCompilationConfig, private val spawner: CompilationSpawner) {
    // Apple frameworks and test bundles aren't supported yet. A framework may also have no binary, so no IR to compile.
    private val mergesLastBuild = config.mergesIcCacheBuild && config.compileFromBitcode.isNullOrEmpty()
            && config.produce in setOf(CompilerOutputKind.PROGRAM, CompilerOutputKind.STATIC, CompilerOutputKind.DYNAMIC)

    private var deferredBuild: CacheBuild? = null

    /**
     * Runs [build]. [canBeMerged]: whether it can be compiled in the IR of the output, if it's the last build,
     * i.e. it only builds per-file caches of libraries subject to incremental compilation.
     */
    fun run(build: CacheBuild, canBeMerged: Boolean) {
        // Only the last build can be compiled with the output: the other ones are needed by the builds that follow.
        takeDeferredBuild()?.let(::spawn)
        if (mergesLastBuild && canBeMerged) deferredBuild = build else spawn(build)
    }

    fun spawn(build: CacheBuild) {
        try {
            spawner.spawn(config.additionalCacheFlags, build.setupConfiguration)
            build.onBuilt()
        } catch (t: Throwable) {
            build.onFailure(t)
        }
    }

    /** The last build, if it wasn't spawned: the caller must compile it with the output, as a spawned compilation would. */
    fun takeDeferredBuild(): CacheBuild? = deferredBuild.also { deferredBuild = null }
}
