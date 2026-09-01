/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JpsBtaToolchainLoaderTest {
    @Test
    fun `takes the jars of the implementation directory handed over by the IDE`(@TempDir implHome: File) {
        File(implHome, "kotlin-build-tools-impl.jar").createNewFile()
        File(implHome, "kotlin-compiler-embeddable.jar").createNewFile()
        File(implHome, "not-a-jar.txt").createNewFile()

        val classpath = JpsBtaToolchainLoader.resolveClasspath(implHome.path)

        assertEquals(
            listOf("kotlin-build-tools-impl.jar", "kotlin-compiler-embeddable.jar"),
            classpath?.map { it.fileName.toString() },
        )
    }

    @Test
    fun `reports no classpath when the implementation directory holds no jars`(@TempDir implHome: File) {
        assertNull(JpsBtaToolchainLoader.resolveClasspath(implHome.path))
    }

    @Test
    fun `reports no classpath when the IDE handed over nothing`() {
        assertNull(JpsBtaToolchainLoader.resolveClasspath(implDirectory = null))
    }

    /**
     * Opt-in: needs a real implementation closure, which no dist ships. Run with
     * `-Dkotlin.jps.btaImplHome=<directory of jars>`.
     */
    @Test
    @EnabledIfSystemProperty(named = JpsBtaToolchainLoader.IMPL_HOME_PROPERTY, matches = ".+")
    fun `loads a toolchain reporting its compiler version`() {
        val toolchains = requireNotNull(JpsBtaToolchainLoader.load()) {
            "No implementation resolved from ${System.getProperty(JpsBtaToolchainLoader.IMPL_HOME_PROPERTY)}"
        }

        val compilerVersion = toolchains.getCompilerVersion()
        val implHome = System.getProperty(JpsBtaToolchainLoader.IMPL_HOME_PROPERTY)
        // Printed so that a demo and a failure report both show which implementation was loaded.
        println("Loaded the Build Tools API implementation of compiler version $compilerVersion from $implHome")

        assertNotNull(compilerVersion)
        toolchains.createBuildSession().close()
    }
}
