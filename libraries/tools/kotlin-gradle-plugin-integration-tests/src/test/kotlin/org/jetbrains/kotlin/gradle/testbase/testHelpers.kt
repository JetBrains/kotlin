/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*

/**
 * Makes a snapshot of the current state of [TestProject] into [destinationPath].
 *
 * Method copies all files into `destinationPath/testProjectName/GradleVersion` directory
 * and setup buildable project.
 *
 * To run task with the same build option as test - use `run.sh` (or `run.bat`) script.
 */
fun TestProject.makeSnapshotTo(destinationPath: String) {
    val dest = Paths
        .get(destinationPath)
        .resolve(projectName)
        .resolve(gradleVersion.version)
        .also {
            if (it.exists()) it.deleteRecursively()
            it.createDirectories()
        }

    projectPath.copyRecursively(dest)
    dest.resolve("gradle.properties").append(
        """
            kotlin_version=${buildOptions.kotlinVersion}
            test_fixes_version=${TestVersions.Kotlin.CURRENT}
            """.trimIndent()
    )

    dest.resolve("run.sh").run {
        writeText(
            """
                #!/usr/bin/env sh
                ./gradlew ${buildOptions.toArguments(gradleVersion).joinToString(separator = " ")} ${'$'}@ 
                """.trimIndent()
        )

        setPosixFilePermissions(
            setOf(
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
            )
        )
    }

    dest.resolve("run.bat").run {
        writeText(
            """
                @rem Executing Gradle build
                gradlew.bat ${buildOptions.toArguments(gradleVersion).joinToString(separator = " ")} %* 
                """.trimIndent()
        )
    }

    val wrapperDir = dest.resolve("gradle").resolve("wrapper").apply { createDirectories() }
    wrapperDir.resolve("gradle-wrapper.properties").writeText(
        """
            distributionUrl=https\://services.gradle.org/distributions/gradle-${gradleVersion.version}-bin.zip
            """.trimIndent()
    )
    // Copied from 'Wrapper' task class implementation
    val projectRoot = Paths.get("../../../")
    projectRoot.resolve("gradle").resolve("wrapper").resolve("gradle-wrapper.jar").run {
        copyTo(wrapperDir.resolve(fileName))
    }
    projectRoot.resolve("gradlew").run {
        copyTo(dest.resolve(fileName))
    }
    projectRoot.resolve("gradlew.bat").run {
        copyTo(dest.resolve(fileName))
    }
}
