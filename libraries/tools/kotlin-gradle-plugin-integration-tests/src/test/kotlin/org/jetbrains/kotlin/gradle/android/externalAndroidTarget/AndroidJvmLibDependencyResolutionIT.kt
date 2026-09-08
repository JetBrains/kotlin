/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.externalAndroidTarget

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.testing.resolveProjectDependencyComponentsWithArtifacts
import org.jetbrains.kotlin.gradle.uklibs.*
import kotlin.test.assertEquals

// Used AGP 9.0 as the minimal stable version supported for the android library
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_90)
@AndroidGradlePluginTests
@OptIn(ExperimentalSerializationApi::class)
class AndroidJvmLibDependencyResolutionIT : KGPBaseTest() {

    // Uses `com.android.kotlin.multiplatform.library`, requires AGP new DSL.
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(enableLegacyAgpDsl = false)

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
            compileAndroidLibraryAndJvmConsumerSources()
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":producer"))
            }
        }.apply {
            include(producer, "producer")

            build(
                ":compileCommonMainKotlinMetadata", ":compileAndroidMain", ":compileKotlinJvm"
            )

            val expected = mapOf(
                "androidCompileClasspath" to "androidApiElements",
                "jvmCompileClasspath" to "jvmApiElements",
                "commonMainResolvableDependenciesMetadata" to "metadataApiElements",
                "androidRuntimeClasspath" to "androidRuntimeElements",
                "jvmRuntimeClasspath" to "jvmRuntimeElements",
            )
            assertSelectedProjectDependencyVariants(expectedVariants = expected)
        }
    }

    @GradleAndroidTest
    fun `test - project dependency resolves androidLibrary variants for kotlin android consumer`(
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

        kotlinAndroidConsumerProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "com.example.kotlin.android.consumer",
        ) {
            buildScriptInjection {
                project.dependencies.add(
                    "implementation",
                    project.dependencies.project(mapOf("path" to ":producer")),
                )
            }
            compileKotlinAndroidConsumerSources()
        }.apply {
            include(producer, "producer")

            build(":compileDebugKotlin")

            val expected = mapOf(
                "debugCompileClasspath" to "androidApiElements",
                "debugRuntimeClasspath" to "androidRuntimeElements",
                "releaseCompileClasspath" to "androidApiElements",
                "releaseRuntimeClasspath" to "androidRuntimeElements",
            )
            assertSelectedProjectDependencyVariants(expectedVariants = expected)
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
                        "org.jetbrains.kotlin.platform.type" to "androidJvm",
                    ),
                ),
                Variant(
                    name = "androidRuntimeElements-published",
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "android",
                        "org.gradle.libraryelements" to "aar",
                        "org.gradle.usage" to "java-runtime",
                        "org.jetbrains.kotlin.platform.type" to "androidJvm",
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
        val expectedAndroidComponentMetadata = GradleMetadata(
            variants = setOf(
                Variant(
                    name = "androidApiElements-published",
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "android",
                        "org.gradle.libraryelements" to "aar",
                        "org.gradle.usage" to "java-api",
                        "org.jetbrains.kotlin.platform.type" to "androidJvm",
                    ),
                ),
                Variant(
                    name = "androidRuntimeElements-published",
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "android",
                        "org.gradle.libraryelements" to "aar",
                        "org.gradle.usage" to "java-runtime",
                        "org.jetbrains.kotlin.platform.type" to "androidJvm",
                    ),
                ),
            ),
        )

        assertEquals(
            expectedMetadata.prettyPrinted,
            producer.rootComponent.readPublishedVariants(expectedMetadata).prettyPrinted,
        )
        assertEquals(
            expectedAndroidComponentMetadata.prettyPrinted,
            producer.androidMultiplatformComponent.readPublishedVariants(expectedAndroidComponentMetadata).prettyPrinted,
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
            compileAndroidLibraryAndJvmConsumerSources()
            sourceSets.getByName("commonMain").dependencies {
                implementation(producer.rootCoordinate)
            }
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

            val expected = mapOf(
                "androidCompileClasspath" to "androidApiElements-published",
                "jvmCompileClasspath" to "jvmApiElements-published",
                "commonMainResolvableDependenciesMetadata" to "metadataApiElements",
                "androidRuntimeClasspath" to "androidRuntimeElements-published",
                "jvmRuntimeClasspath" to "jvmRuntimeElements-published",
            )
            assertSelectedVariantsForDependency(
                dependencyCoordinate = producer.rootCoordinate,
                expectedVariants = expected,
            )
        }
    }

    @GradleAndroidTest
    fun `test - published androidLibrary and jvm library exposes and resolves matching variants for kotlin android consumer`(
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

        kotlinAndroidConsumerProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "com.example.kotlin.android.consumer",
        ) {
            buildScriptInjection {
                project.dependencies.add("implementation", producer.rootCoordinate)
            }
            compileKotlinAndroidConsumerSources()
        }.apply {
            addPublishedProjectToRepositories(producer)

            build(":compileDebugKotlin") {
                assertTasksExecuted(":compileDebugKotlin")
            }

            val expected = mapOf(
                "debugCompileClasspath" to "androidApiElements-published",
                "debugRuntimeClasspath" to "androidRuntimeElements-published",
                "releaseCompileClasspath" to "androidApiElements-published",
                "releaseRuntimeClasspath" to "androidRuntimeElements-published",
            )
            assertSelectedVariantsForDependency(
                dependencyCoordinate = producer.rootCoordinate,
                expectedVariants = expected,
            )
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

            val expected = mapOf(
                "androidCompileClasspath" to "jvmApiElements-published",
                "androidRuntimeClasspath" to "jvmRuntimeElements-published",
            )
            assertSelectedVariantsForDependency(
                dependencyCoordinate = producer.rootCoordinate,
                expectedVariants = expected,
            )
        }
    }

    @GradleAndroidTest
    fun `test - published androidLibrary-only library is not consumable from jvm when only androidJvm variant is available`(
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

            buildAndFail(":compileKotlinJvm") {
                assertOutputContains(
                    "No matching variant of ${producer.rootCoordinate}",
                    "Expected the JVM consumer to reject the Android-only published library",
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
    ): TestProject = externalAndroidLibraryProject(
        gradleVersion = gradleVersion,
        androidVersion = androidVersion,
        jdkVersion = jdkVersion,
        namespace = namespace,
    ) {
        projectPath.resolve("gradle.properties").toFile().appendText("\nkotlin.mpp.applyDefaultHierarchyTemplate=false\n")
        buildScriptInjection {
            kotlinMultiplatform.apply {
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
    ): TestProject = externalAndroidLibraryProject(
        gradleVersion = gradleVersion,
        androidVersion = androidVersion,
        jdkVersion = jdkVersion,
        namespace = namespace,
    ) {
        buildScriptInjection {
            kotlinMultiplatform.apply {
                configureKmp()
            }
        }
    }

    private fun kotlinAndroidConsumerProject(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
        namespace: String,
        configureProject: TestProject.() -> Unit = {},
    ): TestProject {
        return project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = super.defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("android")
                id("com.android.library")
            }
            buildScriptInjection {
                with(androidLibrary) {
                    compileSdk = 34
                    this.namespace = namespace
                    compileOptions {
                        sourceCompatibility = JavaVersion.VERSION_1_8
                        targetCompatibility = JavaVersion.VERSION_1_8
                    }
                }
                project.tasks.withType(KotlinCompile::class.java).configureEach {
                    it.compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
            configureProject()
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

    private fun PublishedProject.Component.readPublishedVariants(expectedMetadata: GradleMetadata): GradleMetadata {
        val expectedVariantNames = expectedMetadata.variants.map { it.name }.toSet()
        return gradleMetadata.inputStream().use { input ->
            GradleMetadata(
                json.decodeFromStream<GradleMetadata>(input).variants
                    .filter { it.name in expectedVariantNames }
                    .map { it.copy(availableAt = null, files = emptyList()) }
                    .toSet(),
            )
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

private fun TestProject.selectedVariantsForDependency(
    dependencyCoordinate: String,
    configurationNames: Iterable<String>,
): Map<String, String> {
    val serializableConfigurationNames = configurationNames.toList()
    return buildScriptReturn {
        project.ignoreAccessViolations {
            serializableConfigurationNames.associateWith { configurationName ->
                project.configurations.getByName(configurationName).resolveProjectDependencyComponentsWithArtifacts()
                    .getValue(dependencyCoordinate).configuration
            }
        }
    }.buildAndReturn(":help")
}

private fun TestProject.selectedProjectDependencyVariants(
    dependencyPath: String,
    configurationNames: Iterable<String>,
): Map<String, String> {
    val serializableConfigurationNames = configurationNames.toList()
    return buildScriptReturn {
        project.ignoreAccessViolations {
            serializableConfigurationNames.associateWith { configurationName ->
                selectedVariantDisplayName(
                    project = project,
                    configurationName = configurationName,
                    dependencyPath = dependencyPath,
                )
            }
        }
    }.buildAndReturn(":help")
}

private fun TestProject.assertSelectedVariantsForDependency(
    dependencyCoordinate: String,
    expectedVariants: Map<String, String>,
) {
    val selectedVariants = selectedVariantsForDependency(
        dependencyCoordinate = dependencyCoordinate,
        configurationNames = expectedVariants.keys,
    )

    expectedVariants.forEach { entry ->
        val configurationName = entry.key
        val expectedVariant = entry.value
        val actualVariant = selectedVariants.getValue(configurationName)
        assertEquals(
            expectedVariant,
            actualVariant,
            "Expected '$dependencyCoordinate' in '$configurationName' to resolve to '$expectedVariant', but was '$actualVariant'",
        )
    }
}

private fun TestProject.assertSelectedProjectDependencyVariants(
    dependencyPath: String = ":producer",
    expectedVariants: Map<String, String>,
) {
    val selectedVariants = selectedProjectDependencyVariants(
        dependencyPath = dependencyPath,
        configurationNames = expectedVariants.keys,
    )

    expectedVariants.forEach { entry ->
        val configurationName = entry.key
        val expectedVariant = entry.value
        val actualVariant = selectedVariants.getValue(configurationName)
        assertEquals(
            expectedVariant,
            actualVariant,
            "Expected '$dependencyPath' in '$configurationName' to resolve to '$expectedVariant', but was '$actualVariant'",
        )
    }
}

private fun TestProject.compileKotlinAndroidConsumerSources() {
    kotlinSourcesDir().source("consumer/AndroidConsumer.kt") {
        """
        package consumer

        import producer.CommonProducer
        import producer.AndroidJvmProducer
        import producer.AndroidProducer

        class AndroidConsumer(
            val common: CommonProducer = CommonProducer(),
            val androidJvmProducer: AndroidJvmProducer = AndroidJvmProducer(),
            val dependency: AndroidProducer = AndroidProducer(),
        )
        """.trimIndent()
    }
}

private fun KotlinMultiplatformExtension.compileAndroidLibraryAndJvmConsumerSources() {
    sourceSets.getByName("commonMain").compileSource(
        """
        package consumer

        class CommonConsumer
        """.trimIndent()
    )
    sourceSets.getByName("androidJvmMain").compileSource(
        """
        package consumer

        import producer.CommonProducer
        import producer.AndroidJvmProducer

        class AndroidJvmConsumer(
            val common: CommonConsumer = CommonConsumer(),
            val producer: CommonProducer = CommonProducer(),
            val androidJvmProducer: AndroidJvmProducer = AndroidJvmProducer(),
        )
        """.trimIndent()
    )
    sourceSets.getByName("androidMain").compileSource(
        """
        package consumer

        import producer.AndroidProducer
        import producer.AndroidJvmProducer

        class AndroidConsumer(
            val common: CommonConsumer = CommonConsumer(),
            val shared: AndroidJvmConsumer = AndroidJvmConsumer(),
            val androidJvmProducer: AndroidJvmProducer = AndroidJvmProducer(),
            val dependency: AndroidProducer = AndroidProducer(),
        )
        """.trimIndent()
    )
    sourceSets.getByName("jvmMain").compileSource(
        """
        package consumer

        import producer.JvmProducer
        import producer.AndroidJvmProducer

        class JvmConsumer(
            val common: CommonConsumer = CommonConsumer(),
            val shared: AndroidJvmConsumer = AndroidJvmConsumer(),
            val androidJvmProducer: AndroidJvmProducer = AndroidJvmProducer(),
            val dependency: JvmProducer = JvmProducer(),
        )
        """.trimIndent()
    )
}
