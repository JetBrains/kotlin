/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import org.jetbrains.kotlin.kapt3.base.newCacheFolder
import org.jetbrains.kotlin.kapt3.base.newFolder
import org.jetbrains.kotlin.kapt3.base.newGeneratedSourcesFolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private val MY_TEST_DIR = File("plugins/kapt3/kapt3-base/testData/runner/incremental/constants")

class ReferencedConstantsTest {

    companion object {
        private lateinit var cache: JavaClassCacheManager
        private lateinit var generatedSources: File

        @JvmStatic
        @BeforeAll
        fun setUp(@TempDir tmp: File) {
            val compiledClasses = tmp.newFolder("compiledClasses")
            compileSources(listOf(MY_TEST_DIR.resolve("CKlass.java")), compiledClasses)

            cache = JavaClassCacheManager(tmp.newCacheFolder())
            generatedSources = tmp.newGeneratedSourcesFolder()
            cache.close()
            val processor = SimpleProcessor().toAggregating()
            val srcFiles = listOf(
                "A.java",
                "B.java",
                "AnnotationA.java",
                "AnnotatedType.java"
            ).map { File(MY_TEST_DIR, it) }
            runAnnotationProcessing(
                srcFiles,
                listOf(processor),
                generatedSources,
                listOf(compiledClasses)
            ) { elements, trees -> MentionedTypesTaskListener(cache.javaCache, elements, trees) }
            cache.updateCache(listOf(processor), false)
        }
    }

    @Test
    fun testConstantInField() {
        val klassA = cache.javaCache.getStructure(MY_TEST_DIR.resolve("A.java"))!! as SourceFileStructure

        assertEquals(setOf("test.A"), klassA.declaredTypes)
        assertEquals(emptySet<String>(), klassA.getMentionedAnnotations())
        assertEquals(emptySet<String>(), klassA.getPrivateTypes())
        assertEquals(setOf("test.A"), klassA.getMentionedTypes())
        assertEquals(
            mapOf(
                "test.B" to setOf("INT_VALUE"),
                "test.CKlass" to setOf("INT_VALUE")
            ), klassA.getMentionedConstants()
        )
    }

    @Test
    fun testConstantInDefaultValue() {
        val annotationA = cache.javaCache.getStructure(MY_TEST_DIR.resolve("AnnotationA.java"))!! as SourceFileStructure

        assertEquals(setOf("test.AnnotationA"), annotationA.declaredTypes)
        assertEquals(emptySet<String>(), annotationA.getMentionedAnnotations())
        assertEquals(emptySet<String>(), annotationA.getPrivateTypes())
        assertEquals(setOf("test.AnnotationA"), annotationA.getMentionedTypes())
        assertEquals(mapOf("test.B" to setOf("INT_VALUE")), annotationA.getMentionedConstants())
    }

    @Test
    fun testConstantInAnnotationElementValue() {
        val annotated = cache.javaCache.getStructure(MY_TEST_DIR.resolve("AnnotatedType.java"))!! as SourceFileStructure

        assertEquals(setOf("test.AnnotatedType"), annotated.declaredTypes)
        assertEquals(setOf("test.AnnotationA"), annotated.getMentionedAnnotations())
        assertEquals(emptySet<String>(), annotated.getPrivateTypes())
        assertEquals(setOf("test.AnnotatedType", "test.AnnotationA"), annotated.getMentionedTypes())
        assertEquals(
            mapOf(
                "test.B" to setOf("INT_VALUE"),
                "test.CKlass" to setOf("INT_VALUE")
            ), annotated.getMentionedConstants()
        )
    }
}
