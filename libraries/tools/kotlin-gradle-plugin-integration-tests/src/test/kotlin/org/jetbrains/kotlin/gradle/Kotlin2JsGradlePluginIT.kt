package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.targets.js.JsCompilerType
import org.jetbrains.kotlin.gradle.tasks.USING_JS_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.tasks.USING_JS_IR_BACKEND_MESSAGE
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.jetbrains.kotlin.gradle.util.jsCompilerType
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Kotlin2JsIrGradlePluginIT : AbstractKotlin2JsGradlePluginIT(true) {
    @Test
    fun generateDts() {
        val project = Project("kotlin2JsIrDtsGeneration")
        project.build("build") {
            assertSuccessful()
            checkIrCompilationMessage()

            assertFileExists("build/kotlin2js/main/lib.js")
            val dts = fileInWorkingDir("build/kotlin2js/main/lib.d.ts")
            assert(dts.exists())
            assert(dts.readText().contains("function bar(): string"))
        }
    }

    @Test
    fun testCleanOutputWithEmptySources() {
        with(Project("kotlin-js-nodejs-project", GradleVersionRequired.AtLeast("5.2"))) {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)

            build(
                "build",
                options = defaultBuildOptions().copy(jsCompilerType = JsCompilerType.ir)
            ) {
                assertSuccessful()

                assertTasksExecuted(":compileProductionKotlinJs")

                assertFileExists("build/js/packages/kotlin-js-nodejs/kotlin/kotlin-js-nodejs.js")
            }

            File("${projectDir.canonicalPath}/src").deleteRecursively()

            gradleBuildScript().appendText(
                """${"\n"}
                tasks {
                    compileProductionKotlinJs {
                        type = org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrType.DEVELOPMENT
                    }
                }
            """.trimIndent()
            )

            build(
                "build",
                options = defaultBuildOptions().copy(jsCompilerType = JsCompilerType.ir)
            ) {
                assertSuccessful()

                assertNoSuchFile("build/js/packages/kotlin-js-nodejs/kotlin/")
            }
        }
    }
}

class Kotlin2JsGradlePluginIT : AbstractKotlin2JsGradlePluginIT(false) {
    @Test
    fun testKotlinJsBuiltins() {
        val project = Project("kotlinBuiltins")

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
}

abstract class AbstractKotlin2JsGradlePluginIT(private val irBackend: Boolean) : BaseGradleIT() {
    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(jsIrBackend = irBackend)

    protected fun CompiledProject.checkIrCompilationMessage() {
        if (irBackend) {
            assertContains(USING_JS_IR_BACKEND_MESSAGE)
        } else {
            assertNotContains(USING_JS_IR_BACKEND_MESSAGE)
        }
    }

    @Test
    fun testBuildAndClean() {
        val project = Project("kotlin2JsProject")

        project.build("build") {
            assertSuccessful()
            assertReportExists()
            checkIrCompilationMessage()

            assertTasksExecuted(
                ":libraryProject:jarSources",
                ":mainProject:compileKotlin2Js",
                ":libraryProject:compileKotlin2Js"
            )

            assertFileExists("mainProject/web/js/app.js")
            if (!irBackend) {
                assertFileExists("mainProject/web/js/lib/kotlin.js")
                assertFileExists("libraryProject/build/kotlin2js/main/test-library.js")
                assertFileExists("mainProject/web/js/app.js.map")
            }
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
            checkIrCompilationMessage()

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
            checkIrCompilationMessage()

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
            checkIrCompilationMessage()
        }
    }

    @Test
    fun testDefaultOutputFile() {
        val project = Project("kotlin2JsNoOutputFileProject")

        project.build("build") {
            assertSuccessful()
            checkIrCompilationMessage()
            if (irBackend) {
                assertFileExists(kotlinClassesDir() + "default/manifest")
            } else {
                assertFileExists(kotlinClassesDir() + "kotlin2JsNoOutputFileProject.js")
            }
            assertFileExists(kotlinClassesDir(sourceSet = "test") + "kotlin2JsNoOutputFileProject_test.js")
        }
    }

    @Test
    fun testCompileTestCouldAccessProduction() {
        val project = Project("kotlin2JsProjectWithTests")

        project.build("build") {
            assertSuccessful()
            checkIrCompilationMessage()

            assertTasksExecuted(
                ":compileKotlin2Js",
                ":compileTestKotlin2Js"
            )
            if (irBackend) {
                assertFileExists("build/kotlin2js/main/default/manifest")
            } else {
                assertFileExists("build/kotlin2js/main/module.js")
            }
            assertFileExists("build/kotlin2js/test/module-tests.js")
        }
    }

    @Test
    fun testCompilerTestAccessInternalProduction() {
        val project = Project("kotlin2JsInternalTest")

        project.build("runRhino") {
            assertSuccessful()
            checkIrCompilationMessage()
        }
    }

    @Test
    fun testJsCustomSourceSet() {
        val project = Project("kotlin2JsProjectWithCustomSourceset")

        project.build("build") {
            assertSuccessful()
            checkIrCompilationMessage()

            assertTasksExecuted(
                ":compileKotlin2Js",
                ":compileIntegrationTestKotlin2Js"
            )

            if (!irBackend) {
                assertFileExists("build/kotlin2js/main/module.js")
            }
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
    fun testKotlinJsSourceMap() {
        // TODO: Support source maps
        assumeFalse(irBackend)

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
        // TODO: Support source maps
        assumeFalse(irBackend)

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

    /** Issue: KT-18495 */
    @Test
    fun testNoSeparateClassesDirWarning() {
        val project = Project("kotlin2JsProject")
        project.build("build") {
            assertSuccessful()
            checkIrCompilationMessage()
            assertNotContains("this build assumes a single directory for all classes from a source set")
        }
    }

    @Test
    fun testIncrementalCompilation() = Project("kotlin2JsICProject").run {
        build("build") {
            assertSuccessful()
            checkIrCompilationMessage()
            if (!irBackend) { // TODO: Support incremental compilation
                assertContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
                assertCompiledKotlinSources(project.relativize(allKotlinFiles))
            }
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
            checkIrCompilationMessage()
            // TODO: Support incremental compilation in IR backend
            if (!irBackend) {
                assertContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
                val affectedFiles = project.projectDir.getFilesByNames("A.kt", "useAInLibMain.kt", "useAInAppMain.kt", "useAInAppTest.kt")
                assertCompiledKotlinSources(project.relativize(affectedFiles))
            }
        }
    }

    @Test
    fun testIncrementalCompilationDisabled() = Project("kotlin2JsICProject").run {
        val options = defaultBuildOptions().copy(incrementalJs = false)

        build("build", options = options) {
            assertSuccessful()
            checkIrCompilationMessage()
            assertNotContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
        }
    }

    @Test
    fun testNewKotlinJsPlugin() = with(Project("kotlin-js-plugin-project", GradleVersionRequired.AtLeast("4.10.2"))) {
        assumeFalse(irBackend) // TODO: Support IR version of kotlinx.html
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)

        build("publish", "runDceKotlin", "test", "runDceBenchmarkKotlin") {
            assertSuccessful()

            assertTasksExecuted(
                ":compileKotlinJs", ":compileTestKotlinJs", ":compileBenchmarkKotlinJs",
                ":runDceKotlin", ":runDceBenchmarkKotlin"
            )

            val moduleDir = "build/repo/com/example/kotlin-js-plugin/1.0/"

            val publishedJar = fileInWorkingDir(moduleDir + "kotlin-js-plugin-1.0.jar")
            ZipFile(publishedJar).use { zip ->
                val entries = zip.entries().asSequence().map { it.name }
                assertTrue { "kotlin-js-plugin.js" in entries }
            }

            val publishedPom = fileInWorkingDir(moduleDir + "kotlin-js-plugin-1.0.pom")
            val kotlinVersion = defaultBuildOptions().kotlinVersion
            val pomText = publishedPom.readText().replace(Regex("\\s+"), "")
            assertTrue { "kotlinx-html-js</artifactId><version>0.6.10</version><scope>compile</scope>" in pomText }
            assertTrue { "kotlin-stdlib-js</artifactId><version>$kotlinVersion</version><scope>runtime</scope>" in pomText }

            assertFileExists(moduleDir + "kotlin-js-plugin-1.0-sources.jar")

            assertFileExists("build/js/node_modules/kotlin/kotlin.js")
            assertFileExists("build/js/node_modules/kotlin/kotlin.js.map")
            assertFileExists("build/js/node_modules/kotlin-test/kotlin-test.js")
            assertFileExists("build/js/node_modules/kotlin-test/kotlin-test.js.map")
            assertFileExists("build/js/node_modules/kotlin-test-js-runner/kotlin-test-nodejs-runner.js")
            assertFileExists("build/js/node_modules/kotlin-test-js-runner/kotlin-test-nodejs-runner.js.map")
            assertFileExists("build/js/node_modules/kotlin-js-plugin/kotlin/kotlin-js-plugin.js")
            assertFileExists("build/js/node_modules/kotlin-js-plugin/kotlin/kotlin-js-plugin.js.map")
            assertFileExists("build/js/node_modules/kotlin-js-plugin-test/kotlin/kotlin-js-plugin-test.js")
            assertFileExists("build/js/node_modules/kotlin-js-plugin-test/kotlin/kotlin-js-plugin-test.js.map")

            assertTestResults("testProject/kotlin-js-plugin-project/tests.xml", "nodeTest")
        }
    }

    @Test
    fun testYarnSetup() = with(Project("yarn-setup", GradleVersionRequired.AtLeast("4.10.2"))) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)

        build("yarnFolderRemove") {
            assertSuccessful()
        }

        build("kotlinYarnSetup", "yarnFolderCheck") {
            assertSuccessful()

            assertTasksExecuted(
                ":kotlinYarnSetup",
                ":yarnFolderCheck"
            )
        }

        gradleBuildScript().appendText("\nyarn.version = \"1.9.3\"")

        build("yarnConcreteVersionFolderChecker") {
            assertSuccessful()

            assertTasksExecuted(
                ":kotlinYarnSetup",
                ":yarnConcreteVersionFolderChecker"
            )
        }
    }

    @Test
    fun testNpmDependenciesClash() = with(Project("npm-dependencies-clash", GradleVersionRequired.AtLeast("4.10.2"))) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        build("test") {
            assertSuccessful()
        }
    }

    @Test
    fun testBrowserDistribution() = with(Project("kotlin-js-browser-project", GradleVersionRequired.AtLeast("4.10.2"))) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)

        if (irBackend) {
            gradleProperties().appendText(jsCompilerType(JsCompilerType.ir))
        }

        build("build") {
            assertSuccessful()

            assertTasksExecuted(":app:browserProductionWebpack")

            assertFileExists("build/js/packages/kotlin-js-browser-base")
            assertFileExists("build/js/packages/kotlin-js-browser-lib")
            assertFileExists("build/js/packages/kotlin-js-browser-app")

            assertFileExists("app/build/distributions/app.js")

            if (!irBackend) {
                assertTasksExecuted(":app:processDceKotlinJs")

                assertFileExists("build/js/packages/kotlin-js-browser-app/kotlin-dce")

                assertFileExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin.js")
                assertFileExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin-js-browser-app.js")
                assertFileExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin-js-browser-lib.js")
                assertFileExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin-js-browser-base.js")

                assertFileExists("app/build/distributions/app.js.map")
            }
        }

        build("clean", "browserDistribution") {
            assertTasksExecuted(
                ":app:processResources",
                ":app:browserDistributeResources"
            )

            assertFileExists("app/build/distributions/index.html")
        }
    }


}
