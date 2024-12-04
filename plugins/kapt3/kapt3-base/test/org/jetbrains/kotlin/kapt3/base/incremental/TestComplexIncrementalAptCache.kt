/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import org.jetbrains.kotlin.kapt3.base.newCacheFolder
import org.jetbrains.kotlin.kapt3.base.newGeneratedSourcesFolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private val MY_TEST_DIR = File("plugins/kapt3/kapt3-base/testData/runner/incremental/complex")

class TestComplexIncrementalAptCache {

    companion object {
        private lateinit var cache: JavaClassCacheManager
        private lateinit var generatedSources: File

        @JvmStatic
        @BeforeAll
        fun setUp(@TempDir tmp: File) {
            cache = JavaClassCacheManager(tmp.newCacheFolder())
            generatedSources = tmp.newGeneratedSourcesFolder()
            cache.close()
            val processor = SimpleProcessor().toAggregating()
            val srcFiles = listOf(
                "MyEnum.java",
                "MyNumber.java",
                "NumberAnnotation.java",
                "NumberException.java",
                "NumberHolder.java",
                "NumberManager.java",
                "GenericNumber.java"
            ).map { File(MY_TEST_DIR, it) }
            runAnnotationProcessing(
                srcFiles,
                listOf(processor),
                generatedSources
            ) { elements, trees -> MentionedTypesTaskListener(cache.javaCache, elements, trees) }
            cache.updateCache(listOf(processor), false)
        }
    }

    @Test
    fun testEnum() {
        val myEnum = cache.javaCache.getStructure(MY_TEST_DIR.resolve("MyEnum.java"))!! as SourceFileStructure

        assertEquals(setOf("test.MyEnum"), myEnum.declaredTypes)
        assertEquals(emptySet<String>(), myEnum.getMentionedAnnotations())
        assertEquals(emptySet<String>(), myEnum.getPrivateTypes())
        assertEquals(setOf("test.MyEnum", "test.TypeGeneratedByApt"), myEnum.getMentionedTypes())
    }

    @Test
    fun testMyNumber() {
        val myNumber = cache.javaCache.getStructure(MY_TEST_DIR.resolve("MyNumber.java"))!! as SourceFileStructure

        assertEquals(
            setOf(
                "test.MyNumber",
                "test.FieldAnnotation",
                "test.MethodAnnotation",
                "test.ParameterAnnotation",
                "test.TypeUseAnnotation",
                "test.AnotherTypeUseAnnotation",
                "test.ThrowTypeUseAnnotation"
            ), myNumber.declaredTypes
        )
        assertEquals(
            setOf(
                "java.lang.annotation.Target",
                "test.FieldAnnotation",
                "test.ParameterAnnotation",
                "test.MethodAnnotation",
                "test.TypeUseAnnotation",
                "test.AnotherTypeUseAnnotation",
                "test.ThrowTypeUseAnnotation"
            ), myNumber.getMentionedAnnotations()
        )
        assertEquals(
            setOf(
                "test.FieldAnnotation",
                "java.lang.String",
                "test.ParameterAnnotation",
                "test.MethodAnnotation",
                "test.AnotherTypeUseAnnotation",
                "test.ThrowTypeUseAnnotation",
                "java.lang.Number",
                "java.lang.RuntimeException"
            ), myNumber.getPrivateTypes()
        )
        assertEquals(
            setOf(
                "test.MyNumber",
                "java.lang.annotation.Target",
                "test.FieldAnnotation",
                "test.ParameterAnnotation",
                "test.MethodAnnotation",
                "test.TypeUseAnnotation",
                "test.AnotherTypeUseAnnotation",
                "test.ThrowTypeUseAnnotation",
                "java.util.HashSet"
            ), myNumber.getMentionedTypes()
        )
    }

    @Test
    fun testAnnotation() {
        val numberAnnotation = cache.javaCache.getStructure(MY_TEST_DIR.resolve("NumberAnnotation.java"))!! as SourceFileStructure

        assertEquals(setOf("test.NumberAnnotation", "test.BaseAnnotation"), numberAnnotation.declaredTypes)
        assertEquals(setOf("test.BaseAnnotation"), numberAnnotation.getMentionedAnnotations())
        assertEquals(emptySet<String>(), numberAnnotation.getPrivateTypes())
        assertEquals(
            setOf(
                "test.BaseAnnotation",
                "test.NumberAnnotation",
                "java.lang.Class",
                "test.MyEnum",
                "test.NumberManager"
            ), numberAnnotation.getMentionedTypes()
        )
    }

    @Test
    fun testNumberException() {
        val numberException = cache.javaCache.getStructure(MY_TEST_DIR.resolve("NumberException.java"))!! as SourceFileStructure

        assertEquals(setOf("test.NumberException"), numberException.declaredTypes)
        assertEquals(emptySet<String>(), numberException.getMentionedAnnotations())
        assertEquals(emptySet<String>(), numberException.getPrivateTypes())
        assertEquals(setOf("test.NumberException", "java.lang.RuntimeException"), numberException.getMentionedTypes())
    }

    @Test
    fun testNumberHolder() {
        val numberHolder = cache.javaCache.getStructure(MY_TEST_DIR.resolve("NumberHolder.java"))!! as SourceFileStructure

        assertEquals(setOf("test.NumberHolder", "test.NumberHolder.MyInnerClass"), numberHolder.declaredTypes)
        assertEquals(setOf("test.NumberAnnotation"), numberHolder.getMentionedAnnotations())
        assertEquals(setOf("test.NumberManager"), numberHolder.getPrivateTypes())
        assertEquals(
            setOf(
                "test.NumberHolder",
                "test.NumberHolder.MyInnerClass",
                "test.NumberAnnotation",
                "test.NumberManager",
                "test.MyNumber",
                "java.util.HashSet",
                "java.lang.Runnable",
                "java.lang.String",
                "test.NumberException"
            ), numberHolder.getMentionedTypes()
        )
    }

    @Test
    fun testNumberManager() {
        val numberManager = cache.javaCache.getStructure(MY_TEST_DIR.resolve("NumberManager.java"))!! as SourceFileStructure

        assertEquals(setOf("test.NumberManager"), numberManager.declaredTypes)
        assertEquals(emptySet<String>(), numberManager.getMentionedAnnotations())
        assertEquals(setOf("test.MyEnum"), numberManager.getPrivateTypes())
        assertEquals(
            setOf(
                "test.NumberManager",
                "java.lang.String",
                "test.NumberHolder"
            ), numberManager.getMentionedTypes()
        )
    }

    @Test
    fun testGenericNumber() {
        val genericNumber = cache.javaCache.getStructure(MY_TEST_DIR.resolve("GenericNumber.java"))!! as SourceFileStructure

        assertEquals(setOf("test.GenericNumber"), genericNumber.declaredTypes)
        assertEquals(emptySet<String>(), genericNumber.getMentionedAnnotations())
        assertEquals(emptySet<String>(), genericNumber.getPrivateTypes())
        assertEquals(
            setOf(
                "test.GenericNumber",
                "java.util.HashSet",
                "java.lang.Runnable",
                "java.lang.Cloneable",
                "java.lang.CharSequence",
                "java.lang.Number"
            ), genericNumber.getMentionedTypes()
        )
    }
}
