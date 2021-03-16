/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.kotlin.gradle.DEFAULT_GROOVY_SETTINGS_FILE
import kotlin.test.assertTrue
import java.nio.file.*
import java.nio.file.Files.copy
import java.nio.file.Files.createDirectories

import java.nio.file.attribute.BasicFileAttributes

/**
 * Create new test project.
 *
 * @param [projectName] test project name in 'src/test/resources/testProject` directory.
 */
fun KGPBaseTest.project(
    projectName: String,
    test: TestProject.() -> Unit
): TestProject {
    val projectPath = setupProjectFromTestResources(projectName, KGPBaseTest.workingDir)
    projectPath.addDefaultBuildFiles()

    val gradleRunner = GradleRunner
        .create()
        .withProjectDir(projectPath.toFile())
        .withTestKitDir(KGPBaseTest.workingDir.testKitDir.toFile())

    val testProject = TestProject(gradleRunner, projectName)
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
    println("<=== Test build: ${this.projectName} ===>")
    val buildResult = gradleRunner
        .withArguments(*buildArguments, "-Pkotlin_version=1.5.255-SNAPSHOT")
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
    println("<=== Test build: ${this.projectName} ===>")
    val buildResult = gradleRunner
        .withArguments(*buildArguments, "-Pkotlin_version=1.5.255-SNAPSHOT")
        .buildAndFail()
    assertions(buildResult)
}

class TestProject(
    val gradleRunner: GradleRunner,
    val projectName: String
)

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
    if (Files.exists(resolve("build.gradle")) &&
        !Files.exists(resolve("settings.gradle"))
    ) {
        resolve("settings.gradle").toFile().writeText(DEFAULT_GROOVY_SETTINGS_FILE)
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