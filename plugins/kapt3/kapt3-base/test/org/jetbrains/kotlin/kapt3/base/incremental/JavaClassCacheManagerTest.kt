/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental;

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.ObjectOutputStream

class JavaClassCacheManagerTest {

    @Rule
    @JvmField
    var tmp = TemporaryFolder()

    private lateinit var cache: JavaClassCacheManager
    private lateinit var cacheDir: File
    private lateinit var classpathHistory: File

    @Before
    fun setUp() {
        cacheDir = tmp.newFolder()
        classpathHistory = tmp.newFolder()
        cache = JavaClassCacheManager(cacheDir, classpathHistory)
    }


    @Test
    fun testClosingCache() {
        cache.close()

        assertTrue(cacheDir.resolve("java-cache.bin").exists())
        assertTrue(cacheDir.resolve("apt-cache.bin").exists())
        assertTrue(cacheDir.resolve("last-build-ts.bin").exists())
    }

    @Test
    fun testMentionedTypes() {
        SourceFileStructure(File("Mentioned.java").toURI()).also {
            it.addDeclaredType("test.Mentioned")
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("Src.java").toURI()).also {
            it.addDeclaredType("test.Src")
            it.addMentionedType("test.Mentioned")
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("ReferencesSrc.java").toURI()).also {
            it.addDeclaredType("test.ReferencesSrc")
            it.addPrivateType("test.Src")
            cache.javaCache.addSourceStructure(it)
        }
        prepareForIncremental()

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf(File("Mentioned.java"))) as SourcesToReprocess.Incremental
        assertEquals(
            listOf(
                File("Mentioned.java").absoluteFile,
                File("Src.java").absoluteFile,
                File("ReferencesSrc.java").absoluteFile
            ), dirtyFiles.toReprocess
        )
    }

    @Test
    fun testPrivateTypes() {
        SourceFileStructure(File("Mentioned.java").toURI()).also {
            it.addDeclaredType("test.Mentioned")
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("Src.java").toURI()).also {
            it.addDeclaredType("test.Src")
            it.addPrivateType("test.Mentioned")
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("ReferencesSrc.java").toURI()).also {
            it.addDeclaredType("test.ReferencesSrc")
            it.addPrivateType("test.Src")
            cache.javaCache.addSourceStructure(it)
        }
        prepareForIncremental()

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf(File("Mentioned.java"))) as SourcesToReprocess.Incremental
        assertEquals(
            listOf(
                File("Mentioned.java").absoluteFile,
                File("Src.java").absoluteFile
            ), dirtyFiles.toReprocess
        )
    }

    @Test
    fun testMultipleDeclared() {
        SourceFileStructure(File("TwoTypes.java").toURI()).also {
            it.addDeclaredType("test.TwoTypes")
            it.addDeclaredType("test.AnotherType")
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("ReferencesTwoTypes.java").toURI()).also {
            it.addDeclaredType("test.ReferencesTwoTypes")
            it.addPrivateType("test.TwoTypes")
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("ReferencesAnotherType.java").toURI()).also {
            it.addDeclaredType("test.ReferencesAnotherType")
            it.addPrivateType("test.AnotherType")
            cache.javaCache.addSourceStructure(it)
        }
        prepareForIncremental()

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf(File("TwoTypes.java"))) as SourcesToReprocess.Incremental
        assertEquals(
            listOf(
                File("TwoTypes.java").absoluteFile,
                File("ReferencesTwoTypes.java").absoluteFile,
                File("ReferencesAnotherType.java").absoluteFile
            ), dirtyFiles.toReprocess
        )
    }

    @Test
    fun testNoClasspathHistory() {
        SourceFileStructure(File("Src.java").toURI()).also {
            it.addDeclaredType("test.Src")
            cache.javaCache.addSourceStructure(it)
        }
        cache.close()
        classpathHistory.resolve(Long.MAX_VALUE.toString()).createNewFile()

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf())
        assertEquals(SourcesToReprocess.FullRebuild, dirtyFiles)
    }

    @Test
    fun testWithClasspathHistoryButNoNewChanges() {
        SourceFileStructure(File("Src.java").toURI()).also {
            it.addDeclaredType("test.Src")
            it.addMentionedType("test.Mentioned")
            cache.javaCache.addSourceStructure(it)
        }
        prepareForIncremental()
        ObjectOutputStream(classpathHistory.resolve("0").outputStream()).use {
            it.writeObject(listOf("test.Mentioned"))
        }

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf()) as SourcesToReprocess.Incremental
        assertEquals(emptyList<File>(), dirtyFiles.toReprocess)
    }

    @Test
    fun testWithClasspathHistoryWithChanges() {
        SourceFileStructure(File("Src.java").toURI()).also {
            it.addDeclaredType("test.Src")
            it.addMentionedType("test.Mentioned")
            cache.javaCache.addSourceStructure(it)
        }
        prepareForIncremental()
        ObjectOutputStream(classpathHistory.resolve(Long.MAX_VALUE.toString()).outputStream()).use {
            it.writeObject(listOf("test.Mentioned"))
        }

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf())
        assertEquals(SourcesToReprocess.FullRebuild, dirtyFiles)
    }

    @Test
    fun testDefinesConstant() {
        SourceFileStructure(File("Constants.java").toURI()).also {
            it.addDeclaredType("test.Constants")
            it.addDefinedConstant("CONST", 123)
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("Unrelated1.java").toURI()).also {
            it.addDeclaredType("test.Unrelated1")
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("Unrelated2.java").toURI()).also {
            it.addDeclaredType("test.Unrelated2")
            cache.javaCache.addSourceStructure(it)
        }
        prepareForIncremental()

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf(File("Constants.java")))
        assertEquals(SourcesToReprocess.FullRebuild, dirtyFiles)
    }

    @Test
    fun testWithAnnotations() {
        SourceFileStructure(File("Annotated1.java").toURI()).also {
            it.addDeclaredType("test.Annotated1")
            it.addMentionedAnnotations("test.Annotation")
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("Annotated2.java").toURI()).also {
            it.addDeclaredType("test.Annotated2")
            it.addMentionedAnnotations("com.test.MyAnnotation")
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("Annotated3.java").toURI()).also {
            it.addDeclaredType("test.Annotated3")
            it.addMentionedAnnotations("Runnable")
            cache.javaCache.addSourceStructure(it)
        }
        prepareForIncremental()

        assertEquals(setOf(File("Annotated1.java").absoluteFile), cache.javaCache.invalidateEntriesAnnotatedWith(setOf("test.Annotation")))
        assertEquals(setOf(File("Annotated2.java").absoluteFile), cache.javaCache.invalidateEntriesAnnotatedWith(setOf("com.test.*")))
        assertEquals(setOf(File("Annotated3.java").absoluteFile), cache.javaCache.invalidateEntriesAnnotatedWith(setOf("*")))
    }

    private fun prepareForIncremental() {
        cache.close()
        classpathHistory.resolve("0").createNewFile()
        cache = JavaClassCacheManager(cacheDir, classpathHistory)
    }
}
