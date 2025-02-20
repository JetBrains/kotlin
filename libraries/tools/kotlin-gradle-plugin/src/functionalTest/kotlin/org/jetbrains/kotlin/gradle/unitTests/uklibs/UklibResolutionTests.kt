/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.Project
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalWasmDsl
class UklibResolutionTests {

    @Test
    fun `uklib resolution - from direct pom dependency with uklib packaging`() {
        val consumer = consumer(KmpResolutionStrategy.ResolveUklibsAndResolvePSMLeniently) {
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
                    artifacts=mutableListOf(jvmApiAttributes + uklibTransformationMetadataAttributes)
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
                    artifacts=mutableListOf(jvmApiAttributes + uklibTransformationIosArm64Attributes)
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
                    artifacts=mutableListOf(jvmRuntimeAttributes + uklibTransformationJvmAttributes)
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
                    artifacts=mutableListOf(jvmRuntimeAttributes + uklibTransformationJsAttributes)
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
                    artifacts=mutableListOf(jvmRuntimeAttributes + uklibTransformationWasmJsAttributes)
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
                    artifacts=mutableListOf(jvmRuntimeAttributes + uklibTransformationWasmWasiAttributes)
                ),
            ),
            wasmWasiVariants
        )
    }

    @Test
    fun `uklib resolution - from direct pom dependency with uklib packaging points to untrasformed uklib - when uklibs are not allowed to resolve`() {
        val consumer = consumer(KmpResolutionStrategy.StandardKMPResolution) {
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
                    artifacts=mutableListOf(jvmApiAttributes + uklibArtifact)
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
                    artifacts=mutableListOf(jvmApiAttributes + uklibArtifact)
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
                    artifacts=mutableListOf(jvmRuntimeAttributes + uklibArtifact)
                ),
            ),
            jvmRuntimeVariants
        )
    }

    // consumer <- pure jvm <- uklib <- PSM-only
    @Test
    fun `uklib resolution - from transitive uklib dependency`() {
        val consumer = consumer(KmpResolutionStrategy.ResolveUklibsAndResolvePSMLeniently) {
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
                    artifacts=mutableListOf(jvmApiAttributes + jarArtifact)
                ),
                "foo.bar:pure-maven-uklib-with-transitive-non-uklib-dependency:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(jvmApiAttributes + uklibTransformationMetadataAttributes)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="metadataApiElements",
                    artifacts=mutableListOf(kmpMetadataVariantAttributes + releaseStatus)
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
                    artifacts=mutableListOf(jvmRuntimeAttributes + jarArtifact)
                ),
                "foo.bar:pure-maven-uklib-with-transitive-non-uklib-dependency:1.0" to ResolvedComponentWithArtifacts(
                    configuration="runtime",
                    artifacts=mutableListOf(jvmRuntimeAttributes + uklibTransformationJvmAttributes)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-gradle-packaging-jvm:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf(kmpJvmRuntimeVariantAttributes + releaseStatus)
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
                    artifacts=mutableListOf(jvmApiAttributes + jarArtifact)
                ),
                "foo.bar:pure-maven-uklib-with-transitive-non-uklib-dependency:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(jvmApiAttributes + uklibTransformationIosArm64Attributes)
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

    //    @Test
    fun `resolve uklib - metadata resolvable configurations`() {
        // FIXME: Similar to above but for JS
    }

    private fun consumer(
        strategy: KmpResolutionStrategy,
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
}
