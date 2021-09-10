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
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING
import org.jetbrains.kotlin.gradle.plugin.MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_WARNING
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.tasks.USING_JVM_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Test
import java.io.File
import java.io.ObjectInputStream
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipFile
import kotlin.test.*

class KotlinGradleIT : BaseGradleIT() {

    @Test
    fun testCrossCompile() {
        val project = Project("kotlinJavaProject")

        project.build("compileDeployKotlin", "build") {
            assertSuccessful()
            assertReportExists()
            assertTasksExecuted(":compileKotlin", ":compileTestKotlin", ":compileDeployKotlin")
        }

        project.build("compileDeployKotlin", "build") {
            assertSuccessful()
            assertTasksUpToDate(
                ":compileKotlin",
                ":compileTestKotlin",
                ":compileDeployKotlin",
                ":compileJava"
            )
        }
    }

    @Test
    fun testRunningInDifferentDir() {
        val wd0 = workingDir
        val wd1 = File(wd0, "subdir").apply { mkdirs() }
        workingDir = wd1
        val project1 = Project("kotlinJavaProject")

        project1.build("assemble") {
            assertSuccessful()
        }

        val wd2 = createTempDir("testRunningInDifferentDir")
        wd1.copyRecursively(wd2)
        wd1.deleteRecursively()
        if (wd1.exists()) {
            val files = buildString {
                wd1.walk().forEach { appendLine("  " + it.relativeTo(wd1).path) }
            }
            error("Some files in $wd1 were not removed:\n$files")
        }
        wd0.setWritable(false)
        workingDir = wd2

        project1.build("test") {
            assertSuccessful()
        }
    }

    @Test
    fun testKotlinOnlyCompile() {
        val project = Project("kotlinProject")

        project.build("build") {
            assertSuccessful()
            assertFileExists(kotlinClassesDir() + "META-INF/kotlinProject.kotlin_module")
            assertReportExists()
            assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
            assertNotContains("Forcing System.gc")
        }

        project.build("build") {
            assertSuccessful()
            assertTasksUpToDate(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testKotlinCompileInFolderWithSpaces() {
        val project = Project(projectName = "Project Path With Spaces")

        project.build("build") {
            assertSuccessful()
            assertReportExists()
            assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
            assertNotContains("Forcing System.gc")
        }
    }

    @Test
    fun testLogLevelForceGC() {
        val debugProject = Project("simpleProject", minLogLevel = LogLevel.LIFECYCLE)
        debugProject.build("build", "-Dkotlin.gradle.test.report.memory.usage=true") {
            assertContains("Forcing System.gc()")
        }

        val infoProject = Project("simpleProject", minLogLevel = LogLevel.QUIET)
        infoProject.build("clean", "build", "-Dkotlin.gradle.test.report.memory.usage=true") {
            assertNotContains("Forcing System.gc()")
        }
    }

    @Test
    fun testKotlinClasspath() {
        Project("classpathTest").build("build") {
            assertSuccessful()
            assertReportExists()
            assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testMultiprojectPluginClasspath() {
        Project("multiprojectClassPathTest").build("build") {
            assertSuccessful()
            assertReportExists("subproject")
            assertTasksExecuted(":subproject:compileKotlin", ":subproject:compileTestKotlin")
            checkKotlinGradleBuildServices()
        }
    }

    @Test
    fun testIncremental() {
        val project = Project("kotlinProject")
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
            val affectedSources = project.projectDir.getFilesByNames(
                "Greeter.kt", "KotlinGreetingJoiner.kt",
                "TestGreeter.kt", "TestKotlinGreetingJoiner.kt"
            )
            assertCompiledKotlinSources(project.relativize(affectedSources))
        }
    }

    @Test
    fun testIncrementalFir() {
        val project = Project("kotlinProject")
        val options = defaultBuildOptions().copy(incremental = true, useFir = true)

        project.build("build", options = options) {
            assertSuccessful()
            assertNoWarnings { removeFirWarning(it) }
        }

        val greeterKt = project.projectDir.getFileByName("Greeter.kt")
        greeterKt.modify {
            it.replace("greeting: String", "greeting: CharSequence")
        }

        project.build("build", options = options) {
            assertSuccessful()
            assertNoWarnings { removeFirWarning(it) }
            val affectedSources = project.projectDir.getFilesByNames(
                "Greeter.kt", "KotlinGreetingJoiner.kt",
                "TestGreeter.kt", "TestKotlinGreetingJoiner.kt"
            )
            assertCompiledKotlinSources(project.relativize(affectedSources))
        }
    }

    private fun removeFirWarning(output: String): String {
        return output.replace(
            """
               |w: ATTENTION!
               | This build uses in-dev FIR: 
               |  -Xuse-fir
            """.trimMargin().replace("\n", SYSTEM_LINE_SEPARATOR), ""
        )
    }

    @Test
    fun testManyClassesIC() {
        val project = Project("manyClasses")
        val options = defaultBuildOptions().copy(incremental = true)

        project.setupWorkingDir()
        val classesKt = project.projectFile("classes.kt")
        classesKt.writeText((0..1024).joinToString("\n") { "class Class$it { fun f() = $it }" })

        project.build("build", options = options) {
            assertSuccessful()
            assertNoWarnings()
        }

        val dummyKt = project.projectFile("dummy.kt")
        dummyKt.modify { "$it " }
        project.build("build", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(dummyKt))
        }
    }

    @Test
    fun testSimpleMultiprojectIncremental() {
        val incremental = defaultBuildOptions().copy(incremental = true)

        Project("multiprojectWithDependency").build("assemble", options = incremental) {
            assertSuccessful()
            assertReportExists("projA")
            assertReportExists("projB")
            assertTasksExecuted(":projA:compileKotlin", ":projB:compileKotlin")
        }
        Project("multiprojectWithDependency").apply {
            val oldSrc = File(this.projectDir, "projA/src/main/kotlin/a.kt")
            val newSrc = File(this.projectDir, "projA/src/main/kotlin/a.kt.new")
            assertTrue { oldSrc.exists() }
            assertTrue { newSrc.exists() }
            newSrc.copyTo(oldSrc, overwrite = true)
        }.build("assemble", options = incremental) {
            assertSuccessful()
            assertReportExists("projA")
            assertReportExists("projB")
            assertTasksExecuted(":projA:compileKotlin", ":projB:compileKotlin")
        }
    }

    @Test
    fun testKotlinInJavaRoot() {
        Project("kotlinInJavaRoot").build("build") {
            assertSuccessful()
            assertReportExists()
            assertTasksExecuted(":compileKotlin")
            assertContains(":compileTestKotlin NO-SOURCE")
        }
    }

    @Test
    fun testIncrementalPropertyFromLocalPropertiesFile() {
        val project = Project("kotlinProject")
        project.setupWorkingDir()

        val localPropertyFile = File(project.projectDir, "local.properties")
        localPropertyFile.writeText("kotlin.incremental=true")

        project.build("build") {
            assertContains(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
        }
    }

    @Test
    fun testIncrementalCompilationLogLevel() {
        val infoProject = Project("kotlinProject", minLogLevel = LogLevel.INFO)
        infoProject.build("build") {
            assertContains(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
        }

        val lifecycleProject = Project("kotlinProject", minLogLevel = LogLevel.LIFECYCLE)
        lifecycleProject.build("build") {
            assertNotContains(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
        }
    }

    @Test
    fun testConvertJavaToKotlin() {
        val project = Project("convertBetweenJavaAndKotlin")
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
    fun testMoveClassToOtherModule() {
        val project = Project("moveClassToOtherModule")

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
        val project = Project("typeAlias")
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
        val project = Project("kotlinBuiltins")

        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testFreeCompilerArgs() {
        val project = Project("kotlinProject")
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
            assertFileExists(kotlinClassesDir() + "META-INF/$customModuleName.kotlin_module")
        }
    }

    @Test
    fun testDowngradePluginVersion() {
        val project = Project("kotlinProject")
        val options = defaultBuildOptions().copy(incremental = true, withDaemon = false)

        project.build("assemble", options = options) {
            assertSuccessful()
        }

        project.build("clean", "assemble", options = options.copy(kotlinVersion = TestVersions.Kotlin.STABLE_RELEASE)) {
            assertSuccessful()
        }
    }

    @Test
    fun testOmittedStdlibVersion() {
        val project = Project("kotlinProject")
        project.setupWorkingDir()
        File(project.projectDir, "build.gradle").modify {
            """
            $it
            
            plugins.apply('maven-publish')
            
            group = "com.example"
            version = "1.0"

            publishing {
                publications {
                   myLibrary(MavenPublication) {
                       from components.kotlin
                   }
                }
                repositories {
                    maven {
                        url = "${'$'}buildDir/repo" 
                    }
                }
            }
            """.trimIndent()
        }

        project.build(
            "build",
            "publishAllPublicationsToMavenRepository",
            options = defaultBuildOptions().copy(warningMode = WarningMode.Summary)
        ) {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
            val pomLines = File(project.projectDir, "build/publications/myLibrary/pom-default.xml").readLines()
            val stdlibVersionLineNumber = pomLines.indexOfFirst { "<artifactId>kotlin-stdlib-jdk8</artifactId>" in it } + 1
            val versionLine = pomLines[stdlibVersionLineNumber]
            assertTrue { "<version>${defaultBuildOptions().kotlinVersion}</version>" in versionLine }
        }
    }

    @Test
    fun testCleanAfterIncrementalBuild() {
        val project = Project("kotlinProject")
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("build", "clean", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testIncrementalTestCompile() {
        val project = Project("kotlinProject")
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("build", options = options) {
            assertSuccessful()
        }

        val joinerKt = project.projectDir.getFileByName("KotlinGreetingJoiner.kt")
        joinerKt.modify {
            it.replace("class KotlinGreetingJoiner", "internal class KotlinGreetingJoiner")
        }

        project.build("build", options = options) {
            assertSuccessful()
            val testJoinerKt = project.projectDir.getFileByName("TestKotlinGreetingJoiner.kt")
            assertCompiledKotlinSources(project.relativize(joinerKt, testJoinerKt))
        }
    }

    @Test
    fun testIncrementalFirTestCompile() {
        val project = Project("kotlinProject")
        val options = defaultBuildOptions().copy(incremental = true, useFir = true)

        project.build("build", options = options) {
            assertSuccessful()
        }

        val joinerKt = project.projectDir.getFileByName("KotlinGreetingJoiner.kt")
        joinerKt.modify {
            it.replace("class KotlinGreetingJoiner", "internal class KotlinGreetingJoiner")
        }

        project.build("build", options = options) {
            assertSuccessful()
            val testJoinerKt = project.projectDir.getFileByName("TestKotlinGreetingJoiner.kt")
            assertCompiledKotlinSources(project.relativize(joinerKt, testJoinerKt))
        }
    }

    @Test
    fun testLanguageVersionApiVersionExplicit() {
        val project = Project("kotlinProject")
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
            """.trimIndent()
            )
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
        updateBuildGradle("1.4", "1.4")
        project.build("clean", "compileKotlin") {
            assertSuccessful()
            assertContains("-language-version 1.4")
            assertContains("-api-version 1.4")
        }

        updateBuildGradle("1.5", "1.5")
        project.build("clean", "compileKotlin") {
            assertSuccessful()
            assertContains("-language-version 1.5")
            assertContains("-api-version 1.5")
        }
    }

    @Test
    fun testSeparateOutputGradle40() {
        val project = Project("kotlinJavaProject")
        project.build("compileDeployKotlin", "assemble") {
            assertSuccessful()

            // Check that the Kotlin classes are placed under directories following the guideline:
            assertFileExists(kotlinClassesDir() + "demo/KotlinGreetingJoiner.class")
            assertFileExists(kotlinClassesDir(sourceSet = "deploy") + "demo/ExampleSource.class")

            // Check that the resulting JAR contains the Kotlin classes, without duplicates:
            val jar = ZipFile(fileInWorkingDir("build/libs/${project.projectName}.jar"))
            assertEquals(1, jar.entries().asSequence().count { it.name == "demo/KotlinGreetingJoiner.class" })

            // Check that the Java output is intact:
            assertFileExists("build/classes/java/main/demo/Greeter.class")
        }
    }

    @Test
    fun testArchiveBaseNameForModuleName() {
        val project = Project("simpleProject")
        project.setupWorkingDir()

        val archivesBaseName = "myArchivesBaseName"

        val buildGradle = File(project.projectDir, "build.gradle")
        buildGradle.appendText("\narchivesBaseName = '$archivesBaseName'")

        // Add top-level members to force generation of the *.kotlin_module files for the two source sets
        val mainHelloWorldKt = File(project.projectDir, "src/main/kotlin/helloWorld.kt")
        mainHelloWorldKt.appendText("\nfun topLevelFun() = 1")
        val deployKotlinSrcKt = File(project.projectDir, "src/deploy/kotlin/kotlinSrc.kt")
        deployKotlinSrcKt.appendText("\nfun topLevelFun() = 1")

        project.build("build", "deployClasses") {
            assertSuccessful()
            // Main source set should have a *.kotlin_module file without '_main'
            assertFileExists(kotlinClassesDir() + "META-INF/$archivesBaseName.kotlin_module")
            assertFileExists(kotlinClassesDir(sourceSet = "deploy") + "META-INF/${archivesBaseName}_deploy.kotlin_module")
        }
    }

    @Test
    fun testJavaPackagePrefix() {
        val project = Project("javaPackagePrefix")
        project.build("build") {
            assertSuccessful()

            // Check that the Java source in a non-full-depth package structure was located correctly:
            checkBytecodeContains(
                File(project.projectDir, kotlinClassesDir() + "my/pack/name/app/MyApp.class"),
                "my/pack/name/util/JUtil.util"
            )
        }
    }

    @Test
    fun testSrcDirTaskDependency() {
        Project("simpleProject").apply {
            setupWorkingDir()
            File(projectDir, "build.gradle").appendText(
                """${'\n'}
                task generateSources {
                    outputs.dir('generated')
                    doLast {
                        def file = new File('generated/test/TestClass.java')
                        file.parentFile.mkdirs()
                        file.text = ""${'"'}
                            package test;

                            public class TestClass { }
                        ""${'"'}
                    }
                }
                sourceSets.main.java.srcDir(tasks.generateSources)
                """.trimIndent()
            )
            File(projectDir, "src/main/kotlin/helloWorld.kt").appendText(
                """${'\n'}
                fun usageOfGeneratedSource() = test.TestClass()
                """.trimIndent()
            )

            build("build") {
                assertSuccessful()
            }
        }
    }

    @Test
    fun testSourceJar() {
        Project("simpleProject").apply {
            setupWorkingDir()
            val additionalSrcDir = "src/additional/kotlin/"

            File(projectDir, additionalSrcDir).mkdirs()
            File(projectDir, "$additionalSrcDir/additionalSource.kt").writeText("fun hello() = 123")

            File(projectDir, "build.gradle").appendText(
                """${'\n'}
                task sourcesJar(type: Jar) {
                    from sourceSets.main.allSource
                    classifier 'source'
                    duplicatesStrategy = 'fail' // fail in case of Java source duplication, see KT-17564
                }

                sourceSets.main.kotlin.srcDir('$additionalSrcDir') // test that additional srcDir is included
                """.trimIndent()
            )

            build("sourcesJar") {
                assertSuccessful()
                val sourcesJar = ZipFile(File(projectDir, "build/libs/simpleProject-source.jar"))
                assertNotNull(sourcesJar.getEntry("additionalSource.kt"))
            }
        }
    }

    @Test
    fun testNoUnnamedInputsOutputs() {

        val projects = listOf(
            Project("simpleProject"),
            Project("kotlin2JsProject"),
            Project("multiplatformProject"),
            Project("simple", directoryPrefix = "kapt2")
        )

        projects.forEach {
            it.apply {
                // Enable caching to make sure Gradle reports the task inputs/outputs during key construction:
                val options = defaultBuildOptions().copy(withBuildCache = true)
                build("assemble", options = options) {
                    // Check that all inputs/outputs added at runtime have proper names
                    // (the unnamed ones are listed as $1, $2 etc.):
                    assertNotContains("Appending inputPropertyHash for '\\$\\d+'".toRegex())
                    assertNotContains("Appending outputPropertyName to build cache key: \\$\\d+".toRegex())
                }
            }
        }
    }

    @Test
    fun testModuleNameFiltering() = with(Project("typeAlias")) { // Use a Project with a top-level typealias
        setupWorkingDir()

        gradleBuildScript().appendText("\n" + """archivesBaseName = 'a/really\\tricky\n\rmodule\tname'""")
        build("classes") {
            assertSuccessful()

            val metaInfDir = File(projectDir, kotlinClassesDir() + "META-INF")
            assertNotNull(metaInfDir.listFiles().singleOrNull { it.name.endsWith(".kotlin_module") })
        }
    }

    @Test
    fun testJavaIcCompatibility() {
        val project = Project("kotlinJavaProject")
        project.setupWorkingDir()

        val buildScript = File(project.projectDir, "build.gradle")

        buildScript.modify { "$it\n" + "compileJava.options.incremental = true" }
        project.build("build") {
            assertSuccessful()
        }

        // Then modify a Java source and check that compileJava is incremental:
        File(project.projectDir, "src/main/java/demo/HelloWorld.java").modify { "$it\n" + "class NewClass { }" }
        project.build("build") {
            assertSuccessful()
            assertContains("Incremental compilation")
            assertNotContains("not incremental")
        }

        // Then modify a Kotlin source and check that Gradle sees that Java is not up-to-date:
        File(project.projectDir, "src/main/kotlin/helloWorld.kt").modify {
            it.trim('\r', '\n').trimEnd('}') + "\nval z: Int = 0 }"
        }
        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertNotContains("not incremental")
            assertNotContains("None of the classes needs to be compiled!")
        }
    }

    @Test
    fun testApplyPluginFromBuildSrc() {
        val project = Project("kotlinProjectWithBuildSrc")
        project.setupWorkingDir()
        File(project.projectDir, "buildSrc/build.gradle").modify { it.replace("\$kotlin_version", KOTLIN_VERSION) }
        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testKotlinPluginDependenciesInBuildSrc() {
        val project = transformProjectWithPluginsDsl("kotlinPluginDepsInBuildSrc")
        project.setupWorkingDir()
        project.build("build") {
            assertSuccessful()
            assertContains("Hi from BuildSrc")
        }

    }

    @Test
    fun testInternalTest() = with(
        Project("internalTest")
    ) {
        build("build") {
            assertSuccessful()
            assertReportExists()
            assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
        }

        // Check KT-35341: use symlinked build dir
        val buildDir = projectDir.resolve("build")
        buildDir.deleteRecursively()
        val externalBuildDir = Files.createTempDirectory(workingDir.toPath(), "externalBuild")
        try {
            Files.createSymbolicLink(buildDir.toPath(), externalBuildDir)
        } catch (_: FileSystemException) {
            //Windows requires SeSymbolicLink privilege and we can't grant it
            null
        } ?: return@with

        build("build") {
            assertSuccessful()
            assertReportExists()
            assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testJavaLibraryCompatibility() {
        val project = Project("javaLibraryProject")

        val compileKotlinTasks = listOf(":libA:compileKotlin", ":libB:compileKotlin", ":app:compileKotlin")
        project.build("build") {
            assertSuccessful()
            assertNotContains("Could not register Kotlin output")
            assertTasksExecuted(compileKotlinTasks)
        }

        // Modify a library source and its usage and re-build the project:
        for (path in listOf("libA/src/main/kotlin/HelloA.kt", "libB/src/main/kotlin/HelloB.kt", "app/src/main/kotlin/App.kt")) {
            File(project.projectDir, path).modify { original ->
                original.replace("helloA", "helloA1")
                    .replace("helloB", "helloB1")
                    .apply { assert(!equals(original)) }
            }
        }

        project.build("build") {
            assertSuccessful()
            assertNotContains("Could not register Kotlin output")
            assertTasksExecuted(compileKotlinTasks)
        }
    }

    @Test
    fun testKotlinSourceInJavaSourceSet() = with(Project("multiplatformProject")) {
        setupWorkingDir()

        val srcDirPrefix = "srcDir: "

        gradleBuildScript().appendText(
            "\n" + """
            subprojects { project ->
                project.afterEvaluate {
                    project.sourceSets.each { sourceSet ->
                        sourceSet.allJava.srcDirs.each { srcDir ->
                            println "$srcDirPrefix" + srcDir.canonicalPath
                        }
                    }
                }
            }
            """.trimIndent()
        )
        val srcDirRegex = "$srcDirPrefix(.*)".toRegex()

        build("help") {
            assertSuccessful()
            val reportedSrcDirs = srcDirRegex.findAll(output).map { it.groupValues[1] }.toSet()

            val expectedKotlinDirs = listOf("lib", "libJvm", "libJs").flatMap { module ->
                listOf("main", "test").map { sourceSet ->
                    projectDir.resolve("$module/src/$sourceSet/kotlin").absolutePath
                }
            }

            expectedKotlinDirs.forEach { assertTrue(it in reportedSrcDirs, "$it should be included into the Java source sets") }
        }
    }

    @Test
    fun testDefaultKotlinVersionIsNotAffectedByTransitiveDependencies() =
        with(Project("simpleProject")) {
            setupWorkingDir()
            // Add a dependency with an explicit lower Kotlin version that has a kotlin-stdlib transitive dependency:
            gradleBuildScript().appendText("\ndependencies { implementation 'org.jetbrains.kotlin:kotlin-reflect:1.2.71' }")
            testResolveAllConfigurations {
                assertSuccessful()
                assertContains(">> :compileClasspath --> kotlin-reflect-1.2.71.jar")
                // Check that the default newer Kotlin version still wins for 'kotlin-stdlib':
                assertContains(">> :compileClasspath --> kotlin-stdlib-${defaultBuildOptions().kotlinVersion}.jar")
            }
        }

    @Test
    fun testKotlinJvmProjectPublishesKotlinApiDependenciesAsCompile() =
        with(Project("simpleProject")) {
            setupWorkingDir()
            gradleBuildScript().appendText(
                "\n" + """
                dependencies {
                    api 'org.jetbrains.kotlin:kotlin-reflect'
                }
                apply plugin: 'maven-publish'
                group "com.example"
                version "1.0"
                publishing {
                    repositories { maven { url file("${'$'}buildDir/repo").toURI() } }
                    publications { maven(MavenPublication) { from components.java } }
                }
                """.trimIndent()
            )
            build("publish") {
                assertSuccessful()
                val pomText = projectDir.resolve("build/repo/com/example/simpleProject/1.0/simpleProject-1.0.pom").readText()
                    .replace("\\s+|\\n".toRegex(), "")
                assertTrue {
                    pomText.contains(
                        "<groupId>org.jetbrains.kotlin</groupId>" +
                                "<artifactId>kotlin-reflect</artifactId>" +
                                "<version>${defaultBuildOptions().kotlinVersion}</version>" +
                                "<scope>compile</scope>"
                    )
                }
            }
        }

    @Test
    fun testNoTaskConfigurationForcing() {
        val projects = listOf(
            Project("simpleProject"),
            Project("kotlin2JsNoOutputFileProject"),
            Project("sample-app", GradleVersionRequired.FOR_MPP_SUPPORT, "new-mpp-lib-and-app")
        )

        projects.forEach {
            it.apply {
                setupWorkingDir()

                val taskConfigureFlag = "Configured the task!"

                gradleBuildScript().appendText("\n" + """
                    tasks.register("myTask") { println '$taskConfigureFlag' }
                """.trimIndent())

                build("help") {
                    assertSuccessful()
                    assertNotContains(taskConfigureFlag)
                }
            }
        }
    }

    @Test
    fun testBuildReportSmokeTest() = with(Project("simpleProject")) {
        build("assemble", "-Pkotlin.build.report.enable=true") {
            assertSuccessful()
            assertContains("Kotlin build report is written to")
        }

        build("clean", "assemble", "-Pkotlin.build.report.enable=true") {
            assertSuccessful()
            assertContains("Kotlin build report is written to")
        }
    }

    @Test
    fun testBuildMetricsSmokeTest() = with(Project("simpleProject")) {
        build("assemble", options = defaultBuildOptions().copy(withReports = listOf(BuildReportType.FILE))) {
            assertSuccessful()
            assertContains("Kotlin build report is written to")
        }
        val reportFolder = projectDir.resolve("build/reports/kotlin-build")
        val reports = reportFolder.listFiles()
        assertNotNull(reports)
        assertEquals(1, reports.size)
        val report = reports[0].readText()

        //Should contains build metrics for all compile kotlin tasks
        assertTrue { report.contains("Time metrics:") }
        assertTrue { report.contains("Run compilation:") }
        assertTrue { report.contains("Incremental compilation in daemon:") }
        assertTrue { report.contains("Build performance metrics:") }
        assertTrue { report.contains("Total size of the cache directory:") }
        assertTrue { report.contains("Total compiler iteration:") }
        assertTrue { report.contains("ABI snapshot size:") }
        //for non-incremental builds
        assertTrue { report.contains("Build attributes:") }
        assertTrue { report.contains("REBUILD_REASON:") }
    }

    @Test
    fun testCompilerBuildMetricsSmokeTest() = with(Project("simpleProject")) {
        build("assemble", options = defaultBuildOptions().copy(withReports = listOf(BuildReportType.FILE))) {
            assertSuccessful()
            assertContains("Kotlin build report is written to")
        }
        val reportFolder = projectDir.resolve("build/reports/kotlin-build")
        val reports = reportFolder.listFiles()
        assertNotNull(reports)
        assertEquals(1, reports.size)
        val report = reports[0].readText()
        assertTrue { report.contains("Compiler code analysis:") }
        assertTrue { report.contains("Compiler code generation:") }
        assertTrue { report.contains("Compiler initialization time:") }
    }


    @Test
    fun testKt29971() = with(Project("kt-29971", GradleVersionRequired.FOR_MPP_SUPPORT)) {
        build("jvm-app:build") {
            assertSuccessful()
            assertTasksExecuted(":jvm-app:compileKotlin")
        }
    }

    @Test
    fun testDetectingDifferentClassLoaders() = with(Project("kt-27059-pom-rewriting", GradleVersionRequired.FOR_MPP_SUPPORT)) {
        setupWorkingDir()

        val originalRootBuildScript = gradleBuildScript().readText()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        build("publish", "-PmppProjectDependency=true") {
            assertSuccessful()
            assertNotContains(MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING)
            assertNotContains(MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_WARNING)
        }

        // Specify the plugin versions in the subprojects with different plugin sets – this will make Gradle use separate class loaders
        gradleBuildScript().modify {
            originalRootBuildScript.checkedReplace("id \"org.jetbrains.kotlin.multiplatform\"", "//")
        }
        gradleBuildScript("mpp-lib").modify {
            it.checkedReplace(
                "id \"org.jetbrains.kotlin.multiplatform\"",
                "id \"org.jetbrains.kotlin.multiplatform\" version \"<pluginMarkerVersion>\""
            ).let(::transformBuildScriptWithPluginsDsl)
        }
        gradleBuildScript("jvm-app").modify {
            it.checkedReplace(
                "id \"org.jetbrains.kotlin.jvm\"",
                "id \"org.jetbrains.kotlin.jvm\" version \"<pluginMarkerVersion>\""
            ).let(::transformBuildScriptWithPluginsDsl)
        }
        gradleBuildScript("js-app").modify {
            it.checkedReplace(
                "id \"kotlin2js\"",
                "id \"kotlin2js\" version \"<pluginMarkerVersion>\""
            ).let(::transformBuildScriptWithPluginsDsl)
        }

        // Also include another project via a composite build:
        transformProjectWithPluginsDsl("allopenPluginsDsl", directoryPrefix = "pluginsDsl").let { other ->
            val result = other.projectName
            other.setupWorkingDir()
            other.projectDir.copyRecursively(projectDir.resolve(result))
            gradleSettingsScript().appendText("\nincludeBuild(\"${result}\")")
            gradleBuildScript().appendText(
                "\ntasks.create(\"publish\").dependsOn(gradle.includedBuild(\"${result}\").task(\":assemble\"))"
            )
            result
        }

        build("publish", "-PmppProjectDependency=true") {
            assertSuccessful()
            assertContains(MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING)

            val specificProjectsReported = Regex("$MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_WARNING((?:'.*'(?:, )?)+)")
                .find(output)!!.groupValues[1].split(", ").map { it.removeSurrounding("'") }.toSet()

            assertEquals(setOf(":mpp-lib", ":jvm-app", ":js-app"), specificProjectsReported)
        }

        // Test the flag that turns off the warnings
        build("publish", "-PmppProjectDependency=true", "-Pkotlin.pluginLoadedInMultipleProjects.ignore=true") {
            assertSuccessful()
            assertNotContains(MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING)
            assertNotContains(MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_WARNING)
        }
    }

    @Test
    fun testNewModelInOldJvmPlugin() = with(Project("new-model-in-old-plugin")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        build("publish", "check", "runBenchmark", options = defaultBuildOptions().copy(warningMode = WarningMode.Summary)) {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileTestKotlin", ":compileBenchmarkKotlin", ":test", ":runBenchmark")

            // Find the benchmark output:
            assertContains("f ran at the speed of light")

            val moduleDir = "build/repo/com/example/new-model/1.0/"

            val publishedJar = fileInWorkingDir(moduleDir + "new-model-1.0.jar")
            ZipFile(publishedJar).use { zip ->
                val entries = zip.entries().asSequence().map { it.name }
                assertTrue { "com/example/A.class" in entries }
            }

            val publishedPom = fileInWorkingDir(moduleDir + "new-model-1.0.pom")
            val kotlinVersion = defaultBuildOptions().kotlinVersion
            val pomText = publishedPom.readText().replace(Regex("\\s+"), "")
            assertTrue { "kotlin-gradle-plugin-api</artifactId><version>$kotlinVersion</version><scope>compile</scope>" in pomText }
            assertTrue { "kotlin-stdlib-jdk8</artifactId><version>$kotlinVersion</version><scope>runtime</scope>" in pomText }

            assertFileExists(moduleDir + "new-model-1.0-sources.jar")
        }
    }

    @Test
    fun testUserDefinedAttributesInSinglePlatformProject() =
        with(Project("multiprojectWithDependency")) {
            setupWorkingDir()
            gradleBuildScript("projA").appendText(
                "\n" + """
                def targetAttribute = Attribute.of("com.example.target", String)
                def compilationAttribute = Attribute.of("com.example.compilation", String)
                kotlin.target.attributes.attribute(targetAttribute, "foo")
                kotlin.target.compilations["main"].attributes.attribute(compilationAttribute, "foo")
                """.trimIndent()
            )
            gradleBuildScript("projB").appendText(
                "\n" + """
                def targetAttribute = Attribute.of("com.example.target", String)
                def compilationAttribute = Attribute.of("com.example.compilation", String)
                kotlin.target.attributes.attribute(targetAttribute, "foo")
                kotlin.target.compilations["main"].attributes.attribute(compilationAttribute, "foo")
                """.trimIndent()
            )
            build(":projB:compileKotlin") {
                assertSuccessful()
            }

            val projectGradleVersion = GradleVersion.version(chooseWrapperVersionOrFinishTest())
            // Break dependency resolution by providing incompatible custom attributes in the target:
            gradleBuildScript("projB").appendText("\nkotlin.target.attributes.attribute(targetAttribute, \"bar\")")
            build(":projB:compileKotlin") {
                assertFailed()
                when {
                    projectGradleVersion < GradleVersion.version("6.4") -> {
                        assertContains("Required com.example.target 'bar'")
                    }
                    projectGradleVersion < GradleVersion.version("6.8.4") -> {
                        assertContains(
                            "No matching variant of project :projA was found. The consumer was configured to find an API of a library " +
                                "compatible with Java 8, preferably in the form of class files, " +
                                "and its dependencies declared externally, " +
                                "as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'jvm', " +
                                "attribute 'com.example.compilation' with value 'foo', " +
                                "attribute 'com.example.target' with value 'bar' but:"
                        )
                    }
                    else -> {
                        assertContains(
                            "No matching variant of project :projA was found. The consumer was configured to find an API of a library " +
                                "compatible with Java 8, preferably in the form of class files, " +
                                "preferably optimized for standard JVMs, and its dependencies declared externally, " +
                                "as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'jvm', " +
                                "attribute 'com.example.compilation' with value 'foo', " +
                                "attribute 'com.example.target' with value 'bar' but:"
                        )
                    }
                }
            }

            // And using the compilation attributes (fix the target attributes first):
            gradleBuildScript("projB").appendText(
                "\n" + """
                kotlin.target.attributes.attribute(targetAttribute, "foo")
                kotlin.target.compilations["main"].attributes.attribute(compilationAttribute, "bar")
                """.trimIndent()
            )
            build(":projB:compileKotlin") {
                assertFailed()
                when {
                    projectGradleVersion < GradleVersion.version("6.4") -> {
                        assertContains("Required com.example.compilation 'bar'")
                    }
                    projectGradleVersion < GradleVersion.version("6.8.4") -> {
                        assertContains(
                            "No matching variant of project :projA was found. The consumer was configured to find an API of a library " +
                                "compatible with Java 8, preferably in the form of class files, and its dependencies declared externally, " +
                                "as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'jvm', " +
                                "attribute 'com.example.compilation' with value 'bar', " +
                                "attribute 'com.example.target' with value 'foo' but:"
                        )
                    }
                    else -> {
                        assertContains(
                            "No matching variant of project :projA was found. The consumer was configured to find an API of a library " +
                                "compatible with Java 8, preferably in the form of class files, preferably optimized for standard JVMs, " +
                                "and its dependencies declared externally, " +
                                "as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'jvm', " +
                                "attribute 'com.example.compilation' with value 'bar', " +
                                "attribute 'com.example.target' with value 'foo' but:"
                        )
                    }
                }
            }
        }

    @Test
    fun testLoadCompilerEmbeddableAfterOtherKotlinArtifacts() = with(Project("simpleProjectClasspath")) {
        setupWorkingDir()
        val buildscriptClasspathPrefix = "buildscript-classpath = "
        gradleBuildScript()
            .appendText(
                """
               
                println "$buildscriptClasspathPrefix" + Arrays.toString(buildscript.classLoader.getURLs())
                """.trimIndent()
            )

        // get the classpath, then reorder it so that kotlin-compiler-embeddable is loaded after all other JARs
        lateinit var classpath: List<String>

        build {
            val classpathLine = output.lines().single { buildscriptClasspathPrefix in it }
            classpath = classpathLine.substringAfter(buildscriptClasspathPrefix).removeSurrounding("[", "]").split(", ")
        }

        gradleBuildScript().modify {
            val reorderedClasspath = run {
                val (kotlinCompilerEmbeddable, others) = classpath.partition { "kotlin-compiler-embeddable" in it }
                others + kotlinCompilerEmbeddable
            }
            val newClasspathString = "classpath files(\n" + reorderedClasspath.joinToString(",\n") { "'$it'" } + "\n)"
            it.checkedReplace("classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version\"", newClasspathString)
        }

        build("compileKotlin") {
            assertSuccessful()
        }
    }

    @Test
    fun testNoScriptingWarning() = with(Project("simpleProject")) {
        // KT-31124
        build {
            assertNotContains(ScriptingGradleSubplugin.MISCONFIGURATION_MESSAGE_SUFFIX)
        }
    }

    @Test
    fun testKtKt35942InternalsFromMainInTestViaTransitiveDeps() = with(Project("kt-35942-jvm", GradleVersionRequired.FOR_MPP_SUPPORT)) {
        build(":lib1:compileTestKotlin") {
            assertSuccessful()
            assertTasksExecuted(":lib1:compileKotlin", ":lib2:jar")
        }
    }

    @Test
    fun testKtKt35942InternalsFromMainInTestViaTransitiveDepsAndroid() = with(
        Project(
            projectName = "kt-35942-android",
            gradleVersionRequirement = GradleVersionRequired.AtLeast("6.7.1")
        )
    ) {
        build(
            ":lib1:compileDebugUnitTestKotlin",
            options = defaultBuildOptions().copy(
                androidGradlePluginVersion = AGPVersion.v4_2_0,
                androidHome = KtTestUtil.findAndroidSdk(),
            ),
        ) {
            assertSuccessful()
            assertTasksExecuted(":lib1:compileDebugKotlin")
        }
    }

    /** Regression test for KT-38692. */
    @Test
    fun testIncrementalWhenNoKotlinSources() = with(
        Project("kotlinProject")
    ) {
        setupWorkingDir()
        assertTrue(this.allKotlinFiles.toList().isNotEmpty())
        build(":compileKotlin") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin")
        }

        // Remove all Kotlin sources and force non-incremental run
        allKotlinFiles.forEach { assertTrue(it.delete()) }
        projectDir.resolve("src/main/java/Sample.java").also {
            it.parentFile.mkdirs()
            it.writeText("public class Sample {}")
        }
        build("compileKotlin", "--rerun-tasks") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin")
            val compiledKotlinClasses = fileInWorkingDir(classesDir()).allFilesWithExtension("class").toList()

            assertTrue(compiledKotlinClasses.isEmpty())
        }
    }

    /** Regression test for KT-45787. **/
    @Test
    fun testPluginDoesNotUseDeprecatedConfigurationsForAssociatedDependencies() {
        with(
            Project(
                projectName = "associatedDependencies",
                minLogLevel = LogLevel.INFO
            )
        ) {
            setupWorkingDir()

            build(
                "tasks",
                options = defaultBuildOptions().copy(warningMode = WarningMode.Fail)
            ) {
                assertSuccessful()
            }
        }
    }

    @Test
    fun testEarlyConfigurationsResolutionKotlin() = testEarlyConfigurationsResolution("kotlinProject", kts = false)

    @Test
    fun testEarlyConfigurationsResolutionKotlinJs() = testEarlyConfigurationsResolution("kotlin-js-browser-project", kts = true)

    private fun testEarlyConfigurationsResolution(projectName: String, kts: Boolean) = with(Project(projectName = projectName)) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        //language=Gradle
        gradleBuildScript().appendText(
            """${'\n'}
            // KT-45834 start
            ${if (kts) "var" else "def"} ready = false
            gradle.taskGraph.whenReady {
                println("Task Graph Ready")
                ready = true
            }
            
            allprojects {
                configurations.forEach { configuration ->
                    configuration.incoming.beforeResolve {
                        println("Resolving ${'$'}configuration")
                        if (!ready) {
                            throw ${if (kts) "" else "new"} GradleException("${'$'}configuration is being resolved at configuration time")
                        }
                    }
                }
            }
            // KT-45834 end
            """.trimIndent()
        )

        build(
            "assemble",
            options = defaultBuildOptions().copy(dryRun = true)
        ) {
            assertSuccessful()
        }
    }
}
