/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.internals.KOTLIN_TEST_MULTIPLATFORM_MODULE_NAME
import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.KotlinTestUtils
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinSpecificDependenciesIT : BaseGradleIT() {

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(androidGradlePluginVersion = AGPVersion.v3_6_0, androidHome = KotlinTestUtils.findAndroidSdk())

    private fun Project.prepare() { // call this when reusing a project after a test, too, in order to remove any added dependencies
        setupWorkingDir()
        gradleSettingsScript().takeIf { it.exists() }?.modify(::transformBuildScriptWithPluginsDsl)
        gradleBuildScript().modify {
            transformBuildScriptWithPluginsDsl(it).lines().filter { line ->
                "stdlib" !in line && "kotlin(\"test" !in line && "kotlin-test" !in line
            }.joinToString("\n")
        }
    }

    private fun jsProject() = Project("kotlin-js-plugin-project").apply {
        setupWorkingDir()
        gradleBuildScript().modify { it.lines().filter { "html" !in it }.joinToString("\n") }
        projectFile("Main.kt").modify { "fun f() = listOf(1, 2, 3).joinToString()" }
        prepare()
    }

    private fun androidProject() = Project("AndroidLibraryKotlinProject").apply { prepare() }

    private fun mppProject() = Project("jvm-and-js-hmpp").apply {
        setupWorkingDir()
        prepare()
    }

    private fun jvmProject() = Project("simpleProject").apply {
        prepare()
        gradleBuildScript().modify { it.lines().filter { "testng" !in it }.joinToString("\n") }
    }

    @Test
    fun testStdlibByDefault() {
        listOf( // projects and tasks:
            androidProject() to listOf("compileDebugKotlin"),
            jvmProject() to listOf("compileKotlin"),
            jsProject() to listOf("compileKotlinJs"),
            mppProject() to listOf(
                "compileKotlinJvm",
                "compileKotlinJs",
                "compileCommonMainKotlinMetadata",
                "compileJvmAndJsMainKotlinMetadata"
            )
        ).forEach { (project, tasks) ->
            with(project) {
                prepare()
                tasks.forEach { task ->
                    project.checkTaskCompileClasspath(task, listOf("kotlin-stdlib" /*any of them*/))
                }
                projectDir.resolve("gradle.properties").appendText(
                    "\nkotlin.stdlib.default.dependency=false"
                )
                tasks.forEach { task ->
                    project.checkTaskCompileClasspath(task, listOf(), checkModulesNotInClasspath = listOf("kotlin-stdlib" /*any of them*/))
                }
            }
        }
    }

    @Test
    fun testStdlibBasedOnJdk() = with(jvmProject()) {
        prepare()
        gradleBuildScript().modify { "$it\nkotlin.target.compilations[\"main\"].kotlinOptions { jvmTarget = \"1.6\" }" }
        val version = defaultBuildOptions().kotlinVersion
        checkTaskCompileClasspath(
            "compileKotlin",
            listOf("kotlin-stdlib-$version"),
            listOf("kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8")
        )
        gradleBuildScript().modify { "$it\nkotlin.target.compilations[\"main\"].kotlinOptions { jvmTarget = \"11\" }" }
        checkTaskCompileClasspath(
            "compileKotlin",
            listOf("kotlin-stdlib", "kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8"),
        )
    }

    @Test
    fun testOverrideStdlib() = with(jvmProject().apply { prepare() }) {
        gradleBuildScript().appendText(
            "\n" + """
            kotlin.target.compilations["main"].kotlinOptions.jvmTarget = "1.8"
            dependencies { implementation("org.jetbrains.kotlin:kotlin-stdlib") }
            """.trimIndent()
        )
        // Check that the explicit stdlib overrides the plugin's choice of stdlib-jdk8
        checkTaskCompileClasspath(
            "compileKotlin",
            listOf("kotlin-stdlib-${defaultBuildOptions().kotlinVersion}"),
            listOf("kotlin-stdlib-jdk8")
        )
    }

    private val kotlinTestMultiplatformDependency = "org.jetbrains.kotlin:$KOTLIN_TEST_MULTIPLATFORM_MODULE_NAME"

    @Test
    fun testKotlinTestSingleDependency() {
        data class TestCase(
            val project: Project,
            val configurationsToAddDependency: List<String>,
            val classpathElementsExpectedByTask: Map<String, List<String>>,
            val filesExpectedByConfiguration: Map<String, List<String>> = emptyMap()
        )

        listOf(
            TestCase(
                androidProject(),
                listOf("testImplementation"),
                mapOf(
                    "compileDebugUnitTestKotlin" to listOf("kotlin-test-junit"),
                    "compileReleaseUnitTestKotlin" to listOf("kotlin-test-junit")
                )
            ),
            TestCase(
                androidProject(),
                listOf("androidTestImplementation"),
                mapOf("compileDebugAndroidTestKotlin" to listOf("kotlin-test-junit"))
            ),
            TestCase(jvmProject(), listOf("testImplementation"), mapOf("compileTestKotlin" to listOf("kotlin-test-testng"))),
            TestCase(jsProject(), listOf("testImplementation"), mapOf("compileTestKotlinJs" to listOf("kotlin-test-js"))),
            TestCase(
                mppProject(),
                listOf("commonTestImplementation"),
                mapOf(
                    "compileTestKotlinJvm" to listOf("kotlin-test-junit"),
                    "compileTestKotlinJs" to listOf("kotlin-test-js")
                ),
                mapOf(
                    "commonTestImplementationDependenciesMetadata" to listOf("kotlin-test-common", "kotlin-test-annotations-common"),
                    "commonTestApiDependenciesMetadata" to listOf("!kotlin-test-common", "!kotlin-test-annotations-common"),
                )
            ),
            TestCase(
                mppProject(),
                listOf("jvmAndJsTestApi", "jvmAndJsTestCompileOnly"), // add to the intermediate source set, and to two scopes
                mapOf(
                    "compileTestKotlinJvm" to listOf("kotlin-test-junit"),
                    "compileTestKotlinJs" to listOf("kotlin-test-js")
                ),
                mapOf(
                    "commonTestApiDependenciesMetadata" to listOf("!kotlin-test-common"),
                    "commonTestCompileOnlyDependenciesMetadata" to listOf("!kotlin-test-common"),
                    "jvmAndJsTestApiDependenciesMetadata" to listOf("kotlin-test-common"),
                    "jvmAndJsTestCompileOnlyDependenciesMetadata" to listOf("kotlin-test-common"),
                    "jvmAndJsTestImplementationDependenciesMetadata" to listOf("!kotlin-test-common"),
                )
            )
        ).forEach { testCase ->
            with(testCase) {
                project.prepare()
                project.gradleBuildScript().appendText(
                    configurationsToAddDependency.joinToString("\n", "\n") { configuration ->
                        "\ndependencies { \"$configuration\"(\"$kotlinTestMultiplatformDependency\") }"
                    }
                )
                classpathElementsExpectedByTask.forEach { (task, expected) ->
                    val (notInClasspath, inClasspath) = expected.partition { it.startsWith("!") }
                    project.checkTaskCompileClasspath(task, inClasspath, notInClasspath.map { it.removePrefix("!") })
                }
                filesExpectedByConfiguration.forEach { (configuration, expected) ->
                    val (notInItems, inItems) = expected.partition { it.startsWith("!") }
                    project.checkConfigurationContent(configuration, subproject = null, inItems, notInItems.map { it.removePrefix("!") })
                }
            }
        }
    }

    @Test
    fun testFrameworkSelection() {
        data class TestCase(
            val project: Project,
            val testTaskName: String,
            val compileTaskName: String,
            val configurationToAddDependency: String
        )

        val frameworks = listOf("useJUnit()" to "junit", "useTestNG()" to "testng", "useJUnitPlatform()" to "junit5")
        listOf(
            TestCase(jvmProject(), "test", "compileTestKotlin", "testImplementation"),
            TestCase(mppProject(), "jvmTest", "compileTestKotlinJvm", "commonTestImplementation"),
            TestCase(mppProject(), "jvmTest", "compileTestKotlinJvm", "jvmAndJsTestImplementation")
        ).forEach { (project, testTaskName, compileTaskName, configuration) ->
            project.prepare()
            project.gradleBuildScript().appendText("""${'\n'}dependencies { "$configuration"("$kotlinTestMultiplatformDependency") }""")

            frameworks.forEach { (setup, frameworkName) ->
                with(project) {
                    gradleBuildScript().appendText("\n(tasks.getByName(\"$testTaskName\") as Test).$setup")
                    val expectedModule = "kotlin-test-$frameworkName-"
                    checkTaskCompileClasspath(
                        compileTaskName,
                        listOf(expectedModule),
                        frameworks.map { "kotlin-test-" + it.second + "-" } - expectedModule
                    )
                }
            }
        }
    }

    @Test
    fun testRemoveKotlinTestDependency() = with(jvmProject().apply { prepare() }) {
        gradleBuildScript().appendText(
            "\n" + """
            dependencies { testImplementation("$kotlinTestMultiplatformDependency") }
            configurations.getByName("testImplementation").dependencies.removeAll { it.name == "$KOTLIN_TEST_MULTIPLATFORM_MODULE_NAME" }
            """.trimIndent()
        )
        checkTaskCompileClasspath("compileTestKotlin", checkModulesNotInClasspath = listOf("kotlin-test"))

        // Add it back after removal:
        gradleBuildScript().appendText(
            "\n" + """
            dependencies { testImplementation("$kotlinTestMultiplatformDependency") }
            """
        )
        checkTaskCompileClasspath("compileTestKotlin", checkModulesInClasspath = listOf("kotlin-test"))
    }

    @Test
    fun testCoreLibraryVersionsDsl() = with(jvmProject().apply { prepare() }) {
        val customVersion = "1.3.70"
        gradleBuildScript().appendText(
            "\n" + """
            kotlin.coreLibrariesVersion = "$customVersion"
            dependencies {
                testImplementation("org.jetbrains.kotlin:kotlin-reflect")
                testImplementation("org.jetbrains.kotlin:kotlin-test-multiplatform")
            }
            test.useJUnit()
        """.trimIndent()
        )
        checkTaskCompileClasspath(
            "compileTestKotlin",
            listOf("kotlin-stdlib-", "kotlin-reflect-", "kotlin-test-junit-").map { it + customVersion }
        )
    }

    @Test
    fun testNoFailureIfConfigurationIsObserved() = with(jvmProject()) {
        lateinit var originalScript: String
        try {
            gradleBuildScript().modify {
                originalScript = it
                """
                    configurations.create("api")
                    dependencies {
                        api("org.jetbrains.kotlin:kotlin-reflect")
                    }
                    println(configurations.api.incoming.dependencies.toList())
                """.trimIndent() + "\n" + it
            }
            checkTaskCompileClasspath("compileKotlin", listOf("kotlin-reflect"))
        } finally {
            gradleBuildScript().writeText(originalScript)
        }
    }

    private fun Project.checkConfigurationContent(
        configurationName: String,
        subproject: String? = null,
        checkModulesInResolutionResult: List<String> = emptyList(),
        checkModulesNotInResolutionResult: List<String> = emptyList()
    ) {
        val expression = """configurations["$configurationName"].toList()"""
        checkPrintedItems(subproject, expression, checkModulesInResolutionResult, checkModulesNotInResolutionResult)
    }
}

private var testBuildRunId = 0

fun BaseGradleIT.Project.checkTaskCompileClasspath(
    taskPath: String,
    checkModulesInClasspath: List<String> = emptyList(),
    checkModulesNotInClasspath: List<String> = emptyList(),
    isNative: Boolean = false
) {
    val subproject = taskPath.substringBeforeLast(":").takeIf { it.isNotEmpty() && it != taskPath }
    val taskName = taskPath.removePrefix(subproject.orEmpty())
    val taskClass = if (isNative) "org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile<*, *>" else "AbstractCompile"
    val expression = """(tasks.getByName("$taskName") as $taskClass).${if (isNative) "libraries" else "classpath"}.toList()"""
    checkPrintedItems(subproject, expression, checkModulesInClasspath, checkModulesNotInClasspath)
}

private fun BaseGradleIT.Project.checkPrintedItems(
    subproject: String?,
    itemsExpression: String,
    checkAnyItemsContains: List<String>,
    checkNoItemContains: List<String>
) = with(testCase) {
    setupWorkingDir()
    val printingTaskName = "printItems${testBuildRunId++}"
    gradleBuildScript(subproject).appendText(
        """
        ${'\n'}
        tasks.create("$printingTaskName") {
            doLast {
                println("###$printingTaskName" + $itemsExpression)
            }
        }
        """.trimIndent()
    )
    build("${subproject?.prependIndent(":").orEmpty()}:$printingTaskName") {
        assertSuccessful()
        val itemsLine = output.lines().single { "###$printingTaskName" in it }.substringAfter(printingTaskName)
        val items = itemsLine.removeSurrounding("[", "]").split(", ").toSet()
        checkAnyItemsContains.forEach { pattern -> assertTrue { items.any { pattern in it } } }
        checkNoItemContains.forEach { pattern -> assertFalse { items.any { pattern in it } } }
    }
}