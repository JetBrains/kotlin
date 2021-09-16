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

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText
import kotlin.io.path.writeText

@ExperimentalPathApi
@DisplayName("Local build cache")
@SimpleGradlePluginTests
class BuildCacheIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions =
        super.defaultBuildOptions.copy(buildCacheEnabled = true)

    private val localBuildCacheDir get() = workingDir.resolve("custom-jdk-build-cache")

    @DisplayName("kotlin.caching.enabled flag should enable caching for Kotlin tasks")
    @GradleTest
    fun testKotlinCachingEnabledFlag(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            build("assemble") {
                assertTasksPackedToCache(":compileKotlin")
            }

            build("clean", "assemble", "-Dkotlin.caching.enabled=false") {
                assertTasksExecuted(":compileKotlin")
            }
        }
    }

    @DisplayName("Kotlin JVM task should be taken from cache")
    @GradleTest
    fun testCacheHitAfterClean(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            build("assemble") {
                assertTasksPackedToCache(":compileKotlin")
            }

            build("clean", "assemble") {
                assertTasksFromCache(":compileKotlin", ":compileJava")
            }
        }
    }

    @DisplayName("Should correctly handle modification/restoration of source file")
    @GradleTest
    fun testCacheHitAfterCacheHit(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            build("assemble") {
                // Should store the output into the cache:
                assertTasksPackedToCache(":compileKotlin")
            }

            val sourceFile = projectPath.resolve("src/main/kotlin/helloWorld.kt")
            val originalSource: String = sourceFile.readText()
            val modifiedSource: String = originalSource.replace(" and ", " + ")
            sourceFile.writeText(modifiedSource)

            build("assemble") {
                assertTasksPackedToCache(":compileKotlin")
            }

            sourceFile.writeText(originalSource)

            build("assemble") {
                // Should load the output from cache:
                assertTasksFromCache(":compileKotlin")
            }

            sourceFile.writeText(modifiedSource)

            build("assemble") {
                // And should load the output from cache again, without compilation:
                assertTasksFromCache(":compileKotlin")
            }
        }
    }

    @DisplayName("Incremental compilation works with cache")
    @GradleTest
    fun testKotlinCompileIncrementalBuildWithoutRelocation(gradleVersion: GradleVersion) {
        project("buildCacheSimple", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            checkKotlinCompileCachingIncrementalBuild(this, this)
        }
    }

    // TODO: move it into relocatable build cache tests
    @DisplayName("Incremental compilation build cache does not break relocated cache")
    @GradleTest
    fun testKotlinCompileCachingIncrementalBuildWithRelocation(gradleVersion: GradleVersion) {
        val firstProject = project("buildCacheSimple", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)
        }

        val secondProject = project("buildCacheSimple", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)
        }

        checkKotlinCompileCachingIncrementalBuild(firstProject, secondProject)
    }

    // TODO: move it into relocatable build cache tests
    @DisplayName("Kapt incremental compilation works with cache")
    @GradleTest
    fun testKaptCachingIncrementalBuildWithoutRelocation(gradleVersion: GradleVersion) {
        project("kapt2/kaptAvoidance", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            checkKaptCachingIncrementalBuild(this, this)
        }
    }

    // TODO: move it into build cache relocation tests
    @DisplayName("Kapt incremental compilation build does not break relocated build cache")
    @GradleTest
    fun testKaptCachingIncrementalBuildWithRelocation(gradleVersion: GradleVersion) {
        val firstProject = project("kapt2/kaptAvoidance", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)
        }

        val secondProject = project("kapt2/kaptAvoidance", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)
        }

        checkKaptCachingIncrementalBuild(firstProject, secondProject)
    }

    @DisplayName("Debug log level should not break build cache")
    @GradleTest
    fun testDebugLogLevelCaching(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            build(
                ":assemble",
                buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
            ) {
                assertTasksPackedToCache(":compileKotlin")
            }

            build("clean", ":assemble") {
                assertTasksFromCache(":compileKotlin")
            }
        }
    }

    private fun checkKotlinCompileCachingIncrementalBuild(
        firstProject: TestProject,
        secondProject: TestProject
    ) {
        // First build, should be stored into the build cache:
        firstProject.build("assemble") {
            assertTasksPackedToCache(":compileKotlin")
        }

        // A cache hit: a clean build without any changes to the project
        secondProject.build("clean", "assemble") {
            assertTasksFromCache(":compileKotlin")
        }

        // Change the return type of foo() from Int to String in foo.kt, and check that fooUsage.kt is recompiled as well:
        val fooKtSourceFile = secondProject.projectPath.resolve("src/main/kotlin/foo.kt")
        fooKtSourceFile.modify { it.replace("Int = 1", "String = \"abc\"") }
        secondProject.build("assemble", forceOutput = true) {
            assertIncrementalCompilation(modifiedFiles = setOf(fooKtSourceFile))
        }

        // Revert the change to the return type of foo(), and check if we get a cache hit
        fooKtSourceFile.modify { it.replace("String = \"abc\"", "Int = 1") }
        secondProject.build("clean", "assemble") {
            assertTasksFromCache(":compileKotlin")
        }
    }

    private fun checkKaptCachingIncrementalBuild(
        firstProject: TestProject,
        secondProject: TestProject
    ) {
        val options = defaultBuildOptions.copy(
            kaptOptions = BuildOptions.KaptOptions(
                verbose = true,
                useWorkers = false,
                incrementalKapt = true,
                includeCompileClasspath = false
            )
        )

        // First build, should be stored into the build cache:
        firstProject.build("clean", ":app:build", buildOptions = options) {
            assertTasksPackedToCache(":app:kaptGenerateStubsKotlin", ":app:kaptKotlin")
        }

        // A cache hit: a clean build without any changes to the project
        secondProject.build("clean", ":app:build", buildOptions = options) {
            assertTasksFromCache(":app:kaptGenerateStubsKotlin", ":app:kaptKotlin")
        }

        // Make changes to annotated class and check kapt tasks are re-executed
        val appClassKtSourceFile = secondProject.projectPath.resolve("app/src/main/kotlin/AppClass.kt")
        appClassKtSourceFile.modify {
            it.replace("val testVal: String = \"text\"", "val testVal: Int = 1")
        }
        secondProject.build("build", buildOptions = options) {
            assertTasksExecuted(":app:kaptGenerateStubsKotlin", ":app:kaptKotlin")
        }

        // Revert changes and check kapt tasks are from cache
        appClassKtSourceFile.modify {
            it.replace("val testVal: Int = 1", "val testVal: String = \"text\"")
        }
        secondProject.build("clean", "build", buildOptions = options) {
            assertTasksFromCache(":app:kaptGenerateStubsKotlin", ":app:kaptKotlin")
        }
    }
}
