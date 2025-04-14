/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.jdk7

import org.junit.AssumptionViolatedException
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalPathApi::class)

class RecursiveDeletionTest {
    @Test
    fun deleteRelativePathRecursively() {
        val cwd = Paths.get("").toAbsolutePath()
        val testDirectory = createTempDirectory(cwd, "deleteRelativePathRecursively-")

        val directoryTreeRoot = testDirectory.relativeTo(cwd)
        // Check a few preconditions
        check(!directoryTreeRoot.isAbsolute)
        check(directoryTreeRoot.parent == null)

        directoryTreeRoot
            .resolve("childDir").createDirectory()
            .resolve("grandChildFile").createFile().let { file ->
                assertTrue(file.exists())
            }
        directoryTreeRoot.resolve("childDir2").createDirectory().let { dir ->
            assertTrue(dir.exists())
        }
        directoryTreeRoot.resolve("childFile").createFile().let { file ->
            assertTrue(file.exists())
        }

        directoryTreeRoot.deleteRecursively()
        assertFalse(testDirectory.exists(), "Directory was not deleted")
        assertTrue(cwd.exists(), "Parent directory was deleted")
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun deleteRelativeSymbolicLink() {
        val cwd = Paths.get("").toAbsolutePath()
        val testDirectory = createTempDirectory(cwd, "deleteRelativePathRecursively-")
        val symlink = cwd.resolve(Uuid.random().toHexString())

        try {
            symlink.createSymbolicLinkPointingTo(testDirectory)
        } catch (e: UnsupportedOperationException) {
            testDirectory.deleteRecursively()
            throw AssumptionViolatedException("FileSystem does not support symbolic links", e)
        }

        val directoryTreeRoot = symlink.relativeTo(cwd)
        // Check a few preconditions
        check(!directoryTreeRoot.isAbsolute)
        check(directoryTreeRoot.parent == null)
        check(directoryTreeRoot.isSymbolicLink())

        directoryTreeRoot.deleteRecursively()

        assertFalse(symlink.exists(), "Symbolic link was not deleted")
        assertTrue(testDirectory.exists(), "A directory the link was pointing to was deleted")
        assertTrue(cwd.exists(), "Parent directory was deleted")

        testDirectory.deleteRecursively()
    }
}
