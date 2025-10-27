@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.assertNoCompileTasksGotExecuted
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency.Type.Regular
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.*
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.*
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals

@OptIn(ExperimentalWasmDsl::class)
@MppGradlePluginTests
@DisplayName("Smoke test uklib interproject consumption")
class UklibInterprojectConsumptionIT : KGPBaseTest() {

    @GradleTest
    @GradleTestVersions
    fun `interproject uklib consumption - dependency with symmetric targets - resolves uklibs`(gradleVersion: GradleVersion) {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            iosArm64()
            @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
            iosX64()
            jvm()
            js()
        }
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjects()
        ) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            val producer = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.setUklibResolutionStrategy()
                    project.setUklibPublicationStrategy()
                    project.applyMultiplatform {
                        targets()
                        sourceSets.commonMain.get().compileSource("class Common")
                    }
                }
            }

            val consumer = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.setUklibPublicationStrategy()
                    project.setUklibResolutionStrategy()
                    project.applyMultiplatform {
                        targets()
                        sourceSets.commonMain.get().compileSource("fun useCommon(arg: Common) {}")

                        sourceSets.commonMain.get().dependencies {
                            implementation(project(":producer"))
                        }
                    }
                }
            }

            include(producer, "producer")
            include(consumer, "consumer")

            build(":consumer:assemble")

            fun resolve(compilation: GradleProjectBuildScriptInjectionContext.() -> KotlinCompilation<*>): Map<ComponentPath, ResolvedComponentWithArtifacts> {
                return consumer.providerBuildScriptReturn {
                    val compilation = compilation()
                    val configuration = project.configurations.getByName(compilation.compileDependencyConfigurationName)
                    val provider = configuration.resolveProjectDependencyComponentsWithArtifactsProvider()
                    compilation.compileTaskProvider.flatMap {
                        it.outputs.files.elements
                    }.map {
                        project.ignoreAccessViolations {
                            provider.get()
                        }
                    }
                }.buildAndReturn(
                    ":consumer:assemble",
                    executingProject = this,
                    configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
                    configurationCacheProblems = BuildOptions.ConfigurationCacheProblems.WARN,
                )
            }

            assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
                mutableMapOf(
                    ":producer" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "klib",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "ios_arm64",
                            ),
                        ),
                        configuration = "uklibApiElements",
                    ),
                    "org.jetbrains.kotlin:kotlin-dom-api-compat:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                        ),
                        configuration = "commonFakeApiElements-published",
                    ),
                    "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                        ),
                        configuration = "nativeApiElements",
                    ),
                ).prettyPrinted,
                resolve {
                    kotlinMultiplatform.iosArm64().compilations.getByName("main")
                }.prettyPrinted,
            )

            assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
                mutableMapOf(
                    ":producer" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "klib",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "js_ir",
                            ),
                        ),
                        configuration = "uklibApiElements",
                    ),
                    "org.jetbrains.kotlin:kotlin-dom-api-compat:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "klib",
                                "org.gradle.category" to "library",
                                "org.gradle.jvm.environment" to "non-jvm",
                                "org.gradle.usage" to "kotlin-api",
                                "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                                "org.jetbrains.kotlin.js.compiler" to "ir",
                                "org.jetbrains.kotlin.platform.type" to "js",
                            ),
                        ),
                        configuration = "jsApiElements-published",
                    ),
                    "org.jetbrains.kotlin:kotlin-stdlib-js:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "klib",
                                "org.gradle.category" to "library",
                                "org.gradle.jvm.environment" to "non-jvm",
                                "org.gradle.usage" to "kotlin-api",
                                "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                                "org.jetbrains.kotlin.js.compiler" to "ir",
                                "org.jetbrains.kotlin.klib.packaging" to "packed",
                                "org.jetbrains.kotlin.platform.type" to "js",
                            ),
                        ),
                        configuration = "jsApiElements",
                    ),
                    "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                        ),
                        configuration = "jsApiElements",
                    ),
                ).prettyPrinted,
                resolve {
                    kotlinMultiplatform.js().compilations.getByName("main")
                }.prettyPrinted,
            )

            assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
                mutableMapOf(
                    ":producer" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "jar",
                                "org.gradle.category" to "library",
                                "org.gradle.libraryelements" to "jar",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "jvm",
                            ),
                        ),
                        configuration = "uklibApiElements",
                    ),
                    "org.jetbrains.kotlin:kotlin-dom-api-compat:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                        ),
                        configuration = if (gradleVersion < GradleVersion.version("8.0")) {
                            "commonFakeApiElements-published"
                        } else {
                            "fallbackVariant_KT-81412"
                        },
                    ),
                    "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "jar",
                                "org.gradle.category" to "library",
                                "org.gradle.jvm.environment" to "standard-jvm",
                                "org.gradle.libraryelements" to "jar",
                                "org.gradle.usage" to "java-api",
                                "org.jetbrains.kotlin.platform.type" to "jvm",
                            ),
                        ),
                        configuration = "jvmApiElements",
                    ),
                    "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "jar",
                                "org.gradle.category" to "library",
                                "org.gradle.libraryelements" to "jar",
                                "org.gradle.usage" to "java-api",
                            ),
                        ),
                        configuration = "compile",
                    ),
                ).prettyPrinted,
                resolve {
                    kotlinMultiplatform.jvm().compilations.getByName("main")
                }.prettyPrinted,
            )
        }
    }

    @GradleTest
    @GradleTestVersions
    fun `interproject ide resolution - dependency with symmetric targets`(gradleVersion: GradleVersion) {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            iosArm64()
            @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
            iosX64()
            jvm()
            js()
        }
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjects()
        ) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            val producer = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.setUklibResolutionStrategy()
                    project.setUklibPublicationStrategy()
                    project.applyMultiplatform {
                        targets()
                    }
                }
            }

            val consumer = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.setUklibPublicationStrategy()
                    project.setUklibResolutionStrategy()
                    project.applyMultiplatform {
                        targets()
                        sourceSets.commonMain.get().dependencies {
                            implementation(project(":producer"))
                        }
                    }
                }
            }

            include(producer, "producer")
            include(consumer, "consumer")

            resolveIdeDependencies("consumer") {
                assertNoCompileTasksGotExecuted()
                it["commonMain"].assertMatches(
                    kotlinStdlibDependencies,
                    binaryCoordinates(Regex(".*kotlin-dom-api-compat.*")),
                    regularSourceDependency(":producer/commonMain"),
                )
                it["appleMain"].assertMatches(
                    kotlinNativeDistributionDependencies,
                    binaryCoordinates(Regex(".*kotlin-dom-api-compat.*")),
                    regularSourceDependency(":producer/commonMain"),
                    regularSourceDependency(":producer/nativeMain"),
                    regularSourceDependency(":producer/appleMain"),
                    regularSourceDependency(":producer/iosMain"),
                    dependsOnDependency(":consumer/commonMain"),
                    dependsOnDependency(":consumer/nativeMain"),
                )
                it["iosArm64Main"].assertMatches(
                    kotlinNativeDistributionDependencies,
                    dependsOnDependency(":consumer/commonMain"),
                    dependsOnDependency(":consumer/nativeMain"),
                    dependsOnDependency(":consumer/appleMain"),
                    dependsOnDependency(":consumer/iosMain"),
                    projectArtifactDependency(Regular, ":producer", FilePathRegex(".*/iosArm64/main/klib/producer")),
                )
                it["jvmMain"].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    dependsOnDependency(":consumer/commonMain"),
                    projectArtifactDependency(Regular, ":producer", FilePathRegex(".*/producer-jvm.jar")),
                )
                it["jsMain"].assertMatches(
                    kotlinStdlibDependencies,
                    binaryCoordinates(Regex(".*kotlin-dom-api-compat.*")),
                    dependsOnDependency(":consumer/commonMain"),
                    dependsOnDependency(":consumer/webMain"),
                    projectArtifactDependency(Regular, ":producer", FilePathRegex(".*/producer/build/classes/kotlin/js/main")),
                )
            }
        }
    }

    @GradleTest
    @GradleTestVersions
    fun `interproject ide resolution - dependency with symmetric targets - with commonization`(gradleVersion: GradleVersion) {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            iosArm64()
            @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
            iosX64()
            sourceSets.commonMain.get().compileSource("class Common")
        }
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjects()
        ) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            val producer = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.enableCinteropCommonization()
                    project.setUklibResolutionStrategy()
                    project.setUklibPublicationStrategy()
                    project.applyMultiplatform {
                        targets()
                    }
                }
            }

            val consumer = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.enableCinteropCommonization()
                    project.setUklibPublicationStrategy()
                    project.setUklibResolutionStrategy()
                    project.applyMultiplatform {
                        targets()
                        sourceSets.commonMain.get().dependencies {
                            implementation(project(":producer"))
                        }
                    }
                }
            }

            include(producer, "producer")
            include(consumer, "consumer")

            build("assemble")
            build("clean")
            resolveIdeDependencies("consumer") {
                assertNoCompileTasksGotExecuted()
            }
        }
    }

}