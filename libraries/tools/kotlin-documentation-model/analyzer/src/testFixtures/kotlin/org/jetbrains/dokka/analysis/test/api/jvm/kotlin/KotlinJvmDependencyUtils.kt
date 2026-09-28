/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.kotlin

private val lazyKotlinJvmStdlibJar by lazy {
    ClassLoader.getSystemResource("kotlin/jvm/Strictfp.class")
        ?.file
        ?.replace("file:", "")
        ?.replaceAfter(".jar", "")
        ?: error("Unable to find Kotlin's JVM stdlib jar.")
}

/**
 * Returns an absolute path to the JAR file of Kotlin's standard library for the JVM platform.
 *
 * Example: `~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.9.10/xx/kotlin-stdlib-1.9.10.jar`
 */
internal fun getKotlinJvmStdlibJarPath(): String {
    return lazyKotlinJvmStdlibJar
}
