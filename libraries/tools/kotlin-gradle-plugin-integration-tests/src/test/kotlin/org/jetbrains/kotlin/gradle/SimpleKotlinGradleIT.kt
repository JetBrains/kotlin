package org.jetbrains.kotlin.gradle

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
    fun testDestinationDirReferencedDuringEvaluation() {
        Project("destinationDirReferencedDuringEvaluation").build("build") {
            assertSuccessful()
            assertContains("GreeterTest PASSED")
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

    @Test
    fun testGroovyTraitsWithFields() {
        Project("groovyTraitsWithFields").build("build") {
            assertSuccessful()
        }
    }
}
