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
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.formCompilationClasspathInConsumingModuleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.validateKgpModelIsUklibCompliantAndCreateKgpFragments
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
                publishUklib()
            }
        ) {
            kotlin {
                iosArm64()
            }

            runLifecycleAwareTest {
                assertEquals(
                    listOf(
                        TestFragment(
                            "iosArm64Main",
                            setOf("ios_arm64"),
                        )
                    ),
                    multiplatformExtension.testFragments()
                )
            }
        }
    }

    @Test
    fun `uklib fragments - with multiple targets and a bamboo structure`() {
        buildProjectWithMPP(
            preApplyCode = {
                publishUklib()
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
                        TestFragment(
                            "customAppleMain",
                            setOf("ios_arm64", "ios_x64"),
                        ),
                        TestFragment(
                            "customIosMain",
                            setOf("ios_arm64", "ios_x64"),
                        ),
                        TestFragment(
                            "customMainMain",
                            setOf("ios_arm64", "ios_x64", "jvm"),
                        ),
                        TestFragment(
                            "iosArm64Main",
                            setOf("ios_arm64"),
                        ),
                        TestFragment(
                            "iosX64Main",
                            setOf("ios_x64"),
                        ),
                        TestFragment(
                            "jvmMain",
                            setOf("jvm")
                        )
                    ),
                    multiplatformExtension.testFragments().toSet()
                )
            }
        }
    }

    @Test
    fun `uklib fragments - all supported targets`() {
        buildProjectWithMPP(
            preApplyCode = {
                publishUklib()
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
                    TestFragment(identifier = "iosArm64Main", attributes = setOf("ios_arm64")),
                    TestFragment(identifier = "iosX64Main", attributes = setOf("ios_x64")),
                    TestFragment(identifier = "appleMain", attributes = setOf("ios_arm64", "ios_x64")),
                    TestFragment(identifier = "iosMain", attributes = setOf("ios_arm64", "ios_x64")),
                    TestFragment(identifier = "nativeMain", attributes = setOf("ios_arm64", "ios_x64")),
                    TestFragment(identifier = "jsMain", attributes = setOf("js_ir")),
                    TestFragment(identifier = "jvmMain", attributes = setOf("jvm")),
                    TestFragment(identifier = "wasmJsMain", attributes = setOf("wasm_js")),
                    TestFragment(identifier = "wasmWasiMain", attributes = setOf("wasm_wasi")),
                    TestFragment(
                        identifier = "commonMain",
                        attributes = setOf("android", "ios_arm64", "ios_x64", "js_ir", "jvm", "wasm_js", "wasm_wasi")
                    ),
                ),
                multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments().map {
                    it.fragment
                }.formCompilationClasspathInConsumingModuleFragment(
                    consumingFragmentAttributes = emptySet(),
                ).map { it.toTestFragment() }.toSet()
            )
        }
    }

    @Test
    fun `project configuration with enabled uklib publication - with external target - emits diagnostic`() {
        buildProjectWithMPP(
            preApplyCode = { publishUklib() }
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
                publishUklib()
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

        buildProjectWithMPP(
            preApplyCode = {
                publishUklib()
                enableCrossCompilation()
            }
        ) {
            kotlin {
                iosArm64()
            }
        }.evaluate().assertNoDiagnostics()
    }

    @Test
    fun `project configuration with enabled uklib publication - with cinterops - emits diagnostic`() {
        buildProjectWithMPP(
            preApplyCode = {
                publishUklib()
            }
        ) {
            kotlin {
                iosArm64().compilations.getByName("main").cinterops.create("foo")
            }
        }.evaluate().assertContainsDiagnostic(
            KotlinToolingDiagnostics.UklibPublicationWithCinterops
        )
    }

    // FIXME: Move source set graph shapes to a separate test
    @Test
    fun `project configuration with enabled uklib publication - multiple same targets - emits diagnostic`() {
        buildProjectWithMPP(
            preApplyCode = {
                publishUklib()
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

    // FIXME: Move source set graph shapes to a separate test
    @Test
    fun `project configuration with enabled uklib publication - under refinement - emits diagnostic`() {
        buildProjectWithMPP(
            preApplyCode = {
                publishUklib()
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

    // FIXME: This is not broken, but we need to check no uklib specific diagnostics are emitted
    //@Test
    fun `project configuration with enabled uklib publication - multirooted graph - is permitted`() {
        buildProjectWithMPP(
            preApplyCode = {
                publishUklib()
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
        }.evaluate().assertNoDiagnostics()
    }

    @Test
    fun `project configuration with enabled uklib publication - source set dependencies`() {
        val p = buildProjectWithMPP(
            preApplyCode = {
                publishUklib()
            }
        ) {
            kotlin {
                sourceSets.commonMain.get().dependencies {
                    implementation("foo:bar:1")
                }
                iosArm64()
            }
        }.evaluate()
        println(p)
//            .multiplatformExtension.iosArm64().internal
//            .compilations
//            .getByName("main").internal.configurations.compileDependencyConfiguration.dependencies
    }

    private data class TestFragment(
        val identifier: String,
        // FIXME: Test transitive refinees
        // val refinees: Set<String>,
        val attributes: Set<String>,
    )

    private suspend fun KotlinMultiplatformExtension.testFragments(): List<TestFragment> = validateKgpModelIsUklibCompliantAndCreateKgpFragments().map {
        it.fragment.toTestFragment()
    }

    private fun UklibFragment.toTestFragment(): TestFragment = TestFragment(
        identifier = identifier,
        attributes = attributes,
    )

}