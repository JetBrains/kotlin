/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.extension
import kotlin.streams.asSequence

val kotlinVersion: String = System.getProperty("kotlin.version")
val mavenLocal: String = System.getProperty("maven.repo.local")
val localRepoPath: Path = Paths.get(mavenLocal, "org/jetbrains/kotlin")
val expectedRepoPath: Path = Paths.get("repo/artifacts-tests/src/test/resources/org/jetbrains/kotlin")

/**
 * Kotlin native bundles are present in TC artifacts but should not be checked until kotlin native enabled project-wide
 */
val nativeBundles = setOf(
    "kotlin-native",
    "kotlin-native-compiler-embeddable",
    "kotlin-native-prebuilt",
)

val excludedProjects = setOf(
    "android-test-fixes",
    "annotation-processor-example",
    "gradle-warnings-detector",
    "kotlin-compiler-args-properties",
    "kotlin-gradle-plugin-tcs-android",
    "kotlin-gradle-subplugin-example",
    "kotlin-java-example",
    "kotlin-maven-plugin-test",
    "org.jetbrains.kotlin.gradle-subplugin-example.gradle.plugin",
    "org.jetbrains.kotlin.test.fixes.android.gradle.plugin",
    "org.jetbrains.kotlin.test.gradle-warnings-detector.gradle.plugin",
    "org.jetbrains.kotlin.test.kotlin-compiler-args-properties.gradle.plugin",
)

/**
 * convert:
 * ${mavenLocal}/org/jetbrains/kotlin/artifact/version/artifact-version.ext
 * to:
 * ${expectedRepository}/org/jetbrains/kotlin/artifact/artifact.ext
 */
fun Path.toExpectedPath(): Path {
    val fileExtension = this.fileName.extension
    val artifactDirPath = localRepoPath.relativize(this).parent.parent
    val expectedFileName = "${artifactDirPath.fileName}.$fileExtension"
    return expectedRepoPath.resolve(artifactDirPath.resolve(expectedFileName))
}

fun findActualArtifacts(extension: String, mustContainKotlinVersion: Boolean = false) = Files.find(
    localRepoPath,
    Integer.MAX_VALUE,
    { path: Path, fileAttributes: BasicFileAttributes ->
        fileAttributes.isRegularFile
                && "${path.fileName}".endsWith(extension, ignoreCase = true)
                && if (mustContainKotlinVersion) path.contains(Paths.get(kotlinVersion)) else true
    }).asSequence()

fun findExpectedArtifacts(extension: String) = Files.find(
    expectedRepoPath,
    Integer.MAX_VALUE,
    { path: Path, fileAttributes: BasicFileAttributes ->
        fileAttributes.isRegularFile
                && "${path.fileName}".endsWith(extension, ignoreCase = true)
    }).asSequence()

