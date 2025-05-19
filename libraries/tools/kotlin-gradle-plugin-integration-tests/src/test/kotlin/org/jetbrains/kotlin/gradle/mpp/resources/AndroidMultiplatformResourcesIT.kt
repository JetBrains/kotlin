/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.resources

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectoriesIgnoringDotFiles
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Path

@MppGradlePluginTests
@DisplayName("Test multiplatform resources consumption and publication in androidTarget()")
class AndroidMultiplatformResourcesIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
        )

    @DisplayName("Multiplatform resources consumption from self, published and project dependencies for Android target")
    @GradleAndroidTest
    fun testConsumption(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        val publishedResourcesProducer = resourcesProducerProject(
            gradleVersion,
            providedJdk,
            androidVersion
        ).publish(
            publisherConfiguration = PublisherConfiguration(),
            deriveBuildOptions = { buildOptions.suppressWarningFromAgpWithGradle813(gradleVersion) }
        )

        val projectDependency = project(
            "multiplatformResources/android/projectDependency",
            gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
        ) {
            configureStandardResourcesProducer(
                MultiplatformResourcesITAndroidConfiguration(
                    androidVersion,
                    "com.android.library",
                )
            )
        }

        val subprojectDependencyName = "subproject"
        project(
            "multiplatformResources/android",
            gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion).suppressWarningFromAgpWithGradle813(gradleVersion),
        ) {
            include(projectDependency, subprojectDependencyName)
            addPublishedProjectToRepositories(publishedResourcesProducer)

            addKgpToBuildScriptCompilationClasspath()
            addAgpToBuildScriptCompilationClasspath(androidVersion)

            configureStandardResourcesProducer(
                MultiplatformResourcesITAndroidConfiguration(
                    androidVersion,
                    "com.android.application",
                ),
                relativeResourcePlacement = {
                    provider {
                        File("embed/self")
                    }
                }
            )
            buildScriptInjection {
                kotlinMultiplatform.sourceSets.getByName("commonMain").dependencies {
                    implementation(project(":${subprojectDependencyName}"))
                    implementation(publishedResourcesProducer.rootCoordinate)
                }
            }

            build(":zipApksForDebug")

            val apkPath = projectPath.resolve("build/outputs/apk/debug/android-debug.apk")
            assertAssetsMatchReference(apkPath)
            assertEmbeddedResourcesMatchReference(apkPath)
        }
    }

    private fun TestProject.assertAssetsMatchReference(
        apkPath: Path,
    ) {
        val assetsPath = projectPath.resolve("assetsInApk")
        unzip(
            apkPath,
            outputDir = assetsPath,
            filesStartingWith = "assets",
        )
        val referenceAssets = projectPath.resolve("reference/assets").toFile()
        assertEqualDirectoriesIgnoringDotFiles(
            expected = referenceAssets,
            actual = assetsPath.toFile(),
            forgiveOtherExtraFiles = false,
        )
    }

    private fun TestProject.assertEmbeddedResourcesMatchReference(
        apkPath: Path,
    ) {
        val resourcesPath = projectPath.resolve("resourcesInApk")
        unzip(
            apkPath,
            outputDir = resourcesPath,
            filesStartingWith = "embed",
        )
        val referenceResources = projectPath.resolve("reference/resources").toFile()
        assertEqualDirectoriesIgnoringDotFiles(
            expected = referenceResources,
            actual = resourcesPath.toFile(),
            forgiveOtherExtraFiles = false,
        )
    }

}
