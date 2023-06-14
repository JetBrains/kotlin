/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.DisplayName

@DisplayName("K/N tests for hierarchical commonizer")
@NativeGradlePluginTests
open class CommonizerHierarchicalIT : KGPBaseTest() {

    @DisplayName("Commonize hierarchically metadata compilations")
    @GradleTest
    fun testCommonizeHierarchicallyMetadataCompilations(gradleVersion: GradleVersion) {
        nativeProject("commonizeHierarchically", gradleVersion) {
            if (HostManager.hostIsMac) {
                build(":p1:compileIosMainKotlinMetadata") {
                    assertFileInProjectExists("p1/build/classes/kotlin/metadata/iosMain/klib/p1_iosMain.klib")
                    assertNoDuplicateLibraryWarning()
                }

                build(":p1:compileAppleMainKotlinMetadata") {
                    assertFileInProjectExists("p1/build/classes/kotlin/metadata/appleMain/klib/p1_appleMain.klib")
                    assertNoDuplicateLibraryWarning()
                }
            }

            build(":p1:compileLinuxMainKotlinMetadata") {
                assertFileInProjectExists("p1/build/classes/kotlin/metadata/linuxMain/klib/p1_linuxMain.klib")
                assertNoDuplicateLibraryWarning()
            }

            build(":p1:compileAppleAndLinuxMainKotlinMetadata") {
                assertFileInProjectExists("p1/build/classes/kotlin/metadata/appleAndLinuxMain/klib/p1_appleAndLinuxMain.klib")
                assertNoDuplicateLibraryWarning()
            }

            build(":p1:compileNativeMainKotlinMetadata") {
                assertFileInProjectExists("p1/build/classes/kotlin/metadata/nativeMain/klib/p1_nativeMain.klib")
                assertNoDuplicateLibraryWarning()
            }

            build(":p1:compileConcurrentMainKotlinMetadata") {
                assertDirectoryInProjectExists("p1/build/classes/kotlin/metadata/concurrentMain/default")
                assertNoDuplicateLibraryWarning()
            }
        }
    }

    @DisplayName("Commonize hierarchically Klibrary compilations")
    @GradleTest
    fun testCommonizeHierarchicallyKlibraryCompilations(gradleVersion: GradleVersion) {
        nativeProject("commonizeHierarchically", gradleVersion) {
            if (HostManager.hostIsMac) {
                build(":p1:iosArm64MainKlibrary", ":p1:iosX64MainKlibrary", ":p1:macosX64MainKlibrary", ":p1:macosArm64MainKLibrary") {
                    assertFileInProjectExists("p1/build/classes/kotlin/iosArm64/main/klib/p1.klib")
                    assertFileInProjectExists("p1/build/classes/kotlin/iosX64/main/klib/p1.klib")
                    assertFileInProjectExists("p1/build/classes/kotlin/macosX64/main/klib/p1.klib")
                    assertFileInProjectExists("p1/build/classes/kotlin/macosArm64/main/klib/p1.klib")
                    assertNoDuplicateLibraryWarning()
                }
            }

            build(":p1:linuxX64MainKlibrary", ":p1:linuxArm64MainKlibrary") {
                assertFileInProjectExists("p1/build/classes/kotlin/linuxX64/main/klib/p1.klib")
                assertFileInProjectExists("p1/build/classes/kotlin/linuxArm64/main/klib/p1.klib")
                assertNoDuplicateLibraryWarning()
            }

            build(":p1:mingwX64MainKlibrary") {
                assertFileInProjectExists("p1/build/classes/kotlin/mingwX64/main/klib/p1.klib")
                assertNoDuplicateLibraryWarning()
            }
        }
    }

    @DisplayName("Commonize hierarchically multi module")
    @GradleTest
    fun testCommonizeHierarchicallyMultiModule(gradleVersion: GradleVersion) {
        nativeProject("commonizeHierarchicallyMultiModule", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(":p1:commonizeCInterop")
                assertTasksExecuted(":p2:commonizeCInterop")
                assertTasksExecuted(":p3:commonizeCInterop")

                /*
                Before we published commonized cinterops, the compiler would emit a warning like
                `"w: Could not find \"commonizeHierarchicallyMultiModule:p1-cinterop-withPosix\" in "`
                Since commonized cinterops are published now, we expect no such 'Could not find' message anymore
                 */
                assertOutputDoesNotContain("Could not find")
            }
        }
    }

    @DisplayName("Platform dependencies on leaf source sets")
    @GradleTest
    fun testPlatformDependenciesOnLeafSourceSets(gradleVersion: GradleVersion) {
        nativeProject("commonizeHierarchicallyPlatformDependencies", gradleVersion) {
            build(":checkPlatformDependencies") {
                val klibPlatform = "${File.separator}klib${File.separator}platform${File.separator}".replace("\\", "\\\\")

                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksExecuted(":checkLinuxX64MainPlatformDependencies")
                assertTasksExecuted(":checkLinuxArm64MainPlatformDependencies")
                assertOutputContains(Regex(""".*linuxX64Main.*$klibPlatform.*[Pp]osix.*"""))
                assertOutputContains(Regex(""".*linuxArm64Main.*$klibPlatform.*[Pp]osix.*"""))
            }
        }
    }

    @DisplayName("KT-50592 - isolated jvm subproject - should not fail commonization")
    @GradleTest
    fun testIsolatedJvmSubprojectShouldNotFailCommonization(gradleVersion: GradleVersion) {
        nativeProject("commonize-kt-50592-withIsolatedJvmSubproject", gradleVersion = gradleVersion) {
            build("commonize") {
                assertTasksExecuted(":commonizeNativeDistribution")
            }
        }
    }

    @DisplayName("KT-51332 - optimistic commonization")
    @GradleTest
    fun testOptimisticCommonization(gradleVersion: GradleVersion) {
        nativeProject("optimisticCommonization", gradleVersion = gradleVersion) {
            build(":compileCommonMainKotlinMetadata") {
                assertOutputDoesNotContain("Unresolved reference")
            }
        }
    }

    @DisplayName("KT-52050 - DIR retains CPointed supertype")
    @GradleTest
    fun testDIRRetainsCPointedSupertype(gradleVersion: GradleVersion) {
        nativeProject("commonize-kt-52050-DIR-supertype", gradleVersion = gradleVersion) {
            build(":compileCommonMainKotlinMetadata") {
                assertOutputDoesNotContain("Unresolved reference")
            }
        }
    }

    private fun BuildResult.assertNoDuplicateLibraryWarning() = assertOutputDoesNotContain("library included more than once")
}
