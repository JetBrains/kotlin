/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.BaseKotlinGradleTest
import kotlinx.validation.api.assertTaskSuccess
import kotlinx.validation.api.buildGradleKts
import kotlinx.validation.api.resolve
import kotlinx.validation.api.runner
import kotlinx.validation.api.test
import org.assertj.core.api.Assertions
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class SubprojectsWithPluginOnSubTests : BaseKotlinGradleTest() {

    /**
     * Sets up a project hierarchy like this:
     * ```
     * build.gradle.kts (without the plugin)
     * settings.gradle.kts (including refs to 4 subprojects)
     * sub1/
     *    build.gradle.kts (with the plugin)
     *    subsub1/build.gradle.kts
     *    subsub2/build.gradle.kts
     * sub2/build.gradle.kts
     * ```
     */
    private fun BaseKotlinScope.createProjectHierarchyWithPluginOnSub1() {
        settingsGradleKts {
            resolve("examples/gradle/settings/settings-with-hierarchy.gradle.kts")
        }
        buildGradleKts {
            resolve("examples/gradle/base/withoutPlugin.gradle.kts")
        }
        dir("sub1") {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin-noKotlinVersion.gradle.kts")
            }
            dir("subsub1") {
                buildGradleKts {
                    resolve("examples/gradle/base/withoutPlugin-noKotlinVersion.gradle.kts")
                }
            }
            dir("subsub2") {
                buildGradleKts {
                    resolve("examples/gradle/base/withoutPlugin-noKotlinVersion.gradle.kts")
                }
            }
        }
        dir("sub2") {
            buildGradleKts {
                resolve("examples/gradle/base/withoutPlugin-noKotlinVersion.gradle.kts")
            }
        }
    }

    @Test
    fun `apiCheck should be run on all subprojects when running check`() {
        val runner = test {
            createProjectHierarchyWithPluginOnSub1()

            dir("sub1") {
                emptyApiFile(projectName = "sub1")

                dir("subsub1") {
                    emptyApiFile(projectName = "subsub1")
                }

                dir("subsub2") {
                    emptyApiFile(projectName = "subsub2")
                }
            }

            runner {
                arguments.add("check")
            }
        }

        runner.build().apply {
            assertTaskNotRun(":apiCheck")
            assertTaskSuccess(":sub1:apiCheck")
            assertTaskSuccess(":sub1:subsub1:apiCheck")
            assertTaskSuccess(":sub1:subsub2:apiCheck")
            assertTaskNotRun(":sub2:apiCheck")
        }
    }

    @Test
    fun `apiCheck should succeed on all subprojects when api files are empty but there are no Kotlin sources`() {
        val runner = test {
            createProjectHierarchyWithPluginOnSub1()

            dir("sub1") {
                emptyApiFile(projectName = "sub1")

                dir("subsub1") {
                    emptyApiFile(projectName = "subsub1")
                }

                dir("subsub2") {
                    emptyApiFile(projectName = "subsub2")
                }
            }

            runner {
                arguments.add("apiCheck")
            }
        }

        runner.build().apply {
            assertTaskNotRun(":apiCheck")
            assertTaskSuccess(":sub1:apiCheck")
            assertTaskSuccess(":sub1:subsub1:apiCheck")
            assertTaskSuccess(":sub1:subsub2:apiCheck")
            assertTaskNotRun(":sub2:apiCheck")
        }
    }

    @Test
    fun `apiCheck should succeed on subproject, when api file is empty but there are no sources`() {
        val runner = test {
            createProjectHierarchyWithPluginOnSub1()

            dir("sub1") {
                emptyApiFile(projectName = "sub1")
            }

            runner {
                arguments.add(":sub1:apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":sub1:apiCheck")
        }
    }

    @Test
    fun `apiCheck should succeed on sub-subproject, when api file is empty but there are no sources`() {
        val runner = test {
            createProjectHierarchyWithPluginOnSub1()

            dir("sub1") {
                dir("subsub2") {
                    emptyApiFile(projectName = "subsub2")
                }
            }

            runner {
                arguments.add(":sub1:subsub2:apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":sub1:subsub2:apiCheck")
        }
    }

    @Test
    fun `apiCheck should succeed on sub-subproject, when public classes match api file`() {
        val runner = test {
            createProjectHierarchyWithPluginOnSub1()

            dir("sub1") {
                dir("subsub2") {
                    kotlin("Subsub2Class.kt") {
                        resolve("examples/classes/Subsub2Class.kt")
                    }
                    apiFile(projectName = "subsub2") {
                        resolve("examples/classes/Subsub2Class.dump")
                    }
                }
            }

            runner {
                arguments.add(":sub1:subsub2:apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":sub1:subsub2:apiCheck")
        }
    }

    @Test
    fun `apiCheck should succeed on subprojects, when public classes match api files`() {
        val runner = test {
            createProjectHierarchyWithPluginOnSub1()

            dir("sub1") {
                emptyApiFile(projectName = "sub1")

                dir("subsub1") {
                    kotlin("Subsub1Class.kt") {
                        resolve("examples/classes/Subsub1Class.kt")
                    }
                    apiFile(projectName = "subsub1") {
                        resolve("examples/classes/Subsub1Class.dump")
                    }
                }
                dir("subsub2") {
                    kotlin("Subsub2Class.kt") {
                        resolve("examples/classes/Subsub2Class.kt")
                    }
                    apiFile(projectName = "subsub2") {
                        resolve("examples/classes/Subsub2Class.dump")
                    }
                }
            }

            runner {
                arguments.add("apiCheck")
            }
        }

        runner.build().apply {
            assertTaskNotRun(":apiCheck")
            assertTaskSuccess(":sub1:apiCheck")
            assertTaskSuccess(":sub1:subsub1:apiCheck")
            assertTaskSuccess(":sub1:subsub2:apiCheck")
            assertTaskNotRun(":sub2:apiCheck")
        }
    }

    @Test
    fun `apiDump should succeed and create empty api on subproject, when no kotlin files are included in SourceSet`() {
        val runner = test {
            createProjectHierarchyWithPluginOnSub1()

            runner {
                arguments.add(":sub1:apiDump")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":sub1:apiDump")

            val apiDumpFile = rootProjectDir.resolve("sub1/api/sub1.api")
            assertTrue(apiDumpFile.exists(), "api dump file ${apiDumpFile.path} should exist")

            Assertions.assertThat(apiDumpFile.readText()).isEqualToIgnoringNewLines("")
        }
    }

    @Test
    fun `apiDump should succeed and create correct api dumps on subprojects`() {
        val runner = test {
            createProjectHierarchyWithPluginOnSub1()

            dir("sub1") {
                dir("subsub1") {
                    kotlin("Subsub1Class.kt") {
                        resolve("examples/classes/Subsub1Class.kt")
                    }
                }
                dir("subsub2") {
                    kotlin("Subsub2Class.kt") {
                        resolve("examples/classes/Subsub2Class.kt")
                    }
                }
            }

            runner {
                arguments.add("apiDump")
            }
        }

        runner.build().apply {
            assertTaskNotRun(":apiDump")
            assertTaskSuccess(":sub1:apiDump")
            assertTaskSuccess(":sub1:subsub1:apiDump")
            assertTaskSuccess(":sub1:subsub2:apiDump")
            assertTaskNotRun(":sub2:apiDump")

            assertFalse(rootProjectApiDump.exists(), "api dump file ${rootProjectApiDump.path} should NOT exist")

            val apiSub1 = rootProjectDir.resolve("sub1/api/sub1.api")
            assertTrue(apiSub1.exists(), "api dump file ${apiSub1.path} should exist")
            Assertions.assertThat(apiSub1.readText()).isEqualToIgnoringNewLines("")

            val apiSubsub1 = rootProjectDir.resolve("sub1/subsub1/api/subsub1.api")
            assertTrue(apiSubsub1.exists(), "api dump file ${apiSubsub1.path} should exist")
            val apiSubsub1Expected = readFileList("examples/classes/Subsub1Class.dump")
            Assertions.assertThat(apiSubsub1.readText()).isEqualToIgnoringNewLines(apiSubsub1Expected)

            val apiSubsub2 = rootProjectDir.resolve("sub1/subsub2/api/subsub2.api")
            assertTrue(apiSubsub2.exists(), "api dump file ${apiSubsub2.path} should exist")
            val apiSubsub2Expected = readFileList("examples/classes/Subsub2Class.dump")
            Assertions.assertThat(apiSubsub2.readText()).isEqualToIgnoringNewLines(apiSubsub2Expected)

            val apiSub2 = rootProjectDir.resolve("sub2/api/sub2.api")
            assertFalse(apiSub2.exists(), "api dump file ${apiSub2.path} should NOT exist")
        }
    }
}
