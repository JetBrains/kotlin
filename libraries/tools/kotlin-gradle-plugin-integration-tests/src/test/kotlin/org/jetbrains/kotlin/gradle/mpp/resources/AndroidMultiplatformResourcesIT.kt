/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.resources

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectoriesIgnoringDotFiles
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path

@MppGradlePluginTests
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_73)
@DisplayName("Test multiplatform resources consumption and publication in androidTarget()")
class AndroidMultiplatformResourcesIT : KGPBaseTest() {

    @DisplayName("Multiplatform resources consumption from self, published and project dependencies for Android target")
    @GradleAndroidTest
    fun testConsumption(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        publishDependencyProject(
            gradleVersion,
            providedJdk,
            androidVersion
        ) { repositoryRoot ->
            project(
                "multiplatformResources/android",
                gradleVersion,
                buildJdk = providedJdk.location,
                localRepoDir = repositoryRoot,
            ) {
                settingsGradleKts.append(
                    """
                        include(":consumer")
                        include(":projectDependency")
                    """.trimIndent()
                )

                buildWithAGPVersion(
                    ":consumer:zipApksForDebug",
                    androidVersion = androidVersion,
                    defaultBuildOptions = defaultBuildOptions,
                )
                val apkPath = projectPath.resolve("consumer/build/outputs/apk/debug/consumer-debug.apk")
                assertAssetsMatchReference(apkPath)
                assertEmbeddedResourcesMatchReference(apkPath)
            }
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

    private fun publishDependencyProject(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk,
        androidVersion: String,
        withRepositoryRoot: (Path) -> (Unit),
    ) {
        project(
            "multiplatformResources/publication",
            gradleVersion,
            buildJdk = providedJdk.location,
        ) {
            buildWithAGPVersion(
                ":publishAllPublicationsToMavenRepository",
                androidVersion = androidVersion,
                defaultBuildOptions = defaultBuildOptions,
            )
            withRepositoryRoot(projectPath.resolve("build/repo"))
        }
    }

}