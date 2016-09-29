package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
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
            assertNotContains("w:")
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
}