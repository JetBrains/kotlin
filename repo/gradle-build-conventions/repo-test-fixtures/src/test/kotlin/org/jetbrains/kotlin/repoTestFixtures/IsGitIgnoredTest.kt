/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.repoTestFixtures

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path as NioPath
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IsGitIgnoredTest {
    @TempDir
    lateinit var repositoryRoot: NioPath

    @Test
    fun `git directory is ignored`() {
        assertTrue(Path(".git").isGitIgnored())
        assertTrue(Path(".git").absolute().isGitIgnored())
        assertTrue(Path("foo/bar/.git").isGitIgnored())
    }

    @Test
    fun `build directory - is ignored`() {
        val relativePath = Path("repo/gradle-build-conventions/repo-test-fixtures/build")
        assertTrue(relativePath.isGitIgnored())
        assertTrue(findRepositoryRoot(Path("").absolute())!!.resolve(relativePath).isGitIgnored())
    }

    @Test
    fun `dist directory - is ignored`() {
        val relativePath = Path("dist")
        assertTrue(relativePath.isGitIgnored())
        assertTrue(findRepositoryRoot(Path("").absolute())!!.resolve(relativePath).isGitIgnored())
    }

    @Test
    fun `build_gradle_kts - is not ignored`() {
        val relativePath = Path("build.gradle.kts")
        assertFalse(relativePath.isGitIgnored())
        assertFalse(relativePath.absolute().isGitIgnored())
    }

    @Test
    fun `compiler directory - is not ignored`() {
        val relativePath = Path("compiler")
        assertFalse(relativePath.isGitIgnored())
        assertFalse(relativePath.absolute().isGitIgnored())
    }

    @Test
    fun `path outside repository is rejected`() {
        val outsidePath = repositoryRoot.resolveSibling("${repositoryRoot.fileName}-outside")

        assertFailsWith<IllegalArgumentException> {
            outsidePath.isGitIgnored(repositoryRoot)
        }
    }

    @Test
    fun `normalized path is matched`() {
        repositoryRoot.resolve(".gitignore").writeText("/ignored\n")

        assertTrue(repositoryRoot.resolve("directory/../ignored").isGitIgnored(repositoryRoot))
    }

    @Test
    fun `git metadata descendants are ignored`() {
        assertTrue(repositoryRoot.resolve(".git/objects/pack").isGitIgnored(repositoryRoot))
        assertTrue(repositoryRoot.resolve("nested/.git/config").isGitIgnored(repositoryRoot))
    }

    @Test
    fun `nested gitignore patterns are relative to their directory`() {
        repositoryRoot.resolve(".gitignore").writeText("/nested/inherited.txt\n")
        repositoryRoot.resolve("nested").createDirectories().resolve(".gitignore").writeText(
            "/ignored.txt\n!/inherited.txt\n"
        )

        assertTrue(repositoryRoot.resolve("nested/ignored.txt").isGitIgnored(repositoryRoot))
        assertFalse(repositoryRoot.resolve("nested/inherited.txt").isGitIgnored(repositoryRoot))
    }

    @Test
    fun `symlinked gitignore is not followed`() {
        repositoryRoot.resolve("rules").writeText("ignored.txt\n")
        Files.createSymbolicLink(repositoryRoot.resolve(".gitignore"), Path("rules"))

        assertFalse(repositoryRoot.resolve("ignored.txt").isGitIgnored(repositoryRoot))
    }

    @Test
    fun `repository root is discovered from a nested directory`() {
        repositoryRoot.resolve(".git").createDirectories()
        val nestedDirectory = repositoryRoot.resolve("nested/directory").createDirectories()

        assertEquals(repositoryRoot, findRepositoryRoot(nestedDirectory))
    }

    @Test
    fun `cached ignore node supports concurrent matching`() {
        repositoryRoot.resolve(".gitignore").writeText("ignored-*\n")
        val executor = Executors.newFixedThreadPool(8)
        try {
            val results = executor.invokeAll(
                List(1_000) { index ->
                    Callable { repositoryRoot.resolve("ignored-$index").isGitIgnored(repositoryRoot) }
                }
            )

            assertTrue(results.all { it.get() })
        } finally {
            executor.shutdownNow()
        }
    }
}
