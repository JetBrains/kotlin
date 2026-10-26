/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import java.io.File
import java.nio.file.Files

/** The jar backing the `kotlin-stdlib` dependency already on this test's own runtime classpath. */
internal val jvmStdlibJar: File by lazy {
    val resourcePath = ClassLoader.getSystemResource("kotlin/jvm/Strictfp.class")?.file
        ?: error("Could not locate kotlin-stdlib on the test runtime classpath")
    File(resourcePath.removePrefix("file:").substringBefore(".jar") + ".jar")
}

internal fun createTempSourceRoot(): File = Files.createTempDirectory("kdm-analyzer-test").toFile()

/**
 * The common stdlib klib of a prebuilt Kotlin/Native distribution under `~/.konan`, so a `NATIVE`-platform source
 * set resolves the *actual* kotlin-native runtime `Throwable` (with its own `printStackTrace`/`getStackTrace`,
 * distinct from the JVM's) instead of a JVM/common stand-in. Every checkout that builds or tests anything
 * Kotlin/Native-related - including this repo's own CI - already provisions `~/.konan`, the same way
 * [jvmStdlibJar] above relies on `kotlin-stdlib` already being on this test's own runtime classpath.
 */
internal val nativeStdlibKlib: File by lazy {
    File(System.getProperty("user.home"), ".konan")
        .listFiles { f -> f.isDirectory && f.name.startsWith("kotlin-native-prebuilt-") }
        ?.mapNotNull { dist -> File(dist, "klib/common/stdlib").takeIf { it.isDirectory } }
        ?.maxByOrNull { it.lastModified() }
        ?: error(
            "Could not find a prebuilt Kotlin/Native distribution under " +
                    "~/.konan/kotlin-native-prebuilt-*/klib/common/stdlib. Run a Kotlin/Native-related Gradle task " +
                    "once (e.g. anything that provisions the K/N compiler) before running this test."
        )
}
