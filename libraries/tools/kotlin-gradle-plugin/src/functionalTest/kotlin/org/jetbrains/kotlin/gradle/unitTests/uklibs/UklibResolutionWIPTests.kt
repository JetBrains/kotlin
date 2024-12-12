///*
// * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
// * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
// */
//
//package org.jetbrains.kotlin.gradle.unitTests.uklibs
//
//import org.gradle.api.Project
//import org.jetbrains.kotlin.gradle.artifacts.UklibResolutionStrategy
//import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
//import org.jetbrains.kotlin.gradle.unitTests.uklibs.UklibResolutionTests.ResolvedComponentWithArtifacts
//import org.jetbrains.kotlin.gradle.util.*
//import org.jetbrains.kotlin.gradle.util.setUklibResolutionStrategy
//import kotlin.reflect.full.memberProperties
//import kotlin.test.assertEquals
//
//class UklibResolutionWIPTests {
//
//    // @Test
//    fun `prefer klib variant`() {
//        val consumer = mixedCompilationsGraphConsumer(
//            uklibResolutionStrategy = UklibResolutionStrategy.AllowResolvingUklibs,
//        )
//
//        val iosArm64CompilationDependencies = consumer.multiplatformExtension.iosArm64().compilations.getByName("main")
//            .configurations.compileDependencyConfiguration
//        val iosArm64ResolvedVariants = iosArm64CompilationDependencies.resolveProjectDependencyComponentsWithArtifacts()
//
//        val jvmRuntimeDependencies = consumer.multiplatformExtension.jvm().compilations.getByName("main")
//            .configurations.runtimeDependencyConfiguration!!
//        val jvmResolvedVariants = jvmRuntimeDependencies.resolveProjectDependencyComponentsWithArtifacts()
//
//        assertEquals(
//            mapOf(
//                ":E_consumes_D" to ResolvedComponentWithArtifacts(
//                    configuration="jvmRuntimeClasspath",
//                    artifacts=mutableListOf()
//                ),
//                ":D_produces_uklib_consumes_C" to ResolvedComponentWithArtifacts(
//                    configuration="jvmRuntimeElements",
//                    artifacts=mutableListOf(platformJvmVariantAttributes)
//                ),
//                ":C_produces_only_uklib_consumes_B" to ResolvedComponentWithArtifacts(
//                    configuration="metadataUklibElements",
//                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationJvmAttributes)
//                ),
//                ":B_produces_only_platform_variant_consumes_A" to ResolvedComponentWithArtifacts(
//                    configuration="jvmRuntimeElements",
//                    artifacts=mutableListOf(platformJvmVariantAttributes)
//                ),
//                ":A_produces_uklib" to ResolvedComponentWithArtifacts(
//                    configuration="jvmRuntimeElements",
//                    artifacts=mutableListOf(platformJvmVariantAttributes),
//                )
//            ),
//            jvmResolvedVariants
//        )
//
//        assertEquals(
//            mapOf(
//                ":E_consumes_D" to ResolvedComponentWithArtifacts(
//                    configuration="iosArm64CompileKlibraries",
//                    artifacts=mutableListOf()
//                ),
//                ":D_produces_uklib_consumes_C" to ResolvedComponentWithArtifacts(
//                    configuration="iosArm64ApiElements",
//                    artifacts=mutableListOf(platformIosArm64Attributes + nonPacked)
//                ),
//                ":C_produces_only_uklib_consumes_B" to ResolvedComponentWithArtifacts(
//                    configuration="metadataUklibElements",
//                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationIosArm64Attributes)
//                ),
//                ":B_produces_only_platform_variant_consumes_A" to ResolvedComponentWithArtifacts(
//                    configuration="iosArm64ApiElements",
//                    artifacts=mutableListOf(platformIosArm64Attributes + nonPacked)
//                ),
//                ":A_produces_uklib" to ResolvedComponentWithArtifacts(
//                    configuration="iosArm64ApiElements",
//                    artifacts=mutableListOf(platformIosArm64Attributes + nonPacked),
//                )
//            ),
//            iosArm64ResolvedVariants
//        )
//    }
//
//    // @Test
//    fun `prefer uklib variant`() {
//        val consumer = mixedCompilationsGraphConsumer(
//            uklibResolutionStrategy = UklibResolutionStrategy.AllowResolvingUklibs,
//        )
//
//        val iosArm64CompilationDependencies = consumer.multiplatformExtension.iosArm64().compilations.getByName("main")
//            .configurations.compileDependencyConfiguration
//        val iosArm64ResolvedVariants = iosArm64CompilationDependencies.resolveProjectDependencyComponentsWithArtifacts()
//
//        val jvmRuntimeDependencies = consumer.multiplatformExtension.jvm().compilations.getByName("main")
//            .configurations.runtimeDependencyConfiguration!!
//        // FIXME: java-api variant doesn't resolve A
//        val jvmResolvedVariants = jvmRuntimeDependencies.resolveProjectDependencyComponentsWithArtifacts()
//
//        assertEquals(
//            mapOf(
//                ":E_consumes_D" to ResolvedComponentWithArtifacts(
//                    configuration="jvmRuntimeClasspath",
//                    artifacts=mutableListOf()
//                ),
//                ":D_produces_uklib_consumes_C" to ResolvedComponentWithArtifacts(
//                    configuration="metadataUklibElements",
//                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationJvmAttributes)
//                ),
//                ":C_produces_only_uklib_consumes_B" to ResolvedComponentWithArtifacts(
//                    configuration="metadataUklibElements",
//                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationJvmAttributes)
//                ),
//                ":B_produces_only_platform_variant_consumes_A" to ResolvedComponentWithArtifacts(
//                    configuration="jvmRuntimeElements",
//                    artifacts=mutableListOf(platformJvmVariantAttributes)
//                ),
//                ":A_produces_uklib" to ResolvedComponentWithArtifacts(
//                    configuration="metadataUklibElements",
//                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationJvmAttributes),
//                )
//            ),
//            jvmResolvedVariants
//        )
//
//        assertEquals(
//            mapOf(
//                ":E_consumes_D" to ResolvedComponentWithArtifacts(
//                    configuration="iosArm64CompileKlibraries",
//                    artifacts=mutableListOf()
//                ),
//                ":D_produces_uklib_consumes_C" to ResolvedComponentWithArtifacts(
//                    configuration="metadataUklibElements",
//                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationIosArm64Attributes)
//                ),
//                ":C_produces_only_uklib_consumes_B" to ResolvedComponentWithArtifacts(
//                    configuration="metadataUklibElements",
//                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationIosArm64Attributes)
//                ),
//                ":B_produces_only_platform_variant_consumes_A" to ResolvedComponentWithArtifacts(
//                    configuration="iosArm64ApiElements",
//                    artifacts=mutableListOf(platformIosArm64Attributes + nonPacked)
//                ),
//                ":A_produces_uklib" to ResolvedComponentWithArtifacts(
//                    configuration="metadataUklibElements",
//                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationIosArm64Attributes),
//                )
//            ),
//            iosArm64ResolvedVariants
//        )
//    }
//
//    private val uklibTransformationIosArm64Attributes = mapOf(
//        "artifactType" to "uklib",
//        "uklibTargetAttribute" to "ios_arm64",
//        "uklibState" to "unzipped",
//    )
//
//    private val uklibTransformationJvmAttributes = mapOf(
//        "artifactType" to "uklib",
//        "uklibTargetAttribute" to "jvm",
//        "uklibState" to "unzipped",
//    )
//
//    private val uklibTransformationMetadataAttributes = mapOf(
//        "artifactType" to "uklib",
//        "uklibTargetAttribute" to "common",
//        "uklibState" to "unzipped",
//    )
//
//    private val uklibTransformationJsAttributes = mapOf(
//        "artifactType" to "uklib",
//        "uklibTargetAttribute" to "js_ir",
//        "uklibState" to "unzipped",
//    )
//
//    private val uklibTransformationWasmJsAttributes = mapOf(
//        "artifactType" to "uklib",
//        "uklibTargetAttribute" to "wasm_js",
//        "uklibState" to "unzipped",
//    )
//
//    private val uklibTransformationWasmWasiAttributes = mapOf(
//        "artifactType" to "uklib",
//        "uklibTargetAttribute" to "wasm_wasi",
//        "uklibState" to "unzipped",
//    )
//
//    private val uklibVariantAttributes = mapOf(
//        "org.gradle.category" to "library",
//        "org.gradle.jvm.environment" to "???",
//        "org.gradle.usage" to "kotlin-uklib",
//        "org.jetbrains.kotlin.klib.packaging" to "packed",
//        "org.jetbrains.kotlin.native.target" to "???",
//        "org.jetbrains.kotlin.platform.type" to "unknown",
//    )
//
//    private val jvmPomRuntimeAttributes = mapOf(
//        "org.gradle.category" to "library",
//        "org.gradle.libraryelements" to "jar",
//        "org.gradle.status" to "release",
//        "org.gradle.usage" to "java-runtime",
//    )
//
//    private val jvmPomApiAttributes = mapOf(
//        "org.gradle.category" to "library",
//        "org.gradle.libraryelements" to "jar",
//        "org.gradle.status" to "release",
//        "org.gradle.usage" to "java-api",
//    )
//
//    private val platformJvmVariantAttributes = mapOf(
//        "artifactType" to "jar",
//        "org.gradle.category" to "library",
//        "org.gradle.jvm.environment" to "standard-jvm",
//        "org.gradle.libraryelements" to "jar",
//        "org.gradle.usage" to "java-runtime",
//        "org.jetbrains.kotlin.platform.type" to "jvm",
//    )
//
//    private val metadataVariantAttributes = mapOf(
//        "artifactType" to "jar",
//        "org.gradle.category" to "library",
//        "org.gradle.jvm.environment" to "non-jvm",
//        "org.gradle.libraryelements" to "jar",
//        "org.gradle.usage" to "kotlin-metadata",
//        "org.jetbrains.kotlin.platform.type" to "common",
//    )
//
//    private val releaseStatus = mapOf(
//        "org.gradle.status" to "release",
//    )
//
//    // We only emit packing in secondary variants which are not published?
//    private val nonPacked = mapOf(
//        "org.jetbrains.kotlin.klib.packaging" to "non-packed",
//    )
//
//    private val jarArtifact = mapOf(
//        "artifactType" to "jar",
//    )
//
//    private val uklibArtifact = mapOf(
//        "artifactType" to "uklib",
//    )
//
//    private val platformIosArm64Attributes = mapOf(
//        "artifactType" to "org.jetbrains.kotlin.klib",
//        "org.gradle.category" to "library",
//        "org.gradle.jvm.environment" to "non-jvm",
//        "org.gradle.usage" to "kotlin-api",
//        "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
//        "org.jetbrains.kotlin.native.target" to "ios_arm64",
//        "org.jetbrains.kotlin.platform.type" to "native",
//    )
//
//
//    private fun mixedCompilationsGraphConsumer(
//        uklibResolutionStrategy: UklibResolutionStrategy,
//    ): Project {
//        val root = buildProject()
//        return root.child(
//            "E_consumes_D",
//            uklibResolutionStrategy = uklibResolutionStrategy,
//            consume= root.child(
//                "D_produces_uklib_consumes_C",
//                consume = root.child(
//                    "C_produces_only_uklib_consumes_B",
//                    disablePlatformComponentReferences = true,
//                    consume = root.child(
//                        "B_produces_only_platform_variant_consumes_A",
//                        publishUklibVariant = false,
//                        consume = root.child(
//                            "A_produces_uklib",
//                            consume = null
//                        )
//                    )
//                )
//            )
//        )
//    }
//
//    private fun Project.child(
//        name: String,
//        consume: Project?,
//        publishUklibVariant: Boolean = true,
//        disablePlatformComponentReferences: Boolean = false,
//        uklibResolutionStrategy: UklibResolutionStrategy = UklibResolutionStrategy.ResolveOnlyPlatformSpecificVariant,
//    ): Project {
//        val parent = this
//        return buildProjectWithMPP(
//            preApplyCode = {
//                if (publishUklibVariant) {
//                    publishUklib()
//                }
//                fakeUklibTransforms()
//                setUklibResolutionStrategy(uklibResolutionStrategy)
//                disablePlatformSpecificComponentReferences(disablePlatformComponentReferences)
//                // Test stdlib in a separate test
//                enableDefaultStdlibDependency(false)
//                enableDefaultJsDomApiDependency(false)
//            },
//            projectBuilder = {
//                withParent(parent)
//                withName(name)
//            }
//        ) {
//            kotlin {
//                iosArm64()
//                iosX64()
//                jvm()
//
//                if (consume != null) {
//                    sourceSets.commonMain.dependencies {
//                        implementation(project(consume.project.path))
//                    }
//                }
//            }
//        }.evaluate()
//    }
//}