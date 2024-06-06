/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File
import java.io.InputStreamReader

internal class MultipleJvmTargetsTest : BaseKotlinGradleTest() {
    private fun BaseKotlinScope.createProjectHierarchyWithPluginOnRoot() {
        settingsGradleKts {
            resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
        }
        buildGradleKts {
            resolve("/examples/gradle/base/multiplatformWithJvmTargets.gradle.kts")
        }
    }

    @Test
    fun testApiCheckPasses() {
        val runner = test {
            createProjectHierarchyWithPluginOnRoot()
            runner {
                arguments.add(":apiCheck")
            }

            dir("$API_DIR/jvm/") {
                file("testproject.api") {
                    resolve("/examples/classes/Subsub1Class.dump")
                    resolve("/examples/classes/Subsub2Class.dump")
                }
            }

            dir("$API_DIR/anotherJvm/") {
                file("testproject.api") {
                    resolve("/examples/classes/Subsub1Class.dump")
                }
            }

            dir("src/jvmMain/kotlin") {}
            kotlin("Subsub1Class.kt", "commonMain") {
                resolve("/examples/classes/Subsub1Class.kt")
            }
            kotlin("Subsub2Class.kt", "jvmMain") {
                resolve("/examples/classes/Subsub2Class.kt")
            }

        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
            assertTaskSuccess(":jvmApiCheck")
            assertTaskSuccess(":anotherJvmApiCheck")
        }
    }

    @Test
    fun testApiCheckFails() {
        val runner = test {
            createProjectHierarchyWithPluginOnRoot()
            runner {
                arguments.add("--continue")
                arguments.add(":check")
            }

            dir("$API_DIR/jvm/") {
                file("testproject.api") {
                    resolve("/examples/classes/Subsub2Class.dump")
                    resolve("/examples/classes/Subsub1Class.dump")
                }
            }

            dir("$API_DIR/anotherJvm/") {
                file("testproject.api") {
                    resolve("/examples/classes/Subsub2Class.dump")
                }
            }

            dir("src/jvmMain/kotlin") {}
            kotlin("Subsub1Class.kt", "commonMain") {
                resolve("/examples/classes/Subsub1Class.kt")
            }
            kotlin("Subsub2Class.kt", "jvmMain") {
                resolve("/examples/classes/Subsub2Class.kt")
            }

        }

        runner.buildAndFail().apply {
            assertTaskNotRun(":apiCheck")
            assertTaskFailure(":jvmApiCheck")
            assertTaskFailure(":anotherJvmApiCheck")
            assertThat(output).contains("API check failed for project testproject")
            assertTaskNotRun(":check")
        }
    }

    @Test
    fun testApiDumpPasses() {
        val runner = test {
            createProjectHierarchyWithPluginOnRoot()

            runner {
                arguments.add(":apiDump")
            }

            dir("src/jvmMain/kotlin") {}
            kotlin("Subsub1Class.kt", "commonMain") {
                resolve("/examples/classes/Subsub1Class.kt")
            }
            kotlin("Subsub2Class.kt", "jvmMain") {
                resolve("/examples/classes/Subsub2Class.kt")
            }

        }
        runner.build().apply {
            assertTaskSuccess(":apiDump")
            assertTaskSuccess(":jvmApiDump")
            assertTaskSuccess(":anotherJvmApiDump")

            System.err.println(output)

            val anotherExpectedApi = readFileList("/examples/classes/Subsub1Class.dump")
            assertThat(anotherApiDump.readText()).isEqualToIgnoringNewLines(anotherExpectedApi)

            val mainExpectedApi = anotherExpectedApi + "\n" + readFileList("/examples/classes/Subsub2Class.dump")
            assertThat(jvmApiDump.readText()).isEqualToIgnoringNewLines(mainExpectedApi)
        }
    }

    // Scenario from #233: if there were two targets (and two dumps, correspondingly),
    // removal of one of the targets should trigger validation failure.
    @Test
    fun testValidationAfterTargetRemoval() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
            }
            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }
            // let's pretend, there were multiple targets before
            for (tgtName in listOf("jvm", "anotherJvm")) {
                dir("$API_DIR/$tgtName/") {
                    file("${rootProjectDir.name.lowercase()}.api") {
                        resolve("/examples/classes/AnotherBuildConfig.dump")
                    }
                }
            }
            runner {
                arguments.add(":apiCheck")
            }
        }
        runner.buildAndFail().apply {
            assertTaskFailure(":apiCheck")
        }
    }

    private val jvmApiDump: File get() = rootProjectDir.resolve("$API_DIR/jvm/testproject.api")
    private val anotherApiDump: File get() = rootProjectDir.resolve("$API_DIR/anotherJvm/testproject.api")

}
