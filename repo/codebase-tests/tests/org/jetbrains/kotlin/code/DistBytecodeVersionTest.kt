/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries

/**
 * The compilers of this repository run on a modern JDK but cross-compile down to Java 8, so nothing stops a build
 * script from silently raising the bytecode version of an artifact we ship. This test scans the distribution and
 * reports every class file that the Java version it is shipped for could not load.
 */
class DistBytecodeVersionTest {

    private val distLibraries = Path("dist/kotlinc/lib")

    private val multiReleaseEntry = Regex("META-INF/versions/(\\d+)/")

    private val classFileMagic = 0xCAFEBABE.toInt()

    /** Java 1 compiles to major version 45, so Java `N` compiles to `44 + N`. */
    private val classFileMajorOfJava1 = 44

    @Test
    fun `distribution stays loadable by java 8`() {
        val jars = distLibraries.listDirectoryEntries("*.jar").sorted()
        if (jars.isEmpty()) fail("No jar found in '$distLibraries'. This test has to run after the ':dist' task.")

        val violations = jars.flatMap { jar ->
            ZipFile(jar.toFile()).use { zip ->
                zip.entries().asSequence()
                    .filter { it.name.endsWith(".class") }
                    // A module descriptor cannot target anything below Java 9, and Java 8 ignores it.
                    .filterNot { it.name.substringAfterLast('/') == "module-info.class" }
                    .mapNotNull { entry ->
                        val major = zip.getInputStream(entry).use(::readMajorVersion) ?: return@mapNotNull null
                        val expected = expectedMaxMajorVersion(entry.name)
                        if (major <= expected) null
                        else "${jar.fileName}!/${entry.name}: major version $major, expected at most $expected"
                    }
                    .toList()
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "The following class files of the distribution cannot be loaded by the Java version they are shipped " +
                        "for. Make sure the modules producing them compile with the default JVM target:\n\n" +
                        violations.joinToString("\n")
            )
        }
    }

    private fun expectedMaxMajorVersion(entryName: String): Int {
        val multiReleaseVersion = multiReleaseEntry.matchAt(entryName, 0)?.groupValues?.get(1)
        return classFileMajorOfJava1 + (multiReleaseVersion?.toInt() ?: 8)
    }

    private fun readMajorVersion(entry: InputStream): Int? = with(DataInputStream(entry)) {
        if (readInt() != classFileMagic) return null
        readUnsignedShort() // minor_version
        readUnsignedShort()
    }
}
