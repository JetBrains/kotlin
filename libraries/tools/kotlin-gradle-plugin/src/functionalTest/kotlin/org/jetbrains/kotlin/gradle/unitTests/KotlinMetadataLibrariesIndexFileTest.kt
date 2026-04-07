/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.KmpModuleIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataLibrariesIndexFile
import org.jetbrains.kotlin.gradle.plugin.mpp.TransformedMetadataLibraryRecord
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KotlinMetadataLibrariesIndexFileTest {

    @field:TempDir
    lateinit var temporaryFolder: File

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
            KotlinMetadataLibrariesIndexFile(temporaryFolder.resolve("test").also { it.mkdirs() }.resolve("does-not-exist")).read()
        }
    }

    @Test
    fun `test - file and parent file does not exist - write`() {
        KotlinMetadataLibrariesIndexFile(temporaryFolder.resolve("test").also { it.mkdirs() }.resolve("does-not-exist")).apply {
            write(emptyList())
            assertEquals(emptyList(), read())
        }
    }

    @Test
    fun `test - different KmpModuleIdentifier types`() {
        val index = KotlinMetadataLibrariesIndexFile(temporaryFolder.resolve("index").also { it.createNewFile() })
        val records = listOf(
            TransformedMetadataLibraryRecord(
                moduleId = KmpModuleIdentifier(
                    moduleId = KmpModuleIdentifier.ModuleId("group1", "name1"),
                    componentId = KmpModuleIdentifier.ModuleComponentId("version1", "classifier1")
                ),
                file = "file1.jar",
                sourceSetName = "sourceSet1"
            ),
            TransformedMetadataLibraryRecord(
                moduleId = KmpModuleIdentifier(
                    moduleId = KmpModuleIdentifier.ModuleId("group2", "name2"),
                    componentId = KmpModuleIdentifier.ModuleComponentId("version2", "classifier2")
                ),
                file = "file2.jar",
                sourceSetName = "sourceSet2"
            ),
            TransformedMetadataLibraryRecord(
                moduleId = KmpModuleIdentifier(
                    moduleId = KmpModuleIdentifier.ModuleId("group3", "name3"),
                    componentId = KmpModuleIdentifier.ModuleComponentId("version3", "")
                ),
                file = "file3.jar",
                sourceSetName = "sourceSet3"
            )
        )
        index.write(records)
        assertEquals(records, index.read())
    }


    private fun testWriteRead(files: Iterable<File>) {
        val index = KotlinMetadataLibrariesIndexFile(temporaryFolder.resolve("index").also { it.createNewFile() })
        val records = files.map {
            TransformedMetadataLibraryRecord(
                moduleId = KmpModuleIdentifier(
                    moduleId = KmpModuleIdentifier.ModuleId("a", "b"),
                    componentId = KmpModuleIdentifier.ModuleComponentId("c", "d")
                ),
                file = it.absolutePath,
                sourceSetName = it.name
            )
        }
        index.write(records)
        assertEquals(records, index.read())
    }
}
