/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import org.jetbrains.kotlin.tools.projectWizard.core.ExceptionError
import org.jetbrains.kotlin.tools.projectWizard.core.div
import org.jetbrains.kotlin.tools.projectWizard.core.onFailure
import org.jetbrains.kotlin.tools.projectWizard.core.service.Services
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.MavenPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.GroovyDslPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.KotlinDslPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.YamlWizard
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

abstract class AbstractBuildFileGenerationTest : AbstractPluginBasedTest() {
    fun doTest(directoryPath: String) {
        val directory = Paths.get(directoryPath)
        val testData = init(directory)
        if (KotlinDslPlugin::class in testData.pluginClasses) {
            doTest(directory, testData, BuildSystem.GRADLE_KOTLIN_DSL)
        }
        if (GroovyDslPlugin::class in testData.pluginClasses) {
            doTest(directory, testData, BuildSystem.GRADLE_GROOVY_DSL)
        }
        if (MavenPlugin::class in testData.pluginClasses) {
            doTest(directory, testData, BuildSystem.MAVEN)
        }
    }

    private fun doTest(directory: Path, testData: WizardTestData, buildSystem: BuildSystem) {
        val yaml = directory.resolve("settings.yaml").toFile().readText() + "\n" +
                defaultStructure + "\n" +
                buildSystem.yaml
        val tempDir = Files.createTempDirectory(null)
        val wizard = YamlWizard(yaml, tempDir.toString(), testData.createPlugins)
        val result = wizard.apply(Services.IDEA_INDEPENDENT_SERVICES, GenerationPhase.ALL)
        result.onFailure { errors ->
            errors.forEach { error ->
                if (error is ExceptionError) {
                    throw error.exception
                }
            }
            fail(errors.joinToString("\n"))
        }

        val expectedDirectory = (directory / EXPECTED_DIRECTORY_NAME).takeIf { Files.exists(it) } ?: directory

        compareFiles(
            expectedDirectory.allBuildFiles(buildSystem), expectedDirectory,
            tempDir.allBuildFiles(buildSystem), tempDir
        )
    }

    private fun Path.allBuildFiles(buildSystem: BuildSystem) =
        listFiles { it.fileName.toString() == buildSystem.buildFileName }

    private enum class BuildSystem(val buildFileName: String, val yaml: String) {
        GRADLE_KOTLIN_DSL(
            buildFileName = "build.gradle.kts",
            yaml = """buildSystem:
                            type: GradleKotlinDsl
                            gradle:
                              createGradleWrapper: false
                              version: 5.4.1""".trimIndent()
        ),
        GRADLE_GROOVY_DSL(
            buildFileName = "build.gradle",
            yaml = """buildSystem:
                            type: GradleGroovyDsl
                            gradle:
                              createGradleWrapper: false
                              version: 5.4.1""".trimIndent()
        ),
        MAVEN(
            buildFileName = "pom.xml",
            yaml = """buildSystem:
                            type: Maven""".trimIndent()
        )
    }

    companion object {
        private const val EXPECTED_DIRECTORY_NAME = "expected"

        private val defaultStructure =
            """structure:
              name: generatedProject
              groupId: testGroupId
              artifactId: testArtifactId
            """.trimIndent()
    }
}
