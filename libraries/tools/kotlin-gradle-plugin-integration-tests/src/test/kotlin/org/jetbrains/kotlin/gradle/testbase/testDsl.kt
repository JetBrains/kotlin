/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import kotlin.test.assertTrue
import java.io.File
import java.nio.file.*
import java.nio.file.Files.copy
import java.nio.file.Files.createDirectories

import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Create new test project.
 *
 * @param [projectName] test project name in 'src/test/resources/testProject` directory.
 * @param [buildOptions] common Gradle build options
 */
fun KGPBaseTest.project(
    projectName: String,
    gradleVersion: GradleVersion,
    buildOptions: KGPBaseTest.BuildOptions = defaultBuildOptions,
    forceOutput: Boolean = false,
    addHeapDumpOptions: Boolean = true,
    enableGradleDebug: Boolean = false,
    test: TestProject.() -> Unit
): TestProject {
    val projectPath = setupProjectFromTestResources(
        projectName,
        gradleVersion,
        workingDir
    )
    projectPath.addDefaultBuildFiles()
    if (addHeapDumpOptions) projectPath.addHeapDumpOptions()

    val gradleRunner = GradleRunner
        .create()
        .withDebug(enableGradleDebug)
        .withProjectDir(projectPath.toFile())
        .withTestKitDir(testKitDir.toAbsolutePath().toFile())
        .withGradleVersion(gradleVersion.version)
        .also {
            if (forceOutput) it.forwardOutput()
        }

    val testProject = TestProject(
        gradleRunner,
        projectName,
        buildOptions,
        projectPath,
        gradleVersion
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
    val projectPath: Path,
    val gradleVersion: GradleVersion
)

private fun TestProject.commonBuildSetup(
    buildArguments: List<String>
): List<String> {
    val buildOptionsArguments = buildOptions.toArguments(gradleVersion)
    val allBuildArguments = buildOptionsArguments + buildArguments + "--full-stacktrace"

    println("<=== Test build: ${this.projectName} ===>")
    println("<=== Using Gradle version: ${gradleVersion.version} ===>")
    println("<=== Run arguments: ${allBuildArguments.joinToString()} ===>")
    println("<=== Project path:  ${projectPath.toAbsolutePath()} ===>")

    return allBuildArguments
}

/**
 * On changing test kit dir location update related location in 'cleanTestKitCache' task.
 */
private val testKitDir get() = Paths.get(".").resolve(".testKitDir")

private fun setupProjectFromTestResources(
    projectName: String,
    gradleVersion: GradleVersion,
    tempDir: Path
): Path {
    val testProjectPath = Paths.get("src", "test", "resources", "testProject", projectName)
    assertTrue("Test project exists") { Files.exists(testProjectPath) }
    assertTrue("Test project path is a directory") { Files.isDirectory(testProjectPath) }

    return tempDir
        .resolve(projectName)
        .resolve(gradleVersion.version)
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

@OptIn(ExperimentalPathApi::class)
private fun Path.addHeapDumpOptions() {
    val propertiesFile = resolve("gradle.properties")
    if (!propertiesFile.exists()) propertiesFile.createFile()

    val propertiesContent = propertiesFile.readText()
    val (existingJvmArgsLine, otherLines) = propertiesContent
        .lines()
        .partition {
            it.trim().startsWith("org.gradle.jvmargs")
        }

    val heapDumpOutOfErrorStr = "-XX:+HeapDumpOnOutOfMemoryError"
    val heapDumpPathStr = "-XX:HeapDumpPath=\"${System.getProperty("user.dir")}${File.separatorChar}build\""

    if (existingJvmArgsLine.isEmpty()) {
        propertiesFile.writeText(
            """
            # modified in addHeapDumpOptions
            org.gradle.jvmargs=$heapDumpOutOfErrorStr $heapDumpPathStr
             
            $propertiesContent
            """.trimIndent()
        )
    } else {
        val argsLine = existingJvmArgsLine.first()
        val appendedOptions = buildString {
            if (!argsLine.contains("HeapDumpOnOutOfMemoryError")) append(" $heapDumpOutOfErrorStr")
            if (!argsLine.contains("HeapDumpPath")) append(" $heapDumpPathStr")
        }

        if (appendedOptions.isNotEmpty()) {
            propertiesFile.writeText(
                """
                # modified in addHeapDumpOptions
                $argsLine$appendedOptions
                
                ${otherLines.joinToString(separator = "\n")}
                """.trimIndent()
            )
        } else {
            println("<=== Heap dump options are already exists! ===>")
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
