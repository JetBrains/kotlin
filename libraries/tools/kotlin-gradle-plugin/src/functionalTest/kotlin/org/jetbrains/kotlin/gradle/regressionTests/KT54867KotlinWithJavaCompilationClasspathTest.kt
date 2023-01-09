/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.javaSourceSets
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class KT54867KotlinWithJavaCompilationClasspathTest {

    @Test
    fun `test - jvm project - kotlin with java compilation - setting classpath on javaSourceSet`() {
        val project = buildProjectWithJvm {
            enableDefaultStdlibDependency(false)
        }

        val customJavaSourceSet = project.javaSourceSets.create("custom")
        val customKotlinSourceSet = project.kotlinJvmExtension.sourceSets.getByName("custom")
        val customKotlinCompilation = project.kotlinJvmExtension.target.compilations.getByName("custom")

        assertSame(customKotlinSourceSet, customKotlinCompilation.defaultSourceSet)
        assertSame(customJavaSourceSet, customKotlinCompilation.javaSourceSet)

        customJavaSourceSet.compileClasspath = project.files("compile", "classpath")
        customJavaSourceSet.runtimeClasspath = project.files("runtime", "classpath")

        assertEquals(
            project.files("compile", "classpath").files, customKotlinCompilation.compileDependencyFiles.files
        )

        assertEquals(
            project.files("runtime", "classpath").files, customKotlinCompilation.runtimeDependencyFiles.files
        )
    }
}