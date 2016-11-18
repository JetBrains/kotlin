package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test

class Kotlin2JsGradlePluginIT : BaseGradleIT() {
    @Test
    fun testBuildAndClean() {
        val project = Project("kotlin2JsProject", "2.10")

        project.build("build") {
            assertSuccessful()
            assertReportExists()

            assertContains(
                    ":libraryProject:jarSources\n",
                    ":mainProject:compileKotlin2Js\n",
                    ":libraryProject:compileKotlin2Js\n"
            )

            listOf("mainProject/web/js/app.js",
                    "mainProject/web/js/lib/kotlin.js",
                    "libraryProject/build/kotlin2js/main/test-library.js",
                    "mainProject/web/js/app.js.map"
            ).forEach { assertFileExists(it) }
        }

        project.build("build") {
            assertSuccessful()
            assertContains(
                    ":mainProject:compileKotlin2Js UP-TO-DATE",
                    ":libraryProject:compileTestKotlin2Js UP-TO-DATE"
            )
        }

        project.build("clean") {
            assertSuccessful()
            assertReportExists()
            assertContains(":mainProject:cleanCompileKotlin2Js\n")
            assertNoSuchFile("mainProject/web/js/app.js")

            // Test that we don't accidentally remove the containing directory
            // This would fail if we used the default clean task of the copy task
            assertFileExists("mainProject/web/js/lib")

            assertNoSuchFile("main/project/web/js/app.js.map")
            assertNoSuchFile("main/project/web/js/example/main.kt")
        }

        project.build("clean") {
            assertSuccessful()
            assertReportExists()
            assertContains(":mainProject:cleanCompileKotlin2Js UP-TO-DATE")
        }
    }

    @Test
    fun testModuleKind() {
        val project = Project("kotlin2JsModuleKind", "2.10")

        project.build("runRhino") {
            assertSuccessful()
        }
    }

    @Test
    fun testNoOutputFileFails() {
        val project = Project("kotlin2JsNoOutputFileProject", "2.10")
        project.build("build") {
            assertFailed()
            assertReportExists()
            assertContains("compileKotlin2Js.kotlinOptions.outputFile should be specified.")
        }
    }

    @Test
    fun testKotlinJsBuiltins() {
        val project = Project("kotlinBuiltins", "3.2")

        project.setupWorkingDir()
        val buildGradle = project.projectDir.getFileByName("build.gradle")
        buildGradle.modify {
            it.replace("apply plugin: \"kotlin\"", "apply plugin: \"kotlin2js\"") +
                    "\ncompileKotlin2Js.kotlinOptions.outputFile = \"out/out.js\""
        }
        project.build("build") {
            assertSuccessful()
        }
    }
}

