/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.compiler.embeddable

import java.io.File
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * trove4j jars should not be included (embedded) into our JARs.
 * This test checks that JARs don't contain any entry of trove library.
 */
class EmbeddableContentsTest {
    @Test
    fun `test current embeddable jars for trove classes`() {
        CompilerSmokeTest.compilerClasspath.filterNot {
            it.name.startsWith("trove")
        }.forEach(::checkJarFile)
    }

    private val konanHomeJars: List<File> by lazy {
        val home = System.getProperty("kotlin.native.home") ?: error("Property kotlin.native.home not specified")
        File(home).resolve("konan/lib")
                .listFiles { _, name -> name.endsWith("jar") }
                ?.toList() ?: error("Unable to find JARs in the `$home/konan/lib` directory")
    }

    @Test
    fun `test distribution jars for trove`() {
        konanHomeJars.filterNot {
            it.name.startsWith("trove")
        }.forEach(::checkJarFile)
    }

    @Test
    fun `self check on trove jar`() {
        assertFailsWith<AssertionError> {
            konanHomeJars.single {
                it.name.startsWith("trove")
            }.let(::checkJarFile)
        }
    }

    private fun checkJarFile(it: File) {
        JarFile(it).use { jar ->
            jar.entries().iterator().forEachRemaining { entry ->
                assert(!entry.name.contains("gnu/trove")) {
                    """
                    Jar file ${it.name} contains trove element: ${entry.name}
                    Check dependencies of embeddable configurations
                    """.trimIndent()
                }
            }
        }
    }
}