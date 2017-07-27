package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class KotlinGradlePluginMultiVersionIT : BaseMultiGradleVersionIT() {
    @Test
    fun testKaptProcessorPath() {
        val project = Project("kaptSimple", gradleVersion)

        project.build("build") {
            assertSuccessful()
            assertContains()
            assertContainsRegex("""-processorpath \S*.build.tmp.kapt.main.wrappers""".toRegex())
            assertFileExists("build/generated/source/kapt/main/example/TestClassGenerated.java")
            assertClassFilesNotContain(File(project.projectDir, "build/classes"), "ExampleSourceAnnotation")
        }
    }

    @Test
    fun testKt19179() {
        val project = Project("kt19179", gradleVersion, directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertFileExists("processor/build/tmp/kapt3/classes/main/META-INF/services/javax.annotation.processing.Processor")

            val processorJar = fileInWorkingDir("processor/build/libs/processor.jar")
            assert(processorJar.exists())

            val zip = ZipFile(processorJar)
            @Suppress("ConvertTryFinallyToUseCall")
            try {
                assert(zip.getEntry("META-INF/services/javax.annotation.processing.Processor") != null)
            } finally {
                zip.close()
            }

            assertTasksExecuted(listOf(
                    ":processor:kaptGenerateStubsKotlin", ":processor:kaptKotlin",
                    ":app:kaptGenerateStubsKotlin", ":app:kaptKotlin"))
        }

        project.projectDir.getFileByName("Test.kt").modify { text ->
            assert("SomeClass()" in text)
            text.replace("SomeClass()", "SomeClass(); val a = 5")
        }

        project.build("build") {
            assertSuccessful()
            assertTasksUpToDate(listOf(":processor:kaptGenerateStubsKotlin", ":processor:kaptKotlin", ":app:kaptKotlin"))
            assertTasksExecuted(listOf(":app:kaptGenerateStubsKotlin"))
        }

        project.projectDir.getFileByName("Test.kt").modify { text ->
            text + "\n\nfun t() {}"
        }

        project.build("build") {
            assertSuccessful()
            assertTasksUpToDate(listOf(":processor:kaptGenerateStubsKotlin", ":processor:kaptKotlin"))
            assertTasksExecuted(listOf(":app:kaptGenerateStubsKotlin", ":app:kaptKotlin"))
        }
    }

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

    @Test fun testApplyPluginFromBuildSrc() {
        val project = Project("kotlinProjectWithBuildSrc", gradleVersion)
        project.setupWorkingDir()
        File(project.projectDir, "buildSrc/build.gradle").modify { it.replace("\$kotlin_version", KOTLIN_VERSION) }
        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testInternalTest() {
        Project("internalTest", gradleVersion).build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }
}