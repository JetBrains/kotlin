package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.plugin.CleanUpBuildListener
import org.jetbrains.kotlin.gradle.tasks.USING_EXPERIMENTAL_INCREMENTAL_MESSAGE
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
        val MEMORY_GROWTH_LIMIT_KB = 200

        fun exitTestDaemon() {
            project.build(userVariantArg, "exit", options = BaseGradleIT.BuildOptions(withDaemon = true)) {
                assertFailed()
                assertContains("The daemon has exited normally or was terminated in response to a user interrupt.")
            }
        }

        fun buildAndGetMemoryAfterBuild(): Int {
            var reportedMemory: Int? = null

            project.build(userVariantArg, "clean", "build", options = BaseGradleIT.BuildOptions(withDaemon = true)) {
                assertSuccessful()
                val matches = "\\[PERF\\] Used memory after build: (\\d+) kb \\(difference since build start: ([+-]?\\d+) kb\\)".toRegex().find(output)
                assert(matches != null && matches.groups.size == 3) { "Used memory after build is not reported by plugin" }
                reportedMemory = matches!!.groupValues[1].toInt()
            }

            return reportedMemory!!
        }

        exitTestDaemon()

        try {
            val startMemory = buildAndGetMemoryAfterBuild()
            val endMemory = (1..10).map { buildAndGetMemoryAfterBuild() }.last()
            val growth = endMemory - startMemory
            assert(growth <= MEMORY_GROWTH_LIMIT_KB) { "Used memory growth $growth kb > $MEMORY_GROWTH_LIMIT_KB kb" }

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

        val incremental = defaultBuildOptions().copy(incremental = true)

        Project("multiprojectWithDependency", "1.6").build("assemble", options = incremental) {
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
        }.build("assemble", options = incremental) {
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

    @Test
    fun testIncrementalPropertyFromLocalPropertiesFile() {
        val project = Project("kotlinProject", "2.10")
        project.setupWorkingDir()

        val localPropertyFile = File(project.projectDir, "local.properties")
        localPropertyFile.writeText("kotlin.incremental=true")

        project.build("build") {
            assertContains(USING_EXPERIMENTAL_INCREMENTAL_MESSAGE)
        }
    }
}