/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import com.intellij.util.io.Compressor
import org.gradle.kotlin.dsl.support.unzipTo
import org.jetbrains.kotlin.gradle.utils.contentEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.fail

class FileUtilsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val zipContentFolder by lazy {
        temporaryFolder.newFolder().apply {
            resolve("stub0.txt").apply {
                parentFile.mkdirs()
                writeText("stub0 content")
            }
        }
    }

    private fun newFile(name: String, text: String): File {
        temporaryFolder.newFolder().apply {
            resolve(name).apply {
                parentFile.mkdirs()
                writeText(text)
                return this
            }
        }
    }

    @Test
    fun `empty files`() {
        val empty = newFile("a.txt", "")
        assertTrue(contentEquals(empty, empty))
    }

    @Test
    fun `short files`() {
        val b = newFile("b.txt", "1")
        val c = newFile("c.txt", "2")
        assertTrue(contentEquals(b, b))
        assertTrue(contentEquals(c, c))
        assertFalse(contentEquals(b, c))
    }

    @Test
    fun `one file is a prefix of the other`() {
        val prefix = newFile("d.txt", "3\n")
        val full = newFile("e.txt", "3\n4\n5\n")
        assertFalse(contentEquals(prefix, full))
        assertFalse(contentEquals(full, prefix))
    }
}
