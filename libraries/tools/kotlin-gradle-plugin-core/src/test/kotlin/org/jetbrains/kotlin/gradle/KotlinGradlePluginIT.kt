package org.jetbrains.kotlin.gradle

import org.junit.Test
import org.jetbrains.kotlin.gradle.BaseGradleIT.Project
import org.gradle.api.logging.LogLevel
import org.junit.Ignore

Ignore("temp")
class BasicKotlinGradleIT : BaseGradleIT() {

    Test fun testSimpleCompile() {
        val project = Project("simpleProject", "1.12")

        project.build("compileDeployKotlin", "build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
            assertReportExists("build/reports/tests/classes/demo.TestSource.html")
            assertContains(":compileKotlin", ":compileTestKotlin", ":compileDeployKotlin")
        }

        project.build("compileDeployKotlin", "build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE", ":compileTestKotlin UP-TO-DATE", ":compileDeployKotlin UP-TO-DATE", ":compileJava UP-TO-DATE")
        }
    }

    Test fun testSuppressWarningsAndVersionInVerboseMode() {
        val project = Project("suppressWarningsAndVersion", "1.6")

        project.build("build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
            assertContains(":compileKotlin", "i: Kotlin Compiler version", "v: Using Kotlin home directory")
            assertNotContains("w:")
        }

        project.build("build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
            assertNotContains("w:")
        }
    }

    Test fun testSuppressWarningsAndVersionInNonVerboseMode() {
        val project = Project("suppressWarningsAndVersion", "1.6", minLogLevel = LogLevel.INFO)

        project.build("build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
            assertContains(":compileKotlin", "i: Kotlin Compiler version")
            assertNotContains("w:", "v:")
        }

        project.build("build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
            assertNotContains("w:", "v:")
        }
    }

    Test fun testKotlinCustomDirectory() {
        Project("customSrcDir", "1.6").build("build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
        }
    }

    Test fun testAdvancedOptions() {
        Project("advancedOptions", "1.6").build("build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
        }
    }

    Test fun testSimpleKDoc() {
        Project("kdocProject", "1.6").build("kdoc", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
            assertReportExists("build/docs/kdoc/demo/MyClass.html")
            assertContains(":kdoc", "Generating kdoc to")
        }
    }

    Test fun testKotlinExtraJavaSrc() {
        Project("additionalJavaSrc", "1.6").build("build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
        }
    }
}