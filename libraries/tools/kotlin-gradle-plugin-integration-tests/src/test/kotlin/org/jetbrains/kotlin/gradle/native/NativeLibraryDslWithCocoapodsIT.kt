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
                    ":shared:generateMylibStaticLibraryLinuxX64Podspec",
                    ":shared:generateMyslibSharedLibraryLinuxX64Podspec",
                    ":shared:generateMyframeFrameworkIosArm64Podspec",
                    ":shared:generateMyfatframeFatFrameworkPodspec",
                    ":shared:generateSharedXCFrameworkPodspec",
                    ":lib:generateGrooframeFrameworkIosArm64Podspec",
                    ":lib:generateGrooxcframeXCFrameworkPodspec",
                    ":shared:generateMyframewihtoutpodspecFrameworkIosArm64Podspec",
                    ":lib:generateGrooxcframewithoutpodspecXCFrameworkPodspec",
                )
            }
        }
    }

    @Test
    fun `generate podspec when assembling static lib`() {
        project {
            build(":shared:assembleMylibStaticLibraryLinuxX64") {
                assertSuccessful()
                assertTasksExecuted(":shared:generateMylibStaticLibraryLinuxX64Podspec")
                assertFilesContentEqual("podspecs/mylib.podspec", "/shared/build/out/static/mylib.podspec")
            }
        }
    }

    @Test
    fun `generate podspec when assembling shared lib`() {
        project {
            build(":shared:assembleMyslibSharedLibraryLinuxX64") {
                assertSuccessful()
                assertTasksExecuted(":shared:generateMyslibSharedLibraryLinuxX64Podspec")
                assertFilesContentEqual("podspecs/myslib.podspec", "/shared/build/out/dynamic/myslib.podspec")
            }
        }
    }

    @Test
    fun `not generate podspec when withPodspec is empty`() {
        project {
            build(":shared:assembleMyslibwithoutpodspecSharedLibraryLinuxX64") {
                assertSuccessful()
                assertTasksSkipped(":shared:generateMyslibwithoutpodspecSharedLibraryLinuxX64Podspec")
                assertContains("Skipping task ':shared:generateMyslibwithoutpodspecSharedLibraryLinuxX64Podspec' because there are no podspec attributes defined")
            }
        }
    }

    @Test
    fun `generate podspec when assembling framework`() {
        project {
            build(":shared:assembleMyframeFrameworkIosArm64") {
                assertSuccessful()
                assertTasksExecuted(":shared:generateMyframeFrameworkIosArm64Podspec")
                assertFilesContentEqual("podspecs/myframe.podspec", "/shared/build/out/framework/myframe.podspec")
            }
        }
    }

    @Test
    fun `not generate podspec when there is no withPodspec`() {
        project {
            build(":shared:assembleMyframewihtoutpodspecFrameworkIosArm64") {
                assertSuccessful()
                assertTasksSkipped(":shared:generateMyframewihtoutpodspecFrameworkIosArm64Podspec")
            }
        }
    }

    @Test
    fun `generate podspec when assembling fat framework`() {
        project {
            build(":shared:assembleMyfatframeFatFramework") {
                assertSuccessful()
                assertTasksExecuted(":shared:generateMyfatframeFatFrameworkPodspec")
                assertFilesContentEqual("podspecs/myfatframe.podspec", "/shared/build/out/fatframework/myfatframe.podspec")
            }
        }
    }

    @Test
    fun `generate podspec when assembling xcframework`() {
        project {
            build(":shared:assembleSharedXCFramework") {
                assertSuccessful()
                assertTasksExecuted(":shared:generateSharedXCFrameworkPodspec")
                assertFilesContentEqual("podspecs/shared.podspec", "/shared/build/out/xcframework/shared.podspec")
            }
        }
    }

    @Test
    fun `generate podspec when assembling xcframework from groovy`() {
        project {
            build(":lib:assembleGrooframeFrameworkIosArm64") {
                assertSuccessful()
                assertTasksExecuted(":lib:generateGrooframeFrameworkIosArm64Podspec")
                assertFilesContentEqual("podspecs/grooframe.podspec", "/lib/build/out/framework/grooframe.podspec")
            }
        }
    }

    @Test
    fun `not generate podspec from groovy when withPodspec is empty`() {
        project {
            build(":lib:assembleGrooxcframeXCFramework") {
                assertSuccessful()
                assertTasksSkipped(":lib:generateGrooxcframeXCFrameworkPodspec")
            }
        }
    }

    @Test
    fun `not generate podspec from groovy when there is no withPodspec`() {
        project {
            build(":lib:assembleGrooxcframewithoutpodspecXCFramework") {
                assertSuccessful()
                assertTasksSkipped(":lib:generateGrooxcframewithoutpodspecXCFrameworkPodspec")
            }
        }
    }

    @Test
    fun `generate podspecs when several frameworks have with the same name`() {
        project {
            projectDir.resolve("shared/build.gradle.kts").appendText("""
                kotlinArtifacts {
                    Native.Library {
                         target = linuxX64
                         
                         withPodspec {}
                    }
                }
            """.trimIndent())

            build(":shared:assembleSharedXCFramework") {
                assertSuccessful()
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