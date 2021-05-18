/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.internal.os.OperatingSystem
import org.junit.Test

class CommonizerHierarchicalIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun `test commonizeHierarchically metadata compilations`() {
        with(Project("commonizeHierarchically")) {
            if (Os.canCompileApple) {
                build(":p1:compileIosMainKotlinMetadata") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/metadata/iosMain/klib/p1_iosMain.klib")
                }

                build(":p1:compileAppleMainKotlinMetadata") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/metadata/appleMain/klib/p1_appleMain.klib")
                }
            }

            if (Os.canCompileLinux) {
                build(":p1:compileLinuxMainKotlinMetadata") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/metadata/linuxMain/klib/p1_linuxMain.klib")
                }
            }

            if (Os.canCompileWindows) {
                build(":p1:compileWindowsMainKotlinMetadata") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/metadata/windowsMain/klib/p1_windowsMain.klib")
                }
            }

            if (Os.canCompileApple || Os.canCompileLinux) {
                build(":p1:compileAppleAndLinuxMainKotlinMetadata") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/metadata/appleAndLinuxMain/klib/p1_appleAndLinuxMain.klib")
                }
            }

            if (Os.canCompileApple || Os.canCompileLinux || Os.canCompileWindows) {
                build(":p1:compileNativeMainKotlinMetadata") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/metadata/nativeMain/klib/p1_nativeMain.klib")
                }
            }
        }
    }

    @Test
    fun `test commonizeHierarchically Klibrary compilations`() {
        with(Project("commonizeHierarchically")) {
            if (Os.canCompileApple) {
                build(":p1:iosArm64MainKlibrary", ":p1:iosX64MainKlibrary", ":p1:macosX64MainKlibrary", ":p1:macosArm64MainKLibrary") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/iosArm64/main/klib/p1.klib")
                    assertFileExists("p1/build/classes/kotlin/iosX64/main/klib/p1.klib")
                    assertFileExists("p1/build/classes/kotlin/macosX64/main/klib/p1.klib")
                    assertFileExists("p1/build/classes/kotlin/macosArm64/main/klib/p1.klib")
                }
            }

            if (Os.canCompileLinux) {
                build(":p1:linuxX64MainKlibrary", ":p1:linuxArm64MainKlibrary") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/linuxX64/main/klib/p1.klib")
                    assertFileExists("p1/build/classes/kotlin/linuxArm64/main/klib/p1.klib")
                }
            }

            if (Os.canCompileWindows) {
                build(":p1:windowsX64MainKlibrary", ":p1:windowsX86MainKlibrary") {
                    assertSuccessful()
                    assertFileExists("p1/build/classes/kotlin/windowsX64/main/klib/p1.klib")
                    assertFileExists("p1/build/classes/kotlin/windowsX86/main/klib/p1.klib")
                }
            }
        }
    }

    @Test
    fun `test commonizeHierarchicallyMultiModule`() {
        with(Project("commonizeHierarchicallyMultiModule")) {
            build(
                "assemble",
                // https://youtrack.jetbrains.com/issue/KT-46279
                options = BuildOptions(warningMode = WarningMode.All)
            ) {
                assertSuccessful()
                assertTasksExecuted(":p1:commonizeCInterop")
                assertTasksExecuted(":p2:commonizeCInterop")
                assertTasksExecuted(":p3:commonizeCInterop")

                /*
                Commonized C-Interops are not published or forwarded to other Gradle projects.
                The missing dependency will be ignored by the metadata compiler.
                We still expect a warning being printed.
                 */
                assertContains("w: Could not find \"commonizeHierarchicallyMultiModule:p1-cinterop-withPosix\" in ")
            }
        }
    }

    private object Os {
        private val os = OperatingSystem.current()
        val canCompileApple get() = os.isMacOsX
        val canCompileLinux get() = os.isLinux || os.isMacOsX
        val canCompileWindows get() = os.isWindows
    }
}
