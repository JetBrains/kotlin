/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.BeforeClass
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeLibraryDslWithCocoapodsIT : BaseGradleIT() {

    companion object {
        @BeforeClass
        @JvmStatic
        fun assumeMacHost() {
            Assume.assumeTrue(HostManager.hostIsMac)
        }
    }

    override val defaultGradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun `check registered gradle tasks`() {
        project {
            build(":shared:tasks") {
                assertSuccessful()
                assertTasksRegistered(
                    ":shared:generateMylibPodspec",
                    ":shared:generateMyslibPodspec",
                    ":shared:generateMyframePodspec",
                    ":shared:generateMyfatframePodspec",
                    ":shared:generateSharedPodspec",
                    ":lib:generateGrooframePodspec",
                )
                assertTasksNotRegistered(
                    ":shared:generateMyfatframewithoutpodspecPodspec",
                )
            }
        }
    }

    @Test
    fun `generate podspec when assembling lib`() {
        project {
            build(":shared:assembleMylibSharedLibraryLinuxX64") {
                assertSuccessful()
                assertTasksExecuted(":shared:generateMylibPodspec")
                assertFilesContentEqual("podspecs/mylib.podspec", "/shared/build/out/dynamic/mylib.podspec")
            }
        }
    }

    @Test
    fun `generate podspec when assembling shared lib`() {
        project {
            build(":shared:assembleMyslibSharedLibraryLinuxX64") {
                assertSuccessful()
                assertTasksExecuted(":shared:generateMyslibPodspec")
                assertFilesContentEqual("podspecs/myslib.podspec", "/shared/build/out/dynamic/myslib.podspec")
            }
        }
    }

    @Test
    fun `generate podspec when assembling framework`() {
        project {
            build(":shared:assembleMyframeFrameworkIosArm64") {
                assertSuccessful()
                assertTasksExecuted(":shared:generateMyframePodspec")
                assertFilesContentEqual("podspecs/myframe.podspec", "/shared/build/out/framework/myframe.podspec")
            }
        }
    }

    @Test
    fun `generate podspec when assembling fat framework`() {
        project {
            build(":shared:assembleMyfatframeFatFramework") {
                assertSuccessful()
                assertTasksExecuted(":shared:generateMyfatframePodspec")
                assertFilesContentEqual("podspecs/myfatframe.podspec", "/shared/build/out/fatframework/myfatframe.podspec")
            }
        }
    }

    @Test
    fun `generate podspec when assembling xcframework`() {
        project {
            build(":shared:assembleSharedXCFramework") {
                assertSuccessful()
                assertTasksExecuted(":shared:generateSharedPodspec")
                assertFilesContentEqual("podspecs/shared.podspec", "/shared/build/out/xcframework/shared.podspec")
            }
        }
    }

    @Test
    fun `generate podspec when assembling framework from groovy`() {
        project {
            build(":lib:assembleGrooframeFrameworkIosArm64") {
                assertSuccessful()
                assertTasksExecuted(":lib:generateGrooframePodspec")
                assertFilesContentEqual("podspecs/grooframe.podspec", "/lib/build/out/framework/grooframe.podspec")
            }
        }
    }

    private fun project(block: Project.() -> Unit) {
        transformProjectWithPluginsDsl("new-kn-library-dsl-cocoapods").block()
    }

    private fun CompiledProject.assertFilesContentEqual(expected: String, actual: String) {
        assertFileExists(expected)
        assertFileExists(actual)
        assertEquals(fileInWorkingDir(expected).readText(), fileInWorkingDir(actual).readText())
    }

}