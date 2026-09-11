/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.cleanGeneratedPackageDirectories
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratedPackageDirectoriesCleanupTest {

    @Test
    fun `stale sources and includes are removed and the directories are recreated`() {
        val root = Files.createTempDirectory("swift-package").toFile()
        val sources = root.resolve("Sources")
        val includes = root.resolve("OtherIncludes")
        sources.resolve("Stale").mkdirs()
        sources.resolve("Stale/Stale.swift").writeText("// stale\n")
        includes.resolve("StaleBridge/include").mkdirs()
        includes.resolve("StaleBridge/include/Stale.h").writeText("// stale\n")

        cleanGeneratedPackageDirectories(sources, includes)

        assertTrue(sources.isDirectory, "Sources must exist after the cleanup")
        assertTrue(includes.isDirectory, "OtherIncludes must exist after the cleanup")
        assertEquals(emptyList(), sources.listFiles().orEmpty().map { it.name })
        assertEquals(emptyList(), includes.listFiles().orEmpty().map { it.name })

        root.deleteRecursively()
    }

    @Test
    fun `absent directories are created`() {
        val root = Files.createTempDirectory("swift-package").toFile()
        val sources = root.resolve("Sources")
        val includes = root.resolve("OtherIncludes")

        cleanGeneratedPackageDirectories(sources, includes)

        assertTrue(sources.isDirectory, "Sources must exist after the cleanup")
        assertTrue(includes.isDirectory, "OtherIncludes must exist after the cleanup")

        root.deleteRecursively()
    }
}
