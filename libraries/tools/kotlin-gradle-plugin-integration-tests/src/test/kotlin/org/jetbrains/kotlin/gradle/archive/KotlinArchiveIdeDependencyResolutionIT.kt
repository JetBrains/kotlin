/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.archive

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.sourcesClasspath
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.IdeaKotlinDependencyMatcher
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.anyDependsOnDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.withResolvedSourcesFile
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.jetbrainsAnnotationDependencies
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.kotlinNativeDistributionDependencies
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.kotlinStdlibDependencies
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.PublishedProject
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.jetbrains.kotlin.gradle.util.IdeaKotlinDependenciesContainer
import org.jetbrains.kotlin.gradle.util.resolveIdeDependencies
import org.junit.jupiter.api.DisplayName
import kotlin.test.fail

@MppGradlePluginTests
@DisplayName("IDE dependency resolution of a project published in the Kotlin Archive format")
class KotlinArchiveIdeDependencyResolutionIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions
            .disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    fun ideDependencyResolutionTest(gradleVersion: GradleVersion) {
        val publishedProject = kotlinArchiveProducer(gradleVersion).publish()
        val consumer = kotlinArchiveConsumer(gradleVersion, publishedProject)

        consumer.resolveIdeDependencies { dependencies ->
            dependencies.assertResolvedDependenciesOnly()
            dependencies.assertNoKotlinArchiveOnClasspath()

            expectedDependencies(publishedProject).forEach { [sourceSetName, expected] ->
                dependencies[sourceSetName].assertMatches(expected)
            }
        }
    }

    private fun expectedDependencies(publishedProject: PublishedProject): Map<String, List<Any>> = with(publishedProject) {
        mapOf(
            "commonMain" to listOf(
                kotlinStdlibDependencies,
                metadataDependency("commonMain"),
            ),
            "nativeMain" to listOf(
                kotlinNativeDistributionDependencies,
                metadataDependency("commonMain"),
                metadataDependency("nativeMain"),
                anyDependsOnDependency(),
            ),
            "appleMain" to listOf(
                kotlinNativeDistributionDependencies,
                metadataDependency("commonMain"),
                metadataDependency("nativeMain"),
                metadataDependency("appleMain"),
                anyDependsOnDependency(),
            ),
            "linuxMain" to listOf(
                kotlinNativeDistributionDependencies,
                metadataDependency("commonMain"),
                metadataDependency("nativeMain"),
                metadataDependency("linuxMain"),
                anyDependsOnDependency(),
            ),
            "webMain" to listOf(
                kotlinStdlibDependencies,
                metadataDependency("commonMain"),
                metadataDependency("webMain"),
                anyDependsOnDependency(),
            ),
            "linuxX64Main" to listOf(
                kotlinNativeDistributionDependencies,
                platformDependency("linuxx64"),
                anyDependsOnDependency(),
            ),
            "linuxArm64Main" to listOf(
                kotlinNativeDistributionDependencies,
                platformDependency("linuxarm64"),
                anyDependsOnDependency(),
            ),
            "iosArm64Main" to listOf(
                kotlinNativeDistributionDependencies,
                platformDependency("iosarm64"),
                anyDependsOnDependency(),
            ),
            "macosArm64Main" to listOf(
                kotlinNativeDistributionDependencies,
                platformDependency("macosarm64"),
                anyDependsOnDependency(),
            ),
            "jsMain" to listOf(
                kotlinStdlibDependencies,
                binaryCoordinates(Regex(".*kotlin-dom-api-compat.*")),
                platformDependency("js"),
                anyDependsOnDependency(),
            ),
            "wasmJsMain" to listOf(
                kotlinStdlibDependencies,
                platformDependency("wasmjs"),
                anyDependsOnDependency(),
            ),
            "jvmMain" to listOf(
                kotlinStdlibDependencies,
                jetbrainsAnnotationDependencies,
                binaryCoordinates("$group:$name-jvm:$version")
                    .withResolvedSourcesFile("$name-jvm-$version-sources.jar"),
                anyDependsOnDependency(),
            ),
        )
    }

    /**
     * Sources for kotlin archive contained target are not supported yet. Would be supported in following PR.
     */
    private fun PublishedProject.platformDependency(legacyTargetName: String): IdeaKotlinDependencyMatcher =
        binaryCoordinates("$group:$name-$legacyTargetName:$version")
            .withoutResolvedSources()

    private fun IdeaKotlinDependencyMatcher.withoutResolvedSources(): IdeaKotlinDependencyMatcher =
        IdeaKotlinDependencyMatcher("$description without resolved sources") { dependency ->
            matches(dependency) && dependency is IdeaKotlinBinaryDependency && dependency.sourcesClasspath.isEmpty()
        }

    private fun PublishedProject.metadataDependency(producerSourceSetName: String): IdeaKotlinDependencyMatcher =
        binaryCoordinates("$group:$name:$producerSourceSetName:$version")
            .withResolvedSourcesFile("${name}-${version}-sources.jar")

    private fun IdeaKotlinDependenciesContainer.assertNoKotlinArchiveOnClasspath() {
        val archiveFiles = values.flatten()
            .filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
            .flatMap { dependency -> dependency.classpath }
            .filter { file -> file.name.endsWith(".kar") || file.name.endsWith(".kar.xz") }
        if (archiveFiles.isNotEmpty()) fail("Kotlin Archive files on the IDE classpath: $archiveFiles")
    }

    private fun kotlinArchiveConsumer(
        gradleVersion: GradleVersion,
        publishedProject: PublishedProject,
    ): TestProject = project("empty", gradleVersion) {
        plugins { kotlin("multiplatform") }
        addPublishedProjectToRepositories(publishedProject)
        settingsBuildScriptInjection {
            settings.rootProject.name = "consumer"
        }
        buildScriptInjection {
            project.applyMultiplatform {
                kotlinArchiveTargets()

                sourceSets.commonMain.dependencies {
                    implementation(publishedProject.rootCoordinate)
                }
            }
        }
    }
}
