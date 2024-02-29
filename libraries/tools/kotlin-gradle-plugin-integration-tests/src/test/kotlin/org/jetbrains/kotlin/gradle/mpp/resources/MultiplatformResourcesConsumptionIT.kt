/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.resources

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.KotlinTargetResourcesResolutionStrategy
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectoriesIgnoringDotFiles
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

@MppGradlePluginTests
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_73)
@DisplayName("Test resources consumption APIs")
class MultiplatformResourcesConsumptionIT : KGPBaseTest() {

    @DisplayName("Resolve resources with consumption API using variant reselection")
    // Before 7.6 Gradle fails to resolve project dependencies with variant reselection
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_74)
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_6)
    @GradleAndroidTest
    fun testWithVariantReselection(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        test(gradleVersion, androidVersion, providedJdk, KotlinTargetResourcesResolutionStrategy.VariantReselection)
    }

    @DisplayName("Resolve resources with consumption API using resources configuration")
    @GradleAndroidTest
    fun testWithConfiguration(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        test(gradleVersion, androidVersion, providedJdk, KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration)
    }

    private fun test(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
        resolutionStrategy: KotlinTargetResourcesResolutionStrategy,
    ) {
        project(
            "multiplatformResources/consumption",
            gradleVersion,
            buildJdk = providedJdk.location,
        ) {
            val sharedRepo = projectPath.resolve("build/repo")
            prepareProjectDependencies(gradleVersion, providedJdk, sharedRepo).forEach {
                settingsGradleKts.append(
                    """
                        include(":${it.name}")
                        project(":${it.name}").projectDir = File("${it.escapedPathString}")
                    """.trimIndent()
                )
            }
            preparePublishedDependencies(gradleVersion, providedJdk, androidVersion, sharedRepo)

            // Gradle 7.4.2 doesn't pick up sharedRepo from build.gradle.kts and Gradle 8.5 doesn't pick it up from settings.gradle.kts
            buildGradleKts.setUpRepositoriesInBuildGradleKts(sharedRepo)
            settingsGradleKts.setUpRepositoriesInSettingGradleKts(sharedRepo)

            val resolvableTargets = listOf(
                "linuxX64",
                "wasmJs",
                "wasmWasi",
            ) + if (HostManager.hostIsMac) listOf("iosArm64") else emptyList()

            resolvableTargets.forEach { target ->
                buildWithAGPVersion(
                    ":${target}ResolveResources", "-Pkotlin.mpp.resourcesResolutionStrategy=${resolutionStrategy.propertyName}",
                    androidVersion = androidVersion,
                    defaultBuildOptions = defaultBuildOptions,
                ) {
                    assertEqualDirectoriesIgnoringDotFiles(
                        projectPath.resolve("reference/${target}").toFile(),
                        projectPath.resolve("build/${target}ResolvedResources").toFile(),
                        forgiveOtherExtraFiles = false,
                    )
                }
            }

            // This platform is not provided in dependency variants
            buildAndFailWithAGPVersion(
                ":linuxArm64",
                androidVersion = androidVersion,
                defaultBuildOptions = defaultBuildOptions,
            )
        }
    }

    private fun prepareProjectDependencies(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk,
        publicationRepository: Path,
    ): List<Path> {
        val projectWithoutResources = project(
            "multiplatformResources/consumption/dependency",
            gradleVersion,
            buildJdk = providedJdk.location,
            projectPathAdditionalSuffix = "projectWithoutResources",
        ) {
            buildGradleKts.replaceText("<dependencies>", "implementation(project(\":projectWithResources\"))")
            buildGradleKts.replaceText("<enablePublication>", "false")
            buildGradleKts.setUpRepositoriesInBuildGradleKts(publicationRepository)
        }
        val projectWithResources = project(
            "multiplatformResources/consumption/dependency",
            gradleVersion,
            buildJdk = providedJdk.location,
            projectPathAdditionalSuffix = "projectWithResources",
        ) {
            buildGradleKts.replaceText("<dependencies>", "implementation(project(\":projectWithResourcesTransitive\"))")
            buildGradleKts.replaceText("<enablePublication>", "true")
            buildGradleKts.setUpRepositoriesInBuildGradleKts(publicationRepository)
        }
        val projectWithResourcesTransitive = project(
            "multiplatformResources/consumption/dependency",
            gradleVersion,
            buildJdk = providedJdk.location,
            projectPathAdditionalSuffix = "projectWithResourcesTransitive",
        ) {
            buildGradleKts.replaceText("<dependencies>", "")
            buildGradleKts.replaceText("<enablePublication>", "true")
            buildGradleKts.setUpRepositoriesInBuildGradleKts(publicationRepository)
        }
        return listOf(projectWithoutResources, projectWithResources, projectWithResourcesTransitive).map { it.projectPath }
    }

    private fun preparePublishedDependencies(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk,
        androidVersion: String,
        publicationRepository: Path,
    ) {
        val publishedWithoutResources = project(
            "multiplatformResources/consumption/dependency",
            gradleVersion,
            buildJdk = providedJdk.location,
            projectPathAdditionalSuffix = "publishedWithoutResources",
            localRepoDir = publicationRepository,
        ) {
            buildGradleKts.replaceText("<dependencies>", "implementation(\"test:publishedWithResources:+\")")
            buildGradleKts.replaceText("<enablePublication>", "false")
            buildGradleKts.setUpPublishing(publicationRepository)
            buildGradleKts.setUpRepositoriesInBuildGradleKts(publicationRepository)
        }
        val publishedWithResources = project(
            "multiplatformResources/consumption/dependency",
            gradleVersion,
            buildJdk = providedJdk.location,
            projectPathAdditionalSuffix = "publishedWithResources",
            localRepoDir = publicationRepository,
        ) {
            buildGradleKts.replaceText("<dependencies>", "implementation(\"test:publishedWithResourcesTransitive:+\")")
            buildGradleKts.replaceText("<enablePublication>", "true")
            buildGradleKts.setUpPublishing(publicationRepository)
            buildGradleKts.setUpRepositoriesInBuildGradleKts(publicationRepository)
        }
        val publishedWithResourcesTransitive = project(
            "multiplatformResources/consumption/dependency",
            gradleVersion,
            buildJdk = providedJdk.location,
            projectPathAdditionalSuffix = "publishedWithResourcesTransitive",
            localRepoDir = publicationRepository,
        ) {
            buildGradleKts.replaceText("<dependencies>", "")
            buildGradleKts.replaceText("<enablePublication>", "true")
            buildGradleKts.setUpPublishing(publicationRepository)
            buildGradleKts.setUpRepositoriesInBuildGradleKts(publicationRepository)
        }
        listOf(publishedWithResourcesTransitive, publishedWithResources, publishedWithoutResources).forEach {
            it.buildWithAGPVersion(
                ":publishAllPublicationsToMavenRepository",
                androidVersion = androidVersion,
                defaultBuildOptions = defaultBuildOptions,
            )
        }
    }

    private fun Path.setUpRepositoriesInBuildGradleKts(publicationRepository: Path) = append(repositories(publicationRepository))
    private fun Path.setUpRepositoriesInSettingGradleKts(publicationRepository: Path) = append(
        """
            dependencyResolutionManagement {
                ${repositories(publicationRepository)}
            }
        """.trimIndent()
    )

    private fun repositories(publicationRepository: Path): String {
        return """
            repositories {
                google()
                mavenCentral()
                mavenLocal()
                maven {
                    url = uri("${publicationRepository.escapedPathString}")
                }
            }
        """.trimIndent()
    }

    private fun Path.setUpPublishing(publicationRepository: Path) {
        append(
            """
                publishing {
                    repositories {
                        maven {
                            url = uri("${publicationRepository.escapedPathString}")
                        }
                    }
                }
            """.trimIndent()
        )
    }

    private val Path.escapedPathString: String get() = pathString.replace("\\", "\\\\")

}