/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import kotlin.test.assertTrue
import java.nio.file.*
import java.nio.file.Files.copy
import java.nio.file.Files.createDirectories

import java.nio.file.attribute.BasicFileAttributes

/**
 * Create new test project.
 *
 * @param [projectName] test project name in 'src/test/resources/testProject` directory.
 * @param [buildOptions] common Gradle build options
 */
fun KGPBaseTest.project(
    projectName: String,
    buildOptions: KGPBaseTest.BuildOptions = defaultBuildOptions,
    test: TestProject.() -> Unit
): TestProject {
    val projectPath = setupProjectFromTestResources(projectName, KGPBaseTest.workingDir)
    projectPath.addDefaultBuildFiles()

    val gradleRunner = GradleRunner
        .create()
        .withProjectDir(projectPath.toFile())
        .withTestKitDir(KGPBaseTest.workingDir.testKitDir.toFile())

    val testProject = TestProject(
        gradleRunner,
        projectName,
        buildOptions,
        projectPath
    )
    testProject.test()
    return testProject
}

/**
 * Trigger test project build with given [buildArguments] and assert build is successful.
 */
fun TestProject.build(
    vararg buildArguments: String,
    assertions: BuildResult.() -> Unit = {}
) {
    val buildResult = gradleRunner
        .withArguments(commonBuildSetup(buildArguments.toList()))
        .build()

    assertions(buildResult)
}

/**
 * Trigger test project build with given [buildArguments] and assert build is failed.
 */
fun TestProject.buildAndFail(
    vararg buildArguments: String,
    assertions: BuildResult.() -> Unit = {}
) {
    val buildResult = gradleRunner
        .withArguments(commonBuildSetup(buildArguments.toList()))
        .buildAndFail()

    assertions(buildResult)
}

class TestProject(
    val gradleRunner: GradleRunner,
    val projectName: String,
    val buildOptions: KGPBaseTest.BuildOptions,
    val projectPath: Path
)

private fun TestProject.commonBuildSetup(
    buildArguments: List<String>
): List<String> {
    val buildOptionsArguments = buildOptions.toArguments()
    val allBuildArguments = buildOptionsArguments + buildArguments + "--full-stacktrace"

    println("<=== Test build: ${this.projectName} ===>")
    println("<=== Run arguments: ${allBuildArguments.joinToString()} ===>")

    return allBuildArguments
}

private val Path.testKitDir get() = resolve("testKitDir")

private fun setupProjectFromTestResources(
    projectName: String,
    tempDir: Path
): Path {
    val testProjectPath = Paths.get("src", "test", "resources", "testProject", projectName)
    assertTrue("Test project exists") { Files.exists(testProjectPath) }
    assertTrue("Test project path is a directory") { Files.isDirectory(testProjectPath) }

    return tempDir
        .resolve(projectName)
        .also {
            testProjectPath.copyRecursively(it)
        }
}

private fun Path.addDefaultBuildFiles() {
    if (Files.exists(resolve("build.gradle"))) {
        val settingsFile = resolve("settings.gradle")
        if (!Files.exists(settingsFile)) {
            settingsFile.toFile().writeText(DEFAULT_GROOVY_SETTINGS_FILE)
        } else {
            val settingsContent = settingsFile.toFile().readText()
            if (!settingsContent
                    .lines()
                    .first { !it.startsWith("//") }
                    .startsWith("pluginManagement {")
            ) {
                settingsFile.toFile().writeText(
                    """
                    $DEFAULT_GROOVY_SETTINGS_FILE
                    
                    $settingsContent
                    """.trimIndent()
                )
            }
        }
    }
}

private fun Path.copyRecursively(dest: Path) {
    Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(
            dir: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult {
            createDirectories(dest.resolve(relativize(dir)))
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult {
            copy(file, dest.resolve(relativize(file)))
            return FileVisitResult.CONTINUE
        }
    })
}
