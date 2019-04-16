/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubpluginsIT : BaseGradleIT() {
    @Test
    fun testGradleSubplugin() {
        val project = Project("kotlinGradleSubplugin")

        project.build("compileKotlin", "build") {
            assertSuccessful()
            assertContains("ExampleSubplugin loaded")
            assertContains("Project component registration: exampleValue")
            assertTasksExecuted(":compileKotlin")
        }

        project.build("compileKotlin", "build") {
            assertSuccessful()
            assertContains("ExampleSubplugin loaded")
            assertNotContains("Project component registration: exampleValue")
            assertTasksUpToDate(":compileKotlin")
        }
    }

    @Test
    fun testAllOpenPlugin() {
        Project("allOpenSimple").build("build") {
            assertSuccessful()

            val classesDir = File(project.projectDir, kotlinClassesDir())
            val openClass = File(classesDir, "test/OpenClass.class")
            val closedClass = File(classesDir, "test/ClosedClass.class")
            assertTrue(openClass.exists())
            assertTrue(closedClass.exists())

            checkBytecodeContains(
                openClass,
                "public class test/OpenClass {",
                "public method()V"
            )

            checkBytecodeContains(
                closedClass,
                "public final class test/ClosedClass {",
                "public final method()V"
            )
        }
    }

    @Test
    fun testKotlinSpringPlugin() {
        Project("allOpenSpring").build("build") {
            assertSuccessful()

            val classesDir = File(project.projectDir, kotlinClassesDir())
            val openClass = File(classesDir, "test/OpenClass.class")
            val closedClass = File(classesDir, "test/ClosedClass.class")
            assertTrue(openClass.exists())
            assertTrue(closedClass.exists())

            checkBytecodeContains(
                openClass,
                "public class test/OpenClass {",
                "public method()V"
            )

            checkBytecodeContains(
                closedClass,
                "public final class test/ClosedClass {",
                "public final method()V"
            )
        }
    }

    @Test
    fun testKotlinJpaPlugin() {
        Project("noArgJpa").build("build") {
            assertSuccessful()

            val classesDir = File(project.projectDir, kotlinClassesDir())

            fun checkClass(name: String) {
                val testClass = File(classesDir, "test/$name.class")
                assertTrue(testClass.exists())
                checkBytecodeContains(testClass, "public <init>()V")
            }

            checkClass("Test")
            checkClass("Test2")
        }
    }

    @Test
    fun testNoArgKt18668() {
        Project("noArgKt18668").build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testSamWithReceiverSimple() {
        Project("samWithReceiverSimple").build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testScripting() {
        Project("scripting").build("build") {
            assertSuccessful()
            assertCompiledKotlinSources(
                listOf("app/src/main/kotlin/world.greet.kts", "script-template/src/main/kotlin/GreetScriptTemplate.kt")
            )
            assertFileExists("${kotlinClassesDir("app", "main")}World_greet.class")
        }
    }

    @Test
    fun testScriptingCustomExtensionNonIncremental() {
        testScriptingCustomExtensionImpl(withIC = false)
    }

    @Test
    fun testScriptingCustomExtensionIncremental() {
        testScriptingCustomExtensionImpl(withIC = true)
    }

    private fun testScriptingCustomExtensionImpl(withIC: Boolean) {
        val project = Project("scriptingCustomExtension")
        val options = defaultBuildOptions().copy(incremental = withIC)

        project.setupWorkingDir()
        val bobGreet = project.projectFile("bob.greet")
        val aliceGreet = project.projectFile("alice.greet")
        val worldGreet = project.projectFile("world.greet")
        val greetScriptTemplateKt = project.projectFile("GreetScriptTemplate.kt")

        var isFailed = false
        project.build("build", options = options) {
            val classesDir = kotlinClassesDir("app", "main")
            if (project.testGradleVersionAtLeast("5.0")) {
                assertSuccessful()
                assertFileExists("${classesDir}World.class")
                assertFileExists("${classesDir}Alice.class")
                assertFileExists("${classesDir}Bob.class")

                if (withIC) {
                    // compile iterations are not logged when IC is disabled
                    assertCompiledKotlinSources(project.relativize(bobGreet, aliceGreet, worldGreet, greetScriptTemplateKt))
                }
            } else {
                assertFailed()
                val usedGradleVersion =
                    GradleVersion.version(
                        System.getProperty("kotlin.gradle.version.for.tests")
                            ?: project.gradleVersionRequirement.minVersion
                    )
                assertEquals(true, usedGradleVersion.version.substringBefore('.').toIntOrNull()?.let { it < 5 }, "Expected gradle version < 5, got ${usedGradleVersion.version}")
                assertContains("kotlin scripting plugin: incompatible Gradle version")
                isFailed = true
            }
        }

        if (!isFailed) {
            bobGreet.modify { it.replace("Bob", "Uncle Bob") }
            project.build("build", options = options) {
                assertSuccessful()

                if (withIC) {
                    assertCompiledKotlinSources(project.relativize(bobGreet))
                }
            }
        }
    }

    @Test
    fun testAllOpenFromNestedBuildscript() {
        Project("allOpenFromNestedBuildscript").build("build") {
            assertSuccessful()
            assertFileExists("${kotlinClassesDir(subproject = "a/b", sourceSet = "main")}MyClass.class")
            assertFileExists("${kotlinClassesDir(subproject = "a/b", sourceSet = "test")}MyTestClass.class")
        }
    }

    @Test
    fun testAllopenFromScript() {
        Project("allOpenFromScript").build("build") {
            assertSuccessful()
            assertFileExists("${kotlinClassesDir(sourceSet = "main")}MyClass.class")
            assertFileExists("${kotlinClassesDir(sourceSet = "test")}MyTestClass.class")
        }
    }
}