/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import org.jetbrains.kotlin.maven.test.jdk.CompositeJdkProvider
import org.jetbrains.kotlin.maven.test.jdk.JavaHomeFallbackJdkProvider
import org.jetbrains.kotlin.maven.test.jdk.JdkProvider
import org.jetbrains.kotlin.maven.test.jdk.SystemPropertiesJdkProvider
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class MavenTestExecutionContext(
    val jdkProvider: JdkProvider,
    val testProjectsDir: Path,
    val testWorkDir: Path,
    val sharedMavenLocal: Path,
    val mavenUserHome: Path,
    val mavenTempDir: Path,
    val kotlinVersion: String,
    val kotlinBuildRepo: Path,
) {
    fun getJavaHomeString(version: TestVersions.Java) = jdkProvider
        .getJavaHome(version)
        ?.absolutePathString()
        ?: error("Can't find JDK $version")

    /** Shortcut to create a [MavenToolchainInfo] for a given JDK version. */
    fun toolchain(version: TestVersions.Java): MavenToolchainInfo {
        val versionString = when (version) {
            TestVersions.Java.JDK_1_8 -> "1.8"
            else -> version.numericVersion.toString()
        }
        return MavenToolchainInfo(versionString, getJavaHomeString(version))
    }
}

private fun systemPropertyOrDefault(
    name: String,
    defaultValue: () -> String
) = System.getProperty(name) ?: defaultValue()

fun createMavenTestExecutionContext(
    tmpDir: Path
): MavenTestExecutionContext {
    val jdkProvider =
        // use CompositeJdkProvider only for Local, non-TeamCity runs for better DX with tests.
        // on TC it should be strict.
        if (isTeamCityRun) SystemPropertiesJdkProvider
        else JavaHomeFallbackJdkProvider(mainProvider = CompositeJdkProvider)

    val testProjectsDir = systemPropertyOrDefault(
        "kotlin.it.testDirs",
        defaultValue = { "${System.getProperty("user.dir")}/src/it/" }
    )

    val mavenRepoLocal = systemPropertyOrDefault(
        "kotlin.it.localRepo",
        defaultValue = { "${System.getProperty("user.dir")}/local-repo" }
    )

    val mavenUserHome = systemPropertyOrDefault(
        "kotlin.it.mavenUserHome",
        defaultValue = { "${System.getProperty("user.home")}/.m2" }
    )

    val mavenTempDir = systemPropertyOrDefault(
        "kotlin.it.mavenTempDir",
        defaultValue = { "${System.getProperty("user.dir")}/maven-temp" }
    )

    val kotlinVersion = systemPropertyOrDefault("kotlin.version",
        defaultValue = { "${KotlinVersion.CURRENT.major}.${KotlinVersion.CURRENT.minor}.255-SNAPSHOT" }
    )

    val kotlinBuildRepo = systemPropertyOrDefault(
        "kotlin.build.repo",
        defaultValue = { "${System.getProperty("user.dir")}/../../../build/repo" }
    )

    return MavenTestExecutionContext(
        jdkProvider = jdkProvider,
        testProjectsDir = Path(testProjectsDir),
        testWorkDir = tmpDir.resolve("maven-test-work"),
        sharedMavenLocal = Path(mavenRepoLocal),
        mavenUserHome = Path(mavenUserHome),
        mavenTempDir = Path(mavenTempDir),
        kotlinVersion = kotlinVersion,
        kotlinBuildRepo = Path(kotlinBuildRepo),
    )
}
