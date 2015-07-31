package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.BaseGradleIT.Project
import org.junit.Test

class KotlinGradleIT: BaseGradleIT() {

    Test fun testCrossCompile() {
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

    Test fun testKotlinOnlyCompile() {
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

    Test fun testKotlinOnlyDaemonMemory() {
        val project = Project("kotlinProject", "2.4", minLogLevel = LogLevel.DEBUG)

        project.stopDaemon {}

        project.build("build", options = BaseGradleIT.BuildOptions(withDaemon = true)) {
            assertSuccessful()
        }

        for (i in 1..3)
            project.build("build", options = BaseGradleIT.BuildOptions(withDaemon = true)) {
                assertSuccessful()
                val matches = "\\[PERF\\] Used memory after build: (\\d+) kb \\(([+-]?\\d+) kb\\)".toRegex().match(output)
                assert(matches != null && matches.groups.size() == 3, "Used memory after build is not reported by plugin")
                val reportedGrowth = matches!!.groups.get(2)!!.value.toInt()
                assert(reportedGrowth <= 1000, "Used memory growth $reportedGrowth > 1000")
            }

        project.stopDaemon {
            assertSuccessful()
        }
    }

    Test fun testKotlinClasspath() {
        Project("classpathTest", "1.6").build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }

    Test fun testMultiprojectPluginClasspath() {
        Project("multiprojectClassPathTest", "1.6").build("build") {
            assertSuccessful()
            assertReportExists("subproject")
            assertContains(":subproject:compileKotlin", ":subproject:compileTestKotlin")
        }
    }

    Test fun testKotlinInJavaRoot() {
        Project("kotlinInJavaRoot", "1.6").build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }

    Test fun testKaptSimple() {
        Project("kaptSimple", "1.12").build("build") {
            assertSuccessful()
            assertContains("kapt: Class file stubs are not used")
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/TestClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
        }
    }

    Test fun testKaptStubs() {
        Project("kaptStubs", "1.12").build("build") {
            assertSuccessful()
            assertContains("kapt: Using class file stubs")
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/tmp/kapt/main/wrappers/annotations.main.txt")
            assertFileExists("build/generated/source/kapt/main/TestClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
        }
    }

    Test fun testKaptArguments() {
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

    Test fun testKaptInheritedAnnotations() {
        Project("kaptInheritedAnnotations", "1.12").build("build") {
            assertSuccessful()
            assertFileExists("build/generated/source/kapt/main/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt/main/AncestorClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/AncestorClassGenerated.class")
        }
    }
}