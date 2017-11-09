package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class SimpleKotlinGradleIT : BaseGradleIT() {

    companion object {
        private const val GRADLE_VERSION = "2.10"
    }

    @Test
    fun testSimpleCompile() {
        val project = Project("simpleProject", GRADLE_VERSION)

        project.build("compileDeployKotlin", "build") {
            assertSuccessful()
            assertContains("Finished executing kotlin compiler using daemon strategy")
            assertReportExists("build/reports/tests/classes/demo.TestSource.html")
            assertContains(":compileKotlin", ":compileTestKotlin", ":compileDeployKotlin")
        }

        project.build("compileDeployKotlin", "build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE", ":compileTestKotlin UP-TO-DATE", ":compileDeployKotlin UP-TO-DATE", ":compileJava UP-TO-DATE")
        }
    }

    @Test
    fun testSuppressWarnings() {
        val project = Project("suppressWarnings", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin")
            assertNotContains("""w: [^\r\n]*?\.kt""".toRegex())
        }
    }

    @Test
    fun testKotlinCustomDirectory() {
        Project("customSrcDir", GRADLE_VERSION).build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testKotlinExtraJavaSrc() {
        Project("additionalJavaSrc", GRADLE_VERSION).build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testLanguageVersion() {
        Project("languageVersion", GRADLE_VERSION).build("build") {
            assertFailed()
            assertContains("This type is sealed")
        }
    }
    @Test
    fun testJvmTarget() {
        Project("jvmTarget", GRADLE_VERSION).build("build") {
            assertFailed()
            assertContains("Unknown JVM target version: 1.7")
        }
    }

    @Test
    fun testCustomJdk() {
        Project("customJdk", GRADLE_VERSION).build("build") {
            assertFailed()
            assertContains("Unresolved reference: stream")
            assertNotContains("AutoCloseable")
        }
    }

    @Test
    fun testGradleSubplugin() {
        val project = Project("kotlinGradleSubplugin", GRADLE_VERSION)

        project.build("compileKotlin", "build") {
            assertSuccessful()
            assertContains("ExampleSubplugin loaded")
            assertContains("Project component registration: exampleValue")
            assertContains(":compileKotlin")
        }

        project.build("compileKotlin", "build") {
            assertSuccessful()
            assertContains("ExampleSubplugin loaded")
            assertNotContains("Project component registration: exampleValue")
            assertContains(":compileKotlin UP-TO-DATE")
        }
    }

    @Test
    fun testDestinationDirReferencedDuringEvaluation() {
        Project("destinationDirReferencedDuringEvaluation", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertContains("GreeterTest PASSED")
        }
    }

    @Test
    fun testAllOpenPlugin() {
        Project("allOpenSimple", GRADLE_VERSION).build("build") {
            assertSuccessful()

            val classesDir = File(project.projectDir, "build/classes/main")
            val openClass = File(classesDir, "test/OpenClass.class")
            val closedClass = File(classesDir, "test/ClosedClass.class")
            assertTrue(openClass.exists())
            assertTrue(closedClass.exists())

            checkBytecodeContains(openClass,
                    "public class test/OpenClass {",
                    "public method()V")

            checkBytecodeContains(closedClass,
                    "public final class test/ClosedClass {",
                    "public final method()V")
        }
    }

    @Test
    fun testKotlinSpringPlugin() {
        Project("allOpenSpring", GRADLE_VERSION).build("build") {
            assertSuccessful()

            val classesDir = File(project.projectDir, "build/classes/main")
            val openClass = File(classesDir, "test/OpenClass.class")
            val closedClass = File(classesDir, "test/ClosedClass.class")
            assertTrue(openClass.exists())
            assertTrue(closedClass.exists())

            checkBytecodeContains(openClass,
                    "public class test/OpenClass {",
                    "public method()V")

            checkBytecodeContains(closedClass,
                    "public final class test/ClosedClass {",
                    "public final method()V")
        }
    }

    @Test
    fun testKotlinJpaPlugin() {
        Project("noArgJpa", GRADLE_VERSION).build("build") {
            assertSuccessful()

            val classesDir = File(project.projectDir, "build/classes/main")

            fun checkClass(name: String) {
                val testClass = File(classesDir, "test/$name.class")
                assertTrue(testClass.exists())
                checkBytecodeContains(testClass, "public <init>()V")
            }

            checkClass("Test")
            checkClass("Test2")
        }
    }

    @Test
    fun testNoArgKt18668() {
        Project("noArgKt18668", GRADLE_VERSION).build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testSamWithReceiverSimple() {
        Project("samWithReceiverSimple", GRADLE_VERSION).build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testBuildDirLazyEvaluation() {
        val project = Project("kotlinProject", GRADLE_VERSION)
        project.setupWorkingDir()

        File(project.projectDir, "build").writeText("This file prevents Gradle from using 'build' as a directory")

        // Change the build directory in the end of the build script:
        val customBuildDirName = "customBuild"
        File(project.projectDir, "build.gradle").modify {
            it + "\nbuildDir = '$customBuildDirName'\n"
        }

        project.build("build") {
            assertSuccessful()
            assertFileExists("$customBuildDirName/classes")
        }
    }
}