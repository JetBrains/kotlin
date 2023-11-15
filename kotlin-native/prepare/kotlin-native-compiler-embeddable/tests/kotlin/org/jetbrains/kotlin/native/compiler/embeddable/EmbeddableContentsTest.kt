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
 * Some IntelliJ dependencies should be included (embedded) into our JARs, but some don't.
 *
 * trove4j jars should not be included while openapi and fastutil should.
 * This test checks JARs for entries of libraries.
 */
class EmbeddableContentsTest {
    @Test
    fun `test current embeddable jars for trove classes`() {
        CompilerSmokeTest.compilerClasspath.filterNot {
            it.name.startsWith("trove")
        }.forEach(::checkJarForTrove)
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
        }.forEach(::checkJarForTrove)
    }

    @Test
    fun `self check on trove jar`() {
        assertFailsWith<AssertionError> {
            konanHomeJars.single {
                it.name.startsWith("trove")
            }.let(::checkJarForTrove)
        }
    }

    private fun checkJarForTrove(file: File) {
        JarFile(file).use { jar ->
            jar.entries().iterator().forEachRemaining { entry ->
                assert(!entry.name.contains("gnu/trove")) {
                    """
                    Jar file ${jar.name} contains trove element: ${entry.name}
                    Check dependencies of embeddable configurations
                    """.trimIndent()
                }
            }
        }
    }

    @Test
    fun `test jars contain intellij dependencies`() {
        konanHomeJars.filterNot {
            it.name.startsWith("trove")
        }.forEach {
            it.checkJarContains("it/unimi/dsi/fastutil/objects/ReferenceOpenHashSet")
            it.checkJarContains("com/intellij/openapi/util/")
        }
    }

    @Test
    fun `test jars have no jna`() {
        konanHomeJars.filterNot {
            it.name.startsWith("trove")
        }.forEach {
            it.checkJarDoesntContain("com/sun/jna")
        }
    }

    private fun File.checkJarContains(string: String) {
        JarFile(this).use { jar ->
            assert(jar.entries()
                    .asSequence()
                    .any { it.name.contains(string) }
            ) {
                "Jar file ${jar.name} doesn't contain element: $string"
            }
        }
    }

    private fun File.checkJarDoesntContain(string: String) {
        JarFile(this).use { jar ->
            assert(jar.entries()
                    .asSequence()
                    .none { it.name.contains(string) }
            ) {
                "Jar file ${jar.name} contains element: $string"
            }
        }
    }
}