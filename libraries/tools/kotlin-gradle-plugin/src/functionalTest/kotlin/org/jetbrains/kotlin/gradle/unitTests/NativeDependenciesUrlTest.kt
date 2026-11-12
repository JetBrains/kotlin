/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.internal.properties.resolveNativeDependenciesUrl
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the `kotlin.native.dependenciesUrl` value resolution.
 *
 * Path assertions are expressed through [File] rather than exact strings, so that they hold on every host.
 */
class NativeDependenciesUrlTest {

    @Test
    fun urlsArePassedThroughUnchanged() {
        for (url in listOf(
            "https://download.jetbrains.com/kotlin/native",
            "http://example.com/konan",
            "ftp://example.com/konan",
            "file:///opt/konan",
        )) {
            assertEquals(url, resolveNativeDependenciesUrl(url))
        }
    }

    @Test
    fun pathsAreConvertedToFileUrls() {
        val path = File("/opt/konan").absoluteFile
        assertEquals(path.toURI().toString().trimEnd('/'), resolveNativeDependenciesUrl(path.path))
    }

    @Test
    fun windowsDrivePathIsNotTreatedAsUrl() {
        // `URL` rejects unknown protocols, so the drive letter is not mistaken for a scheme.
        val url = resolveNativeDependenciesUrl("C:/konan")
        assertTrue(url.startsWith("file:"), url)
        assertTrue(url.endsWith("/konan"), url)
    }

    @Test
    fun trailingSlashIsRemovedFromPaths() {
        // `File.toURI` adds a trailing slash for existing directories.
        val existingDir = File(System.getProperty("java.io.tmpdir")).absoluteFile
        assertFalse(resolveNativeDependenciesUrl(existingDir.path).endsWith("/"))
    }

    @Test
    fun surroundingWhitespaceIsIgnored() {
        assertEquals(
            resolveNativeDependenciesUrl("https://example.com/konan"),
            resolveNativeDependenciesUrl("  https://example.com/konan  "),
        )
    }
}
