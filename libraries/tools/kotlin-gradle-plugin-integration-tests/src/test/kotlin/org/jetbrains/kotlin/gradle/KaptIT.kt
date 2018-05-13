package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.allJavaFiles
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class KaptIT : BaseGradleIT() {

    @Test
    fun testSimple() {
        val project = Project("kaptSimple")
        project.allowOriginalKapt()

        project.build("build") {
            assertSuccessful()
            assertContains("kapt: Class file stubs are not used")
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            assertFileExists(javaClassesDir() + "example/TestClassGenerated.class")
            assertNoSuchFile(javaClassesDir() + "example/SourceAnnotatedTestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/RuntimeAnnotatedTestClassGenerated.class")
            assertContains("example.JavaTest PASSED")
            assertContains("example.KotlinTest PASSED")
            assertClassFilesNotContain(File(project.projectDir, kotlinClassesDir()), "ExampleSourceAnnotation")
            assertClassFilesNotContain(File(project.projectDir, javaClassesDir()), "ExampleSourceAnnotation")
        }

        // clean build is important
        // because clean can delete hack annotation file before build
        project.build("clean", "build") {
            assertSuccessful()
        }
    }

    @Test
    fun testEnumConstructor() {
        val project = Project("kaptEnumConstructor")
        project.allowOriginalKapt()

        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
        }

        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testStubs() {
        val project = Project("kaptStubs", GradleVersionRequired.Exact("3.5"))
        project.allowOriginalKapt()

        project.build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            assertFileExists(javaClassesDir() + "example/TestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/SourceAnnotatedTestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/RuntimeAnnotatedTestClassGenerated.class")
            assertNotContains("w: Classpath entry points to a non-existent location")
            assertContains("example.JavaTest PASSED")
        }

        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testStubsWithoutJava() {
        val project = Project("kaptStubs", GradleVersionRequired.Exact("3.5"))
        project.allowOriginalKapt()
        project.projectDir.allJavaFiles().forEach { it.delete() }

        project.build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            assertFileExists(javaClassesDir() + "example/TestClassGenerated.class")
        }
    }

    @Test
    fun testSimpleIncrementalBuild() {
        doTestIncrementalBuild("kaptSimple", listOf(":compileKotlin", ":compileJava"))
    }

    @Test
    fun testStubsIncrementalBuild() {
        doTestIncrementalBuild("kaptStubs", listOf(":compileKotlin", ":compileJava", ":compileKotlinAfterJava"))
    }

    private fun doTestIncrementalBuild(projectName: String, compileTasks: List<String>) {
        val project = Project(projectName, GradleVersionRequired.Exact("3.5"))
        project.allowOriginalKapt()

        project.build("build") {
            assertSuccessful()
        }

        project.projectDir.getFileByName("test.kt").appendText(" ")
        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(compileTasks)
        }

        repeat(2) {
            project.build("build") {
                assertSuccessful()
                assertTasksUpToDate(compileTasks)
            }
        }

        project.build("clean", "build") {
            assertSuccessful()
            assertTasksExecuted(compileTasks)
        }

        repeat(2) {
            project.build("build") {
                assertSuccessful()
                assertTasksUpToDate(compileTasks)
            }
        }
    }

    @Test
    fun testArguments() {
        val project = Project("kaptArguments")
        project.allowOriginalKapt()

        project.build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/example/TestClassCustomized.java")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            assertFileExists(javaClassesDir() + "example/TestClassCustomized.class")
        }
    }

    @Test
    fun testInheritedAnnotations() {
        val project = Project("kaptInheritedAnnotations")
        project.allowOriginalKapt()

        project.build("build") {
            assertSuccessful()
            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/example/AncestorClassGenerated.java")
            assertFileExists(javaClassesDir() + "example/TestClassGenerated.class")
            assertFileExists(javaClassesDir() + "example/AncestorClassGenerated.class")
        }
    }

    @Test
    fun testOutputKotlinCode() {
        val project = Project("kaptOutputKotlinCode")
        project.allowOriginalKapt()

        project.build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertTasksExecuted(":compileKotlin", ":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/example/TestClassCustomized.java")
            assertFileExists("build/tmp/kapt/main/kotlinGenerated/TestClass.kt")
            assertFileExists(kotlinClassesDir() + "example/TestClass.class")
            assertFileExists(javaClassesDir() + "example/TestClassCustomized.class")
        }
    }

    @Test
    fun testInternalUserIsModifiedStubsIC() {
        val options = defaultBuildOptions().copy(incremental = true)

        val project = Project("kaptStubs", GradleVersionRequired.Exact("3.5"))
        project.allowOriginalKapt()

        project.build("build", options = options) {
            assertSuccessful()
        }

        val internalDummyUserKt = project.projectDir.getFileByName("InternalDummyUser.kt")
        internalDummyUserKt.modify { it + " " }
        val internalDummyTestKt = project.projectDir.getFileByName("InternalDummyTest.kt")
        internalDummyTestKt.modify { it + " " }

        project.build("build", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(internalDummyUserKt, internalDummyTestKt))
        }
    }

    @Test
    fun testKotlinCompilerNotCalledStubsIC() {
        val options = defaultBuildOptions().copy(incremental = true)

        val project = Project("kaptStubs", GradleVersionRequired.Exact("3.5"))
        project.allowOriginalKapt()

        project.build("build", options = options) {
            assertSuccessful()
        }

        val javaDummy = project.projectDir.getFileByName("JavaDummy.java")
        javaDummy.modify { it + " " }

        project.build("build", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(emptyList())
        }
    }
}