package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.jetbrains.kotlin.gradle.util.isLegacyAndroidGradleVersion
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test
import java.io.File


class KotlinAndroidGradleIT :
    AbstractKotlinAndroidGradleTests(gradleVersion = GradleVersionRequired.AtLeast("3.4"), androidGradlePluginVersion = "2.3.0")

// TODO If we there is a way to fetch the latest Android plugin version, test against the latest version
class KotlinAndroid32GradleIT : KotlinAndroid3GradleIT(GradleVersionRequired.AtLeast("4.6"), "3.2.0-alpha15") {

    @Test
    fun testKaptUsingApOptionProvidersAsNestedInputOutput() = with(Project("AndroidProject", gradleVersion)) {
        setupWorkingDir()

        gradleBuildScript(subproject = "Android").appendText(
            """

            apply plugin: 'kotlin-kapt'

            class MyNested implements org.gradle.process.CommandLineArgumentProvider {

                @InputFile
                File inputFile = null

                @Override
                @Internal
                Iterable<String> asArguments() {
                    // Read the arguments from a file, because changing them in a build script is treated as an
                    // implementation change by Gradle:
                    return [new File('args.txt').text]
                }
            }

            def nested = new MyNested()
            nested.inputFile = file("${'$'}projectDir/in.txt")

            android.applicationVariants.all {
                it.javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders.add(nested)
            }
            """.trimIndent()
        )

        File(projectDir, "Android/in.txt").appendText("1234")
        File(projectDir, "args.txt").appendText("1234")

        val kaptTasks = listOf(":Android:kaptFlavor1DebugKotlin")
        val javacTasks = listOf(":Android:compileFlavor1DebugJavaWithJavac")

        val buildTasks = (kaptTasks + javacTasks).toTypedArray()

        build(*buildTasks) {
            assertSuccessful()
            assertTasksExecuted(kaptTasks + javacTasks)
        }

        File(projectDir, "Android/in.txt").appendText("5678")

        build(*buildTasks) {
            assertSuccessful()
            assertTasksExecuted(kaptTasks)
            assertTasksUpToDate(javacTasks)
        }

        // Changing only the annotation provider arguments should not trigger the tasks to run, as the arguments may be outputs,
        // internals or neither:
        File(projectDir, "args.txt").appendText("5678")

        build(*buildTasks) {
            assertSuccessful()
            assertTasksUpToDate(javacTasks + kaptTasks)
        }
    }
}

class KotlinAndroid30GradleIT : KotlinAndroid3GradleIT(GradleVersionRequired.AtLeast("4.1"), "3.0.0")

abstract class KotlinAndroid3GradleIT(
    gradleVersionRequired: GradleVersionRequired,
    androidGradlePluginVersion: String
) : AbstractKotlinAndroidGradleTests(gradleVersionRequired, androidGradlePluginVersion) {

    @Test
    fun testApplyWithFeaturePlugin() {
        val project = Project("AndroidProject", gradleVersion)

        project.setupWorkingDir()
        File(project.projectDir, "Lib/build.gradle").modify { text ->
            // Change the applied plugin to com.android.feature
            text.replace("com.android.library", "com.android.feature")
                .replace("compileSdkVersion 22", "compileSdkVersion 26")
                .apply { assert(!equals(text)) }
                .plus("\nandroid { baseFeature true }")
        }

        // Check that Kotlin tasks were created for both lib and feature variants:
        val kotlinTaskNames =
            listOf("Debug").flatMap { buildType ->
                listOf("Flavor1", "Flavor2").flatMap { flavor ->
                    listOf("", "Feature").map { isFeature -> ":Lib:compile$flavor$buildType${isFeature}Kotlin" }
                }
            }

        project.build(":Lib:assembleDebug") {
            assertSuccessful()
            assertTasksExecuted(*kotlinTaskNames.toTypedArray())
        }
    }
}

abstract class AbstractKotlinAndroidGradleTests(
    protected val gradleVersion: GradleVersionRequired,
    private val androidGradlePluginVersion: String
) : BaseGradleIT() {

    override fun defaultBuildOptions() =
        super.defaultBuildOptions().copy(
            androidHome = KotlinTestUtils.findAndroidSdk(),
            androidGradlePluginVersion = androidGradlePluginVersion
        )

    @Test
    fun testSimpleCompile() {
        val project = Project("AndroidProject", gradleVersion)

        val modules = listOf("Android", "Lib")
        val flavors = listOf("Flavor1", "Flavor2")
        val buildTypes = listOf("Debug")

        val tasks = arrayListOf<String>()
        for (module in modules) {
            for (flavor in flavors) {
                for (buildType in buildTypes) {
                    tasks.add(":$module:compile$flavor${buildType}Kotlin")
                }
            }
        }

        project.build("assembleDebug", "test") {
            assertSuccessful()
            // Before 3.0 AGP test only modules are compiled only against one flavor and one build type,
            // and contain only the compileDebugKotlin task.
            // After 3.0 AGP test only modules contain a compile<Variant>Kotlin task for each variant.
            tasks.addAll(findTasksByPattern(":Test:compile[\\w\\d]+Kotlin"))
            assertTasksExecuted(tasks)
            assertContains("InternalDummyTest PASSED")
            checkKotlinGradleBuildServices()
        }

        // Run the build second time, assert everything is up-to-date
        project.build("assembleDebug") {
            assertSuccessful()
            assertTasksUpToDate(tasks)
        }

        // Run the build third time, re-run tasks

        project.build("assembleDebug", "--rerun-tasks") {
            assertSuccessful()
            assertTasksExecuted(tasks)
            checkKotlinGradleBuildServices()
        }
    }

    @Test
    fun testAssembleAndroidTestFirst() {
        val project = Project("AndroidProject", gradleVersion, minLogLevel = LogLevel.INFO)

        // Execute 'assembleAndroidTest' first, without 'build' side effects
        project.build("assembleAndroidTest") {
            assertSuccessful()
            if (project.testGradleVersionBelow("4.0")) {
                val tasks = ArrayList<String>().apply {
                    for (subProject in listOf("Android", "Lib")) {
                        for (flavor in listOf("Flavor1", "Flavor2")) {
                            add(":$subProject:copy${flavor}DebugKotlinClasses")
                        }
                    }
                }
                // with the new AGP we don't need copy classes tasks
                assertTasksExecuted(tasks)
            }
        }
    }

    @Test
    fun testIncrementalCompile() {
        val project = Project("AndroidIncrementalSingleModuleProject", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }

        val getSomethingKt = project.projectDir.walk().filter { it.isFile && it.name.endsWith("getSomething.kt") }.first()
        getSomethingKt.writeText(
            """
package com.example

fun getSomething() = 10
"""
        )

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedKotlinFiles = listOf(
                "app/src/main/kotlin/com/example/KotlinActivity1.kt",
                "app/src/main/kotlin/com/example/getSomething.kt"
            )
            assertCompiledKotlinSources(affectedKotlinFiles)
            assertCompiledJavaSources(listOf("app/src/main/java/com/example/JavaActivity.java"), weakTesting = true)
        }
    }

    @Test
    fun testMultiModuleIC() {
        val project = Project("AndroidProject", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }

        val libUtilKt = project.projectDir.getFileByName("libUtil.kt")
        libUtilKt.modify { it.replace("fun libUtil(): String", "fun libUtil(): CharSequence") }

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("libUtil.kt", "MainActivity2.kt")
            assertCompiledKotlinSources(project.relativize(affectedSources), weakTesting = false)
        }
    }

    @Test
    fun testMultiModuleICNonAndroidModuleIsChanged() {
        val project = Project("AndroidIncrementalMultiModule", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = true, kotlinDaemonDebugPort = null)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }

        val libAndroidUtilKt = project.projectDir.getFileByName("libAndroidUtil.kt")
        libAndroidUtilKt.modify { it.replace("fun libAndroidUtil(): String", "fun libAndroidUtil(): CharSequence") }
        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("libAndroidUtil.kt", "useLibAndroidUtil.kt")
            assertCompiledKotlinSources(project.relativize(affectedSources), weakTesting = false)
        }

        val libJvmUtilKt = project.projectDir.getFileByName("LibJvmUtil.kt")
        libJvmUtilKt.modify { it.replace("fun libJvmUtil(): String", "fun libJvmUtil(): CharSequence") }
        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("LibJvmUtil.kt", "useLibJvmUtil.kt")
            assertCompiledKotlinSources(project.relativize(affectedSources), weakTesting = false)
        }
    }

    @Test
    fun testIncrementalBuildWithNoChanges() {
        val project = Project("AndroidIncrementalSingleModuleProject", gradleVersion)
        val tasksToExecute = listOf(
            ":app:compileDebugKotlin",
            ":app:compileDebugJavaWithJavac"
        )

        project.build("assembleDebug") {
            assertSuccessful()
            assertTasksExecuted(tasksToExecute)
        }

        project.build("assembleDebug") {
            assertSuccessful()
            assertTasksUpToDate(tasksToExecute)
        }
    }

    @Test
    fun testAndroidDaggerIC() {
        val project = Project("AndroidDaggerProject", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }

        val androidModuleKt = project.projectDir.getFileByName("AndroidModule.kt")
        androidModuleKt.modify {
            it.replace(
                "fun provideApplicationContext(): Context {",
                "fun provideApplicationContext(): Context? {"
            )
        }

        project.build(":app:assembleDebug", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(androidModuleKt))
            assertTasksExecuted(
                ":app:kaptGenerateStubsDebugKotlin",
                ":app:kaptDebugKotlin",
                ":app:compileDebugKotlin",
                ":app:compileDebugJavaWithJavac"
            )
        }
    }

    @Test
    fun testAndroidIcepickProject() {
        val project = Project("AndroidIcepickProject", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = false)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testAndroidExtensions() {
        val project = Project("AndroidExtensionsProject", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = false)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testAndroidExtensionsIncremental() {
        val project = Project("AndroidExtensionsProject", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames(
                "MyActivity.kt", "noLayoutUsages.kt"
            )
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }

        val activityLayout = File(project.projectDir, "app/src/main/res/layout/activity_main.xml")
        activityLayout.modify { it.replace("textView", "newTextView") }

        project.build("assembleDebug", options = options) {
            assertFailed()
            val affectedSources = project.projectDir.getFilesByNames("MyActivity.kt")
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }
    }


    @Test
    fun testAndroidExtensionsManyVariants() {
        if (isLegacyAndroidGradleVersion(androidGradlePluginVersion)) {
            // Library dependencies are not supported in older versions of Android Gradle plugin (< 3.0)
            return
        }

        val project = Project("AndroidExtensionsManyVariants", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = false)

        project.build("assemble", options = options) {
            if (isLegacyAndroidGradleVersion(androidGradlePluginVersion)) {
                // Library dependencies are not supported in older versions of Android Gradle plugin (< 3.0)
                assertFailed()
                assertContains("Unresolved reference: layout_in_library")
                assertContains("Unresolved reference: text_view")
            } else {
                assertSuccessful()
            }
        }
    }

    @Test
    fun testAndroidExtensionsSpecificFeatures() {
        val project = Project("AndroidExtensionsSpecificFeatures", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = false)

        project.build("assemble", options = options) {
            assertFailed()
            assertContains("Unresolved reference: textView")
        }

        File(project.projectDir, "app/build.gradle").modify { it.replace("[\"parcelize\"]", "[\"views\"]") }

        project.build("assemble", options = options) {
            assertFailed()
            assertContains("Class 'User' is not abstract and does not implement abstract member public abstract fun writeToParcel")
        }

        File(project.projectDir, "app/build.gradle").modify { it.replace("[\"views\"]", "[\"parcelize\", \"views\"]") }

        project.build("assemble", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testMultiplatformAndroidCompile() = with(Project("multiplatformAndroidProject", gradleVersion)) {
        setupWorkingDir()

        if (!isLegacyAndroidGradleVersion(androidGradlePluginVersion)) {
            // Check that the common module is not added to the deprecated configuration 'compile' (KT-23719):
            gradleBuildScript("libAndroid").appendText(
                """${'\n'}
                configurations.compile.dependencies.all { aDependencyExists ->
                    throw GradleException("Check failed")
                }
                """.trimIndent()
            )
        }

        build("build") {
            assertSuccessful()
            assertTasksExecuted(
                ":lib:compileKotlinCommon",
                ":lib:compileTestKotlinCommon",
                ":libJvm:compileKotlin",
                ":libJvm:compileTestKotlin",
                ":libAndroid:compileDebugKotlin",
                ":libAndroid:compileReleaseKotlin",
                ":libAndroid:compileDebugUnitTestKotlin",
                ":libAndroid:compileReleaseUnitTestKotlin"
            )

            val kotlinFolder = if (project.testGradleVersionAtLeast("4.0")) "kotlin" else ""

            assertFileExists("lib/build/classes/$kotlinFolder/main/foo/PlatformClass.kotlin_metadata")
            assertFileExists("lib/build/classes/$kotlinFolder/test/foo/PlatformTest.kotlin_metadata")
            assertFileExists("libJvm/build/classes/$kotlinFolder/main/foo/PlatformClass.class")
            assertFileExists("libJvm/build/classes/$kotlinFolder/test/foo/PlatformTest.class")

            assertFileExists("libAndroid/build/tmp/kotlin-classes/debug/foo/PlatformClass.class")
            assertFileExists("libAndroid/build/tmp/kotlin-classes/release/foo/PlatformClass.class")
            assertFileExists("libAndroid/build/tmp/kotlin-classes/debugUnitTest/foo/PlatformTest.class")
            assertFileExists("libAndroid/build/tmp/kotlin-classes/debugUnitTest/foo/PlatformTest.class")
        }
    }
}