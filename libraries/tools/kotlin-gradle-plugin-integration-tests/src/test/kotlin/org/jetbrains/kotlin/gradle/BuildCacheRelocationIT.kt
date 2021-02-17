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

import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.gradle.util.createTempDir
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class BuildCacheRelocationIT : BaseGradleIT() {

    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(
            withBuildCache = true,
            androidHome = KtTestUtil.findAndroidSdk()
        )

    @Parameterized.Parameter
    lateinit var testCase: TestCase

    val workingDirs = (0..1).map { createTempDir("BuildCacheRelocationIT$it") }

    @Test
    fun testRelocation() = with(testCase) {
        val localBuildCacheDirectory = createTempDir("buildCache$projectName")

        val originalWorkingDir = workingDir

        val (firstProject, secondProject) = (0..1).map { id ->
            workingDir = workingDirs[id]
            Project(projectName, directoryPrefix = projectDirectoryPrefix).apply {
                setupWorkingDir()
                initProject()
                prepareLocalBuildCache(localBuildCacheDirectory)
            }
        }

        try {
            lateinit var firstOutputHashes: List<Pair<File, Int>>

            workingDir = workingDirs[0]
            firstProject.build(
                *testCase.taskToExecute,
                options = defaultBuildOptions().copy(androidGradlePluginVersion = testCase.androidGradlePluginVersion)
            ) {
                assertSuccessful()
                firstOutputHashes = hashOutputFiles(outputRoots)
                cacheableTaskNames.forEach { assertTaskPackedToCache(":$it") }
            }

            workingDir = workingDirs[1]
            val alternateBuildEnvOptions = if (withAnotherGradleHome) {
                val alternateGradleHome = File(firstProject.projectDir.parentFile, "gradleUserHome")
                defaultBuildOptions().copy(
                    gradleUserHome = alternateGradleHome, androidGradlePluginVersion = testCase.androidGradlePluginVersion)
            } else {
                defaultBuildOptions().copy(androidGradlePluginVersion = testCase.androidGradlePluginVersion)
            }
            secondProject.build(*testCase.taskToExecute, options = alternateBuildEnvOptions) {
                assertSuccessful()
                val secondOutputHashes = hashOutputFiles(outputRoots)
                assertEquals(firstOutputHashes, secondOutputHashes)
                cacheableTaskNames.forEach { assertContains(":$it FROM-CACHE") }
            }
        } finally {
            workingDir = originalWorkingDir
            workingDirs.forEach { it.deleteRecursively() }
        }
    }

    class TestCase(
        val projectName: String,
        val cacheableTaskNames: List<String>,
        val projectDirectoryPrefix: String? = null,
        val outputRootPaths: List<String> = listOf("build"),
        val initProject: Project.() -> Unit = {},
        val taskToExecute: Array<String>,
        val withAnotherGradleHome: Boolean = false,
        val androidGradlePluginVersion: AGPVersion? = null
    ) {

        override fun toString(): String = (projectDirectoryPrefix?.plus("/") ?: "") + projectName

        val CompiledProject.outputRoots
            get() = outputRootPaths.map { outputRoot ->
                File(project.projectDir, outputRoot)
            }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "project: {0}")
        fun testCases(): List<Array<TestCase>> = listOf(
            TestCase(
                "simpleProject",
                taskToExecute = arrayOf("classes", "testClasses"),
                cacheableTaskNames = listOf("compileKotlin", "compileTestKotlin")
            ),
            TestCase("simple",
                     projectDirectoryPrefix = "kapt2",
                     taskToExecute = arrayOf("classes", "testClasses"),
                     cacheableTaskNames = listOf(
                         "kaptKotlin", "kaptGenerateStubsKotlin", "compileKotlin", "compileTestKotlin", "compileJava"
                     ),
                     initProject = { File(projectDir, "build.gradle").appendText("\nkapt.useBuildCache = true") },
                     withAnotherGradleHome = true
            ),
            TestCase("kotlin2JsDceProject",
                     taskToExecute = arrayOf("assemble", "runDceKotlinJs"),
                     cacheableTaskNames = listOf("mainProject", "libraryProject").map { "$it:compileKotlin2Js" } +
                             "mainProject:runDceKotlinJs",
                     initProject = {
                         // Fix the problem that the destinationDir of the compile task (i.e. buildDir) contains files from other tasks:
                         File(projectDir, "mainProject/build.gradle").modify { it.replace("/exampleapp.js", "/web/exampleapp.js") }
                         File(projectDir, "libraryProject/build.gradle").modify { it.replace("/examplelib.js", "/web/examplelib.js") }
                         // Fix assembling the JAR from the whole buildDir
                         File(projectDir, "libraryProject/build.gradle").modify {
                             it.replace("from buildDir", "from compileKotlin2Js.destinationDir")
                         }
                     }
            ),
            TestCase("multiplatformProject",
                     taskToExecute = arrayOf("classes", "testClasses"),
                     cacheableTaskNames = listOf(
                         "lib:compileKotlinCommon", "libJvm:compileKotlin", "libJvm:compileTestKotlin",
                         "libJs:compileKotlin2Js", "libJs:compileTestKotlin2Js"
                     ),
                     outputRootPaths = listOf("lib", "libJvm", "libJs").map { "$it/build" }
            ),
            TestCase("AndroidProject",
                     taskToExecute = arrayOf("assembleDebug"),
                     cacheableTaskNames = listOf("Lib", "Android").flatMap { module ->
                         listOf("Flavor1", "Flavor2").flatMap { flavor ->
                             listOf("Debug").map { buildType ->
                                 "$module:compile$flavor${buildType}Kotlin"
                             }
                         }
                     },
                     outputRootPaths = listOf("Lib", "Android", "Test").map { "$it/build" },
                     androidGradlePluginVersion = AGPVersion.v3_6_0
            ),
            TestCase("android-dagger",
                     taskToExecute = arrayOf("assembleDebug"),
                     projectDirectoryPrefix = "kapt2",
                     cacheableTaskNames = listOf("Debug").flatMap { buildType ->
                         listOf("kapt", "kaptGenerateStubs", "compile").map { kotlinTask ->
                             "app:$kotlinTask${buildType}Kotlin"
                         }
                     },
                     outputRootPaths = listOf("app/build"),
                     initProject = { File(projectDir, "app/build.gradle").appendText("\nkapt.useBuildCache = true") },
                     androidGradlePluginVersion = AGPVersion.v3_6_0
            ),
            TestCase("native-build-cache",
                     taskToExecute = arrayOf("build-cache-lib:publish", "build-cache-app:assemble"),
                     cacheableTaskNames = listOf("build-cache-lib:compileKotlinHost", "build-cache-app:compileKotlinHost",
                                                 "build-cache-app:lib-module:compileKotlinHost",
                                                 "build-cache-app:linkDebugStaticHost", "build-cache-app:linkDebugSharedHost"),
                     outputRootPaths = listOf("build-cache-app/build", "build-cache-lib/build", "build-cache-app/lib-module/build"),
                     initProject = {
                         val localRepoUri = projectDir.resolve("repo").toURI()
                         val buildKtsApp = projectDir.resolve("build-cache-app").resolve("build.gradle.kts")
                         val buildKtsLib = projectDir.resolve("build-cache-lib").resolve("build.gradle.kts")
                         buildKtsApp.appendText("\nrepositories { maven { setUrl(\"$localRepoUri\") } }")
                         buildKtsApp.modify(::transformBuildScriptWithPluginsDsl)
                         buildKtsLib.modify(::transformBuildScriptWithPluginsDsl)
                     },
                     withAnotherGradleHome = true
            ),
        ).map { arrayOf(it) }
    }

    private val outputExtensions = setOf("java", "kt", "class", "js", "kotlin_module")

    fun hashOutputFiles(directories: List<File>) =
        directories.flatMap { dir ->
            dir.walkTopDown()
                .filter { it.extension in outputExtensions }
                .map { it.relativeTo(dir) to it.readBytes().contentHashCode() }
                .toList()
        }
}
