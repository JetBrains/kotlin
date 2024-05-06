/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.KotlinTargetResourcesResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KotlinTargetVariantResourcesResolutionTests {

    @Test
    fun `test direct dependency - is the same - for all resolution methods and supported scopes`() {
        KotlinTargetResourcesResolutionStrategy.values().forEach { resolutionMethod ->
            dependencyScopesWithResources().forEach { dependencyScope ->
                testDirectDependencyOnResourcesProducer(
                    producerTarget = { linuxX64() },
                    consumerTarget = { linuxX64() },
                    dependencyScope = dependencyScope,
                    resolutionStrategy = resolutionMethod,
                    expectedResult = { _, producer ->
                        hashSetOf(producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.kotlin_resources.zip"))
                    },
                )
            }
        }
    }

    @Test
    fun `test direct dependency - between different native targets - with artifact view`() {
        testDirectDependencyOnResourcesProducer(
            producerTarget = { linuxX64() },
            consumerTarget = { linuxArm64() },
            resolutionStrategy = KotlinTargetResourcesResolutionStrategy.VariantReselection,
            expectedResult = { _, _ -> emptySet() },
        )
    }

    @Test
    fun `test direct dependency - between different native targets - with resources configuration`() {
        testDirectDependencyOnResourcesProducer(
            producerTarget = { linuxX64() },
            consumerTarget = { linuxArm64() },
            resolutionStrategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
            expectedResult = { _, _ -> emptySet() },
        )
    }

    @Test
    fun `test direct dependency - for wasmJs, wasmWasi, js targets - when using artifact view`() {
        listOf<TargetProvider>(
            { wasmJs() },
            { wasmWasi() },
            { js() },
        ).forEach { target ->
            testDirectDependencyOnResourcesProducer(
                producerTarget = { target() },
                consumerTarget = { target() },
                resolutionStrategy = KotlinTargetResourcesResolutionStrategy.VariantReselection,
                expectedResult = { _, producer ->
                    hashSetOf(
                        producer.buildFile(
                            "kotlin-multiplatform-resources/zip-for-publication/${producer.multiplatformExtension.target().name}/producer.kotlin_resources.zip"
                        ),
                    )
                },
            )
        }
    }

    @Test
    fun `test direct dependency - for wasmJs - when using resources configuration`() {
        testDirectDependencyOnResourcesProducer(
            producerTarget = { wasmJs() },
            consumerTarget = { wasmJs() },
            resolutionStrategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
            filterResolvedFiles = {
                it.filterNot {
                    it.path.contains("kotlin-stdlib-wasm-js")
                }.toSet()
            },
            expectedResult = { _, producer ->
                hashSetOf(
                    producer.buildFile(
                        "kotlin-multiplatform-resources/zip-for-publication/wasmJs/producer.kotlin_resources.zip"
                    ),
                )
            },
        )
    }

    @Test
    fun `test direct dependency - for wasmWasi - when using resources configuration`() {
        testDirectDependencyOnResourcesProducer(
            producerTarget = { wasmWasi() },
            consumerTarget = { wasmWasi() },
            resolutionStrategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
            filterResolvedFiles = {
                it.filterNot {
                    it.path.contains("kotlin-stdlib-wasm-wasi")
                }.toSet()
            },
            expectedResult = { _, producer ->
                hashSetOf(
                    producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/wasmWasi/producer.kotlin_resources.zip"),
                )
            },
        )
    }

    @Test
    fun `test direct dependency - for js - when using resources configuration`() {
        testDirectDependencyOnResourcesProducer(
            producerTarget = { js() },
            consumerTarget = { js() },
            resolutionStrategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
            filterResolvedFiles = {
                it.filterNot {
                    it.path.contains("kotlin-stdlib-js") || it.path.contains("kotlin-dom-api-compat")
                }.toSet()
            },
            expectedResult = { _, producer ->
                hashSetOf(
                    producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/js/producer.kotlin_resources.zip"),
                )
            },
        )
    }

    @Test
    fun `test transitive dependency - without resources in middle project - with configuration`() {
        testTransitiveDependencyOnResourcesProducer(
            targetProvider = { linuxX64() },
            resolutionStrategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
            expectedResult = { _, middle, producer ->
                setOf(
                    producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.kotlin_resources.zip"),
                    middle.buildFile("classes/kotlin/linuxX64/main/klib/middle.klib"),
                )
            }
        )
    }

    @Test
    fun `test transitive dependency - without resources in middle project in wasm - with configuration`() {
        dependencyScopesWithResources().forEach { dependencyScope ->
            testTransitiveDependencyOnResourcesProducer(
                targetProvider = { wasmJs() },
                resolutionStrategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
                dependencyScope = dependencyScope,
                filterResolvedFiles = {
                    it.filterNot {
                        it.path.contains("kotlin-stdlib-wasm-js")
                    }.toSet()
                },
                expectedResult = { _, middle, producer ->
                    setOf(
                        producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/wasmJs/producer.kotlin_resources.zip"),
                        middle.buildFile("libs/middle-wasm-js.klib"),
                    )
                }
            )
        }
    }

    @Test
    fun `test transitive dependency - without resources in middle project in wasm - with artifact view`() {
        dependencyScopesWithResources().forEach { dependencyScope ->
            testTransitiveDependencyOnResourcesProducer(
                targetProvider = { wasmJs() },
                resolutionStrategy = KotlinTargetResourcesResolutionStrategy.VariantReselection,
                dependencyScope = dependencyScope,
                filterResolvedFiles = {
                    it.filterNot {
                        it.path.contains("kotlin-stdlib-wasm-js")
                    }.toSet()
                },
                expectedResult = { _, _, producer ->
                    setOf(
                        producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/wasmJs/producer.kotlin_resources.zip"),
                    )
                }
            )
        }
    }

    @Test
    fun `test transitive dependency - without resources in middle project in js - with configuration`() {
        dependencyScopesWithResources().forEach { dependencyScope ->
            testTransitiveDependencyOnResourcesProducer(
                targetProvider = { js() },
                resolutionStrategy = KotlinTargetResourcesResolutionStrategy.VariantReselection,
                dependencyScope = dependencyScope,
                filterResolvedFiles = {
                    it.filterNot {
                        it.path.contains("kotlin-stdlib-js") || it.path.contains("kotlin-dom-api-compat")
                    }.toSet()
                },
                expectedResult = { _, _, producer ->
                    setOf(
                        producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/js/producer.kotlin_resources.zip"),
                    )
                }
            )
        }
    }

    @Test
    fun `test transitive dependency - without resources in middle project in js - with artifact view`() {
        dependencyScopesWithResources().forEach { dependencyScope ->
            testTransitiveDependencyOnResourcesProducer(
                targetProvider = { js() },
                resolutionStrategy = KotlinTargetResourcesResolutionStrategy.VariantReselection,
                dependencyScope = dependencyScope,
                filterResolvedFiles = { it },
                expectedResult = { _, _, producer ->
                    setOf(
                        producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/js/producer.kotlin_resources.zip"),
                    )
                }
            )
        }
    }

    @Test
    fun `test transitive dependency - without resources in middle project - with artifact view`() {
        testTransitiveDependencyOnResourcesProducer(
            targetProvider = { linuxX64() },
            resolutionStrategy = KotlinTargetResourcesResolutionStrategy.VariantReselection,
            expectedResult = { _, _, producer ->
                setOf(
                    producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.kotlin_resources.zip"),
                    // VariantReselection strategy disables compatibility rule that resolves klibs for resources configuration
                    // middle.buildFile("classes/kotlin/linuxX64/main/klib/middle.klib"),
                )
            }
        )
    }

    @Test
    fun `test transitive dependency - with resources in middle project - with artifact view`() {
        dependencyScopesWithResources().forEach { dependencyScope ->
            testTransitiveDependencyOnResourcesProducer(
                targetProvider = { linuxX64() },
                resolutionStrategy = KotlinTargetResourcesResolutionStrategy.VariantReselection,
                dependencyScope = dependencyScope,
                middlePublishesResources = true,
                expectedResult = { _, middle, producer ->
                    setOf(
                        middle.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/middle.kotlin_resources.zip"),
                        producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.kotlin_resources.zip"),
                    )
                }
            )
        }
        testTransitiveDependencyOnResourcesProducer(
            targetProvider = { linuxX64() },
            resolutionStrategy = KotlinTargetResourcesResolutionStrategy.VariantReselection,
            middlePublishesResources = true,
            consumerPublishesResources = true,
            expectedResult = { _, middle, producer ->
                setOf(
                    middle.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/middle.kotlin_resources.zip"),
                    producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.kotlin_resources.zip"),
                )
            }
        )
    }

    @Test
    fun `test transitive dependency - with resources in middle project - with configuration`() {
        dependencyScopesWithResources().forEach { dependencyScope ->
            testTransitiveDependencyOnResourcesProducer(
                targetProvider = { linuxX64() },
                resolutionStrategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
                dependencyScope = dependencyScope,
                middlePublishesResources = true,
                expectedResult = { _, middle, producer ->
                    setOf(
                        middle.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/middle.kotlin_resources.zip"),
                        producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.kotlin_resources.zip"),
                    )
                }
            )
        }
        testTransitiveDependencyOnResourcesProducer(
            targetProvider = { linuxX64() },
            resolutionStrategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
            middlePublishesResources = true,
            consumerPublishesResources = true,
            expectedResult = { _, middle, producer ->
                setOf(
                    middle.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/middle.kotlin_resources.zip"),
                    producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.kotlin_resources.zip"),
                )
            }
        )
    }

    @Test
    fun `test resources - don't leak into non-resources configurations`() {
        resourcesConfigurationResolutionSanityCheck()

        val targetsToTest = listOf<TargetProvider>(
            { wasmJs() },
            { wasmWasi() },
            { js() },
            { linuxX64() },
            { iosArm64() },
        )
        targetsToTest.indices.forEach { index ->
            // Test when target is matching
            testNonResourcesConfigurationDontResolveResourceVariants(
                producerTarget = targetsToTest[index],
                consumerTarget = targetsToTest[index],
                strategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
            )

            // Test when target is not matching
            testNonResourcesConfigurationDontResolveResourceVariants(
                producerTarget = targetsToTest[index],
                consumerTarget = targetsToTest[(index + 1) % targetsToTest.count()],
                strategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
            )
        }
    }

    @Test
    fun `test KT66393 - resolving resources with java-api dependencies in native configuration`() {
        val resolutionStrategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration
        val rootProject = buildProject()
        val producer = rootProject.createSubproject(
            "producer",
            resolutionStrategy = resolutionStrategy,
        ) {
            kotlin {
                linuxArm64()
                sourceSets.commonMain {
                    dependencies {
                        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")
                    }
                }
            }
        }
        val consumer = rootProject.createSubproject(
            "consumer",
            resolutionStrategy = resolutionStrategy,
        ) {
            kotlin {
                linuxArm64()
                sourceSets.commonMain {
                    dependencies {
                        implementation(project(":${producer.name}"))
                    }
                }
            }
        }

        listOf(rootProject, producer, consumer).forEach { it.evaluate() }
        producer.publishFakeResources(producer.multiplatformExtension.linuxArm64())

        assertEquals(
            hashSetOf(producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxArm64/producer.kotlin_resources.zip")),
            resolutionStrategy.resourceArchives(
                consumer.multiplatformExtension.linuxArm64().compilations.getByName("main")
            ).files
        )
    }

    @Test
    fun `test resources configuration - only exists in projects with resources configuration strategy`() {
        val rootProject = buildProject()
        assertNotNull(
            rootProject.createSubproject(
                "resourcesConfiguration",
                resolutionStrategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
            ) { kotlin { linuxArm64() } }.configurations.findByName("linuxArm64ResourcesPath")
        )
        assertNull(
            rootProject.createSubproject(
                "variantReselection",
                resolutionStrategy = KotlinTargetResourcesResolutionStrategy.VariantReselection,
            ) { kotlin { linuxArm64() } }.configurations.findByName("linuxArm64ResourcesPath")
        )
    }

    private fun dependencyScopesWithResources(): List<DependencyScopeProvider> {
        return listOf(
            { this::implementation },
            { this::api },
        )
    }

    private fun resourcesConfigurationResolutionSanityCheck() {
        directDependencyOnResourcesProducer(
            producerTarget = { linuxX64() },
            consumerTarget = { linuxX64() },
            strategy = KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration,
            assert = { consumer, _ ->
                val resourcesConfiguration = consumer.multiplatformExtension.linuxX64()
                    .compilations.getByName("main")
                    .internal.configurations.resourcesConfiguration ?: error("Missing resources configuration")

                assertEquals(
                    mapOf(
                        // linuxX64ResourcesPath is the root resolvable configuration for the resolution
                        "test:consumer" to listOf(listOf("linuxX64ResourcesPath")),
                        // stdlib doesn't have resources, so apiElements is selected as per compatibility rule
                        "org.jetbrains.kotlin:kotlin-stdlib" to listOf(listOf("nativeApiElements")),
                        // producer provides the consumable configuration with resources for the consumer
                        "test:producer" to listOf(listOf("linuxX64ResourcesElements")),
                    ),
                    resourcesConfiguration.incoming.resolutionResult.allComponents.groupBy(
                        keySelector = { "${it.moduleVersion?.group}:${it.moduleVersion?.name}" },
                        valueTransform = { it.variants.map { it.displayName } },
                    )
                )
            }
        )
    }

    private fun testNonResourcesConfigurationDontResolveResourceVariants(
        producerTarget: TargetProvider,
        consumerTarget: TargetProvider,
        strategy: KotlinTargetResourcesResolutionStrategy,
    ) {
        directDependencyOnResourcesProducer(
            producerTarget = producerTarget,
            consumerTarget = consumerTarget,
            strategy = strategy,
            assert = { consumer, _ ->
                val resourcesConfigurations = consumer.multiplatformExtension.targets.flatMap {
                    it.compilations.mapNotNull { it.internal.configurations.resourcesConfiguration }
                }

                val nonResourcesConfigurations: Set<Configuration> = consumer.configurations.filter {
                    it.isCanBeResolved
                }.toHashSet() - resourcesConfigurations
                assert(nonResourcesConfigurations.isNotEmpty())

                nonResourcesConfigurations.forEach { resolvableConfiguration ->
                    val resolvedComponents = resolvableConfiguration.incoming.resolutionResult.allComponents

                    resolvedComponents.forEach { resolvedComponent ->
                        val resolvedVariants = resolvedComponent.variants

                        resolvedVariants.forEach { variant ->
                            val variantPath = { "$resolvableConfiguration -> $resolvedComponent -> $variant" }
                            val typedUsage = variant.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)
                            val stringUsage = variant.attributes.getAttribute(Attribute.of(Usage.USAGE_ATTRIBUTE.name, String::class.java))
                            val isKotlinCompilerClasspath = variant.attributes.keySet().isEmpty()
                            assert(
                                typedUsage != null || stringUsage != null || isKotlinCompilerClasspath,
                                variantPath
                            )
                            assert(
                                typedUsage != consumer.project.usageByName(KotlinUsages.KOTLIN_RESOURCES),
                                variantPath,
                            )
                            assert(
                                stringUsage != KotlinUsages.KOTLIN_RESOURCES,
                                variantPath,
                            )
                        }
                    }
                }
            }
        )
    }

    private fun testDirectDependencyOnResourcesProducer(
        producerTarget: TargetProvider,
        consumerTarget: TargetProvider,
        dependencyScope: DependencyScopeProvider = { ::implementation },
        resolutionStrategy: KotlinTargetResourcesResolutionStrategy,
        filterResolvedFiles: (Set<File>) -> Set<File> = { it },
        expectedResult: (consumer: Project, producer: Project) -> Set<File>,
    ) = directDependencyOnResourcesProducer(
        producerTarget = producerTarget,
        consumerTarget = consumerTarget,
        dependencyScope = dependencyScope,
        strategy = resolutionStrategy,
        assert = { consumer: Project, producer: Project ->
            assertEquals(
                expectedResult(consumer, producer),
                filterResolvedFiles(
                    resolutionStrategy.resourceArchives(
                        consumer.multiplatformExtension.consumerTarget().compilations.getByName("main"),
                    ).files
                )
            )
        }
    )

    private fun directDependencyOnResourcesProducer(
        producerTarget: TargetProvider,
        consumerTarget: TargetProvider,
        strategy: KotlinTargetResourcesResolutionStrategy,
        dependencyScope: DependencyScopeProvider = { ::implementation },
        assert: (consumer: Project, producer: Project) -> Unit,
    ) {
        val rootProject = buildProject()
        val producer = rootProject.createSubproject(
            "producer",
            resolutionStrategy = strategy,
        ) {
            kotlin { producerTarget() }
        }
        val consumer = rootProject.createSubproject(
            "consumer",
            resolutionStrategy = strategy,
        ) {
            kotlin {
                consumerTarget()
                sourceSets.commonMain {
                    dependencies {
                        dependencyScope()(project(":${producer.name}"))
                    }
                }
            }
        }

        listOf(rootProject, producer, consumer).forEach { it.evaluate() }
        producer.publishFakeResources(producer.multiplatformExtension.producerTarget())

        assert(consumer, producer)
    }

    private fun testTransitiveDependencyOnResourcesProducer(
        targetProvider: TargetProvider,
        resolutionStrategy: KotlinTargetResourcesResolutionStrategy,
        filterResolvedFiles: (Set<File>) -> Set<File> = { it },
        dependencyScope: DependencyScopeProvider = { ::implementation },
        middlePublishesResources: Boolean = false,
        consumerPublishesResources: Boolean = false,
        expectedResult: (consumer: Project, middle: Project, producer: Project) -> Set<File>,
    ) {
        val rootProject = buildProject()
        val producer = rootProject.createSubproject(
            "producer",
            resolutionStrategy = resolutionStrategy,
            preApplyCode = {
                enableMppResourcesPublication(true)
            }
        ) {
            kotlin { targetProvider() }
        }

        val middle = rootProject.createSubproject(
            "middle",
            resolutionStrategy = resolutionStrategy,
            preApplyCode = {
                enableMppResourcesPublication(middlePublishesResources)
            }
        ) {
            kotlin {
                targetProvider()
                sourceSets.commonMain {
                    dependencies {
                        dependencyScope()(dependencies.project(":${producer.name}"))
                    }
                }
            }
        }

        val consumer = rootProject.createSubproject(
            "consumer",
            resolutionStrategy = resolutionStrategy,
            preApplyCode = {
                enableMppResourcesPublication(consumerPublishesResources)
            }
        ) {
            kotlin {
                targetProvider()
                sourceSets.commonMain {
                    dependencies {
                        dependencyScope()(dependencies.project(":${middle.name}"))
                    }
                }
            }
        }

        listOf(rootProject, producer, middle, consumer).forEach { it.evaluate() }

        producer.publishFakeResources(producer.multiplatformExtension.targetProvider())
        if (middlePublishesResources) middle.publishFakeResources(middle.multiplatformExtension.targetProvider())
        if (consumerPublishesResources) consumer.publishFakeResources(consumer.multiplatformExtension.targetProvider())

        assertEquals(
            expectedResult(consumer, middle, producer),
            filterResolvedFiles(
                resolutionStrategy.resourceArchives(
                    consumer.multiplatformExtension.targetProvider().compilations.getByName("main"),
                ).files
            ),
        )
    }

    private fun Project.buildFile(path: String) = layout.buildDirectory.file(path).get().asFile

    private fun ProjectInternal.createSubproject(
        name: String,
        preApplyCode: Project.() -> Unit = { },
        resolutionStrategy: KotlinTargetResourcesResolutionStrategy,
        code: Project.() -> Unit = {},
    ) = buildProjectWithMPPAndStdlib(
        projectBuilder = {
            withParent(this@createSubproject)
            withName(name)
        },
        preApplyCode = {
            preApplyCode()
            setMppResourcesResolutionStrategy(resolutionStrategy)
        },
        code = code,
    )

    private fun buildProjectWithMPPAndStdlib(
        projectBuilder: ProjectBuilder.() -> Unit = { },
        preApplyCode: Project.() -> Unit = {},
        code: Project.() -> Unit = {},
    ) = buildProjectWithMPP(
        projectBuilder = projectBuilder,
        preApplyCode = preApplyCode,
    ) {
        enableDefaultStdlibDependency(true)
        repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()
        code()
    }

    private fun Project.publishFakeResources(target: KotlinTarget) {
        project.multiplatformExtension.resourcesPublicationExtension?.publishResourcesAsKotlinComponent(
            target,
            resourcePathForSourceSet = {
                KotlinTargetResourcesPublication.ResourceRoot(
                    project.provider { File(it.name) },
                    emptyList(),
                    emptyList(),
                )
            },
            relativeResourcePlacement = project.provider { File("test") },
        )
    }

}

private typealias TargetProvider = KotlinMultiplatformExtension.() -> (KotlinTarget)
private typealias DependencyScopeProvider = KotlinDependencyHandler.() -> ((Any) -> Dependency?)