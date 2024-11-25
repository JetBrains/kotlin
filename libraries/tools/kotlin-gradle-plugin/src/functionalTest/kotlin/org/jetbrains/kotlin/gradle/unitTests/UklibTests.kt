/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.artifacts.UklibResolutionStrategy
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.projectPathOrNull
import org.jetbrains.kotlin.utils.keysToMap
import kotlin.test.Test
import kotlin.test.assertEquals

class UklibTests {

    @Test
    fun `resolve uklib - from pom with uklib packaging`() {
        val consumer = consumer(UklibResolutionStrategy.PreferPlatformSpecificVariant) {
            sourceSets.commonMain.dependencies {
                implementation("foo.bar:uklib-maven-uklib-packaging:1.0")
            }
        }

        val iosArm64CompilationDependencies = consumer.multiplatformExtension.iosArm64().compilations.getByName("main")
            .configurations.compileDependencyConfiguration
        val iosArm64ResolvedVariants = iosArm64CompilationDependencies.resolveProjectDependencyComponentsWithArtifacts()

        val jvmRuntimeDependencies = consumer.multiplatformExtension.jvm().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!
        val jvmResolvedVariants = jvmRuntimeDependencies.resolveProjectDependencyComponentsWithArtifacts()

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-uklib-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="runtime",
                    artifacts=mutableListOf(transformedJvmAttributesFromPom)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-gradle-packaging-jvm:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf(platformJvmAttributes + releaseStatus)
                ),
            ),
            jvmResolvedVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64CompileKlibraries",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-uklib-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(transformedIosArm64AttributesFromPom)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements-published",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-gradle-packaging-iosarm64:1.0" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements-published",
                    artifacts=mutableListOf(
                        platformIosArm64Attributes + releaseStatus,
                        platformIosArm64Attributes + releaseStatus,
                    )
                ),
            ),
            iosArm64ResolvedVariants
        )
    }

    @Test
    fun `resolve uklib - from pom with jar packaging - with explicit extension`() {
        val consumer = consumer(UklibResolutionStrategy.PreferPlatformSpecificVariant) {
            sourceSets.commonMain.dependencies {
                implementation("foo.bar:uklib-maven-jar-packaging:1.0@uklib") {
                    assert(!isTransitive)
                    isTransitive = true
                }
            }
        }

        val iosArm64CompilationDependencies = consumer.multiplatformExtension.iosArm64().compilations.getByName("main")
            .configurations.compileDependencyConfiguration
        val iosArm64ResolvedVariants = iosArm64CompilationDependencies.resolveProjectDependencyComponentsWithArtifacts()

        val jvmRuntimeDependencies = consumer.multiplatformExtension.jvm().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!
        val jvmResolvedVariants = jvmRuntimeDependencies.resolveProjectDependencyComponentsWithArtifacts()

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-jar-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="runtime",
                    artifacts=mutableListOf(finalUklibTransformationJvmAttributes + releaseStatus)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-gradle-packaging-jvm:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf(platformJvmAttributes + releaseStatus)
                ),
            ),
            jvmResolvedVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64CompileKlibraries",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-jar-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(finalUklibTransformationIosArm64Attributes + releaseStatus)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements-published",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-gradle-packaging-iosarm64:1.0" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements-published",
                    artifacts=mutableListOf(
                        platformIosArm64Attributes + releaseStatus,
                        platformIosArm64Attributes + releaseStatus,
                    )
                ),
            ),
            iosArm64ResolvedVariants
        )
    }


    // FIXME: Maybe this is usable?
//        consumer.configurations.configureEach {
//            if (it.isCanBeResolved) {
//                it.resolutionStrategy.eachDependency {
//                    it.artifactSelection {
//
//                    }
//                }
//                it.resolutionStrategy.componentSelection.all {
//                    it.
//                }
//            }
//        }
    @Test
    fun `resolve uklib - from pom with jar packaging - with explicit extension - and component rule`() {
        val consumer = consumer(UklibResolutionStrategy.PreferUklibVariant) {
            sourceSets.commonMain.dependencies {
                // implementation("foo.bar:uklib-maven-jar-packaging:1.0")
                // consume "foo.bar:uklib-maven-jar-packaging:1.0" transitively
                // FIXME: With a naive component metadata rule regular jars fail to resolve
                implementation("foo.bar:regular-maven-jar-packaging:1.0")
            }

            project.dependencies.components.all { component ->
                component.maybeAddVariant(
                    "uklib",
                    // FIXME: runtime?
                    "compile",
                ) {
                    // How do you know that the file will be there?
                    it.withFiles {
                        // When there are multiple artifacts, Gradle doesn't set artifactType
                        it.removeAllFiles()
                        it.addFile("${component.id.name}-${component.id.version}.uklib")
                    }
                    it.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(KotlinUsages.KOTLIN_UKLIB))
                }
            }
        }

        val iosArm64CompilationDependencies = consumer.multiplatformExtension.iosArm64().compilations.getByName("main")
            .configurations.compileDependencyConfiguration
        val iosArm64ResolvedVariants = iosArm64CompilationDependencies.resolveProjectDependencyComponentsWithArtifacts()

        val jvmRuntimeDependencies = consumer.multiplatformExtension.jvm().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!
        val jvmResolvedVariants = jvmRuntimeDependencies.resolveProjectDependencyComponentsWithArtifacts()

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-jar-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="uklib",
                    artifacts=mutableListOf(transformedIosArm64AttributesFromPom)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-gradle-packaging-jvm:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf(platformJvmAttributes + releaseStatus)
                ),
            ),
            jvmResolvedVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64CompileKlibraries",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-jar-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(finalUklibTransformationIosArm64Attributes + releaseStatus)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements-published",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-gradle-packaging-iosarm64:1.0" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements-published",
                    artifacts=mutableListOf(
                        platformIosArm64Attributes + releaseStatus,
                        platformIosArm64Attributes + releaseStatus,
                    )
                ),
            ),
            iosArm64ResolvedVariants
        )
    }

    // @Test
    // FIXME: This is probably pretty useless because with the classifier artifactType has to be jar and its unclear how to force a transform
    fun `resolve uklib - from pom with jar packaging - with explicit classifier`() {
        val consumer = consumer(UklibResolutionStrategy.PreferPlatformSpecificVariant) {
            sourceSets.commonMain.dependencies {
                implementation("foo.bar:uklib-maven-jar-packaging-classifier:1.0:uklib")
            }
        }

        val iosArm64CompilationDependencies = consumer.multiplatformExtension.iosArm64().compilations.getByName("main")
            .configurations.compileDependencyConfiguration
        val iosArm64ResolvedVariants = iosArm64CompilationDependencies.resolveProjectDependencyComponentsWithArtifacts()

        val jvmRuntimeDependencies = consumer.multiplatformExtension.jvm().compilations.getByName("main")
            .configurations.runtimeDependencyConfiguration!!
        val jvmResolvedVariants = jvmRuntimeDependencies.resolveProjectDependencyComponentsWithArtifacts()

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeClasspath",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-jar-packaging-classifier:1.0" to ResolvedComponentWithArtifacts(
                    configuration="runtime",
                    artifacts=mutableListOf(transformedJvmAttributesFromPom)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-gradle-packaging-jvm:1.0" to ResolvedComponentWithArtifacts(
                    configuration="jvmRuntimeElements-published",
                    artifacts=mutableListOf(platformJvmAttributes + releaseStatus)
                ),
            ),
            jvmResolvedVariants
        )

        assertEquals(
            mapOf(
                ":" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64CompileKlibraries",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-jar-packaging-classifier:1.0" to ResolvedComponentWithArtifacts(
                    configuration="compile",
                    artifacts=mutableListOf(transformedIosArm64AttributesFromPom)
                ),
                "foo.bar:uklib-maven-gradle-packaging:1.0" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements-published",
                    artifacts=mutableListOf()
                ),
                "foo.bar:uklib-maven-gradle-packaging-iosarm64:1.0" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements-published",
                    artifacts=mutableListOf(
                        platformIosArm64Attributes + releaseStatus,
                        platformIosArm64Attributes + releaseStatus,
                    )
                ),
            ),
            iosArm64ResolvedVariants
        )
    }

    private fun consumer(
        strategy: UklibResolutionStrategy,
        configure: KotlinMultiplatformExtension.() -> Unit,
    ): Project {
        return buildProjectWithMPP(
            preApplyCode = {
                publishUklibVariant()
                fakeUklibTransforms()
                setUklibResolutionStrategy(strategy)
                // Test stdlib in a separate test
                enableDefaultStdlibDependency(false)
            }
        ) {
            kotlin {
                iosArm64()
                iosX64()
                jvm()

                configure()
            }

            repositories.maven(javaClass.getResource("/dependenciesResolution/UklibTests/repo")!!)
        }.evaluate()
    }

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
                    artifacts=mutableListOf(platformIosArm64Attributes + nonPacked)
                ),
                ":C_produces_only_uklib_consumes_B" to ResolvedComponentWithArtifacts(
                    configuration="metadataUklibElements",
                    artifacts=mutableListOf(transformedIosArm64Attributes)
                ),
                ":B_produces_only_platform_variant_consumes_A" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements",
                    artifacts=mutableListOf(platformIosArm64Attributes + nonPacked)
                ),
                ":A_produces_uklib" to ResolvedComponentWithArtifacts(
                    configuration="iosArm64ApiElements",
                    artifacts=mutableListOf(platformIosArm64Attributes + nonPacked),
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
                    artifacts=mutableListOf(platformIosArm64Attributes + nonPacked)
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
        "artifactType" to "uklib",
        "org.gradle.category" to "library",
        "org.gradle.jvm.environment" to "???",
        "org.gradle.usage" to "kotlin-uklib",
        "org.jetbrains.kotlin.klib.packaging" to "packed",
        "org.jetbrains.kotlin.native.target" to "???",
        "org.jetbrains.kotlin.platform.type" to "unknown",
        "uklibNativeSlice" to "unknown",
        "uklibPlatform" to "jvm",
        "uklibState" to "unzipped",
    )

    private val transformedJvmAttributesFromPom = mapOf(
        "artifactType" to "uklib",
        "org.gradle.category" to "library",
        "org.gradle.libraryelements" to "jar",
        "org.gradle.status" to "release",
        "org.gradle.usage" to "java-runtime",
        "uklibNativeSlice" to "unknown",
        "uklibPlatform" to "jvm",
        "uklibState" to "unzipped",
    )

    private val platformJvmAttributes = mapOf(
        "artifactType" to "jar",
        "org.gradle.category" to "library",
        "org.gradle.jvm.environment" to "standard-jvm",
        "org.gradle.libraryelements" to "jar",
        "org.gradle.usage" to "java-runtime",
        "org.jetbrains.kotlin.platform.type" to "jvm",
    )

    private val releaseStatus = mapOf(
        "org.gradle.status" to "release",
    )

    private val nonPacked = mapOf(
        "org.jetbrains.kotlin.klib.packaging" to "non-packed",
    )

    private val defaultGradleJvmAttributes = mapOf(
        "artifactType" to "jar",
        "org.gradle.category" to "library",
        "org.gradle.libraryelements" to "jar",
        "org.gradle.usage" to "java-runtime",
    ) + releaseStatus

    private val transformedIosArm64Attributes = mapOf(
        "artifactType" to "uklib",
        "org.gradle.category" to "library",
        "org.gradle.jvm.environment" to "???",
        "org.gradle.usage" to "kotlin-uklib",
        "org.jetbrains.kotlin.klib.packaging" to "packed",
        "org.jetbrains.kotlin.native.target" to "???",
        "org.jetbrains.kotlin.platform.type" to "unknown",
        "uklibNativeSlice" to "iosArm64",
        "uklibPlatform" to "native",
        "uklibState" to "unzipped",
    )

    private val transformedIosArm64AttributesFromPom = mapOf(
        "artifactType" to "uklib",
        "org.gradle.category" to "library",
        "org.gradle.libraryelements" to "jar",
        "org.gradle.status" to "release",
        "org.gradle.usage" to "java-api",
        "uklibNativeSlice" to "iosArm64",
        "uklibPlatform" to "native",
        "uklibState" to "unzipped",
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

    private val finalUklibTransformationIosArm64Attributes = mapOf(
        "artifactType" to "uklib",
        "uklibNativeSlice" to "iosArm64",
        "uklibPlatform" to "native",
        "uklibState" to "unzipped",
    )

    private val finalUklibTransformationJvmAttributes = mapOf(
        "artifactType" to "uklib",
        "uklibNativeSlice" to "unknown",
        "uklibPlatform" to "jvm",
        "uklibState" to "unzipped",
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
                !listOf(
                    it.id.displayName.contains("stdlib"),
                ).any { it }
            }.map { artifact ->
                val uklibAttributes: List<Attribute<*>> = artifact.variant.attributes.keySet()
                    .sortedBy { it.name }
                ResolvedVariant(
                    artifact.variant.owner.projectPathOrNull ?: artifact.variant.owner.displayName,
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
                !listOf(
                    it.id.displayName.contains("stdlib"),
                ).any { it }
            }.map { component ->
                ResolvedComponent(
                    component.id.projectPathOrNull ?: component.id.displayName,
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
                // Test stdlib in a separate test
                enableDefaultStdlibDependency(false)
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
        }.evaluate()
    }

}
