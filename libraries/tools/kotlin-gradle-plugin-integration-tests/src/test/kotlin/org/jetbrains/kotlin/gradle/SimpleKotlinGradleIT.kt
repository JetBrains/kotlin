package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class SimpleKotlinGradleIT : BaseGradleIT() {

    @Test
    fun testSimpleCompile() {
        val project = Project("simpleProject")

        project.build("compileDeployKotlin", "build") {
            assertSuccessful()
            assertContains("Finished executing kotlin compiler using daemon strategy")
            assertTrue {
                fileInWorkingDir("build/reports/tests/classes/demo.TestSource.html").exists() ||
                        fileInWorkingDir("build/reports/tests/test/classes/demo.TestSource.html").exists()
            }
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
    fun testSuppressWarnings() {
        val project = Project("suppressWarnings")

        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin")
            assertNotContains("""w: [^\r\n]*?\.kt""".toRegex())
        }
    }

    @Test
    fun testKotlinCustomDirectory() {
        Project("customSrcDir").build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testKotlinExtraJavaSrc() {
        Project("additionalJavaSrc").build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testLanguageVersion() {
        Project("languageVersion").build("build") {
            assertFailed()
            assertContains("This type is sealed")
        }
    }

    @Test
    fun testJvmTarget() {
        Project("jvmTarget").build("build") {
            assertFailed()
            assertContains("Unknown JVM target version: 1.7")
        }
    }

    @Test
    fun testCustomJdk() {
        Project("customJdk").build("build") {
            assertFailed()
            assertContains("Unresolved reference: stream")
            assertNotContains("AutoCloseable")
        }
    }

    @Test
    fun testGradleSubplugin() {
        val project = Project("kotlinGradleSubplugin")

        project.build("compileKotlin", "build") {
            assertSuccessful()
            assertContains("ExampleSubplugin loaded")
            assertContains("Project component registration: exampleValue")
            assertTasksExecuted(":compileKotlin")
        }

        project.build("compileKotlin", "build") {
            assertSuccessful()
            assertContains("ExampleSubplugin loaded")
            assertNotContains("Project component registration: exampleValue")
            assertTasksUpToDate(":compileKotlin")
        }
    }

    @Test
    fun testDestinationDirReferencedDuringEvaluation() {
        Project("destinationDirReferencedDuringEvaluation").build("build") {
            assertSuccessful()
            assertContains("GreeterTest PASSED")
        }
    }

    @Test
    fun testAllOpenPlugin() {
        Project("allOpenSimple").build("build") {
            assertSuccessful()

            val classesDir = File(project.projectDir, kotlinClassesDir())
            val openClass = File(classesDir, "test/OpenClass.class")
            val closedClass = File(classesDir, "test/ClosedClass.class")
            assertTrue(openClass.exists())
            assertTrue(closedClass.exists())

            checkBytecodeContains(
                openClass,
                "public class test/OpenClass {",
                "public method()V"
            )

            checkBytecodeContains(
                closedClass,
                "public final class test/ClosedClass {",
                "public final method()V"
            )
        }
    }

    @Test
    fun testKotlinSpringPlugin() {
        Project("allOpenSpring").build("build") {
            assertSuccessful()

            val classesDir = File(project.projectDir, kotlinClassesDir())
            val openClass = File(classesDir, "test/OpenClass.class")
            val closedClass = File(classesDir, "test/ClosedClass.class")
            assertTrue(openClass.exists())
            assertTrue(closedClass.exists())

            checkBytecodeContains(
                openClass,
                "public class test/OpenClass {",
                "public method()V"
            )

            checkBytecodeContains(
                closedClass,
                "public final class test/ClosedClass {",
                "public final method()V"
            )
        }
    }

    @Test
    fun testKotlinJpaPlugin() {
        Project("noArgJpa").build("build") {
            assertSuccessful()

            val classesDir = File(project.projectDir, kotlinClassesDir())

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
        Project("noArgKt18668").build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testSamWithReceiverSimple() {
        Project("samWithReceiverSimple").build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testBuildDirLazyEvaluation() {
        val project = Project("kotlinProject")
        project.setupWorkingDir()

        // Change the build directory in the end of the build script:
        val customBuildDirName = "customBuild"
        File(project.projectDir, "build.gradle").modify {
            it + "\nbuildDir = '$customBuildDirName'\n"
        }

        project.build("build") {
            assertSuccessful()
            assertFileExists("$customBuildDirName/classes")
            assertNoSuchFile("build")
        }
    }
}