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
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.createDirectory

@DisplayName("Build cache relocation")
@SimpleGradlePluginTests
class BuildCacheRelocationIT : KGPBaseTest() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(buildCacheEnabled = true)

    private val localBuildCacheDir get() = workingDir.resolve("remote-jdk-build-cache")

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
        )
    }

    @DisplayName("works with JS/DCE project")
    @GradleTest
    fun testRelocationKotlin2JsDce(gradleVersion: GradleVersion) {
        val (firstProject, secondProject) = prepareTestProjects(
            "kotlin2JsDceProject",
            gradleVersion
        ) { testProject ->
            // Fix the problem that the destinationDir of compile task (i.e. buildDir) contains files from other tasks:
            testProject.subProject("mainProject").buildGradle.modify {
                it.replace("/exampleapp.js", "/web/exampleapp.js")
            }
            testProject.subProject("libraryProject").buildGradle.modify {
                it.replace("/exampleapp.js", "/web/exampleapp.js")
                // Fix assembling the JAR from the whole buildDir
                it.replace("from buildDir", "from compileKotlin2Js.destinationDir")
            }
        }

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf("assemble", "runDceKotlinJs"),
            listOf(":libraryProject:compileKotlin2Js", ":mainProject:compileKotlin2Js", ":mainProject:runDceKotlinJs")
        )
    }

    @DisplayName("works with Multiplatform")
    @GradleTest
    fun testRelocationMultiplatform(gradleVersion: GradleVersion) {
        val (firstProject, secondProject) = prepareTestProjects(
            "new-mpp-lib-with-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                warningMode = WarningMode.Summary // Remove it once project will be updated
            )
        )

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf("build"),
            listOf(
                ":compileCommonMainKotlinMetadata",
                ":compileKotlinJvmWithJava",
                ":compileTestKotlinJvmWithJava",
                ":compileKotlinJs",
                ":compileTestKotlinJs"
            )
        )
    }

    @DisplayName("works with Android project")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_6_7)
    @GradleTest
    fun testRelocationAndroidProject(gradleVersion: GradleVersion) {
        val (firstProject, secondProject) = prepareTestProjects(
            "AndroidProject",
            gradleVersion,
            defaultBuildOptions.copy(androidVersion = TestVersions.AGP.AGP_42)
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

    @DisplayName("Test relocation for Android with dagger project")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_6_7)
    @GradleTest
    fun testRelocationAndroidDagger(gradleVersion: GradleVersion) {
        val (firstProject, secondProject) = prepareTestProjects(
            "kapt2/android-dagger",
            gradleVersion,
            defaultBuildOptions.copy(androidVersion = TestVersions.AGP.AGP_42)
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

    @DisplayName("KT-48617: Kapt ignores empty directories from Android variant")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_6_8)
    @GradleTest
    fun kaptIgnoreEmptyAndroidVariant(gradleVersion: GradleVersion) {
        val (firstProject, secondProject) = prepareTestProjects(
            "kapt2/android-dagger",
            gradleVersion,
            defaultBuildOptions.copy(androidVersion = TestVersions.AGP.AGP_42)
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

    @DisplayName("KT-48849: Kotlin compile should ignore empty layout resource directories added by kotlin android extensions")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_6_8)
    @GradleTest
    fun compileIgnoreEmptyAndroidResLayoutDirs(gradleVersion: GradleVersion) {
        val (firstProject, secondProject) = prepareTestProjects(
            "AndroidExtensionsProject",
            gradleVersion,
            defaultBuildOptions.copy(androidVersion = TestVersions.AGP.AGP_42)
        ) {
            it.subProject("app").buildGradle.append(
                """
                |
                |androidExtensions {
                |    experimental = true
                |}
                """.trimMargin()
            )
        }

        firstProject
            .subProject("app")
            .projectPath
            .resolve("src/main/res/layout-ar")
            .createDirectory()

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf("assembleDebug"),
            listOf(":app:compileDebugKotlin")
        )
    }

    @DisplayName("with native project")
    @GradleTest
    fun testRelocationNative(gradleVersion: GradleVersion) {
        val (firstProject, secondProject) = prepareTestProjects(
            "native-build-cache",
            gradleVersion,
            defaultBuildOptions.copy(parallel = false) // disabled to be able to consume published library before app compilation
        ) {
            val localRepoUri = it.projectPath.resolve("repo").toUri()
            it.subProject("build-cache-app").buildGradleKts.append(
                """
                
                repositories {
                    maven {
                        setUrl("$localRepoUri")
                    }
                }
                """.trimIndent()
            )
        }

        checkBuildCacheRelocation(
            firstProject,
            secondProject,
            listOf(":build-cache-lib:publish", ":build-cache-app:assemble"),
            listOf(
                ":build-cache-lib:compileKotlinHost",
                ":build-cache-app:compileKotlinHost",
                ":build-cache-app:lib-module:compileKotlinHost",
                ":build-cache-app:linkDebugStaticHost",
                ":build-cache-app:linkDebugSharedHost"
            )
        )
    }

    @DisplayName("Kapt incremental compilation works with cache")
    @GradleTest
    fun testKaptCachingIncrementalBuildWithoutRelocation(gradleVersion: GradleVersion) {
        project("kapt2/kaptAvoidance", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            checkKaptCachingIncrementalBuild(this, this)
        }
    }

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
        additionalConfiguration: (TestProject) -> Unit = {}
    ): Pair<TestProject, TestProject> {
        val firstProject = project(projectName, gradleVersion, buildOptions) {
            enableLocalBuildCache(localBuildCacheDir)
            additionalConfiguration(this)
        }

        val secondProject = project(projectName, gradleVersion, buildOptions) {
            enableLocalBuildCache(localBuildCacheDir)
            additionalConfiguration(this)
        }

        return firstProject to secondProject
    }

    private fun checkBuildCacheRelocation(
        firstProject: TestProject,
        secondProject: TestProject,
        tasksToExecute: List<String>,
        cacheableTasks: List<String>
    ) {
        firstProject.build(*tasksToExecute.toTypedArray()) {
            assertTasksPackedToCache(*cacheableTasks.toTypedArray())
        }

        secondProject.build(*tasksToExecute.toTypedArray()) {
            assertTasksFromCache(*cacheableTasks.toTypedArray())
        }
    }

    @DisplayName("Kotlin incremental compilation should work correctly")
    @GradleTest
    fun testKotlinIncrementalCompilation(gradleVersion: GradleVersion) {
        checkKotlinIncrementalCompilation(gradleVersion)
    }

    @DisplayName("Kotlin incremental compilation with `kotlin.incremental.useClasspathSnapshot` feature should work correctly")
    @GradleTest
    fun testKotlinIncrementalCompilation_withGradleClasspathSnapshot(gradleVersion: GradleVersion) {
        checkKotlinIncrementalCompilation(gradleVersion, useGradleClasspathSnapshot = true)
    }

    @DisplayName("Kotlin incremental compilation with `kotlin.incremental.classpath.snapshot.enabled` feature should work correctly")
    @GradleTest
    fun testKotlinIncrementalCompilation_withICClasspathSnapshot(gradleVersion: GradleVersion) {
        checkKotlinIncrementalCompilation(gradleVersion, useICClasspathSnapshot = true)
    }

    private fun checkKotlinIncrementalCompilation(
        gradleVersion: GradleVersion,
        useGradleClasspathSnapshot: Boolean? = null,
        useICClasspathSnapshot: Boolean? = null
    ) {
        val buildOptions = defaultBuildOptions.copy(
            useGradleClasspathSnapshot = useGradleClasspathSnapshot,
            useICClasspathSnapshot = useICClasspathSnapshot
        )
        val (firstProject, secondProject) = prepareTestProjects("buildCacheSimple", gradleVersion, buildOptions)

        // First build, should be stored into the build cache:
        firstProject.build("assemble") {
            assertTasksPackedToCache(":compileKotlin")
        }

        // A cache hit: a clean build without any changes to the project
        secondProject.build("clean", "assemble") {
            assertTasksFromCache(":compileKotlin")
        }

        // Check whether compilation after a cache hit is incremental (KT-34862)
        val fooKtSourceFile = secondProject.kotlinSourcesDir().resolve("foo.kt")
        fooKtSourceFile.modify { it.replace("Int = 1", "String = \"abc\"") }
        secondProject.build("assemble", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
            if (useGradleClasspathSnapshot == true || useICClasspathSnapshot == true) {
                assertIncrementalCompilation(listOf("src/main/kotlin/foo.kt", "src/main/kotlin/fooUsage.kt"))
            } else {
                assertNonIncrementalCompilation()
            }
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
}
