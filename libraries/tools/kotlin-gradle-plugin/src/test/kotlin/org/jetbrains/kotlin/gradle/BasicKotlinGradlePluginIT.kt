package org.jetbrains.kotlin.gradle

import org.junit.Test
import org.jetbrains.kotlin.gradle.BaseGradleIT.Project
import org.gradle.api.logging.LogLevel
import org.junit.Ignore

class SimpleKotlinGradleIT : BaseGradleIT() {

    @Test
    fun testSimpleCompile() {
        val project = Project("simpleProject", "1.12")

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
    fun testSuppressWarningsAndVersionInVerboseMode() {
        val project = Project("suppressWarningsAndVersion", "1.6")

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin", "i: Kotlin Compiler version", "v: Using Kotlin home directory")
            assertNotContains("w:")
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
            assertNotContains("w:")
        }
    }

    @Test
    fun testSuppressWarningsAndVersionInNonVerboseMode() {
        val project = Project("suppressWarningsAndVersion", "1.6", minLogLevel = LogLevel.INFO)

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin", "i: Kotlin Compiler version")
            assertNotContains("w:", "v:")
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
            assertNotContains("w:", "v:")
        }
    }

    @Test
    fun testKotlinCustomDirectory() {
        Project("customSrcDir", "1.6").build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testKotlinCustomModuleName() {
        Project("moduleNameCustom", "1.6").build("build") {
            assertSuccessful()
            assertContains("args.moduleName = myTestName")
        }
    }

    @Test
    fun testKotlinDefaultModuleName() {
        Project("moduleNameDefault", "1.6").build("build") {
            assertSuccessful()
            assertContains("args.moduleName = moduleNameDefault-compileKotlin")
        }
    }

    @Test
    fun testAdvancedOptions() {
        Project("advancedOptions", "1.6").build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testKotlinExtraJavaSrc() {
        Project("additionalJavaSrc", "1.6").build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testGradleSubplugin() {
        val project = Project("kotlinGradleSubplugin", "1.6")

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
}