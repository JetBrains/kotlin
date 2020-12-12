/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class SubpluginsIT : BaseGradleIT() {

    override fun defaultBuildOptions(): BuildOptions {
        return super.defaultBuildOptions().copy(warningMode = WarningMode.Summary)
    }

    @Test
    fun testGradleSubplugin() {
        val project = Project("kotlinGradleSubplugin")

        project.build("compileKotlin", "build") {
            assertSuccessful()
            assertContains("ExampleSubplugin loaded")
            assertContains("ExampleLegacySubplugin loaded")
            assertContains("Project component registration: exampleValue")
            assertContains("Project component registration: exampleLegacyValue")
            assertTasksExecuted(":compileKotlin")
        }

        project.build("compileKotlin", "build") {
            assertSuccessful()
            assertContains("ExampleSubplugin loaded")
            assertContains("ExampleLegacySubplugin loaded")
            assertNotContains("Project component registration: exampleValue")
            assertNotContains("Project component registration: exampleLegacyValue")
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
            assertSuccessful()
            assertFileExists("${classesDir}World.class")
            assertFileExists("${classesDir}Alice.class")
            assertFileExists("${classesDir}Bob.class")

            if (withIC) {
                // compile iterations are not logged when IC is disabled
                assertCompiledKotlinSources(project.relativize(bobGreet, aliceGreet, worldGreet, greetScriptTemplateKt))
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

    @Test
    fun testKotlinVersionDowngradeInSupbrojectKt39809() = with(Project("android-dagger", directoryPrefix = "kapt2")) {
        setupWorkingDir()

        gradleBuildScript("app").modify {
            """
                buildscript {
                	repositories {
                		mavenCentral()
                	}
                	dependencies {
                		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
                	}
                }
                
                $it
            """.trimIndent()
        }
        build(
            ":app:compileDebugKotlin",
            options = defaultBuildOptions().copy(
                androidGradlePluginVersion = AGPVersion.v3_4_1,
                androidHome = KotlinTestUtils.findAndroidSdk()
            )
        ) {
            assertSuccessful()
        }
    }

    @Test
    fun testKotlinVersionDowngradeWithNewerSubpluginsKt39809() = with(Project("multiprojectWithDependency")) {
        setupWorkingDir()

        val subprojectBuildGradle = projectDir.resolve("projA/build.gradle")
        val originalScript = subprojectBuildGradle.readText()

        listOf("allopen", "noarg", "sam-with-receiver", "serialization").forEach { plugin ->
            projectDir.resolve("projA/build.gradle").modify {
                """
                    buildscript {
                        repositories {
                            mavenLocal()
                            mavenCentral()
                        }
                        dependencies {
                            classpath("org.jetbrains.kotlin:kotlin-$plugin:${defaultBuildOptions().kotlinVersion}")
                        }
                    }
                    
                    apply plugin: "org.jetbrains.kotlin.plugin.${plugin.replace("-", ".")}"
                    
                    $originalScript
                """.trimIndent()
            }
            build(":projA:compileKotlin", options = defaultBuildOptions().copy(kotlinVersion = "1.3.72")) {
                assertFailed()
                assertContains(
                    "This version of the kotlin-$plugin Gradle plugin is built for a newer Kotlin version. " +
                            "Please use an older version of kotlin-$plugin or upgrade the Kotlin Gradle plugin version to make them match."
                )
            }
        }
    }
}