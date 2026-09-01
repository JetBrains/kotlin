/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.jps.incremental.GlobalContextKey
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains

/**
 * The [KotlinToolchains.BuildSession] of one JPS build, mirroring what the Kotlin Gradle plugin does per Gradle
 * build. Created lazily on first use, so that a build compiling no Kotlin never loads the implementation, and
 * closed in `KotlinBuilder.buildFinished`.
 */
class JpsBtaBuildSession internal constructor() {
    private var session: KotlinToolchains.BuildSession? = null

    /**
     * @return the session, or `null` when the implementation is unavailable - the caller stays on the legacy path.
     */
    @Synchronized
    fun getOrCreate(): KotlinToolchains.BuildSession? {
        session?.let { return it }
        val toolchains = JpsBtaToolchainLoader.load() ?: return null
        return toolchains.createBuildSession().also { session = it }
    }

    @Synchronized
    fun close() {
        session?.close()
        session = null
    }
}

internal val jpsBtaBuildSessionKey = GlobalContextKey<JpsBtaBuildSession>("kotlinJpsBuildToolsApiSession")
