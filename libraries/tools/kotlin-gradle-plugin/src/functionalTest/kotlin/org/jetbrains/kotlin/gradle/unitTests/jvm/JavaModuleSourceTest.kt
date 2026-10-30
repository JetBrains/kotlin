/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.jvm

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

internal class JavaModuleSourceTest {

    @Test
    fun multiplatformJvmTargetDetectsModuleInfoInJavaSources() {
        val project = buildProjectWithMPP { kotlin { jvm() } }
        project.projectDir.resolve("src/jvmMain/java").apply { mkdirs() }.resolve("module-info.java").writeText("module example {}")

        project.evaluate()

        val task = project.tasks.getByName("compileKotlinJvm") as KotlinCompile
        assertEquals(true, task.inferModulePath.get())
        assertEquals(true, task.hasJavaModuleSource.get())
    }

    @Test
    fun multiplatformJvmTargetWithoutModuleInfoHasNoJavaModuleSource() {
        val project = buildProjectWithMPP { kotlin { jvm() } }

        project.evaluate()

        val task = project.tasks.getByName("compileKotlinJvm") as KotlinCompile
        assertEquals(false, task.hasJavaModuleSource.get())
    }
}
