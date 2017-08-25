package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.tasks.USING_EXPERIMENTAL_JS_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.util.allKotlinFiles
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            assertEquals(1, jar.entries().asSequence().count { it.name == "kotlin2JsNoOutputFileProject.js" },
                         "The jar should contain an entry `kotlin2JsNoOutputFileProject.js` with no duplicates")
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
            assertFileExists("build/classes/main/kotlin2JsNoOutputFileProject.js")
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
        val project = Project("kotlinBuiltins", "4.0")

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

    @Test
    fun testKotlinJsSourceMap() {
        val project = Project("kotlin2JsNoOutputFileProject", "2.10")

        project.setupWorkingDir()

        project.projectDir.getFileByName("build.gradle").modify {
            it + "\n" +
                    "compileKotlin2Js.kotlinOptions.sourceMap = true\n" +
                    "compileKotlin2Js.kotlinOptions.sourceMapPrefix = \"prefixprefix/\"\n" +
                    "compileKotlin2Js.kotlinOptions.outputFile = \"\${buildDir}/kotlin2js/main/app.js\"\n"
        }

        project.build("build") {
            assertSuccessful()

            val mapFilePath = "build/kotlin2js/main/app.js.map"
            assertFileExists(mapFilePath)
            val map = fileInWorkingDir(mapFilePath).readText()

            val sourceFilePath = "prefixprefix/example/Dummy.kt"
            assertTrue("Source map should contain reference to $sourceFilePath") { map.contains("\"$sourceFilePath\"") }
        }
    }

    @Test
    fun testKotlinJsSourceMapInline() {
        val project = Project("kotlin2JsProjectWithSourceMapInline", "2.10")

        project.build("build") {
            assertSuccessful()

            val mapFilePath = "app/build/classes/main/app.js.map"
            assertFileExists(mapFilePath)
            val map = fileInWorkingDir(mapFilePath).readText()

            assertTrue("Source map should contain reference to main.kt") { map.contains("\"main.kt\"") }
            assertTrue("Source map should contain reference to foo.kt") { map.contains("\"foo.kt\"") }
            assertTrue("Source map should contain source of main.kt") { map.contains("\"fun main(args: Array<String>) {\\n") }
            assertTrue("Source map should contain source of foo.kt") { map.contains("\"inline fun foo(): String {\\n") }
        }
    }

    @Test
    fun testDce() {
        val project = Project("kotlin2JsDceProject", "2.10", minLogLevel = LogLevel.INFO)

        project.build("runRhino") {
            println(output)
            assertSuccessful()
            val pathPrefix = "mainProject/build/min"
            assertFileExists("$pathPrefix/exampleapp.js.map")
            assertFileExists("$pathPrefix/examplelib.js.map")
        }
    }

    /** Issue: KT-18495 */
    @Test
    fun testNoSeparateClassesDirWarning() {
        val project = Project("kotlin2JsProject", "4.0")
        project.build("build") {
            assertSuccessful()
            assertNotContains("this build assumes a single directory for all classes from a source set")
        }
    }

    @Test
    fun testIncrementalCompilation() {
        val project = Project("kotlin2JsICProject", "4.0")
        project.build("build") {
            assertSuccessful()
            assertContains(USING_EXPERIMENTAL_JS_INCREMENTAL_COMPILATION_MESSAGE)
            assertCompiledKotlinSources(project.relativize(project.projectDir.allKotlinFiles()))
        }

        val aKt = project.projectDir.getFileByName("A.kt").apply {
            modify { it.replace("val x: String", "val x: Int") }
        }
        val useAKt = project.projectDir.getFileByName("useA.kt")
        project.build("build") {
            assertSuccessful()
            assertContains(USING_EXPERIMENTAL_JS_INCREMENTAL_COMPILATION_MESSAGE)
            assertCompiledKotlinSources(project.relativize(aKt, useAKt))
        }
    }
}
