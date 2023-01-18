/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataLibrariesIndexFile
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KotlinMetadataLibrariesIndexFileTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `test - empty list`() {
        testWriteRead(emptyList())
    }

    @Test
    fun `test - absolute and relative files`() {
        testWriteRead(listOf(File("relative"), File("absolute").absoluteFile))
    }

    @Test
    fun `test - file does not exist - read`() {
        assertFailsWith<FileNotFoundException> {
            KotlinMetadataLibrariesIndexFile(temporaryFolder.newFolder().resolve("does-not-exist")).read()
        }
    }

    @Test
    fun `test - file and parent file does not exist - write`() {
        KotlinMetadataLibrariesIndexFile(temporaryFolder.newFolder().resolve("does-not-exist")).apply {
            write(emptyList())
            assertEquals(emptyList(), read())
        }
    }

    private fun testWriteRead(files: Iterable<File>) {
        val index = KotlinMetadataLibrariesIndexFile(temporaryFolder.newFile())
        index.write(files)
        assertEquals(files.toList(), index.read())
    }
}