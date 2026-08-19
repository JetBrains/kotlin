/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.GlobalContextKey
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.jps.build.KotlinBuilder

/**
 * What one build holds on to on the Build Tools API side: the loaded [KotlinToolchains] and the
 * [KotlinToolchains.BuildSession] opened on them.
 *
 * The two are stored together on purpose. [JpsBtaToolchainsProvider] caches the toolchains behind a
 * [java.lang.ref.SoftReference], so a *different* `KotlinToolchains`, on a different classloader, can appear part-way
 * through a build — precisely under the memory pressure that compiling causes. Building an operation from those new
 * toolchains and executing it on the session of the old ones mixes two compiler classloaders in one call, which at
 * best fails as a `ClassCastException`. Pinning the pair for the duration of the build means every chunk uses the
 * toolchains its session was opened on.
 */
internal class JpsBtaBuild(
    val toolchains: KotlinToolchains,
    val session: KotlinToolchains.BuildSession,
)

/**
 * The [JpsBtaBuild] of the current build.
 *
 * A session owns caches shared by all operations of one build, so it is created lazily before the first Kotlin chunk
 * and closed in `KotlinBuilder.buildFinished`. It is shared between all threads, hence a [GlobalContextKey], next to
 * the existing `kotlinCompileContextKey`.
 */
private val btaBuildKey = GlobalContextKey<JpsBtaBuild>("kotlin-bta-build")

/**
 * @param loadToolchains called at most once per build, while holding the lock.
 * @param onCreated run while holding the lock, only for the build that actually opens the session. Used to log the
 * session once per build rather than once per module.
 */
internal fun CompileContext.getOrCreateBtaBuild(
    loadToolchains: () -> KotlinToolchains,
    onCreated: (JpsBtaBuild) -> Unit = {},
): JpsBtaBuild {
    getUserData(btaBuildKey)?.let { return it }

    synchronized(btaBuildKey) {
        getUserData(btaBuildKey)?.let { return it }

        val toolchains = loadToolchains()
        val build = JpsBtaBuild(toolchains, toolchains.createBuildSession())
        onCreated(build)
        putUserData(btaBuildKey, build)
        return build
    }
}

internal fun CompileContext.closeBtaBuild() {
    if (getUserData(btaBuildKey) == null) return

    synchronized(btaBuildKey) {
        val build = getUserData(btaBuildKey) ?: return
        putUserData(btaBuildKey, null)
        KotlinBuilder.LOG.info("Closing the Kotlin Build Tools API build session")
        build.session.close()
    }
}
