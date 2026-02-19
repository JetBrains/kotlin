/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

fun configureMavenWrapperInProjectDirectory(
    projectDirectory: Path,
    mavenVersion: String
) {
    val classLoader = object {}.javaClass.classLoader

    // Copy mvnw
    val mvnwPath = projectDirectory.resolve("mvnw")
    classLoader.getResourceAsStream("maven-wrapper/mvnw")?.use { input ->
        Files.copy(input, mvnwPath)
    } ?: error("Resource maven-wrapper/mvnw not found")

    // Set execute permissions for mvnw
    try {
        val permissions = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_EXECUTE
        )
        Files.setPosixFilePermissions(mvnwPath, permissions)
    } catch (e: UnsupportedOperationException) {
        // POSIX permissions not supported on this file system (e.g., Windows)
    }

    // Copy mvnw.cmd
    val mvnwCmdPath = projectDirectory.resolve("mvnw.cmd")
    classLoader.getResourceAsStream("maven-wrapper/mvnw.cmd")?.use { input ->
        Files.copy(input, mvnwCmdPath)
    } ?: error("Resource maven-wrapper/mvnw.cmd not found")

    // Create .mvn/wrapper directory
    val wrapperDir = projectDirectory.resolve(".mvn/wrapper")
    wrapperDir.createDirectories()

    // Copy maven-wrapper.jar
    classLoader.getResourceAsStream("maven-wrapper/maven-wrapper.jar")?.use { input ->
        wrapperDir.resolve("maven-wrapper.jar").outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: error("Resource maven-wrapper/maven-wrapper.jar not found")

    // Create maven-wrapper.properties
    val propertiesContent = """
        distributionType=bin
        distributionUrl=https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip
    """.trimIndent()

    wrapperDir.resolve("maven-wrapper.properties").writeText(propertiesContent)
}