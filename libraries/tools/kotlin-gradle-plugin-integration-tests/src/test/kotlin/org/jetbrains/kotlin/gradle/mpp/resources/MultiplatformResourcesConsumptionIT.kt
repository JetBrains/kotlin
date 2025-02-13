/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.mpp.resources

import org.gradle.api.tasks.Copy
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.*
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectoriesIgnoringDotFiles
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.DisplayName

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
        project(
            "multiplatformResources/consumption",
            gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
        ) {
            addKgpToBuildScriptCompilationClasspath()
            addAgpToBuildScriptCompilationClasspath(androidVersion)

            prepareProjectDependencies(gradleVersion, providedJdk, androidVersion).forEach {
                include(it.second, it.first)
            }
            preparePublishedDependencies(gradleVersion, providedJdk, androidVersion).forEach {
                addPublishedProjectToRepositories(it)
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    val publication = project.extraProperties.get(
                        KotlinTargetResourcesPublication.EXTENSION_NAME
                    ) as KotlinTargetResourcesPublication
                    listOf(
                        linuxArm64(),
                        linuxX64(),
                        iosArm64(),
                        iosSimulatorArm64(),
                        wasmJs(),
                        wasmWasi(),
                        js(),
                    ).forEach { target ->
                        val assemblyTask = publication.resolveResources(target)
                        project.tasks.register("${target.disambiguationClassifier}ResolveResources", Copy::class.java) {
                            it.from(assemblyTask)
                            it.into(project.layout.buildDirectory.dir("${target.disambiguationClassifier}ResolvedResources"))
                        }
                    }

                    sourceSets.getByName("commonMain").dependencies {
                        implementation("test:publishedA:+")
                        implementation(project(":projectA"))
                    }
                }
            }

            val resolvableTargets = listOf(
                "linuxX64",
                "wasmJs",
                "wasmWasi",
                "js",
            ) + if (HostManager.hostIsMac) listOf("iosArm64") else emptyList()

            resolvableTargets.forEach { target ->
                build(":${target}ResolveResources") {
                    assertEqualDirectoriesIgnoringDotFiles(
                        projectPath.resolve("reference/${target}").toFile(),
                        projectPath.resolve("build/${target}ResolvedResources").toFile(),
                        forgiveOtherExtraFiles = false,
                    )
                }
            }

            val linuxArm64ResourcesDirectory = providerBuildScriptReturn {
                val publication = project.extraProperties.get(
                    KotlinTargetResourcesPublication.EXTENSION_NAME
                ) as KotlinTargetResourcesPublication
                publication.resolveResources(kotlinMultiplatform.linuxArm64())
            }.buildAndReturn("linuxArm64ResolveResources")
            assert(
                linuxArm64ResourcesDirectory.list().isEmpty(),
                { "linuxArm64 resources resolution is expected to produce an empty directory or a resolution failure because dependencies of test project don't contain linuxArm64 resources" }
            )
        }
    }

    data class ResourcesProject(
        val name: String,
        val resourcesDependency: String?,
        val hasResources: Boolean,
    ) : java.io.Serializable

    // Keep this list in reversed topsorted order by resourcesDependency
    private val dependencies: List<ResourcesProject> = listOf(
        ResourcesProject("A", resourcesDependency = "B", hasResources = false),
        ResourcesProject("B", resourcesDependency = "C", hasResources = true),
        ResourcesProject("C", resourcesDependency = "D", hasResources = false),
        ResourcesProject("D", resourcesDependency = null, hasResources = true),
    )

    private fun prepareProjectDependencies(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk,
        androidVersion: String,
    ) = dependencies.map { dependencyProject ->
        "project${dependencyProject.name}" to resourceProducer(
            gradleVersion,
            providedJdk,
            androidVersion,
            dependencyProject,
        ) {
            buildScriptInjection {
                dependencyProject.resourcesDependency?.let { dependencyName ->
                    kotlinMultiplatform.sourceSets.getByName("commonMain").dependencies {
                        implementation(project(":project${dependencyName}"))
                    }
                }
            }
        }
    }

    private fun preparePublishedDependencies(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk,
        androidVersion: String,
    ): List<PublishedProject> {
        val publishedProjects = mutableMapOf<String, PublishedProject>()
        dependencies.reversed().forEach { dependencyProject ->
            val published = resourceProducer(
                gradleVersion,
                providedJdk,
                androidVersion,
                dependencyProject,
            ) {
                val dependency: PublishedProject? = dependencyProject.resourcesDependency?.let { publishedProjects[it]!! }
                publishedProjects.values.forEach {
                    addPublishedProjectToRepositories(it)
                }
                settingsBuildScriptInjection {
                    settings.rootProject.name = "published${dependencyProject.name}"
                }
                buildScriptInjection {
                    dependency?.let { dep ->
                        kotlinMultiplatform.sourceSets.getByName("commonMain").dependencies {
                            implementation(dep.rootCoordinate)
                        }
                    }
                }
            }.publish(
                publisherConfiguration = PublisherConfiguration(group = "test"),
                deriveBuildOptions = {
                    buildOptions.copy(androidVersion = androidVersion)
                }
            )
            publishedProjects[dependencyProject.name] = published
        }
        return publishedProjects.values.toList()
    }

    private fun resourceProducer(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk,
        androidVersion: String,
        dependencyProject: ResourcesProject,
        configuration: TestProject.() -> Unit,
    ): TestProject {
        return project(
            "multiplatformResources/consumption/dependency",
            gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
        ) {
            addKgpToBuildScriptCompilationClasspath()
            addAgpToBuildScriptCompilationClasspath(androidVersion)
            buildScriptInjection {
                project.applyMultiplatform {}
                project.plugins.apply("com.android.library")

                configureStandardResourcesProducerTargets(withAndroid = true)

                if (dependencyProject.hasResources) {
                    kotlinMultiplatform.configureStandardResourcesAndAssetsPublication()
                }
            }
            configuration()
        }
    }

}