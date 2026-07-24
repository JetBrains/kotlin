/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ProjectPathTests {
    @Test
    fun `file name`() {
        assertEquals("foo.txt", ProjectFilePath("foo.txt").fileName)
        assertEquals("bar.txt", ProjectFilePath("foo/bar.txt").fileName)
        assertEquals("baz.txt", ProjectFilePath("foo/bar/baz.txt").fileName)
    }

    @Test
    fun `file dir`() {
        assertEquals("", ProjectFilePath("foo.txt").dir.pathFromProjectRoot)
        assertEquals("foo", ProjectFilePath("foo/bar.txt").dir.pathFromProjectRoot)
        assertEquals("foo/bar", ProjectFilePath("foo/bar/baz.txt").dir.pathFromProjectRoot)
    }

    @Test
    fun `dir parent`() {
        assertEquals(null, ProjectDirPath("").parent?.pathFromProjectRoot)
        assertEquals("", ProjectDirPath("foo").parent?.pathFromProjectRoot)
        assertEquals("foo", ProjectDirPath("foo/bar").parent?.pathFromProjectRoot)
        assertEquals("foo/bar", ProjectDirPath("foo/bar/baz").parent?.pathFromProjectRoot)
    }

    @Test
    fun `dir file`() {
        fun test(dirPath: String, relativePath: String, filePath: String) {
            val dir = ProjectDirPath(dirPath)
            val file = dir.file(relativePath)
            assertEquals(filePath, file.pathFromProjectRoot)
            assertEquals(relativePath, dir.relativePathToFileInside(file))
        }

        fun testDotDot(dirPath: String, relativePath: String, filePath: String) {
            val dir = ProjectDirPath(dirPath)
            val file = dir.file(relativePath)
            assertEquals(filePath, file.pathFromProjectRoot)
            assertNull(dir.relativePathToFileInside(file))
        }

        test("", "foo.txt", "foo.txt")
        test("", "foo/bar.txt", "foo/bar.txt")
        test("", "foo/bar/baz.txt", "foo/bar/baz.txt")

        test("foo", "bar.txt", "foo/bar.txt")
        test("foo", "bar/baz.txt", "foo/bar/baz.txt")
        test("foo", "bar/baz/qux.txt", "foo/bar/baz/qux.txt")

        test("foo/bar", "baz.txt", "foo/bar/baz.txt")
        test("foo/bar", "baz/qux.txt", "foo/bar/baz/qux.txt")
        test("foo/bar", "baz/qux/quux.txt", "foo/bar/baz/qux/quux.txt")

        testDotDot("foo", "../foo.txt", "foo.txt")
        testDotDot("foo", "../bar/foo.txt", "bar/foo.txt")
        testDotDot("foo/bar", "../../foo.txt", "foo.txt")
        testDotDot("foo/bar", "../../bar/foo.txt", "bar/foo.txt")

        assertFailsWith<IllegalStateException> { ProjectDirPath("").file("../bar.txt") }
        assertFailsWith<IllegalStateException> { ProjectDirPath("foo").file("../../bar.txt") }
    }
}
