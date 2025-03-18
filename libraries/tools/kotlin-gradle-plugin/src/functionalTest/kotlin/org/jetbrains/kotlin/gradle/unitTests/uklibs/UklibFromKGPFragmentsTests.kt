/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.ERROR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.WARNING
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.validateKgpModelIsUklibCompliantAndCreateKgpFragments
import org.jetbrains.kotlin.gradle.testing.PrettyPrint
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalWasmDsl
class UklibFromKGPFragmentsTests {

    @Test
    fun `uklib fragments - single target project has a single fragment`() {
        buildProjectWithMPP(
            preApplyCode = {
                setUklibPublicationStrategy()
            }
        ) {
            kotlin {
                iosArm64()
            }

            runLifecycleAwareTest {
                assertEquals(
                    listOf(
                        TestAttributesFragment(
                            "iosArm64Main",
                            setOf("ios_arm64"),
                        )
                    ),
                    multiplatformExtension.testFragments()
                )
                assertEquals<PrettyPrint<List<TestRefineesFragment>>>(
                    mutableListOf(
                        TestRefineesFragment(
                            identifier = "iosArm64Main",
                            refinees = mutableSetOf(
                                "appleMain",
                                "commonMain",
                                "iosMain",
                                "nativeMain",
                            ),
                        ),
                    ).prettyPrinted,
                    multiplatformExtension.refinees().prettyPrinted,
                )
            }
        }
    }

    @Test
    fun `uklib fragments - with multiple targets and a bamboo structure`() {
        buildProjectWithMPP(
            preApplyCode = {
                setUklibPublicationStrategy()
                enableCrossCompilation()
            }
        ) {
            kotlin {
                iosArm64()
                iosX64()
                jvm()

                applyHierarchyTemplate {
                    group("customMain") {
                        group("customApple") {
                            group("customIos") {
                                withIosArm64()
                                withIosX64()
                            }
                        }
                        withJvm()
                    }
                }
            }

            runLifecycleAwareTest {
                assertEquals(
                    setOf(
                        TestAttributesFragment(
                            "customAppleMain",
                            setOf("ios_arm64", "ios_x64"),
                        ),
                        TestAttributesFragment(
                            "customIosMain",
                            setOf("ios_arm64", "ios_x64"),
                        ),
                        TestAttributesFragment(
                            "customMainMain",
                            setOf("ios_arm64", "ios_x64", "jvm"),
                        ),
                        TestAttributesFragment(
                            "iosArm64Main",
                            setOf("ios_arm64"),
                        ),
                        TestAttributesFragment(
                            "iosX64Main",
                            setOf("ios_x64"),
                        ),
                        TestAttributesFragment(
                            "jvmMain",
                            setOf("jvm")
                        )
                    ),
                    multiplatformExtension.testFragments().toSet()
                )
                assertEquals<PrettyPrint<List<TestRefineesFragment>>>(
                    mutableListOf(
                        TestRefineesFragment(
                            identifier = "iosArm64Main",
                            refinees = mutableSetOf(
                                "customAppleMain",
                                "customIosMain",
                                "customMainMain",
                            ),
                        ),
                        TestRefineesFragment(
                            identifier = "iosX64Main",
                            refinees = mutableSetOf(
                                "customAppleMain",
                                "customIosMain",
                                "customMainMain",
                            ),
                        ),
                        TestRefineesFragment(
                            identifier = "jvmMain",
                            refinees = mutableSetOf(
                                "customMainMain",
                            ),
                        ),
                        TestRefineesFragment(
                            identifier = "customAppleMain",
                            refinees = mutableSetOf(
                                "customMainMain",
                            ),
                        ),
                        TestRefineesFragment(
                            identifier = "customIosMain",
                            refinees = mutableSetOf(
                                "customAppleMain",
                                "customMainMain",
                            ),
                        ),
                        TestRefineesFragment(
                            identifier = "customMainMain",
                            refinees = mutableSetOf(
                            ),
                        ),
                    ).prettyPrinted,
                    multiplatformExtension.refinees().prettyPrinted,
                )
            }
            // Check that bamboo refinement is not emitted; we must check for bamboo refinement at execution
            assertNoDiagnostics(
                filterDiagnosticIds = listOf(
                    KotlinToolingDiagnostics.OldNativeVersionDiagnostic,
                    KotlinToolingDiagnostics.InternalKotlinGradlePluginPropertiesUsed,
                    KotlinToolingDiagnostics.UnusedSourceSetsWarning,
                )
            )
        }
    }

    @Test
    fun `uklib fragments - all supported targets`() {
        buildProjectWithMPP(
            preApplyCode = {
                setUklibPublicationStrategy()
            }
        ) {
            androidLibrary { compileSdk = 31 }
            kotlin {
                iosArm64()
                iosX64()
                jvm()
                js()
                wasmJs()
                wasmWasi()
                androidTarget()
            }
        }.runLifecycleAwareTest {
            assertEquals(
                setOf(
                    TestAttributesFragment(identifier = "iosArm64Main", attributes = setOf("ios_arm64")),
                    TestAttributesFragment(identifier = "iosX64Main", attributes = setOf("ios_x64")),
                    TestAttributesFragment(identifier = "appleMain", attributes = setOf("ios_arm64", "ios_x64")),
                    TestAttributesFragment(identifier = "iosMain", attributes = setOf("ios_arm64", "ios_x64")),
                    TestAttributesFragment(identifier = "nativeMain", attributes = setOf("ios_arm64", "ios_x64")),
                    TestAttributesFragment(identifier = "jsMain", attributes = setOf("js_ir")),
                    TestAttributesFragment(identifier = "jvmMain", attributes = setOf("jvm")),
                    TestAttributesFragment(identifier = "wasmJsMain", attributes = setOf("wasm_js")),
                    TestAttributesFragment(identifier = "wasmWasiMain", attributes = setOf("wasm_wasi")),
                    TestAttributesFragment(
                        identifier = "commonMain",
                        attributes = setOf("android", "ios_arm64", "ios_x64", "js_ir", "jvm", "wasm_js", "wasm_wasi")
                    ),
                ),
                multiplatformExtension.testFragments().toSet()
            )
        }
    }

    //@Test
    fun `uklib fragments - correct and incorrect diamonds`() {
        // FIXME: ...
    }

    @Test
    fun `project configuration with enabled uklib publication - with external target - emits diagnostic`() {
        buildProjectWithMPP(
            preApplyCode = { setUklibPublicationStrategy() }
        ) {
            kotlin {
                createExternalKotlinTarget<FakeTarget> { defaults() }.createCompilation<FakeCompilation> { defaults(this@kotlin) }
            }
        }.evaluate().assertContainsDiagnostic(KotlinToolingDiagnostics.UklibFragmentFromUnexpectedTarget)
    }

    @Test
    fun `project configuration with enabled uklib publication - without enabled cross compilation - emits diagnostic`() {
        buildProjectWithMPP(
            preApplyCode = {
                setUklibPublicationStrategy()
            }
        ) {
            kotlin {
                iosArm64()
            }
        }.evaluate().assertContainsDiagnostic(
            KotlinToolingDiagnostics.UklibPublicationWithoutCrossCompilation(
                if (HostManager.hostIsMac) WARNING else ERROR
            )
        )
    }

    @Test
    fun `project configuration with enabled uklib publication - with cinterops - emits diagnostic`() {
        buildProjectWithMPP(
            preApplyCode = {
                setUklibPublicationStrategy()
            }
        ) {
            kotlin {
                iosArm64().compilations.getByName("main").cinterops.create("foo")
            }
        }.evaluate().assertContainsDiagnostic(
            KotlinToolingDiagnostics.UklibPublicationWithCinterops
        )
    }

    @Test
    fun `project configuration with enabled uklib publication - multiple same targets - emits diagnostic`() {
        buildProjectWithMPP(
            preApplyCode = {
                setUklibPublicationStrategy()
            }
        ) {
            kotlin {
                iosArm64("a")
                iosArm64("b")
            }
        }.evaluate().assertContainsDiagnostic(
            KotlinToolingDiagnostics.KotlinTargetAlreadyDeclaredError
        )
    }

    @Test
    fun `project configuration with enabled uklib publication - under refinement - emits diagnostic`() {
        buildProjectWithMPP(
            preApplyCode = {
                setUklibPublicationStrategy()
            }
        ) {
            kotlin {
                iosArm64()
                iosX64()
                iosSimulatorArm64()

                applyHierarchyTemplate {
                    group("iosAll") {
                        withIosArm64()
                        withIosX64()
                        withIosSimulatorArm64()
                    }
                    group("iosSimulator") {
                        withIosX64()
                        withIosSimulatorArm64()
                    }
                }
            }
        }.evaluate().assertContainsDiagnostic(
            KotlinToolingDiagnostics.UklibSourceSetStructureUnderRefinementViolation
        )
    }

    @Test
    fun `project configuration with enabled uklib publication - multirooted graph - is permitted`() {
        buildProjectWithMPP(
            preApplyCode = {
                setUklibPublicationStrategy()
                enableCrossCompilation()
            }
        ) {
            kotlin {
                applyHierarchyTemplate {
                    group("linux") {
                        withLinuxArm64()
                        withLinuxX64()
                    }
                    group("ios") {
                        withIosArm64()
                        withIosX64()
                    }
                }

                linuxX64()
                linuxArm64()
                iosArm64()
                iosX64()
            }
        }.evaluate().assertNoDiagnostics(
            filterDiagnosticIds = listOf(
                KotlinToolingDiagnostics.OldNativeVersionDiagnostic,
                KotlinToolingDiagnostics.InternalKotlinGradlePluginPropertiesUsed,
                KotlinToolingDiagnostics.UnusedSourceSetsWarning,
            )
        )
    }

    internal data class TestAttributesFragment(
        val identifier: String,
        val attributes: Set<String>,
    )

    private suspend fun KotlinMultiplatformExtension.testFragments(): List<TestAttributesFragment> =
        validateKgpModelIsUklibCompliantAndCreateKgpFragments().map {
            TestAttributesFragment(
                identifier = it.fragment.get().identifier,
                attributes = it.fragment.get().attributes,
            )
        }

    internal data class TestRefineesFragment(
        val identifier: String,
        val refinees: Set<String>,
    )

    private suspend fun KotlinMultiplatformExtension.refinees(): List<TestRefineesFragment> =
        validateKgpModelIsUklibCompliantAndCreateKgpFragments()
            .map {
                TestRefineesFragment(
                    identifier = it.fragment.get().identifier,
                    refinees = it.refineesTransitiveClosure,
                )
            }
}