package org.jetbrains.kotlin.gradle

import org.junit.Test

class KotlinGradleIncrementalIT: BaseGradleIT() {

    @Test
    fun testIncrementalKotlinOnlyCompile() {
        val project = Project("kotlinProject", "2.4")

        project.build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
            assertCompiledKotlinSources("src/main/kotlin/helloWorld.kt", "src/test/kotlin/tests.kt")
        }

        project.modify()

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin", ":compileTestKotlin")
            assertCompiledKotlinSources("src/main/kotlin/helloWorld.kt")
        }
    }
}
