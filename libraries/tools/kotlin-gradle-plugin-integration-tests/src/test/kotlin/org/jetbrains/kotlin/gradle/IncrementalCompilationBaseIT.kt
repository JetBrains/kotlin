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

    protected fun doTest(
        fileToModify: String,
        modifyFileContents: (String) -> String,
        expectedAffectedFileNames: Collection<String>,
    ) {
        doTest(
            { it.projectDir.getFileByName(fileToModify).modify(modifyFileContents) },
            expectedAffectedFileNames
        )
    }

    protected fun doTest(
        modifyProject: (Project) -> Unit,
        expectedAffectedFileNames: Collection<String>,
    ) {
        doTest(defaultBuildOptions(), modifyProject, expectedAffectedFileNames)
    }

    protected fun doTest(
        options: BuildOptions,
        modifyProject: (Project) -> Unit,
        expectedAffectedFileNames: Collection<String>,
    ) {
        val project = defaultProject()
        project.build("build") {
            assertSuccessful()
        }

        modifyProject(project)

        project.build("build", options = options) {
            assertSuccessful()
            val expectedAffectedFiles = project.projectDir.getFilesByNames(*expectedAffectedFileNames.toTypedArray())
            val expectedAffectedFileRelativePaths = project.relativize(expectedAffectedFiles)
            assertCompiledKotlinSources(expectedAffectedFileRelativePaths)
        }
    }
}
