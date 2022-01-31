/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.testFramework.TestDataPath
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internals.KOTLIN_12X_MPP_DEPRECATION_WARNING
import org.jetbrains.kotlin.gradle.plugin.EXPECTED_BY_CONFIG_NAME
import org.jetbrains.kotlin.gradle.plugin.IMPLEMENT_CONFIG_NAME
import org.jetbrains.kotlin.gradle.plugin.IMPLEMENT_DEPRECATION_WARNING
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import java.io.File
import kotlin.test.assertTrue

@TestDataPath("\$CONTENT_ROOT/resources")
@MppGradlePluginTests
class MultiplatformGradleIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)

    @GradleTestWithOsCondition
    fun testMultiplatformCompile(gradleVersion: GradleVersion) {
        with(project("multiplatformProject", gradleVersion)) {
            build("build") {
                assertOutputContains(KOTLIN_12X_MPP_DEPRECATION_WARNING)

                assertTasksExecuted(
                    ":lib:compileKotlinCommon",
                    ":lib:compileTestKotlinCommon",
                    ":libJvm:compileKotlin",
                    ":libJvm:compileTestKotlin",
                    ":libJs:compileKotlin2Js",
                    ":libJs:compileTestKotlin2Js"
                )
                assertFileInProjectExists("lib/build/classes/kotlin/main/foo/PlatformClass.kotlin_metadata")
                assertFileInProjectExists("lib/build/classes/kotlin/test/foo/PlatformTest.kotlin_metadata")
                assertFileInProjectExists("libJvm/build/classes/kotlin/main/foo/PlatformClass.class")
                assertFileInProjectExists("libJvm/build/classes/kotlin/test/foo/PlatformTest.class")
                assertFileInProjectExists("libJs/build/classes/kotlin/main/libJs.js")
                assertFileInProjectExists("libJs/build/classes/kotlin/test/libJs_test.js")
            }

            projectDir.resolve("gradle.properties").appendText("\nkotlin.internal.mpp12x.deprecation.suppress=true")
            build {
                assertOutputDoesNotContain(KOTLIN_12X_MPP_DEPRECATION_WARNING)
            }
        }
    }

    @GradleTestWithOsCondition
    fun testDeprecatedImplementWarning(gradleVersion: GradleVersion) {
        with(project("multiplatformProject", gradleVersion)) {
            build("build") {
                assertOutputDoesNotContain(IMPLEMENT_DEPRECATION_WARNING)
            }

            projectDir.walk().filter { it.name == "build.gradle" }.forEach { buildGradle ->
                buildGradle.modify { it.replace(EXPECTED_BY_CONFIG_NAME, IMPLEMENT_CONFIG_NAME) }
            }

            build("build") {
                assertOutputContains(IMPLEMENT_DEPRECATION_WARNING)
            }
        }
    }

    @GradleTestWithOsCondition
    fun testCommonKotlinOptions(gradleVersion: GradleVersion) {
        with(project("multiplatformProject", gradleVersion)) {
            File(projectDir, "lib/build.gradle").appendText(
                "\ncompileKotlinCommon.kotlinOptions.freeCompilerArgs = ['-Xno-inline']" +
                        "\ncompileKotlinCommon.kotlinOptions.suppressWarnings = true"
            )

            build("build") {
                assertOutputContains("-Xno-inline")
                assertOutputContains("-nowarn")
            }
        }
    }

    @GradleTestWithOsCondition
    fun testSubprojectWithAnotherClassLoader(gradleVersion: GradleVersion) {
        with(project("multiplatformProject", gradleVersion)) {
            // Make sure there is a plugin applied with the plugins DSL, so that Gradle loads the
            // plugins separately for the subproject, with a different class loader:
            File(projectDir, "libJs/build.gradle").modify {
                "plugins { id 'com.moowork.node' version '1.0.1' }" + "\n" + it
            }

            // Remove the root project buildscript dependency, needed for the same purpose:
            File(projectDir, "build.gradle").modify {
                it.replace("classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:\$kotlin_version\"", "")
                    .apply { assert(!equals(it)) }
            }

            // Instead, add the dependencies directly to the subprojects buildscripts:
            listOf("lib", "libJvm", "libJs").forEach { subDirectory ->
                File(projectDir, "$subDirectory/build.gradle").modify {
                    """
                    buildscript {
                        repositories { 
                             mavenLocal();
                             maven { url = uri("https://jcenter.bintray.com/") }
                        }
                        dependencies {
                            classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version"
                        }
                    }
                    """.trimIndent() + "\n" + it
                }
            }

            build("build")
        }
    }

    // todo: also make incremental compilation test
    @GradleTestWithOsCondition
    fun testIncrementalBuild(gradleVersion: GradleVersion): Unit = with(project("multiplatformProject", gradleVersion)) {
        val compileCommonTask = ":lib:compileKotlinCommon"
        val compileJsTask = ":libJs:compileKotlin2Js"
        val compileJvmTask = ":libJvm:compileKotlin"
        val allKotlinTasks = listOf(compileCommonTask, compileJsTask, compileJvmTask)

        build("build")

        val commonProjectDir = File(projectDir, "lib")
        commonProjectDir.getFileByName("PlatformClass.kt").modify { it + "\n" }
        build("build") {
            assertTasksExecuted(*allKotlinTasks.toTypedArray())
        }

        val jvmProjectDir = File(projectDir, "libJvm")
        jvmProjectDir.getFileByName("PlatformClass.kt").modify { it + "\n" }
        build("build") {
            assertTasksExecuted(compileJvmTask)
            assertTasksUpToDate(compileCommonTask, compileJsTask)
        }

        val jsProjectDir = File(projectDir, "libJs")
        jsProjectDir.getFileByName("PlatformClass.kt").modify { it + "\n" }
        build("build") {
            assertTasksExecuted(compileJsTask)
            assertTasksUpToDate(compileCommonTask, compileJvmTask)
        }
    }

    @GradleTestWithOsCondition
    fun testMultipleCommonModules(gradleVersion: GradleVersion): Unit = with(project("multiplatformMultipleCommonModules", gradleVersion)) {
        build("build", buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Summary)) {
            val sourceSets = listOf("", "Test")
            val commonTasks = listOf("libA", "libB").flatMap { module ->
                sourceSets.map { sourceSet -> ":$module:compile${sourceSet}KotlinCommon" }
            }
            val platformTasks = listOf("libJvm" to "", "libJs" to "2Js").flatMap { (module, platformSuffix) ->
                sourceSets.map { sourceSet -> ":$module:compile${sourceSet}Kotlin$platformSuffix" }
            }
            assertTasksExecuted(*(commonTasks + platformTasks).toTypedArray())

            val expectedJvmMainClasses =
                listOf("PlatformClassB", "PlatformClassA", "JavaLibUseKt", "CommonClassB", "CommonClassA").map { "foo/$it" }
            val jvmMainClassesDir = subProject("libJvm").kotlinClassesDir().toFile()
            expectedJvmMainClasses.forEach { className ->
                assertTrue(File(jvmMainClassesDir, "$className.class").isFile, "Class $className should be compiled for JVM.")
            }

            val expectedJvmTestClasses = listOf("PlatformTestB", "PlatformTestA", "CommonTestB", "CommonTestA").map { "foo/$it" }
            val jvmTestClassesDir = subProject("libJvm").kotlinClassesDir(sourceSet = "test").toFile()
            expectedJvmTestClasses.forEach { className ->
                assertTrue(File(jvmTestClassesDir, "$className.class").isFile, "Class $className should be compiled for JVM.")
            }
        }
    }

    @GradleTestWithOsCondition
    fun testFreeCompilerArgsAssignment(gradleVersion: GradleVersion): Unit = with(project("multiplatformProject", gradleVersion)) {
        val overrideCompilerArgs = "kotlinOptions.freeCompilerArgs = ['-verbose']"

        gradleBuildScript("lib").appendText("\ncompileKotlinCommon.$overrideCompilerArgs")
        gradleBuildScript("libJvm").appendText("\ncompileKotlin.$overrideCompilerArgs")
        gradleBuildScript("libJs").appendText("\ncompileKotlin2Js.$overrideCompilerArgs")

        build("build") {
            assertTasksExecuted(":lib:compileKotlinCommon", ":libJvm:compileKotlin", ":libJs:compileKotlin2Js")
        }
    }

    @GradleTestWithOsCondition
    fun testCommonModuleAsTransitiveDependency(gradleVersion: GradleVersion) = with(project("multiplatformProject", gradleVersion)) {
        gradleBuildScript("libJvm").appendText(
            """
            ${'\n'}
            task printCompileConfiguration(type: DefaultTask) {
                doFirst {
                    configurations.getByName("api").dependencies.each {
                        println("Dependency: '" + it.name + "'")
                    }
                }
            }
            """.trimIndent()
        )

        build("printCompileConfiguration") {
            // Check that `lib` is contained in the resolved compile artifacts of `libJvm`:
            assertOutputContains("Dependency: 'lib'")
        }
    }

    @GradleTestWithOsCondition
    fun testArchivesBaseNameAsCommonModuleName(gradleVersion: GradleVersion) = with(project("multiplatformProject", gradleVersion)) {
        val moduleName = "my_module_name"

        gradleBuildScript("lib").appendText("\narchivesBaseName = '$moduleName'")

        build("compileKotlinCommon") {
            assertFileExists(subProject("lib").kotlinClassesDir().resolve("META-INF/$moduleName.kotlin_module"))
        }
    }

    @GradleTestWithOsCondition
    fun testKt23092(gradleVersion: GradleVersion) = with(project("multiplatformProject", gradleVersion)) {
        val successMarker = "Found JavaCompile task:"

        gradleBuildScript("lib").appendText(
            "\n" + """
            afterEvaluate {
                println('$successMarker ' + tasks.getByName('compileJava').path)
                println('$successMarker ' + tasks.getByName('compileTestJava').path)
            }
            """.trimIndent()
        )

        build(":lib:tasks") {
            assertOutputContains("$successMarker :lib:compileJava")
            assertOutputContains("$successMarker :lib:compileTestJava")
        }
    }

    @GradleTestWithOsCondition
    fun testCustomSourceSets(gradleVersion: GradleVersion) = with(project("multiplatformProject", gradleVersion)) {
        val sourceSetName = "foo"
        val sourceSetDeclaration = "\nsourceSets { $sourceSetName { } }"

        listOf("lib", "libJvm", "libJs").forEach { module ->
            gradleBuildScript(module).appendText(sourceSetDeclaration)
        }

        listOf(
            "expect fun foo(): String" to "lib/src/$sourceSetName/kotlin",
            "actual fun foo(): String = \"jvm\"" to "libJvm/src/$sourceSetName/kotlin",
            "actual fun foo(): String = \"js\"" to "libJs/src/$sourceSetName/kotlin"
        ).forEach { (code, path) ->
            File(projectDir, path).run {
                mkdirs();
                File(this, "Foo.kt").writeText(code)
            }
        }

        val customSourceSetCompileTasks = listOf(":lib" to "Common", ":libJs" to "2Js", ":libJvm" to "")
            .map { (module, platform) -> "$module:compile${sourceSetName.capitalize()}Kotlin$platform" }

        build(*customSourceSetCompileTasks.toTypedArray(), buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Summary)) {
            assertTasksExecuted(*customSourceSetCompileTasks.toTypedArray())
        }
    }

    @GradleTestWithOsCondition
    fun testWithJavaDuplicatedResourcesFail(gradleVersion: GradleVersion) = with(
        project(
            projectName = "mpp-single-jvm-target",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.WARN)
        )
    ) {
        gradleBuildScript().modify { buildFileContent ->
            buildFileContent
                .lines()
                .joinToString(separator = "\n") {
                    if (it.contains("jvm()")) {
                        "jvm { withJava() }"
                    } else {
                        it
                    }
                }
        }

        val resDir = projectDir.resolve("src/jvmMain/resources").also { it.mkdirs() }
        resDir.resolve("test.properties").writeText(
            """
            one=true
            two=false
            """.trimIndent()
        )

        build("assemble") {
            assertOutputDoesNotContain("no duplicate handling strategy has been set")
        }
    }
}