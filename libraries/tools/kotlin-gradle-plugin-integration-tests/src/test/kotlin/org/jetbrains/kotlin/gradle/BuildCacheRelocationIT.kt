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
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

@DisplayName("Build cache relocation")
class BuildCacheRelocationIT : KGPBaseTest() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(buildCacheEnabled = true)

    private val localBuildCacheDir get() = workingDir.resolve("remote-jdk-build-cache")

    @JvmGradlePluginTests
    @DisplayName("works for Kotlin simple project")
    @GradleTest
    fun testRelocationSimpleProject(gradleVersion: GradleVersion) {
        val (firstProject, secondProject) = prepareTestProjects("simpleProject", gradleVersion)

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf(":classes", ":testClasses"),
            listOf(":compileKotlin", ":compileTestKotlin")
        )
    }

    @OtherGradlePluginTests
    @DisplayName("works for Kotlin with Kapt simple project")
    @GradleTest
    fun testRelocationSimpleKapt(gradleVersion: GradleVersion) {
        val (firstProject, secondProject) = prepareTestProjects(
            "kapt2/simple",
            gradleVersion
        ) {
            it.gradleProperties.append(
                """
                
                kapt.useBuildCache = true
                """.trimIndent()
            )
        }

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf(":classes", ":testClasses"),
            listOf(":kaptKotlin", ":kaptGenerateStubsKotlin", ":compileKotlin", ":compileTestKotlin", ":compileJava")
        ) {
            assertNoBuildWarnings(expectedK2KaptWarnings)
        }
    }

    @JsGradlePluginTests
    @DisplayName("works with JS/DCE project")
    @GradleTest
    fun testRelocationKotlinJs(gradleVersion: GradleVersion) {
        val (firstProject, secondProject) = prepareTestProjects("kotlin-js-dce", gradleVersion)

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf("assemble"),
            listOf(
                ":libraryProject:compileKotlinJs",
                ":mainProject:compileKotlinJs",
                ":mainProject:compileProductionExecutableKotlinJs",
                ":mainProject:browserProductionWebpack"
            )
        )
    }

    @MppGradlePluginTests
    @DisplayName("works with Multiplatform")
    @GradleTest
    fun testRelocationMultiplatform(gradleVersion: GradleVersion) {
        val (firstProject, secondProject) = prepareTestProjects("new-mpp-lib-with-tests", gradleVersion)

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf("build"),
            listOf(
                ":compileCommonMainKotlinMetadata",
                ":compileKotlinJvmWithoutJava",
                ":compileTestKotlinJvmWithoutJava",
                ":compileKotlinJs",
                ":compileTestKotlinJs"
            )
        )
    }

    @AndroidGradlePluginTests
    @DisplayName("works with Android project")
    @GradleAndroidTest
    fun testRelocationAndroidProject(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkProvider: JdkVersions.ProvidedJdk,
    ) {
        val (firstProject, secondProject) = prepareTestProjects(
            "AndroidProject",
            gradleVersion,
            defaultBuildOptions.copy(androidVersion = agpVersion),
            jdkProvider.location
        )

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf("assembleDebug"),
            listOf(":Lib", ":Android").flatMap { module ->
                listOf("Flavor1", "Flavor2").flatMap { flavor ->
                    listOf("Debug").map { buildType ->
                        "$module:compile$flavor${buildType}Kotlin"
                    }
                }
            }
        )
    }

    @AndroidGradlePluginTests
    @DisplayName("Test relocation for Android with dagger project")
    @GradleAndroidTest
    fun testRelocationAndroidDagger(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkProvider: JdkVersions.ProvidedJdk,
    ) {
        val (firstProject, secondProject) = prepareTestProjects(
            "kapt2/android-dagger",
            gradleVersion,
            defaultBuildOptions.copy(androidVersion = agpVersion),
            jdkProvider.location
        ) {
            it.subProject("app").buildGradle.append("\nkapt.useBuildCache = true")
        }

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf("assembleDebug"),
            listOf("Debug").flatMap { buildType ->
                listOf("kapt", "kaptGenerateStubs", "compile").map { kotlinTask ->
                    ":app:$kotlinTask${buildType}Kotlin"
                }
            }
        )
    }

    @AndroidGradlePluginTests
    @DisplayName("KT-48617: Kapt ignores empty directories from Android variant")
    @GradleAndroidTest
    fun kaptIgnoreEmptyAndroidVariant(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkProvider: JdkVersions.ProvidedJdk,
    ) {
        val (firstProject, secondProject) = prepareTestProjects(
            "kapt2/android-dagger",
            gradleVersion,
            defaultBuildOptions.copy(androidVersion = agpVersion),
            jdkProvider.location
        ) {
            it.subProject("app").buildGradle.append("\nkapt.useBuildCache = true")
        }

        firstProject.subProject("app").javaSourcesDir()
            .resolve("com/example/dagger/kotlin/fakeempty").createDirectory()

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf("assembleDebug"),
            listOf("Debug").flatMap { buildType ->
                listOf("kapt", "kaptGenerateStubs", "compile").map { kotlinTask ->
                    ":app:$kotlinTask${buildType}Kotlin"
                }
            }
        )
    }

    @NativeGradlePluginTests
    @DisplayName("with native project")
    @GradleTest
    fun testRelocationNative(gradleVersion: GradleVersion) {
        val localRepoDir = defaultLocalRepo(gradleVersion)
        val buildOptionsBeforeCaching = defaultBuildOptions.copy(
            nativeOptions = super.defaultBuildOptions.nativeOptions.copy(
                version = TestVersions.Kotlin.STABLE_RELEASE,
                distributionDownloadFromMaven = true
            )
        )
        val (firstProject, secondProject) = prepareTestProjects(
            "native-build-cache",
            gradleVersion,
            buildOptions = buildOptionsBeforeCaching,
            localRepoDir = localRepoDir
        )

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf(":build-cache-lib:publish"),
            listOf(
                ":build-cache-lib:compileKotlinHost",
            )
        )

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf(":build-cache-app:assemble"),
            listOf(
                ":build-cache-app:compileKotlinHost",
                ":build-cache-app:lib-module:compileKotlinHost",
                ":build-cache-app:linkDebugStaticHost",
                ":build-cache-app:linkDebugSharedHost"
            )
        )
    }

    @OtherGradlePluginTests
    @DisplayName("Kapt incremental compilation works with cache")
    @GradleTest
    fun testKaptCachingIncrementalBuildWithoutRelocation(gradleVersion: GradleVersion) {
        project("kapt2/kaptAvoidance", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            checkKaptCachingIncrementalBuild(this, this)
        }
    }

    @OtherGradlePluginTests
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

    private fun prepareTestProjects(
        projectName: String,
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions = defaultBuildOptions,
        buildJdk: File? = null,
        localRepoDir: Path? = null,
        additionalConfiguration: (TestProject) -> Unit = {},
    ): Pair<TestProject, TestProject> {
        val firstProject = project(projectName, gradleVersion, buildOptions, buildJdk = buildJdk, localRepoDir = localRepoDir) {
            enableLocalBuildCache(localBuildCacheDir)
            additionalConfiguration(this)
        }

        val secondProject = project(projectName, gradleVersion, buildOptions, buildJdk = buildJdk, localRepoDir = localRepoDir) {
            enableLocalBuildCache(localBuildCacheDir)
            additionalConfiguration(this)
        }

        return firstProject to secondProject
    }

    private fun checkBuildCacheRelocation(
        firstProject: TestProject,
        secondProject: TestProject,
        tasksToExecute: List<String>,
        cacheableTasks: List<String>,
        additionalAssertions: BuildResult.() -> Unit = {},
    ) {
        firstProject.build(*tasksToExecute.toTypedArray()) {
            assertTasksPackedToCache(*cacheableTasks.toTypedArray())
            additionalAssertions()
        }

        firstProject.build("clean")

        secondProject.build(*tasksToExecute.toTypedArray()) {
            assertTasksFromCache(*cacheableTasks.toTypedArray())
            additionalAssertions()
        }
    }

    private fun checkKaptCachingIncrementalBuild(
        firstProject: TestProject,
        secondProject: TestProject,
    ) {
        val options = defaultBuildOptions.copy(
            kaptOptions = BuildOptions.KaptOptions(
                verbose = true,
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
        val appClassKtSourceFile = secondProject.subProject("app").kotlinSourcesDir().resolve("AppClass.kt")
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

    @JvmGradlePluginTests
    @DisplayName("Kotlin incremental compilation should work correctly after cache hint")
    @GradleTest
    fun testKotlinIncrementalCompilationAfterCacheHit(gradleVersion: GradleVersion) {
        checkKotlinIncrementalCompilationAfterCacheHit(gradleVersion)
    }

    @JvmGradlePluginTests
    @DisplayName("test custom source set and build directory located outside project directory") // Regression test for KT-61852 and KT-58547
    @GradleTest
    fun testCustomSourceSetAndBuildDirectory(gradleVersion: GradleVersion) {
        val projects = mutableListOf<TestProject>()

        checkKotlinIncrementalCompilationAfterCacheHit(
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(useDaemonFallbackStrategy = false)
        ) { project ->
            project.projectPath.toFile().resolve("../SOURCE_SET_OUTSIDE_PROJECT/src/main/kotlin").let {
                it.mkdirs()
                it.resolve("SomeClass.kt").createNewFile()
            }
            project.buildGradle.append("sourceSets { main.java.srcDirs += \"../SOURCE_SET_OUTSIDE_PROJECT/src/main/kotlin\" }")
            project.buildGradle.append("buildDir = \"../BUILD_DIR_OUTSIDE_PROJECT\"")

            projects.add(project)
        }

        // Also check that output files do not contain non-relocatable paths
        val projectPath = projects.first().projectPath
        val outputFilesContainingNonRelocatablePaths =
            projectPath.resolve("../BUILD_DIR_OUTSIDE_PROJECT/kotlin/compileKotlin").walk().filter {
                // Use readText() even for binary files as we don't have a better way for now
                it.isRegularFile() && it.readText().let { text ->
                    text.contains(projectPath.parent.name) || text.contains("BUILD_DIR_OUTSIDE_PROJECT")
                }
            }.toList()
        assert(outputFilesContainingNonRelocatablePaths.isEmpty()) {
            "The following output files contain non-relocatable paths:\n" + outputFilesContainingNonRelocatablePaths.joinToString("\n")
        }
    }

    private fun checkKotlinIncrementalCompilationAfterCacheHit(
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions = defaultBuildOptions,
        configureProject: (TestProject) -> Unit = {},
    ) {
        val (firstProject, secondProject) =
            prepareTestProjects("buildCacheSimple", gradleVersion, buildOptions, buildJdk = null) {
                configureProject(it)
            }

        // Build the first project -- It should be a cache miss
        firstProject.build(":compileKotlin") {
            assertTasksPackedToCache(":compileKotlin")
        }

        // Build the second project -- It should be a cache hit
        secondProject.build(":compileKotlin") {
            assertTasksFromCache(":compileKotlin")
        }

        // Make a change to the second project and build again -- Compilation should be incremental
        secondProject.kotlinSourcesDir().resolve("foo.kt").modify {
            it.replace("Int = 1", "String = \"abc\"")
        }
        secondProject.build("assemble", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
            assertIncrementalCompilation(listOf("src/main/kotlin/foo.kt", "src/main/kotlin/fooUsage.kt").toPaths())
        }
    }
}
