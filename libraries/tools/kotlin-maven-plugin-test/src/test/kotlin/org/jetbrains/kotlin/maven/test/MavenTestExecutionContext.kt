/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.plugin.test

import org.jetbrains.kotlin.maven.test.TestVersions
import org.jetbrains.kotlin.maven.test.jdk.CompositeJdkProvider
import org.jetbrains.kotlin.maven.test.jdk.JavaHomeFallbackJdkProvider
import org.jetbrains.kotlin.maven.test.jdk.JdkProvider
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class MavenTestExecutionContext(
    val jdkProvider: JdkProvider,
    val testProjectsDir: Path,
    val testWorkDir: Path,
    val sharedMavenLocal: Path,
    val kotlinVersion: String,
    val kotlinBuildRepo: Path,
    val verifyCommonBshLocation: Path,
) {
    fun getJavaHomeString(version: TestVersions.Java) = jdkProvider
        .getJavaHome(version)
        ?.absolutePathString()
        ?: error("Can't find JDK $version")
}

class EnvironmentConfigProvider {
    /**
     * Replaces all non-word characters (anything except letters, digits, and underscores)
     * with underscores and converts the result to uppercase.
     * 
     * Examples:
     * - "kotlin.version" → "KOTLIN_VERSION"
     */
    private fun String.toEnvironmentKey() = replace("\\W".toRegex(), "_").uppercase()
    
    fun get(
        propertyKey: String,
        environmentKey: String = propertyKey.toEnvironmentKey(),
        defaultValue: String? = null
    ): String? {
        return System.getProperty(propertyKey)
            ?: System.getenv(environmentKey)
            ?: defaultValue
    }
}

fun createMavenTestExecutionContext(
    tmpDir: Path,
    configProvider: EnvironmentConfigProvider = EnvironmentConfigProvider(),
): MavenTestExecutionContext {
    val jdkProvider = JavaHomeFallbackJdkProvider(
        mainProvider = CompositeJdkProvider
    )

    val testProjectsDir = configProvider.get(
        "kotlin.it.testDirs",
        defaultValue = "${System.getProperty("user.dir")}/src/it/"
    ) ?: error("kotlin.it.testDirs system property is not set, set it to location with test projects")

    val mavenRepoLocal = configProvider.get(
        "kotlin.it.localRepo",
        defaultValue = "${System.getProperty("user.dir")}/local-repo"
    ) ?: error("kotlin.it.localRepo system property is not set")

    val kotlinVersion = configProvider.get("kotlin.version",
        defaultValue = KotlinVersion.CURRENT.toString() + "-SNAPSHOT"
    ) ?: error("kotlin.version system property is not set")

    val kotlinBuildRepo = configProvider.get(
        "kotlin.build.repo",
        defaultValue = "${System.getProperty("user.dir")}/../../../build/repo"
    )
        ?: error("""
            "kotlin.build.repo system property is not set.
             It should point to maven repository created after ./gradlew publish in kotlin.git"
             By default it is '{kotlinProjectDir}/build/repo'
        """.trimIndent())

    val verifyCommonBshLocation = configProvider.get(
        "kotlin.it.verify-common.bsh",
        defaultValue = "${System.getProperty("user.dir")}/verify-common.bsh"
    ) ?: error("kotlin.it.verify-common.bsh system property is not set")

    return MavenTestExecutionContext(
        jdkProvider = jdkProvider,
        testProjectsDir = Path(testProjectsDir),
        testWorkDir = tmpDir.resolve("maven-test-work"),
        sharedMavenLocal = Path(mavenRepoLocal),
        kotlinVersion = kotlinVersion,
        kotlinBuildRepo = Path(kotlinBuildRepo),
        verifyCommonBshLocation = Path(verifyCommonBshLocation),
    )
}