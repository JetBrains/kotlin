/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class CompilerClasspathOrderingCacheTest {

    @Test
    fun `ClassLoadersCachingBuildService must reuse ClassLoader regardless of classpath order`() {
        val tempDir = Files.createTempDirectory("kgp-cache-test").toFile()
        try {
            val jar1 = File(tempDir, "kotlin-compiler-embeddable.jar").apply { createNewFile() }
            val jar2 = File(tempDir, "kotlin-build-tools-impl.jar").apply { createNewFile() }

            val classpathOrderA = listOf(jar1, jar2)
            val classpathOrderB = listOf(jar2, jar1)

            val project = ProjectBuilder.builder().build()
            val serviceProvider = ClassLoadersCachingBuildService.registerIfAbsent(project)
            val service = serviceProvider.get()

            val classLoaderA = service.getClassLoader(classpathOrderA)
            val classLoaderB = service.getClassLoader(classpathOrderB)

            // BUG (KT-90565): Fails because ClassLoaderCacheKey uses order-sensitive List.equals()
            assertSame(
                classLoaderA,
                classLoaderB,
                "Expected the same ClassLoader instance to be reused for identical classpath JARs regardless of order"
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
