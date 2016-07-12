package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File


class KotlinAndroidGradleCLIOnly : AbstractKotlinAndroidGradleTests(gradleVersion = "2.3", androidGradlePluginVersion = "1.5.+")

abstract class AbstractKotlinAndroidGradleTests(
        private val gradleVersion: String,
        private val androidGradlePluginVersion: String
) : BaseGradleIT() {

    override fun defaultBuildOptions() =
            BuildOptions(withDaemon = true,
                         androidHome = File("../../../dependencies/android-sdk-for-tests"),
                         androidGradlePluginVersion = androidGradlePluginVersion)

    @Test
    fun testSimpleCompile() {
        val project = Project("AndroidProject", gradleVersion)

        project.build("build") {
            assertSuccessful()
            assertContains(":Lib:compileReleaseKotlin",
                    ":Test:compileDebugKotlin",
                    ":compileFlavor1DebugKotlin",
                    ":compileFlavor2DebugKotlin",
                    ":compileFlavor1JnidebugKotlin",
                    ":compileFlavor1ReleaseKotlin",
                    ":compileFlavor2JnidebugKotlin",
                    ":compileFlavor2ReleaseKotlin",
                    ":compileFlavor1Debug",
                    ":compileFlavor2Debug",
                    ":compileFlavor1Jnidebug",
                    ":compileFlavor2Jnidebug",
                    ":compileFlavor1Release",
                    ":compileFlavor2Release")
        }

        // Run the build second time, assert everything is up-to-date
        project.build("build") {
            assertSuccessful()
            assertContains(":Lib:compileReleaseKotlin UP-TO-DATE")
        }

        // Run the build third time, re-run tasks

        project.build("build", "--rerun-tasks") {
            assertSuccessful()
            assertContains(":Lib:compileReleaseKotlin",
                    ":Test:compileDebugKotlin",
                    ":compileFlavor1DebugKotlin",
                    ":compileFlavor2DebugKotlin",
                    ":compileFlavor1JnidebugKotlin",
                    ":compileFlavor1ReleaseKotlin",
                    ":compileFlavor2JnidebugKotlin",
                    ":compileFlavor2ReleaseKotlin",
                    ":compileFlavor1Debug",
                    ":compileFlavor2Debug",
                    ":compileFlavor1Jnidebug",
                    ":compileFlavor2Jnidebug",
                    ":compileFlavor1Release",
                    ":compileFlavor2Release")
        }
    }

    @Test
    fun testIncrementalCompile() {
        val project = Project("AndroidIncrementalSingleModuleProject", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("build", options = options) {
            assertSuccessful()
        }

        val getSomethingKt = project.projectDir.walk().filter { it.isFile && it.name.endsWith("getSomething.kt") }.first()
        getSomethingKt.writeText("""
package foo

fun getSomething() = 10
""")

        project.build("build", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(listOf("src/main/kotlin/foo/KotlinActivity1.kt", "src/main/kotlin/foo/getSomething.kt"))
            assertCompiledJavaSources(listOf("app/src/main/java/foo/JavaActivity.java"), weakTesting = true)
        }
    }

    @Test
    fun testIncrementalBuildWithNoChanges() {
        val project = Project("AndroidIncrementalSingleModuleProject", gradleVersion)
        val tasksToExecute = arrayOf(
                ":app:prepareComAndroidSupportAppcompatV72311Library",
                ":app:prepareComAndroidSupportSupportV42311Library",
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
    fun testModuleNameAndroid() {
        val project = Project("AndroidProject", gradleVersion)

        project.build("build") {
            assertContains(
                    "args.moduleName = Lib-compileReleaseKotlin",
                    "args.moduleName = Lib-compileDebugKotlin",
                    "args.moduleName = Android-compileFlavor1DebugKotlin",
                    "args.moduleName = Android-compileFlavor2DebugKotlin",
                    "args.moduleName = Android-compileFlavor1JnidebugKotlin",
                    "args.moduleName = Android-compileFlavor2JnidebugKotlin",
                    "args.moduleName = Android-compileFlavor1ReleaseKotlin",
                    "args.moduleName = Android-compileFlavor2ReleaseKotlin")
        }
    }

    @Test
    fun testAndroidDaggerIC() {
        val project = Project("AndroidDaggerProject", gradleVersion)
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }

        val file = project.projectDir.getFileByName("AndroidModule.kt")
        file.modify { it.replace("fun provideApplicationContext(): Context {",
                                 "fun provideApplicationContext(): Context? {") }

        project.build(":app:assembleDebug", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativizeToSubproject("app", file))
        }
    }
}