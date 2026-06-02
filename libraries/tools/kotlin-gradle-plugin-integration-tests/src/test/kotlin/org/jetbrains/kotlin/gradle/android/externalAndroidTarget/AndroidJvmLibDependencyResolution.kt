/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.externalAndroidTarget

import com.android.build.api.dsl.androidLibrary
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.testing.resolveProjectDependencyComponentsWithArtifacts
import org.jetbrains.kotlin.gradle.uklibs.*
import kotlin.test.assertEquals

@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_813)
@AndroidGradlePluginTests
@OptIn(ExperimentalSerializationApi::class)
class AndroidJvmLibDependencyResolutionIT : KGPBaseTest() {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @GradleAndroidTest
    fun `test - project dependency resolves matching androidLibrary and jvm variants`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val producer = androidLibraryAndJvmProducerProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "com.example.producer",
        )

        androidLibraryAndJvmProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "com.example.consumer",
        ) {
            sourceSets.getByName("commonMain").compileSource(
                """
                package consumer

                class CommonConsumer
                """.trimIndent()
            )
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":producer"))
            }
            sourceSets.getByName("androidJvmMain").compileSource(
                """
                package consumer

                import producer.CommonProducer

                class AndroidJvmConsumer(
                    val common: CommonConsumer = CommonConsumer(),
                    val producer: CommonProducer = CommonProducer(),
                )
                """.trimIndent()
            )
            sourceSets.getByName("androidMain").compileSource(
                """
                package consumer

                import producer.AndroidProducer

                class AndroidConsumer(
                    val common: CommonConsumer = CommonConsumer(),
                    val shared: AndroidJvmConsumer = AndroidJvmConsumer(),
                    val dependency: AndroidProducer = AndroidProducer(),
                )
                """.trimIndent()
            )
            sourceSets.getByName("jvmMain").compileSource(
                """
                package consumer

                import producer.JvmProducer

                class JvmConsumer(
                    val common: CommonConsumer = CommonConsumer(),
                    val shared: AndroidJvmConsumer = AndroidJvmConsumer(),
                    val dependency: JvmProducer = JvmProducer(),
                )
                """.trimIndent()
            )
        }.apply {
            include(producer, "producer")

            build(
                ":compileCommonMainKotlinMetadata", ":compileAndroidMain", ":compileKotlinJvm"
            ) {
                assertTasksExecuted(
                    ":compileCommonMainKotlinMetadata", ":compileAndroidMain", ":compileKotlinJvm"
                )
            }
            val resolvedVariants = buildScriptReturn {
                project.ignoreAccessViolations {
                    mapOf(
                        "androidCompileClasspath" to selectedVariantDisplayName(
                            project = project,
                            configurationName = "androidCompileClasspath",
                        ),
                        "jvmCompileClasspath" to selectedVariantDisplayName(
                            project = project,
                            configurationName = "jvmCompileClasspath",
                        ),
                        "commonMainMetadata" to selectedVariantDisplayName(
                            project = project,
                            configurationName = "commonMainResolvableDependenciesMetadata",
                        ),
                        "androidRuntimeClasspath" to selectedVariantDisplayName(
                            project = project,
                            configurationName = "androidRuntimeClasspath",
                        ),
                        "jvmRuntimeClasspath" to selectedVariantDisplayName(
                            project = project,
                            configurationName = "jvmRuntimeClasspath",
                        ),
                    )
                }
            }.buildAndReturn(":help")

            val expected = mapOf(
                "androidCompileClasspath" to "androidApiElements",
                "jvmCompileClasspath" to "jvmApiElements",
                "commonMainMetadata" to "metadataApiElements",
                "androidRuntimeClasspath" to "androidRuntimeElements",
                "jvmRuntimeClasspath" to "jvmRuntimeElements",
            )

            expected.forEach { (configurationName, expectedVariant) ->
                val actualVariant = resolvedVariants.getValue(configurationName)
                assertEquals(
                    expectedVariant,
                    actualVariant,
                    "Expected '$configurationName' to resolve '$expectedVariant', but was '$actualVariant'",
                )
            }
        }
    }

    @GradleAndroidTest
    fun `test - published androidLibrary and jvm library publishes expected variants`(
        version: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val producer = androidLibraryAndJvmProducerProject(
            gradleVersion = version,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "com.example.published.producer",
        ).publish()
        val expectedMetadata = GradleMetadata(
            variants = setOf(
                Variant(
                    name = "androidApiElements-published",
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "android",
                        "org.gradle.libraryelements" to "aar",
                        "org.gradle.usage" to "java-api",
                        "org.jetbrains.kotlin.platform.type" to "jvm",
                    ),
                ),
                Variant(
                    name = "androidRuntimeElements-published",
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "android",
                        "org.gradle.libraryelements" to "aar",
                        "org.gradle.usage" to "java-runtime",
                        "org.jetbrains.kotlin.platform.type" to "jvm",
                    ),
                ),
                Variant(
                    name = "jvmApiElements-published",
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "standard-jvm",
                        "org.gradle.libraryelements" to "jar",
                        "org.gradle.usage" to "java-api",
                        "org.jetbrains.kotlin.platform.type" to "jvm",
                    ),
                ),
                Variant(
                    name = "jvmRuntimeElements-published",
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "standard-jvm",
                        "org.gradle.libraryelements" to "jar",
                        "org.gradle.usage" to "java-runtime",
                        "org.jetbrains.kotlin.platform.type" to "jvm",
                    ),
                ),
            ),
        )

        assertEquals(
            expectedMetadata.prettyPrinted,
            producer.rootComponent.gradleMetadata.inputStream().use { input ->
                GradleMetadata(
                    json.decodeFromStream<GradleMetadata>(input).variants.filter {
                            it.name in expectedMetadata.variants.map { it.name }.toSet()
                        }.map { it.copy(availableAt = null, files = emptyList()) }.toSet(),
                ).prettyPrinted
            },
        )
    }

    @GradleAndroidTest
    fun `test - published androidLibrary and jvm library exposes and resolves matching variants for KMP consumer targets`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val producer = androidLibraryAndJvmProducerProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "com.example.published.producer",
        ).publish()

        androidLibraryAndJvmProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "com.example.published.consumer",
        ) {
            sourceSets.getByName("commonMain").compileSource(
                """
                package consumer

                class CommonConsumer
                """.trimIndent()
            )
            sourceSets.getByName("commonMain").dependencies {
                implementation(producer.rootCoordinate)
            }
            sourceSets.getByName("androidJvmMain").compileSource(
                """
                package consumer

                import producer.CommonProducer

                class AndroidJvmConsumer(
                    val common: CommonConsumer = CommonConsumer(),
                    val producer: CommonProducer = CommonProducer(),
                )
                """.trimIndent()
            )
            sourceSets.getByName("androidMain").compileSource(
                """
                package consumer

                import producer.AndroidProducer

                class AndroidConsumer(
                    val common: CommonConsumer = CommonConsumer(),
                    val shared: AndroidJvmConsumer = AndroidJvmConsumer(),
                    val dependency: AndroidProducer = AndroidProducer(),
                )
                """.trimIndent()
            )
            sourceSets.getByName("jvmMain").compileSource(
                """
                package consumer

                import producer.JvmProducer

                class JvmConsumer(
                    val common: CommonConsumer = CommonConsumer(),
                    val shared: AndroidJvmConsumer = AndroidJvmConsumer(),
                    val dependency: JvmProducer = JvmProducer(),
                )
                """.trimIndent()
            )
        }.apply {
            addPublishedProjectToRepositories(producer)

            build(
                ":compileAndroidMain",
                ":compileCommonMainKotlinMetadata",
                ":compileKotlinJvm",
            ) {
                assertTasksExecuted(
                    ":compileAndroidMain",
                    ":compileCommonMainKotlinMetadata",
                    ":compileKotlinJvm",
                )
            }

            val producerRootCoordinate = producer.rootCoordinate
            val expected = mapOf(
                "androidCompileClasspath" to "androidApiElements-published",
                "jvmCompileClasspath" to "jvmApiElements-published",
                "commonMainResolvableDependenciesMetadata" to "metadataApiElements",
                "androidRuntimeClasspath" to "androidRuntimeElements-published",
                "jvmRuntimeClasspath" to "jvmRuntimeElements-published",
            )
            val configurationNames = expected.keys.toList()
            val selectedVariants = buildScriptReturn {
                project.ignoreAccessViolations {
                    configurationNames.associateWith { configurationName ->
                        project.configurations.getByName(configurationName).resolveProjectDependencyComponentsWithArtifacts()
                            .getValue(producerRootCoordinate).configuration
                    }
                }
            }.buildAndReturn(":help")

            expected.forEach { (configurationName, expectedVariant) ->
                val actualVariant = selectedVariants.getValue(configurationName)
                assertEquals(
                    expectedVariant,
                    actualVariant,
                    "Expected '${producer.rootCoordinate}' in '$configurationName' to resolve to '$expectedVariant', but was '$actualVariant'",
                )
            }
        }
    }

    @GradleAndroidTest
    fun `test - published jvm-only library is consumable from androidLibrary when no android variant is available`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val producer = jvmOnlyProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
        ) {
            sourceSets.getByName("commonMain").compileSource(
                """
                package producer

                class CommonProducer
                """.trimIndent()
            )
            sourceSets.getByName("jvmMain").compileSource(
                """
                package producer

                class JvmProducer
                """.trimIndent()
            )
        }.publish()

        androidLibraryOnlyProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "com.example.android.only.consumer",
        ) {
            sourceSets.getByName("commonMain").dependencies {
                implementation(producer.rootCoordinate)
            }
            sourceSets.getByName("commonMain").compileSource(
                """
                package consumer

                import producer.CommonProducer

                class CommonConsumer(val dependency: CommonProducer = CommonProducer())
                """.trimIndent()
            )
            sourceSets.getByName("androidMain").compileSource(
                """
                package consumer

                import producer.JvmProducer

                class AndroidConsumer(
                    val common: CommonConsumer = CommonConsumer(),
                    val dependency: JvmProducer = JvmProducer(),
                )
                """.trimIndent()
            )
        }.apply {
            addPublishedProjectToRepositories(producer)

            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
            }

            val producerRootCoordinate = producer.rootCoordinate
            val expected = mapOf(
                "androidCompileClasspath" to "jvmApiElements-published",
                "androidRuntimeClasspath" to "jvmRuntimeElements-published",
            )
            val configurationNames = expected.keys.toList()
            val selectedVariants = buildScriptReturn {
                project.ignoreAccessViolations {
                    configurationNames.associateWith { configurationName ->
                        project.configurations.getByName(configurationName).resolveProjectDependencyComponentsWithArtifacts()
                            .getValue(producerRootCoordinate).configuration
                    }
                }
            }.buildAndReturn(":help")

            expected.forEach { (configurationName, expectedVariant) ->
                val actualVariant = selectedVariants.getValue(configurationName)
                assertEquals(
                    expectedVariant,
                    actualVariant,
                    "Expected '${producer.rootCoordinate}' in '$configurationName' to resolve to '$expectedVariant', but was '$actualVariant'",
                )
            }
        }
    }

    @GradleAndroidTest
    fun `test - published androidLibrary-only library is consumable from jvm when no jvm variant is available`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val producer = androidLibraryOnlyProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "com.example.android.only.producer",
        ) {
            sourceSets.getByName("commonMain").compileSource(
                """
                package producer

                class CommonProducer
                """.trimIndent()
            )
            sourceSets.getByName("androidMain").compileSource(
                """
                package producer

                class AndroidProducer
                """.trimIndent()
            )
        }.publish()

        jvmOnlyProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
        ) {
            sourceSets.getByName("jvmMain").dependencies {
                implementation(producer.rootCoordinate)
            }
            sourceSets.getByName("jvmMain").compileSource(
                """
                package consumer

                class JvmConsumer
                """.trimIndent()
            )
        }.apply {
            addPublishedProjectToRepositories(producer)

            build(":compileKotlinJvm") {
                assertTasksExecuted(":compileKotlinJvm")
            }

            val producerRootCoordinate = producer.rootCoordinate
            val expected = mapOf(
                "jvmCompileClasspath" to "androidApiElements-published",
                "jvmRuntimeClasspath" to "androidRuntimeElements-published",
            )
            val configurationNames = expected.keys.toList()
            val selectedVariants = buildScriptReturn {
                project.ignoreAccessViolations {
                    configurationNames.associateWith { configurationName ->
                        project.configurations.getByName(configurationName).resolveProjectDependencyComponentsWithArtifacts()
                            .getValue(producerRootCoordinate).configuration
                    }
                }
            }.buildAndReturn(":help")

            expected.forEach { (configurationName, expectedVariant) ->
                val actualVariant = selectedVariants.getValue(configurationName)
                assertEquals(
                    expectedVariant,
                    actualVariant,
                    "Expected '$producerRootCoordinate' in '$configurationName' to resolve to '$expectedVariant', but was '$actualVariant'",
                )
            }
        }
    }

    private fun androidLibraryAndJvmProducerProject(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
        namespace: String,
    ): TestProject = androidLibraryAndJvmProject(
        gradleVersion = gradleVersion,
        androidVersion = androidVersion,
        jdkVersion = jdkVersion,
        namespace = namespace,
    ) {
        sourceSets.getByName("commonMain").compileSource(
            """
            package producer

            class CommonProducer
            """.trimIndent()
        )
        sourceSets.getByName("androidJvmMain").compileSource(
            """
            package producer

            class AndroidJvmProducer(val common: CommonProducer = CommonProducer())
            """.trimIndent()
        )
        sourceSets.getByName("androidMain").compileSource(
            """
            package producer

            class AndroidProducer(val shared: AndroidJvmProducer = AndroidJvmProducer())
            """.trimIndent()
        )
        sourceSets.getByName("jvmMain").compileSource(
            """
            package producer

            class JvmProducer(val shared: AndroidJvmProducer = AndroidJvmProducer())
            """.trimIndent()
        )
    }

    private fun androidLibraryAndJvmProject(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
        namespace: String,
        configureKmp: KotlinMultiplatformExtension.() -> Unit = {},
    ): TestProject = project(
        "empty",
        gradleVersion = gradleVersion,
        buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
        buildJdk = jdkVersion.location,
    ) {
        plugins {
            kotlin("multiplatform")
            id("com.android.kotlin.multiplatform.library")
        }
        buildScriptInjection {
            kotlinMultiplatform.apply {
                androidLibrary {
                    compileSdk = 34
                    this.namespace = namespace
                }
                jvm()
                macosArm64()
                val commonMain = sourceSets.getByName("commonMain")
                val androidJvmMain = sourceSets.maybeCreate("androidJvmMain").apply {
                    dependsOn(commonMain)
                }
                sourceSets.getByName("androidMain").dependsOn(androidJvmMain)
                sourceSets.getByName("jvmMain").dependsOn(androidJvmMain)
                configureKmp()
            }
        }
    }

    private fun androidLibraryOnlyProject(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
        namespace: String,
        configureKmp: KotlinMultiplatformExtension.() -> Unit = {},
    ): TestProject = project(
        "empty",
        gradleVersion = gradleVersion,
        buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
        buildJdk = jdkVersion.location,
    ) {
        plugins {
            kotlin("multiplatform")
            id("com.android.kotlin.multiplatform.library")
        }
        buildScriptInjection {
            kotlinMultiplatform.apply {
                androidLibrary {
                    compileSdk = 34
                    this.namespace = namespace
                }
                configureKmp()
            }
        }
    }

    private fun jvmOnlyProject(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
        configureKmp: KotlinMultiplatformExtension.() -> Unit = {},
    ): TestProject = project(
        "empty",
        gradleVersion = gradleVersion,
        buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
        buildJdk = jdkVersion.location,
    ) {
        plugins {
            kotlin("multiplatform")
        }
        buildScriptInjection {
            kotlinMultiplatform.apply {
                jvm()
                configureKmp()
            }
        }
    }

}

private fun selectedVariantDisplayName(
    project: Project,
    configurationName: String,
    dependencyPath: String = ":producer",
): String {
    val configuration = project.configurations.getByName(configurationName)
    val components = configuration.incoming.resolutionResult.allComponents.filter { component ->
            (component.id as? ProjectComponentIdentifier)?.projectPath == dependencyPath
        }

    val component = components.singleOrNull()
    checkNotNull(component) {
        "Expected '$dependencyPath' to be present in '$configurationName', but found: ${components.map { it.id.displayName }}"
    }

    val variant = component.variants.singleOrNull()
    checkNotNull(variant) {
        "Expected '$dependencyPath' to select a single variant in '$configurationName', but found: ${component.variants.map { it.displayName }}"
    }

    return variant.displayName
}
