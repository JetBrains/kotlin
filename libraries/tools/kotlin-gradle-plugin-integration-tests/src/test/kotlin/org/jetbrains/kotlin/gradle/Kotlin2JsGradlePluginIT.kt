/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.google.gson.Gson
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.sources.METADATA_CONFIGURATION_NAME_SUFFIX
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.tasks.USING_JS_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.tasks.USING_JS_IR_BACKEND_MESSAGE
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Assert
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.io.File
import java.io.FileFilter
import java.util.zip.ZipFile
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Kotlin2JsIrGradlePluginIT : AbstractKotlin2JsGradlePluginIT(true) {

    override fun defaultBuildOptions(): BuildOptions {
        return super.defaultBuildOptions().copy(warningMode = WarningMode.Summary)
    }

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
        with(Project("kotlin-js-nodejs-project")) {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)

            build(
                "build",
                options = defaultBuildOptions().copy(jsCompilerType = KotlinJsCompilerType.IR)
            ) {
                assertSuccessful()

                assertTasksExecuted(":compileProductionExecutableKotlinJs")

                assertFileExists("build/js/packages/kotlin-js-nodejs/kotlin/kotlin-js-nodejs.js")
            }

            File("${projectDir.canonicalPath}/src").deleteRecursively()

            gradleBuildScript().appendText(
                """${"\n"}
                tasks.named("compileProductionExecutableKotlinJs").configure {
                    mode = org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.DEVELOPMENT
                }
            """.trimIndent()
            )

            build(
                "build",
                options = defaultBuildOptions().copy(jsCompilerType = KotlinJsCompilerType.IR)
            ) {
                assertSuccessful()

                assertNoSuchFile("build/js/packages/kotlin-js-nodejs/kotlin/")
            }
        }
    }

    @Test
    fun testJsCompositeBuild() {
        val rootProjectName = "js-composite-build"
        val appProject = transformProjectWithPluginsDsl(
            projectName = "app",
            directoryPrefix = rootProjectName,
        )

        val libProject = transformProjectWithPluginsDsl(
            projectName = "lib",
            directoryPrefix = rootProjectName,
        )

        libProject.gradleProperties().appendText(jsCompilerType(KotlinJsCompilerType.IR))
        appProject.gradleProperties().appendText(jsCompilerType(KotlinJsCompilerType.IR))

        libProject.projectDir.copyRecursively(appProject.projectDir.resolve(libProject.projectDir.name))

        fun Project.asyncVersion(rootModulePath: String, moduleName: String): String =
            NpmProjectModules(projectDir.resolve(rootModulePath))
                .require(moduleName)
                .let { File(it).parentFile.parentFile.resolve(NpmProject.PACKAGE_JSON) }
                .let { fromSrcPackageJson(it) }
                .let { it!!.version }

        with(appProject) {
            build("build") {
                assertSuccessful()
                val appAsyncVersion = asyncVersion("build/js/node_modules/app", "async")
                assertEquals("3.2.0", appAsyncVersion)

                val libAsyncVersion = asyncVersion("build/js/node_modules/lib2", "async")
                assertEquals("2.6.2", libAsyncVersion)
            }
        }
    }

    @Test
    fun testJsIrIncrementalInParallel() = with(Project("kotlin-js-browser-project")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleProperties().appendText("\nkotlin.incremental.js.ir=true")
        gradleProperties().appendText("\norg.gradle.parallel=true")

        build(
            "assemble",
            options = defaultBuildOptions()
                .copy(
                    jsCompilerType = KotlinJsCompilerType.IR,
                    warningMode = WarningMode.Fail
                )
        ) {
            assertSuccessful()
        }
    }
}

class Kotlin2JsGradlePluginIT : AbstractKotlin2JsGradlePluginIT(false) {

    override fun defaultBuildOptions(): BuildOptions {
        return super.defaultBuildOptions().copy(warningMode = WarningMode.Summary)
    }

    @Test
    fun testIncrementalCompilation() = Project("kotlin2JsICProject").run {
        setupWorkingDir()
        val modules = listOf("app", "lib")
        val mainFiles = modules.flatMapTo(LinkedHashSet()) {
            projectDir.resolve("$it/src/main").allKotlinFiles()
        }

        build("build") {
            assertSuccessful()
            checkIrCompilationMessage()
            assertContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
            if (irBackend) {
                assertCompiledKotlinSources(project.relativize(mainFiles))
            } else {
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
            assertContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
            val affectedFiles = project.projectDir.getFilesByNames("A.kt", "useAInLibMain.kt", "useAInAppMain.kt", "useAInAppTest.kt")
            if (irBackend) {
                // only klib ic is supported for now, so tests are generated non-incrementally with ir backend
                assertCompiledKotlinSources(project.relativize(affectedFiles.filter { it in mainFiles }))
            } else {
                assertCompiledKotlinSources(project.relativize(affectedFiles))
            }
        }
    }

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

    @Test
    fun testKotlinJsDependencyWithJsFiles() = with(Project("kotlin-js-dependency-with-js-files")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        build(
            "packageJson"
        ) {
            assertSuccessful()

            val dependency = "2p-parser-core"
            val version = "0.11.1"

            assertFileExists("build/js/packages_imported/$dependency/$version")

            fun getPackageJson(dependency: String, version: String) =
                fileInWorkingDir("build/js/packages_imported/$dependency/$version")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let {
                        Gson().fromJson(it.readText(), PackageJson::class.java)
                    }


            val packageJson = getPackageJson(dependency, version)

            assertEquals(dependency, packageJson.name)
            assertEquals(version, packageJson.version)
            assertEquals("$dependency.js", packageJson.main)
        }
    }
}

abstract class AbstractKotlin2JsGradlePluginIT(val irBackend: Boolean) : BaseGradleIT() {
    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(
            jsIrBackend = irBackend,
            jsCompilerType = if (irBackend) KotlinJsCompilerType.IR else KotlinJsCompilerType.LEGACY
        )

    protected fun CompiledProject.checkIrCompilationMessage() {
        if (irBackend) {
            assertContains(USING_JS_IR_BACKEND_MESSAGE)
        } else {
            assertNotContains(USING_JS_IR_BACKEND_MESSAGE)
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
            if (!irBackend) {
                assertEquals(
                    1, jar.entries().asSequence().count { it.name == "kotlin2JsNoOutputFileProject.js" },
                    "The jar should contain an entry `kotlin2JsNoOutputFileProject.js` with no duplicates"
                )
            }
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
            if (!irBackend) {
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

    @Test
    fun testIncrementalCompilationDisabled() = Project("kotlin2JsICProject").run {
        val options = defaultBuildOptions().run {
            if (irBackend) copy(incrementalJsKlib = false) else copy(incrementalJs = false)
        }

        build("build", options = options) {
            assertSuccessful()
            checkIrCompilationMessage()
            assertNotContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
        }
    }

    @Test
    fun testNewKotlinJsPlugin() = with(Project("kotlin-js-plugin-project")) {
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
            assertTrue { "kotlinx-html-js</artifactId><version>0.6.12</version><scope>compile</scope>" in pomText }
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

            // Gradle 6.6+ slightly changed format of xml test results
            val testGradleVersion = GradleVersion.version(project.chooseWrapperVersionOrFinishTest())
            if (testGradleVersion < GradleVersion.version("6.6")) {
                assertTestResults("testProject/kotlin-js-plugin-project/tests_pre6.6.xml", "nodeTest")
            } else {
                assertTestResults("testProject/kotlin-js-plugin-project/tests.xml", "nodeTest")
            }
        }
    }

    @Test
    fun testYarnSetup() = with(Project("yarn-setup")) {
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
    fun testNpmDependencies() = with(Project("npm-dependencies")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        build("build") {
            assertSuccessful()
            assertFileExists("build/js/node_modules/file-dependency")
            assertFileExists("build/js/node_modules/file-dependency-2")
            assertFileExists("build/js/node_modules/file-dependency-3/index.js")
            assertFileExists("build/js/node_modules/42/package.json")
        }
    }

    @Test
    fun testPackageJsonWithPublicNpmDependencies() = with(Project("npm-dependencies")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        build("jsJar") {
            assertSuccessful()

            val archive = fileInWorkingDir("build")
                .resolve("libs")
                .listFiles(FileFilter {
                    it.extension == if (irBackend) KLIB_TYPE else "jar"
                })!!
                .single()

            val zipFile = ZipFile(archive)
            val packageJsonCandidates = zipFile.entries()
                .asSequence()
                .filter { it.name == NpmProject.PACKAGE_JSON }
                .toList()

            assertTrue("Expected existence of package.json in archive") {
                packageJsonCandidates.size == 1
            }

            zipFile.getInputStream(packageJsonCandidates.single()).use {
                it.reader().use {
                    val packageJson = Gson().fromJson(it, PackageJson::class.java)
                    val devDep = "42"
                    val devDepVersion = "0.0.1"
                    assertTrue(
                        "Dev dependency \"$devDep\": \"$devDepVersion\" in package.json expected, but actual:\n" +
                                "${packageJson.devDependencies}"
                    ) {
                        val devDependencies = packageJson.devDependencies
                        devDependencies
                            .containsKey(devDep) &&
                                devDependencies[devDep] == devDepVersion
                    }

                    val dep = "@yworks/optimizer"
                    val depVersion = "1.0.6"
                    assertTrue(
                        "Dependency \"$dep\": \"$depVersion\" in package.json expected, but actual:\n" +
                                "${packageJson.dependencies}"
                    ) {
                        val dependencies = packageJson.dependencies
                        dependencies
                            .containsKey(dep) &&
                                dependencies[dep] == depVersion
                    }

                    val peerDep = "date-arithmetic"
                    val peerDepVersion = "4.1.0"
                    assertTrue(
                        "Peer dependency \"$peerDep\": \"$peerDepVersion\" in package.json expected, but actual:\n" +
                                "${packageJson.peerDependencies}"
                    ) {
                        val peerDependencies = packageJson.peerDependencies
                        peerDependencies
                            .containsKey(peerDep) &&
                                peerDependencies[peerDep] == peerDepVersion
                    }
                }
            }
        }

        val newGradleBuildScript = gradleBuildScript()
            .useLines { lines ->
                lines
                    .filterNot {
                        it.contains("npm", ignoreCase = true)
                    }
                    .joinToString("\n")
            }
        gradleBuildScript().modify {
            newGradleBuildScript
        }

        build("jsJar") {
            assertSuccessful()
            val archive = fileInWorkingDir("build")
                .resolve("libs")
                .listFiles(FileFilter {
                    it.extension == if (irBackend) KLIB_TYPE else "jar"
                })!!
                .single()

            val packageJsonCandidates = ZipFile(archive).entries()
                .asSequence()
                .filter { it.name == NpmProject.PACKAGE_JSON }
                .toList()

            assertTrue("Expected absence of package.json in ${archive.name}") {
                packageJsonCandidates.isEmpty()
            }
        }
    }

    @Test
    fun testBrowserDistribution() = with(Project("kotlin-js-browser-project")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)

        if (irBackend) {
            gradleProperties().appendText(jsCompilerType(KotlinJsCompilerType.IR))
        }

        build("build") {
            assertSuccessful()

            assertTasksExecuted(":app:browserProductionWebpack")

            assertFileExists("build/js/packages/kotlin-js-browser-base-js-ir")
            assertFileExists("build/js/packages/kotlin-js-browser-base-js-legacy")
            assertFileExists("build/js/packages/kotlin-js-browser-lib")
            assertFileExists("build/js/packages/kotlin-js-browser-app")

            assertFileExists("app/build/distributions/app.js")

            if (!irBackend) {
                assertTasksExecuted(":app:processDceKotlinJs")

                assertFileExists("build/js/packages/kotlin-js-browser-app/kotlin-dce")

                assertFileExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin.js")
                assertFileExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin-js-browser-app.js")
                assertFileExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin-js-browser-lib.js")
                assertFileExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin-js-browser-base-js-legacy.js")

                assertFileExists("app/build/distributions/app.js.map")
            }
        }

        build("clean", "browserDistribution") {
            assertTasksExecuted(
                ":app:processResources",
                if (irBackend) ":app:browserProductionExecutableDistributeResources" else ":app:browserDistributeResources"
            )

            assertFileExists("app/build/distributions/index.html")
        }
    }

    @Test
    fun testResolveJsProjectDependencyToMetadata() = with(Project("kotlin-js-browser-project")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleProperties().appendText("kotlin.js.compiler=both")

        val compiler = if (irBackend) "IR" else "LEGACY"

        val pathPrefix = "metadataDependency: "

        val appBuild = projectDir.resolve("app/build.gradle.kts")
        appBuild.modify {
            it.replace("target {", "js($compiler) {")
        }
        appBuild.appendText(
            "\n" + """
                kotlin.sourceSets {
                    val main by getting {
                        dependencies {
                            // add these dependencies to check that they are resolved to metadata
                                api(project(":base"))
                                implementation(project(":base"))
                                compileOnly(project(":base"))
                                runtimeOnly(project(":base"))
                            }
                        }
                    }
                    
                task("printMetadataFiles") {
                    doFirst {
                        listOf("api", "implementation", "compileOnly", "runtimeOnly").forEach { kind ->
                            val configuration = configurations.getByName(kind + "DependenciesMetadata")
                            configuration.files.forEach { println("$pathPrefix" + configuration.name + "->" + it.name) }
                        }
                    }
                }
            """.trimIndent()
        )

        val metadataDependencyRegex = "$pathPrefix(.*?)->(.*)".toRegex()

        build(
            "printMetadataFiles",
            options = defaultBuildOptions().copy(jsCompilerType = if (irBackend) KotlinJsCompilerType.IR else KotlinJsCompilerType.LEGACY)
        ) {
            assertSuccessful()

            val suffix = if (irBackend) "ir" else "legacy"
            val ext = if (irBackend) "klib" else "jar"

            val expectedFileName = "base-$suffix.$ext"

            val paths = metadataDependencyRegex
                .findAll(output).map { it.groupValues[1] to it.groupValues[2] }
                .filter { (_, f) -> "base" in f }
                .toSet()

            Assert.assertEquals(
                listOf("api", "implementation", "compileOnly", "runtimeOnly").map {
                    "$it$METADATA_CONFIGURATION_NAME_SUFFIX" to expectedFileName
                }.toSet(),
                paths
            )
        }
    }

    @Test
    fun testNoUnintendedDevDependencies() = with(Project("kotlin-js-browser-project")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)

        build("browserProductionWebpack") {
            assertSuccessful()

            val appPackageJson = getSubprojectPackageJson(projectName = "kotlin-js-browser", subProject = "app")
            val libPackageJson = getSubprojectPackageJson(projectName = "kotlin-js-browser", subProject = "lib")

            assertTrue("${appPackageJson.name} should contain css-loader") {
                "css-loader" in appPackageJson.devDependencies
            }
            assertFalse("${libPackageJson.name} shouldn't contain css-loader") {
                "css-loader" in libPackageJson.devDependencies
            }
        }
    }

    @Test
    fun testYarnResolution() = with(Project("kotlin-js-yarn-resolutions")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        build("packageJson", "rootPackageJson", "kotlinNpmInstall") {
            assertSuccessful()

            fun getPackageJson() =
                fileInWorkingDir("build/js")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let {
                        Gson().fromJson(it.readText(), PackageJson::class.java)
                    }

            val name = "lodash"
            val version = getPackageJson().resolutions?.get(name)
            val requiredVersion = ">=1.0.0 <1.2.1 || >1.4.0 <2.0.0"
            assertTrue("Root package.json must have resolution $name with version $requiredVersion, but $version found") {
                version == requiredVersion
            }

            val react = "react"
            val reactVersion = getPackageJson().resolutions?.get(react)
            val requiredReactVersion = "16.0.0"
            assertTrue("Root package.json must have resolution $react with version $requiredReactVersion, but $reactVersion found") {
                reactVersion == requiredReactVersion
            }
        }
    }

    @Test
    fun testDirectoryDependencyNotFailProjectResolution() {
        with(Project("kotlin-js-nodejs-project")) {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)

            gradleBuildScript().appendText(
                """${"\n"}
                dependencies {
                    implementation(files("${"$"}{projectDir}/custom"))
                    implementation(files("${"$"}{projectDir}/custom2"))
                }
            """.trimIndent()
            )

            build(
                "packageJson"
            ) {
                assertSuccessful()
            }
        }
    }

    @Test
    fun testDynamicWebpackConfigD() {
        with(Project("js-dynamic-webpack-config-d")) {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

            build(
                "build"
            ) {
                assertSuccessful()
                assertFileExists("build/js/packages/js-dynamic-webpack-config-d")
                assertFileContains("build/js/packages/js-dynamic-webpack-config-d/webpack.config.js", "// hello from patch.js")
            }
        }
    }

    @Test
    fun testBrowserNoTasksConfigurationOnHelp() = with(transformProjectWithPluginsDsl("kotlin-js-browser-project")) {
        gradleBuildScript().appendText(
            "\n" + """
            allprojects {
                tasks.configureEach {
                    if (this is org.gradle.configuration.Help) return@configureEach
                    throw GradleException("Task ${'$'}{path} shouldn't be configured")
                }
            }
        """.trimIndent()
        )
        build {
            assertSuccessful()
        }
    }

    @Test
    fun testNodeJsNoTasksConfigurationOnHelp() = with(transformProjectWithPluginsDsl("kotlin-js-nodejs-project")) {
        gradleBuildScript().appendText(
            "\n" + """
            allprojects {
                tasks.configureEach {
                    if (it instanceof org.gradle.configuration.Help) return
                    throw new GradleException("Task ${'$'}{path} shouldn't be configured")
                }
            }
        """.trimIndent()
        )
        build {
            assertSuccessful()
        }
    }

    private fun CompiledProject.getSubprojectPackageJson(subProject: String, projectName: String? = null) =
        fileInWorkingDir("build/js/packages/${projectName ?: project.projectName}-$subProject")
            .resolve(NpmProject.PACKAGE_JSON)
            .let {
                Gson().fromJson(it.readText(), PackageJson::class.java)
            }

    @Test
    fun testWebpackConfig() {
        with(Project("kotlin-js-test-webpack-config")) {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

            build(
                "browserDevelopmentWebpack"
            ) {
                assertSuccessful()
            }

            build(
                "checkConfigDevelopmentWebpack",
            ) {
                assertSuccessful()
            }

            build(
                "checkConfigProductionWebpack",
            ) {
                assertSuccessful()
            }

            build(
                "checkConfigDevelopmentRun",
            ) {
                assertSuccessful()
            }

            build(
                "checkConfigProductionRun",
            ) {
                assertSuccessful()
            }
        }
    }
}

class GeneralKotlin2JsGradlePluginIT : BaseGradleIT() {
    @Test
    fun testJsBothModeWithTests() = with(Project("kotlin-js-both-mode-with-tests")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        build("build") {
            assertSuccessful()
            assertNoWarnings()
        }
    }

    @Test
    fun testNodeJsAndYarnDownload() = with(transformProjectWithPluginsDsl("cleanTask")) {

        build("checkDownloadedFolder") {
            assertSuccessful()
        }

        build("checkIfLastModifiedNotNow", "--rerun-tasks") {
            assertSuccessful()
        }
    }

    @Test
    fun testYarnLockStore(gradleVersion: GradleVersion) {
        project("cleanTask", gradleVersion) {
            buildGradle.modify(::transformBuildScriptWithPluginsDsl)
            build("assemble") {
                assertFileExists(projectPath.resolve("kotlin-js-store").resolve("yarn.lock"))
                assert(projectPath.resolve("kotlin-js-store").resolve("yarn.lock").readText() == projectPath.resolve("build/js/yarn.lock").readText())
            }
        }
    }
}