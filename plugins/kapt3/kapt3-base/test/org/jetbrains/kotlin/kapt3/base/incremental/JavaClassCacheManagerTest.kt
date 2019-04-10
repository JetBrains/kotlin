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

    @Before
    fun setUp() {
        cacheDir = tmp.newFolder()
        cache = JavaClassCacheManager(cacheDir)
    }


    @Test
    fun testClosingCache() {
        cache.close()

        assertTrue(cacheDir.resolve("java-cache.bin").exists())
        assertTrue(cacheDir.resolve("apt-cache.bin").exists())
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

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf(File("Mentioned.java")), emptyList()) as SourcesToReprocess.Incremental
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

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf(File("Mentioned.java")), emptyList()) as SourcesToReprocess.Incremental
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

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf(File("TwoTypes.java")), emptyList()) as SourcesToReprocess.Incremental
        assertEquals(
            listOf(
                File("TwoTypes.java").absoluteFile,
                File("ReferencesTwoTypes.java").absoluteFile,
                File("ReferencesAnotherType.java").absoluteFile
            ), dirtyFiles.toReprocess
        )
    }

    @Test
    fun testWithClasspathChanges() {
        SourceFileStructure(File("Src.java").toURI()).also {
            it.addDeclaredType("test.Src")
            it.addMentionedType("test.Mentioned")
            cache.javaCache.addSourceStructure(it)
        }
        prepareForIncremental()

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf(), listOf("test/Mentioned")) as SourcesToReprocess.Incremental
        assertEquals(listOf(File("Src.java").absoluteFile), dirtyFiles.toReprocess)
    }

    @Test
    fun testReferencedConstant() {
        SourceFileStructure(File("Constants.java").toURI()).also {
            it.addDeclaredType("test.Constants")
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("MentionsConst.java").toURI()).also {
            it.addDeclaredType("test.MentionsConst")
            it.addMentionedConstant("test.Constants", "CONST")
            cache.javaCache.addSourceStructure(it)
        }
        SourceFileStructure(File("MentionsOtherConst.java").toURI()).also {
            it.addDeclaredType("test.MentionsOtherConst")
            it.addMentionedConstant("test.OtherConstants", "CONST")
            cache.javaCache.addSourceStructure(it)
        }
        prepareForIncremental()

        val dirtyFiles =
            cache.invalidateAndGetDirtyFiles(
                listOf(File("Constants.java")), emptyList()
            ) as SourcesToReprocess.Incremental
        assertEquals(
            listOf(File("Constants.java").absoluteFile, File("MentionsConst.java").absoluteFile),
            dirtyFiles.toReprocess
        )
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
        cache = JavaClassCacheManager(cacheDir)
    }
}
