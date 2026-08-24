/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheValue
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.IsolatedProjectsMode
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.project
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.test.assertTrue

internal const val KOTLIN_2_4_0 = "2.4.0"
internal const val ENABLE_KAR_PUBLICATION = "-Pkotlin.publicationFormat=KOTLIN_ARCHIVE"

// TODO: Change this to 2.4.20 when Kotlin 2.4.20 is released.
internal const val KOTLIN_2_4_20_BETA1 = "2.4.20-Beta1"

private val publicationLock = Any()

internal fun KGPBaseTest.publishKarOnce(
    gradleVersion: GradleVersion,
    publicationRoot: Path,
): Path {
    val repository = publicationRoot.resolve(gradleVersion.version)
    val kar = repository.resolve("org/jetbrains/kotlin/kar/test/sample/1.0/sample-1.0.kar.xz")
    val jvmPublication = repository.resolve("org/jetbrains/kotlin/kar/test/sample-jvm/1.0")
    val jvmPom = jvmPublication.resolve("sample-jvm-1.0.pom")

    synchronized(publicationLock) {
        if (!kar.exists() || !jvmPom.exists()) {
            project(
                projectName = "karPublication/producer",
                gradleVersion = gradleVersion,
                localRepoDir = repository,
                buildOptions = defaultBuildOptions.copy(
                    configurationCache = ConfigurationCacheValue.DISABLED,
                    isolatedProjects = IsolatedProjectsMode.DISABLED,
                    freeArgs = defaultBuildOptions.freeArgs + ENABLE_KAR_PUBLICATION,
                ),
            ) {
                build("publishAllPublicationsToMavenRepository") {
                    assertTasksExecuted(":packKotlinArchive")
                }
            }
        }
    }

    assertTrue(kar.exists(), "KAR was not published at $kar")
    assertTrue(
        jvmPublication.listDirectoryEntries("*.jar").any { !it.fileName.toString().endsWith("-sources.jar") },
        "JVM artifact was not published under $jvmPublication",
    )
    return repository
}
