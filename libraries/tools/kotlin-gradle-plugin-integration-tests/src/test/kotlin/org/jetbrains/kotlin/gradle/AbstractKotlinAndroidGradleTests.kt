package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.isLegacyAndroidGradleVersion
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File


class KotlinAndroidGradleIT : AbstractKotlinAndroidGradleTests(gradleVersion = "3.4", androidGradlePluginVersion = "2.3.0")
class KotlinAndroidWithJackGradleIT : AbstractKotlinAndroidWithJackGradleTests(gradleVersion = "3.4", androidGradlePluginVersion = "2.3.+")

class KotlinAndroid30GradleIT : AbstractKotlinAndroidGradleTests(gradleVersion = "4.1-rc-1", androidGradlePluginVersion = "3.0.0-beta1") {

    @Test
    fun testApplyWithFeaturePlugin() {
        val project = Project("AndroidProject", gradleVersion)

        project.setupWorkingDir()
        File(project.projectDir, "Lib/build.gradle").modify { text ->
            // Change the applied plugin to com.android.feature
            text.replace("com.android.library", "com.android.feature")
                    .replace("compileSdkVersion 22", "compileSdkVersion 26")
                    .apply { assert(!equals(text)) }
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
            assertContains(*kotlinTaskNames.toTypedArray())
        }
    }
}

const val ANDROID_HOME_PATH = "../../../dependencies/androidSDK"

abstract class AbstractKotlinAndroidGradleTests(
        protected val gradleVersion: String,
        private val androidGradlePluginVersion: String
) : BaseGradleIT() {

    override fun defaultBuildOptions() =
            super.defaultBuildOptions().copy(androidHome = File(ANDROID_HOME_PATH),
                                             androidGradlePluginVersion = androidGradlePluginVersion)

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
        val project = Project("AndroidProject", gradleVersion)

        // Execute 'assembleAndroidTest' first, without 'build' side effects
        project.build("assembleAndroidTest") {
            assertSuccessful()
            if (isLegacyAndroidGradleVersion(androidGradlePluginVersion)) {
                // with the new AGP we don't need copy classes tasks
                assertContains(":copyFlavor1DebugKotlinClasses")
                assertContains(":copyFlavor2DebugKotlinClasses")
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
        getSomethingKt.writeText("""
package foo

fun getSomething() = 10
""")

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(listOf("app/src/main/kotlin/foo/KotlinActivity1.kt", "app/src/main/kotlin/foo/getSomething.kt"))
            assertCompiledJavaSources(listOf("app/src/main/java/foo/JavaActivity.java"), weakTesting = true)
        }
    }

    @Test
    fun testIncrementalBuildWithNoChanges() {
        val project = Project("AndroidIncrementalSingleModuleProject", gradleVersion)
        val tasksToExecute = arrayOf(
                ":app:compileDebugKotlin",
                ":app:compileDebugJavaWithJavac"
        )

        project.build("assembleDebug") {
            assertSuccessful()
            assertContains(*tasksToExecute)
        }

        project.build("assembleDebug") {
            assertSuccessful()
            assertContains(*tasksToExecute.map { it + " UP-TO-DATE" }.toTypedArray())
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
        androidModuleKt.modify { it.replace("fun provideApplicationContext(): Context {",
                                            "fun provideApplicationContext(): Context? {") }
        // rebuilt because DaggerApplicationComponent.java was regenerated
        val baseApplicationKt = project.projectDir.getFileByName("BaseApplication.kt")
        // rebuilt because BuildConfig.java was regenerated (timestamp was changed)
        val useBuildConfigJavaKt = project.projectDir.getFileByName("useBuildConfigJava.kt")

        project.build(":app:assembleDebug", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(
                    androidModuleKt,
                    baseApplicationKt,
                    useBuildConfigJavaKt
            ))
        }
    }

    @Test
    fun testKaptKt15814() {
        val project = Project("kaptKt15814", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = false)

        project.build("assembleDebug", "test", options = options) {
            assertSuccessful()
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
    fun testAndroidExtensionsManyVariants() {
        val project = Project("AndroidExtensionsManyVariants", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = false)

        project.build("assemble", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testAndroidKaptChangingDependencies() {
        val project = Project("AndroidKaptChangingDependencies", gradleVersion)

        project.build("assembleDebug") {
            assertSuccessful()
            assertNotContains("Changed dependencies of configuration .+ after it has been included in dependency resolution".toRegex())
        }
    }
}


abstract class AbstractKotlinAndroidWithJackGradleTests(
        private val gradleVersion: String,
        private val androidGradlePluginVersion: String
) : BaseGradleIT() {

    fun getEnvJDK_18() = System.getenv()["JDK_18"]

    override fun defaultBuildOptions() =
            super.defaultBuildOptions().copy(androidHome = File(ANDROID_HOME_PATH),
                    androidGradlePluginVersion = androidGradlePluginVersion, javaHome = File(getEnvJDK_18()))

    @Test
    fun testSimpleCompile() {
        val project = Project("AndroidJackProject", gradleVersion)

        project.build("assemble") {
            assertFailed()
            assertContains("Kotlin Gradle plugin does not support the deprecated Jack toolchain")
        }
    }
}

