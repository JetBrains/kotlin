package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.plugin.CleanUpBuildListener
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class KotlinGradleIT: BaseGradleIT() {

    @Test
    fun testCrossCompile() {
        val project = Project("kotlinJavaProject", "1.6")

        project.build("compileDeployKotlin", "build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin", ":compileDeployKotlin")
        }

        project.build("compileDeployKotlin", "build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE", ":compileTestKotlin UP-TO-DATE", ":compileDeployKotlin UP-TO-DATE", ":compileJava UP-TO-DATE")
        }
    }

    @Test
    fun testKotlinOnlyCompile() {
        val project = Project("kotlinProject", "1.6")

        project.build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE", ":compileTestKotlin UP-TO-DATE")
        }
    }

    // For corresponding documentation, see https://docs.gradle.org/current/userguide/gradle_daemon.html
    // Setting user.variant to different value implies a new daemon process will be created.
    // In order to stop daemon process, special exit task is used ( System.exit(0) ).
    @Test
    fun testKotlinOnlyDaemonMemory() {
        val project = Project("kotlinProject", "2.4")
        val VARIANT_CONSTANT = "ForTest"
        val userVariantArg = "-Duser.variant=$VARIANT_CONSTANT"

        fun exitTestDaemon() {
            project.build(userVariantArg, "exit", options = BaseGradleIT.BuildOptions(withDaemon = true)) {
                assertFailed()
                assertContains("The daemon has exited normally or was terminated in response to a user interrupt.")
            }
        }

        exitTestDaemon()

        try {
            // build to "warm up" the daemon, if it is not started yet
            project.build(userVariantArg, "build", options = BaseGradleIT.BuildOptions(withDaemon = true)) {
                assertSuccessful()
            }

            for (i in 1..3) {
                project.build(userVariantArg, "clean", "build", options = BaseGradleIT.BuildOptions(withDaemon = true)) {
                    assertSuccessful()
                    val matches = "\\[PERF\\] Used memory after build: (\\d+) kb \\(difference since build start: ([+-]?\\d+) kb\\)".toRegex().find(output)
                    assert(matches != null && matches.groups.size == 3) { "Used memory after build is not reported by plugin on attempt $i" }
                    val reportedGrowth = matches!!.groups.get(2)!!.value.removePrefix("+").toInt()
                    val expectedGrowthLimit = 2500
                    assert(reportedGrowth <= expectedGrowthLimit) { "Used memory growth $reportedGrowth > $expectedGrowthLimit" }
                }
            }

            // testing that nothing remains locked by daemon, see KT-9440
            project.build(userVariantArg, "clean", options = BaseGradleIT.BuildOptions(withDaemon = true)) {
                assertSuccessful()
            }
        }
        finally {
            exitTestDaemon()
        }
    }

    @Test
    fun testLogLevelForceGC() {
        val debugProject = Project("simpleProject", "1.12", minLogLevel = LogLevel.DEBUG)
        debugProject.build("build") {
            assertContains(CleanUpBuildListener.FORCE_SYSTEM_GC_MESSAGE)
        }

        val infoProject = Project("simpleProject", "1.12", minLogLevel = LogLevel.INFO)
        infoProject.build("clean", "build") {
            assertNotContains(CleanUpBuildListener.FORCE_SYSTEM_GC_MESSAGE)
        }
    }

    @Test
    fun testKotlinClasspath() {
        Project("classpathTest", "1.6").build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testInternalTest() {
        Project("internalTest", "1.6").build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testMultiprojectPluginClasspath() {
        Project("multiprojectClassPathTest", "1.6").build("build") {
            assertSuccessful()
            assertReportExists("subproject")
            assertContains(":subproject:compileKotlin", ":subproject:compileTestKotlin")
        }
    }

    @Test
    fun testSimpleMultiprojectIncremental() {

        fun Project.modify(body: Project.() -> Unit): Project {
            this.body()
            return this
        }

        Project("multiprojectWithDependency", "1.6").build("-PincrementalOption=true", "assemble") {
            assertSuccessful()
            assertReportExists("projA")
            assertContains(":projA:compileKotlin")
            assertNotContains("projA:compileKotlin UP-TO-DATE")
            assertReportExists("projB")
            assertContains(":projB:compileKotlin")
            assertNotContains("projB:compileKotlin UP-TO-DATE")
        }
        Project("multiprojectWithDependency", "1.6").modify {
            val oldSrc = File(this.projectDir, "projA/src/main/kotlin/a.kt")
            val newSrc = File(this.projectDir, "projA/src/main/kotlin/a.kt.new")
            assertTrue { oldSrc.exists() }
            assertTrue { newSrc.exists() }
            newSrc.copyTo(oldSrc, overwrite = true)
        }.build("-PincrementalOption=true", "assemble") {
            assertSuccessful()
            assertReportExists("projA")
            assertContains(":projA:compileKotlin")
            assertContains("[KOTLIN] is incremental == true")
            assertNotContains("projA:compileKotlin UP-TO-DATE")
            assertReportExists("projB")
            assertContains(":projB:compileKotlin")
            assertNotContains("projB:compileKotlin UP-TO-DATE")
        }
    }

    @Test
    fun testKotlinInJavaRoot() {
        Project("kotlinInJavaRoot", "1.6").build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testKaptSimple() {
        val project = Project("kaptSimple", "1.12")

        project.build("build") {
            assertSuccessful()
            assertContains("kapt: Class file stubs are not used")
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/TestClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
        }

        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testKaptStubs() {
        val project = Project("kaptStubs", "1.12")

        project.build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/TestClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
        }

        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testKaptStubsIncrementalBuild() {
        val project = Project("kaptStubs", "1.12")

        project.build("build") {
            assertSuccessful()

            // Modify the Kotlin source file somehow
            val someJavaFile = fileInWorkingDir("src/main/java/test.kt")
            someJavaFile.appendText(" ")
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertNotContains(":compileJava UP-TO-DATE")
        }
    }

    @Test
    fun testKaptArguments() {
        Project("kaptArguments", "1.12").build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/TestClassCustomized.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassCustomized.class")
        }
    }

    @Test
    fun testKaptInheritedAnnotations() {
        Project("kaptInheritedAnnotations", "1.12").build("build") {
            assertSuccessful()
            assertFileExists("build/generated/source/kapt/main/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/AncestorClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/AncestorClassGenerated.class")
        }
    }

    @Test
    fun testKaptOutputKotlinCode() {
        Project("kaptOutputKotlinCode", "1.12").build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/TestClassCustomized.java")
            assertFileExists("build/tmp/kapt/main/kotlinGenerated/TestClass.kt")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassCustomized.class")
        }
    }
}