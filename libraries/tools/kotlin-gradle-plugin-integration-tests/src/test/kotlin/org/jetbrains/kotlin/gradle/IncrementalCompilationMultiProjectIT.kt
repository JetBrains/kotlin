package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.allKotlinFiles
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class IncrementalCompilationMultiProjectIT : BaseGradleIT() {
    companion object {
        private val GRADLE_VERSION = "2.10"
        private val ANDROID_GRADLE_PLUGIN_VERSION = "1.5.+"
    }

    private fun androidBuildOptions() =
            BuildOptions(withDaemon = true,
                    androidHome = File(ANDROID_HOME_PATH),
                    androidGradlePluginVersion = ANDROID_GRADLE_PLUGIN_VERSION,
                    incremental = true)

    override fun defaultBuildOptions(): BuildOptions =
            super.defaultBuildOptions().copy(withDaemon = true, incremental = true)


    @Test
    fun testMoveFunctionFromLib() {
        val project = Project("incrementalMultiproject", GRADLE_VERSION)
        project.build("build") {
            assertSuccessful()
        }

        val barUseABKt = project.projectDir.getFileByName("barUseAB.kt")
        val barInApp = File(project.projectDir, "app/src/main/java/bar").apply { mkdirs() }
        barUseABKt.copyTo(File(barInApp, barUseABKt.name))
        barUseABKt.delete()

        project.build("build") {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("fooCallUseAB.kt", "barUseAB.kt")
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testAddNewMethodToLib() {
        val project = Project("incrementalMultiproject", GRADLE_VERSION)
        project.build("build") {
            assertSuccessful()
        }

        val aKt = project.projectDir.getFileByName("A.kt")
        aKt.writeText("""
package bar

open class A {
    fun a() {}
    fun newA() {}
}
""")

        project.build("build") {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("A.kt", "B.kt", "AA.kt", "BB.kt")
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testLibClassBecameFinal() {
        val project = Project("incrementalMultiproject", GRADLE_VERSION)
        project.build("build") {
            assertSuccessful()
        }

        val bKt = project.projectDir.getFileByName("B.kt")
        bKt.modify { it.replace("open class", "class") }

        project.build("build") {
            assertFailed()
            val affectedSources = project.projectDir.getFilesByNames(
                    "B.kt", "barUseAB.kt", "barUseB.kt",
                    "BB.kt", "fooCallUseAB.kt", "fooUseB.kt")
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testModifyJavaInLib() {
        val project = Project("incrementalMultiproject", GRADLE_VERSION)
        project.build("build") {
            assertSuccessful()
        }

        val javaClassJava = project.projectDir.getFileByName("JavaClass.java")
        javaClassJava.modify { it.replace("String getString", "Object getString") }

        project.build("build") {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("JavaClassChild.kt", "useJavaClass.kt", "useJavaClassFooMethodUsage.kt")
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testModifyTrackedJavaClassInLib() {
        val project = Project("incrementalMultiproject", GRADLE_VERSION)
        project.build("build") {
            assertSuccessful()
        }

        val javaClassJava = project.projectDir.getFileByName("TrackedJavaClass.java")
        javaClassJava.modify { it.replace("String getString", "Object getString") }

        project.build("build") {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames(
                    "TrackedJavaClassChild.kt", "useTrackedJavaClass.kt", "useTrackedJavaClassSameModule.kt"
            )
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testCleanBuildLib() {
        val project = Project("incrementalMultiproject", GRADLE_VERSION)
        project.build("build") {
            assertSuccessful()
        }

        project.build(":lib:clean", ":lib:build") {
            assertSuccessful()
            val affectedSources = File(project.projectDir, "lib").allKotlinFiles()
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }

        project.build("build") {
            assertSuccessful()
            val affectedSources = File(project.projectDir, "app").allKotlinFiles()
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testCompileErrorInLib() {
        val project = Project("incrementalMultiproject", GRADLE_VERSION)
        project.build("build") {
            assertSuccessful()
        }

        val bKt = project.projectDir.getFileByName("B.kt")
        bKt.delete()

        project.build("build") {
            assertFailed()
        }

        project.projectDir.getFileByName("barUseB.kt").delete()
        project.projectDir.getFileByName("barUseAB.kt").delete()

        project.build("build") {
            assertFailed()
            val affectedSources = project.projectDir.allKotlinFiles()
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    // checks that multi-project ic is disabled when there is a task that outputs to javaDestination dir
    // that is not JavaCompile or KotlinCompile
    @Test
    fun testCompileLibWithGroovy() {
        val project = Project("incrementalMultiproject", GRADLE_VERSION)
        project.setupWorkingDir()
        val lib = File(project.projectDir, "lib")
        val libBuildGradle = File(lib, "build.gradle")
        libBuildGradle.modify {
            """
            apply plugin: 'groovy'
            apply plugin: 'kotlin'

            dependencies {
                compile 'org.codehaus.groovy:groovy-all:2.4.7'
            }
            """.trimIndent()
        }

        val libGroovySrcBar = File(lib, "src/main/groovy/bar").apply { mkdirs() }
        val groovyClass = File(libGroovySrcBar, "GroovyClass.groovy")
        groovyClass.writeText("""
            package bar

            class GroovyClass {}
        """)

        project.build("build") {
            assertSuccessful()
        }

        project.projectDir.getFileByName("barUseB.kt").delete()
        project.build("build") {
            assertSuccessful()
            val affectedSources = File(project.projectDir, "app").allKotlinFiles()
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testAndroid() {
        val project = Project("AndroidProject", GRADLE_VERSION)
        val options = androidBuildOptions()

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }

        val libUtilKt = project.projectDir.getFileByName("libUtil.kt")
        libUtilKt.modify { it.replace("fun libUtil(): String", "fun libUtil(): Any") }

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("libUtil.kt", "MainActivity2.kt")
            assertCompiledKotlinSources(project.relativize(affectedSources), weakTesting = false)
        }
    }
}