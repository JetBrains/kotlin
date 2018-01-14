/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.plugin.EXPECTED_BY_CONFIG_NAME
import org.jetbrains.kotlin.gradle.plugin.IMPLEMENT_CONFIG_NAME
import org.jetbrains.kotlin.gradle.plugin.IMPLEMENT_DEPRECATION_WARNING
import org.jetbrains.kotlin.gradle.util.allKotlinFiles
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class MultiplatformGradleIT : BaseGradleIT() {
    companion object {
        private const val GRADLE_VERSION = "4.2"
    }

    @Test
    fun testMultiplatformCompile() {
        val project = Project("multiplatformProject", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertContains(":lib:compileKotlinCommon",
                           ":lib:compileTestKotlinCommon",
                           ":libJvm:compileKotlin",
                           ":libJvm:compileTestKotlin",
                           ":libJs:compileKotlin2Js",
                           ":libJs:compileTestKotlin2Js")
            assertFileExists("lib/build/classes/kotlin/main/foo/PlatformClass.kotlin_metadata")
            assertFileExists("lib/build/classes/kotlin/test/foo/PlatformTest.kotlin_metadata")
            assertFileExists("libJvm/build/classes/kotlin/main/foo/PlatformClass.class")
            assertFileExists("libJvm/build/classes/kotlin/test/foo/PlatformTest.class")
            assertFileExists("libJs/build/classes/kotlin/main/libJs.js")
            assertFileExists("libJs/build/classes/kotlin/test/libJs_test.js")
        }
    }

    @Test
    fun testDeprecatedImplementWarning() {
        val project = Project("multiplatformProject", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertNotContains(IMPLEMENT_DEPRECATION_WARNING)
        }

        project.projectDir.walk().filter { it.name == "build.gradle" }.forEach { buildGradle ->
            buildGradle.modify { it.replace(EXPECTED_BY_CONFIG_NAME, IMPLEMENT_CONFIG_NAME) }
        }

        project.build("build") {
            assertSuccessful()
            assertContains(IMPLEMENT_DEPRECATION_WARNING)
        }
    }

    @Test
    fun testCommonKotlinOptions() {
        with(Project("multiplatformProject", GRADLE_VERSION)) {
            setupWorkingDir()

            File(projectDir, "lib/build.gradle").appendText(
                    "\ncompileKotlinCommon.kotlinOptions.freeCompilerArgs = ['-Xno-inline']" +
                    "\ncompileKotlinCommon.kotlinOptions.suppressWarnings = true")

            build("build") {
                assertSuccessful()
                assertContains("-Xno-inline")
                assertContains("-nowarn")
            }
        }
    }

    @Test
    fun testSubprojectWithAnotherClassLoader() {
        with(Project("multiplatformProject", GRADLE_VERSION)) {
            setupWorkingDir()

            // Make sure there is a plugin applied with the plugins DSL, so that Gradle loads the
            // plugins separately for the subproject, with a different class loader:
            File(projectDir, "libJs/build.gradle").modify {
                "plugins { id 'com.moowork.node' version '1.0.1' }" + "\n" + it
            }

            // Remove the root project buildscript dependency, needed for the same purpose:
            File(projectDir, "build.gradle").modify {
                it.replace("classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:\$kotlin_version\"", "")
                        .apply { assert(!equals(it)) }
            }

            // Instead, add the dependencies directly to the subprojects buildscripts:
            listOf("lib", "libJvm", "libJs").forEach { subDirectory ->
                File(projectDir, "$subDirectory/build.gradle").modify {
                    """
                    buildscript {
                        repositories { mavenLocal(); jcenter() }
                        dependencies {
                            classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version"
                        }
                    }
                    """.trimIndent() + "\n" + it
                }
            }

            build("build") {
                assertSuccessful()
            }
        }
    }

    // todo: also make incremental compilation test
    @Test
    fun testIncrementalBuild(): Unit = Project("multiplatformProject", GRADLE_VERSION).run {
        val compileCommonTask = ":lib:compileKotlinCommon"
        val compileJsTask = ":libJs:compileKotlin2Js"
        val compileJvmTask = ":libJvm:compileKotlin"
        val allKotlinTasks = listOf(compileCommonTask, compileJsTask, compileJvmTask)

        build("build") {
            assertSuccessful()
        }

        val commonProjectDir = File(projectDir, "lib")
        commonProjectDir.getFileByName("PlatformClass.kt").modify { it + "\n" }
        build("build") {
            assertSuccessful()
            assertTasksExecuted(allKotlinTasks)
        }

        val jvmProjectDir = File(projectDir, "libJvm")
        jvmProjectDir.getFileByName("PlatformClass.kt").modify { it + "\n" }
        build("build") {
            assertSuccessful()
            assertTasksExecuted(listOf(compileJvmTask))
            assertTasksUpToDate(listOf(compileCommonTask, compileJsTask))
        }

        val jsProjectDir = File(projectDir, "libJs")
        jsProjectDir.getFileByName("PlatformClass.kt").modify { it + "\n" }
        build("build") {
            assertSuccessful()
            assertTasksExecuted(listOf(compileJsTask))
            assertTasksUpToDate(listOf(compileCommonTask, compileJvmTask))
        }
    }
}