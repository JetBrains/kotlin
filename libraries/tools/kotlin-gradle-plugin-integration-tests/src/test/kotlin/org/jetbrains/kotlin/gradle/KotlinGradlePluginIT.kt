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
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.USING_JVM_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.testResolveAllConfigurations
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlin.test.*

@DisplayName("Basic Kotlin/JVM plugin tests")
@JvmGradlePluginTests
class KotlinGradleIT : KGPBaseTest() {

    @DisplayName("Kotlin/Java cross compilation")
    @GradleTest
    fun testCrossCompile(gradleVersion: GradleVersion) {
        project("kotlinJavaProject", gradleVersion) {
            build("compileDeployKotlin", "build") {
                assertTasksExecuted(
                    ":compileKotlin",
                    ":compileTestKotlin",
                    ":compileDeployKotlin"
                )
            }

            build("compileDeployKotlin", "build") {
                assertTasksUpToDate(
                    ":compileKotlin",
                    ":compileTestKotlin",
                    ":compileDeployKotlin",
                    ":compileJava"
                )
            }
        }
    }

    @DisplayName("Kotlin only project compilation")
    @GradleTest
    fun testKotlinOnlyCompile(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            build("build") {
                assertFileExists(kotlinClassesDir().resolve("META-INF/kotlinProject.kotlin_module"))
                assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
                assertOutputDoesNotContain("Forcing System.gc")
            }

            build("build") {
                assertTasksUpToDate(
                    ":compileKotlin",
                    ":compileTestKotlin"
                )
            }
        }
    }

    @DisplayName("Project path contains spaces")
    @GradleTest
    fun testKotlinCompileInFolderWithSpaces(gradleVersion: GradleVersion) {
        project(projectName = "Project Path With Spaces", gradleVersion) {
            build("build") {
                assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
                assertOutputDoesNotContain("Forcing System.gc")
            }
        }
    }

    @DisplayName("Logs contain memory usage entries on LIFECYCLE log level")
    @GradleTest
    fun testLogLevelForceGC(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build(
                "assemble",
                "-Dkotlin.gradle.test.report.memory.usage=true",
                buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.LIFECYCLE)
            ) {
                assertOutputContains("Forcing System.gc()")
            }
            build(
                "clean",
                "assemble",
                "-Dkotlin.gradle.test.report.memory.usage=true",
                buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.QUIET)
            ) {
                assertOutputDoesNotContain("Forcing System.gc()")
            }
        }
    }

    @DisplayName("Plugin is avialble when applied via buildscript classpath")
    @GradleTest
    fun testMultiprojectPluginClasspath(gradleVersion: GradleVersion) {
        project("multiprojectClassPathTest", gradleVersion) {
            build("build") {
                assertTasksExecuted(
                    ":subproject:compileKotlin",
                    ":subproject:compileTestKotlin"
                )
            }
        }
    }

    @DisplayName("Incremental logs are available on INFO log level")
    @GradleTest
    fun testIncrementalCompilationLogLevel(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            build(
                "assemble",
                buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.INFO)
            ) {
                assertOutputContains(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
            }

            build(
                "assemble",
                buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.LIFECYCLE)
            ) {
                assertOutputDoesNotContain(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
            }
        }
    }

    @DisplayName("Conversion between Kotlin and Java")
    @GradleTest
    fun testConvertJavaToKotlin(gradleVersion: GradleVersion) {
        project("convertBetweenJavaAndKotlin", gradleVersion) {
            val barKt = javaSourcesDir().resolve("foo/Bar.kt")
            val barKtContent = barKt.readText()
            barKt.deleteIfExists()

            build("build")

            val barClass = kotlinClassesDir().resolve("foo/Bar.class").toFile()
            val barClassTimestamp = barClass.lastModified()

            val barJava = javaSourcesDir().resolve("foo/Bar.java")
            barJava.deleteIfExists()
            barKt.writeText(barKtContent)

            build("build") {
                assertTasksExecuted(":compileKotlin", ":compileJava")
                assertNotEquals(
                    barClassTimestamp,
                    barClass.lastModified(),
                    "Bar.class timestamp hasn't been updated"
                )
            }
        }
    }

    @DisplayName("Moving class to another Gradle subproject")
    @GradleTest
    fun testMoveClassToOtherModule(gradleVersion: GradleVersion) {
        project("moveClassToOtherModule", gradleVersion) {
            build("assemble")

            with(subProject("lib").javaSourcesDir()) {
                resolve("bar/A.kt.new").moveTo(resolve("bar/A.kt"))
            }
            with(subProject("app").javaSourcesDir()) {
                resolve("foo/A.kt").deleteIfExists()
                resolve("foo/useA.kt.new")
                    .moveTo(resolve("foo/useA.kt"), overwrite = true)
            }

            build("assemble")
        }
    }

    @DisplayName("Adding free compiler arguments")
    @GradleTest
    fun testFreeCompilerArgs(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            val customModuleName = "custom_module_name"

            buildGradle.appendText(
                //language=Groovy
                """
                
                compileKotlin {
                    kotlinOptions.freeCompilerArgs = [ "-module-name", "$customModuleName" ]
                }
                """.trimIndent()
            )

            build("build") {
                assertFileExists(kotlinClassesDir().resolve("META-INF/$customModuleName.kotlin_module"))
            }
        }
    }

    @DisplayName("KT-52239: Changing Kotlin options via deprecated 'dsl.KotlinJvmOptions' interface")
    @GradleTest
    fun testKotlinOptionsViaDeprecatedKotlinJvmOptionsDsl(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            val customModuleName = "custom_module_name"

            buildGradle.appendText(
                //language=Groovy
                """
                
                tasks.withType(org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile.class).configureEach {
                    kotlinOptions.moduleName = "$customModuleName"
                }
                """.trimIndent()
            )

            build("assemble") {
                assertFileExists(kotlinClassesDir().resolve("META-INF/$customModuleName.kotlin_module"))
            }
        }
    }

    @DisplayName("Downgrading Kotlin plugin version")
    @GradleTest
    fun testDowngradePluginVersion(gradleVersion: GradleVersion) {
        project(
            "kotlinProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(incremental = true)
        ) {
            build("assemble")
            val buildOptions = buildOptions.copy(
                kotlinVersion = TestVersions.Kotlin.STABLE_RELEASE
            )
            build(
                "clean",
                "assemble",
                buildOptions = buildOptions
            )
        }
    }

    @DisplayName("Passing api/language version")
    @GradleTest
    fun testLanguageVersionApiVersionExplicit(gradleVersion: GradleVersion) {
        project(
            "kotlinProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            val buildGradleContentCopy = buildGradle.readText()

            fun updateBuildGradle(langVersion: String, apiVersion: String) {
                buildGradle.writeText(
                    //language=Groovy
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
            build("clean", "compileKotlin") {
                assertOutputDoesNotContain("-language-version")
                assertOutputDoesNotContain("-api-version")
                assertNoBuildWarnings()
            }

            // check the arguments are always passed if specified explicitly
            updateBuildGradle("1.6", "1.6")
            build("clean", "compileKotlin") {
                assertOutputContains("-language-version 1.6")
                assertOutputContains("-api-version 1.6")
            }

            updateBuildGradle("1.7", "1.7")
            build("clean", "compileKotlin") {
                assertOutputContains("-language-version 1.7")
                assertOutputContains("-api-version 1.7")
            }
        }
    }

    @DisplayName("Mixed project with additional sourceset produce outputs in correct dirs")
    @GradleTest
    fun testSeparateOutputGradle40(gradleVersion: GradleVersion) {
        project("kotlinJavaProject", gradleVersion) {
            build("compileDeployKotlin", "assemble") {
                // Check that the Kotlin classes are placed under directories following the guideline:
                assertFileExists(kotlinClassesDir().resolve("demo/KotlinGreetingJoiner.class"))
                assertFileExists(kotlinClassesDir(sourceSet = "deploy").resolve("demo/ExampleSource.class"))

                // Check that the resulting JAR contains the Kotlin classes, without duplicates:
                ZipFile(projectPath.resolve("build/libs/$projectName.jar").toFile()).use { jar ->
                    assertEquals(
                        1,
                        jar.entries().asSequence().count { it.name == "demo/KotlinGreetingJoiner.class" }
                    )
                }


                // Check that the Java output is intact:
                assertFileInProjectExists("build/classes/java/main/demo/Greeter.class")
            }
        }
    }

    @DisplayName("archivesBaseName is used for module name")
    @GradleTest
    fun testArchiveBaseNameForModuleName(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val archivesBaseName = "myArchivesBaseName"
            buildGradle.appendText("\narchivesBaseName = '$archivesBaseName'")

            // Add top-level members to force generation of the *.kotlin_module files for the two source sets
            val mainHelloWorldKt = kotlinSourcesDir().resolve("helloWorld.kt")
            mainHelloWorldKt.appendText("\nfun topLevelFun() = 1")
            val deployKotlinSrcKt = kotlinSourcesDir(sourceSet = "deploy").resolve("kotlinSrc.kt")
            deployKotlinSrcKt.appendText("\nfun topLevelFun() = 1")

            build("build", "deployClasses") {
                // Main source set should have a *.kotlin_module file without '_main'
                assertFileExists(kotlinClassesDir().resolve("META-INF/$archivesBaseName.kotlin_module"))
                assertFileExists(kotlinClassesDir(sourceSet = "deploy").resolve("META-INF/${archivesBaseName}_deploy.kotlin_module"))
            }
        }
    }

    @DisplayName("'javaPackagePrefix' change is applied")
    @GradleTest
    fun testJavaPackagePrefix(gradleVersion: GradleVersion) {
        project("javaPackagePrefix", gradleVersion) {
            build("build") {
                // Check that the Java source in a non-full-depth package structure was located correctly:
                checkBytecodeContains(
                    kotlinClassesDir().resolve("my/pack/name/app/MyApp.class").toFile(),
                    "my/pack/name/util/JUtil.util"
                )
            }
        }
    }

    @DisplayName("Should add generated sources from task dependency")
    @Disabled("Not working as expected")
    @GradleTest
    fun testSrcDirTaskDependency(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion
        ) {
            buildGradle.appendText(
                """
                
                def generateTask = tasks.register('generateSources') {
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
                sourceSets.main.java.srcDir(generateTask)
                """.trimIndent()
            )
            kotlinSourcesDir().resolve("helloWorld.kt").appendText(
                """
                
                fun usageOfGeneratedSource() = test.TestClass()
                """.trimIndent()
            )

            build("build")
        }
    }

    @DisplayName("Sources jar include Kotlin files")
    @GradleTest
    fun testSourceJar(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val additionalSrcDir = "src/additional/kotlin/"

            with(projectPath.resolve(additionalSrcDir)) {
                createDirectories()
                resolve("additionalSource.kt").writeText("fun hello() = 123")
            }

            buildGradle.appendText(
                """
                
                tasks.register("sourcesJar", Jar) {
                    from sourceSets.main.allSource
                    archiveClassifier = 'source'
                    duplicatesStrategy = 'fail' // fail in case of Java source duplication, see KT-17564
                }

                sourceSets.main.kotlin.srcDir('$additionalSrcDir') // test that additional srcDir is included
                """.trimIndent()
            )

            build("sourcesJar") {
                ZipFile(projectPath.resolve("build/libs/simpleProject-source.jar").toFile()).use {
                    assertNotNull(it.getEntry("additionalSource.kt"))
                }
            }
        }
    }

    @DisplayName("Handling special characters in Kotlin module name")
    @GradleTest
    fun testModuleNameFiltering(gradleVersion: GradleVersion) {
        project("typeAlias", gradleVersion) { // Use a Project with a top-level typealias
            buildGradle.appendText(
                """
                                    
                archivesBaseName = 'a/really\\trick\n\rmodule\tname'
                
                tasks.withType(Jar.class).configureEach {
                    archiveBaseName.set('typeAlias')
                }
                """.trimIndent()
            )

            build("classes") {
                val metaInfDir = kotlinClassesDir().resolve("META-INF").toFile()
                assertNotNull(
                    metaInfDir.listFiles()?.singleOrNull {
                        it.name.endsWith(".kotlin_module")
                    }
                )
            }
        }
    }

    @DisplayName("Plugin from buildSrc dependencies is available")
    @GradleTest
    fun testApplyPluginFromBuildSrc(gradleVersion: GradleVersion) {
        project("kotlinProjectWithBuildSrc", gradleVersion) {
            settingsGradle.writeText(
                //language=Groovy
                """
                pluginManagement {
                    repositories {
                        mavenLocal()
                        gradlePluginPortal()
                    }
                    
                    plugins {
                        id "org.jetbrains.kotlin.test.fixes.android" version "${'$'}test_fixes_version"
                    }
                }
                """.trimIndent()
            )
            build("build")
        }
    }

    @DisplayName("KGP dependencies in buildSrc module")
    @GradleTest
    fun testKotlinPluginDependenciesInBuildSrc(gradleVersion: GradleVersion) {
        project("kotlinPluginDepsInBuildSrc", gradleVersion) {
            build("build") {
                assertOutputContains("Hi from BuildSrc")
            }
        }
    }

    @DisplayName("Test sources should be able to access internal methods or properties")
    @GradleTest
    fun testInternalTest(gradleVersion: GradleVersion) {
        project("internalTest", gradleVersion) {
            build("build") {
                assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
            }
        }
    }

    @DisplayName("KT-35341: symlinked build dir")
    @DisabledOnOs(OS.WINDOWS, disabledReason = "Windows requires SeSymbolicLink privilege and we can't grant it")
    @GradleTest
    fun symlinkedBuildDir(
        gradleVersion: GradleVersion,
        @TempDir tempDir: Path,
    ) {
        project("internalTest", gradleVersion) {
            val externalBuildDir = tempDir.resolve("externalBuild")
            externalBuildDir.createSymbolicLinkPointingTo(projectPath.resolve("build"))

            build("build") {
                assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
            }
        }
    }

    @DisplayName("'java-library' plugin compatibility")
    @GradleTest
    fun testJavaLibraryCompatibility(gradleVersion: GradleVersion) {
        project("javaLibraryProject", gradleVersion) {
            val compileKotlinTasks = arrayOf(":libA:compileKotlin", ":libB:compileKotlin", ":app:compileKotlin")
            build("build") {
                assertTasksExecuted(*compileKotlinTasks)
                assertOutputDoesNotContain("Could not register Kotlin output")
            }

            // Modify a library source and its usage and re-build the project:
            listOf(
                subProject("libA").kotlinSourcesDir().resolve("HelloA.kt"),
                subProject("libB").kotlinSourcesDir().resolve("HelloB.kt"),
                subProject("app").kotlinSourcesDir().resolve("App.kt")
            ).forEach { sourceFile ->
                sourceFile.modify {
                    it.replace("helloA", "helloA1")
                        .replace("helloB", "helloB1")
                }
            }

            build("build") {
                assertOutputDoesNotContain("Could not register Kotlin output")
                assertTasksExecuted(*compileKotlinTasks)
            }
        }
    }

    @DisplayName("Default Kotlin version is not affected by transitive dependencies")
    @GradleTest
    fun testDefaultKotlinVersionIsNotAffectedByTransitiveDependencies(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            // Add a dependency with an explicit lower Kotlin version that has a kotlin-stdlib transitive dependency:
            buildGradle.appendText("\ndependencies { implementation 'org.jetbrains.kotlin:kotlin-reflect:1.5.32' }")
            testResolveAllConfigurations { unresolvedConfigurations, buildResult ->
                assertTrue("Unresolved configurations: $unresolvedConfigurations") { unresolvedConfigurations.isEmpty() }
                buildResult.assertOutputContains(">> :compileClasspath --> kotlin-reflect-1.5.32.jar")
                // Check that the default newer Kotlin version still wins for 'kotlin-stdlib':
                buildResult.assertOutputContains(
                    ">> :compileClasspath --> kotlin-stdlib-${buildOptions.kotlinVersion}.jar"
                )
            }
        }
    }

    @DisplayName("KT-29971: concurrent modification exception on task execution")
    @GradleTest
    fun concurrentModificationException(gradleVersion: GradleVersion) {
        project("kt-29971", gradleVersion) {
            build("jvm-app:build") {
                assertTasksExecuted(":jvm-app:compileKotlin")
            }
        }
    }

    @DisplayName("New extensions models are working as expected")
    @GradleTest
    fun testNewModelInOldJvmPlugin(gradleVersion: GradleVersion) {
        project(
            "new-model-in-old-plugin",
            gradleVersion
        ) {
            build(
                "publish",
                "check",
                "runBenchmark"
            ) {
                assertTasksExecuted(
                    ":compileKotlin",
                    ":compileTestKotlin",
                    ":compileBenchmarkKotlin",
                    ":test",
                    ":runBenchmark"
                )

                // Find the benchmark output:
                assertOutputContains("f ran at the speed of light")

                val moduleDir = projectPath.resolve("build/repo/com/example/new-model/1.0/")

                val publishedJar = moduleDir.resolve("new-model-1.0.jar")
                ZipFile(publishedJar.toFile()).use { zip ->
                    val entries = zip.entries().asSequence().map { it.name }
                    assertTrue { "com/example/A.class" in entries }
                }

                val publishedPom = moduleDir.resolve("new-model-1.0.pom")
                val kotlinVersion = buildOptions.kotlinVersion
                val pomText = publishedPom.readText().replace(Regex("\\s+"), "")
                assertTrue { "kotlin-gradle-plugin-api</artifactId><version>$kotlinVersion</version><scope>compile</scope>" in pomText }
                assertTrue { "kotlin-stdlib-jdk8</artifactId><version>$kotlinVersion</version><scope>runtime</scope>" in pomText }

                assertFileExists(moduleDir.resolve("new-model-1.0-sources.jar"))
            }
        }
    }

    @DisplayName("User-defined attributes")
    @GradleTest
    fun testUserDefinedAttributesInSinglePlatformProject(gradleVersion: GradleVersion) {
        project("multiprojectWithDependency", gradleVersion) {
            subProject("projA").buildGradle.appendText(
                """
                
                def targetAttribute = Attribute.of("com.example.target", String)
                def compilationAttribute = Attribute.of("com.example.compilation", String)
                kotlin.target.attributes.attribute(targetAttribute, "foo")
                kotlin.target.compilations["main"].attributes.attribute(compilationAttribute, "foo")
                """.trimIndent()
            )
            subProject("projB").buildGradle.appendText(
                """
                
                def targetAttribute = Attribute.of("com.example.target", String)
                def compilationAttribute = Attribute.of("com.example.compilation", String)
                kotlin.target.attributes.attribute(targetAttribute, "foo")
                kotlin.target.compilations["main"].attributes.attribute(compilationAttribute, "foo")
                """.trimIndent()
            )

            build(":projB:compileKotlin")

            // Break dependency resolution by providing incompatible custom attributes in the target:
            subProject("projB").buildGradle.appendText("\nkotlin.target.attributes.attribute(targetAttribute, \"bar\")")
            buildAndFail(":projB:compileKotlin") {
                when {
                    gradleVersion < GradleVersion.version("6.8.4") -> {
                        assertOutputContains(
                            "No matching variant of project :projA was found. The consumer was configured to find an API of a library " +
                                    "compatible with Java 8, preferably in the form of class files, " +
                                    "and its dependencies declared externally, " +
                                    "as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'jvm', " +
                                    "attribute 'com.example.compilation' with value 'foo', " +
                                    "attribute 'com.example.target' with value 'bar' but:"
                        )
                    }
                    else -> {
                        assertOutputContains(
                            "No matching variant of project :projA was found. The consumer was configured to find a library for use during compile-time, " +
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
            subProject("projB").buildGradle.appendText(
                """
                
                kotlin.target.attributes.attribute(targetAttribute, "foo")
                kotlin.target.compilations["main"].attributes.attribute(compilationAttribute, "bar")
                """.trimIndent()
            )
            buildAndFail(":projB:compileKotlin") {
                when {
                    gradleVersion < GradleVersion.version("6.8.4") -> {
                        assertOutputContains(
                            "No matching variant of project :projA was found. The consumer was configured to find an API of a library " +
                                    "compatible with Java 8, preferably in the form of class files, and its dependencies declared externally, " +
                                    "as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'jvm', " +
                                    "attribute 'com.example.compilation' with value 'bar', " +
                                    "attribute 'com.example.target' with value 'foo' but:"
                        )
                    }
                    else -> {
                        assertOutputContains(
                            "No matching variant of project :projA was found. The consumer was configured to find a library for use during compile-time, " +
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
    }

    @DisplayName("Load compiler-embeddable after other plugin artifacts")
    @GradleTest
    fun testLoadCompilerEmbeddableAfterOtherKotlinArtifacts(gradleVersion: GradleVersion) {
        project("simpleProjectClasspath", gradleVersion) {
            val buildscriptClasspathPrefix = "buildscript-classpath = "
            buildGradle.appendText(
                """
                
                println "$buildscriptClasspathPrefix" + Arrays.toString(buildscript.classLoader.getURLs())
                """.trimIndent()
            )

            // get the classpath, then reorder it so that kotlin-compiler-embeddable is loaded after all other JARs
            lateinit var classpath: List<String>

            build("help") {
                val classpathLine = output.lines().single { buildscriptClasspathPrefix in it }
                classpath = classpathLine
                    .substringAfter(buildscriptClasspathPrefix)
                    .removeSurrounding("[", "]")
                    .split(", ")
            }

            buildGradle.modify {
                val reorderedClasspath = run {
                    val (kotlinCompilerEmbeddable, others) = classpath.partition {
                        "kotlin-compiler-embeddable" in it ||
                                // build-common should be loaded prior compiler-embedable, otherwise we could depend on old version of
                                // serializer classes and fail with NSME
                                "kotlin-build-common" in it
                    }
                    others + kotlinCompilerEmbeddable
                }
                val newClasspathString = "classpath files(\n" + reorderedClasspath.joinToString(",\n") { "'$it'" } + "\n)"
                it.checkedReplace("classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version\"", newClasspathString)
            }

            build("compileKotlin")
        }
    }

    /** Regression test for KT-45787. **/
    @DisplayName("KT-45787: no Gradle deprecation on adding associated dependencies")
    @GradleTest
    fun testNoDeprecationOnAssociatedDep(gradleVersion: GradleVersion) {
        project(
            "associatedDependencies",
            gradleVersion
        ) {
            build("tasks")
        }
    }

    @DisplayName("KT-51913: KGP should not add attributes to 'legacy' configurations")
    @GradleTest
    fun noAttributesLegacyConfigurations(gradleVersion: GradleVersion) {
        project(
            "legacyConfigurationConsumer",
            gradleVersion
        ) {
            build(":consumer:aggregate")
        }
    }

    @DisplayName("KT-61273: task output backup works correctly if the first output is absent")
    @GradleTest
    fun taskOutputBackupWorksIfFirstOutputIsAbsent(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            buildGradle.append(
                //language=Gradle
                """
                tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configure {
                    Provider<Directory> dir = project.layout.buildDirectory.dir(".a") // name it that's way so it will be the first output in an ordered set
                    outputs.dir(dir)
                    doFirst {
                        dir.get().getAsFile().delete()
                    }
                }
                """.trimIndent()
            )

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")
            }

            kotlinSourcesDir().resolve("Dummy.kt").append(
                """
                fun foo() {}
                """.trimIndent()
            )


            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")
            }
        }
    }
}
