/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.utils.MachO
import org.junit.jupiter.api.io.TempDir
import kotlin.test.Test
import java.io.File
import java.io.InputStream
import kotlin.test.assertEquals

class MachOTest {

    @field:TempDir
    lateinit var temporaryFolder: File

    @Test
    fun `dynamic dummy`() {
        assertDylib(true, "/cocoapods/dynamic/dummy.framework/dummy")
    }

    @Test
    fun `static dummy`() {
        assertDylib(false, "/cocoapods/static/dummy.framework/dummy")
    }

    @Test
    fun `dynamic fat`() {
        assertDylib(true, "/machoBinaries/dynamicFat")
    }

    @Test
    fun `static fat`() {
        assertDylib(false, "/machoBinaries/staticFat")
    }

    @Test
    fun `dynamic lib`() {
        assertDylib(true, "/machoBinaries/dynamicLib")
    }

    @Test
    fun `static lib`() {
        assertDylib(false, "/machoBinaries/staticLib")
    }

    private fun assertDylib(expected: Boolean, resource: String) {
        val tmp = temporaryFolder.resolve("tmp").also { it.createNewFile() }
        tmp.writeResource(resource)
        return assertEquals(expected, MachO.isDylib(tmp, buildProject().logger))
    }

    private fun File.writeResource(resource: String) {
        outputStream().use { out ->
            MachOTest::class.java.getResourceAsStream(resource)!!.use { input: InputStream ->
                input.copyTo(out)
            }
        }
    }
}
