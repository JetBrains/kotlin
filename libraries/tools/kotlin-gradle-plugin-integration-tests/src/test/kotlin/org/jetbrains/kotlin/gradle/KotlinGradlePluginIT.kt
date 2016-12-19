/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.tasks.USING_EXPERIMENTAL_INCREMENTAL_MESSAGE
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KotlinGradleIT: BaseGradleIT() {

    companion object {
        private const val GRADLE_VERSION = "2.10"
    }

    @Test
    fun testCrossCompile() {
        val project = Project("kotlinJavaProject", GRADLE_VERSION)

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
        val project = Project("kotlinProject", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertFileExists("build/classes/main/META-INF/kotlinProject_main.kotlin_module")
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
        val project = Project("kotlinProject", GRADLE_VERSION)
        val VARIANT_CONSTANT = "ForTest"
        val userVariantArg = "-Duser.variant=$VARIANT_CONSTANT"
        val MEMORY_MAX_GROWTH_LIMIT_KB = 500
        val BUILD_COUNT = 15

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
            val usedMemory = (1..BUILD_COUNT).map { buildAndGetMemoryAfterBuild() }

            // ensure that the maximum of the used memory established after several first builds doesn't raise significantly in the subsequent builds
            val establishedMaximum = usedMemory.take(5).max()!!
            val totalMaximum = usedMemory.max()!!

            val maxGrowth = totalMaximum - establishedMaximum
            assertTrue(maxGrowth <= MEMORY_MAX_GROWTH_LIMIT_KB,
                    "Maximum used memory over series of builds growth $maxGrowth (from $establishedMaximum to $totalMaximum) kb > $MEMORY_MAX_GROWTH_LIMIT_KB kb")

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
        val debugProject = Project("simpleProject", GRADLE_VERSION, minLogLevel = LogLevel.DEBUG)
        debugProject.build("build") {
            assertContains("Forcing System.gc()")
        }

        val infoProject = Project("simpleProject", GRADLE_VERSION, minLogLevel = LogLevel.INFO)
        infoProject.build("clean", "build") {
            assertNotContains("Forcing System.gc()")
        }
    }

    @Test
    fun testKotlinClasspath() {
        Project("classpathTest", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testInternalTest() {
        Project("internalTest", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testMultiprojectPluginClasspath() {
        Project("multiprojectClassPathTest", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertReportExists("subproject")
            assertContains(":subproject:compileKotlin", ":subproject:compileTestKotlin")
            checkKotlinGradleBuildServices()
        }
    }

    @Test
    fun testSimpleMultiprojectIncremental() {
        fun Project.modify(body: Project.() -> Unit): Project {
            this.body()
            return this
        }

        val incremental = defaultBuildOptions().copy(incremental = true)

        Project("multiprojectWithDependency", GRADLE_VERSION).build("assemble", options = incremental) {
            assertSuccessful()
            assertReportExists("projA")
            assertContains(":projA:compileKotlin")
            assertNotContains("projA:compileKotlin UP-TO-DATE")
            assertReportExists("projB")
            assertContains(":projB:compileKotlin")
            assertNotContains("projB:compileKotlin UP-TO-DATE")
        }
        Project("multiprojectWithDependency", GRADLE_VERSION).modify {
            val oldSrc = File(this.projectDir, "projA/src/main/kotlin/a.kt")
            val newSrc = File(this.projectDir, "projA/src/main/kotlin/a.kt.new")
            assertTrue { oldSrc.exists() }
            assertTrue { newSrc.exists() }
            newSrc.copyTo(oldSrc, overwrite = true)
        }.build("assemble", options = incremental) {
            assertSuccessful()
            assertReportExists("projA")
            assertContains(":projA:compileKotlin")
            assertNotContains("projA:compileKotlin UP-TO-DATE")
            assertReportExists("projB")
            assertContains(":projB:compileKotlin")
            assertNotContains("projB:compileKotlin UP-TO-DATE")
        }
    }

    @Test
    fun testKotlinInJavaRoot() {
        Project("kotlinInJavaRoot", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertReportExists()
            assertContains(":compileKotlin", ":compileTestKotlin")
        }
    }

    @Test
    fun testIncrementalPropertyFromLocalPropertiesFile() {
        val project = Project("kotlinProject", GRADLE_VERSION)
        project.setupWorkingDir()

        val localPropertyFile = File(project.projectDir, "local.properties")
        localPropertyFile.writeText("kotlin.incremental=true")

        project.build("build") {
            assertContains(USING_EXPERIMENTAL_INCREMENTAL_MESSAGE)
        }
    }

    @Test
    fun testConvertJavaToKotlin() {
        val project = Project("convertBetweenJavaAndKotlin", GRADLE_VERSION)
        project.setupWorkingDir()

        val barKt = project.projectDir.getFileByName("Bar.kt")
        val barKtContent = barKt.readText()
        barKt.delete()

        project.build("build") {
            assertSuccessful()
        }

        val barClass = project.projectDir.getFileByName("Bar.class")
        val barClassTimestamp = barClass.lastModified()

        val barJava = project.projectDir.getFileByName("Bar.java")
        barJava.delete()
        barKt.writeText(barKtContent)

        project.build("build") {
            assertSuccessful()
            assertNotContains(":compileKotlin UP-TO-DATE", ":compileJava UP-TO-DATE")
            assertNotEquals(barClassTimestamp, barClass.lastModified(), "Bar.class timestamp hasn't been updated")
        }
    }

    @Test
    fun testWipeClassesDirectoryBetweenBuilds() {
        val project = Project("kotlinJavaProject", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
        }

        val javaOutputDir = File(project.projectDir, "build/classes")
        assert(javaOutputDir.isDirectory) { "Classes directory does not exist $javaOutputDir" }
        javaOutputDir.deleteRecursively()

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
        }
    }

    @Test
    fun testMoveClassToOtherModule() {
        val project = Project("moveClassToOtherModule", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertContains("Connected to daemon")
        }

        project.performModifications()
        project.build("build") {
            assertSuccessful()
            assertContains("Connected to daemon")
        }
    }

    @Test
    fun testKotlinBuiltins() {
        val project = Project("kotlinBuiltins", "3.2")

        project.build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testCustomCompilerFile() {
        val project = Project("customCompilerFile", GRADLE_VERSION)
        project.setupWorkingDir()

        // copy compiler embeddable to project dir using custom name
        val classpath = System.getProperty("java.class.path").split(File.pathSeparator)
        val kotlinEmbeddableJar = File(classpath.find { it.contains("kotlin-compiler-embeddable") })
        val compilerJar = File(project.projectDir, "compiler.jar")
        kotlinEmbeddableJar.copyTo(compilerJar)

        project.build("build") {
            assertSuccessful()
            assertContains("Kotlin compiler classpath: $compilerJar")
        }
    }
}