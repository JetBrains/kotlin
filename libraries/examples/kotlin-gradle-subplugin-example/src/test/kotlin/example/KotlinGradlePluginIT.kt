package org.jetbrains.kotlin.gradle

import org.junit.Test
import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.BaseGradleIT.Project

class BasicKotlinGradleIT : BaseGradleIT() {

    Test fun testSimpleCompile() {
        val project = Project("simpleProject", "1.12")

        project.build("compileKotlin", "build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
            assertContains("/src/main/kotlin/helloWorld.kt")
            assertContains("ExampleSubplugin loaded")
            assertContains("Project component registration: exampleValue")
            assertContains(":compileKotlin")
        }

        project.build("compileKotlin", "build", "-Pkotlin.gradle.plugin.version=0.1-SNAPSHOT") {
            assertSuccessful()
            assertContains("ExampleSubplugin loaded")
            assertNotContains("Project component registration: exampleValue")
            assertContains(":compileKotlin UP-TO-DATE")
        }
    }
}