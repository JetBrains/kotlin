/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import com.intellij.testFramework.UsefulTestCase
import org.hamcrest.core.Is
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.tools.projectWizard.core.ExceptionError
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.div
import org.jetbrains.kotlin.tools.projectWizard.core.onFailure
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

internal fun Path.readFile() = toFile().readText().trim()

internal fun Path.listFiles(filter: (Path) -> Boolean) =
    Files.walk(this).filter { path ->
        Files.isRegularFile(path) && filter(path)
    }.collect(Collectors.toList()).sorted()

internal fun compareFilesAndGenerateMissing(
    expectedFiles: List<Path>, expectedDir: Path,
    actualFiles: List<Path>, actualDir: Path,
    readActualFile: (Path) -> String
) {
    val expectedFilesSorted = expectedFiles.sorted()
    val actualFilesSorted = actualFiles.sorted()

    val missingFiles =
        actualFiles.map(actualDir::relativize).toSet() - expectedFiles.map(expectedDir::relativize).toSet()

    if (missingFiles.isNotEmpty()) {
        missingFiles.forEach { missingFile ->
            val targetFile = expectedDir / missingFile
            Files.createDirectories(targetFile.parent)
            Files.copy(actualDir / missingFile, targetFile)
        }
        fail("The following files was missed: $missingFiles and thus was generated")
    }

    for ((actualFile, expectedFile) in actualFilesSorted zip expectedFilesSorted) {
        KotlinTestUtils.assertEqualsToFile(expectedFile.toFile(), readActualFile(actualFile))
    }
}

fun readSettingsYaml(directory: Path, buildSystem: BuildSystem): String? =
    readSettingsYamlWithoutDefaultStructure(directory)?.let { text ->
        text + "\n" +
                defaultStructure + "\n" +
                buildSystem.yaml
    }

fun readSettingsYamlWithoutDefaultStructure(directory: Path): String? =
    directory.resolve("settings.yaml").takeIf { Files.exists(it) }?.readFile()


enum class BuildSystem(
    val buildFileName: String,
    val additionalFileNames: List<String> = emptyList(),
    val yaml: String
) {
    GRADLE_KOTLIN_DSL(
        buildFileName = "build.gradle.kts",
        additionalFileNames = listOf("settings.gradle.kts"),
        yaml = """buildSystem:
                            type: GradleKotlinDsl
                            """.trimIndent()
    ),
    GRADLE_GROOVY_DSL(
        buildFileName = "build.gradle",
        additionalFileNames = listOf("settings.gradle"),
        yaml = """buildSystem:
                            type: GradleGroovyDsl
                            """.trimIndent()
    ),
    MAVEN(
        buildFileName = "pom.xml",
        yaml = """buildSystem:
                            type: Maven""".trimIndent()
    )
}

val BuildSystem.buildSystemType
    get() = when (this) {
        BuildSystem.GRADLE_KOTLIN_DSL -> BuildSystemType.GradleKotlinDsl
        BuildSystem.GRADLE_GROOVY_DSL -> BuildSystemType.GradleGroovyDsl
        BuildSystem.MAVEN -> BuildSystemType.Maven
    }

val BuildSystem.allBuildFileNames
    get() = additionalFileNames + buildFileName

val BuildSystem.isGradle
    get() = this == BuildSystem.GRADLE_KOTLIN_DSL || this == BuildSystem.GRADLE_GROOVY_DSL

private val defaultStructure =
    """structure:
              name: generatedProject
              groupId: testGroupId
              artifactId: testArtifactId
            """.trimIndent()

fun TaskResult<Any>.assertSuccess() {
    onFailure { errors ->
        errors.forEach { error ->
            if (error is ExceptionError) {
                throw error.exception
            }
        }
        UsefulTestCase.fail(errors.joinToString("\n"))
    }
}