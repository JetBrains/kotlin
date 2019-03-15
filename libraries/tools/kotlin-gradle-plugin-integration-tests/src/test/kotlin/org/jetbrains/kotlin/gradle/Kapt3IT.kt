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

import org.jetbrains.kotlin.gradle.tasks.USING_JVM_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

abstract class Kapt3BaseIT : BaseGradleIT() {
    companion object {
        private val KAPT_SUCCESSFUL_REGEX = "Annotation processing complete, errors: 0".toRegex()
    }

    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(kaptOptions = kaptOptions())

    protected open fun kaptOptions(): KaptOptions =
        KaptOptions(verbose = true, useWorkers = false)

    fun CompiledProject.assertKaptSuccessful() {
        KAPT_SUCCESSFUL_REGEX.findAll(this.output).count() > 0
    }
}

class Kapt3WorkersIT : Kapt3IT() {
    override fun kaptOptions(): KaptOptions =
        super.kaptOptions().copy(useWorkers = true)

    @Test
    fun testJavacIsLoadedOnce() {
        // todo: actual minimum version is 4.3, but I had some problems. Investigate later.
        // todo: consider minimum version for the whole class, with Gradle <4.3 all tests duplicate tests without workers
        val gradleVersionRequired = GradleVersionRequired.AtLeast("4.5.1")

        val project =
            Project("javacIsLoadedOnce", directoryPrefix = "kapt2", gradleVersionRequirement = gradleVersionRequired)
        project.build("build") {
            assertSuccessful()
            assertSubstringCount("Loaded com.sun.tools.javac.util.Context from", 1)
        }
    }

    @Test
    fun testKaptSkipped() {
        val gradleVersionRequired = GradleVersionRequired.AtLeast("4.3")

        val project =
            Project("kaptSkipped", directoryPrefix = "kapt2", gradleVersionRequirement = gradleVersionRequired)
        project.build("build") {
            assertSuccessful()
        }
    }
}

open class Kapt3IT : Kapt3BaseIT() {
    @Test
    fun testAnnotationProcessorAsFqName() {
        val project = Project("annotationProcessorAsFqName", directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            assertFileExists(javaClassesDir() + "example/TestClassGenerated.class")
        }
    }

    @Test
    fun testSimple() {
        val project = Project("simple", directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            val javaClassesDir = javaClassesDir()
            assertFileExists(javaClassesDir + "example/TestClassGenerated.class")
            assertFileExists(javaClassesDir + "example/SourceAnnotatedTestClassGenerated.class")
            assertFileExists(javaClassesDir + "example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists(javaClassesDir + "example/RuntimeAnnotatedTestClassGenerated.class")
            assertContains("example.JavaTest PASSED")
            assertClassFilesNotContain(File(project.projectDir, "build/classes"), "ExampleSourceAnnotation")
            assertNotContains("warning: The following options were not recognized by any processor")
            assertContains("Need to discovery annotation processors in the AP classpath")
        }

        project.build("build") {
            assertSuccessful()
            assertTasksUpToDate(":compileKotlin", ":compileJava")
        }
    }

    @Test
    fun testSimpleWithIC() {
        val options = defaultBuildOptions().copy(incremental = true)
        val project = Project("simple", directoryPrefix = "kapt2")
        val javaClassesDir = File(project.projectDir, project.classesDir(language = "java"))

        project.build("clean", "build", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertClassFilesNotContain(javaClassesDir, "ExampleSourceAnnotation")
        }

        project.projectDir.getFilesByNames("InternalDummy.kt", "test.kt").forEach { it.appendText(" ") }
        project.build("build", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertTasksExecuted(":compileKotlin")
            // there are no actual changes in Java sources, generated sources, Kotlin classes
            assertTasksUpToDate(":compileJava")
            assertClassFilesNotContain(javaClassesDir, "ExampleSourceAnnotation")
        }

        // emulating wipe by android plugin's IncrementalSafeguardTask
        javaClassesDir.deleteRecursively()
        project.build("build", options = options) {
            assertSuccessful()
            assertTasksUpToDate(":kaptGenerateStubsKotlin", ":kaptKotlin", ":compileKotlin")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            assertClassFilesNotContain(javaClassesDir, "ExampleSourceAnnotation")
        }
    }

    @Test
    fun testDisableIcForGenerateStubs() {
        val project = Project("simple", directoryPrefix = "kapt2")
        project.build("build", options = defaultBuildOptions().copy(incremental = false)) {
            assertSuccessful()
            assertTasksExecuted(":kaptGenerateStubsKotlin")
            assertNotContains(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
        }
    }

    @Test
    fun testInheritedAnnotations() {
        Project("inheritedAnnotations", directoryPrefix = "kapt2").build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/AncestorClassGenerated.java")
            assertFileExists(javaClassesDir() + "example/TestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/AncestorClassGenerated.class")
        }
    }

    @Test
    fun testArguments() {
        Project("arguments", directoryPrefix = "kapt2").build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(
                "AP options: {suffix=Customized, justColon=:, justEquals==, containsColon=a:b, " +
                        "containsEquals=a=b, startsWithColon=:a, startsWithEquals==a, endsWithColon=a:, " +
                        "endsWithEquals=a:, withSpace=a b c,"
            )
            assertContains("-Xmaxerrs=500, -Xlint:all=-Xlint:all") // Javac options test
            assertFileExists("build/generated/source/kapt/main/example/TestClassCustomized.java")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            assertFileExists(javaClassesDir() + "example/TestClassCustomized.class")
            assertContains("Annotation processor class names are set, skip AP discovery")
        }
    }

    @Test
    fun testGeneratedDirectoryIsUpToDate() {
        val project = Project("generatedDirUpToDate", directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")

            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/SourceAnnotatedTestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/BinaryAnnotatedTestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/RuntimeAnnotatedTestClassGenerated.java")

            assertFileExists(javaClassesDir() + "example/TestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/SourceAnnotatedTestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/RuntimeAnnotatedTestClassGenerated.class")
        }

        val testKt = project.projectDir.getFileByName("test.kt")
        testKt.writeText(testKt.readText().replace("@ExampleBinaryAnnotation", ""))

        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")

            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/SourceAnnotatedTestClassGenerated.java")
            /*!*/   assertNoSuchFile("build/generated/source/kapt/main/example/BinaryAnnotatedTestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/RuntimeAnnotatedTestClassGenerated.java")

            assertFileExists(javaClassesDir() + "example/TestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/SourceAnnotatedTestClassGenerated.class")
            /*!*/   assertNoSuchFile(javaClassesDir() + "example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/RuntimeAnnotatedTestClassGenerated.class")
        }
    }

    @Test
    fun testRemoveJavaClassICRebuild() {
        testICRebuild { project ->
            project.projectFile("Foo.java").delete()
        }
    }

    @Test
    fun testChangeClasspathICRebuild() {
        testICRebuild { project ->
            project.projectFile("build.gradle").modify {
                "$it\ndependencies { compile 'org.jetbrains.kotlin:kotlin-reflect:' + kotlin_version }"
            }
        }
    }

    // tests all output directories are cleared when IC rebuilds
    private fun testICRebuild(performChange: (Project) -> Unit) {
        val project = Project("incrementalRebuild", directoryPrefix = "kapt2")
        val options = defaultBuildOptions().copy(incremental = true)
        val generatedSrc = "build/generated/source/kapt/main"

        project.build("build", options = options) {
            assertSuccessful()

            // generated sources
            assertFileExists("$generatedSrc/bar/UseBar_MembersInjector.java")
        }

        performChange(project)
        project.projectFile("UseBar.kt").modify { it.replace("package bar", "package foo.bar") }

        project.build("build", options = options) {
            assertSuccessful()
            assertTasksExecuted(":kaptGenerateStubsKotlin", ":kaptKotlin", ":compileKotlin", ":compileJava")

            // generated sources
            assertFileExists("$generatedSrc/foo/bar/UseBar_MembersInjector.java")
            assertNoSuchFile("$generatedSrc/bar/UseBar_MembersInjector.java")

            // classes
            assertFileExists(kotlinClassesDir() + "foo/bar/UseBar.class")
            assertNoSuchFile(kotlinClassesDir() + "bar/UseBar.class")
            assertFileExists(javaClassesDir() + "foo/bar/UseBar_MembersInjector.class")
            assertNoSuchFile(javaClassesDir() + "bar/UseBar_MembersInjector.class")
        }
    }

    @Test
    fun testRemoveAnnotationIC() {
        val project = Project("simple", directoryPrefix = "kapt2")
        val options = defaultBuildOptions().copy(incremental = true)
        project.setupWorkingDir()
        val internalDummyKt = project.projectDir.getFileByName("InternalDummy.kt")

        // add annotation
        val exampleAnn = "@example.ExampleAnnotation "
        internalDummyKt.modify { it.addBeforeSubstring(exampleAnn, "internal class InternalDummy") }

        project.build("classes", options = options) {
            assertSuccessful()
            assertFileExists("build/generated/source/kapt/main/foo/InternalDummyGenerated.java")
        }

        // remove annotation
        internalDummyKt.modify { it.replace(exampleAnn, "") }

        project.build("classes", options = options) {
            assertSuccessful()
            val allMainKotlinSrc = File(project.projectDir, "src/main").allKotlinFiles()
            assertCompiledKotlinSources(project.relativize(allMainKotlinSrc))
            assertNoSuchFile("build/generated/source/kapt/main/foo/InternalDummyGenerated.java")
        }
    }

    @Test
    fun testKt18799() {
        val project = Project("kt18799", directoryPrefix = "kapt2")

        project.build("kaptKotlin") {
            assertSuccessful()
        }

        project.projectDir.getFileByName("com.b.A.kt").modify {
            val line = "@Factory(factoryClass = CLASS_NAME, something = arrayOf(Test()))"
            assert(line in it)
            it.replace(line, "@Factory(factoryClass = CLASS_NAME)")
        }

        project.build("kaptKotlin") {
            assertSuccessful()
        }
    }

    /**
     * Tests that compile arguments are properly copied from compileKotlin to kaptTask
     */
    @Test
    fun testCopyCompileArguments() {
        val project = Project("simple", directoryPrefix = "kapt2")
        project.setupWorkingDir()

        val arg = "-Xskip-runtime-version-check"
        project.projectDir.getFileByName("build.gradle").modify {
            it + """
                $SYSTEM_LINE_SEPARATOR
                compileKotlin { kotlinOptions.freeCompilerArgs = ['$arg'] }
            """.trimIndent()
        }

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            val regex = "(?m)^.*Kotlin compiler args.*-P plugin:org\\.jetbrains\\.kotlin\\.kapt3.*$".toRegex()
            val kaptArgs = regex.find(output)?.value ?: error("Kapt compiler arguments are not found!")
            assert(kaptArgs.contains(arg)) { "Kapt compiler arguments should contain '$arg'" }
        }
    }

    @Test
    fun testOutputKotlinCode() {
        Project("kaptOutputKotlinCode", directoryPrefix = "kapt2").build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("build/generated/source/kapt/main/example/TestClassCustomized.java")
            assertFileExists("build/generated/source/kaptKotlin/main/TestClass.kt")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            assertFileExists(javaClassesDir() + "example/TestClassCustomized.class")
        }
    }

    @Test
    fun testLocationMapping() {
        val project = Project("locationMapping", directoryPrefix = "kapt2")
        val regex = "((Test\\.java)|(test\\.kt)):(\\d+): error: GenError element".toRegex()

        fun CompiledProject.getErrorMessages(): String =
            regex.findAll(output).map { it.value }.joinToString("\n")

        fun genJavaErrorString(vararg lines: Int) =
            lines.joinToString("\n") { "Test.java:$it: error: GenError element" }

        fun genKotlinErrorString(vararg lines: Int) =
            lines.joinToString("\n") { "test.kt:$it: error: GenError element" }

        project.build("build") {
            assertFailed()
            val actual = getErrorMessages()
            // try as 0 starting lines first, then as 1 starting line
            try {
                Assert.assertEquals(genJavaErrorString(9, 17), actual)
            } catch (e: AssertionError) {
                Assert.assertEquals(genJavaErrorString(10, 18), actual)
            }
        }

        project.projectDir.getFileByName("build.gradle").modify {
            it.replace("mapDiagnosticLocations = false", "mapDiagnosticLocations = true")
        }

        project.build("build") {
            assertFailed()
            val actual = getErrorMessages()
            // try as 0 starting lines first, then as 1 starting line
            try {
                Assert.assertEquals(genKotlinErrorString(2, 6), actual)
            } catch (e: AssertionError) {
                Assert.assertEquals(genKotlinErrorString(3, 7), actual)
            }
        }
    }

    @Test
    fun testNoKaptPluginApplied() {
        val project = Project("nokapt", directoryPrefix = "kapt2")

        project.build("build") {
            assertFailed()
            assertContains("Could not find method kapt() for arguments")
        }
    }

    @Test
    fun testChangesInLocalAnnotationProcessor() {
        val project = Project("localAnnotationProcessor", directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
        }

        val testAnnotationProcessor = project.projectDir.getFileByName("TestAnnotationProcessor.kt")
        testAnnotationProcessor.modify { text ->
            val commentText = "// print warning "
            assert(text.contains(commentText))
            text.replace(commentText, "")
        }

        project.build("build") {
            assertSuccessful()
            assertNotContains(
                ":example:kaptKotlin UP-TO-DATE",
                ":example:kaptGenerateStubsKotlin UP-TO-DATE"
            )

            assertContains("Additional warning message from AP")
        }
    }

    @Test
    fun testKaptConfigurationLazyResolution() = with(Project("simple", directoryPrefix = "kapt2")) {
        setupWorkingDir()
        File(projectDir, "build.gradle").appendText(
            "\ndependencies { kapt project.files { throw new GradleException(\"Resolved!\") } }"
        )
        // Check that the kapt configuration does not get resolved during the project evaluation:
        build("tasks") {
            assertSuccessful()
            assertNotContains("Resolved!")
        }
    }

    @Test
    fun testDisableDiscoveryInCompileClasspath() = with(Project("kaptAvoidance", directoryPrefix = "kapt2")) {
        setupWorkingDir()
        val buildGradle = projectDir.resolve("app/build.gradle")
        buildGradle.modify {
            it.addBeforeSubstring("//", "kapt \"org.jetbrains.kotlin")
        }
        build("assemble") {
            assertSuccessful()
            assertContains("Annotation processors discovery from compile classpath is deprecated")
        }

        buildGradle.modify {
            "$it\n\nkapt.includeCompileClasspath = false"
        }
        build("assemble") {
            assertFailed()
            assertNotContains("Annotation processors discovery from compile classpath is deprecated")
        }
    }


    @Test
    fun testKaptAvoidance() = with(Project("kaptAvoidance", directoryPrefix = "kapt2")) {
        build("assemble") {
            assertSuccessful()
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
        val libClassKt = projectDir.getFileByName("LibClass.kt")
        libClassKt.modify { it.checkedReplace(original, replacement1) }

        build("assemble") {
            assertSuccessful()
            assertTasksExecuted(
                ":lib:compileKotlin",
                ":app:kaptGenerateStubsKotlin",
                ":app:kaptKotlin"
            )
        }

        // enable discovery
        projectDir.resolve("app/build.gradle").modify {
            "$it\n\nkapt.includeCompileClasspath = false"
        }
        build("assemble") {
            assertSuccessful()
            assertTasksUpToDate(":lib:compileKotlin")
            assertTasksExecuted(
                ":app:kaptGenerateStubsKotlin",
                ":app:kaptKotlin"
            )
        }

        libClassKt.modify { it.checkedReplace(replacement1, replacement2) }
        build("assemble") {
            assertSuccessful()
            assertTasksExecuted(":lib:compileKotlin", ":app:kaptGenerateStubsKotlin")
            assertTasksUpToDate(":app:kaptKotlin")
        }
    }

    @Test
    fun testKt19179() {
        val project = Project("kt19179", directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertFileExists("processor/build/tmp/kapt3/classes/main/META-INF/services/javax.annotation.processing.Processor")

            val processorJar = fileInWorkingDir("processor/build/libs/processor.jar")
            assert(processorJar.exists())

            val zip = ZipFile(processorJar)
            @Suppress("ConvertTryFinallyToUseCall")
            try {
                assert(zip.getEntry("META-INF/services/javax.annotation.processing.Processor") != null)
            } finally {
                zip.close()
            }

            assertTasksExecuted(
                ":processor:kaptGenerateStubsKotlin",
                ":processor:kaptKotlin",
                ":app:kaptGenerateStubsKotlin",
                ":app:kaptKotlin"
            )
        }

        project.projectDir.getFileByName("Test.kt").modify { text ->
            assert("SomeClass()" in text)
            text.replace("SomeClass()", "SomeClass(); val a = 5")
        }

        project.build("build") {
            assertSuccessful()
            assertTasksUpToDate(":processor:kaptGenerateStubsKotlin", ":processor:kaptKotlin", ":app:kaptKotlin")
            assertTasksExecuted(":app:kaptGenerateStubsKotlin")
        }

        project.projectDir.getFileByName("Test.kt").modify { text ->
            text + "\n\nfun t() {}"
        }

        project.build("build") {
            assertSuccessful()
            assertTasksUpToDate(":processor:kaptGenerateStubsKotlin", ":processor:kaptKotlin")
            assertTasksExecuted(":app:kaptGenerateStubsKotlin", ":app:kaptKotlin")
        }
    }

    @Test
    fun testDependencyOnKaptModule() = with(Project("simpleProject")) {
        setupWorkingDir()

        val kaptProject = Project("simple", directoryPrefix = "kapt2").apply { setupWorkingDir() }
        kaptProject.projectDir.copyRecursively(projectDir.resolve("simple"))
        projectDir.resolve("settings.gradle").writeText("include 'simple'")
        gradleBuildScript().appendText("\ndependencies { implementation project(':simple') }")

        testResolveAllConfigurations()
    }

    @Test
    fun testMPPKaptPresence() {
        val project = Project("mpp-kapt-presence", directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":dac:jdk:kaptGenerateStubsKotlin", ":dac:jdk:compileKotlin")
        }
    }
}