/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmWithJavaTargetPreset
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test

class MppDiagnosticsFunctionalTest {

    @Test
    fun testKmmLibrary() {
        checkDiagnosticsWithMppProject("kmmLibrary") {
            androidLibrary {
                compileSdk = 32
            }

            kotlin {
                androidTarget()
                ios()
            }
        }
    }

    @Test
    fun testKmmApplication() {
        checkDiagnosticsWithMppProject("kmmApplication") {
            androidApplication {
                compileSdk = 32
            }

            kotlin {
                androidTarget()
                ios()
            }
        }
    }

    @Test
    fun testCommonMainWithDependsOn() {
        checkDiagnosticsWithMppProject("commonMainWithDependsOn") {
            kotlin {
                jvm()
                linuxX64()

                sourceSets.apply {
                    val myCustomCommonMain = create("myCustomCommonMain")
                    val myCustomCommonMain2 = create("myCustomCommonMain2")

                    commonMain {
                        dependsOn(myCustomCommonMain)
                        dependsOn(myCustomCommonMain2) // check that diagnostic isn't duplicated
                    }
                }
            }
        }
    }

    @Test
    fun testDeprecatedJvmWithJavaPreset() {
        checkDiagnosticsWithMppProject("deprecatedJvmWithJavaPreset") {
            kotlin {
                targetFromPreset(presets.getByName(KotlinJvmWithJavaTargetPreset.PRESET_NAME))
            }
        }
    }

    @Test
    fun testUnusedSourceSets() {
        checkDiagnosticsWithMppProject("unusedSourceSets") {
            kotlin {
                jvm()
                linuxX64()

                sourceSets.apply {
                    val unused1 = create("unused1")
                    // Check that dependsOn doesn't make source set "used"
                    create("unused2").dependsOn(unused1)
                    // Check that depending on used source sets doesn't make source set "used"
                    create("unusedWithDependsOnUsed").dependsOn(commonMain)

                    // Check that custom intermediate source set isn't reported as unused
                    val intermediate = create("intermediate")
                    jvmMain.dependsOn(intermediate)
                    intermediate.dependsOn(commonMain)
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

                sourceSets.apply {
                    val intermediateBetweenAndroid = create("intermediate")
                    androidMain.dependsOn(intermediateBetweenAndroid)
                    intermediateBetweenAndroid.dependsOn(commonMain)
                }
            }
        }
    }
}
