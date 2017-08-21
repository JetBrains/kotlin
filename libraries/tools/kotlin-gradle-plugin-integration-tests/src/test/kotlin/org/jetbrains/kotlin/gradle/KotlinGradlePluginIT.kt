/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.gradle.plugin.CopyClassesToJavaOutputStatus
import org.jetbrains.kotlin.gradle.tasks.USING_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KotlinGradleIT: BaseGradleIT() {

    companion object {
        private const val GRADLE_VERSION = "2.10"
    }

    @Test
    fun testCrossCompile() {
        val project = Project("kotlinJavaProject", GRADLE_VERSION)

        project.build("compileDeployKotlin", "build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin", ":compileDeployKotlin")
        }

        project.build("compileDeployKotlin", "build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE", ":compileTestKotlin UP-TO-DATE", ":compileDeployKotlin UP-TO-DATE", ":compileJava UP-TO-DATE")
        }
    }

    @Test
    fun testRunningInDifferentDir() {
        val wd0 = workingDir
        val wd1 = File(wd0, "subdir").apply { mkdirs() }
        workingDir = wd1
        val project1 = Project("kotlinJavaProject", "3.3")

        project1.build("assemble") {
            assertSuccessful()
        }

        val wd2 = FileUtil.createTempDirectory("testRunningInDifferentDir", null)
        wd1.copyRecursively(wd2)
        wd1.deleteRecursively()
        assert(!wd1.exists())
        wd0.setWritable(false)
        workingDir = wd2

        project1.build("test") {
            assertSuccessful()
        }
    }

    @Test
    fun testKotlinOnlyCompile() {
        val project = Project("kotlinProject", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertFileExists("build/classes/main/META-INF/kotlinProject.kotlin_module")
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
            assertNotContains("Forcing System.gc")
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE", ":compileTestKotlin UP-TO-DATE")
        }
    }

    // For corresponding documentation, see https://docs.gradle.org/current/userguide/gradle_daemon.html
    // Setting user.variant to different value implies a new daemon process will be created.
    // In order to stop daemon process, special exit task is used ( System.exit(0) ).
    @Test
    fun testKotlinOnlyDaemonMemory() {
        val project = Project("kotlinProject", GRADLE_VERSION)
        val VARIANT_CONSTANT = "ForTest"
        val userVariantArg = "-Duser.variant=$VARIANT_CONSTANT"
        val MEMORY_MAX_GROWTH_LIMIT_KB = 500
        val BUILD_COUNT = 15
        val reportMemoryUsage = "-Dkotlin.gradle.test.report.memory.usage=true"
        val options =  BaseGradleIT.BuildOptions(withDaemon = true)

        fun exitTestDaemon() {
            project.build(userVariantArg, reportMemoryUsage, "exit", options = options) {
                assertFailed()
                assertContains("The daemon has exited normally or was terminated in response to a user interrupt.")
            }
        }

        fun buildAndGetMemoryAfterBuild(): Int {
            var reportedMemory: Int? = null

            project.build(userVariantArg, reportMemoryUsage, "clean", "build", options = options) {
                assertSuccessful()
                val matches = "\\[KOTLIN\\]\\[PERF\\] Used memory after build: (\\d+) kb \\(difference since build start: ([+-]?\\d+) kb\\)"
                        .toRegex().find(output)
                assert(matches != null && matches.groups.size == 3) { "Used memory after build is not reported by plugin" }
                reportedMemory = matches!!.groupValues[1].toInt()
            }

            return reportedMemory!!
        }

        exitTestDaemon()

        try {
            val usedMemory = (1..BUILD_COUNT).map { buildAndGetMemoryAfterBuild() }

            // ensure that the maximum of the used memory established after several first builds doesn't raise significantly in the subsequent builds
            val establishedMaximum = usedMemory.take(5).max()!!
            val totalMaximum = usedMemory.max()!!

            val maxGrowth = totalMaximum - establishedMaximum
            assertTrue(maxGrowth <= MEMORY_MAX_GROWTH_LIMIT_KB,
                    "Maximum used memory over series of builds growth $maxGrowth (from $establishedMaximum to $totalMaximum) kb > $MEMORY_MAX_GROWTH_LIMIT_KB kb")

            // testing that nothing remains locked by daemon, see KT-9440
            project.build(userVariantArg, "clean", options = BaseGradleIT.BuildOptions(withDaemon = true)) {
                assertSuccessful()
            }
        }
        finally {
            exitTestDaemon()
        }
    }

    @Test
    fun testLogLevelForceGC() {
        val debugProject = Project("simpleProject", GRADLE_VERSION, minLogLevel = LogLevel.LIFECYCLE)
        debugProject.build("build", "-Dkotlin.gradle.test.report.memory.usage=true") {
            assertContains("Forcing System.gc()")
        }

        val infoProject = Project("simpleProject", GRADLE_VERSION, minLogLevel = LogLevel.QUIET)
        infoProject.build("clean", "build", "-Dkotlin.gradle.test.report.memory.usage=true") {
            assertNotContains("Forcing System.gc()")
        }
    }

    @Test
    fun testKotlinClasspath() {
        Project("classpathTest", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testMultiprojectPluginClasspath() {
        Project("multiprojectClassPathTest", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertReportExists("subproject")
            assertContains(":subproject:compileKotlin", ":subproject:compileTestKotlin")
            checkKotlinGradleBuildServices()
        }
    }

    @Test
    fun testIncremental() {
        val project = Project("kotlinProject", GRADLE_VERSION)
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("build", options = options) {
            assertSuccessful()
            assertNoWarnings()
        }

        val greeterKt = project.projectDir.getFileByName("Greeter.kt")
        greeterKt.modify {
            it.replace("greeting: String", "greeting: CharSequence")
        }

        project.build("build", options = options) {
            assertSuccessful()
            assertNoWarnings()
            val affectedSources = project.projectDir.getFilesByNames("Greeter.kt", "KotlinGreetingJoiner.kt",
                    "TestGreeter.kt", "TestKotlinGreetingJoiner.kt")
            assertCompiledKotlinSources(project.relativize(affectedSources), weakTesting = false)
        }
    }

    @Test
    fun testSimpleMultiprojectIncremental() {
        fun Project.modify(body: Project.() -> Unit): Project {
            this.body()
            return this
        }

        val incremental = defaultBuildOptions().copy(incremental = true)

        Project("multiprojectWithDependency", GRADLE_VERSION).build("assemble", options = incremental) {
            assertSuccessful()
            assertReportExists("projA")
            assertContains(":projA:compileKotlin")
            assertNotContains("projA:compileKotlin UP-TO-DATE")
            assertReportExists("projB")
            assertContains(":projB:compileKotlin")
            assertNotContains("projB:compileKotlin UP-TO-DATE")
        }
        Project("multiprojectWithDependency", GRADLE_VERSION).modify {
            val oldSrc = File(this.projectDir, "projA/src/main/kotlin/a.kt")
            val newSrc = File(this.projectDir, "projA/src/main/kotlin/a.kt.new")
            assertTrue { oldSrc.exists() }
            assertTrue { newSrc.exists() }
            newSrc.copyTo(oldSrc, overwrite = true)
        }.build("assemble", options = incremental) {
            assertSuccessful()
            assertReportExists("projA")
            assertContains(":projA:compileKotlin")
            assertNotContains("projA:compileKotlin UP-TO-DATE")
            assertReportExists("projB")
            assertContains(":projB:compileKotlin")
            assertNotContains("projB:compileKotlin UP-TO-DATE")
        }
    }

    @Test
    fun testKotlinInJavaRoot() {
        Project("kotlinInJavaRoot", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testIncrementalPropertyFromLocalPropertiesFile() {
        val project = Project("kotlinProject", GRADLE_VERSION)
        project.setupWorkingDir()

        val localPropertyFile = File(project.projectDir, "local.properties")
        localPropertyFile.writeText("kotlin.incremental=true")

        project.build("build") {
            assertContains(USING_INCREMENTAL_COMPILATION_MESSAGE)
        }
    }

    @Test
    fun testConvertJavaToKotlin() {
        val project = Project("convertBetweenJavaAndKotlin", GRADLE_VERSION)
        project.setupWorkingDir()

        val barKt = project.projectDir.getFileByName("Bar.kt")
        val barKtContent = barKt.readText()
        barKt.delete()

        project.build("build") {
            assertSuccessful()
        }

        val barClass = project.projectDir.getFileByName("Bar.class")
        val barClassTimestamp = barClass.lastModified()

        val barJava = project.projectDir.getFileByName("Bar.java")
        barJava.delete()
        barKt.writeText(barKtContent)

        project.build("build") {
            assertSuccessful()
            assertNotContains(":compileKotlin UP-TO-DATE", ":compileJava UP-TO-DATE")
            assertNotEquals(barClassTimestamp, barClass.lastModified(), "Bar.class timestamp hasn't been updated")
        }
    }

    @Test
    fun testWipeClassesDirectoryBetweenBuilds() {
        val project = Project("kotlinJavaProject", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
        }

        val javaOutputDir = File(project.projectDir, "build/classes")
        assert(javaOutputDir.isDirectory) { "Classes directory does not exist $javaOutputDir" }
        javaOutputDir.deleteRecursively()

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
        }
    }

    @Test
    fun testMoveClassToOtherModule() {
        val project = Project("moveClassToOtherModule", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertContains("Connected to daemon")
        }

        project.performModifications()
        project.build("build") {
            assertSuccessful()
            assertContains("Connected to daemon")
        }
    }

    @Test
    fun testTypeAliasIncremental() {
        val project = Project("typeAlias", GRADLE_VERSION)
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("build", options = options) {
            assertSuccessful()
        }

        val curryKt = project.projectDir.getFileByName("Curry.kt")
        val useCurryKt = project.projectDir.getFileByName("UseCurry.kt")

        curryKt.modify {
            it.replace("class Curry", "internal class Curry")
        }

        project.build("build", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(curryKt, useCurryKt))
        }
    }

    @Test
    fun testKotlinBuiltins() {
        val project = Project("kotlinBuiltins", "4.0")

        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testCustomCompilerFile() {
        val project = Project("customCompilerFile", GRADLE_VERSION)
        project.setupWorkingDir()

        // copy compiler embeddable to project dir using custom name
        val classpath = System.getProperty("java.class.path").split(File.pathSeparator)
        val kotlinEmbeddableJar = File(classpath.find { it.contains("kotlin-compiler-embeddable") })
        val compilerJar = File(project.projectDir, "compiler.jar")
        kotlinEmbeddableJar.copyTo(compilerJar)

        project.build("build") {
            assertSuccessful()
            assertContains("Kotlin compiler classpath: $compilerJar")
        }
    }


    @Test
    fun testMultiplatformCompile() {
        val project = Project("multiplatformProject", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertContains(":lib:compileKotlinCommon",
                    ":lib:compileTestKotlinCommon",
                    ":libJvm:compileKotlin",
                    ":libJvm:compileTestKotlin",
                    ":libJs:compileKotlin2Js",
                    ":libJs:compileTestKotlin2Js")
            assertFileExists("lib/build/classes/main/foo/PlatformClass.kotlin_metadata")
            assertFileExists("lib/build/classes/test/foo/PlatformTest.kotlin_metadata")
            assertFileExists("libJvm/build/classes/main/foo/PlatformClass.class")
            assertFileExists("libJvm/build/classes/test/foo/PlatformTest.class")
            assertFileExists("libJs/build/classes/main/libJs.js")
            assertFileExists("libJs/build/classes/test/libJs_test.js")
        }
    }

    @Test
    fun testFreeCompilerArgs() {
        val project = Project("kotlinProject", GRADLE_VERSION)
        project.setupWorkingDir()

        val customModuleName = "custom_module_name"

        File(project.projectDir, "build.gradle").modify {
            it + """
            compileKotlin {
                kotlinOptions.freeCompilerArgs = [ "-module-name", "$customModuleName" ]
            }"""
        }

        project.build("build") {
            assertSuccessful()
            assertFileExists("build/classes/main/META-INF/$customModuleName.kotlin_module")
        }
    }

    @Test
    fun testChangeDestinationDir() {
        val project = Project("kotlinProject", "3.3")
        project.setupWorkingDir()

        val fileToRemove = File(project.projectDir, "src/main/kotlin/removeMe.kt")
        fileToRemove.writeText("val x = 1")
        val classFilePath = "build/classes/main/RemoveMeKt.class"

        project.build("build") {
            assertSuccessful()
            assertFileExists(classFilePath)
        }

        // Check that after the change the build succeeds and no stale classes remain in the java classes dir
        File(project.projectDir, "build.gradle").modify {
            "$it\n\ncompileKotlin.destinationDir = file(\"\${project.buildDir}/compileKotlin\")"
        }
        fileToRemove.delete()

        project.build("build") {
            assertSuccessful()
            assertNoSuchFile(classFilePath)
            // Check that the fallback to non-incremental copying was chosen
            assertContains("Non-incremental copying files")
        }

        // Check that the classes are copied incrementally under normal conditions
        fileToRemove.writeText("val x = 1")
        project.build("build") {
            assertSuccessful()
            assertFileExists(classFilePath)
            assertNotContains("Non-incremental copying files")
        }
    }

    @Test
    fun testDowngradeTo106() {
        val project = Project("kotlinProject", GRADLE_VERSION)
        val options = defaultBuildOptions().copy(incremental = true, withDaemon = false)

        project.build("assemble", options = options) {
            assertSuccessful()
        }

        project.build("clean", "assemble", options = options.copy(kotlinVersion = "1.0.6")) {
            assertSuccessful()
        }
    }

    @Test
    fun testOmittedStdlibVersion() {
        val project = Project("kotlinProject", "2.3")
        project.setupWorkingDir()
        File(project.projectDir, "build.gradle").modify {
            it.replace("kotlin-stdlib:\$kotlin_version", "kotlin-stdlib").apply { check(!equals(it)) }
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testCleanAfterIncrementalBuild() {
        val project = Project("kotlinProject", "3.3")
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("build", "clean", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testLanguageVersionApiVersionExplicit() {
        val project = Project("kotlinProject", "3.3")
        project.setupWorkingDir()

        val buildGradle = File(project.projectDir, "build.gradle")
        val buildGradleContentCopy = buildGradle.readText()

        fun updateBuildGradle(langVersion: String, apiVersion: String) {
            buildGradle.writeText(
                    """
                $buildGradleContentCopy

                compileKotlin {
                    kotlinOptions {
                        languageVersion = '$langVersion'
                        apiVersion = '$apiVersion'
                    }
                }
            """.trimIndent())
        }

        assert(buildGradleContentCopy.indexOf("languageVersion") < 0) { "build.gradle should not contain 'languageVersion'" }
        assert(buildGradleContentCopy.indexOf("apiVersion") < 0) { "build.gradle should not contain 'apiVersion'" }

        // check the arguments are not passed by default (they are inferred by the compiler)
        project.build("clean", "compileKotlin") {
            assertSuccessful()
            assertNotContains("-language-version")
            assertNotContains("-api-version")
            assertNoWarnings()
        }

        // check the arguments are always passed if specified explicitly
        updateBuildGradle("1.0", "1.0")
        project.build("clean", "compileKotlin") {
            assertSuccessful()
            assertContains("-language-version 1.0")
            assertContains("-api-version 1.0")
        }

        updateBuildGradle("1.1", "1.1")
        project.build("clean", "compileKotlin") {
            assertSuccessful()
            assertContains("-language-version 1.1")
            assertContains("-api-version 1.1")
        }
    }

    @Test
    fun testSeparateOutputGradle40() {
        val project = Project("kotlinJavaProject", "4.0")
        project.build("compileDeployKotlin", "assemble") {
            assertSuccessful()

            // Check that the Kotlin classes are placed under directories following the guideline:
            assertFileExists("build/classes/kotlin/main/demo/KotlinGreetingJoiner.class")
            assertFileExists("build/classes/kotlin/deploy/demo/ExampleSource.class")

            // Check that the resulting JAR contains the Kotlin classes, without duplicates:
            val jar = ZipFile(fileInWorkingDir("build/libs/${project.projectName}.jar"))
            assertEquals(1, jar.entries().asSequence().count { it.name == "demo/KotlinGreetingJoiner.class" })

            // Check that the Java output is intact:
            assertFileExists("build/classes/java/main/demo/Greeter.class")

            // Check that the sync output task is not used with Gradle 4.0+ and there's no old Kotlin output layout
            assertNotContains(":copyMainKotlinClasses")
            assertNoSuchFile("build/kotlin-classes")
        }
    }

    @Test
    fun testArchiveBaseNameForModuleName() {
        val project = Project("simpleProject", "4.0")
        project.setupWorkingDir()

        val archivesBaseName = "myArchivesBaseName"

        val buildGradle = File(project.projectDir, "build.gradle")
        buildGradle.appendText("\narchivesBaseName = '$archivesBaseName'")

        // Add top-level members to force generation of the *.kotlin_module files for the two source sets
        val mainHelloWorldKt = File(project.projectDir, "src/main/kotlin/helloWorld.kt")
        mainHelloWorldKt.appendText("\nfun topLevelFun() = 1")
        val deployKotlinSrcKt = File(project.projectDir, "src/deploy/kotlin/kotlinSrc.kt")
        deployKotlinSrcKt.appendText("\nfun topLevelFun() = 1")

        project.build("build", "compileDeployKotlin") {
            assertSuccessful()
            // Main source set should have a *.kotlin_module file without '_main'
            assertFileExists("build/classes/kotlin/main/META-INF/$archivesBaseName.kotlin_module")
            assertFileExists("build/classes/kotlin/deploy/META-INF/${archivesBaseName}_deploy.kotlin_module")
        }
    }

    @Test
    fun testJavaPackagePrefix() {
        val project = Project("javaPackagePrefix", "4.0")
        project.build("build") {
            assertSuccessful()

            // Check that the Java source in a non-full-depth package structure was located correctly:
            checkBytecodeContains(
                    File(project.projectDir, "build/classes/kotlin/main/my/pack/name/app/MyApp.class"),
                    "my/pack/name/util/JUtil.util")
        }
    }

    @Test
    fun testDisableSeparateClassesDirs() {
        val separateDirPath = "build/classes/kotlin/main/demo/KotlinGreetingJoiner.class"
        val singleDirPath = "build/classes/java/main/demo/KotlinGreetingJoiner.class"

        fun CompiledProject.check(copyClassesToJavaOutput: Boolean?,
                                  expectBuildCacheWarning: Boolean,
                                  expectGradleLowVersionWarning: Boolean) {
            assertSuccessful()
            when (copyClassesToJavaOutput) {
                true -> {
                    assertNoSuchFile(separateDirPath)
                    assertFileExists(singleDirPath)
                }
                false -> {
                    assertFileExists(separateDirPath)
                    assertNoSuchFile(singleDirPath)
                }
            }

            if (expectBuildCacheWarning)
                assertContains(CopyClassesToJavaOutputStatus.buildCacheWarningMessage)
            else
                assertNotContains(CopyClassesToJavaOutputStatus.buildCacheWarningMessage)

            if (expectGradleLowVersionWarning)
                assertContains(CopyClassesToJavaOutputStatus.gradleVersionTooLowWarningMessage)
            else
                assertNotContains(CopyClassesToJavaOutputStatus.gradleVersionTooLowWarningMessage)
        }

        Project("simpleProject", "4.0").apply {
            build("build") {
                check(copyClassesToJavaOutput = false,
                        expectBuildCacheWarning = false,
                        expectGradleLowVersionWarning = false)
            }
            File(projectDir, "build.gradle").appendText("\nkotlin.copyClassesToJavaOutput = true")
            build("clean", "build") {
                check(copyClassesToJavaOutput = true,
                        expectBuildCacheWarning = false,
                        expectGradleLowVersionWarning = false)
            }
            build("clean", "build", "--build-cache") {
                check(copyClassesToJavaOutput = true,
                        expectBuildCacheWarning = true,
                        expectGradleLowVersionWarning = false)
            }
            projectDir.deleteRecursively()
        }

        Project("simpleProject", "3.4").apply {
            setupWorkingDir()
            File(projectDir, "build.gradle").appendText("\nkotlin.copyClassesToJavaOutput = true")
            build("build") {
                check(copyClassesToJavaOutput = null,
                        expectBuildCacheWarning = false,
                        expectGradleLowVersionWarning = true)
            }
        }
    }
}