package org.jetbrains.kotlin.gradle

import org.junit.Test
import org.junit.Ignore
import org.jetbrains.kotlin.gradle.BaseGradleIT.Project

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

    Ignore fun testKotlinExtraJavaSrc() {
        Project("additionalJavaSrc", "1.6").build("build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
        }
    }
}