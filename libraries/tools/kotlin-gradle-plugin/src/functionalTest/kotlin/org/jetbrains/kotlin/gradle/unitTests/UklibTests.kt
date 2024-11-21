/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.artifacts.UklibResolutionStrategy
import org.jetbrains.kotlin.gradle.artifacts.uklibsPublication.UklibArchiveTask
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.projectPathOrNull
import org.jetbrains.kotlin.utils.keysToMap
import kotlin.test.Test
import kotlin.test.assertEquals

class UklibTests {

    @Test
    fun `prefer klib variant`() {
        val consumer = mixedCompilationsGraphConsumer(
            uklibResolutionStrategy = UklibResolutionStrategy.PreferPlatformSpecificVariant,
        )

        val iosArm64CompilationDependencies = consumer.multiplatformExtension.iosArm64().compilations.getByName("main")
            .configurations.compileDependencyConfiguration
        val iosArm64ResolvedVariants = iosArm64CompilationDependencies.resolveProjectDependencyComponentsWithArtifacts()

        val jvmRuntimeDependencies = consumer.multiplatformExtension.jvm().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!
        val jvmResolvedVariants = jvmRuntimeDependencies.resolveProjectDependencyComponentsWithArtifacts()

        assertEquals(
            mapOf(
                ":E_consumes_D" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                ":D_produces_uklib_consumes_C" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements",
                    artifacts=mutableListOf(platformJvmAttributes)
                ),
                ":C_produces_only_uklib_consumes_B" to ResolvedComponentWithArtifacts(
                    configuration="metadataUklibElements",
                    artifacts=mutableListOf(transformedJvmAttributes)
                ),
                ":B_produces_only_platform_variant_consumes_A" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements",
                    artifacts=mutableListOf(platformJvmAttributes)
                ),
                ":A_produces_uklib" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements",
                    artifacts=mutableListOf(platformJvmAttributes),
                )
            ),
            jvmResolvedVariants
        )

        assertEquals(
            mapOf(
                ":E_consumes_D" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64CompileKlibraries",
                    artifacts=mutableListOf()
                ),
                ":D_produces_uklib_consumes_C" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements",
                    artifacts=mutableListOf(platformIosArm64Attributes)
                ),
                ":C_produces_only_uklib_consumes_B" to ResolvedComponentWithArtifacts(
                    configuration="metadataUklibElements",
                    artifacts=mutableListOf(transformedIosArm64Attributes)
                ),
                ":B_produces_only_platform_variant_consumes_A" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements",
                    artifacts=mutableListOf(platformIosArm64Attributes)
                ),
                ":A_produces_uklib" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements",
                    artifacts=mutableListOf(platformIosArm64Attributes),
                )
            ),
            iosArm64ResolvedVariants
        )
    }

    @Test
    fun `prefer uklib variant`() {
        val consumer = mixedCompilationsGraphConsumer(
            uklibResolutionStrategy = UklibResolutionStrategy.PreferUklibVariant,
        )

        val iosArm64CompilationDependencies = consumer.multiplatformExtension.iosArm64().compilations.getByName("main")
            .configurations.compileDependencyConfiguration
        val iosArm64ResolvedVariants = iosArm64CompilationDependencies.resolveProjectDependencyComponentsWithArtifacts()

        val jvmRuntimeDependencies = consumer.multiplatformExtension.jvm().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!
        // FIXME: java-api variant doesn't resolve A
        val jvmResolvedVariants = jvmRuntimeDependencies.resolveProjectDependencyComponentsWithArtifacts()

        assertEquals(
            mapOf(
                ":E_consumes_D" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                ":D_produces_uklib_consumes_C" to ResolvedComponentWithArtifacts(
                    configuration="metadataUklibElements",
                    artifacts=mutableListOf(transformedJvmAttributes)
                ),
                ":C_produces_only_uklib_consumes_B" to ResolvedComponentWithArtifacts(
                    configuration="metadataUklibElements",
                    artifacts=mutableListOf(transformedJvmAttributes)
                ),
                ":B_produces_only_platform_variant_consumes_A" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements",
                    artifacts=mutableListOf(platformJvmAttributes)
                ),
                ":A_produces_uklib" to ResolvedComponentWithArtifacts(
                    configuration="metadataUklibElements",
                    artifacts=mutableListOf(transformedJvmAttributes),
                )
            ),
            jvmResolvedVariants
        )

        assertEquals(
            mapOf(
                ":E_consumes_D" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64CompileKlibraries",
                    artifacts=mutableListOf()
                ),
                ":D_produces_uklib_consumes_C" to ResolvedComponentWithArtifacts(
                    configuration="metadataUklibElements",
                    artifacts=mutableListOf(transformedIosArm64Attributes)
                ),
                ":C_produces_only_uklib_consumes_B" to ResolvedComponentWithArtifacts(
                    configuration="metadataUklibElements",
                    artifacts=mutableListOf(transformedIosArm64Attributes)
                ),
                ":B_produces_only_platform_variant_consumes_A" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements",
                    artifacts=mutableListOf(platformIosArm64Attributes)
                ),
                ":A_produces_uklib" to ResolvedComponentWithArtifacts(
                    configuration="metadataUklibElements",
                    artifacts=mutableListOf(transformedIosArm64Attributes),
                )
            ),
            iosArm64ResolvedVariants
        )
    }

    private fun mixedCompilationsGraphConsumer(
        uklibResolutionStrategy: UklibResolutionStrategy,
    ): Project {
        val root = buildProject()
        return root.child(
            "E_consumes_D",
            uklibResolutionStrategy = uklibResolutionStrategy,
            consume= root.child(
                "D_produces_uklib_consumes_C",
                consume = root.child(
                    "C_produces_only_uklib_consumes_B",
                    disablePlatformComponentReferences = true,
                    consume = root.child(
                        "B_produces_only_platform_variant_consumes_A",
                        publishUklibVariant = false,
                        consume = root.child(
                            "A_produces_uklib",
                            consume = null
                        )
                    )
                )
            )
        )
    }

    private val transformedJvmAttributes = mapOf(
        "org.gradle.usage" to "kotlin-uklib",
        "org.jetbrains.kotlin.klib.packaging" to "packed",
        "org.jetbrains.kotlin.native.target" to "???",
        "org.jetbrains.kotlin.platform.type" to "unknown",
        "uklibNativeSlice" to "unknown",
        "uklibPlatform" to "jvm",
        "uklibState" to "unzipped"
    )

    private val platformJvmAttributes = mapOf(
        "org.gradle.usage" to "java-runtime",
        "org.jetbrains.kotlin.platform.type" to "jvm",
    )

    private val transformedIosArm64Attributes = mapOf(
        "org.gradle.usage" to "kotlin-uklib",
        "org.jetbrains.kotlin.klib.packaging" to "packed",
        "org.jetbrains.kotlin.native.target" to "???",
        "org.jetbrains.kotlin.platform.type" to "unknown",
        "uklibNativeSlice" to "iosArm64",
        "uklibPlatform" to "native",
        "uklibState" to "unzipped"
    )

    private val platformIosArm64Attributes = mapOf(
        "org.gradle.usage" to "kotlin-api",
        "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
        "org.jetbrains.kotlin.klib.packaging" to "non-packed",
        "org.jetbrains.kotlin.native.target" to "ios_arm64",
        "org.jetbrains.kotlin.platform.type" to "native",
    )

    data class ResolvedComponentWithArtifacts(
        val configuration: String,
        val artifacts: MutableList<Map<String, String>> = mutableListOf(),
    )

    private fun Configuration.resolveProjectDependencyComponentsWithArtifacts(): Map<String, ResolvedComponentWithArtifacts> {
        val artifacts = resolveProjectDependencyVariantsFromArtifacts()
        val components = resolveProjectDependencyComponents()
        val componentToArtifacts = LinkedHashMap<String, ResolvedComponentWithArtifacts>()
        components.forEach { component ->
            if (componentToArtifacts[component.path] == null) {
                componentToArtifacts[component.path] = ResolvedComponentWithArtifacts(component.configuration)
            } else {
                error("${component} resolved multiple times?")
            }
        }
        artifacts.forEach { artifact ->
            componentToArtifacts[artifact.path]?.let {
                it.artifacts.add(artifact.attributes)
            } ?: error("Missing resolved component for artifact: ${artifact}")
        }
        return componentToArtifacts
    }

    data class ResolvedVariant(
        val path: String,
        val attributes: Map<String, String>,
    )

    private fun Configuration.resolveProjectDependencyVariantsFromArtifacts(): List<ResolvedVariant> {
        return incoming.artifacts.artifacts
            .filter {
                // Resolve only project components, so filter out stdlib and platform libraries in K/N
                it.variant.owner is ProjectComponentIdentifier
            }.map { artifact ->
                val uklibAttributes: List<Attribute<*>> = artifact.variant.attributes.keySet()
                    .filter { it.type == Usage::class.java || it.name.contains("uklib") || it.name.contains("kotlin") }
                    .sortedBy { it.name }
                ResolvedVariant(
                    artifact.variant.owner.projectPathOrNull!!,
                    uklibAttributes.keysToMap {
                        artifact.variant.attributes.getAttribute(it as Attribute<*>).toString()
                    }.mapKeys { it.key.name }
                )
            }
    }

    data class ResolvedComponent(
        val path: String,
        val configuration: String,
    )

    private fun Configuration.resolveProjectDependencyComponents(): List<ResolvedComponent> {
        return incoming.resolutionResult.allComponents
            .filter {
                it.id is ProjectComponentIdentifier
            }.map { component ->
                ResolvedComponent(
                    component.id.projectPathOrNull!!,
                    // Expect a single variant to always be selected?
                    component.variants.single().displayName
                )
            }
    }

    private fun Project.child(
        name: String,
        consume: Project?,
        publishUklibVariant: Boolean = true,
        disablePlatformComponentReferences: Boolean = false,
        uklibResolutionStrategy: UklibResolutionStrategy = UklibResolutionStrategy.ResolveOnlyPlatformSpecificVariant,
    ): Project {
        val parent = this
        return buildProjectWithMPP(
            preApplyCode = {
                if (publishUklibVariant) {
                    publishUklibVariant()
                }
                fakeUklibTransforms()
                setUklibResolutionStrategy(uklibResolutionStrategy)
                disablePlatformSpecificComponentReferences(disablePlatformComponentReferences)
            },
            projectBuilder = {
                withParent(parent)
                withName(name)
            }
        ) {
            kotlin {
                iosArm64()
                iosX64()
                jvm()

                if (consume != null) {
                    sourceSets.commonMain.dependencies {
                        implementation(project(consume.project.path))
                    }
                }
            }

            repositories.mavenLocal()
            repositories.mavenCentralCacheRedirector()
        }.evaluate()
    }

}