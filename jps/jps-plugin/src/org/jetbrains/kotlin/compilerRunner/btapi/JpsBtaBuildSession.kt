/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.jps.incremental.GlobalContextKey
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.utils.KotlinPaths

/**
 * The [KotlinToolchains.BuildSession] of one JPS build.
 *
 * Opened in `KotlinBuilder.buildStarted` and closed in `KotlinBuilder.buildFinished`, mirroring what the Kotlin
 * Gradle plugin does per Gradle build. The session is created lazily on first use, because the `kotlinc` home is
 * only resolved once a chunk is about to be compiled.
 */
class JpsBtaBuildSession internal constructor() {
    private var session: KotlinToolchains.BuildSession? = null

    @Synchronized
    fun getOrCreate(kotlinPaths: KotlinPaths): KotlinToolchains.BuildSession {
        session?.let { return it }

        val toolchains = JpsBtaToolchainLoader.load(kotlinPaths.libPath)
        return toolchains.createBuildSession().also { session = it }
    }

    @Synchronized
    fun close() {
        val current = session ?: return
        session = null
        current.close()
    }
}

internal val jpsBtaBuildSessionKey = GlobalContextKey<JpsBtaBuildSession>("kotlinJpsBuildToolsApiSession")
