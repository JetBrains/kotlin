/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.*

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class KotlinMavenTestBase {

    @TempDir
    lateinit var tmpDir: Path

    lateinit var context: MavenTestExecutionContext
    open val buildOptions: MavenBuildOptions = MavenBuildOptions()

    @BeforeEach
    fun setup() {
        context = createMavenTestExecutionContext(tmpDir)
    }

    fun testProject(
        projectDir: String,
        mavenVersion: TestVersions.Maven,
        buildOptions: MavenBuildOptions = this.buildOptions,
        code: (MavenTestProject.() -> Unit)? = null,
    ): MavenTestProject {
        val workDir = copyProjectDir(projectDir, mavenVersion.version)
        configureMavenWrapperInProjectDirectory(workDir, mavenVersion.version)

        context.verifyCommonBshLocation.copyTo(workDir.resolve("verify-common.bsh"))

        val settingsXml = workDir.resolve("settings.xml")
        settingsXml.checkOrWriteKotlinMavenTestSettingsXml(context.kotlinBuildRepo)

        val project = MavenTestProject(
            name = projectDir,
            context = context,
            workDir = workDir,
            settingsFile = settingsXml,
            buildOptions = buildOptions,
            mavenVersion = mavenVersion.version
        )

        if (code != null) code(project)
        return project
    }

    private fun copyProjectDir(projectDir: String, mavenVersion: String): Path {
        val originalProjectDir = context.testProjectsDir.resolve(projectDir)
        if (!originalProjectDir.exists()) error("Project dir $originalProjectDir does not exist")

        val copyTo = context.testWorkDir.resolve(projectDir).resolve(mavenVersion)
        copyTo.createDirectories()

        @OptIn(ExperimentalPathApi::class)
        originalProjectDir.copyToRecursively(
            copyTo,
            overwrite = false, // let it fail in case something is wrong and it tries to write to the same dir
            followLinks = true
        )

        return copyTo
    }

    fun Path.replaceFirstInFile(target: String, replacement: String) {
        val content = toFile().readText()
        val newContent = content.replaceFirst(target, replacement)
        toFile().writeText(newContent)
    }

    fun Path.deleteFile() {
        toFile().delete()
    }
}