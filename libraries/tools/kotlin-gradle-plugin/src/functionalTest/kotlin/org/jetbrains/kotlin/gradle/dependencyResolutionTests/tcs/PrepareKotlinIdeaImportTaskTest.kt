/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(Idea222Api::class)

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.plugin.ide.prepareKotlinIdeaImportTask
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.assertDependsOn
import org.jetbrains.kotlin.gradle.util.assertTaskDependenciesEquals
import org.jetbrains.kotlin.gradle.util.enableCInteropCommonization
import org.jetbrains.kotlin.tooling.core.UnsafeApi
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(UnsafeApi::class)
class PrepareKotlinIdeaImportTaskTest {

    private companion object {
        const val prepareKotlinIdeaImportTaskName = "prepareKotlinIdeaImport"
    }

    @Test
    fun `test prepareKotlinIdeaImport task depending on commonizer and cinterop tasks`() {
        val rootProject = ProjectBuilder.builder().build() as ProjectInternal
        val subproject = ProjectBuilder.builder().withParent(rootProject).withName("subproject").build() as ProjectInternal
        rootProject.enableCInteropCommonization()
        subproject.enableCInteropCommonization()

        val kotlin = subproject.applyMultiplatformPlugin()
        kotlin.linuxX64()
        kotlin.linuxArm64()

        rootProject.evaluate()
        subproject.evaluate()

        assertTrue(
            prepareKotlinIdeaImportTaskName in subproject.tasks.names,
            "Expected a task named '$prepareKotlinIdeaImportTaskName' to be registered"
        )

        subproject.prepareKotlinIdeaImportTask.get().assertDependsOn(subproject.tasks.getByName("commonize"))
        subproject.prepareKotlinIdeaImportTask.get().assertDependsOn(subproject.tasks.getByName("copyCommonizeCInteropForIde"))
    }

    @Test
    fun `test declaring dependsOnIdeaImport`() {
        val project = ProjectBuilder.builder().build()

        assertNull(
            project.tasks.findByName(prepareKotlinIdeaImportTaskName),
            "Expected task $prepareKotlinIdeaImportTaskName to not be registered when no task declares a dependency"
        )

        val testTaskA = project.tasks.register("testTaskA")
        project.ideaImportDependsOn(testTaskA)

        val prepareKotlinIdeaImportTask = assertNotNull(
            project.tasks.findByName(prepareKotlinIdeaImportTaskName),
            "Expected task $prepareKotlinIdeaImportTaskName to be registered after $testTaskA declared dependency"
        )

        prepareKotlinIdeaImportTask.assertTaskDependenciesEquals(setOf(testTaskA.get()))

        val testTaskB = project.tasks.register("testTaskB")
        project.ideaImportDependsOn(testTaskB)

        prepareKotlinIdeaImportTask.assertTaskDependenciesEquals(setOf(testTaskA.get(), testTaskB.get()))
    }
}
