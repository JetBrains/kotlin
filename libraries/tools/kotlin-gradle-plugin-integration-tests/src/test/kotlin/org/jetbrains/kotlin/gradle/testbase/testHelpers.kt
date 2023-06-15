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
            |#!/usr/bin/env sh
            |${formatEnvironmentForScript(envCommand = "export")}
            |./gradlew ${buildOptions.toArguments(gradleVersion).joinToString(separator = " ")} ${'$'}@ 
            |""".trimMargin()
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
            |@rem Executing Gradle build
            |${formatEnvironmentForScript(envCommand = "set")}
            |gradlew.bat ${buildOptions.toArguments(gradleVersion).joinToString(separator = " ")} %* 
            |""".trimMargin()
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

private fun TestProject.formatEnvironmentForScript(envCommand: String): String {
    return environmentVariables.environmentalVariables.asSequence().joinToString(separator = "\n|") { (key, value) ->
        "$envCommand $key=\"$value\""
    }
}

/**
 *
 * Configures the JVM memory settings for the Gradle project.
 * @param memorySizeInGb The amount of memory to allocate to the JVM, in gigabytes.
 *                     Defaults to 1 gigabyte.
 */
fun GradleProject.configureJvmMemory(memorySizeInGb: Number = 1) {
    addPropertyToGradleProperties(
        propertyName = "org.gradle.jvmargs",
        mapOf("-Xmx" to "-Xmx${memorySizeInGb}g")
    )
}

/**
 * Adds the given options to a Gradle property specified by name, in the project's Gradle properties file.
 * If the property does not exist, it is created.
 * @param propertyName The name of the Gradle property to modify or create.
 * @param propertyValues Map with key = "option prefix", and value = "option value".
 *                       For example, for options: -Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
 *                       Map would be look like: Map.of("-Xmx", "-Xmx2g",
 *                                                      "-XX:MaxMetaspaceSize","-XX:MaxMetaspaceSize=512m",
 *                                                      "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+HeapDumpOnOutOfMemoryError" )
 */
fun GradleProject.addPropertyToGradleProperties(
    propertyName: String,
    propertyValues: Map<String, String>
) {
    if (!gradleProperties.exists()) gradleProperties.createFile()

    val propertiesContent = gradleProperties.readText()
    val (existingPropertyLine, otherLines) = propertiesContent
        .lines()
        .partition {
            it.trim().startsWith(propertyName)
        }

    if (existingPropertyLine.isEmpty()) {
        gradleProperties.writeText(
            """
            |${propertyName}=${propertyValues.values.joinToString(" ")}
            | 
            |$propertiesContent
            """.trimMargin()
        )
    } else {
        val argsLine = existingPropertyLine.single()
        val optionsToRewrite = mutableListOf<String>()
        val appendedOptions = buildString {
            propertyValues.forEach {
                if (argsLine.contains(it.key) &&
                    !argsLine.contains(it.value)
                ) optionsToRewrite.add(it.value)
                else
                    if (!argsLine.contains(it.key)) append(" ${it.value}")
            }
        }

        assert(optionsToRewrite.isEmpty()) {
            """
            |You are trying to write options: $optionsToRewrite 
            |for property: $propertyName 
            |in $gradleProperties
            |But these options are already exists with another values.
            |Current property value is: $argsLine
            """.trimMargin()
        }

        gradleProperties.writeText(
            """
            |$argsLine$appendedOptions
            |
            |${otherLines.joinToString(separator = "\n")}
            """.trimMargin()
        )

    }
}