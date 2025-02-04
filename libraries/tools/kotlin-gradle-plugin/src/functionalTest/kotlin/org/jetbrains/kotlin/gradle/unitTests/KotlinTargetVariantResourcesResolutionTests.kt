/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.KotlinTargetResourcesResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KotlinTargetVariantResourcesResolutionTests {

    @Test
    fun `test direct dependency - is the same - for all supported scopes`() {
        dependencyScopesWithResources().forEach { dependencyScope ->
            testDirectDependencyOnResourcesProducer(
                producerTarget = { linuxX64() },
                consumerTarget = { linuxX64() },
                dependencyScope = dependencyScope,
                expectedResult = { _, producer ->
                    hashSetOf(producer.buildFile("kotlin-multiplatform-resources/zip-for-publication/linuxX64/producer.kotlin_resources.zip"))
                },
            )
        }
    }

    @Test
    fun `test direct dependency - between different native targets - with artifact view`() {
        testDirectDependencyOnResourcesProducer(
            producerTarget = { linuxX64() },
            consumerTarget = { linuxArm64() },
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
    fun `test transitive dependency - without resources in middle project in wasm - with artifact view`() {
        dependencyScopesWithResources().forEach { dependencyScope ->
            testTransitiveDependencyOnResourcesProducer(
                targetProvider = { wasmJs() },
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
    fun `test resources configuration - only exists in projects with resources configuration strategy`() {
        val rootProject = buildProject()
        assertNull(
            rootProject.createSubproject(
                "variantReselection",
            ) { kotlin { linuxArm64() } }.configurations.findByName("linuxArm64ResourcesPath")
        )
    }

    private fun dependencyScopesWithResources(): List<DependencyScopeProvider> {
        return listOf(
            { this::implementation },
            { this::api },
        )
    }

    private fun testDirectDependencyOnResourcesProducer(
        producerTarget: TargetProvider,
        consumerTarget: TargetProvider,
        dependencyScope: DependencyScopeProvider = { ::implementation },
        filterResolvedFiles: (Set<File>) -> Set<File> = { it },
        expectedResult: (consumer: Project, producer: Project) -> Set<File>,
    ) = directDependencyOnResourcesProducer(
        producerTarget = producerTarget,
        consumerTarget = consumerTarget,
        dependencyScope = dependencyScope,
        assert = { consumer: Project, producer: Project ->
            assertEquals(
                expectedResult(consumer, producer),
                filterResolvedFiles(
                    KotlinTargetResourcesResolution.resourceArchives(
                        consumer.multiplatformExtension.consumerTarget().compilations.getByName("main"),
                    ).files
                )
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
        val producer = rootProject.createSubproject(
            "producer",
        ) {
            kotlin { producerTarget() }
        }
        val consumer = rootProject.createSubproject(
            "consumer",
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
        filterResolvedFiles: (Set<File>) -> Set<File> = { it },
        dependencyScope: DependencyScopeProvider = { ::implementation },
        middlePublishesResources: Boolean = false,
        consumerPublishesResources: Boolean = false,
        expectedResult: (consumer: Project, middle: Project, producer: Project) -> Set<File>,
    ) {
        val rootProject = buildProject()
        val producer = rootProject.createSubproject(
            "producer",
            preApplyCode = {
                enableMppResourcesPublication(true)
            }
        ) {
            kotlin { targetProvider() }
        }

        val middle = rootProject.createSubproject(
            "middle",
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
                KotlinTargetResourcesResolution.resourceArchives(
                    consumer.multiplatformExtension.targetProvider().compilations.getByName("main"),
                ).files
            ),
        )
    }

    private fun Project.buildFile(path: String) = layout.buildDirectory.file(path).get().asFile

    private fun ProjectInternal.createSubproject(
        name: String,
        preApplyCode: Project.() -> Unit = { },
        code: Project.() -> Unit = {},
    ) = buildProjectWithMPPAndStdlib(
        projectBuilder = {
            withParent(this@createSubproject)
            withName(name)
        },
        preApplyCode = {
            preApplyCode()
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