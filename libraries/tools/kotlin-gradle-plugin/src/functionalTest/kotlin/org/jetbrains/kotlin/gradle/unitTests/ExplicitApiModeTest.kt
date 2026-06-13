/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * Configuration-phase counterpart of the JVM/MPP scenarios of the former KGP `ExplicitApiIT`: the explicit-api mode
 * configured via the `kotlin {}` DSL must end up as the `-Xexplicit-api` compiler argument of the production compile
 * tasks (and not the test ones). Verified by computing each task's arguments at configuration time. The strict-mode
 * compilation failure and the Android variant scenarios stay in the integration test (they need a real build / AGP).
 */
class ExplicitApiModeTest {

    private val explicitApiWarningArgument = "-Xexplicit-api=warning"

    @Test
    fun explicitApiWarningAppliesToMainNotTest() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.explicitApiWarning()
        project.evaluate()

        assertContains(project.compileArguments("compileKotlin"), explicitApiWarningArgument)
        // the explicit-api mode must not leak into the test compilation
        assertFalse(
            explicitApiWarningArgument in project.compileArguments("compileTestKotlin"),
            "explicit-api must not be applied to compileTestKotlin",
        )
    }

    @Test
    fun explicitApiWarningCoexistsWithFreeCompilerArgs() {
        // KT-57653: an explicitly added free compiler arg must not drop the explicit-api argument.
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.apply {
            explicitApiWarning()
            compilerOptions.freeCompilerArgs.add("-Xcontext-parameters")
        }
        project.evaluate()

        val arguments = project.compileArguments("compileKotlin")
        assertContains(arguments, "-Xcontext-parameters")
        assertContains(arguments, explicitApiWarningArgument)
    }

    @Test
    fun explicitApiWarningReachesAllMppTargets() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                js()
                linuxX64()
                applyDefaultHierarchyTemplate()
                explicitApiWarning()
            }
        }
        project.evaluate()

        for (task in listOf("compileCommonMainKotlinMetadata", "compileKotlinJvm", "compileKotlinJs", "compileKotlinLinuxX64")) {
            assertContains(project.compileArguments(task), explicitApiWarningArgument, "$task is missing explicit-api")
        }
    }

    private fun Project.compileArguments(taskName: String): List<String> {
        val task = tasks.getByName(taskName) as KotlinCompilerArgumentsProducer
        return ArgumentUtils.convertArgumentsToStringList(task.createCompilerArguments(lenient))
    }
}
