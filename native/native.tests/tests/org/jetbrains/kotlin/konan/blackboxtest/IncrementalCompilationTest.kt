/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.blackboxtest.CachesAutoBuildTest.Companion.TEST_SUITE_PATH
import org.jetbrains.kotlin.konan.blackboxtest.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.CacheMode
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.OptimizationMode
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertNotEquals
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("caches")
@EnforcedHostTarget
@TestMetadata(TEST_SUITE_PATH)
@TestDataPath("\$PROJECT_ROOT")
@UsePartialLinkage(UsePartialLinkage.Mode.DISABLED)
class IncrementalCompilationTest : AbstractNativeSimpleTest() {
    @BeforeEach
    fun assumeCachesAreEnabled() {
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache)
    }

    @Test
    @TestMetadata("simple")
    fun testSimple() = withRootDir(File("$TEST_SUITE_PATH/simple")) {
        val lib = compileLibrary("lib") { "lib/lib.kt" copyTo "lib.kt" }
        val main = compileToExecutable("main", lib) { "main/main.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val libKtCacheDir = getLibraryFileCache("lib", "lib/lib.kt", "")
        assertTrue(libKtCacheDir.exists())
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib>'s cache won't be recompiled if nothing has changed.
        val modified = libKtCacheDir.lastModified()
        compileToExecutable("main", lib) { "main/main.kt" copyTo "main.kt" }
        assertTrue(libKtCacheDir.exists())
        assertEquals(modified, libKtCacheDir.lastModified())
    }

    @Test
    @TestMetadata("modifiedFile")
    fun testModifiedFile() = withRootDir(File("$TEST_SUITE_PATH/simple")) {
        val lib = compileLibrary("lib") { "lib/lib.kt" copyTo "lib.kt" }
        val main = compileToExecutable("main", lib) { "main/main.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val libKtCacheDir = getLibraryFileCache("lib", "lib/lib.kt", "")
        assertTrue(libKtCacheDir.exists())
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib>'s cache will be recompiled after changing the file.
        val modified = libKtCacheDir.lastModified()
        val lib1 = compileLibrary("lib") { "lib/lib.1.kt" copyTo "lib.kt" }
        val main1 = compileToExecutable("main", lib1) { "main/main.1.kt" copyTo "main.kt" }
        assertTrue(libKtCacheDir.exists())
        assertNotEquals(modified, libKtCacheDir.lastModified())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("addedFile")
    fun testAddedFile() = withRootDir(File("$TEST_SUITE_PATH/addDeleteFile")) {
        val lib = compileLibrary("lib") { "lib/lib.file1.kt" copyTo "lib.file1.kt" }
        val main = compileToExecutable("main", lib) { "main/main.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val libFile1KtCacheDir = getLibraryFileCache("lib", "lib/lib.file1.kt", "")
        assertTrue(libFile1KtCacheDir.exists())
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib>'s file1 cache won't change after adding an independent file.
        val modified = libFile1KtCacheDir.lastModified()
        val lib1 = compileLibrary("lib") {
            "lib/lib.file1.kt" copyTo "lib.file1.kt"
            "lib/lib.file2.kt" copyTo "lib.file2.kt"
        }
        val main1 = compileToExecutable("main", lib1) { "main/main.1.kt" copyTo "main.kt" }
        assertTrue(libFile1KtCacheDir.exists())
        assertEquals(modified, libFile1KtCacheDir.lastModified())
        val libFile2KtCacheDir = getLibraryFileCache("lib", "lib/lib.file2.kt", "")
        assertTrue(libFile2KtCacheDir.exists())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("removedFile")
    fun testRemovedFile() = withRootDir(File("$TEST_SUITE_PATH/addDeleteFile")) {
        val lib = compileLibrary("lib") {
            "lib/lib.file1.kt" copyTo "lib.file1.kt"
            "lib/lib.file2.kt" copyTo "lib.file2.kt"
        }
        val main = compileToExecutable("main", lib) { "main/main.1.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val libFile1KtCacheDir = getLibraryFileCache("lib", "lib/lib.file1.kt", "")
        assertTrue(libFile1KtCacheDir.exists())
        val libFile2KtCacheDir = getLibraryFileCache("lib", "lib/lib.file2.kt", "")
        assertTrue(libFile2KtCacheDir.exists())
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib>'s file1 cache won't change after removing an independent file.
        val modified = libFile1KtCacheDir.lastModified()
        val lib1 = compileLibrary("lib") { "lib/lib.file1.kt" copyTo "lib.file1.kt" }
        val main1 = compileToExecutable("main", lib1) { "main/main.kt" copyTo "main.kt" }
        assertTrue(libFile1KtCacheDir.exists())
        assertEquals(modified, libFile1KtCacheDir.lastModified())
        // Check, <lib>'s file2 cache has been removed.
        assertFalse(libFile2KtCacheDir.exists())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("renamedFile")
    fun testRenamedFile() = withRootDir(File("$TEST_SUITE_PATH/renameFileOrPackage")) {
        val lib = compileLibrary("lib") { "lib/lib.kt" copyTo "lib.kt" }
        val main = compileToExecutable("main", lib) { "main/main.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val libKtCacheDir = getLibraryFileCache("lib", "lib/lib.kt", "test")
        assertTrue(libKtCacheDir.exists())
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib>'s file cache is moved to other directory.
        val lib1 = compileLibrary("lib") { "lib/lib.kt" copyTo "lib.changed.kt" }
        val main1 = compileToExecutable("main", lib1) { "main/main.kt" copyTo "main.kt" }
        assertFalse(libKtCacheDir.exists())
        val changedLibKtCacheDir = getLibraryFileCache("lib", "lib/lib.changed.kt", "test")
        assertTrue(changedLibKtCacheDir.exists())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("renamedPackage")
    fun testRenamedPackage() = withRootDir(File("$TEST_SUITE_PATH/renameFileOrPackage")) {
        val lib = compileLibrary("lib") { "lib/lib.kt" copyTo "lib.kt" }
        val main = compileToExecutable("main", lib) { "main/main.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val libKtCacheDir = getLibraryFileCache("lib", "lib/lib.kt", "test")
        assertTrue(libKtCacheDir.exists())
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib>'s file cache is moved to other directory.
        val lib1 = compileLibrary("lib") { "lib/lib.1.kt" copyTo "lib.kt" }
        val main1 = compileToExecutable("main", lib1) { "main/main.1.kt" copyTo "main.kt" }
        assertFalse(libKtCacheDir.exists())
        val changedLibKtCacheDir = getLibraryFileCache("lib", "lib/lib.kt", "tezd")
        assertTrue(changedLibKtCacheDir.exists())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("changedFileIndex")
    fun testChangedFileIndex() = withRootDir(File("$TEST_SUITE_PATH/changeFileIndex")) {
        val lib = compileLibrary("lib") {
            "lib/lib.file1.kt" copyTo "libB.kt"
            "lib/lib.file2.kt" copyTo "libC.kt"
        }
        val main = compileToExecutable("main", lib) { "main/main.kt" copyTo "main.kt" }

        assertTrue(main.executableFile.exists())
        val libBKtCacheDir = getLibraryFileCache("lib", "lib/libB.kt", "")
        assertTrue(libBKtCacheDir.exists())
        runExecutableAndVerify(main.testCase, main.testExecutable)

        val modified = libBKtCacheDir.lastModified()
        val lib1 = compileLibrary("lib") {
            "lib/lib.file1.kt" copyTo "libB.kt"
            "lib/lib.file2.kt" copyTo "libA.kt"
        }
        val main1 = compileToExecutable("main", lib1) { "main/main.kt" copyTo "main.kt" }
        assertTrue(libBKtCacheDir.exists())
        assertEquals(modified, libBKtCacheDir.lastModified())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("changedExternalDependencyVersion")
    fun testChangedExternalDependencyVersion() = withRootDir(File("$TEST_SUITE_PATH/externalDependency")) {
        val externalLib = compileLibrary("externalLib") {
            libraryVersion = "1.0"
            outputDir = "external"
            "externalLib/file1.kt" copyTo "file1.kt"
        }
        val userLib = compileLibrary("userLib", externalLib) { "userLib/file1.kt" copyTo "file1.kt" }
        val main = compileToExecutable("main", externalLib, userLib) { "main/main.kt" copyTo "main.kt" }

        assertTrue(main.executableFile.exists())
        val libFile1KtCacheDir = getLibraryFileCache("userLib", "userLib/file1.kt", "")
        assertTrue(libFile1KtCacheDir.exists())
        runExecutableAndVerify(main.testCase, main.testExecutable)

        val modified = libFile1KtCacheDir.lastModified()
        val externalLib1 = compileLibrary("externalLib") {
            libraryVersion = "1.1"
            outputDir = "external"
            "externalLib/file1.kt" copyTo "file1.kt"
        }
        val userLib1 = compileLibrary("userLib", externalLib1) { "userLib/file1.kt" copyTo "file1.kt" }
        val main1 = compileToExecutable("main", externalLib1, userLib1) { "main/main.kt" copyTo "main.kt" }
        assertTrue(libFile1KtCacheDir.exists())
        assertNotEquals(modified, libFile1KtCacheDir.lastModified())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("changedExternalDependency")
    fun testChangedExternalDependency() = withRootDir(File("$TEST_SUITE_PATH/externalDependency")) {
        val externalLib = compileLibrary("externalLib") {
            outputDir = "external"
            "externalLib/file1.kt" copyTo "file1.kt"
            "externalLib/file2.kt" copyTo "file2.kt"
        }
        val userLib = compileLibrary("userLib", externalLib) {
            "userLib/file1.kt" copyTo "file1.kt"
            "userLib/file2.kt" copyTo "file2.kt"
        }
        val main = compileToExecutable("main", externalLib, userLib) {
            "main/main.kt" copyTo "main.kt"
            "main/main2.kt" copyTo "main2.kt"
        }

        assertTrue(main.executableFile.exists())
        val libFile1KtCacheDir = getLibraryFileCache("userLib", "userLib/file1.kt", "")
        val libFile2KtCacheDir = getLibraryFileCache("userLib", "userLib/file2.kt", "")
        assertTrue(libFile1KtCacheDir.exists())
        assertTrue(libFile2KtCacheDir.exists())
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, both file caches will be recompiled after changing only one file in the external library.
        val modified1 = libFile1KtCacheDir.lastModified()
        val modified2 = libFile2KtCacheDir.lastModified()
        val externalLib1 = compileLibrary("externalLib") {
            outputDir = "external"
            "externalLib/file1.kt" copyTo "file1.kt"
            "externalLib/file2.1.kt" copyTo "file2.kt"
        }
        val main1 = compileToExecutable("main", externalLib1, userLib) {
            "main/main.kt" copyTo "main.kt"
            "main/main2.1.kt" copyTo "main2.kt"
        }
        assertTrue(libFile1KtCacheDir.exists())
        assertNotEquals(modified1, libFile1KtCacheDir.lastModified())
        assertTrue(libFile2KtCacheDir.exists())
        assertNotEquals(modified2, libFile2KtCacheDir.lastModified())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("fileDependencies1")
    fun testFileDependencies1() = withRootDir(File("$TEST_SUITE_PATH/fileDependencies1")) {
        val lib = compileLibrary("lib") {
            "lib/lib.file1.kt" copyTo "file1.kt"
            "lib/lib.file2.kt" copyTo "file2.kt"
            "lib/lib.file3.kt" copyTo "file3.kt"
        }
        val main = compileToExecutable("main", lib) { "main/main.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val file1KtCacheDir = getLibraryFileCache("lib", "lib/file1.kt", "test")
        val file2KtCacheDir = getLibraryFileCache("lib", "lib/file2.kt", "test")
        val file3KtCacheDir = getLibraryFileCache("lib", "lib/file3.kt", "test")
        assertTrue(file1KtCacheDir.exists())
        assertTrue(file2KtCacheDir.exists())
        assertTrue(file3KtCacheDir.exists())
        val modified1 = file1KtCacheDir.lastModified()
        val modified2 = file2KtCacheDir.lastModified()
        val modified3 = file3KtCacheDir.lastModified()
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib>'s file1 and file3 caches will be recompiled after changing file1.
        val lib1 = compileLibrary("lib") {
            "lib/lib.file1.1.kt" copyTo "file1.kt"
            "lib/lib.file2.kt" copyTo "file2.kt"
            "lib/lib.file3.kt" copyTo "file3.kt"
        }
        val main1 = compileToExecutable("main", lib1) { "main/main.1.kt" copyTo "main.kt" }
        assertTrue(main1.executableFile.exists())
        assertTrue(file1KtCacheDir.exists())
        assertTrue(file2KtCacheDir.exists())
        assertTrue(file3KtCacheDir.exists())
        assertNotEquals(modified1, file1KtCacheDir.lastModified())
        assertEquals(modified2, file2KtCacheDir.lastModified())
        assertNotEquals(modified3, file3KtCacheDir.lastModified())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("fileDependencies2")
    fun testFileDependencies2() = withRootDir(File("$TEST_SUITE_PATH/fileDependencies2")) {
        val lib1 = compileLibrary("lib1") { "lib1/lib1.kt" copyTo "lib1.kt" }
        val lib2 = compileLibrary("lib2", lib1) {
            "lib2/lib2.file1.kt" copyTo "file1.kt"
            "lib2/lib2.file2.kt" copyTo "file2.kt"
        }
        val main = compileToExecutable("main", lib1, lib2) { "main/main.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val lib1KtCacheDir = getLibraryFileCache("lib1", "lib1/lib1.kt", "test1")
        val lib2File1KtCacheDir = getLibraryFileCache("lib2", "lib2/file1.kt", "test2")
        val lib2File2KtCacheDir = getLibraryFileCache("lib2", "lib2/file2.kt", "test2")
        assertTrue(lib1KtCacheDir.exists())
        assertTrue(lib2File1KtCacheDir.exists())
        assertTrue(lib2File2KtCacheDir.exists())
        val modified11 = lib1KtCacheDir.lastModified()
        val modified21 = lib2File1KtCacheDir.lastModified()
        val modified22 = lib2File2KtCacheDir.lastModified()
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib2>'s file2 cache will be recompiled after changing lib1/lib1.kt.
        val lib11 = compileLibrary("lib1") { "lib1/lib1.1.kt" copyTo "lib1.kt" }
        val lib21 = compileLibrary("lib2", lib11) {
            "lib2/lib2.file1.kt" copyTo "file1.kt"
            "lib2/lib2.file2.kt" copyTo "file2.kt"
        }
        val main1 = compileToExecutable("main", lib11, lib21) { "main/main.1.kt" copyTo "main.kt" }
        assertTrue(main1.executableFile.exists())
        assertTrue(lib1KtCacheDir.exists())
        assertTrue(lib2File1KtCacheDir.exists())
        assertTrue(lib2File2KtCacheDir.exists())
        assertNotEquals(modified11, lib1KtCacheDir.lastModified())
        assertEquals(modified21, lib2File1KtCacheDir.lastModified())
        assertNotEquals(modified22, lib2File2KtCacheDir.lastModified())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("addMethodToOpenClass1")
    fun addMethodToOpenClass1() = withRootDir(File("$TEST_SUITE_PATH/addMethodToOpenClass1")) {
        val lib1 = compileLibrary("lib1") { "lib1/lib1.kt" copyTo "lib1.kt" }
        val lib2 = compileLibrary("lib2", lib1) { "lib2/lib2.kt" copyTo "lib2.kt" }
        val main = compileToExecutable("main", lib1, lib2) { "main/main.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val lib1KtCacheDir = getLibraryFileCache("lib1", "lib1/lib1.kt", "test1")
        val lib2KtCacheDir = getLibraryFileCache("lib2", "lib2/lib2.kt", "test2")
        assertTrue(lib1KtCacheDir.exists())
        assertTrue(lib2KtCacheDir.exists())
        val modified1 = lib1KtCacheDir.lastModified()
        val modified2 = lib2KtCacheDir.lastModified()
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib2>'s cache won't be recompiled after changing lib1/lib1.kt.
        val lib11 = compileLibrary("lib1") { "lib1/lib1.1.kt" copyTo "lib1.kt" }
        val lib21 = compileLibrary("lib2", lib11) { "lib2/lib2.kt" copyTo "lib2.kt" }
        val main1 = compileToExecutable("main", lib11, lib21) { "main/main.kt" copyTo "main.kt" }
        assertTrue(main1.executableFile.exists())
        assertTrue(lib1KtCacheDir.exists())
        assertTrue(lib2KtCacheDir.exists())
        assertNotEquals(modified1, lib1KtCacheDir.lastModified())
        assertEquals(modified2, lib2KtCacheDir.lastModified())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("addMethodToOpenClass2")
    fun addMethodToOpenClass2() = withRootDir(File("$TEST_SUITE_PATH/addMethodToOpenClass2")) {
        val lib1 = compileLibrary("lib1") {
            "lib1/lib1.file1.kt" copyTo "lib1.file1.kt"
            "lib1/lib1.file2.kt" copyTo "lib1.file2.kt"
        }
        val lib2 = compileLibrary("lib2", lib1) { "lib2/lib2.kt" copyTo "lib2.kt" }
        val main = compileToExecutable("main", lib1, lib2) { "main/main.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val lib1File1KtCacheDir = getLibraryFileCache("lib1", "lib1/lib1.file1.kt", "test1")
        val lib1File2KtCacheDir = getLibraryFileCache("lib1", "lib1/lib1.file2.kt", "test1")
        val lib2KtCacheDir = getLibraryFileCache("lib2", "lib2/lib2.kt", "test2")
        assertTrue(lib1File1KtCacheDir.exists())
        assertTrue(lib1File2KtCacheDir.exists())
        assertTrue(lib2KtCacheDir.exists())
        val modified11 = lib1File1KtCacheDir.lastModified()
        val modified12 = lib1File2KtCacheDir.lastModified()
        val modified2 = lib2KtCacheDir.lastModified()
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib2>'s cache won't be recompiled after changing lib1/lib1.file1.kt.
        val lib11 = compileLibrary("lib1") {
            "lib1/lib1.file1.1.kt" copyTo "lib1.file1.kt"
            "lib1/lib1.file2.kt" copyTo "lib1.file2.kt"
        }
        val lib21 = compileLibrary("lib2", lib11) { "lib2/lib2.kt" copyTo "lib2.kt" }
        val main1 = compileToExecutable("main", lib11, lib21) { "main/main.kt" copyTo "main.kt" }
        assertTrue(main1.executableFile.exists())
        assertTrue(lib1File1KtCacheDir.exists())
        assertTrue(lib1File2KtCacheDir.exists())
        assertTrue(lib2KtCacheDir.exists())
        assertNotEquals(modified11, lib1File1KtCacheDir.lastModified())
        assertNotEquals(modified12, lib1File2KtCacheDir.lastModified())
        assertEquals(modified2, lib2KtCacheDir.lastModified())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("addMethodToInterface1")
    fun addMethodToInterface1() = withRootDir(File("$TEST_SUITE_PATH/addMethodToInterface1")) {
        val lib1 = compileLibrary("lib1") { "lib1/lib1.kt" copyTo "lib1.kt" }
        val lib2 = compileLibrary("lib2", lib1) { "lib2/lib2.kt" copyTo "lib2.kt" }
        val main = compileToExecutable("main", lib1, lib2) { "main/main.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val lib1KtCacheDir = getLibraryFileCache("lib1", "lib1/lib1.kt", "test1")
        val lib2KtCacheDir = getLibraryFileCache("lib2", "lib2/lib2.kt", "test2")
        assertTrue(lib1KtCacheDir.exists())
        assertTrue(lib2KtCacheDir.exists())
        val modified1 = lib1KtCacheDir.lastModified()
        val modified2 = lib2KtCacheDir.lastModified()
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib2>'s cache won't be recompiled after changing lib1/lib1.kt.
        val lib11 = compileLibrary("lib1") { "lib1/lib1.1.kt" copyTo "lib1.kt" }
        val lib21 = compileLibrary("lib2", lib11) { "lib2/lib2.kt" copyTo "lib2.kt" }
        val main1 = compileToExecutable("main", lib11, lib21) { "main/main.1.kt" copyTo "main.kt" }
        assertTrue(main1.executableFile.exists())
        assertTrue(lib1KtCacheDir.exists())
        assertTrue(lib2KtCacheDir.exists())
        assertNotEquals(modified1, lib1KtCacheDir.lastModified())
        assertEquals(modified2, lib2KtCacheDir.lastModified())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    @Test
    @TestMetadata("addMethodToInterface2")
    fun addMethodToInterface2() = withRootDir(File("$TEST_SUITE_PATH/addMethodToInterface2")) {
        val lib1 = compileLibrary("lib1") {
            "lib1/lib1.file1.kt" copyTo "lib1.file1.kt"
            "lib1/lib1.file2.kt" copyTo "lib1.file2.kt"
        }
        val lib2 = compileLibrary("lib2", lib1) { "lib2/lib2.kt" copyTo "lib2.kt" }
        val main = compileToExecutable("main", lib1, lib2) { "main/main.kt" copyTo "main.kt" }

        // Check, <lib> has been compiled to cache.
        assertTrue(main.executableFile.exists())
        val lib1File1KtCacheDir = getLibraryFileCache("lib1", "lib1/lib1.file1.kt", "test1")
        val lib1File2KtCacheDir = getLibraryFileCache("lib1", "lib1/lib1.file2.kt", "test1")
        val lib2KtCacheDir = getLibraryFileCache("lib2", "lib2/lib2.kt", "test2")
        assertTrue(lib1File1KtCacheDir.exists())
        assertTrue(lib1File2KtCacheDir.exists())
        assertTrue(lib2KtCacheDir.exists())
        val modified11 = lib1File1KtCacheDir.lastModified()
        val modified12 = lib1File2KtCacheDir.lastModified()
        val modified2 = lib2KtCacheDir.lastModified()
        runExecutableAndVerify(main.testCase, main.testExecutable)

        // Check, <lib2>'s cache won't be recompiled after changing lib1/lib1.file1.kt.
        val lib11 = compileLibrary("lib1") {
            "lib1/lib1.file1.1.kt" copyTo "lib1.file1.kt"
            "lib1/lib1.file2.kt" copyTo "lib1.file2.kt"
        }
        val lib21 = compileLibrary("lib2", lib11) { "lib2/lib2.kt" copyTo "lib2.kt" }
        val main1 = compileToExecutable("main", lib11, lib21) { "main/main.1.kt" copyTo "main.kt" }
        assertTrue(main1.executableFile.exists())
        assertTrue(lib1File1KtCacheDir.exists())
        assertTrue(lib1File2KtCacheDir.exists())
        assertTrue(lib2KtCacheDir.exists())
        assertNotEquals(modified11, lib1File1KtCacheDir.lastModified())
        assertNotEquals(modified12, lib1File2KtCacheDir.lastModified())
        assertEquals(modified2, lib2KtCacheDir.lastModified())
        runExecutableAndVerify(main1.testCase, main1.testExecutable)
    }

    private inline fun withRootDir(rootDir: File, block: RootDirHolder.() -> Unit) = RootDirHolder(rootDir).block()

    private inner class RootDirHolder(val rootDir: File) {
        inline fun compileLibrary(
            targetSrc: String,
            vararg dependencies: TestCompilationArtifact.KLIB,
            block: LibraryBuilder.() -> Unit
        ) = with(LibraryBuilder(this@IncrementalCompilationTest, rootDir, targetSrc, dependencies.asList())) {
            block()
            build()
        }

        inline fun compileToExecutable(
            targetSrc: String,
            vararg dependencies: TestCompilationArtifact.KLIB,
            block: ExecutableBuilder.() -> Unit
        ) = with(ExecutableBuilder(this@IncrementalCompilationTest, rootDir, targetSrc, false, dependencies.asList())) {
            externalLibsDir.mkdirs()
            icCacheDir.mkdirs()
            autoCacheDir.mkdirs()
            +"-Xauto-cache-from=${externalLibsDir.absolutePath}"
            +"-Xauto-cache-dir=${autoCacheDir.absolutePath}"
            +"-Xic-cache-dir=${icCacheDir.absolutePath}"
            +"-Xenable-incremental-compilation"
            +"-verbose"
            block()
            build()
        }
    }

    private val externalLibsDir: File get() = buildDir.resolve("external")
    private val autoCacheDir: File get() = buildDir.resolve("__auto_cache__")
    private val icCacheDir: File get() = buildDir.resolve("__ic_cache__")
    private val cacheFlavor: String
        get() = CacheMode.computeCacheDirName(
            testRunSettings.get<KotlinNativeTargets>().testTarget,
            "STATIC",
            testRunSettings.get<OptimizationMode>() == OptimizationMode.DEBUG,
            partialLinkageEnabled = false
        )

    private fun getLibraryFileCache(libName: String, libFileRelativePath: String, fqName: String): File {
        val libCacheDir = icCacheDir.resolve(cacheFlavor).resolve("$libName-per-file-cache")
        val fileId = cacheFileId(fqName, buildDir.resolve(libFileRelativePath).absolutePath)
        return libCacheDir.resolve(fileId)
    }

    private fun cacheFileId(fqName: String, filePath: String) =
        "${if (fqName == "") "ROOT" else fqName}.${filePath.hashCode().toString(Character.MAX_RADIX)}"

    companion object {
        const val TEST_SUITE_PATH = "native/native.tests/testData/caches/ic"
    }
}
