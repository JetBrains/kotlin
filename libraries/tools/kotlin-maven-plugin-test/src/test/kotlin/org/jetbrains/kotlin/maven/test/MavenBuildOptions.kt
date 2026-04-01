/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

data class MavenBuildOptions(
    val javaVersion: TestVersions.Java = TestVersions.Java.JDK_17,
    val useKotlinDaemon: Boolean? = null,
    val extraMavenProperties: Map<String, String> = emptyMap(),
) {
    fun asCliArgs(): List<String> = buildList {
        useKotlinDaemon?.let { add("-Dkotlin.compiler.daemon=$it") }
        extraMavenProperties.forEach { (key, value) -> add("-D$key=$value") }
    }
}

/**
 * Disables the Kotlin compiler daemon with a mandatory [reason].
 *
 * Use this instead of setting `useKotlinDaemon = false` directly,
 * so that each call site documents why the daemon is being disabled explicitly.
 */
fun MavenBuildOptions.withoutKotlinDaemon(@Suppress("UNUSED_PARAMETER") reason: String): MavenBuildOptions =
    copy(useKotlinDaemon = false)