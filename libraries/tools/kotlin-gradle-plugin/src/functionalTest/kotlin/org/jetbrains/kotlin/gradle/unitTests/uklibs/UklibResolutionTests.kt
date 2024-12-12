/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.UklibResolutionStrategy
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.projectPathOrNull
import org.jetbrains.kotlin.utils.keysToMap
import kotlin.test.Test
import kotlin.test.assertEquals

class UklibResolutionTests {

    @Test
    fun `uklib resolution - from direct pom dependency with uklib packaging`() {
        val consumer = consumer(UklibResolutionStrategy.ResolveUklibsInMavenComponents) {
            jvm()
            iosArm64()
            iosX64()
            js()
            wasmJs()
            wasmWasi()

            sourceSets.commonMain.dependencies {
                implementation("foo.bar:pure-maven-uklib:1.0")
            }
        }

        val metadataCompilationVariants = consumer.multiplatformExtension.sourceSets.getByName("commonMain")
            .internal.resolvableMetadataConfiguration
            .resolveProjectDependencyComponentsWithArtifacts()
        val iosArm64CompilationVariants = consumer.multiplatformExtension.iosArm64().compilations.getByName("main")
            .configurations.compileDependencyConfiguration.resolveProjectDependencyComponentsWithArtifacts()
        val jvmRuntimeVariants = consumer.multiplatformExtension.jvm().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!.resolveProjectDependencyComponentsWithArtifacts()
        val jsRuntimeVariants = consumer.multiplatformExtension.js().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!.resolveProjectDependencyComponentsWithArtifacts()
        val wasmJsVariants = consumer.multiplatformExtension.wasmJs().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!.resolveProjectDependencyComponentsWithArtifacts()
        val wasmWasiVariants = consumer.multiplatformExtension.wasmWasi().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!.resolveProjectDependencyComponentsWithArtifacts()

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="commonMainResolvableDependenciesMetadata",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-uklib:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(jvmPomApiAttributes + uklibTransformationMetadataAttributes)
                ),
            ),
            metadataCompilationVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64CompileKlibraries",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-uklib:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(jvmPomApiAttributes + uklibTransformationIosArm64Attributes)
                ),
            ),
            iosArm64CompilationVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-uklib:1.0" to ResolvedComponentWithArtifacts(
                    configuration="runtime",
                    artifacts=mutableListOf(jvmPomRuntimeAttributes + uklibTransformationJvmAttributes)
                ),
            ),
            jvmRuntimeVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="jsRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-uklib:1.0" to ResolvedComponentWithArtifacts(
                    configuration="runtime",
                    artifacts=mutableListOf(jvmPomRuntimeAttributes + uklibTransformationJsAttributes)
                ),
            ),
            jsRuntimeVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="wasmJsRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-uklib:1.0" to ResolvedComponentWithArtifacts(
                    configuration="runtime",
                    artifacts=mutableListOf(jvmPomRuntimeAttributes + uklibTransformationWasmJsAttributes)
                ),
            ),
            wasmJsVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="wasmWasiRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-uklib:1.0" to ResolvedComponentWithArtifacts(
                    configuration="runtime",
                    artifacts=mutableListOf(jvmPomRuntimeAttributes + uklibTransformationWasmWasiAttributes)
                ),
            ),
            wasmWasiVariants
        )
    }

    @Test
    fun `uklib resolution - from direct pom dependency with uklib packaging points to untrasformed uklib - when uklibs are not allowed to resolve`() {
        val consumer = consumer(UklibResolutionStrategy.IgnoreUklibs) {
            jvm()
            iosArm64()
            iosX64()

            sourceSets.commonMain.dependencies {
                implementation("foo.bar:pure-maven-uklib:1.0")
            }
        }

        /**
         * FIXME: This is unfortunate and in opposition to how KMP dependencies resolve right now because:
         *
         * KMP dependencies will fail to resolve if they have a missing platform/native type attribute
         *
         * but this is similar to how pure java dependencies (e.g. com.google.guava:guava:+) will resolve just fine
         */

        val metadataCompilationVariants = consumer.multiplatformExtension.sourceSets.getByName("commonMain")
            .internal.resolvableMetadataConfiguration
            .resolveProjectDependencyComponentsWithArtifacts()
        val iosArm64CompilationVariants = consumer.multiplatformExtension.iosArm64().compilations.getByName("main")
            .configurations.compileDependencyConfiguration.resolveProjectDependencyComponentsWithArtifacts()
        val jvmRuntimeVariants = consumer.multiplatformExtension.jvm().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!.resolveProjectDependencyComponentsWithArtifacts()

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="commonMainResolvableDependenciesMetadata",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-uklib:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(jvmPomApiAttributes + uklibArtifact)
                ),
            ),
            metadataCompilationVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64CompileKlibraries",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-uklib:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(jvmPomApiAttributes + uklibArtifact)
                ),
            ),
            iosArm64CompilationVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-uklib:1.0" to ResolvedComponentWithArtifacts(
                    configuration="runtime",
                    artifacts=mutableListOf(jvmPomRuntimeAttributes + uklibArtifact)
                ),
            ),
            jvmRuntimeVariants
        )
    }

    // consumer <- pure jvm <- uklib <- PSM-only
    @Test
    fun `uklib resolution - from transitive uklib dependency`() {
        val consumer = consumer(UklibResolutionStrategy.ResolveUklibsInMavenComponents) {
            jvm()
            iosArm64()
            iosX64()

            sourceSets.commonMain.dependencies {
                implementation("foo.bar:pure-maven-jvm-with-trasitive-uklib-dependency:1.0")
            }
        }

        val metadataCompilationVariants = consumer.multiplatformExtension.sourceSets.getByName("commonMain")
            .internal.resolvableMetadataConfiguration
            .resolveProjectDependencyComponentsWithArtifacts()

        val iosArm64CompilationVariants = consumer.multiplatformExtension.iosArm64().compilations.getByName("main")
            .configurations.compileDependencyConfiguration.resolveProjectDependencyComponentsWithArtifacts()

        val jvmRuntimeVariants = consumer.multiplatformExtension.jvm().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!.resolveProjectDependencyComponentsWithArtifacts()

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="commonMainResolvableDependenciesMetadata",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-jvm-with-trasitive-uklib-dependency:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(jvmPomApiAttributes + jarArtifact)
                ),
                "foo.bar:pure-maven-uklib-with-transitive-non-uklib-dependency:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(jvmPomApiAttributes + uklibTransformationMetadataAttributes)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="metadataApiElements",
                    artifacts=mutableListOf(metadataVariantAttributes + releaseStatus)
                ),
            ),
            metadataCompilationVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-jvm-with-trasitive-uklib-dependency:1.0" to ResolvedComponentWithArtifacts(
                    configuration="runtime",
                    artifacts=mutableListOf(jvmPomRuntimeAttributes + jarArtifact)
                ),
                "foo.bar:pure-maven-uklib-with-transitive-non-uklib-dependency:1.0" to ResolvedComponentWithArtifacts(
                    configuration="runtime",
                    artifacts=mutableListOf(jvmPomRuntimeAttributes + uklibTransformationJvmAttributes)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-gradle-packaging-jvm:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf(platformJvmVariantAttributes + releaseStatus)
                ),
            ),
            jvmRuntimeVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64CompileKlibraries",
                    artifacts=mutableListOf()
                ),
                "foo.bar:pure-maven-jvm-with-trasitive-uklib-dependency:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(jvmPomApiAttributes + jarArtifact)
                ),
                "foo.bar:pure-maven-uklib-with-transitive-non-uklib-dependency:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(jvmPomApiAttributes + uklibTransformationIosArm64Attributes)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements-published",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-gradle-packaging-iosarm64:1.0" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements-published",
                    // klib + cinterop klib
                    artifacts=mutableListOf(
                        platformIosArm64Attributes + releaseStatus,
                        platformIosArm64Attributes + releaseStatus,
                    )
                ),
            ),
            iosArm64CompilationVariants
        )
    }

//    @Test
    fun `resolve uklib - in Swift Export and ObjC binaries export configurations`() {
        // FIXME: Request attributes in Swift Export and ObjC binary export configurations
    }

//    @Test
    fun `resolve uklib - in resolvable JS binaries`() {
        // FIXME: Similar to above but for JS
    }

    private fun consumer(
        strategy: UklibResolutionStrategy,
        configure: KotlinMultiplatformExtension.() -> Unit,
    ): Project {
        return buildProjectWithMPP(
            preApplyCode = {
                publishUklib()
                fakeUklibTransforms()
                setUklibResolutionStrategy(strategy)
                // Test stdlib in a separate test
                enableDefaultStdlibDependency(false)
                enableDefaultJsDomApiDependency(false)
            }
        ) {
            kotlin {
                configure()
            }

            repositories.maven(javaClass.getResource("/dependenciesResolution/UklibTests/repo")!!)
        }.evaluate()
    }

    private val uklibTransformationIosArm64Attributes = mapOf(
        "artifactType" to "uklib",
        "uklibTargetAttribute" to "ios_arm64",
        "uklibState" to "unzipped",
    )

    private val uklibTransformationJvmAttributes = mapOf(
        "artifactType" to "uklib",
        "uklibTargetAttribute" to "jvm",
        "uklibState" to "unzipped",
    )

    private val uklibTransformationMetadataAttributes = mapOf(
        "artifactType" to "uklib",
        "uklibTargetAttribute" to "common",
        "uklibState" to "unzipped",
    )

    private val uklibTransformationJsAttributes = mapOf(
        "artifactType" to "uklib",
        "uklibTargetAttribute" to "js_ir",
        "uklibState" to "unzipped",
    )

    private val uklibTransformationWasmJsAttributes = mapOf(
        "artifactType" to "uklib",
        "uklibTargetAttribute" to "wasm_js",
        "uklibState" to "unzipped",
    )

    private val uklibTransformationWasmWasiAttributes = mapOf(
        "artifactType" to "uklib",
        "uklibTargetAttribute" to "wasm_wasi",
        "uklibState" to "unzipped",
    )

    private val uklibVariantAttributes = mapOf(
        "org.gradle.category" to "library",
        "org.gradle.jvm.environment" to "???",
        "org.gradle.usage" to "kotlin-uklib",
        "org.jetbrains.kotlin.klib.packaging" to "packed",
        "org.jetbrains.kotlin.native.target" to "???",
        "org.jetbrains.kotlin.platform.type" to "unknown",
    )

    private val jvmPomRuntimeAttributes = mapOf(
        "org.gradle.category" to "library",
        "org.gradle.libraryelements" to "jar",
        "org.gradle.status" to "release",
        "org.gradle.usage" to "java-runtime",
    )

    private val jvmPomApiAttributes = mapOf(
        "org.gradle.category" to "library",
        "org.gradle.libraryelements" to "jar",
        "org.gradle.status" to "release",
        "org.gradle.usage" to "java-api",
    )

    private val platformJvmVariantAttributes = mapOf(
        "artifactType" to "jar",
        "org.gradle.category" to "library",
        "org.gradle.jvm.environment" to "standard-jvm",
        "org.gradle.libraryelements" to "jar",
        "org.gradle.usage" to "java-runtime",
        "org.jetbrains.kotlin.platform.type" to "jvm",
    )

    private val metadataVariantAttributes = mapOf(
        "artifactType" to "jar",
        "org.gradle.category" to "library",
        "org.gradle.jvm.environment" to "non-jvm",
        "org.gradle.libraryelements" to "jar",
        "org.gradle.usage" to "kotlin-metadata",
        "org.jetbrains.kotlin.platform.type" to "common",
    )

    private val releaseStatus = mapOf(
        "org.gradle.status" to "release",
    )

    // We only emit packing in secondary variants which are not published?
    private val nonPacked = mapOf(
        "org.jetbrains.kotlin.klib.packaging" to "non-packed",
    )

    private val jarArtifact = mapOf(
        "artifactType" to "jar",
    )

    private val uklibArtifact = mapOf(
        "artifactType" to "uklib",
    )

    private val platformIosArm64Attributes = mapOf(
        "artifactType" to "org.jetbrains.kotlin.klib",
        "org.gradle.category" to "library",
        "org.gradle.jvm.environment" to "non-jvm",
        "org.gradle.usage" to "kotlin-api",
        "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
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
            .map { artifact ->
                val uklibAttributes: List<Attribute<*>> = artifact.variant.attributes.keySet()
                    .sortedBy { it.name }
                ResolvedVariant(
                    artifact.variant.owner.projectPathOrNull ?: artifact.variant.owner.displayName,
                    uklibAttributes.keysToMap {
                        artifact.variant.attributes.getAttribute(it).toString()
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
            .map { component ->
                ResolvedComponent(
                    component.id.projectPathOrNull ?: component.id.displayName,
                    // Expect a single variant to always be selected?
                    component.variants.single().displayName
                )
            }
    }
}
