/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.plugin.test

import java.nio.file.Path
import kotlin.io.path.Path

class MavenTestExecutionContext(
    val javaHomeProvider: (version: String) -> Path,
    val mavenDistributionProvider: (version: String) -> MavenDistribution,
    val testProjectsDir: Path,
    val testWorkDir: Path,
    val sharedMavenLocal: Path,
    val kotlinVersion: String,
    val kotlinBuildRepo: Path,
    val verifyCommonBshLocation: Path,
)

fun createMavenTestExecutionContextFromEnvironment(
    tmpDir: Path,
): MavenTestExecutionContext {
    // FIXME: KT-83111 Add JavaVersion argument resolver for kotlin-maven-plugin-test
    val javaHomeProvider = { _: String ->
        // prefer JDK 17, fallback to java home
        val jdk17 = System.getProperty("jdk17Home")
        val javaHome = jdk17 ?: System.getProperty("java.home")
        Path(javaHome)
    }

    // FIXME: KT-83112 Add MavenVersion argument resolver for kotlin-maven-plugin-test
    val mavenDistributionProvider = { _: String ->
        val mavenHome = System.getProperty("maven.home") ?: error("maven.home system property is not set")
        MavenDistribution(Path(mavenHome))
    }

    val testProjectsDir = System.getProperty("kotlin.it.testDirs")
        ?: error("kotlin.it.testDirs system property is not set, set it to location with test projects")

    val mavenRepoLocal = System.getProperty("kotlin.maven.local.repo.for.tests") ?: error("kotlin.maven.local.repo.for.tests system property is not set")

    val kotlinVersion = System.getProperty("kotlin.version") ?: error("kotlin.version system property is not set")
    val kotlinBuildRepo = System.getProperty("kotlin.build.repo")
        ?: error("""
            "kotlin.build.repo system property is not set.
             It should point to maven repository created after ./gradlew publish in kotlin.git"
             By default it is '{kotlinProjectDir}/build/repo'
        """.trimIndent())

    val verifyCommonBshLocation = System.getProperty("kotlin.it.verify-common.bsh")
        ?: error("kotlin.it.verify-common.bsh system property is not set")

    return MavenTestExecutionContext(
        javaHomeProvider = javaHomeProvider,
        mavenDistributionProvider = mavenDistributionProvider,
        testProjectsDir = Path(testProjectsDir),
        testWorkDir = tmpDir.resolve("maven-test-work"),
        sharedMavenLocal = Path(mavenRepoLocal),
        kotlinVersion = kotlinVersion,
        kotlinBuildRepo = Path(kotlinBuildRepo),
        verifyCommonBshLocation = Path(verifyCommonBshLocation),
    )
}
