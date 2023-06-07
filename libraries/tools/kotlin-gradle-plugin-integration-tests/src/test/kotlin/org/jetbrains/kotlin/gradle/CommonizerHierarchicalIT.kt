/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Test

open class CommonizerHierarchicalIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun `test commonizeHierarchically metadata compilations`() {

        with(Project("commonizeHierarchically")) {
            if (HostManager.hostIsMac) {
                build(":p1:compileIosMainKotlinMetadata") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/metadata/iosMain/klib/p1_iosMain.klib")
                    assertNoDuplicateLibraryWarning()
                }

                build(":p1:compileAppleMainKotlinMetadata") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/metadata/appleMain/klib/p1_appleMain.klib")
                    assertNoDuplicateLibraryWarning()
                }
            }

            build(":p1:compileLinuxMainKotlinMetadata") {
                assertSuccessful()
                assertFileExists("p1/build/classes/kotlin/metadata/linuxMain/klib/p1_linuxMain.klib")
                assertNoDuplicateLibraryWarning()
            }

            build(":p1:compileMingwMainKotlinMetadata") {
                assertSuccessful()
                assertFileExists("p1/build/classes/kotlin/metadata/mingwMain/klib/p1_mingwMain.klib")
            }

            build(":p1:compileAppleAndLinuxMainKotlinMetadata") {
                assertSuccessful()
                assertFileExists("p1/build/classes/kotlin/metadata/appleAndLinuxMain/klib/p1_appleAndLinuxMain.klib")
                assertNoDuplicateLibraryWarning()
            }

            build(":p1:compileNativeMainKotlinMetadata") {
                assertSuccessful()
                assertFileExists("p1/build/classes/kotlin/metadata/nativeMain/klib/p1_nativeMain.klib")
                assertNoDuplicateLibraryWarning()
            }

            build(":p1:compileConcurrentMainKotlinMetadata") {
                assertSuccessful()
                assertFileExists("p1/build/classes/kotlin/metadata/concurrentMain/default")
                assertNoDuplicateLibraryWarning()
            }
        }
    }

    @Test
    fun `test commonizeHierarchically Klibrary compilations`() {
        with(Project("commonizeHierarchically")) {
            if (HostManager.hostIsMac) {
                build(":p1:iosArm64MainKlibrary", ":p1:iosX64MainKlibrary", ":p1:macosX64MainKlibrary", ":p1:macosArm64MainKLibrary") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/iosArm64/main/klib/p1.klib")
                    assertFileExists("p1/build/classes/kotlin/iosX64/main/klib/p1.klib")
                    assertFileExists("p1/build/classes/kotlin/macosX64/main/klib/p1.klib")
                    assertFileExists("p1/build/classes/kotlin/macosArm64/main/klib/p1.klib")
                    assertNoDuplicateLibraryWarning()
                }
            }

            build(":p1:linuxX64MainKlibrary", ":p1:linuxArm64MainKlibrary") {
                assertSuccessful()
                assertFileExists("p1/build/classes/kotlin/linuxX64/main/klib/p1.klib")
                assertFileExists("p1/build/classes/kotlin/linuxArm64/main/klib/p1.klib")
                assertNoDuplicateLibraryWarning()
            }

            build(":p1:mingwX64MainKlibrary", ":p1:mingwX86MainKlibrary") {
                assertSuccessful()
                assertFileExists("p1/build/classes/kotlin/mingwX64/main/klib/p1.klib")
                assertFileExists("p1/build/classes/kotlin/mingwX86/main/klib/p1.klib")
                assertNoDuplicateLibraryWarning()
            }
        }
    }

    @Test
    fun `test commonizeHierarchicallyMultiModule`() {
        with(Project("commonizeHierarchicallyMultiModule")) {
            build("assemble") {
                assertSuccessful()
                assertTasksExecuted(":p1:commonizeCInterop")
                assertTasksExecuted(":p2:commonizeCInterop")
                assertTasksExecuted(":p3:commonizeCInterop")

                /*
                Before we published commonized cinterops, the compiler would emit a warning like
                `"w: Could not find \"commonizeHierarchicallyMultiModule:p1-cinterop-withPosix\" in "`
                Since commonized cinterops are published now, we expect no such 'Could not find' message anymore
                 */
                assertNotContains("Could not find")
            }
        }
    }

    @Test
    fun `test platform dependencies on leaf source sets`() {
        with(Project("commonizeHierarchicallyPlatformDependencies")) {
            build(":checkPlatformDependencies") {
                val klibPlatform = "${File.separator}klib${File.separator}platform${File.separator}".replace("\\", "\\\\")

                assertSuccessful()
                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksExecuted(":checkLinuxX64MainPlatformDependencies")
                assertTasksExecuted(":checkLinuxArm64MainPlatformDependencies")
                assertContainsRegex(Regex(""".*linuxX64Main.*$klibPlatform.*[Pp]osix.*"""))
                assertContainsRegex(Regex(""".*linuxArm64Main.*$klibPlatform.*[Pp]osix.*"""))
            }
        }
    }

    @Test
    fun `test KT-50592 - isolated jvm subproject - should not fail commonization`() {
        with(Project("commonize-kt-50592-withIsolatedJvmSubproject")) {
            setupWorkingDir(applyAndroidTestFixes = false) // Necessary to ensure separated classpath
            build("commonize") {
                assertSuccessful()
                assertTasksExecuted(":commonizeNativeDistribution")
            }
        }
    }

    @Test
    fun `test KT-51332 optimistic commonization`() {
        with(Project("optimisticCommonization")) {
            build(":compileCommonMainKotlinMetadata") {
                assertNotContains("Unresolved reference")
                assertSuccessful()
            }
        }
    }

    @Test
    fun `test KT-52050 - DIR retains CPointed supertype`() {
        with(Project("commonize-kt-52050-DIR-supertype")) {
            build(":compileCommonMainKotlinMetadata") {
                assertNotContains("Unresolved reference")
                assertSuccessful()
            }
        }
    }

    private fun CompiledProject.assertNoDuplicateLibraryWarning() = assertNotContains("library included more than once")
}
