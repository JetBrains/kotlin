/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.jetbrains.kotlin.gradle.util.modify

abstract class IncrementalCompilationBaseIT : BaseGradleIT() {

    protected abstract fun defaultProject(): Project

    override fun defaultBuildOptions() = super.defaultBuildOptions().copy(incremental = true)

    protected fun doTest(
        fileToModify: String,
        modifyFileContents: (originalContents: String) -> String,
        expectedCompiledFileNames: Collection<String>,
    ) {
        doTest(
            modifyProject = { projectDir.getFileByName(fileToModify).modify(modifyFileContents) },
            expectedCompiledFileNames = expectedCompiledFileNames
        )
    }

    protected fun doTest(
        fileToModify: String,
        modifyFileContents: (originalContents: String) -> String,
        assertResults: CompiledProject.() -> Unit,
    ) {
        doTest(
            modifyProject = { projectDir.getFileByName(fileToModify).modify(modifyFileContents) },
            assertResults = { assertResults() }
        )
    }

    protected fun doTest(
        project: Project = defaultProject(),
        task: String = "build",
        options: BuildOptions = defaultBuildOptions(),
        modifyProject: Project.() -> Unit,
        expectedCompiledFileNames: Collection<String>,
    ) {
        doTest(
            project, task, options, modifyProject,
            assertResults = {
                assertCompiledKotlinFiles(project.projectDir.getFilesByNames(*expectedCompiledFileNames.toTypedArray()))
            }
        )
    }

    protected fun doTest(
        project: Project = defaultProject(),
        task: String = "build",
        options: BuildOptions = defaultBuildOptions(),
        modifyProject: Project.() -> Unit,
        assertResults: CompiledProject.() -> Unit
    ) {
        project.build(task, options = options) {
            assertSuccessful()
        }

        modifyProject(project)

        project.build(task, options = options) {
            assertSuccessful()
            assertResults()
        }
    }
}
