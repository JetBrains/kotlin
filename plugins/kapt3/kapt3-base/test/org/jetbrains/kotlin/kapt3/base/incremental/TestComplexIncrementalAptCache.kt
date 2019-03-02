/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.test.org.jetbrains.kotlin.kapt3.base.incremental

import org.jetbrains.kotlin.kapt3.base.incremental.JavaClassCacheManager
import org.jetbrains.kotlin.kapt3.base.incremental.MentionedTypesTaskListener
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private val MY_TEST_DIR = File("plugins/kapt3/kapt3-base/testData/runner/incremental/complex")

class TestComplexIncrementalAptCache {

    companion object {
        @ClassRule
        @JvmField
        var tmp = TemporaryFolder()

        private lateinit var cache: JavaClassCacheManager
        private lateinit var generatedSources: File

        @JvmStatic
        @BeforeClass
        fun setUp() {
            val classpathHistory = tmp.newFolder()
            cache = JavaClassCacheManager(tmp.newFolder(), classpathHistory)
            generatedSources = tmp.newFolder()
            cache.close()
            classpathHistory.resolve("0").createNewFile()
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
            ) { trees -> MentionedTypesTaskListener(cache.javaCache, trees) }
            cache.updateCache(listOf(processor))
        }
    }

    @Test
    fun testEnum() {
        val myEnum = cache.javaCache.getStructure(MY_TEST_DIR.resolve("MyEnum.java"))!!

        assertEquals(setOf("test.MyEnum"), myEnum.getDeclaredTypes())
        assertEquals(emptySet<String>(), myEnum.getMentionedAnnotations())
        assertEquals(emptySet<String>(), myEnum.getPrivateTypes())
        assertEquals(setOf("test.MyEnum", "test.TypeGeneratedByApt"), myEnum.getMentionedTypes())
        assertEquals(emptyMap<String, Any>(), myEnum.getDefinedConstants())
    }

    @Test
    fun testMyNumber() {
        val myNumber = cache.javaCache.getStructure(MY_TEST_DIR.resolve("MyNumber.java"))!!

        assertEquals(
            setOf(
                "test.MyNumber",
                "test.FieldAnnotation",
                "test.MethodAnnotation",
                "test.ParameterAnnotation",
                "test.TypeUseAnnotation",
                "test.AnotherTypeUseAnnotation",
                "test.ThrowTypeUseAnnotation"
            ), myNumber.getDeclaredTypes()
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
        assertEquals(emptyMap<String, String>(), myNumber.getDefinedConstants())
    }

    @Test
    fun testAnnotation() {
        val numberAnnotation = cache.javaCache.getStructure(MY_TEST_DIR.resolve("NumberAnnotation.java"))!!

        assertEquals(setOf("test.NumberAnnotation", "test.BaseAnnotation"), numberAnnotation.getDeclaredTypes())
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
        assertEquals(emptyMap<String, String>(), numberAnnotation.getDefinedConstants())
    }

    @Test
    fun testNumberException() {
        val numberException = cache.javaCache.getStructure(MY_TEST_DIR.resolve("NumberException.java"))!!

        assertEquals(setOf("test.NumberException"), numberException.getDeclaredTypes())
        assertEquals(emptySet<String>(), numberException.getMentionedAnnotations())
        assertEquals(emptySet<String>(), numberException.getPrivateTypes())
        assertEquals(setOf("test.NumberException", "java.lang.RuntimeException"), numberException.getMentionedTypes())
        assertEquals(emptyMap<String, String>(), numberException.getDefinedConstants())
    }

    @Test
    fun testNumberHolder() {
        val numberHolder = cache.javaCache.getStructure(MY_TEST_DIR.resolve("NumberHolder.java"))!!

        assertEquals(setOf("test.NumberHolder", "test.NumberHolder.MyInnerClass"), numberHolder.getDeclaredTypes())
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
        assertEquals(emptyMap<String, String>(), numberHolder.getDefinedConstants())
    }

    @Test
    fun testNumberManager() {
        val numberManager = cache.javaCache.getStructure(MY_TEST_DIR.resolve("NumberManager.java"))!!

        assertEquals(setOf("test.NumberManager"), numberManager.getDeclaredTypes())
        assertEquals(emptySet<String>(), numberManager.getMentionedAnnotations())
        assertEquals(setOf("test.MyEnum"), numberManager.getPrivateTypes())
        assertEquals(
            setOf(
                "test.NumberManager",
                "java.lang.String",
                "test.NumberHolder"
            ), numberManager.getMentionedTypes()
        )
        assertEquals(mapOf("CONST" to "STRING_CONST", "INT_CONST" to 246), numberManager.getDefinedConstants())
    }

    @Test
    fun testGenericNumber() {
        val genericNumber = cache.javaCache.getStructure(MY_TEST_DIR.resolve("GenericNumber.java"))!!

        assertEquals(setOf("test.GenericNumber"), genericNumber.getDeclaredTypes())
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
        assertEquals(emptyMap<String, String>(), genericNumber.getDefinedConstants())
    }
}