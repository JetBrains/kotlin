/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.jetbrains.kotlin.incremental.createDirectory
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeVersionValueSourceTest {

    @Rule
    @JvmField
    var tmp = TemporaryFolder()

    @Test
    fun testMoveToNonEmptyDir() {
        val nativeDir = tmp.newFolder("native_dir").also { it.mkdir() }
        val versionDir = nativeDir.resolve("version").also { it.createDirectory() }
        versionDir.resolve("A.kt").createNewFile()
        versionDir.resolve("C.kt").createNewFile()

        NativeVersionValueSource.copyNativeBundleDistribution(createTarGz(), versionDir)
        assertEquals("class A {}", versionDir.resolve("A.kt").readText())
        assertTrue("File B.kt should be copied from directory") { versionDir.resolve("B.kt").exists() }
        assertTrue("Marker file should be created") { versionDir.resolve(NativeVersionValueSource.Companion.MARKER_FILE).exists() }
    }

    private fun createTarGz(): File {
        val tarFile = tmp.newFile("version.tar.gz").also { it.createNewFile() }
        TarArchiveOutputStream(
            GZIPOutputStream(
                BufferedOutputStream(
                    FileOutputStream(tarFile)
                )
            )
        ).use {
            val fileContents = "class A {}".toByteArray()
            val entry = TarArchiveEntry("version/A.kt")
            entry.size = fileContents.size.toLong()
            it.putArchiveEntry(entry)
            it.write(fileContents)
            it.closeArchiveEntry()
            it.putArchiveEntry(TarArchiveEntry("version/B.kt"))
            it.closeArchiveEntry()
        }
        return tarFile
    }
}