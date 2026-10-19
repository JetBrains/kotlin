/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.repoTestFixtures

import org.jetbrains.kotlin.testFederation.RepositoryPath
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
        val absolutePath = findRepositoryRoot(Path("").absolute())!!.resolve("repo/gradle-build-conventions/repo-test-fixtures/build")
        val relativePath = Path("").absolute().relativize(absolutePath)
        assertTrue(relativePath.isGitIgnored())
        assertTrue(absolutePath.isGitIgnored())
    }

    @Test
    fun `dist directory - is ignored`() {
        val absolutePath = findRepositoryRoot(Path("").absolute())!!.resolve("dist")
        val relativePath = Path("").absolute().relativize(absolutePath)
        assertTrue(relativePath.isGitIgnored())
        assertTrue(absolutePath.isGitIgnored())
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
        val outsidePath = RepositoryPath(repositoryRoot, Path("../${repositoryRoot.fileName}-outside"))

        assertFailsWith<IllegalArgumentException> {
            outsidePath.isGitIgnored()
        }
    }

    @Test
    fun `normalized path is matched`() {
        repositoryRoot.resolve(".gitignore").writeText("/ignored\n")

        assertTrue(RepositoryPath(repositoryRoot, Path("directory/../ignored")).isGitIgnored())
    }

    @Test
    fun `repository path is resolved from its explicit root`() {
        repositoryRoot.resolve(".gitignore").writeText("/ignored\n")

        assertTrue(RepositoryPath(repositoryRoot, Path("ignored")).isGitIgnored())
        assertFalse(RepositoryPath(repositoryRoot, Path("included")).isGitIgnored())
    }

    @Test
    fun `relative repository root is normalized`() {
        repositoryRoot.resolve(".gitignore").writeText("/ignored\n")
        val relativeRoot = Path("").absolute().relativize(repositoryRoot).resolve("directory/..")

        assertTrue(RepositoryPath(relativeRoot, Path("ignored")).isGitIgnored())
    }

    @Test
    fun `gitignore rules outside the explicit root are not used`() {
        repositoryRoot.resolve(".gitignore").writeText("ignored\n")
        val nestedRoot = repositoryRoot.resolve("nested").createDirectories()

        assertFalse(RepositoryPath(nestedRoot, Path("ignored")).isGitIgnored())
        assertFalse(RepositoryPath(nestedRoot, Path("")).isGitIgnored())
    }

    @Test
    fun `gitignore rules are scoped to each repository root`() {
        val firstRoot = repositoryRoot.resolve("first").createDirectories()
        val secondRoot = repositoryRoot.resolve("second").createDirectories()
        firstRoot.resolve(".gitignore").writeText("/ignored\n")
        secondRoot.resolve(".gitignore").writeText("/other\n")

        assertTrue(RepositoryPath(firstRoot, Path("ignored")).isGitIgnored())
        assertFalse(RepositoryPath(secondRoot, Path("ignored")).isGitIgnored())
        assertTrue(RepositoryPath(secondRoot, Path("other")).isGitIgnored())
    }

    @Test
    fun `git metadata descendants are ignored`() {
        assertTrue(RepositoryPath(repositoryRoot, Path(".git/objects/pack")).isGitIgnored())
        assertTrue(RepositoryPath(repositoryRoot, Path("nested/.git/config")).isGitIgnored())
    }

    @Test
    fun `nested gitignore patterns are relative to their directory`() {
        repositoryRoot.resolve(".gitignore").writeText("/nested/inherited.txt\n")
        repositoryRoot.resolve("nested").createDirectories().resolve(".gitignore").writeText(
            "/ignored.txt\n!/inherited.txt\n"
        )

        assertTrue(RepositoryPath(repositoryRoot, Path("nested/ignored.txt")).isGitIgnored())
        assertFalse(RepositoryPath(repositoryRoot, Path("nested/inherited.txt")).isGitIgnored())
    }

    @Test
    fun `symlinked gitignore is not followed`() {
        repositoryRoot.resolve("rules").writeText("ignored.txt\n")
        Files.createSymbolicLink(repositoryRoot.resolve(".gitignore"), Path("rules"))

        assertFalse(RepositoryPath(repositoryRoot, Path("ignored.txt")).isGitIgnored())
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
                    Callable { RepositoryPath(repositoryRoot, Path("ignored-$index")).isGitIgnored() }
                }
            )

            assertTrue(results.all { it.get() })
        } finally {
            executor.shutdownNow()
        }
    }
}
