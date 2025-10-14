package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.nativeMain
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.partiallyUnresolvedPlatformDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.testing.PrettyPrint
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableEagerUnresolvedDependenciesDiagnostic
import org.jetbrains.kotlin.gradle.util.enableUnresolvedDependenciesDiagnostic
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.assertEquals

class KmpPartiallyResolvedDependenciesCheckerTests {
    internal data class TestUnresolvedKmpDependency(
        val displayName: String,
        var resolvedMetadataComponentIdentifier: String? = null,
        val resolvedVariants: List<ResolvedVariant> = mutableListOf(),
        val unresolvedComponents: List<UnresolvedComponent> = mutableListOf(),
    ) {
        data class ResolvedVariant(
            val compilationName: String,
            val configurationName: String,
            val variant: String,
        )

        data class UnresolvedComponent(
            val compilationName: String,
            val configurationName: String,
        )
    }

    @org.junit.Test
    fun `partially resolved kmp dependencies checker - emits diagnostic when direct project dependency is missing a target`() {
        val root = buildProject {
            repositories.mavenLocal()
        }
        val transitiveProducer = buildProjectWithMPP(projectBuilder = {
            withName("transitiveProducer")
            withParent(root)
        }) {
            repositories.mavenLocal()
            kotlin {
                jvm()
                iosArm64()
                linuxArm64()
            }
        }

        val directProducer = buildProjectWithMPP(projectBuilder = {
            withName("directProducer")
            withParent(root)
        }) {
            repositories.mavenLocal()
            kotlin {
                jvm()
                iosArm64()
                linuxArm64()

                sourceSets.commonMain.dependencies {
                    implementation(project(":transitiveProducer"))
                }
            }
        }

        val consumer = buildProjectWithMPP(
            projectBuilder = {
                withParent(root)
            },
        ) {
            repositories.mavenLocal()
            kotlin {
                jvm()
                iosArm64()
                linuxArm64()

                js()

                sourceSets.commonMain.dependencies {
                    implementation(project(":directProducer"))
                }
            }
        }

        transitiveProducer.evaluate()
        directProducer.evaluate()
        consumer.evaluate()
        consumer.gradle.buildListenerBroadcaster.projectsEvaluated(consumer.gradle)

        val commonMainTransformationParameters = consumer.locateOrRegisterMetadataDependencyTransformationTask(
            consumer.multiplatformExtension.sourceSets.commonMain.get()
        ).get().transformationParameters

        assertEquals<PrettyPrint<List<TestUnresolvedKmpDependency>>>(
            mutableListOf(
                TestUnresolvedKmpDependency(
                    displayName = "project :directProducer",
                    resolvedMetadataComponentIdentifier = "project :directProducer",
                    resolvedVariants = mutableListOf(
                        TestUnresolvedKmpDependency.ResolvedVariant(
                            compilationName = "iosArm64Main",
                            configurationName = "iosArm64CompileKlibraries",
                            variant = "iosArm64ApiElements",
                        ),
                        TestUnresolvedKmpDependency.ResolvedVariant(
                            compilationName = "jvmMain",
                            configurationName = "jvmCompileClasspath",
                            variant = "jvmApiElements",
                        ),
                        TestUnresolvedKmpDependency.ResolvedVariant(
                            compilationName = "linuxArm64Main",
                            configurationName = "linuxArm64CompileKlibraries",
                            variant = "linuxArm64ApiElements",
                        ),
                    ),
                    unresolvedComponents = mutableListOf(
                        TestUnresolvedKmpDependency.UnresolvedComponent(
                            compilationName = "jsMain",
                            configurationName = "jsCompileClasspath",
                        ),
                    ),
                ),
            ).prettyPrinted,
            partiallyUnresolvedPlatformDependencies(
                commonMainTransformationParameters.dependingPlatformCompilations,
                commonMainTransformationParameters.resolvedMetadataConfiguration.resolvedComponent,
            ).normalize().prettyPrinted
        )

        val nativeMainTransformationParameters = consumer.locateOrRegisterMetadataDependencyTransformationTask(
            consumer.multiplatformExtension.sourceSets.nativeMain.get()
        ).get().transformationParameters
        assertEquals<PrettyPrint<List<TestUnresolvedKmpDependency>>>(
            mutableListOf<TestUnresolvedKmpDependency>(
            ).prettyPrinted,
            partiallyUnresolvedPlatformDependencies(
                nativeMainTransformationParameters.dependingPlatformCompilations,
                nativeMainTransformationParameters.resolvedMetadataConfiguration.resolvedComponent,
            ).normalize().prettyPrinted
        )
    }

    @org.junit.Test
    fun `partially resolved kmp dependencies checker - metadata only resolution produces diagnostic`() {
        val root = buildProject { repositories.mavenLocal() }
        val directProducer = buildProjectWithMPP(projectBuilder = {
            withName("directProducer")
            withParent(root)
        }) {
            repositories.mavenLocal()
            kotlin {
                jvm()
            }
        }

        val consumer = buildProjectWithMPP(
            projectBuilder = {
                withParent(root)
            },
        ) {
            repositories.mavenLocal()
            kotlin {
                iosArm64()
                iosX64()

                sourceSets.commonMain.dependencies {
                    implementation(project(":directProducer"))
                }
            }
        }

        directProducer.evaluate()
        consumer.evaluate()
        consumer.gradle.buildListenerBroadcaster.projectsEvaluated(consumer.gradle)

        val commonMainTransformationParameters = consumer.locateOrRegisterMetadataDependencyTransformationTask(
            consumer.multiplatformExtension.sourceSets.commonMain.get()
        ).get().transformationParameters

        assertEquals<PrettyPrint<List<TestUnresolvedKmpDependency>>>(
            mutableListOf(
                TestUnresolvedKmpDependency(
                    displayName = "project :directProducer",
                    resolvedMetadataComponentIdentifier = "project :directProducer",
                    resolvedVariants = mutableListOf(
                    ),
                    unresolvedComponents = mutableListOf(
                        TestUnresolvedKmpDependency.UnresolvedComponent(
                            compilationName = "iosArm64Main",
                            configurationName = "iosArm64CompileKlibraries",
                        ),
                        TestUnresolvedKmpDependency.UnresolvedComponent(
                            compilationName = "iosX64Main",
                            configurationName = "iosX64CompileKlibraries",
                        ),
                    ),
                ),
            ).prettyPrinted,
            partiallyUnresolvedPlatformDependencies(
                commonMainTransformationParameters.dependingPlatformCompilations,
                commonMainTransformationParameters.resolvedMetadataConfiguration.resolvedComponent,
            ).normalize().prettyPrinted
        )
    }

    @org.junit.Test
    fun `partially resolved kmp dependencies checker - non eager implementation produces diagnostic only when metadata tasks are materialized`() {
        val root = buildProject { repositories.mavenLocal() }
        val directProducer = buildProjectWithMPP(projectBuilder = {
            withName("directProducer")
            withParent(root)
        }) {
            repositories.mavenLocal()
            kotlin {
                jvm()
            }
        }

        val consumer = buildProjectWithMPP(
            projectBuilder = {
                withParent(root)
            },
            preApplyCode = {
                enableEagerUnresolvedDependenciesDiagnostic(false)
            }
        ) {
            repositories.mavenLocal()
            kotlin {
                iosArm64()
                iosX64()

                sourceSets.commonMain.dependencies {
                    implementation(project(":directProducer"))
                }
            }
        }

        directProducer.evaluate()
        consumer.evaluate()
        consumer.gradle.buildListenerBroadcaster.projectsEvaluated(consumer.gradle)

        consumer.assertNoDiagnostics()

        consumer.locateOrRegisterMetadataDependencyTransformationTask(
            consumer.multiplatformExtension.sourceSets.commonMain.get()
        ).get()

        consumer.assertContainsDiagnostic(
            KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies
        )
    }

    @org.junit.Test
    fun `partially resolved kmp dependencies checker - is disableable`() {
        val root = buildProject { repositories.mavenLocal() }
        val directProducer = buildProjectWithMPP(projectBuilder = {
            withName("directProducer")
            withParent(root)
        }) {
            repositories.mavenLocal()
            kotlin {
                jvm()
            }
        }

        val consumer = buildProjectWithMPP(
            projectBuilder = {
                withParent(root)
            },
            preApplyCode = {
                enableUnresolvedDependenciesDiagnostic(false)
            }
        ) {
            repositories.mavenLocal()
            kotlin {
                iosArm64()
                iosX64()

                sourceSets.commonMain.dependencies {
                    implementation(project(":directProducer"))
                }
            }
        }

        directProducer.evaluate()
        consumer.evaluate()
        consumer.gradle.buildListenerBroadcaster.projectsEvaluated(consumer.gradle)

        consumer.assertNoDiagnostics()

        consumer.locateOrRegisterMetadataDependencyTransformationTask(
            consumer.multiplatformExtension.sourceSets.commonMain.get()
        ).get()

        consumer.assertNoDiagnostics()
    }

    @org.junit.Test
    fun `partially resolved kmp dependencies checker - single target consumption produces no diagnostic`() {
        val root = buildProject { repositories.mavenLocal() }
        val directProducer = buildProjectWithMPP(projectBuilder = {
            withName("directProducer")
            withParent(root)
        }) {
            repositories.mavenLocal()
            kotlin {
                jvm()
            }
        }

        val consumer = buildProjectWithMPP(
            projectBuilder = {
                withParent(root)
            },
        ) {
            repositories.mavenLocal()
            kotlin {
                iosArm64()
                sourceSets.commonMain.dependencies {
                    implementation(project(":directProducer"))
                }
            }
        }

        directProducer.evaluate()
        consumer.evaluate()
        consumer.gradle.buildListenerBroadcaster.projectsEvaluated(consumer.gradle)

        // FIXME: KT-79205
        consumer.assertNoDiagnostics()
    }

    @org.junit.Test
    fun `partially resolved kmp dependencies checker - doesn't emit diagnostic for non completely unresolvable dependencies`() {
        val consumer = buildProjectWithMPP {
            repositories.mavenLocal()
            kotlin {
                jvm()
                iosArm64()
                linuxArm64()

                sourceSets.commonMain.dependencies {
                    implementation("foo:bar:1.0")
                }
            }
        }
        consumer.evaluate()
        consumer.gradle.buildListenerBroadcaster.projectsEvaluated(consumer.gradle)

        consumer.assertNoDiagnostics()

        val commonMainTransformationParameters = consumer.locateOrRegisterMetadataDependencyTransformationTask(
            consumer.multiplatformExtension.sourceSets.commonMain.get()
        ).get().transformationParameters

        assertEquals<PrettyPrint<List<TestUnresolvedKmpDependency>>>(
            mutableListOf<TestUnresolvedKmpDependency>(
            ).prettyPrinted,
            partiallyUnresolvedPlatformDependencies(
                commonMainTransformationParameters.dependingPlatformCompilations,
                commonMainTransformationParameters.resolvedMetadataConfiguration.resolvedComponent,
            ).normalize().prettyPrinted
        )
    }


    private fun Collection<KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies.UnresolvedKmpDependency>.normalize() = map { it.normalize() }

    private fun KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies.UnresolvedKmpDependency.normalize() = TestUnresolvedKmpDependency(
        displayName = displayName,
        resolvedMetadataComponentIdentifier = resolvedMetadataComponentIdentifier?.toString(),
        resolvedVariants = resolvedVariants.map {
            TestUnresolvedKmpDependency.ResolvedVariant(
                compilationName = it.compilationName,
                configurationName = it.configurationName,
                variant = it.variant,
            )
        },
        unresolvedComponents = unresolvedComponents.map {
            TestUnresolvedKmpDependency.UnresolvedComponent(
                compilationName = it.compilationName,
                configurationName = it.configurationName,
            )
        }
    )
}