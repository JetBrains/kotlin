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

import org.jetbrains.kotlin.gradle.plugin.EXPECTED_BY_CONFIG_NAME
import org.jetbrains.kotlin.gradle.plugin.IMPLEMENT_CONFIG_NAME
import org.jetbrains.kotlin.gradle.plugin.IMPLEMENT_DEPRECATION_WARNING
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class MultiplatformGradleIT : BaseGradleIT() {

    @Test
    fun testMultiplatformCompile() {
        val project = Project("multiplatformProject")

        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(
                ":lib:compileKotlinCommon",
                ":lib:compileTestKotlinCommon",
                ":libJvm:compileKotlin",
                ":libJvm:compileTestKotlin",
                ":libJs:compileKotlin2Js",
                ":libJs:compileTestKotlin2Js"
            )
            assertFileExists("lib/build/classes/kotlin/main/foo/PlatformClass.kotlin_metadata")
            assertFileExists("lib/build/classes/kotlin/test/foo/PlatformTest.kotlin_metadata")
            assertFileExists("libJvm/build/classes/kotlin/main/foo/PlatformClass.class")
            assertFileExists("libJvm/build/classes/kotlin/test/foo/PlatformTest.class")
            assertFileExists("libJs/build/classes/kotlin/main/libJs.js")
            assertFileExists("libJs/build/classes/kotlin/test/libJs_test.js")
        }
    }

    @Test
    fun testDeprecatedImplementWarning() {
        val project = Project("multiplatformProject")

        project.build("build") {
            assertSuccessful()
            assertNotContains(IMPLEMENT_DEPRECATION_WARNING)
        }

        project.projectDir.walk().filter { it.name == "build.gradle" }.forEach { buildGradle ->
            buildGradle.modify { it.replace(EXPECTED_BY_CONFIG_NAME, IMPLEMENT_CONFIG_NAME) }
        }

        project.build("build") {
            assertSuccessful()
            assertContains(IMPLEMENT_DEPRECATION_WARNING)
        }
    }

    @Test
    fun testCommonKotlinOptions() {
        with(Project("multiplatformProject")) {
            setupWorkingDir()

            File(projectDir, "lib/build.gradle").appendText(
                "\ncompileKotlinCommon.kotlinOptions.freeCompilerArgs = ['-Xno-inline']" +
                        "\ncompileKotlinCommon.kotlinOptions.suppressWarnings = true"
            )

            build("build") {
                assertSuccessful()
                assertContains("-Xno-inline")
                assertContains("-nowarn")
            }
        }
    }

    @Test
    fun testSubprojectWithAnotherClassLoader() {
        with(Project("multiplatformProject")) {
            setupWorkingDir()

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
                        repositories { mavenLocal(); jcenter() }
                        dependencies {
                            classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version"
                        }
                    }
                    """.trimIndent() + "\n" + it
                }
            }

            build("build") {
                assertSuccessful()
            }
        }
    }

    // todo: also make incremental compilation test
    @Test
    fun testIncrementalBuild(): Unit = Project("multiplatformProject").run {
        val compileCommonTask = ":lib:compileKotlinCommon"
        val compileJsTask = ":libJs:compileKotlin2Js"
        val compileJvmTask = ":libJvm:compileKotlin"
        val allKotlinTasks = listOf(compileCommonTask, compileJsTask, compileJvmTask)

        build("build") {
            assertSuccessful()
        }

        val commonProjectDir = File(projectDir, "lib")
        commonProjectDir.getFileByName("PlatformClass.kt").modify { it + "\n" }
        build("build") {
            assertSuccessful()
            assertTasksExecuted(allKotlinTasks)
        }

        val jvmProjectDir = File(projectDir, "libJvm")
        jvmProjectDir.getFileByName("PlatformClass.kt").modify { it + "\n" }
        build("build") {
            assertSuccessful()
            assertTasksExecuted(compileJvmTask)
            assertTasksUpToDate(compileCommonTask, compileJsTask)
        }

        val jsProjectDir = File(projectDir, "libJs")
        jsProjectDir.getFileByName("PlatformClass.kt").modify { it + "\n" }
        build("build") {
            assertSuccessful()
            assertTasksExecuted(compileJsTask)
            assertTasksUpToDate(compileCommonTask, compileJvmTask)
        }
    }

    @Test
    fun testMultipleCommonModules(): Unit = with(Project("multiplatformMultipleCommonModules")) {
        build("build") {
            assertSuccessful()

            val sourceSets = listOf("", "Test")
            val commonTasks = listOf("libA", "libB").flatMap { module ->
                sourceSets.map { sourceSet -> ":$module:compile${sourceSet}KotlinCommon" }
            }
            val platformTasks = listOf("libJvm" to "", "libJs" to "2Js").flatMap { (module, platformSuffix) ->
                sourceSets.map { sourceSet -> ":$module:compile${sourceSet}Kotlin$platformSuffix" }
            }
            assertTasksExecuted(commonTasks + platformTasks)

            val expectedJvmMainClasses =
                listOf("PlatformClassB", "PlatformClassA", "JavaLibUseKt", "CommonClassB", "CommonClassA").map { "foo/$it" }
            val jvmMainClassesDir = File(projectDir, kotlinClassesDir(subproject = "libJvm"))
            expectedJvmMainClasses.forEach { className ->
                assertTrue(File(jvmMainClassesDir, className + ".class").isFile, "Class $className should be compiled for JVM.")
            }

            val expectedJvmTestClasses = listOf("PlatformTestB", "PlatformTestA", "CommonTestB", "CommonTestA").map { "foo/$it" }
            val jvmTestClassesDir = File(projectDir, kotlinClassesDir(subproject = "libJvm", sourceSet = "test"))
            expectedJvmTestClasses.forEach { className ->
                assertTrue(File(jvmTestClassesDir, className + ".class").isFile, "Class $className should be compiled for JVM.")
            }
        }
    }

    @Test
    fun testFreeCompilerArgsAssignment(): Unit = with(Project("multiplatformProject")) {
        setupWorkingDir()

        val overrideCompilerArgs = "kotlinOptions.freeCompilerArgs = ['-verbose']"

        gradleBuildScript("lib").appendText("\ncompileKotlinCommon.$overrideCompilerArgs")
        gradleBuildScript("libJvm").appendText("\ncompileKotlin.$overrideCompilerArgs")
        gradleBuildScript("libJs").appendText("\ncompileKotlin2Js.$overrideCompilerArgs")

        build("build") {
            assertSuccessful()
            assertTasksExecuted(":lib:compileKotlinCommon", ":libJvm:compileKotlin", ":libJs:compileKotlin2Js")
        }
    }

    @Test
    fun testCommonModuleAsTransitiveDependency() = with(Project("multiplatformProject")) {
        setupWorkingDir()
        gradleBuildScript("libJvm").appendText(
            """
            ${'\n'}
            task printCompileConfiguration(type: DefaultTask) {
                doFirst {
                    configurations.compile.resolvedConfiguration.resolvedArtifacts.each {
                        println("Dependency: '" + it.name + "'")
                    }
                }
            }
            """.trimIndent()
        )

        build("printCompileConfiguration") {
            assertSuccessful()
            // Check that `lib` is contained in the resolved compile artifacts of `libJvm`:
            assertContains("Dependency: 'lib'")
        }
    }

    @Test
    fun testArchivesBaseNameAsCommonModuleName() = with(Project("multiplatformProject")) {
        setupWorkingDir()

        val moduleName = "my_module_name"

        gradleBuildScript("lib").appendText("\narchivesBaseName = '$moduleName'")

        build("compileKotlinCommon") {
            assertSuccessful()
            assertFileExists(kotlinClassesDir(subproject = "lib") + "META-INF/$moduleName.kotlin_module")
        }
    }

    @Test
    fun testKt23092() = with(Project("multiplatformProject")) {
        setupWorkingDir()
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
            assertSuccessful()
            assertContains("$successMarker :lib:compileJava")
            assertContains("$successMarker :lib:compileTestJava")
        }
    }

    @Test
    fun testCustomSourceSets() = with(Project("multiplatformProject")) {
        setupWorkingDir()

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

        build(*customSourceSetCompileTasks.toTypedArray()) {
            assertSuccessful()
            assertTasksExecuted(customSourceSetCompileTasks)
        }
    }
}