/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.test.org.jetbrains.kotlin.kapt3.base.incremental

import org.jetbrains.kotlin.kapt3.base.incremental.AnnotationProcessorDependencyCollector
import org.jetbrains.kotlin.kapt3.base.incremental.RuntimeProcType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class AnnotationProcessorDependencyCollectorTest {
    @Test
    fun testAggregating() {
        val aggregating = AnnotationProcessorDependencyCollector(RuntimeProcType.AGGREGATING)
        val generated = listOf("GeneratedA.java", "GeneratedB.java", "GeneratedC.java").map { File(it).toURI() }
        generated.forEach { aggregating.add(it, emptyArray()) }

        assertEquals(aggregating.getGeneratedToSources(), generated.map { File(it) to null }.toMap())
        assertEquals(aggregating.getRuntimeType(), RuntimeProcType.AGGREGATING)
    }

    @Test
    fun testIsolatingWithoutOrigin() {
        val isolating = AnnotationProcessorDependencyCollector(RuntimeProcType.ISOLATING)
        isolating.add(File("GeneratedA.java").toURI(), emptyArray())

        assertEquals(isolating.getRuntimeType(), RuntimeProcType.NON_INCREMENTAL)
        assertEquals(isolating.getGeneratedToSources(), emptyMap<File, File?>())
    }

    @Test
    fun testNonIncremental() {
        val nonIncremental = AnnotationProcessorDependencyCollector(RuntimeProcType.NON_INCREMENTAL)
        nonIncremental.add(File("GeneratedA.java").toURI(), emptyArray())
        nonIncremental.add(File("GeneratedB.java").toURI(), emptyArray())

        assertEquals(nonIncremental.getRuntimeType(), RuntimeProcType.NON_INCREMENTAL)
        assertEquals(nonIncremental.getGeneratedToSources(), emptyMap<File, File?>())
    }
}

