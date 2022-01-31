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

import org.gradle.api.JavaVersion
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.USING_JVM_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.addBeforeSubstring
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.testResolveAllConfigurations
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import java.nio.file.Files
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.deleteExisting
import kotlin.io.path.outputStream
import kotlin.test.assertEquals

@OtherGradlePluginTests
abstract class Kapt3BaseIT : KGPBaseTest() {
    companion object {
        private const val KAPT_SUCCESSFUL_MESSAGE = "Annotation processing complete, errors: 0"
    }

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions
        .copy(
            kaptOptions = this.kaptOptions(),
            warningMode = WarningMode.Summary
        )

    protected open fun kaptOptions(): BuildOptions.KaptOptions = BuildOptions.KaptOptions(
        verbose = true,
        useWorkers = false
    )

    fun BuildResult.assertKaptSuccessful() {
        val kaptSuccessfulMessagesCount = output
            .lineSequence()
            .filter { it.contains(KAPT_SUCCESSFUL_MESSAGE) }
            .count()
        assert(kaptSuccessfulMessagesCount > 0) {
            printBuildOutput()
            "Kapt hasn't done any processing"
        }
    }

    protected val String.withPrefix get() = "kapt2/$this"
}

@DisplayName("Kapt executing via workers")
open class Kapt3WorkersIT : Kapt3IT() {
    override fun kaptOptions(): BuildOptions.KaptOptions =
        super.kaptOptions().copy(useWorkers = true)

    @DisplayName("Kapt is skipped when no annotation processors are added")
    @GradleTest
    fun testKaptSkipped(gradleVersion: GradleVersion) {
        project("kaptSkipped".withPrefix, gradleVersion) {
            build("build") {
                assertTasksSkipped(":kaptGenerateStubsKotlin", ":kaptKotlin")
            }
        }
    }

    @DisplayName("Kapt is working with newer JDKs")
    @JdkVersions(versions = [JavaVersion.VERSION_1_10, JavaVersion.VERSION_11, JavaVersion.VERSION_16])
    @GradleWithJdkTest
    fun doTestSimpleWithCustomJdk(
        gradleVersion: GradleVersion,
        jdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "simple".withPrefix,
            gradleVersion,
            buildJdk = jdk.location
        ) {
            build("assemble") {
                assertTasksExecuted(":kaptGenerateStubsKotlin", ":kaptKotlin")
                // Check added because of https://youtrack.jetbrains.com/issue/KT-33056.
                assertOutputDoesNotContain("javaslang.match.PatternsProcessor")
            }
        }
    }

    @DisplayName("Additional Kapt jvm arguments are passed to the process")
    @GradleTest
    internal fun additionalJvmArgumentsArePassed(gradleVersion: GradleVersion) {
        project("simple".withPrefix, gradleVersion) {
            gradleProperties.append(
                """
                
                kapt.workers.isolation = process
                """.trimIndent()
            )

            buildGradle.append(
                //language=Groovy
                """
                
                tasks
                    .withType(org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask.class)
                    .configureEach {
                        it.kaptProcessJvmArgs.addAll(['-Xmx64m', '-Duser.country=DE'])
                    }
                """.trimIndent()
            )

            build("assemble") {
                assertOutputContains("Starting process 'Gradle Worker Daemon.*-Xmx64m.*-Duser.country=DE.*".toRegex())
            }
        }
    }

    @DisplayName("Warning is produced on additional Kapt jvm arguments and 'none' workers isolation mode")
    @GradleTest
    internal fun warningOnNoneIsolationModeAndAdditionalJvmArguments(gradleVersion: GradleVersion) {
        project("simple".withPrefix, gradleVersion) {
            gradleProperties.append(
                """
                
                kapt.workers.isolation = none
                """.trimIndent()
            )

            buildGradle.append(
                //language=Groovy
                """
                
                tasks
                    .withType(org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask.class)
                    .configureEach {
                        it.kaptProcessJvmArgs.addAll(['-Xmx64m', '-Duser.country=DE'])
                    }
                """.trimIndent()
            )

            build("assemble") {
                assertOutputContains("Kapt additional JVM arguments are ignored in 'NONE' workers isolation mode")
            }
        }
    }
}

@DisplayName("Kapt with classloaders cache executing via workers ")
class Kapt3ClassLoadersCacheIT : Kapt3WorkersIT() {
    override fun kaptOptions(): BuildOptions.KaptOptions = super.kaptOptions().copy(
        classLoadersCacheSize = 10,
        includeCompileClasspath = false
    )

    @Disabled("classloaders cache is incompatible with AP discovery in classpath")
    override fun testDisableDiscoveryInCompileClasspath(gradleVersion: GradleVersion) {
    }

    @Disabled("classloaders cache is leaking file descriptors that prevents cleaning test project")
    override fun testChangesInLocalAnnotationProcessor(gradleVersion: GradleVersion) {
    }

    @Disabled("classloaders cache is leaking file descriptors that prevents cleaning test project")
    override fun testKt19179andKt37241(gradleVersion: GradleVersion) {
    }

    @Disabled("classloaders cache is leaking file descriptors that prevents cleaning test project")
    override fun testChangesToKaptConfigurationDoNotTriggerStubGeneration(gradleVersion: GradleVersion) {
    }

    override fun testAnnotationProcessorAsFqName(gradleVersion: GradleVersion) {
        project("annotationProcessorAsFqName".withPrefix, gradleVersion) {
            //classloaders caching is not compatible with includeCompileClasspath
            buildGradle.modify {
                it.addBeforeSubstring(
                    "kapt \"org.jetbrains.kotlin:annotation-processor-example:\$kotlin_version\"\n",
                    "implementation \"org.jetbrains.kotlin:annotation-processor-example"
                )
            }

            build("build") {
                assertKaptSuccessful()
                assertTasksExecuted(":compileKotlin", ":compileJava")
                assertFileInProjectExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
                assertFileExists(kotlinClassesDir().resolve("example/TestClass.class"))
                assertFileExists(javaClassesDir().resolve("example/TestClassGenerated.class"))
            }
        }
    }
}

@DisplayName("Kapt without workers")
open class Kapt3IT : Kapt3BaseIT() {

    @DisplayName("Should find annotation processor via FQName")
    @GradleTest
    open fun testAnnotationProcessorAsFqName(gradleVersion: GradleVersion) {
        project(
            "annotationProcessorAsFqName".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                kaptOptions = kaptOptions().copy(includeCompileClasspath = true)
            )
        ) {
            build("build", forceOutput = true) {
                assertTasksExecuted(":kaptGenerateStubsKotlin", ":kaptKotlin", ":compileKotlin", ":compileJava")
                assertKaptSuccessful()
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/TestClassGenerated.java"))
                assertFileExists(kotlinClassesDir().resolve("example/TestClass.class"))
                assertFileExists(javaClassesDir().resolve("example/TestClassGenerated.class"))
            }
        }
    }

    @DisplayName("Kapt tasks is up-to-date on the second run")
    @GradleTest
    fun testSimple(gradleVersion: GradleVersion) {
        project("simple".withPrefix, gradleVersion) {

            build("build") {
                assertKaptSuccessful()
                assertTasksExecuted(":kaptGenerateStubsKotlin", ":kaptKotlin", ":compileKotlin", ":compileJava")
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/TestClassGenerated.java"))
                assertFileExists(kotlinClassesDir().resolve("example/TestClass.class"))
                assertFileExists(javaClassesDir().resolve("example/TestClassGenerated.class"))
                assertFileExists(javaClassesDir().resolve("example/SourceAnnotatedTestClassGenerated.class"))
                assertFileExists(javaClassesDir().resolve("example/BinaryAnnotatedTestClassGenerated.class"))
                assertFileExists(javaClassesDir().resolve("example/RuntimeAnnotatedTestClassGenerated.class"))
                assertFileNotExistsInTree("build/classes", "ExampleSourceAnnotation.class")
                assertOutputDoesNotContain("warning: The following options were not recognized by any processor")
                assertOutputContains("Need to discovery annotation processors in the AP classpath")
            }

            build("build") {
                assertTasksUpToDate(":kaptGenerateStubsKotlin", ":kaptKotlin", ":compileKotlin", ":compileJava")
            }
        }
    }

    @DisplayName("Kapt is working with incremental compilation")
    @GradleTest
    fun testSimpleWithIC(gradleVersion: GradleVersion) {
        project(
            "simple".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(incremental = true)
        ) {
            build("clean", "build") {
                assertTasksExecuted(":kaptGenerateStubsKotlin", ":kaptKotlin", ":compileKotlin", ":compileJava")
                assertKaptSuccessful()
                assertFileNotExistsInTree(javaClassesDir(), "ExampleSourceAnnotation.class")
            }

            javaSourcesDir().resolve("test.kt").append(" ")
            javaSourcesDir().resolve("foo/InternalDummy.kt").append(" ")
            build("build") {
                assertTasksExecuted(":kaptGenerateStubsKotlin", ":compileKotlin")
                // there are no actual changes in Java sources, generated sources, Kotlin classes
                assertTasksUpToDate(":kaptKotlin", ":compileJava")
                assertFileNotExistsInTree(javaClassesDir(), "ExampleSourceAnnotation.class")
            }

            // emulating wipe by android plugin's IncrementalSafeguardTask
            javaClassesDir().toFile().deleteRecursively()
            build("build") {
                assertTasksUpToDate(":kaptGenerateStubsKotlin", ":kaptKotlin", ":compileKotlin")
                assertFileExists(kotlinClassesDir().resolve("example/TestClass.class"))
                assertFileNotExistsInTree(javaClassesDir(), "ExampleSourceAnnotation.class")
            }
        }
    }

    @DisplayName("Disabled incremental compilation should disable it also for generate stubs task")
    @GradleTest
    fun testDisableIcForGenerateStubs(gradleVersion: GradleVersion) {
        project(
            "simple".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(incremental = false)
        ) {
            build("build") {
                assertTasksExecuted(":kaptGenerateStubsKotlin")
                assertOutputDoesNotContain(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
            }
        }
    }

    @DisplayName("Works with inherited annotations")
    @GradleTest
    fun testInheritedAnnotations(gradleVersion: GradleVersion) {
        project("inheritedAnnotations".withPrefix, gradleVersion) {
            build("build") {
                assertKaptSuccessful()
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/TestClassGenerated.java"))
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/AncestorClassGenerated.java"))
                assertFileExists(javaClassesDir().resolve("example/TestClassGenerated.class"))
                assertFileExists(javaClassesDir().resolve("example/AncestorClassGenerated.class"))
            }
        }
    }

    @DisplayName("passes arguments from kapt configuration")
    @GradleTest
    fun testArguments(gradleVersion: GradleVersion) {
        project("arguments".withPrefix, gradleVersion) {
            build("build") {
                assertKaptSuccessful()
                assertOutputContains(
                    "AP options: {suffix=Customized, justColon=:, justEquals==, containsColon=a:b, " +
                            "containsEquals=a=b, startsWithColon=:a, startsWithEquals==a, endsWithColon=a:, " +
                            "endsWithEquals=a:, withSpace=a b c,"
                )
                assertOutputContains("-Xmaxerrs=500, -Xlint:all=-Xlint:all") // Javac options test
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/TestClassCustomized.java"))
                assertFileExists(kotlinClassesDir().resolve("example/TestClass.class"))
                assertFileExists(javaClassesDir().resolve("example/TestClassCustomized.class"))
                assertOutputContains("Annotation processor class names are set, skip AP discovery")
            }
        }
    }

    @DisplayName("generated directory is up-to-date on binary annotation remove")
    @GradleTest
    fun testGeneratedDirectoryIsUpToDate(gradleVersion: GradleVersion) {
        project("generatedDirUpToDate".withPrefix, gradleVersion) {

            build("build") {
                assertTasksExecuted(":kaptGenerateStubsKotlin", ":kaptKotlin", ":compileKotlin", ":compileJava")
                assertKaptSuccessful()
                assertFileExists(kotlinClassesDir().resolve("example/TestClass.class"))

                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/TestClassGenerated.java"))
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/SourceAnnotatedTestClassGenerated.java"))
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/BinaryAnnotatedTestClassGenerated.java"))
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/RuntimeAnnotatedTestClassGenerated.java"))

                assertFileExists(javaClassesDir().resolve("example/TestClassGenerated.class"))
                assertFileExists(javaClassesDir().resolve("example/SourceAnnotatedTestClassGenerated.class"))
                assertFileExists(javaClassesDir().resolve("example/BinaryAnnotatedTestClassGenerated.class"))
                assertFileExists(javaClassesDir().resolve("example/RuntimeAnnotatedTestClassGenerated.class"))
            }

            javaSourcesDir().resolve("test.kt").modify {
                it.replace("@ExampleBinaryAnnotation", "")
            }

            build("build") {
                assertTasksExecuted(":kaptGenerateStubsKotlin", ":kaptKotlin", ":compileKotlin")
                assertTasksUpToDate(":compileJava")
                assertFileExists(kotlinClassesDir().resolve("example/TestClass.class"))

                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/TestClassGenerated.java"))
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/SourceAnnotatedTestClassGenerated.java"))
                assertFileInProjectNotExists("build/generated/source/kapt/main/example/BinaryAnnotatedTestClassGenerated.java")
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/RuntimeAnnotatedTestClassGenerated.java"))

                assertFileExists(javaClassesDir().resolve("example/TestClassGenerated.class"))
                assertFileExists(javaClassesDir().resolve("example/SourceAnnotatedTestClassGenerated.class"))
                assertFileNotExists(javaClassesDir().resolve("example/BinaryAnnotatedTestClassGenerated.class"))
                assertFileExists(javaClassesDir().resolve("example/RuntimeAnnotatedTestClassGenerated.class"))
            }
        }
    }

    @DisplayName("Should incrementally rebuild on java class deletion")
    @GradleTest
    fun testRemoveJavaClassICRebuild(gradleVersion: GradleVersion) {
        testICRebuild(gradleVersion) { project ->
            project.javaSourcesDir().resolve("foo/Foo.java").deleteExisting()
        }
    }

    @DisplayName("Should incrementally rebuild on classpath change")
    @GradleTest
    fun testChangeClasspathICRebuild(gradleVersion: GradleVersion) {
        testICRebuild(gradleVersion) { project ->
            project.buildGradle.modify {
                "$it\ndependencies { implementation 'org.jetbrains.kotlin:kotlin-reflect:' + kotlin_version }"
            }
        }
    }

    // tests all output directories are cleared when IC rebuilds
    private fun testICRebuild(
        gradleVersion: GradleVersion,
        performChange: (TestProject) -> Unit
    ) {
        project(
            "incrementalRebuild".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(incremental = true)
        ) {
            val generatedSrc = "build/generated/source/kapt/main"

            build("build") {
                // generated sources
                assertFileExists(projectPath.resolve("$generatedSrc/bar/UseBar_MembersInjector.java"))
            }

            performChange(this)

            javaSourcesDir().resolve("bar/UseBar.kt").modify { it.replace("package bar", "package foo.bar") }

            build("build") {
                assertTasksExecuted(":kaptGenerateStubsKotlin", ":kaptKotlin", ":compileKotlin", ":compileJava")

                // generated sources
                assertFileExists(projectPath.resolve("$generatedSrc/foo/bar/UseBar_MembersInjector.java"))
                assertFileInProjectNotExists("$generatedSrc/bar/UseBar_MembersInjector.java")

                // classes
                assertFileExists(kotlinClassesDir().resolve("foo/bar/UseBar.class"))
                assertFileNotExists(kotlinClassesDir().resolve("bar/UseBar.class"))
                assertFileExists(javaClassesDir().resolve("foo/bar/UseBar_MembersInjector.class"))
                assertFileNotExists(javaClassesDir().resolve("bar/UseBar_MembersInjector.class"))
            }
        }
    }

    @DisplayName("Should run processing incrementally on annotation removal")
    @GradleTest
    fun testRemoveAnnotationIC(gradleVersion: GradleVersion) {
        project(
            "simple".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(incremental = true)
        ) {
            val internalDummyKt = javaSourcesDir().resolve("foo/InternalDummy.kt")

            // add annotation
            val exampleAnn = "@example.ExampleAnnotation "
            internalDummyKt.modify { it.addBeforeSubstring(exampleAnn, "internal class InternalDummy") }

            build("classes") {
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/foo/InternalDummyGenerated.java"))
            }

            // remove annotation
            internalDummyKt.modify { it.replace(exampleAnn, "") }

            build("classes", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                val allMainKotlinSrc = relativeToProject(javaSourcesDir().allKotlinSources).toSet()
                assertCompiledKotlinSources(allMainKotlinSrc, output)
                assertFileInProjectNotExists("build/generated/source/kapt/main/foo/InternalDummyGenerated.java")
            }
        }
    }

    @DisplayName("KT18799: generate annotation value for constant values in documented types")
    @GradleTest
    fun testKt18799(gradleVersion: GradleVersion) {
        project("kt18799".withPrefix, gradleVersion) {
            build("kaptKotlin")

            subProject("app")
                .javaSourcesDir()
                .resolve("com.b.A.kt")
                .modify {
                    val line = "@Factory(factoryClass = CLASS_NAME, something = arrayOf(Test()))"
                    assert(line in it)
                    it.replace(line, "@Factory(factoryClass = CLASS_NAME)")
                }

            build("kaptKotlin")
        }
    }

    @DisplayName("compile arguments are properly copied from compileKotlin to kaptTask")
    @GradleTest
    fun testCopyCompileArguments(gradleVersion: GradleVersion) {
        project(
            "simple".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            val arg = "-Xsuppress-version-warnings"
            buildGradle.modify {
                //language=Gradle
                """
                $it
                $SYSTEM_LINE_SEPARATOR
                compileKotlin { kotlinOptions.freeCompilerArgs = ['$arg'] }
                """.trimIndent()
            }

            build("build") {
                assertKaptSuccessful()
                val regex = "(?m)^.*Kotlin compiler args.*-P plugin:org\\.jetbrains\\.kotlin\\.kapt3.*$".toRegex()
                val kaptArgs = regex.find(output)?.value ?: error("Kapt compiler arguments are not found!")
                assert(kaptArgs.contains(arg)) { "Kapt compiler arguments should contain '$arg'" }
            }
        }
    }

    @DisplayName("generates Kotlin code")
    @GradleTest
    fun testOutputKotlinCode(gradleVersion: GradleVersion) {
        project("kaptOutputKotlinCode".withPrefix, gradleVersion) {
            build("build") {
                assertKaptSuccessful()
                assertFileExists(projectPath.resolve("build/generated/source/kapt/main/example/TestClassCustomized.java"))
                assertFileExists(projectPath.resolve("build/generated/source/kaptKotlin/main/TestClass.kt"))
                assertFileExists(kotlinClassesDir().resolve("example/TestClass.class"))
                assertFileExists(javaClassesDir().resolve("example/TestClassCustomized.class"))
            }
        }
    }

    @DisplayName("location mapping is working as expected")
    @GradleTest
    fun testLocationMapping(gradleVersion: GradleVersion) {
        project("locationMapping".withPrefix, gradleVersion) {
            val regex = "((Test\\.java)|(test\\.kt)):(\\d+): error: GenError element".toRegex()

            fun BuildResult.getErrorMessages(): String =
                regex.findAll(output).map { it.value }.joinToString("\n")

            fun genJavaErrorString(vararg lines: Int) =
                lines.joinToString("\n") { "Test.java:$it: error: GenError element" }

            fun genKotlinErrorString(vararg lines: Int) =
                lines.joinToString("\n") { "test.kt:$it: error: GenError element" }

            buildAndFail("build") {
                val actual = getErrorMessages()
                // try as 0 starting lines first, then as 1 starting line
                try {
                    assertEquals(expected = genJavaErrorString(8, 20), actual = actual)
                } catch (e: AssertionError) {
                    assertEquals(expected = genJavaErrorString(9, 21), actual = actual)
                }
            }

            buildGradle.modify {
                it.replace("mapDiagnosticLocations = false", "mapDiagnosticLocations = true")
            }

            buildAndFail("build") {
                val actual = getErrorMessages()
                // try as 0 starting lines first, then as 1 starting line
                try {
                    assertEquals(expected = genKotlinErrorString(3, 6), actual = actual)
                } catch (e: AssertionError) {
                    assertEquals(expected = genKotlinErrorString(4, 7), actual = actual)
                }
            }
        }
    }

    @DisplayName("should fail to add dependency into 'kapt' configuration when plugin is not applied")
    @GradleTest
    fun testNoKaptPluginApplied(gradleVersion: GradleVersion) {
        project("nokapt".withPrefix, gradleVersion) {

            buildAndFail("build") {
                assertOutputContains("Could not find method kapt() for arguments")
            }
        }
    }

    @DisplayName("Should re-run kapt on changes in local annotation processor")
    @GradleTest
    open fun testChangesInLocalAnnotationProcessor(gradleVersion: GradleVersion) {
        project("localAnnotationProcessor".withPrefix, gradleVersion) {
            build("build")

            val testAnnotationProcessor = subProject("annotation-processor").javaSourcesDir().resolve("TestAnnotationProcessor.kt")
            testAnnotationProcessor.modify { text ->
                val commentText = "// print warning "
                assert(text.contains(commentText))
                text.replace(commentText, "")
            }

            build("build") {
                assertTasksExecuted(
                    ":example:kaptKotlin",
                    ":example:kaptGenerateStubsKotlin"
                )

                assertOutputContains("Additional warning message from AP")
            }

            val exampleSubProjectBuildDir = subProject("example").projectPath.resolve("build")
            build(
                "build",
                buildOptions = defaultBuildOptions.copy(incremental = false)
            ) {
                // Java stubs should not be generated for Kotlin sources generated by annotation processors.
                assertFileNotExistsInTree(
                    exampleSubProjectBuildDir,
                    "TestGeneratedKt.java"
                )
                assertFileNotExistsInTree(
                    exampleSubProjectBuildDir,
                    "AnotherGenerated.java"
                )

                assertFileExistsInTree(
                    exampleSubProjectBuildDir,
                    "TestGeneratedKt.class"
                )
                assertFileExistsInTree(
                    exampleSubProjectBuildDir,
                    "AnotherGenerated.class"
                )
            }
        }
    }

    @DisplayName("should not resolve 'kapt' configuration during build configuration phase")
    @GradleTest
    fun testKaptConfigurationLazyResolution(gradleVersion: GradleVersion) {
        project("simple".withPrefix, gradleVersion) {
            buildGradle.append(
                "\ndependencies { kapt project.files { throw new GradleException(\"Resolved!\") } }"
            )
            // Check that the kapt configuration does not get resolved during the project evaluation:
            build("tasks") {
                assertOutputDoesNotContain("Resolved!")
            }
        }
    }

    @DisplayName("Should be possible to disable discovery in compile classpath")
    @GradleTest
    open fun testDisableDiscoveryInCompileClasspath(gradleVersion: GradleVersion) {
        project(
            "kaptAvoidance".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                kaptOptions = kaptOptions().copy(includeCompileClasspath = true)
            )
        ) {
            val appSubproject = subProject("app")

            appSubproject.buildGradle.modify {
                it.addBeforeSubstring("//", "kapt \"org.jetbrains.kotlin")
            }
            build("assemble") {
                assertOutputContains("Annotation processors discovery from compile classpath is deprecated")
            }

            appSubproject.buildGradle.modify {
                "$it\n\nkapt.includeCompileClasspath = false"
            }
            buildAndFail("assemble") {
                assertOutputDoesNotContain("Annotation processors discovery from compile classpath is deprecated")
            }
        }
    }

    @DisplayName("up-to-date checks are working")
    @GradleTest
    fun testKaptAvoidance(gradleVersion: GradleVersion) {
        project("kaptAvoidance".withPrefix, gradleVersion) {

            subProject("app").buildGradle.modify {
                "$it\n\nkapt.includeCompileClasspath = true"
            }

            build("assemble") {
                assertTasksExecuted(
                    ":app:kaptGenerateStubsKotlin",
                    ":app:kaptKotlin",
                    ":app:compileKotlin",
                    ":app:compileJava",
                    ":lib:compileKotlin"
                )
            }

            val original = "fun foo() = 0"
            val replacement1 = "fun foo() = 1"
            val replacement2 = "fun foo() = 2"
            val libClassKt = subProject("lib").kotlinSourcesDir().resolve("LibClass.kt")
            libClassKt.modify { it.checkedReplace(original, replacement1) }

            build("assemble") {
                assertTasksExecuted(
                    ":lib:compileKotlin",
                    ":app:kaptGenerateStubsKotlin",
                    ":app:kaptKotlin"
                )
            }

            // enable discovery
            subProject("app").buildGradle.modify {
                it.replace(
                    "kapt.includeCompileClasspath = true",
                    "kapt.includeCompileClasspath = false"
                )
            }
            build("assemble") {
                assertTasksUpToDate(":lib:compileKotlin")
                assertTasksExecuted(
                    ":app:kaptGenerateStubsKotlin",
                    ":app:kaptKotlin"
                )
            }

            libClassKt.modify { it.checkedReplace(replacement1, replacement2) }
            build("assemble") {
                assertTasksExecuted(":lib:compileKotlin", ":app:kaptGenerateStubsKotlin")
                assertTasksUpToDate(":app:kaptKotlin")
            }
        }
    }

    @DisplayName("KT19179 and KT37241: kapt is not skipped and does not generate stubs for non-existent entries")
    @GradleTest
    open fun testKt19179andKt37241(gradleVersion: GradleVersion) {
        project("kt19179".withPrefix, gradleVersion) {

            build("build") {
                val processorSubproject = subProject("processor")
                processorSubproject
                    .assertFileInProjectExists("build/tmp/kapt3/classes/main/META-INF/services/javax.annotation.processing.Processor")

                val processorJar = processorSubproject.projectPath.resolve("build/libs/processor.jar")
                assertFileExists(processorJar)

                val zip = ZipFile(processorJar.toFile())
                @Suppress("ConvertTryFinallyToUseCall")
                try {
                    assert(zip.getEntry("META-INF/services/javax.annotation.processing.Processor") != null) {
                        "Generated annotation processor jar file does not contain processor service entry!"
                    }
                } finally {
                    zip.close()
                }

                assertTasksExecuted(
                    ":processor:kaptGenerateStubsKotlin",
                    ":processor:kaptKotlin",
                    ":app:kaptGenerateStubsKotlin",
                    ":app:kaptKotlin"
                )

                // Test for KT-37241, check the that non-existent classpath entry is filtered out:
                assertOutputDoesNotContain("Classpath entry points to a non-existent location")
            }

            val testKt = subProject("app").kotlinSourcesDir().resolve("Test.kt")
            testKt.modify { text ->
                assert("SomeClass()" in text)
                text.replace("SomeClass()", "SomeClass(); val a = 5")
            }

            build("build") {
                assertTasksUpToDate(
                    ":processor:kaptGenerateStubsKotlin",
                    ":processor:kaptKotlin",
                    ":app:kaptKotlin"
                )
                assertTasksExecuted(":app:kaptGenerateStubsKotlin")
            }

            testKt.modify { text ->
                "$text\n\nfun t() {}"
            }

            build("build") {
                assertTasksUpToDate(":processor:kaptGenerateStubsKotlin", ":processor:kaptKotlin")
                assertTasksExecuted(":app:kaptGenerateStubsKotlin", ":app:kaptKotlin")
            }
        }
    }

    @DisplayName("Dependency on kapt module should not resolve all configurations")
    @GradleTest
    fun testDependencyOnKaptModule(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            includeOtherProjectAsSubmodule("simple", "kapt2")
            buildGradle.append("\ndependencies { implementation project(':simple') }")

            testResolveAllConfigurations()
        }
    }

    @DisplayName("kapt works with old MPP")
    @GradleTest
    fun testMPPKaptPresence(gradleVersion: GradleVersion) {
        project("mpp-kapt-presence".withPrefix, gradleVersion) {

            build("build") {
                assertTasksExecuted(":dac:jdk:kaptGenerateStubsKotlin", ":dac:jdk:compileKotlin")
            }
        }
    }

    @DisplayName("KT-31127: processor using Filer api does not break 'javaCompile' task")
    @GradleTest
    fun testKotlinProcessorUsingFiler(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            buildGradle.modify {
                val subStringBeforePlugins = it.substringBefore("}")
                val subStringAfterPlugins = it.substringAfter("}")

                """
                |$subStringBeforePlugins
                |    id 'org.jetbrains.kotlin.kapt'
                |}
                |$subStringAfterPlugins
                |
                |dependencies {
                |   kapt "org.jetbrains.kotlin:annotation-processor-example:${"$"}kotlin_version"
                |   implementation "org.jetbrains.kotlin:annotation-processor-example:${"$"}kotlin_version"
                |}
                """.trimMargin()
            }

            // The test must not contain any java sources in order to detect the issue.
            assertEquals(emptyList(), projectPath.resolve("src").allJavaSources)
            kotlinSourcesDir().resolve("Dummy.kt").modify {
                it.replace("class Dummy", "@example.KotlinFilerGenerated class Dummy")
            }

            build("build") {
                assertFileInProjectExists("build/generated/source/kapt/main/demo/DummyGenerated.kt")
                assertTasksExecuted(":compileKotlin")
                assertTasksNoSource(":compileJava")
            }
        }
    }

    @DisplayName("should do annotation processing when 'sourceCompatibility = 8' and JDK is 11+")
    @JdkVersions(versions = [JavaVersion.VERSION_11])
    @GradleWithJdkTest
    fun testSimpleWithJdk11AndSourceLevel8(
        gradleVersion: GradleVersion,
        jdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "simple".withPrefix,
            gradleVersion,
            buildJdk = jdk.location
        ) {
            buildGradle.append(
                "\nsourceCompatibility = '8'"
            )

            build("assemble") {
                assertTasksExecuted(":kaptKotlin", ":kaptGenerateStubsKotlin")
                assertOutputContains("Javac options: {-source=1.8}")
            }
        }
    }

    @DisplayName("Works with JPMS on JDK 9+")
    @JdkVersions(versions = [JavaVersion.VERSION_1_9])
    @GradleWithJdkTest
    fun testJpmsModule(
        gradleVersion: GradleVersion,
        jdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "jpms-module".withPrefix,
            gradleVersion,
            buildJdk = jdk.location
        ) {
            build("assemble") {
                assertTasksExecuted(":kaptKotlin", ":kaptGenerateStubsKotlin", ":compileKotlin", ":compileJava")
                assertFileInProjectExists("build/generated/source/kapt/main/lab/TestClassGenerated.java")
                assertFileExists(kotlinClassesDir().resolve("lab/TestClass.class"))
            }

            build("assemble") {
                assertTasksUpToDate(":kaptKotlin", ":kaptGenerateStubsKotlin", ":compileKotlin", ":compileJava")
            }

            kotlinSourcesDir().resolve("dagger_example/InjectedClass.kt").modify { text ->
                text.checkedReplace(
                    "//placeholder",
                    "fun someChange() = null"
                )
            }

            build("assemble") {
                assertTasksExecuted(":kaptKotlin", ":kaptGenerateStubsKotlin", ":compileKotlin", ":compileJava")
            }
        }
    }

    @DisplayName("KT-46651: kapt is tracking source files properly with configuration cache enabled")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_6_7)
    @GradleTest
    fun kaptGenerateStubsShouldNotCaptureSourcesStateInConfigurationCache(gradleVersion: GradleVersion) {
        project(
            "incrementalRebuild".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.withConfigurationCache
        ) {
            build("assemble")

            javaSourcesDir().resolve("bar/UseBar.kt").apply {
                modify {
                    it.replace("UseBar", "UseBar1")
                }
                Files.move(this, parent.resolve("UseBar1.kt"))
            }

            build("assemble")
        }
    }

    @DisplayName("KT-47347: kapt processors should not be an input files for stub generation")
    @GradleTest
    open fun testChangesToKaptConfigurationDoNotTriggerStubGeneration(gradleVersion: GradleVersion) {
        project("localAnnotationProcessor".withPrefix, gradleVersion) {
            build("assemble")

            ZipOutputStream(projectPath.resolve("fake_processor.jar").outputStream()).close()
            subProject("example").buildGradle.append(
                //language=Gradle
                """

                dependencies {
                    kapt files("../fake_processor.jar")
                }
                """.trimIndent()
            )

            build("assemble") {
                assertTasksExecuted(":example:kaptKotlin")
                assertTasksUpToDate(":example:kaptGenerateStubsKotlin")
            }
        }
    }
}
