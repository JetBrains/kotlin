package org.jetbrains.kotlin.gradle

import org.junit.Test
import org.jetbrains.kotlin.gradle.BaseGradleIT.Project

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
}