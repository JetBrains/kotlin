/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication.resourcesVariantViewFromCompileDependencyConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class KotlinTargetVariantResourcesResolutionTests {

    @Test
    fun `test direct dependency - is the same - for all resolution methods and supported scopes`() {
        resolutionMethods().forEach { resolutionMethod ->
            dependencyScopesWithResources().forEach { dependencyScope ->
                testDirectDependencyOnResourcesProducer(
                    producerTarget = { linuxX64() },
                    consumerTarget = { linuxX64() },
                    dependencyScope = dependencyScope,
                    resolutionMethod = resolutionMethod,
                    expectedResult = { _, producer ->
                        hashSetOf(producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.zip"))
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
            resolutionMethod = { resolveResourcesWithVariantReselectionFiles() },
            expectedResult = { _, _ -> emptySet() },
        )
    }

    @Test
    fun `test direct dependency - between different native targets - with resources configuration`() {
        testDirectDependencyOnResourcesProducer(
            producerTarget = { linuxX64() },
            consumerTarget = { linuxArm64() },
            resolutionMethod = { resolveResourcesWithResourcesConfigurationFiles(lenient = true) },
            expectedResult = { _, _ -> emptySet() },
        )
    }

    @Test
    fun `test direct dependency - for wasmJs and wasmWasi targets - when using artifact view`() {
        listOf<TargetProvider>(
            { wasmJs() },
            { wasmWasi() },
        ).forEach { target ->
            testDirectDependencyOnResourcesProducer(
                producerTarget = { target() },
                consumerTarget = { target() },
                resolutionMethod = {
                    resolveResourcesWithVariantReselectionFiles().filterNot {
                        it.path.contains("org/jetbrains/kotlin/kotlin-stdlib")
                    }.toSet()
                },
                expectedResult = { _, producer ->
                    hashSetOf(
                        producer.buildFile(
                            "kotlin-multiplatform-resources/zip-for-publication/${producer.multiplatformExtension.target().name}/producer.zip"
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
            resolutionMethod = {
                resolveResourcesWithResourcesConfigurationFiles().filterNot {
                    it.path.contains("org/jetbrains/kotlin/kotlin-stdlib")
                }.toSet()
            },
            expectedResult = { consumer, producer ->
                hashSetOf(
                    producer.buildFile(
                        "kotlin-multiplatform-resources/zip-for-publication/wasmJs/producer.zip"
                    ),
                    // ???
                    consumer.buildFile(
                        "classes/kotlin/wasmJs/main"
                    )
                )
            },
        )
    }

    @Test
    fun `test direct dependency - for wasmWasi - when using resources configuration`() {
        testDirectDependencyOnResourcesProducer(
            producerTarget = { wasmWasi() },
            consumerTarget = { wasmWasi() },
            resolutionMethod = {
                resolveResourcesWithResourcesConfigurationFiles().filterNot {
                    it.path.contains("org/jetbrains/kotlin/kotlin-stdlib")
                }.toSet()
            },
            expectedResult = { consumer, producer ->
                hashSetOf(
                    producer.buildFile(
                        "kotlin-multiplatform-resources/zip-for-publication/wasmWasi/producer.zip"
                    ),
                    // ???
                    consumer.buildFile(
                        "classes/kotlin/wasmWasi/main"
                    )
                )
            },
        )
    }

    @Test
    fun `test transitive dependency - without resources in middle project - with configuration`() {
        testTransitiveDependencyOnResourcesProducer(
            resolutionMethod = { resolveResourcesWithResourcesConfigurationFiles() },
            expectedResult = { consumer, middle, producer ->
                setOf(
                    producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.zip"),
                    middle.buildFile("classes/kotlin/linuxX64/main/klib/withoutResources.klib"),
                )
            }
        )
        // Resources can be filtered by classifier
        testTransitiveDependencyOnResourcesProducer(
            resolutionMethod = {
                resolveResourcesWithResourcesConfiguration().resolvedConfiguration.resolvedArtifacts.filter {
                    it.classifier == "kotlin_resources"
                }.map { it.file }.toSet()
            },
            expectedResult = { consumer, middle, producer ->
                setOf(
                    producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.zip"),
                )
            }
        )
    }

    @Test
    fun `test transitive dependency - without resources in middle project - with artifact view`() {
        testTransitiveDependencyOnResourcesProducer(
            resolutionMethod = { resolveResourcesWithVariantReselectionFiles() },
            expectedResult = { consumer, middle, producer ->
                setOf(
                    producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.zip"),
                    middle.buildFile("classes/kotlin/linuxX64/main/klib/withoutResources.klib"),
                )
            }
        )
        // Resources can be filtered by attribute, since artifactView doesn't have access to classifier
        testTransitiveDependencyOnResourcesProducer(
            resolutionMethod = {
                resolveResourcesWithVariantReselection().artifacts.filter {
                    it.variant.attributes.getAttribute(
                        Usage.USAGE_ATTRIBUTE
                    ) == project.usageByName(KotlinUsages.KOTLIN_RESOURCES)
                }.map { it.file }.toSet()
            },
            expectedResult = { consumer, middle, producer ->
                setOf(
                    producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.zip"),
                )
            }
        )
    }

    @Test
    fun `test resources - don't leak into non-resources configurations`() {
        resolutionSanityCheck()

        val targetsToTest = listOf<TargetProvider>(
            { wasmJs() },
            { wasmWasi() },
            { linuxX64() },
            { iosArm64() },
        )
        targetsToTest.indices.forEach { index ->
            // Test when target is matching
            testNonResourcesConfigurationDontResolveResourceVariants(
                producerTarget = targetsToTest[index],
                consumerTarget = targetsToTest[index],
            )

            // Test when target is not matching
            testNonResourcesConfigurationDontResolveResourceVariants(
                producerTarget = targetsToTest[index],
                consumerTarget = targetsToTest[(index + 1) % targetsToTest.count()],
            )
        }
    }

    private fun resolutionMethods(): List<ResolutionMethod> {
        return listOf(
            { resolveResourcesWithResourcesConfigurationFiles() },
            { resolveResourcesWithVariantReselectionFiles() },
        )
    }

    private fun dependencyScopesWithResources(): List<DependencyScopeProvider> {
        return listOf(
            { this::implementation },
            { this::api },
            { this::compileOnly },
            // { this::runtimeOnly }, ???
        )
    }

    private fun resolutionSanityCheck() {
        directDependencyOnResourcesProducer(
            producerTarget = { linuxX64() },
            consumerTarget = { linuxX64() },
            assert = { consumer, producer ->
                val resourcesConfiguration = consumer.multiplatformExtension.linuxX64()
                    .compilations.getByName("main")
                    .internal.configurations.resourcesConfiguration

                assertEquals(
                    mapOf(
                        // linuxX64ResourcesPath is the root resolvable configuration for the resolution
                        "project :consumer" to listOf("linuxX64ResourcesPath"),
                        // stdlib doesn't have resources, so apiElements is selected as per compatibility rule
                        "org.jetbrains.kotlin:kotlin-stdlib:2.0.255-SNAPSHOT" to listOf("nativeApiElements"),
                        // producer provides the consumable configuration with resources for the consumer
                        "project :producer" to listOf("linuxX64ResourcesElements"),
                    ),
                    resourcesConfiguration.incoming.resolutionResult.allComponents.flatMap { resolvedComponent ->
                        resolvedComponent.variants
                    }.groupBy(
                        keySelector = { it.owner.displayName },
                        valueTransform = { it.displayName },
                    )
                )
            }
        )
    }

    private fun testNonResourcesConfigurationDontResolveResourceVariants(
        producerTarget: TargetProvider,
        consumerTarget: TargetProvider,
    ) {
        directDependencyOnResourcesProducer(
            producerTarget = producerTarget,
            consumerTarget = consumerTarget,
            assert = { consumer, producer ->
                val resourcesConfigurations = consumer.multiplatformExtension.targets.map {
                    it.compilations.getByName("main").internal.configurations.resourcesConfiguration
                }
                assert(resourcesConfigurations.isNotEmpty())

                val nonResourcesConfigurations: Set<Configuration> = consumer.configurations.filter {
                    it.isCanBeResolved
                }.toHashSet() - resourcesConfigurations
                assert(nonResourcesConfigurations.isNotEmpty())

                nonResourcesConfigurations.forEach { resolvableConfiguration ->
                    val resolvedComponents = resolvableConfiguration.incoming.resolutionResult.allComponents

                    resolvedComponents.forEach { resolvedComponent ->
                        val resolvedVariants = resolvedComponent.variants

                        resolvedVariants.forEach { variant ->
                            // Getting usage properly doesn't work here in this API
                            // variant.attributes.getAttribute(Usage.USAGE_ATTRIBUTE) is always null
                            val dumpAttributesToString = variant.attributes.toString()
                            val isWeirdToolingConfiguration = dumpAttributesToString == "{}"
                            val variantPath = { "$resolvableConfiguration -> $resolvedComponent -> $variant" }
                            assert(
                                dumpAttributesToString.contains("org.gradle.usage=") || isWeirdToolingConfiguration,
                                variantPath,
                            )
                            assert(
                                !dumpAttributesToString.contains("org.gradle.usage=${KotlinUsages.KOTLIN_RESOURCES}"),
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
        resolutionMethod: ResolutionMethod,
        expectedResult: (consumer: Project, producer: Project) -> Set<File>,
    ) = directDependencyOnResourcesProducer(
        producerTarget = producerTarget,
        consumerTarget = consumerTarget,
        dependencyScope = dependencyScope,
        assert = { consumer: Project, producer: Project ->
            assertEquals(
                expectedResult(consumer, producer),
                consumer.multiplatformExtension.consumerTarget().resolutionMethod(),
            )
        }
    )

    private fun directDependencyOnResourcesProducer(
        producerTarget: TargetProvider,
        consumerTarget: TargetProvider,
        dependencyScope: DependencyScopeProvider = { ::implementation },
        assert: (consumer: Project, producer: Project) -> Unit,
    ) {
        val rootProject = buildProject()
        val producer = rootProject.createSubproject("producer") {
            kotlin { producerTarget() }
            enableMppResourcesPublication(true)
        }
        val consumer = rootProject.createSubproject("consumer") {
            kotlin {
                consumerTarget()
                sourceSets.commonMain {
                    dependencies {
                        dependencyScope()(project(":${producer.name}"))
                    }
                }
            }
            enableMppResourcesPublication(false)
        }

        listOf(rootProject, producer, consumer).forEach { it.evaluate() }
        producer.publishFakeResources(producer.multiplatformExtension.producerTarget())

        assert(consumer, producer)
    }

    private fun testTransitiveDependencyOnResourcesProducer(
        resolutionMethod: ResolutionMethod,
        expectedResult: (consumer: Project, middle: Project, producer: Project) -> Set<File>,
    ) {
        val rootProject = buildProject()
        val producer = rootProject.createSubproject("producer") {
            kotlin { linuxX64() }
            enableMppResourcesPublication(true)
        }

        val middle = rootProject.createSubproject("withoutResources") {
            kotlin {
                linuxX64()
                sourceSets.commonMain {
                    dependencies {
                        implementation(dependencies.project(":${producer.name}"))
                    }
                }
            }
            enableMppResourcesPublication(false)
        }

        val consumer = rootProject.createSubproject("consumer") {
            kotlin {
                linuxX64()
                sourceSets.commonMain {
                    dependencies {
                        implementation(dependencies.project(":${middle.name}"))
                    }
                }
                enableMppResourcesPublication(false)
            }
        }

        listOf(rootProject, producer, middle, consumer).forEach { it.evaluate() }

        producer.publishFakeResources(producer.multiplatformExtension.linuxX64())

        assertEquals(
            expectedResult(consumer, middle, producer),
            consumer.multiplatformExtension.linuxX64().resolutionMethod(),
        )
    }

    private fun Project.buildFile(path: String) = layout.buildDirectory.file(path).get().asFile

    private fun ProjectInternal.createSubproject(
        name: String,
        code: Project.() -> Unit = {},
    ) = buildProjectWithMPPAndStdlib(
        projectBuilder = {
            withParent(this@createSubproject)
            withName(name)
        },
        code = code,
    )

    private fun buildProjectWithMPPAndStdlib(
        projectBuilder: ProjectBuilder.() -> Unit = { },
        code: Project.() -> Unit = {},
    ) = buildProjectWithMPP(
        projectBuilder = projectBuilder
    ) {
        enableDependencyVerification(false)
        enableDefaultStdlibDependency(true)
        repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()
        code()
    }

    private fun KotlinTarget.resolveResourcesWithVariantReselection() = compilations.getByName("main")
        .resourcesVariantViewFromCompileDependencyConfiguration { lenient(true) }

    private fun KotlinTarget.resolveResourcesWithVariantReselectionFiles(): Set<File> = resolveResourcesWithVariantReselection()
        .files.toSet()

    private fun KotlinTarget.resolveResourcesWithResourcesConfiguration() =
        compilations.getByName("main").internal.configurations.resourcesConfiguration

    private fun KotlinTarget.resolveResourcesWithResourcesConfigurationFiles(
        lenient: Boolean = false
    ): Set<File> = resolveResourcesWithResourcesConfiguration().incoming.artifactView { it.isLenient = lenient }.files.toSet()

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
private typealias ResolutionMethod = KotlinTarget.() -> Set<File>
private typealias DependencyScopeProvider = KotlinDependencyHandler.() -> ((Any) -> Dependency?)