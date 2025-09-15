package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.iosMain
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.testing.ComponentPath
import org.jetbrains.kotlin.gradle.testing.ResolvedComponentWithArtifacts
import org.jetbrains.kotlin.gradle.testing.compilationResolution
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.testing.resolveProjectDependencyComponentsWithArtifacts
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import kotlin.test.assertEquals

class UklibInterprojectResolutionTests {

    @Test
    fun `interproject uklib resolution - transitive dependency through jvm library - resolves uklib variants in transitive dependency`() {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            iosArm64()
            iosX64()
            jvm()
            js()
        }
        val root = buildProject()
        projectWithUklibs(root, "producer") {
            kotlin { targets() }
        }.evaluate()

        val directJavaConsumer = buildProject(
            projectBuilder = {
                withParent(root)
                withName("directJavaConsumer")
            }
        ) {
            plugins.apply("java-library")
            dependencies {
                "api"(project(":producer"))
            }
        }.evaluate()

        val transitiveUklibConsumer = projectWithUklibs(root, "transitiveUklibConsumer") {
            kotlin {
                targets()
                dependencies {
                    implementation(project(":directJavaConsumer"))
                }
            }
        }.evaluate()

        assertEquals(
            mapOf(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmApiElements",
                ),
            ).prettyPrinted,
            directJavaConsumer.configurations.getByName("compileClasspath")
                .resolveProjectDependencyComponentsWithArtifacts()
                .prettyPrinted
        )

        assertResolveArtifactMatchesAttributes(
            transitiveUklibConsumer.multiplatformExtension.iosArm64(),
            mutableMapOf(
                /** FIXME: KT-81055 java classes will not be filtered by [org.jetbrains.kotlin.gradle.tasks.canKlibBePassedToCompiler] */
                ":directJavaConsumer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "java-classes-directory",
                            "org.gradle.category" to "library",
                            "org.gradle.dependency.bundling" to "external",
                            "org.gradle.jvm.version" to "17",
                            "org.gradle.libraryelements" to "classes",
                            "org.gradle.usage" to "java-api",
                        ),
                    ),
                    configuration = "apiElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "ios_arm64",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            ),
        )

        assertResolveArtifactMatchesAttributes(
            transitiveUklibConsumer.multiplatformExtension.jvm(),
            mapOf(
                ":directJavaConsumer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "java-classes-directory",
                            "org.gradle.category" to "library",
                            "org.gradle.dependency.bundling" to "external",
                            "org.gradle.jvm.version" to "17",
                            "org.gradle.libraryelements" to "classes",
                            "org.gradle.usage" to "java-api",
                        ),
                    ),
                    configuration = "apiElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "not-a-metadata-jar",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            )
        )

        listOf(
            transitiveUklibConsumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration,
            transitiveUklibConsumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration,
        ).forEach {
            assertEquals(
                mapOf(
                    ":directJavaConsumer" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "jar",
                                "org.gradle.category" to "library",
                                "org.gradle.dependency.bundling" to "external",
                                "org.gradle.jvm.version" to "17",
                                "org.gradle.libraryelements" to "jar",
                                "org.gradle.usage" to "java-api",
                                "org.jetbrains.kotlin.isMetadataJar" to "unknown",
                            ),
                        ),
                        configuration = "apiElements",
                    ),
                    ":producer" to ResolvedComponentWithArtifacts(
                        configuration = "uklibApiElements",
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklibManifest",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "whole_uklib",
                            ),
                        ),
                    ),
                ).prettyPrinted,
                it.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted,
                it.name,
            )
        }
    }


    @Test
    fun `interproject uklib resolution - direct dependency a uklib producing component - with matching set of targets`() {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            iosArm64()
            iosX64()
            jvm()
            js()
        }
        val root = buildProject()
        projectWithUklibs(root, "producer") {
            kotlin { targets() }
        }.evaluate()

        val consumer = projectWithUklibs(root, "consumer") {
            kotlin {
                targets()
                sourceSets.commonMain.dependencies {
                    implementation(project(":producer"))
                }
            }
        }.evaluate()

        assertResolveArtifactMatchesAttributes(
            consumer.multiplatformExtension.iosArm64(),
            mapOf(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "ios_arm64",
                        )
                    ),
                    configuration = "uklibApiElements",
                ),
            ),
        )

        assertResolveArtifactMatchesAttributes(
            consumer.multiplatformExtension.jvm(),
            mapOf(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "not-a-metadata-jar",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            )
        )

        assertResolveArtifactMatchesAttributes(
            consumer.multiplatformExtension.js(),
            mapOf(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "js_ir",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            )
        )

        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration,
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration,
        ).forEach {
            assertEquals(
                mapOf(
                    ":producer" to ResolvedComponentWithArtifacts(
                        configuration = "uklibApiElements",
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklibManifest",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "whole_uklib",
                            ),
                        ),
                    ),
                ).prettyPrinted,
                it.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted,
                it.name,
            )
        }
    }

    @Test
    fun `interproject uklib resolution - direct dependency on a uklib producing component - with a subset of targets`() {
        val root = buildProject()
        projectWithUklibs(root, "producer") {
            kotlin {
                iosArm64()
            }
        }.evaluate()

        val consumer = projectWithUklibs(root, "consumer") {
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
        }.evaluate()

        listOf(
            consumer.multiplatformExtension.iosSimulatorArm64().compilationResolution(),
            consumer.multiplatformExtension.jvm().compilationResolution(),
            consumer.multiplatformExtension.js().compilationResolution(),
        ).forEach {
            assertEquals(
                mapOf(
                    ":producer" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(),
                        configuration = "uklibApiElements",
                    ),
                ).prettyPrinted,
                it.prettyPrinted,
                it.toString(),
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
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklibManifest",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "whole_uklib",
                            ),
                        ),
                    ),
                ).prettyPrinted,
                it.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted,
                it.name,
            )
        }
    }

    @Test
    fun `interproject uklib resolution - transitive dependency though a uklib producing component - with a subset of targets`() {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            iosArm64()
            iosX64()
            jvm()
            js()
        }
        val root = buildProject()
        projectWithUklibs(root, "producer") {
            kotlin { targets() }
        }.evaluate()

        projectWithUklibs(root, "intermediate") {
            kotlin {
                jvm()
                sourceSets.commonMain.dependencies {
                    api(project(":producer"))
                }
            }
        }.evaluate()

        val consumer = projectWithUklibs(root, "consumer") {
            kotlin {
                targets()
                sourceSets.commonMain.dependencies {
                    implementation(project(":intermediate"))
                }
            }
        }.evaluate()

        assertResolveArtifactMatchesAttributes(
            consumer.multiplatformExtension.iosArm64(),
            mapOf(
                ":intermediate" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "uklibApiElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "ios_arm64",
                        )
                    ),
                    configuration = "uklibApiElements",
                ),
            ),
        )

        assertResolveArtifactMatchesAttributes(
            consumer.multiplatformExtension.jvm(),
            mapOf(
                ":intermediate" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "not-a-metadata-jar",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "not-a-metadata-jar",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            )
        )

        assertResolveArtifactMatchesAttributes(
            consumer.multiplatformExtension.js(),
            mapOf(
                ":intermediate" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "uklibApiElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "js_ir",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            )
        )

        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration,
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration,
        ).forEach {
            assertEquals(
                mapOf(
                    ":intermediate" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklibManifest",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "whole_uklib",
                            ),
                        ),
                        configuration = "uklibApiElements",
                    ),
                    ":producer" to ResolvedComponentWithArtifacts(
                        configuration = "uklibApiElements",
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklibManifest",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "whole_uklib",
                            ),
                        ),
                    ),
                ).prettyPrinted,
                it.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted,
                it.name,
            )
        }
    }

    private fun projectWithUklibs(
        root: Project,
        name: String,
        code: Project.() -> Unit
    ) = buildProjectWithMPP(
        projectBuilder = {
            withName(name)
            withParent(root)
        },
        preApplyCode = {
            setUklibPublicationStrategy()
            setUklibResolutionStrategy(KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs)
            enableDefaultStdlibDependency(false)
            enableDefaultJsDomApiDependency(false)
        },
        code = code,
    )

    fun assertResolveArtifactMatchesAttributes(
        target: KotlinTarget,
        resolvedComponents: Map<ComponentPath, ResolvedComponentWithArtifacts>,
    ) {
        assertEquals(
            resolvedComponents.prettyPrinted,
            target.compilationResolution().prettyPrinted,
            target.name,
        )
    }

}