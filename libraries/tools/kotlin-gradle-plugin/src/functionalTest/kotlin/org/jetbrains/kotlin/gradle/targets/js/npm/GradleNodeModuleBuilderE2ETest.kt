/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.internal.extensions.core.serviceOf
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.zipTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * KT-89245: a dependency must not place its imported package outside the npm cache. These tests go
 * through [GradleNodeModuleBuilder] over a real archive, not through the path guard on its own.
 */
class GradleNodeModuleBuilderE2ETest {

    @Test
    fun rejectsTraversalNameFromPackageJson(
        @TempDir
        tempDir: File,
    ) {
        val cacheDir = tempDir.cacheDir()
        // where "../../evil" from the cache lands
        val victim = tempDir.resolve("build/evil/1.0.0").apply { mkdirs() }
        val keepMe = victim.resolve("keep_me.txt").apply { writeText("precious") }

        val jar = tempDir.jar(
            "evil",
            "package.json" to """{ "name": "../../evil", "version": "1.0.0" }""",
            "evil.js" to "module.exports = {}",
        )

        assertFailsWith<GradleException> { builderFor(jar, cacheDir).rebuild() }

        assertTrue(keepMe.exists(), "a directory outside the cache must not be deleted")
        assertTrue(cacheDir.listFiles().isNullOrEmpty(), "nothing should be written for a rejected name")
    }

    /** A `.meta.js` file name overwrites the parsed `name`. `"...meta.js"` minus its suffix is `".."`. */
    @Test
    fun rejectsTraversalNameFromMetaJsFileName(
        @TempDir
        tempDir: File,
    ) {
        val cacheDir = tempDir.cacheDir()
        val jar = tempDir.jar("meta", "...meta.js" to "module.exports = {}")

        assertFailsWith<GradleException> { builderFor(jar, cacheDir).rebuild() }

        assertTrue(cacheDir.listFiles().isNullOrEmpty(), "nothing should be written for a rejected name")
    }

    @Test
    fun importsLegitimatePackage(
        @TempDir
        tempDir: File,
    ) {
        val cacheDir = tempDir.cacheDir()
        val jar = tempDir.jar(
            "legit",
            "package.json" to """{ "name": "my-package", "version": "1.0.0" }""",
            "my-package.js" to "module.exports = {}",
        )

        val dir = builderFor(jar, cacheDir).rebuild()

        assertEquals(cacheDir.resolve("my-package").resolve("1.0.0"), dir)
        assertTrue(dir!!.resolve("package.json").exists(), "package.json should be written")
        assertTrue(dir.resolve("my-package.js").exists(), "runtime files should be copied")
    }

    private val project = buildProject()

    private fun builderFor(jar: File, cacheDir: File): GradleNodeModuleBuilder = GradleNodeModuleBuilder(
        fs = project.serviceOf<FileSystemOperations>(),
        archiveOperations = project.serviceOf<ArchiveOperations>(),
        moduleName = "module",
        moduleVersion = "1.0.0",
        srcFiles = listOf(jar),
        cacheDir = cacheDir,
    ).apply { visitArtifacts() }

    // Nested like the real cache, so `../..` stays in the temp dir
    private fun File.cacheDir(): File = resolve("build/js/packages_imported").apply { mkdirs() }

    private fun File.jar(name: String, vararg entries: Pair<String, String>): File {
        val content = resolve("$name-content").apply { mkdirs() }
        entries.forEach { (path, text) -> content.resolve(path).writeText(text) }
        return resolve("$name.jar").also { zipTo(it, content) }
    }
}