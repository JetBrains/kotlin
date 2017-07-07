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
    companion object {
        private const val GRADLE_VERSION = "3.3"
    }

    @Test
    fun testAnnotationProcessorAsFqName() {
        val project = Project("annotationProcessorAsFqName", GRADLE_VERSION, directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
        }
    }

    @Test
    fun testSimple() {
        val project = Project("simple", GRADLE_VERSION, directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/SourceAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/RuntimeAnnotatedTestClassGenerated.class")
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
        val project = Project("simple", GRADLE_VERSION, directoryPrefix = "kapt2")
        val classesDir = File(project.projectDir, "build/classes")

        project.build("clean", "build", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertClassFilesNotContain(classesDir, "ExampleSourceAnnotation")
        }

        project.projectDir.getFilesByNames("InternalDummy.kt", "test.kt").forEach { it.appendText(" ") }
        project.build("build", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertClassFilesNotContain(classesDir, "ExampleSourceAnnotation")
        }

        // emulating wipe by android plugin's IncrementalSafeguardTask
        classesDir.deleteRecursively()
        project.build("build", options = options) {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
            assertContains(":kaptGenerateStubsKotlin UP-TO-DATE")
            assertContains(":kaptKotlin UP-TO-DATE")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertClassFilesNotContain(classesDir, "ExampleSourceAnnotation")
        }
    }

    @Test
    fun testDisableIcForGenerateStubs() {
        val project = Project("simple", GRADLE_VERSION, directoryPrefix = "kapt2")
        project.build("build", options = defaultBuildOptions().copy(incremental = false)) {
            assertSuccessful()
            assertContains(":kaptGenerateStubsKotlin")
            assertNotContains(USING_INCREMENTAL_COMPILATION_MESSAGE)
        }
    }

    @Test
    fun testArguments() {
        Project("arguments", GRADLE_VERSION, directoryPrefix = "kapt2").build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains("Options: {suffix=Customized, justColon=:, justEquals==, containsColon=a:b, " +
                    "containsEquals=a=b, startsWithColon=:a, startsWithEquals==a, endsWithColon=a:, " +
                    "endsWithEquals=a:, withSpace=a b c,")
            assertContains("-Xmaxerrs=500, -Xlint:all=-Xlint:all") // Javac options test
            assertFileExists("build/generated/source/kapt/main/example/TestClassCustomized.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassCustomized.class")
        }
    }

    @Test
    fun testInheritedAnnotations() {
        Project("inheritedAnnotations", GRADLE_VERSION, directoryPrefix = "kapt2").build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/AncestorClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/AncestorClassGenerated.class")
        }
    }

    @Test
    fun testGeneratedDirectoryIsUpToDate() {
        val project = Project("generatedDirUpToDate", GRADLE_VERSION, directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/classes/main/example/TestClass.class")

            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/SourceAnnotatedTestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/BinaryAnnotatedTestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/RuntimeAnnotatedTestClassGenerated.java")

            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/SourceAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/RuntimeAnnotatedTestClassGenerated.class")
        }

        val testKt = project.projectDir.getFileByName("test.kt")
        testKt.writeText(testKt.readText().replace("@ExampleBinaryAnnotation", ""))

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/classes/main/example/TestClass.class")

            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/SourceAnnotatedTestClassGenerated.java")
            /*!*/   assertNoSuchFile("build/generated/source/kapt/main/example/BinaryAnnotatedTestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/RuntimeAnnotatedTestClassGenerated.java")

            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/SourceAnnotatedTestClassGenerated.class")
            /*!*/   assertNoSuchFile("build/classes/main/example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/RuntimeAnnotatedTestClassGenerated.class")
        }
    }

    @Test
    fun testRemoveAnnotationIC() {
        val project = Project("simple", GRADLE_VERSION, directoryPrefix = "kapt2")
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
        val project = Project("kt18799", GRADLE_VERSION, directoryPrefix = "kapt2")

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
        val project = Project("autoService", GRADLE_VERSION, directoryPrefix = "kapt2")

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
        val project = Project("simple", GRADLE_VERSION, directoryPrefix = "kapt2")
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
        Project("kaptOutputKotlinCode", GRADLE_VERSION, directoryPrefix = "kapt2").build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("build/generated/source/kapt/main/example/TestClassCustomized.java")
            assertFileExists("build/generated/source/kaptKotlin/main/TestClass.kt")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassCustomized.class")
        }
    }
}