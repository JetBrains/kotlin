/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File
import java.util.*

enum class WarningStatus { NO_WARNINGS, OLD_LIBRARY_WARNING, TOO_NEW_LIBRARY_WARNING }

class TestVersion(val basicVersion: KotlinVersion, val postfix: String) : Comparable<TestVersion> {
    constructor(major: Int, minor: Int, patch: Int, postfix: String = "") : this(KotlinVersion(major, minor, patch), postfix)

    override fun compareTo(other: TestVersion) = basicVersion.compareTo(other.basicVersion)
    override fun equals(other: Any?) = (other as? TestVersion)?.basicVersion == basicVersion
    override fun hashCode() = basicVersion.hashCode()
    override fun toString() = basicVersion.toString() + postfix
}

interface DummyLibraryCompiler {
    fun compileDummyLibrary(
        libraryVersion: TestVersion?,
        compilerVersion: TestVersion?,
        expectedWarningStatus: WarningStatus,
        exportKlibToOlderAbiVersion: Boolean = false,
    )
}

abstract class LibrarySpecialCompatibilityChecksTest : TestCaseWithTmpdir(), DummyLibraryCompiler {
    /**
     * Since the ABI version is bumped after the language version, it may happen that after bumping the language version
     * [KotlinAbiVersion.CURRENT] != [LanguageVersion.LATEST_STABLE]. This can cause issues in library compatibility tests: for example,
     * when exporting a klib to the previous ABI version, we may use a 2.X compiler while the previous ABI version is 2.(X − 2).
     * Since this is only a temporary situation (the ABI version is usually bumped shortly after the language version),
     * we simply ignore these tests when this happens.
     */
    override fun shouldRunTest(): Boolean =
        LanguageVersion.LATEST_STABLE.major == KotlinAbiVersion.CURRENT.major && LanguageVersion.LATEST_STABLE.minor == KotlinAbiVersion.CURRENT.minor

    fun testSameBasicCompilerVersion() {
        for (versionsWithSameBasicVersion in SORTED_TEST_COMPILER_VERSION_GROUPS) {
            for (libraryVersion in versionsWithSameBasicVersion) {
                for (compilerVersion in versionsWithSameBasicVersion) {
                    compileDummyLibrary(
                        libraryVersion = libraryVersion,
                        compilerVersion = compilerVersion,
                        expectedWarningStatus = WarningStatus.NO_WARNINGS
                    )
                }
            }
        }
    }

    fun testNewerCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            compileDummyLibrary(
                libraryVersion = currentVersion,
                compilerVersion = nextVersion,
                expectedWarningStatus = WarningStatus.OLD_LIBRARY_WARNING
            )
        }
    }

    fun testOlderCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            val sameLanguageVersion = haveSameLanguageVersion(currentVersion, nextVersion)
            compileDummyLibrary(
                libraryVersion = nextVersion,
                compilerVersion = currentVersion,
                expectedWarningStatus = if (sameLanguageVersion) WarningStatus.NO_WARNINGS else WarningStatus.TOO_NEW_LIBRARY_WARNING
            )
        }
    }

    private inline fun testCurrentAndNextBasicVersions(block: (currentVersion: TestVersion, nextVersion: TestVersion) -> Unit) {
        for (i in 0..SORTED_TEST_COMPILER_VERSION_GROUPS.size - 2) {
            val versionsWithSameBasicVersion = SORTED_TEST_COMPILER_VERSION_GROUPS[i]
            val versionsWithNextSameBasicVersion = SORTED_TEST_COMPILER_VERSION_GROUPS[i + 1]

            for (currentVersion in versionsWithSameBasicVersion) {
                for (nextVersion in versionsWithNextSameBasicVersion) {
                    block(currentVersion, nextVersion)
                }
            }
        }
    }

    fun testEitherVersionIsMissing() {
        listOf(
            TestVersion(2, 0, 0) to null,
            null to TestVersion(2, 0, 0),
        ).forEach { (libraryVersion, compilerVersion) ->
            compileDummyLibrary(
                libraryVersion = libraryVersion,
                compilerVersion = compilerVersion,
                expectedWarningStatus = WarningStatus.NO_WARNINGS
            )
        }
    }

    private fun haveSameLanguageVersion(a: TestVersion, b: TestVersion): Boolean =
        a.basicVersion.major == b.basicVersion.major && a.basicVersion.minor == b.basicVersion.minor

    protected abstract val originalLibraryPath: String
    protected open fun additionalLibraries(): List<String> = listOf()

    protected fun createDir(name: String): File = tmpdir.resolve(name).apply { mkdirs() }
    protected fun createFile(name: String): File = tmpdir.resolve(name).apply { parentFile.mkdirs() }

    companion object {
        private val currentKotlinVersion = KotlinVersion.CURRENT

        private val VERSIONS = listOf(
            0 to "",
            0 to "-dev-1234",
            0 to "-dev-4321",
            0 to "-Beta1",
            0 to "-Beta2",
            20 to "",
            20 to "-Beta1",
            20 to "-Beta2",
            255 to "-SNAPSHOT",
        )

        val SORTED_TEST_COMPILER_VERSION_GROUPS: List<Collection<TestVersion>> =
            VERSIONS.map { (patch, postfix) -> TestVersion(currentKotlinVersion.major, currentKotlinVersion.minor, patch, postfix) }
                .groupByTo(TreeMap()) { it.basicVersion }.values.toList()

        val SORTED_TEST_OLD_LIBRARY_VERSION_GROUPS: List<TestVersion> =
            VERSIONS.map { (patch, postfix) -> TestVersion(currentKotlinVersion.major, currentKotlinVersion.minor - 1, patch, postfix) }
    }
}
