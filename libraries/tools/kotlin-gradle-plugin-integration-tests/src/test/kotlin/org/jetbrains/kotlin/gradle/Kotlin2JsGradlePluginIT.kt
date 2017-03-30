package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertEquals

class Kotlin2JsGradlePluginIT : BaseGradleIT() {
    @Test
    fun testBuildAndClean() {
        val project = Project("kotlin2JsProject", "2.10")

        project.build("build") {
            assertSuccessful()
            assertReportExists()

            assertContains(
                    ":libraryProject:jarSources",
                    ":mainProject:compileKotlin2Js",
                    ":libraryProject:compileKotlin2Js"
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

            // Test that we don't accidentally remove the containing directory
            // This would fail if we used the default clean task of the copy task
            assertFileExists("mainProject/web/js/lib")

            assertNoSuchFile("main/project/web/js/app.js.map")
            assertNoSuchFile("main/project/web/js/example/main.kt")
        }

        project.build("clean") {
            assertSuccessful()
            assertReportExists()
        }
    }

    @Test
    fun testJarIncludesJsDefaultOutput() {
        val project = Project("kotlin2JsNoOutputFileProject", "2.10")

        project.build("jar") {
            assertSuccessful()

            assertContains(":compileKotlin2Js")
            val jarPath = "build/libs/kotlin2JsNoOutputFileProject.jar"
            assertFileExists(jarPath)
            val jar = ZipFile(fileInWorkingDir(jarPath))
            assertEquals(1, jar.entries().asSequence().count { it.name == "kotlin2JsNoOutputFileProject_main.js" },
                         "The jar should contain an entry `kotlin2JsNoOutputFileProject_main.js` with no duplicates")
        }
    }

    @Test
    fun testJarIncludesJsOutputSetExplicitly() {
        val project = Project("kotlin2JsModuleKind", "2.10")

        project.build(":jar") {
            assertSuccessful()

            assertContains(":compileKotlin2Js")
            val jarPath = "build/libs/kotlin2JsModuleKind.jar"
            assertFileExists(jarPath)
            val jar = ZipFile(fileInWorkingDir(jarPath))
            assertEquals(1, jar.entries().asSequence().count { it.name == "app.js" },
                         "The jar should contain an entry `app.js` with no duplicates")
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
    fun testDefaultOutputFile() {
        val project = Project("kotlin2JsNoOutputFileProject", "2.10")

        project.build("build") {
            assertSuccessful()
            assertFileExists("build/classes/main/kotlin2JsNoOutputFileProject_main.js")
            assertFileExists("build/classes/test/kotlin2JsNoOutputFileProject_test.js")
        }
    }

    @Test
    fun testCompileTestCouldAccessProduction() {
        val project = Project("kotlin2JsProjectWithTests", "2.10")

        project.build("build") {
            assertSuccessful()

            assertContains(
                    ":compileKotlin2Js",
                    ":compileTestKotlin2Js"
            )

            assertFileExists("build/kotlin2js/main/module.js")
            assertFileExists("build/kotlin2js/test/module-tests.js")
        }
    }

    @Test
    fun testCompilerTestAccessInternalProduction() {
        val project = Project("kotlin2JsInternalTest", "2.10")

        project.build("runRhino") {
            assertSuccessful()
        }
    }

    @Test
    fun testJsCustomSourceSet() {
        val project = Project("kotlin2JsProjectWithCustomSourceset", "2.10")

        project.build("build") {
            assertSuccessful()

            assertContains(
                    ":compileKotlin2Js",
                    ":compileIntegrationTestKotlin2Js"
            )

            assertFileExists("build/kotlin2js/main/module.js")
            assertFileExists("build/kotlin2js/integrationTest/module-inttests.js")

            val jarPath = "build/libs/kotlin2JsProjectWithCustomSourceset-inttests.jar"
            assertFileExists(jarPath)
            val jar = ZipFile(fileInWorkingDir(jarPath))
            assertEquals(1, jar.entries().asSequence().count { it.name == "module-inttests.js" },
                         "The jar should contain an entry `module-inttests.js` with no duplicates")
        }
    }

    @Test
    fun testKotlinJsBuiltins() {
        val project = Project("kotlinBuiltins", "3.2")

        project.setupWorkingDir()
        val buildGradle = File(project.projectDir, "app").getFileByName("build.gradle")
        buildGradle.modify {
            it.replace("apply plugin: \"kotlin\"", "apply plugin: \"kotlin2js\"") +
                    "\ncompileKotlin2Js.kotlinOptions.outputFile = \"out/out.js\""
        }
        project.build("build") {
            assertSuccessful()
        }
    }
}
