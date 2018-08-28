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
import org.jetbrains.kotlin.gradle.plugin.CopyClassesToJavaOutputStatus
import org.jetbrains.kotlin.gradle.tasks.USING_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        assert(!wd1.exists())
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
            assertContains(USING_INCREMENTAL_COMPILATION_MESSAGE)
        }
    }

    @Test
    fun testIncrementalCompilationLogLevel() {
        val infoProject = Project("kotlinProject", minLogLevel = LogLevel.INFO)
        infoProject.build("build") {
            assertContains(USING_INCREMENTAL_COMPILATION_MESSAGE)
        }

        val lifecycleProject = Project("kotlinProject", minLogLevel = LogLevel.LIFECYCLE)
        lifecycleProject.build("build") {
            assertNotContains(USING_INCREMENTAL_COMPILATION_MESSAGE)
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
    fun testWipeClassesDirectoryBetweenBuilds() {
        val project = Project("kotlinJavaProject", GradleVersionRequired.Exact("3.5"))

        project.build("build") {
            assertSuccessful()
        }

        val javaOutputDir = File(project.projectDir, "build/classes")
        assert(javaOutputDir.isDirectory) { "Classes directory does not exist $javaOutputDir" }
        javaOutputDir.deleteRecursively()

        project.build("build") {
            assertSuccessful()
            assertTasksUpToDate(":compileKotlin")
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
        val project = Project("kotlinBuiltins", GradleVersionRequired.AtLeast("4.0"))

        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testCustomCompilerFile() {
        val project = Project("customCompilerFile")
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
    fun testChangeDestinationDir() {
        val project = Project("kotlinProject", GradleVersionRequired.Exact("3.5"))
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
        val project = Project("kotlinProject")
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
        val project = Project("kotlinProject", GradleVersionRequired.AtLeast("4.4"))
        project.setupWorkingDir()
        File(project.projectDir, "build.gradle").modify {
            it.replace("kotlin-stdlib:\$kotlin_version", "kotlin-stdlib").apply { check(!equals(it)) } + "\n" + """
            apply plugin: 'maven'
            install.repositories { maven { url "file://${'$'}buildDir/repo" } }
            """.trimIndent()
        }

        project.build("build", "install") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
            val pomLines = File(project.projectDir, "build/poms/pom-default.xml").readLines()
            val stdlibVersionLineNumber = pomLines.indexOfFirst { "<artifactId>kotlin-stdlib</artifactId>" in it } + 1
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
        val project = Project("kotlinJavaProject", GradleVersionRequired.AtLeast("4.0"))
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

            // Check that the sync output task is not used with Gradle 4.0+ and there's no old Kotlin output layout
            assertNotContains(":copyMainKotlinClasses")
            assertNoSuchFile("build/kotlin-classes")
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
    fun testDisableSeparateClassesDirs() {

        fun CompiledProject.check(
            copyClassesToJavaOutput: Boolean?,
            expectBuildCacheWarning: Boolean,
            expectGradleLowVersionWarning: Boolean
        ) {

            val separateDirPath = kotlinClassesDir() + "demo/KotlinGreetingJoiner.class"
            val singleDirPath = javaClassesDir() + "demo/KotlinGreetingJoiner.class"

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

        Project("simpleProject", GradleVersionRequired.Exact("4.0")).apply {
            build("build") {
                check(
                    copyClassesToJavaOutput = false,
                    expectBuildCacheWarning = false,
                    expectGradleLowVersionWarning = false
                )
            }
            File(projectDir, "build.gradle").appendText("\nkotlin.copyClassesToJavaOutput = true")
            build("clean", "build") {
                check(
                    copyClassesToJavaOutput = true,
                    expectBuildCacheWarning = false,
                    expectGradleLowVersionWarning = false
                )
            }
            build("clean", "build", options = defaultBuildOptions().copy(withBuildCache = true)) {
                check(
                    copyClassesToJavaOutput = true,
                    expectBuildCacheWarning = true,
                    expectGradleLowVersionWarning = false
                )
            }
            projectDir.deleteRecursively()
        }

        Project("simpleProject", GradleVersionRequired.Exact("3.4")).apply {
            setupWorkingDir()
            File(projectDir, "build.gradle").appendText("\nkotlin.copyClassesToJavaOutput = true")
            build("build") {
                check(
                    copyClassesToJavaOutput = null,
                    expectBuildCacheWarning = false,
                    expectGradleLowVersionWarning = true
                )
            }
        }
    }

    @Test
    fun testSrcDirTaskDependency() {
        Project("simpleProject", GradleVersionRequired.AtLeast("4.1")).apply {
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
        // Use a new Gradle version to enable the usage of the input/output builders, which are new API:
        val gradleVersionRequirement = GradleVersionRequired.AtLeast("4.4")

        val projects = listOf(
            Project("simpleProject", gradleVersionRequirement),
            Project("kotlin2JsProject", gradleVersionRequirement),
            Project("multiplatformProject", gradleVersionRequirement),
            Project("simple", gradleVersionRequirement, "kapt2")
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
    fun testInternalTest() {
        Project("internalTest").build("build") {
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
}
