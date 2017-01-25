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
            super.defaultBuildOptions().copy(androidHome = File("../../../dependencies/android-sdk-for-tests"),
                                             androidGradlePluginVersion = androidGradlePluginVersion)



    @Test
    fun testSimpleCompile() {
        val project = Project("AndroidProject", gradleVersion)

        project.build("build", "assembleAndroidTest") {
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
                    ":compileFlavor2Release",
                    ":compileFlavor1DebugUnitTestKotlin",
                    "InternalDummyTest PASSED",
                    ":compileFlavor1DebugAndroidTestKotlin")
            checkKotlinGradleBuildServices()
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
            checkKotlinGradleBuildServices()
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
            assertCompiledKotlinSources(listOf("app/src/main/kotlin/foo/KotlinActivity1.kt", "app/src/main/kotlin/foo/getSomething.kt"))
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

        val stringsXml = project.projectDir.getFileByName("strings.xml")
        stringsXml.modify { """
            <resources>
                <string name="app_name">kotlin</string>
                <string name="app_name1">kotlin1</string>
            </resources>
        """ }
        // rebuilt because R.java changed
        val homeActivityKt = project.projectDir.getFileByName("HomeActivity.kt")
        val useRJavaActivity = project.projectDir.getFileByName("UseRJavaActivity.kt")

        project.build(":app:assembleDebug", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(
                    androidModuleKt,
                    baseApplicationKt,
                    useBuildConfigJavaKt,
                    homeActivityKt,
                    useRJavaActivity
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
    fun testAndroidKaptChangingDependencies() {
        val project = Project("AndroidKaptChangingDependencies", gradleVersion)

        project.build("build") {
            assertSuccessful()
            assertNotContains("Changed dependencies of configuration .+ after it has been included in dependency resolution".toRegex())
        }
    }
}