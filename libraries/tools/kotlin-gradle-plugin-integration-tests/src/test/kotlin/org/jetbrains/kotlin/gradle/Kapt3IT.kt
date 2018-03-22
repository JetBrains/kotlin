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

import org.jetbrains.kotlin.gradle.tasks.USING_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import java.io.File

abstract class Kapt3BaseIT : BaseGradleIT() {
    companion object {
        private val KAPT_SUCCESSFUL_REGEX = "Annotation processing complete, errors: 0".toRegex()
    }

    override fun defaultBuildOptions(): BuildOptions =
            super.defaultBuildOptions().copy(withDaemon = true)

    fun CompiledProject.assertKaptSuccessful() {
        KAPT_SUCCESSFUL_REGEX.findAll(this.output).count() > 0
    }
}

open class Kapt3IT : Kapt3BaseIT() {
    @Test
    fun testAnnotationProcessorAsFqName() {
        val project = Project("annotationProcessorAsFqName", directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
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
            assertContains(":compileKotlin")
            assertContains(":compileJava")
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
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
            assertContains(":compileJava UP-TO-DATE")
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
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertClassFilesNotContain(javaClassesDir, "ExampleSourceAnnotation")
        }

        project.projectDir.getFilesByNames("InternalDummy.kt", "test.kt").forEach { it.appendText(" ") }
        project.build("build", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertClassFilesNotContain(javaClassesDir, "ExampleSourceAnnotation")
        }

        // emulating wipe by android plugin's IncrementalSafeguardTask
        javaClassesDir.deleteRecursively()
        project.build("build", options = options) {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
            assertContains(":kaptGenerateStubsKotlin UP-TO-DATE")
            assertContains(":kaptKotlin UP-TO-DATE")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            assertClassFilesNotContain(javaClassesDir, "ExampleSourceAnnotation")
        }
    }

    @Test
    fun testDisableIcForGenerateStubs() {
        val project = Project("simple", directoryPrefix = "kapt2")
        project.build("build", options = defaultBuildOptions().copy(incremental = false)) {
            assertSuccessful()
            assertContains(":kaptGenerateStubsKotlin")
            assertNotContains(USING_INCREMENTAL_COMPILATION_MESSAGE)
        }
    }

    @Test
    fun testArguments() {
        Project("arguments", directoryPrefix = "kapt2").build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains("Options: {suffix=Customized, justColon=:, justEquals==, containsColon=a:b, " +
                    "containsEquals=a=b, startsWithColon=:a, startsWithEquals==a, endsWithColon=a:, " +
                    "endsWithEquals=a:, withSpace=a b c,")
            assertContains("-Xmaxerrs=500, -Xlint:all=-Xlint:all") // Javac options test
            assertFileExists("build/generated/source/kapt/main/example/TestClassCustomized.java")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            assertFileExists(javaClassesDir() + "example/TestClassCustomized.class")
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
    fun testGeneratedDirectoryIsUpToDate() {
        val project = Project("generatedDirUpToDate", directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
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
            assertContains(":compileKotlin")
            assertContains(":compileJava")
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
            assertTasksExecuted(listOf(":kaptGenerateStubsKotlin", ":kaptKotlin", ":compileKotlin", ":compileJava"))

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
        internalDummyKt.modify { it.addBeforeSubstring(exampleAnn, "internal class InternalDummy")}

        project.build("classes", options = options) {
            assertSuccessful()
            assertFileExists("build/generated/source/kapt/main/foo/InternalDummyGenerated.java")
        }

        // remove annotation
        internalDummyKt.modify { it.replace(exampleAnn, "")}

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

    @Test
    fun testKaptClassesDirSync() {
        val project = Project("autoService", GradleVersionRequired.Exact("3.5"), directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("processor/build/classes/main/META-INF/services/javax.annotation.processing.Processor")
            assertFileExists("processor/build/classes/main/processor/MyProcessor.class")
        }

        project.projectDir.getFileByName("MyProcessor.kt").modify {
            it.replace("@AutoService(Processor::class)", "")
        }

        project.build(":processor:build") {
            assertSuccessful()
            assertNoSuchFile("processor/build/classes/main/META-INF/services/javax.annotation.processing.Processor")
            assertFileExists("processor/build/classes/main/processor/MyProcessor.class")
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

        project.build("build") {
            assertFailed()

            assertContains("Test.java:9: error: GenError element")
            assertContains("Test.java:17: error: GenError element")
        }

        project.projectDir.getFileByName("build.gradle").modify {
            it.replace("mapDiagnosticLocations = false", "mapDiagnosticLocations = true")
        }

        project.build("build") {
            assertFailed()

            assertNotContains("Test.java:9: error: GenError element")
            assertNotContains("Test.java:17: error: GenError element")

            assertContains("test.kt:3: error: GenError element")
            assertContains("test.kt:7: error: GenError element")
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
            assertNotContains(":example:kaptKotlin UP-TO-DATE",
                              ":example:kaptGenerateStubsKotlin UP-TO-DATE")

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
}