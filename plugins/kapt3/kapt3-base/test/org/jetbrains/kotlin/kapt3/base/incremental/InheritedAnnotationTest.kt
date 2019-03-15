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

private val MY_TEST_DIR = File("plugins/kapt3/kapt3-base/testData/runner/incremental/complex/inherited")

class TestInheritedAnnotation {

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
                "BaseClass.java",
                "InheritableAnnotation.java",
                "ExtendsBase.java"
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
    fun testAnnotationInherited() {
        val shouldInheritAnnotation = cache.javaCache.getStructure(MY_TEST_DIR.resolve("ExtendsBase.java"))!!

        assertEquals(setOf("test.ExtendsBase"), shouldInheritAnnotation.getDeclaredTypes())
        assertEquals(setOf("test.InheritableAnnotation"), shouldInheritAnnotation.getMentionedAnnotations())
        assertEquals(emptySet<String>(), shouldInheritAnnotation.getPrivateTypes())
        assertEquals(
            setOf(
                "test.ExtendsBase",
                "test.BaseClass"
            ), shouldInheritAnnotation.getMentionedTypes()
        )
        assertEquals(emptyMap<String, String>(), shouldInheritAnnotation.getDefinedConstants())
    }
}