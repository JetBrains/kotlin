package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.iosMain
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.ERROR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.WARNING
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.validateKgpModelIsUklibCompliantAndCreateKgpFragments
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.testing.ResolvedComponentWithArtifacts
import org.jetbrains.kotlin.gradle.testing.compilationResolution
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.testing.resolveProjectDependencyComponentsWithArtifacts
import org.jetbrains.kotlin.gradle.testing.compilationConfiguration
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleMetadataComponent.Variant
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.projectPathOrNull
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Test
import kotlin.test.assertEquals

class UklibInterprojectResolutionTests {

    @Test
    fun `interproject uklib resolution - direct dependency on a pure uklib component - with matching set of targets`() {
        val root = buildProject()
        buildProjectWithMPP(
            projectBuilder = {
                withName("producer")
                withParent(root)
            },
            preApplyCode = {
                setUklibPublicationStrategy()
                setUklibResolutionStrategy(KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs)
                enableDefaultStdlibDependency(false)
                enableDefaultJsDomApiDependency(false)
            },
            code = {
                kotlin {
                    iosArm64()
                    iosX64()
                    jvm()
                    js()
                }
            }
        ).evaluate()

        val consumer = buildProjectWithMPP(
            projectBuilder = {
                withParent(root)
            },
            preApplyCode = {
                setUklibPublicationStrategy()
                setUklibResolutionStrategy(KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs)
                enableDefaultStdlibDependency(false)
                enableDefaultJsDomApiDependency(false)
            },
            code = {
                kotlin {
                    iosArm64()
                    iosX64()
                    jvm()
                    js()
                    sourceSets.commonMain.dependencies {
                        implementation(project(":producer"))
                    }
                }
            }
        ).evaluate()

        listOf(
            consumer.multiplatformExtension.iosArm64(),
            consumer.multiplatformExtension.jvm(),
            consumer.multiplatformExtension.js(),
        ).forEach {
            assertEquals(
                mapOf(
                    ":producer" to ResolvedComponentWithArtifacts(
                        configuration = "uklibApiElements",
                        artifacts = mutableListOf()
                    ),
                ).prettyPrinted,
                it.compilationResolution().prettyPrinted,
                it.name,
            )
        }

        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration,
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration,
        ).forEach {
            assertEquals(
                mapOf(
                    ":producer" to ResolvedComponentWithArtifacts(
                        configuration = "uklibApiElements",
                        artifacts = mutableListOf()
                    ),
                ).prettyPrinted,
                it.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted,
                it.name,
            )
        }
    }

    @Test
    fun `interproject uklib resolution - direct dependency on a non-uklib producing component - with a subset of targets`() {
        val root = buildProject()
        buildProjectWithMPP(
            projectBuilder = {
                withName("producer")
                withParent(root)
            },
            preApplyCode = {
                enableDefaultStdlibDependency(false)
                enableDefaultJsDomApiDependency(false)
            },
            code = {
                kotlin {
                    iosArm64()
                }
            }
        ).evaluate()

        val consumer = buildProjectWithMPP(
            projectBuilder = {
                withParent(root)
            },
            preApplyCode = {
                setUklibResolutionStrategy(KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs)
                enableDefaultStdlibDependency(false)
                enableDefaultJsDomApiDependency(false)
            },
            code = {
                kotlin {
                    iosArm64()
                    iosSimulatorArm64()
                    iosX64()
                    jvm()
                    js()
                    sourceSets.commonMain.dependencies {
                        implementation(project(":producer"))
                    }
                }
            }
        ).evaluate()

        listOf(
            consumer.multiplatformExtension.iosSimulatorArm64().compilationConfiguration(),
            consumer.multiplatformExtension.jvm().compilationConfiguration(),
            consumer.multiplatformExtension.js().compilationConfiguration(),
        ).forEach {
            assertEquals(
                listOf<Pair<String, String>>(
                    Pair(
                        first = "project :producer",
                        second = "metadataApiElements",
                    ),
                ).prettyPrinted,
                it.resolveVariantNames().prettyPrinted,
            )
        }

        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration.resolveVariantNames(),
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveVariantNames(),
        ).forEach {
            assertEquals(
                listOf<Pair<String, String>>(
                    Pair(
                        first = "project :producer",
                        second = "metadataApiElements",
                    ),
                ).prettyPrinted,
                it.prettyPrinted,
            )
        }
    }

    fun Configuration.resolveVariantNames(): List<Pair<String, String>> {
        val selfProjectPath = incoming.resolutionResult.root.variants.single().owner.projectPathOrNull
        return incoming.resolutionResult.allComponents
            .filter {
                it.id.projectPathOrNull != selfProjectPath
            }
            .map { component ->
                component.id.displayName to component.variants.single().displayName
            }
    }

}