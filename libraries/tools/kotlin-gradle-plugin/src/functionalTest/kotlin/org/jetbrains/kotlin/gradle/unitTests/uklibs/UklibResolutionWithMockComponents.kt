/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.Project
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleMetadataComponent.Variant
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.setUklibResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test

class UklibResolutionTestsWithMockComponents {
    @get:Rule
    val tmpDir = TemporaryFolder()

    @Test
    fun `uklib resolution - resolving pure uklib component`() {
        val repo = generateMockRepository(
            tmpDir,
            listOf(
                GradleComponent(
                    GradleMetadataComponent(
                        component = fooBarGradleComponent,
                        variants = listOf(
                            uklibApiVariant,
                            jvmApiVariant,
                        ),
                    ),
                    fooBarUklibMavenComponent,
                )
            )
        )

        val consumer = uklibConsumer {
            kotlin {
                iosArm64()
                jvm()
                sourceSets.commonMain.dependencies { implementation("foo:bar:1.0") }
            }
            repositories.maven(repo)
        }

        assertEqualsPP(
            mapOf(
                "foo:bar:1.0" to ResolvedComponentWithArtifacts(
                    configuration="uklibApiElements",
                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationIosArm64Attributes + releaseStatus)
                ),
            ),
            consumer.multiplatformExtension.iosArm64().compilationResolution(),
        )
        assertEqualsPP(
            mapOf(
                "foo:bar:1.0" to ResolvedComponentWithArtifacts(
                    configuration="uklibApiElements",
                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationJvmAttributes + releaseStatus)
                ),
            ),
            consumer.multiplatformExtension.jvm().compilationResolution(),
        )
        assertEqualsPP(
            mapOf(
                "foo:bar:1.0" to ResolvedComponentWithArtifacts(
                    configuration="uklibApiElements",
                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationMetadataAttributes + releaseStatus)
                ),
            ),
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
        )
    }

    @Test
    fun `uklib resolution - resolving existings kmp publication`() {
        val repo = generateMockRepository(
            tmpDir,
            listOf(
                GradleComponent(
                    GradleMetadataComponent(
                        component = fooBarGradleComponent,
                        variants = listOf(
                            kmpMetadataJarVariant,
                            kmpIosArm64KlibVariant,
                            kmpJvmApiVariant,
                        ),
                    ),
                    fooBarUklibMavenComponent,
                )
            )
        )

        val consumer = uklibConsumer {
            kotlin {
                iosArm64()
                jvm()
                sourceSets.commonMain.dependencies { implementation("foo:bar:1.0") }
            }
            repositories.maven(repo)
        }

        assertEqualsPP(
            mapOf(
                "foo:bar:1.0" to ResolvedComponentWithArtifacts(
                    configuration="uklibApiElements",
                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationIosArm64Attributes + releaseStatus)
                ),
            ),
            consumer.multiplatformExtension.iosArm64().compilationResolution(),
        )
        assertEqualsPP(
            mapOf(
                "foo:bar:1.0" to ResolvedComponentWithArtifacts(
                    configuration="uklibApiElements",
                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationJvmAttributes + releaseStatus)
                ),
            ),
            consumer.multiplatformExtension.jvm().compilationResolution(),
        )
        assertEqualsPP(
            mapOf(
                "foo:bar:1.0" to ResolvedComponentWithArtifacts(
                    configuration="uklibApiElements",
                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationMetadataAttributes + releaseStatus)
                ),
            ),
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
        )
    }

    private fun uklibConsumer(code: Project.() -> Unit = {}): Project {
        return buildProjectWithMPP(
            preApplyCode = {
                fakeUklibTransforms()
                setUklibResolutionStrategy(KmpResolutionStrategy.ResolveUklibsAndResolvePSMLeniently)
                // Test stdlib in a separate test
                enableDefaultStdlibDependency(false)
                enableDefaultJsDomApiDependency(false)
            },
            code = code,
        ).evaluate()
    }

    private val fooBarGradleComponent = GradleMetadataComponent.Component(
        group = "foo",
        module = "bar",
        version = "1.0",
    )
    private val fooBarUklibMavenComponent = MavenComponent(
        "foo", "bar", "1.0",
        packaging = "uklib",
        dependencies = listOf(),
        true,
    )

    private val uklibApiVariant = Variant(
        name = "uklibApiElements",
        attributes = mapOf(
            "org.gradle.usage" to "kotlin-uklib-api",
            "org.gradle.category" to "library",
        ),
        files = listOf(
            GradleMetadataComponent.StubVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "uklib"
            )
        ),
        dependencies = listOf()
    )

    private val jvmApiVariant = Variant(
        name = "jvmApiElements",
        attributes = jvmApiAttributes,
        files = listOf(
            GradleMetadataComponent.StubVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar"
            )
        ),
        dependencies = listOf()
    )

    private val aarApiVariant = Variant(
        name = "jvmApiElements",
        attributes = jvmApiAttributes,
        files = listOf(
            GradleMetadataComponent.StubVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar"
            )
        ),
        dependencies = listOf()
    )

    private val kmpMetadataJarVariant = Variant(
        name = "metadataApiElements",
        attributes = kmpMetadataVariantAttributes,
        files = listOf(
            GradleMetadataComponent.StubVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar"
            )
        ),
        dependencies = listOf()
    )

    private val kmpIosArm64KlibVariant = Variant(
        name = "iosArm64ApiElements-published",
        attributes = mapOf(
            "artifactType" to "org.jetbrains.kotlin.klib", // wtf?
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-api",
            "org.jetbrains.kotlin.native.target" to "ios_arm64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        files = listOf(
            GradleMetadataComponent.StubVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "klib"
            )
        ),
        dependencies = listOf()
    )

    private val kmpJvmApiVariant = Variant(
        name = "jvmApiElements-published",
        attributes = kmpJvmApiVariantAttributes,
        files = listOf(
            GradleMetadataComponent.StubVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar"
            )
        ),
        dependencies = listOf()
    )
}