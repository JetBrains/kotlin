/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.repoTestFixtures

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RepositoryPathsTest {
    @Test
    fun `relative paths are resolved from the working directory`() {
        val relativeRoot = Path("").absolute().relativize(repositoryRoot)
        for (path in listOf(Path(""), Path("."), Path("tests/mute-common.csv"), Path("missing/../deleted.txt"), relativeRoot)) {
            val expected = path.absolute().normalize()

            assertEquals(expected, path.toRepositoryPath().resolve())
            assertEquals(expected, path.toFile().toRepositoryPath().resolve())
        }
    }

    @Test
    fun `absolute paths are normalized without changing their base`() {
        val path = repositoryRoot.resolve("missing/../deleted.txt")

        assertEquals(path.normalize(), path.toRepositoryPath().resolve())
        assertEquals(path.normalize(), path.toFile().toRepositoryPath().resolve())
    }

    @Test
    fun `ignore helpers agree for relative and absolute paths`() {
        assertIgnoreLevel(repositoryRoot.resolve(".git"), IgnoreLevel.GitIgnored)
        assertIgnoreLevel(repositoryRoot.resolve("tests/mute-common.csv"), IgnoreLevel.TestFederationIgnored)
        assertIgnoreLevel(repositoryRoot.resolve("build.gradle.kts"), null)
        assertIgnoreLevel(Path("").absolute(), null)
    }

    @Test
    fun `relative ignore helpers use the working directory`() {
        for (path in listOf(Path(""), Path("."), Path("tests/mute-common.csv"), Path("dist"))) {
            val absolutePath = path.absolute()
            assertIgnoreLevel(absolutePath, absolutePath.ignoreLevel)
        }
    }

    @Test
    fun `paths escaping the repository are rejected by all helpers`() {
        val absolutePath = repositoryRoot.resolve("../outside").normalize()
        val relativePath = Path("").absolute().relativize(absolutePath)
        for (path in listOf(relativePath, absolutePath)) {
            assertFailsWith<IllegalArgumentException> { path.toRepositoryPath() }
            assertFailsWith<IllegalArgumentException> { path.toFile().toRepositoryPath() }
            assertFailsWith<IllegalArgumentException> { path.isGitIgnored() }
            assertFailsWith<IllegalArgumentException> { path.toFile().isGitIgnored() }
            assertFailsWith<IllegalArgumentException> { path.isTestFederationIgnored() }
            assertFailsWith<IllegalArgumentException> { path.toFile().isTestFederationIgnored() }
            assertFailsWith<IllegalArgumentException> { path.ignoreLevel }
            assertFailsWith<IllegalArgumentException> { path.toFile().ignoreLevel }
        }
    }
}

private fun assertIgnoreLevel(absolutePath: Path, expected: IgnoreLevel?) {
    val relativePath = Path("").absolute().relativize(absolutePath)
    for (path in listOf(relativePath, absolutePath)) {
        assertEquals(expected, path.ignoreLevel, "$path")
        assertEquals(expected, path.toFile().ignoreLevel, "$path")
        assertEquals(expected == IgnoreLevel.GitIgnored, path.isGitIgnored(), "$path")
        assertEquals(expected == IgnoreLevel.GitIgnored, path.toFile().isGitIgnored(), "$path")
        assertEquals(expected == IgnoreLevel.TestFederationIgnored, path.isTestFederationIgnored(), "$path")
        assertEquals(expected == IgnoreLevel.TestFederationIgnored, path.toFile().isTestFederationIgnored(), "$path")
    }
}
