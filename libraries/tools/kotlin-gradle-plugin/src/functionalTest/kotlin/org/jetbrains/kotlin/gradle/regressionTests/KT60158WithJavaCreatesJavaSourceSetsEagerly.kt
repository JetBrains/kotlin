/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.javaSourceSets
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KT60158WithJavaCreatesJavaSourceSetsEagerly {
    @Test
    fun `test jvm withJava creates corresponding java source sets eagerly`() = buildProjectWithMPP().runLifecycleAwareTest {
        assertNull(javaSourceSets.findByName("main"))

        multiplatformExtension.jvm()
        assertNull(
            javaSourceSets.findByName("main"),
            "Expected no java source set 'main' to be created without 'withJava()'"
        )

        multiplatformExtension.jvm().withJava()
        assertNotNull(
            javaSourceSets.findByName("main"),
            "Expected java source set 'main' to be created eagerly"
        )
        assertNotNull(
            javaSourceSets.findByName("test"),
            "Expected java source set 'test' to be created eagerly"
        )

        multiplatformExtension.jvm().compilations.create("custom")
        assertNotNull(
            javaSourceSets.findByName("custom"),
            "Expected java source set 'custom' to be created eagerly"
        )
    }
}
