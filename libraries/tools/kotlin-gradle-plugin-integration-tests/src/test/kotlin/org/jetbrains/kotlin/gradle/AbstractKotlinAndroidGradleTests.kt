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

class KotlinAndroidWithJackGradleIT : AbstractKotlinAndroidWithJackGradleTests(androidGradlePluginVersion = "2.3.+")

// TODO If we there is a way to fetch the latest Android plugin version, test against the latest version
class KotlinAndroid32GradleIT : KotlinAndroid3GradleIT(GradleVersionRequired.AtLeast("4.6"), "3.2.0-alpha15") {

    @Test
    fun testKaptUsingApOptionProvidersAsNestedInputOutput() = with(Project("AndroidProject", gradleVersion)) {
        setupWorkingDir()

        gradleBuildScript(subproject = "Android").appendText(
            """

            apply plugin: 'kotlin-kapt'

            class MyNested implements org.gradle.process.CommandLineArgumentProvider {
                String value = ""

                @InputFile
                File inputFile = null

                @Override
                Iterable<String> asArguments() { return [value] }
            }

            def nested = new MyNested()
            nested.value = '123'
            nested.inputFile = file("${'$'}projectDir/in.txt")

            android.applicationVariants.all {
                it.javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders.add(nested)
            }
            """.trimIndent()
        )

        File(projectDir, "Android/in.txt").appendText("1234")

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

        gradleBuildScript(subproject = "Android").modify {
            it.replace("nested.value = '123'", "nested.value = '456'")
        }

        build(*buildTasks) {
            assertSuccessful()
            assertTasksExecuted(kaptTasks)
            assertTasksUpToDate(javacTasks)
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
            listOf("Debug", "Release").flatMap { buildType ->
                listOf("Flavor1", "Flavor2").flatMap { flavor ->
                    listOf("", "Feature").map { isFeature -> ":Lib:compile$flavor$buildType${isFeature}Kotlin" }
                }
            }

        project.build(":Lib:assemble") {
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
        val buildTypes = listOf("Debug", "Release")

        val tasks = arrayListOf<String>()
        for (module in modules) {
            for (flavor in flavors) {
                for (buildType in buildTypes) {
                    tasks.add(":$module:compile$flavor${buildType}Kotlin")
                }
            }
        }

        project.build("build", "assembleAndroidTest") {
            assertSuccessful()
            // Before 3.0 AGP test only modules are compiled only against one flavor and one build type,
            // and contain only the compileDebugKotlin task.
            // After 3.0 AGP test only modules contain a compile<Variant>Kotlin task for each variant.
            tasks.addAll(findTasksByPattern(":Test:compile[\\w\\d]+Kotlin"))
            assertTasksExecuted(tasks)
            if (isLegacyAndroidGradleVersion(androidGradlePluginVersion)) {
                // known bug: new AGP does not run Kotlin tests
                // https://issuetracker.google.com/issues/38454212
                // TODO: remove when the bug is fixed
                assertContains("InternalDummyTest PASSED")
            }
            checkKotlinGradleBuildServices()
        }

        // Run the build second time, assert everything is up-to-date
        project.build("build") {
            assertSuccessful()
            assertTasksUpToDate(tasks)
        }

        // Run the build third time, re-run tasks

        project.build("build", "--rerun-tasks") {
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
            if (isLegacyAndroidGradleVersion(androidGradlePluginVersion)) {
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
package foo

fun getSomething() = 10
"""
        )

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(listOf("app/src/main/kotlin/foo/KotlinActivity1.kt", "app/src/main/kotlin/foo/getSomething.kt"))
            assertCompiledJavaSources(listOf("app/src/main/java/foo/JavaActivity.java"), weakTesting = true)
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
        val project = Project("AndroidExtensionsManyVariants", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = false)

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


abstract class AbstractKotlinAndroidWithJackGradleTests(
    private val androidGradlePluginVersion: String
) : BaseGradleIT() {

    fun getEnvJDK_18() = System.getenv()["JDK_18"]

    override fun defaultBuildOptions() =
        super.defaultBuildOptions().copy(
            androidHome = KotlinTestUtils.findAndroidSdk(),
            androidGradlePluginVersion = androidGradlePluginVersion, javaHome = File(getEnvJDK_18())
        )

    @Test
    fun testSimpleCompile() {
        val project = Project("AndroidJackProject", GradleVersionRequired.Exact("3.4"))

        project.build("assemble") {
            assertFailed()
            assertContains("Kotlin Gradle plugin does not support the deprecated Jack toolchain")
        }
    }
}