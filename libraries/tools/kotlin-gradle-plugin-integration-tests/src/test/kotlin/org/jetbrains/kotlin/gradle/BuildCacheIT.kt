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
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.capitalize
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

@ExperimentalPathApi
@DisplayName("Local build cache")
@JvmGradlePluginTests
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

            val sourceFile = kotlinSourcesDir().resolve("helloWorld.kt")
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

    @DisplayName("Enabled statistic should not break build cache")
    @GradleTest
    fun testCacheWithStatistic(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            build(
                ":assemble"
            ) {
                assertTasksPackedToCache(":compileKotlin")
            }

            build(
                "clean", ":assemble",
                buildOptions = defaultBuildOptions.copy(buildReport = listOf(BuildReportType.FILE))
            ) {
                assertTasksFromCache(":compileKotlin")
            }
        }
    }

    @DisplayName("Changing native toolchain location should not break build cache")
    @GradleTest
    fun testNativeToolchainWithBuildCache(gradleVersion: GradleVersion, @TempDir customNativeHomePath: Path) {
        nativeProject("native-simple-project", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            val buildOptionsBeforeCaching = defaultBuildOptions.copy(
                nativeOptions = super.defaultBuildOptions.nativeOptions.copy(
                    version = TestVersions.Kotlin.STABLE_RELEASE,
                    distributionDownloadFromMaven = true
                )
            )
            val nativeCompileTask = ":compileKotlin${HostManager.host.presetName.capitalize()}"
            build(nativeCompileTask, buildOptions = buildOptionsBeforeCaching) {
                assertTasksPackedToCache(nativeCompileTask)
            }

            val buildOptionsAfterCaching = buildOptionsBeforeCaching.copy(
                konanDataDir = customNativeHomePath,
            )

            build("clean", nativeCompileTask, buildOptions = buildOptionsAfterCaching) {
                assertTasksFromCache(nativeCompileTask)
            }
        }
    }

    @DisplayName("Restore from build cache should not break incremental compilation")
    @GradleTest
    fun testIncrementalCompilationAfterCacheHit(gradleVersion: GradleVersion) {
        project("incrementalMultiproject", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)
            build("assemble")
            build("clean", "assemble") {
                assertTasksFromCache(":lib:compileKotlin")
                assertTasksFromCache(":app:compileKotlin")
            }
            val bKtSourceFile = projectPath.resolve("lib/src/main/kotlin/bar/B.kt")

            bKtSourceFile.modify { it.replace("fun b() {}", "fun b() {}\nfun b2() {}") }
            val affectedAppSourceFile = projectPath.resolve("app/src/main/kotlin/foo/BB.kt")

            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertIncrementalCompilation(
                    expectedCompiledKotlinFiles = relativeToProject(listOf(bKtSourceFile, affectedAppSourceFile))
                )
            }
        }
    }

    @DisplayName("Restore from build cache and consequent compilation error should not break incremental compilation")
    @GradleTest
    fun testIncrementalCompilationAfterCacheHitAndCompilationError(gradleVersion: GradleVersion) {
        project("incrementalMultiproject", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)
            build("assemble")
            build("clean", "assemble") {
                assertTasksFromCache(":lib:compileKotlin")
                assertTasksFromCache(":app:compileKotlin")
            }
            val bKtSourceFile = projectPath.resolve("lib/src/main/kotlin/bar/B.kt")

            bKtSourceFile.modify { it.replace("fun b() {}", "fun b() {}\nfun b2) {}") }

            buildAndFail("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksFailed(":lib:compileKotlin")
                assertOutputDoesNotContain("On recompilation full rebuild will be performed")
                val affectedFiles = setOf(
                    bKtSourceFile,
                )
                assertCompiledKotlinSources(affectedFiles.relativizeTo(projectPath), output)
            }

            bKtSourceFile.modify { it.replace("fun b2) {}", "fun b2() {}") }

            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                val affectedFiles = setOf(
                    bKtSourceFile,
                    subProject("app").kotlinSourcesDir().resolve("foo/BB.kt"),
                )
                assertIncrementalCompilation(expectedCompiledKotlinFiles = affectedFiles.relativizeTo(projectPath))
            }
        }
    }

    @DisplayName("A compilation error doesn't break kapt incremental compilation after restoring from build cache")
    @GradleTest
    fun testKaptIncrementalCompilationAfterCacheHitAndCompilationError(gradleVersion: GradleVersion) {
        project("kapt2/kaptAvoidance", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)
            build("assemble")
            build("clean", "assemble") {
                assertTasksFromCache(
                    ":app:kaptGenerateStubsKotlin",
                    ":app:kaptKotlin",
                    ":app:compileKotlin",
                    ":lib:compileKotlin"
                )
            }

            val fileToEdit = projectPath.resolve("app/src/main/kotlin/AppClass.kt")
            fileToEdit.modify { current ->
                val lastBrace = current.lastIndexOf("}")
                current.substring(0, lastBrace) +
                        """
                            internal class InternalAppClass {
                                @example.ExampleAnnotation
                                fun intFunGen() : InternalAppClass {
                                    return 
                                }
                            }
                        }  
                        """.trimIndent()
            }

            buildAndFail("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksFailed(":app:compileKotlin")
                assertOutputDoesNotContain("On recompilation full rebuild will be performed")
                assertCompiledKotlinSources(listOf(projectPath.relativize(fileToEdit)), output)
            }

            fileToEdit.replaceText("return", "return this")

            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertIncrementalCompilation(expectedCompiledKotlinFiles = listOf(projectPath.relativize(fileToEdit)))
                assertCompiledKotlinSourcesHandleKapt(
                    listOf(projectPath.relativize(fileToEdit)),
                    ":app"
                )
            }
        }
    }

    @DisplayName("A compilation error doesn't break kapt incremental compilation in test sources after restoring from build cache")
    @GradleTest
    fun testKaptIncrementalCompilationInTestSourcesAfterCacheHit(gradleVersion: GradleVersion) {
        project("kapt2/kaptAvoidance", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)
            build("build")
            build("clean", "build") {
                assertTasksFromCache(
                    ":app:kaptGenerateStubsTestKotlin",
                    ":app:kaptTestKotlin",
                    ":app:compileTestKotlin"
                )
            }

            val fileToEdit = projectPath.resolve("app/src/main/kotlin/AppClass.kt")
            val testFileToEdit = projectPath.resolve("app/src/test/kotlin/AppClassTest.kt")
            fileToEdit.modify {
                it.replace(
                    "val testVal: String = \"text\"",
                    "var testVal: String = \"text\".plus()"
                )
            }

            buildAndFail("build", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksFailed(":app:compileKotlin")
                assertCompiledKotlinSources(listOf(projectPath.relativize(fileToEdit)), output)
            }

            fileToEdit.modify { it.replace("\"text\".plus()", "\"text\".plus(\"+\")") }
            testFileToEdit.replaceText("\"text\"", "\"text+\"")

            build("build", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {

                assertCompiledKotlinTestSourcesAreHandledByKapt(
                    listOf(projectPath.relativize(testFileToEdit)),
                    ":app"
                )
            }
        }
    }
}
