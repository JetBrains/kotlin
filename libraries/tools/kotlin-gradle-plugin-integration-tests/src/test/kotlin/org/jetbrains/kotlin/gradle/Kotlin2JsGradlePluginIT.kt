package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.tasks.USING_EXPERIMENTAL_JS_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Kotlin2JsGradlePluginIT : BaseGradleIT() {
    @Test
    fun testBuildAndClean() {
        val project = Project("kotlin2JsProject")

        project.build("build") {
            assertSuccessful()
            assertReportExists()

            assertTasksExecuted(
                ":libraryProject:jarSources",
                ":mainProject:compileKotlin2Js",
                ":libraryProject:compileKotlin2Js"
            )

            listOf(
                "mainProject/web/js/app.js",
                "mainProject/web/js/lib/kotlin.js",
                "libraryProject/build/kotlin2js/main/test-library.js",
                "mainProject/web/js/app.js.map"
            ).forEach { assertFileExists(it) }
        }

        project.build("build") {
            assertSuccessful()
            assertTasksUpToDate(":mainProject:compileKotlin2Js")
            assertContainsRegex(":libraryProject:compileTestKotlin2Js (UP-TO-DATE|NO-SOURCE)".toRegex())
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
        val project = Project("kotlin2JsNoOutputFileProject")

        project.build("jar") {
            assertSuccessful()

            assertTasksExecuted(":compileKotlin2Js")
            val jarPath = "build/libs/kotlin2JsNoOutputFileProject.jar"
            assertFileExists(jarPath)
            val jar = ZipFile(fileInWorkingDir(jarPath))
            assertEquals(
                1, jar.entries().asSequence().count { it.name == "kotlin2JsNoOutputFileProject.js" },
                "The jar should contain an entry `kotlin2JsNoOutputFileProject.js` with no duplicates"
            )
        }
    }

    @Test
    fun testJarIncludesJsOutputSetExplicitly() {
        val project = Project("kotlin2JsModuleKind")

        project.build(":jar") {
            assertSuccessful()

            assertTasksExecuted(":compileKotlin2Js")
            val jarPath = "build/libs/kotlin2JsModuleKind.jar"
            assertFileExists(jarPath)
            val jar = ZipFile(fileInWorkingDir(jarPath))
            assertEquals(
                1, jar.entries().asSequence().count { it.name == "app.js" },
                "The jar should contain an entry `app.js` with no duplicates"
            )
        }
    }

    @Test
    fun testModuleKind() {
        val project = Project("kotlin2JsModuleKind")

        project.build("runRhino") {
            assertSuccessful()
        }
    }

    @Test
    fun testDefaultOutputFile() {
        val project = Project("kotlin2JsNoOutputFileProject")

        project.build("build") {
            assertSuccessful()
            assertFileExists(kotlinClassesDir() + "kotlin2JsNoOutputFileProject.js")
            assertFileExists(kotlinClassesDir(sourceSet = "test") + "kotlin2JsNoOutputFileProject_test.js")
        }
    }

    @Test
    fun testCompileTestCouldAccessProduction() {
        val project = Project("kotlin2JsProjectWithTests")

        project.build("build") {
            assertSuccessful()

            assertTasksExecuted(
                ":compileKotlin2Js",
                ":compileTestKotlin2Js"
            )

            assertFileExists("build/kotlin2js/main/module.js")
            assertFileExists("build/kotlin2js/test/module-tests.js")
        }
    }

    @Test
    fun testCompilerTestAccessInternalProduction() {
        val project = Project("kotlin2JsInternalTest", GradleVersionRequired.Exact("3.5"))

        project.build("runRhino") {
            assertSuccessful()
        }
    }

    @Test
    fun testJsCustomSourceSet() {
        val project = Project("kotlin2JsProjectWithCustomSourceset")

        project.build("build") {
            assertSuccessful()

            assertTasksExecuted(
                ":compileKotlin2Js",
                ":compileIntegrationTestKotlin2Js"
            )

            assertFileExists("build/kotlin2js/main/module.js")
            assertFileExists("build/kotlin2js/integrationTest/module-inttests.js")

            val jarPath = "build/libs/kotlin2JsProjectWithCustomSourceset-inttests.jar"
            assertFileExists(jarPath)
            val jar = ZipFile(fileInWorkingDir(jarPath))
            assertEquals(
                1, jar.entries().asSequence().count { it.name == "module-inttests.js" },
                "The jar should contain an entry `module-inttests.js` with no duplicates"
            )
        }
    }

    @Test
    fun testKotlinJsBuiltins() {
        val project = Project("kotlinBuiltins", GradleVersionRequired.AtLeast("4.0"))

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
        val project = Project("kotlin2JsNoOutputFileProject")

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

            val sourceFilePath = "prefixprefix/src/main/kotlin/example/Dummy.kt"
            assertTrue("Source map should contain reference to $sourceFilePath") { map.contains("\"$sourceFilePath\"") }
        }
    }

    @Test
    fun testKotlinJsSourceMapInline() {
        val project = Project("kotlin2JsProjectWithSourceMapInline")

        project.build("build") {
            assertSuccessful()

            val mapFilePath = kotlinClassesDir(subproject = "app") + "app.js.map"
            assertFileExists(mapFilePath)
            val map = fileInWorkingDir(mapFilePath).readText()

            assertTrue("Source map should contain reference to main.kt") { map.contains("\"./src/main/kotlin/main.kt\"") }
            assertTrue("Source map should contain reference to foo.kt") { map.contains("\"./src/main/kotlin/foo.kt\"") }
            assertTrue("Source map should contain source of main.kt") { map.contains("\"fun main(args: Array<String>) {") }
            assertTrue("Source map should contain source of foo.kt") { map.contains("\"inline fun foo(): String {") }
        }
    }

    @Test
    fun testDce() {
        val project = Project("kotlin2JsDceProject", minLogLevel = LogLevel.INFO)

        project.build("runRhino") {
            assertSuccessful()
            val pathPrefix = "mainProject/build/kotlin-js-min/main"
            assertFileExists("$pathPrefix/exampleapp.js.map")
            assertFileExists("$pathPrefix/examplelib.js.map")
            assertFileContains("$pathPrefix/exampleapp.js.map", "\"../../../src/main/kotlin/exampleapp/main.kt\"")

            assertFileExists("$pathPrefix/kotlin.js")
            assertTrue(fileInWorkingDir("$pathPrefix/kotlin.js").length() < 500 * 1000, "Looks like kotlin.js file was not minified by DCE")
        }
    }

    @Test
    fun testDceOutputPath() {
        val project = Project("kotlin2JsDceProject", minLogLevel = LogLevel.INFO)

        project.setupWorkingDir()
        File(project.projectDir, "mainProject/build.gradle").modify {
            it + "\n" +
                    "runDceKotlinJs.dceOptions.outputDirectory = \"\${buildDir}/min\"\n" +
                    "runRhino.args = [\"-f\", \"min/kotlin.js\", \"-f\", \"min/examplelib.js\", \"-f\", \"min/exampleapp.js\"," +
                    "\"-f\", \"../check.js\"]\n"
        }

        project.build("runRhino") {
            assertSuccessful()
            val pathPrefix = "mainProject/build/min"
            assertFileExists("$pathPrefix/exampleapp.js.map")
            assertFileExists("$pathPrefix/examplelib.js.map")
            assertFileContains("$pathPrefix/exampleapp.js.map", "\"../../src/main/kotlin/exampleapp/main.kt\"")

            assertFileExists("$pathPrefix/kotlin.js")
            assertTrue(fileInWorkingDir("$pathPrefix/kotlin.js").length() < 500 * 1000, "Looks like kotlin.js file was not minified by DCE")
        }
    }

    @Test
    fun testDceDevMode() {
        val project = Project("kotlin2JsDceProject", minLogLevel = LogLevel.INFO)

        project.setupWorkingDir()
        File(project.projectDir, "mainProject/build.gradle").modify {
            it + "\n" +
                    "runDceKotlinJs.dceOptions.devMode = true\n"
        }

        project.build("runRhino") {
            assertSuccessful()
            val pathPrefix = "mainProject/build/kotlin-js-min/main"
            assertFileExists("$pathPrefix/exampleapp.js.map")
            assertFileExists("$pathPrefix/examplelib.js.map")
            assertFileContains("$pathPrefix/exampleapp.js.map", "\"../../../src/main/kotlin/exampleapp/main.kt\"")

            assertFileExists("$pathPrefix/kotlin.js")
            assertTrue(fileInWorkingDir("$pathPrefix/kotlin.js").length() > 1000 * 1000, "Looks like kotlin.js file was minified by DCE")
        }
    }

    @Test
    fun testDceFileCollectionDependency() {
        val project = Project("kotlin2JsDceProject", minLogLevel = LogLevel.INFO)

        project.setupWorkingDir()
        File(project.projectDir, "mainProject/build.gradle").modify {
            it.replace("compile project(\":libraryProject\")", "compile project(\":libraryProject\").sourceSets.main.output")
        }

        project.build("runRhino") {
            assertSuccessful()
            val pathPrefix = "mainProject/build/kotlin-js-min/main"
            assertFileExists("$pathPrefix/exampleapp.js.map")
            assertFileExists("$pathPrefix/examplelib.js.map")
            assertFileContains("$pathPrefix/exampleapp.js.map", "\"../../../src/main/kotlin/exampleapp/main.kt\"")

            assertFileExists("$pathPrefix/kotlin.js")
            assertTrue(fileInWorkingDir("$pathPrefix/kotlin.js").length() < 500 * 1000, "Looks like kotlin.js file was not minified by DCE")
        }
    }

    /** Issue: KT-18495 */
    @Test
    fun testNoSeparateClassesDirWarning() {
        val project = Project("kotlin2JsProject", GradleVersionRequired.AtLeast("4.0"))
        project.build("build") {
            assertSuccessful()
            assertNotContains("this build assumes a single directory for all classes from a source set")
        }
    }

    @Test
    fun testIncrementalCompilation() = Project("kotlin2JsICProject", GradleVersionRequired.AtLeast("4.0")).run {
        build("build") {
            assertSuccessful()
            assertContains(USING_EXPERIMENTAL_JS_INCREMENTAL_COMPILATION_MESSAGE)
            assertCompiledKotlinSources(project.relativize(allKotlinFiles))
        }

        build("build") {
            assertSuccessful()
            assertCompiledKotlinSources(emptyList())
        }

        projectFile("A.kt").modify {
            it.replace("val x = 0", "val x = \"a\"")
        }
        build("build") {
            assertSuccessful()
            assertContains(USING_EXPERIMENTAL_JS_INCREMENTAL_COMPILATION_MESSAGE)
            val affectedFiles = project.projectDir.getFilesByNames("A.kt", "useAInLibMain.kt", "useAInAppMain.kt", "useAInAppTest.kt")
            assertCompiledKotlinSources(project.relativize(affectedFiles))
        }
    }
}
