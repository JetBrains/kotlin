package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class KotlinGradlePluginMultiVersionIT : BaseMultiGradleVersionIT() {
    @Test
    fun testJavaIcCompatibility() {
        val version = gradleVersion.split(".").map(String::toInt)
        val expectIncrementalCompilation = version.let { (major, minor) -> major > 2 || major == 2 && minor >= 14 }
        val expectVerboseIncrementalLogs = version.let { (major, minor) -> major < 3 || major == 3 && minor < 4 }

        val project = Project("kotlinJavaProject", gradleVersion)
        project.setupWorkingDir()

        val buildScript = File(project.projectDir, "build.gradle")

        buildScript.modify { "$it\n" + "compileJava.options.incremental = true" }
        project.build("build") {
            assertSuccessful()
        }

        // Then modify a Java source and check that compileJava is incremental:
        File(project.projectDir, "src/main/java/demo/HelloWorld.java").modify { "$it\n" + "class NewClass { }" }
        project.build("build") {
            assertSuccessful()
            if (expectIncrementalCompilation && expectVerboseIncrementalLogs)
                assertContains("Incremental compilation")
            if (expectIncrementalCompilation)
                assertNotContains("not incremental") else
                assertContains("not incremental")
        }

        // Then modify a Kotlin source and check that Gradle sees that Java is not up-to-date:
        File(project.projectDir, "src/main/kotlin/helloWorld.kt").modify {
            it.trim('\r', '\n').trimEnd('}') + "\nval z: Int = 0 }"
        }
        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin")
            assertNotContains(":compileJava UP-TO-DATE")
            if (expectIncrementalCompilation)
                assertNotContains("not incremental") else
                assertContains("not incremental")
            assertNotContains("None of the classes needs to be compiled!")
        }
    }
}