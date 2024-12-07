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

private val MY_TEST_DIR = File("plugins/kapt3/kapt3-base/testData/runner/incremental/complex/inherited")

class TestInheritedAnnotation {
    companion object {
        private lateinit var cache: JavaClassCacheManager
        private lateinit var generatedSources: File

        @JvmStatic
        @BeforeAll
        fun setUp(@TempDir tmp: File) {
            val classpathHistory = tmp.newFolder("classpathHistory")
            cache = JavaClassCacheManager(tmp.newCacheFolder())
            generatedSources = tmp.newGeneratedSourcesFolder()
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
            ) { elementUtils, trees -> MentionedTypesTaskListener(cache.javaCache, elementUtils, trees) }
            cache.updateCache(listOf(processor), false)
        }
    }

    @Test
    fun testAnnotationInherited() {
        val shouldInheritAnnotation = cache.javaCache.getStructure(MY_TEST_DIR.resolve("ExtendsBase.java"))!! as SourceFileStructure

        assertEquals(setOf("test.ExtendsBase"), shouldInheritAnnotation.declaredTypes)
        assertEquals(setOf("test.InheritableAnnotation"), shouldInheritAnnotation.getMentionedAnnotations())
        assertEquals(emptySet<String>(), shouldInheritAnnotation.getPrivateTypes())
        assertEquals(
            setOf(
                "test.ExtendsBase",
                "test.BaseClass"
            ), shouldInheritAnnotation.getMentionedTypes()
        )
    }
}
