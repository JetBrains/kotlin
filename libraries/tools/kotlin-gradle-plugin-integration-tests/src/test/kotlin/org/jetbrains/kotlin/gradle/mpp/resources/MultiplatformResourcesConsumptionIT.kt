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
@DisplayName("Test resources consumption APIs")
class MultiplatformResourcesConsumptionIT : KGPBaseTest() {

    @DisplayName("Resolve resources with consumption API using variant reselection")
    // Before 7.6 Gradle fails to resolve project dependencies with variant reselection
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_74)
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

            buildGradleKts.setDependencies(
                """
                implementation("test:publishedA:+")
                implementation(project(":projectA"))
                """.trimIndent()
            )
            // Gradle 7.4.2 doesn't pick up sharedRepo from build.gradle.kts and Gradle 8.5 doesn't pick it up from settings.gradle.kts
            buildGradleKts.setUpRepositoriesInBuildGradleKts(sharedRepo)
            settingsGradleKts.setUpRepositoriesInSettingGradleKts(sharedRepo)

            val resolvableTargets = listOf(
                "linuxX64",
                "wasmJs",
                "wasmWasi",
                "js",
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

    data class Project(
        val name: String,
        val dependsOn: String?,
        val hasResources: Boolean,
    )

    private val dependencies: List<Project> = listOf(
        Project("A", dependsOn = "B", hasResources = false),
        Project("B", dependsOn = "C", hasResources = true),
        Project("C", dependsOn = "D", hasResources = false),
        Project("D", dependsOn = null, hasResources = true),
    )

    private fun prepareProjectDependencies(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk,
        publicationRepository: Path,
    ): List<Path> {
        val projectPaths = mutableListOf<Path>()
        dependencies.forEach { dependencyProject ->
            projectPaths.add(
                project(
                    "multiplatformResources/consumption/dependency",
                    gradleVersion,
                    buildJdk = providedJdk.location,
                    projectPathAdditionalSuffix = "project${dependencyProject.name}",
                ) {
                    buildGradleKts.setDependencies(
                        dependencyProject.dependsOn?.let { "implementation(project(\":project${it}\"))" } ?: ""
                    )
                    buildGradleKts.enableResourcesPublication(dependencyProject.hasResources)
                    buildGradleKts.setUpRepositoriesInBuildGradleKts(publicationRepository)
                }.projectPath
            )
        }
        return projectPaths
    }

    private fun preparePublishedDependencies(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk,
        androidVersion: String,
        publicationRepository: Path,
    ) {
        val projectToPublish = mutableListOf<TestProject>()
        dependencies.forEach { dependencyProject ->
            projectToPublish.add(
                project(
                    "multiplatformResources/consumption/dependency",
                    gradleVersion,
                    buildJdk = providedJdk.location,
                    projectPathAdditionalSuffix = "published${dependencyProject.name}",
                    localRepoDir = publicationRepository,
                ) {
                    buildGradleKts.setDependencies(
                        dependencyProject.dependsOn?.let { "implementation(\"test:published${it}:+\")" } ?: ""
                    )
                    buildGradleKts.enableResourcesPublication(dependencyProject.hasResources)
                    buildGradleKts.setUpPublishing(publicationRepository)
                    buildGradleKts.setUpRepositoriesInBuildGradleKts(publicationRepository)
                }
            )
        }
        projectToPublish.reversed().forEach {
            it.buildWithAGPVersion(
                ":publishAllPublicationsToMavenRepository",
                androidVersion = androidVersion,
                defaultBuildOptions = defaultBuildOptions,
            )
        }
    }

    private fun Path.setDependencies(dependencies: String) = replaceText("<dependencies>", dependencies)
    private fun Path.enableResourcesPublication(publish: Boolean) = replaceText("<enablePublication>", if (publish) "true" else "false")

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