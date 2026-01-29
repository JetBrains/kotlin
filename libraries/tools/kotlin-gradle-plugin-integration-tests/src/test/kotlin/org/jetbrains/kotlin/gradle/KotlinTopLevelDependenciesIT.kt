/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.jetbrains.kotlin.gradle.plugin.mpp.MinSupportedGradleVersionWithDependencyCollectorsConst
import org.jetbrains.kotlin.gradle.testing.PrettyPrint
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.uklibs.publishJavaPlatform
import org.jetbrains.kotlin.gradle.util.MavenModule
import org.jetbrains.kotlin.gradle.util.parsePom
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@MppGradlePluginTests
@GradleTestVersions(additionalVersions = [MinSupportedGradleVersionWithDependencyCollectorsConst])
class KotlinTopLevelDependenciesIT : KGPBaseTest() {

    override val defaultBuildOptions =
        super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()
        
    @DisplayName("Top-level dependencies block FUS events")
    @GradleTest
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun testFusCollection(gradleVersion: GradleVersion) {
        val fusEventName = BooleanMetrics.KMP_TOP_LEVEL_DEPENDENCIES_BLOCK.name
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    jvm()
                    sourceSets.commonMain.dependencies {
                        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                    }
                }
            }
            assertEquals(
                emptyList(),
                collectFusEvents(
                    "assemble",
                    buildAction = BuildActions.build
                ).filter { it.startsWith(fusEventName) },
            )
            buildScriptInjection {
                kotlinMultiplatform.dependencies {  }
            }
            val action = if (gradleVersion < GradleVersion.version(MinSupportedGradleVersionWithDependencyCollectorsConst)) {
                BuildActions.buildAndFail
            } else {
                BuildActions.build
            }
            assertEquals(
                listOf("${fusEventName}=true"),
                collectFusEvents(
                    "assemble",
                    buildAction = action
                ).filter { it.startsWith(fusEventName) },
            )
        }
    }

    @DisplayName("Test kts evaluation of top-level dependencies block")
    @GradleTest
    fun testKotlinTopLevelDependenciesKotlin(gradleVersion: GradleVersion) {
        @Language("kotlin")
        val dependenciesBlock = """
            
            kotlin {
                dependencies {
                    implementation(platform("platform:platformDependency:1.0"))
                    implementation(project(":projectDependency"))
                    implementation(libs.atomicfu) {
                        version {
                            strictly("0.28.0")
                        }
                    }
                    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
                    testImplementation(kotlin("test"))
                }
            }
        """.trimIndent()
        testKotlinDependenciesBlock("emptyKts", gradleVersion, dependenciesBlock)
    }

    @DisplayName("Test Groovy build script evaluation of top-level dependencies block")
    @GradleTest
    fun testKotlinTopLevelDependenciesGroovy(gradleVersion: GradleVersion) {
        @Language("groovy")
        val dependenciesBlock = """
            
            kotlin {
                dependencies {
                    implementation platform("platform:platformDependency:1.0")
                    implementation(project(":projectDependency"))
                    implementation(libs.atomicfu) {
                        version {
                            strictly("0.28.0")
                        }
                    }
                    api 'org.jetbrains.kotlinx:kotlinx-coroutines-core'
                    testImplementation(kotlin("test"))
                }
            }
        """.trimIndent()
        testKotlinDependenciesBlock("empty", gradleVersion, dependenciesBlock)
    }

    private fun testKotlinDependenciesBlock(
        template: String,
        gradleVersion: GradleVersion,
        consumerDependenciesBlock: String,
    ) {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            jvm()
            js(IR) {
                browser()
            }
            linuxX64()
        }
        val projectDependency = project(template, gradleVersion) {
            plugins {
                kotlin("multiplatform")
                id("org.gradle.maven-publish")
            }
            buildScriptInjection {
                project.group = "foo"
                kotlinMultiplatform.apply {
                    targets()
                    sourceSets.commonMain.get().compileSource(
                        """
                        class ProjectDependency
                        """.trimIndent()
                    )
                }
            }
        }

        val platformDependency = project(template, gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "platformDependency"
            }
            plugins {
                `java-platform`
            }
            buildScriptInjection {
                project.dependencies {
                    constraints.add("api", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                }
            }
        }.publishJavaPlatform(publisherConfiguration = PublisherConfiguration(group = "platform"))

        project(template, gradleVersion) {
            transferPluginRepositoriesIntoBuildScript()
            transferPluginDependencyConstraintsIntoBuildscriptClasspathDependencyConstraints()
            addPublishedProjectToRepositories(platformDependency)
            include(projectDependency, "projectDependency")
            val targetScript = if (buildGradle.exists()) buildGradle else buildGradleKts

            @Language("toml")
            val versionCatalog = """
                [libraries]
                atomicfu = { group = "org.jetbrains.kotlinx", name = "atomicfu" }
            """.trimIndent()
            projectPath.resolve("gradle/libs.versions.toml").writeText(versionCatalog)

            targetScript.modify {
                it.insertBlockToBuildScriptAfterPluginsAndImports(
                    """
                    plugins {
                        id("org.jetbrains.kotlin.multiplatform")
                    }
                """.trimIndent()
                )
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    val repos = project.repositories.filterIsInstance<MavenArtifactRepository>().map { it.url }
                    println(repos)
                    targets()
                    sourceSets.commonMain.get().compileSource(
                        """
                        fun main() {
                          val a = kotlinx.coroutines.Dispatchers.Default
                          val b = ProjectDependency()
                          val c = kotlinx.atomicfu.AtomicInt::class
                        }
                        """.trimIndent()
                    )
                    sourceSets.commonTest.get().compileSource(
                        """
                        fun test() {
                          val a = kotlin.test.Test::class
                        }
                        """.trimIndent()
                    )
                }
            }

            targetScript.appendText(consumerDependenciesBlock)

            if (gradleVersion < GradleVersion.version(MinSupportedGradleVersionWithDependencyCollectorsConst)) {
                buildAndFail(":assemble") {
                    assertHasDiagnostic(KotlinToolingDiagnostics.KotlinTopLevelDependenciesUsedInIncompatibleGradleVersion)
                    // Make sure we went through the configuration phase
                    assertTasksFailed(":checkKotlinGradlePluginConfigurationErrors")
                }
            } else {
                testKotlinTopLevelDependenciesCompilationAndPublication(gradleVersion)
            }
        }
    }

    private fun TestProject.testKotlinTopLevelDependenciesCompilationAndPublication(gradleVersion: GradleVersion) {
        build(":assemble", ":compileTestKotlinJvm") {
            assertNoDiagnostic(KotlinToolingDiagnostics.KotlinTopLevelDependenciesUsedInIncompatibleGradleVersion)
        }

        // Verify that the published jvm POM contains the kotlinx-coroutines dependency
        val pomFile = parsePom(
            publish(
                deriveBuildOptions = {
                    buildOptions.disableIsolatedProjectsBecauseOfSubprojectGroupAccessInPublicationBeforeGradle12(gradleVersion)
                }
            ).jvmMultiplatformComponent.pom
        )

        assertEquals<PrettyPrint<List<MavenModule>>>(
            mutableListOf(
                MavenModule(
                    artifactId = "kotlinx-coroutines-core",
                    groupId = "org.jetbrains.kotlinx",
                    scope = "compile",
                    version = null,
                ),
                MavenModule(
                    artifactId = "kotlin-stdlib",
                    groupId = "org.jetbrains.kotlin",
                    scope = "compile",
                    version = defaultBuildOptions.kotlinVersion,
                ),
                MavenModule(
                    artifactId = "projectDependency-jvm",
                    groupId = "foo",
                    scope = "runtime",
                    version = "unspecified",
                ),
                MavenModule(
                    artifactId = "atomicfu-jvm",
                    groupId = "org.jetbrains.kotlinx",
                    scope = "runtime",
                    version = "0.28.0",
                ),
            ).prettyPrinted,
            pomFile.dependencies().prettyPrinted,
        )
    }
}
