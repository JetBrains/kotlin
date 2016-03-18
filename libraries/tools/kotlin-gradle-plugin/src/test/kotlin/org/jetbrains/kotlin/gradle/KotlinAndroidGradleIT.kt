package org.jetbrains.kotlin.gradle

import org.junit.Ignore
import org.junit.Test

@Ignore("Requires Android SDK")
class KotlinAndroidGradleIT: BaseGradleIT() {

        override fun defaultBuildOptions() =
                BuildOptions(withDaemon = true, assertThreadLeaks = false)

        @Test
        fun testSimpleCompile() {
            val project = Project("AndroidProject", "2.3")

            project.build("build") {
                assertSuccessful()
                assertContains(":Lib:compileReleaseKotlin",
                               ":compileFlavor1DebugKotlin",
                               ":compileFlavor2DebugKotlin",
                               ":compileFlavor1JnidebugKotlin",
                               ":compileFlavor1ReleaseKotlin",
                               ":compileFlavor2JnidebugKotlin",
                               ":compileFlavor2ReleaseKotlin",
                               ":compileFlavor1Debug",
                               ":compileFlavor2Debug",
                               ":compileFlavor1Jnidebug",
                               ":compileFlavor2Jnidebug",
                               ":compileFlavor1Release",
                               ":compileFlavor2Release")
            }

            // Run the build second time, assert everything is up-to-date
            project.build("build") {
                assertSuccessful()
                assertContains(":Lib:compileReleaseKotlin UP-TO-DATE", ":Lib:compileRelease UP-TO-DATE")
            }

            // Run the build third time, re-run tasks

            project.build("build", "--rerun-tasks") {
                assertSuccessful()
                assertContains(":Lib:compileReleaseKotlin",
                        ":compileFlavor1DebugKotlin",
                        ":compileFlavor2DebugKotlin",
                        ":compileFlavor1JnidebugKotlin",
                        ":compileFlavor1ReleaseKotlin",
                        ":compileFlavor2JnidebugKotlin",
                        ":compileFlavor2ReleaseKotlin",
                        ":compileFlavor1Debug",
                        ":compileFlavor2Debug",
                        ":compileFlavor1Jnidebug",
                        ":compileFlavor2Jnidebug",
                        ":compileFlavor1Release",
                        ":compileFlavor2Release")
            }
        }

    @Test
    fun testModuleNameAndroid() {
        val project = Project("AndroidProject", "2.3")

        project.build("build") {
            assertContains(
                    "args.moduleName = Lib-compileReleaseKotlin",
                    "args.moduleName = Lib-compileDebugKotlin",
                    "args.moduleName = Android-compileFlavor1DebugKotlin",
                    "args.moduleName = Android-compileFlavor2DebugKotlin",
                    "args.moduleName = Android-compileFlavor1JnidebugKotlin",
                    "args.moduleName = Android-compileFlavor2JnidebugKotlin",
                    "args.moduleName = Android-compileFlavor1ReleaseKotlin",
                    "args.moduleName = Android-compileFlavor2ReleaseKotlin")
        }
    }
}