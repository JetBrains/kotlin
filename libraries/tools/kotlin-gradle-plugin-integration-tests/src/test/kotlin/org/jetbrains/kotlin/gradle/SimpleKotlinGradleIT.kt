package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class SimpleKotlinGradleIT : BaseGradleIT() {

    override fun defaultBuildOptions(): BuildOptions {
        return super.defaultBuildOptions().copy(warningMode = WarningMode.Summary)
    }

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
            assertContains("'break' and 'continue' are not allowed in 'when' statements")
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
    fun testModuleName() {
        Project("moduleName").build("build") {
            assertSuccessful()
            assertFileExists("build/classes/kotlin/main/META-INF/FLAG.kotlin_module")
            assertNoSuchFile("build/classes/kotlin/main/META-INF/moduleName.kotlin_module")
            assertNotContains("Argument -module-name is passed multiple times")
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
    fun testGroovyInterop() {
        Project("groovyInterop").build("build") {
            assertSuccessful()
            assertTasksExecuted(":test")
            assertContains("GroovyInteropTest PASSED")
        }
    }

    //Proguard corrupts RuntimeInvisibleParameterAnnotations/RuntimeVisibleParameterAnnotations tables:
    // https://sourceforge.net/p/proguard/bugs/735/
    @Test
    fun testInteropWithProguarded() {
        Project("interopWithProguarded").build("build") {
            assertSuccessful()
            assertTasksExecuted(":test")
            assertContains("InteropWithProguardedTest PASSED")
        }
    }

    @Test
    fun testScalaInterop() {
        Project("scalaInterop").build("build") {
            assertTasksExecuted(":test")
            assertContains("ScalaInteropTest PASSED")
            assertSuccessful()
        }
    }

    // Should not produce kotlin-stdlib version conflict on Kotlin files compilation in 'buildSrc' module"
    @Test
    internal fun testKotlinDslStdlibVersionConflict() {
        val project = Project(projectName = "buildSrcUsingKotlinCompilationAndKotlinPlugin")
        listOf(
            "compileClasspath",
            "compileOnly",
            "runtimeClasspath"
        ).forEach { configuration ->
            project.build("-p", "buildSrc", "dependencies", "--configuration", configuration) {
                assertSuccessful()
                listOf(
                    "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions().kotlinVersion}",
                    "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${defaultBuildOptions().kotlinVersion}",
                    "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${defaultBuildOptions().kotlinVersion}",
                    "org.jetbrains.kotlin:kotlin-stdlib-common:${defaultBuildOptions().kotlinVersion}",
                    "org.jetbrains.kotlin:kotlin-reflect:${defaultBuildOptions().kotlinVersion}",
                    "org.jetbrains.kotlin:kotlin-script-runtime:${defaultBuildOptions().kotlinVersion}"
                ).forEach {
                    assertNotContains(it)
                }
            }
        }

        project.build("assemble") { assertSuccessful() }
    }
}
