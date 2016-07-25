package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.allJavaFiles
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.junit.Test

class KaptIT: BaseGradleIT() {

    companion object {
        private const val GRADLE_VERSION = "2.10"
    }

    override fun defaultBuildOptions(): BuildOptions =
            super.defaultBuildOptions().copy(withDaemon = true)

    @Test
    fun testSimple() {
        val project = Project("kaptSimple", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertContains("kapt: Class file stubs are not used")
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/TestClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertNoSuchFile("build/classes/main/example/SourceAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/RuntimeAnnotatedTestClassGenerated.class")
            assertContains("example.JavaTest PASSED")
            assertContains("example.KotlinTest PASSED")
        }

        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testEnumConstructor() {
        val project = Project("kaptEnumConstructor", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
        }

        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testStubs() {
        val project = Project("kaptStubs", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/SourceAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/RuntimeAnnotatedTestClassGenerated.class")
            assertNotContains("w: Classpath entry points to a non-existent location")
            assertContains("example.JavaTest PASSED")
        }

        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testStubsWithoutJava() {
        val project = Project("kaptStubs", GRADLE_VERSION)
        project.setupWorkingDir()
        project.projectDir.allJavaFiles().forEach { it.delete() }

        project.build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
        }
    }

    @Test
    fun testSimpleIncrementalBuild() {
        doTestIncrementalBuild("kaptSimple", arrayOf(":compileKotlin", ":compileJava"))
    }

    @Test
    fun testStubsIncrementalBuild() {
        doTestIncrementalBuild("kaptStubs", arrayOf(":compileKotlin", ":compileJava", ":compileKotlinAfterJava"))
    }

    private fun doTestIncrementalBuild(projectName: String, compileTasks: Array<String>) {
        val compileTasksUpToDate = compileTasks.map { it + " UP-TO-DATE" }.toTypedArray()
        val project = Project(projectName, GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
        }

        project.projectDir.getFileByName("test.kt").appendText(" ")
        project.build("build") {
            assertSuccessful()
            assertContains(*compileTasks)
            assertNotContains(*compileTasksUpToDate)
        }

        repeat(2) {
            project.build("build") {
                assertSuccessful()
                assertContains(*compileTasksUpToDate)
            }
        }

        project.build("clean", "build") {
            assertSuccessful()
            assertContains(*compileTasks)
            assertNotContains(*compileTasksUpToDate)
        }

        repeat(2) {
            project.build("build") {
                assertSuccessful()
                assertContains(*compileTasksUpToDate)
            }
        }
    }

    @Test
    fun testArguments() {
        Project("kaptArguments", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/example/TestClassCustomized.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassCustomized.class")
        }
    }

    @Test
    fun testInheritedAnnotations() {
        Project("kaptInheritedAnnotations", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertFileExists("build/generated/source/kapt/main/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/AncestorClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/AncestorClassGenerated.class")
        }
    }

    @Test
    fun testOutputKotlinCode() {
        Project("kaptOutputKotlinCode", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/example/TestClassCustomized.java")
            assertFileExists("build/tmp/kapt/main/kotlinGenerated/TestClass.kt")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassCustomized.class")
        }
    }
}