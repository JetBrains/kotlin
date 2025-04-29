/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.targets.native.internal.retrievePlatformDependencies

/**
 * Legacy -jdk8 and -jdk7 dependencies:
 * Those artifacts will be published as empty jars starting from Kotlin 1.8.0 as
 * the classes will be included in the kotlin-stdlib artifact already.
 *
 * Note: The kotlin-stdlib will add a constraint to always resolve 1.8.0 of those artifacts.
 * This will be necessary in the future, when no more jdk8 or jdk7 artifacts will be published:
 * In this case we need to still resolve to a version that will contain empty artifacts (1.8.0)
 *
 */
fun legacyStdlibJdkDependencies(version: String = "1.8.0") = listOf(
    binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"),
    binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$version"),
)

/**
 * Download and unpack Kotlin Native distribution
 *
 * This is a workaround for the issue where Kotlin Native isn't unpacked into ~/.konan dir when used from local Maven repo: KT-77580
 * as opposed to CDN where it happens in the project configuration phase.
 * It should be run before tests that check dependencies in ~/.konan.
 * The project in this function is created only to trigger downloading and unpacking Kotlin Native
 * and isn't supposed to be used otherwise.
 */
fun provisionKotlinNativeDistribution() {
    val project = buildProjectWithMPP {
        if (project.nativeProperties.actualNativeHomeDirectory.get().exists()) return@buildProjectWithMPP

        // For CI
        repositories.mavenLocal()
        // For local runs
        repositories.maven("https://packages.jetbrains.team/maven/p/kt/bootstrap")
        // Remove after KTI-1994
        listOf(
            "https://download.jetbrains.com/kotlin/native/builds/releases",
            "https://download.jetbrains.com/kotlin/native/builds/dev",
        ).forEach { repoUrl ->
            repositories.ivy {
                it.url = project.uri(repoUrl)
                it.patternLayout {
                    it.artifact("[revision]/[classifier]/[artifact]-[classifier]-[revision].[ext]")
                }
                it.metadataSources {
                    it.artifact()
                }
            }
        }

        kotlin {
            iosArm64()
            iosX64()
            linuxX64()
            linuxArm64()
            mingwX64()
            kotlinPluginLifecycle.launch {
                KotlinPluginLifecycle.Stage.ReadyForExecution.await()
                val downloadDependencies = project.files()
                targets.filter { it.platformType != KotlinPlatformType.common }.forEach {
                    downloadDependencies.from(
                        (it.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME) as AbstractKotlinNativeCompilation)
                            .retrievePlatformDependencies()
                    )
                }
                downloadDependencies.files
            }
        }
    }
    project.evaluate()
}