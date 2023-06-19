/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertEquals

class KT57023CompileTaskDescriptionsTest {
    @Test
    fun `test - description of compile tasks`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.linuxX64()
        kotlin.linuxArm64()
        kotlin.jvm()
        kotlin.js(KotlinJsCompilerType.IR)
        kotlin.applyDefaultHierarchyTemplate()

        project.evaluate()

        assertEquals(
            "Compiles the kotlin sources in compilation 'commonMain' in target 'metadata' to Metadata.",
            kotlin.metadata().compilations.getByName("commonMain").compileTaskProvider.get().description
        )

        assertEquals(
            "Compiles a klibrary from the 'linuxMain' compilation in target 'metadata'.",
            kotlin.metadata().compilations.getByName("linuxMain").compileTaskProvider.get().description
        )

        assertEquals(
            "Compiles the compilation 'main' in target 'jvm'.",
            kotlin.jvm().compilations.getByName("main").compileTaskProvider.get().description
        )

        assertEquals(
            "Compiles the compilation 'test' in target 'jvm'.",
            kotlin.jvm().compilations.getByName("test").compileTaskProvider.get().description
        )

        assertEquals(
            "Compiles a klibrary from the 'main' compilation in target 'linuxX64'.",
            kotlin.linuxX64().compilations.getByName("main").compileTaskProvider.get().description
        )
    }
}