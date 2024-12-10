/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.Fragment
import org.jetbrains.kotlin.gradle.artifacts.uklibsPublication.validateKgpModelIsUklibCompliantAndCreateKgpFragments
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.formCompilationClasspathInConsumingModuleFragment
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.ERROR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.WARNING
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `uklib and GMT fragments - with multiple targets and a bamboo structure`() {
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
                    listOf(
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
                    multiplatformExtension.testFragments()
                )

                assertEquals(
                    listOf(
                        TestFragment(
                            "iosArm64Main",
                            setOf("ios_arm64"),
                        ),
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
                    ),
                    multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments().map { it.fragment }.formCompilationClasspathInConsumingModuleFragment(
                        consumingFragmentAttributes = setOf("ios_arm64"),
                    ).map { it.toTestFragment() },
                )

                KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
                assertEquals(
                    setOf("ios_arm64", "ios_x64"),
                    project.locateOrRegisterMetadataDependencyTransformationTask(
                        multiplatformExtension.sourceSets.getByName("customIosMain")
                    ).get().transformationParameters.uklibFragmentAttributes
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
                listOf(
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
                multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments().map { it.fragment }.formCompilationClasspathInConsumingModuleFragment(
                    consumingFragmentAttributes = emptySet(),
                ).map { it.toTestFragment() }
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
        }.evaluate().assertContainsDiagnostic(
            KotlinToolingDiagnostics.UklibFragmentFromUnexpectedTarget
        )
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
            KotlinToolingDiagnostics.UklibPublicationWithoutCrossCompilation(if (HostManager.hostIsMac) WARNING else ERROR)
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

    @Test
    fun `project configuration with enabled uklib publication - multirooted graph - emits diagnostic`() {
        val p = buildProjectWithMPP(
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
        }.evaluate()
        println(p.multiplatformExtension.sourceSets)
//            .assertContainsDiagnostic(
//            KotlinToolingDiagnostics.MultipleSourceSetRootsInCompilation
//        )
    }

    private data class TestFragment(
        val identifier: String,
        val attributes: Set<String>,
    )

    private suspend fun KotlinMultiplatformExtension.testFragments(): List<TestFragment> = validateKgpModelIsUklibCompliantAndCreateKgpFragments().map {
        it.fragment.toTestFragment()
    }

    private fun Fragment.toTestFragment(): TestFragment = TestFragment(
        identifier,
        attributes,
    )

}