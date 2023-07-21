/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.targetFromPresetInternal
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmWithJavaTargetPreset
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.checkDiagnosticsWithMppProject
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.Test

class MppDiagnosticsFunctionalTest {

    @Test
    fun testCommonMainOrTestWithDependsOn() {
        checkDiagnosticsWithMppProject("commonMainOrTestWithDependsOn") {
            kotlin {
                jvm()
                linuxX64()
                applyDefaultHierarchyTemplate()

                sourceSets.apply {
                    val myCustomCommonMain = create("myCustomCommonMain")
                    val myCustomCommonMain2 = create("myCustomCommonMain2")

                    commonMain {
                        dependsOn(myCustomCommonMain)
                        dependsOn(myCustomCommonMain2) // check that diagnostic isn't duplicated
                    }

                    val myCustomCommonTest = create("myCustomCommonTest")
                    val myCustomCommonTest2 = create("myCustomCommonTest2")
                    commonTest {
                        dependsOn(myCustomCommonTest)
                        dependsOn(myCustomCommonTest2) // check that diagnostic isn't duplicated
                    }
                }
            }
        }
    }

    @Test
    fun testDeprecatedJvmWithJavaPreset() {
        checkDiagnosticsWithMppProject("deprecatedJvmWithJavaPreset") {
            kotlin {
                @Suppress("DEPRECATION")
                targetFromPresetInternal(presets.getByName(KotlinJvmWithJavaTargetPreset.PRESET_NAME))
            }
        }
    }

    @Test
    fun testUnusedSourceSets() {
        checkDiagnosticsWithMppProject("unusedSourceSets") {
            kotlin {
                jvm()
                linuxX64()
                applyDefaultHierarchyTemplate()

                sourceSets.apply {
                    val unused1 = create("unused1")
                    // Check that dependsOn doesn't make source set "used"
                    create("unused2").dependsOn(unused1)
                    // Check that depending on used source sets doesn't make source set "used"
                    create("unusedWithDependsOnUsed").dependsOn(commonMain.get())

                    // Check that custom intermediate source set isn't reported as unused
                    val intermediate = create("intermediate")
                    jvmMain.get().dependsOn(intermediate)
                    intermediate.dependsOn(commonMain.get())
                }
            }
        }
    }

    @Test
    fun unusedSourceSetsAndroid() {
        checkDiagnosticsWithMppProject("unusedSourceSetsAndroid") {
            androidLibrary {
                compileSdk = 32

                // Check that source sets of custom build types are not reported as unused
                buildTypes {
                    create("staging")
                }

                // Check that source sets of custom product flavors are not reported as unused
                flavorDimensions += "version"
                productFlavors {
                    create("demo")
                    create("paid")
                }
            }

            kotlin {
                androidTarget()
                linuxX64()
                applyDefaultHierarchyTemplate()

                sourceSets.apply {
                    val intermediateBetweenAndroid = create("intermediate")
                    androidMain.get().dependsOn(intermediateBetweenAndroid)
                    intermediateBetweenAndroid.dependsOn(commonMain.get())
                }
            }
        }
    }

    @Test
    fun testNoTargetsDeclared() {
        checkDiagnosticsWithMppProject("noTargetsDeclared") {
            kotlin { }
        }
    }

    @Test
    fun testKotlinCompilationSourceDeprecation() {
        checkDiagnosticsWithMppProject("kotlinCompilationSourceDeprecation") {
            kotlin {
                val customMain = sourceSets.create("customMain")
                @Suppress("DEPRECATION")
                jvm().compilations.create("custom").source(customMain)
            }
        }
    }
}
