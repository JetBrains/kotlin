/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.tools.projectWizard.core.div
import org.jetbrains.kotlin.tools.projectWizard.core.service.Services
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.wizard.Wizard
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

abstract class AbstractBuildFileGenerationTest : UsefulTestCase() {
    abstract fun createWizard(directory: Path, buildSystem: BuildSystem, projectDirectory: Path): Wizard

    fun doTest(directoryPath: String) {
        val directory = Paths.get(directoryPath)
        val expectedDirectory = expectedDirectory(directory)

        for (buildSystem in BuildSystem.values()) {
            if (Files.exists(expectedDirectory / buildSystem.buildFileName)) {
                doTest(directory, buildSystem)
            }
        }
    }

    private fun doTest(directory: Path, buildSystem: BuildSystem) {
        val tempDirectory = Files.createTempDirectory(null)
        val wizard = createWizard(directory, buildSystem, tempDirectory)
        val result = wizard.apply(Services.IDEA_INDEPENDENT_SERVICES, GenerationPhase.ALL)
        result.assertSuccess()

        val expectedDirectory = expectedDirectory(directory)

        compareFiles(
            expectedDirectory.allBuildFiles(buildSystem), expectedDirectory,
            tempDirectory.allBuildFiles(buildSystem), tempDirectory
        )
    }

    private fun Path.allBuildFiles(buildSystem: BuildSystem) =
        listFiles { it.fileName.toString() in buildSystem.allBuildFileNames }

    private fun expectedDirectory(directory: Path): Path =
        (directory / EXPECTED_DIRECTORY_NAME).takeIf { Files.exists(it) } ?: directory

    companion object {
        private const val EXPECTED_DIRECTORY_NAME = "expected"
    }
}
