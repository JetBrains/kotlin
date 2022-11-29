/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.*
import java.nio.file.Path
import java.util.UUID
import java.util.stream.Stream
import kotlin.io.path.appendText
import kotlin.streams.asStream
import kotlin.streams.toList
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("Kotlin default dependencies")
class KotlinSpecificDependenciesIT : KGPBaseTest() {

    @JvmGradlePluginTests
    @DisplayName("JVM: kotlin-stdlib dependency is added by default")
    @GradleTest
    fun testStdlibByDefaultJvm(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            removeDependencies(buildGradle)
            checkTaskCompileClasspath("compileKotlin", listOf("kotlin-stdlib"))
        }
    }

    @JvmGradlePluginTests
    @DisplayName("JVM: kotlin-stdlib dependency is not added when disabled via properties")
    @GradleTest
    fun testStdlibNotAddedJvm(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            removeDependencies(buildGradle)
            gradleProperties.appendText(
                "\nkotlin.stdlib.default.dependency=false"
            )
            checkTaskCompileClasspath(
                "compileKotlin",
                listOf(),
                checkModulesNotInClasspath = listOf("kotlin-stdlib" /*any of them*/)
            )
        }
    }

    @JsGradlePluginTests
    @DisplayName("JS: kotlin-stdlib dependency is added by default")
    @GradleTest
    fun testStdlibByDefaultJs(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-plugin-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Summary)
        ) {
            buildGradleKts.modify { it.lines().filter { "html" !in it }.joinToString("\n") }
            kotlinSourcesDir().resolve("Main.kt").modify { "fun f() = listOf(1, 2, 3).joinToString()" }
            removeDependencies(buildGradleKts)

            checkTaskCompileClasspath(
                "compileKotlinJs",
                listOf("kotlin-stdlib-js"),
                isBuildGradleKts = true
            )
        }
    }

    @JsGradlePluginTests
    @DisplayName("JS: kotlin-stdlib dependency is not added when disabled via properties")
    @GradleTest
    fun testStdlibDisabledJs(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-plugin-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Summary)
        ) {
            buildGradleKts.modify { it.lines().filter { "html" !in it }.joinToString("\n") }
            kotlinSourcesDir().resolve("Main.kt").modify { "fun f() = listOf(1, 2, 3).joinToString()" }
            removeDependencies(buildGradleKts)

            gradleProperties.appendText(
                "\nkotlin.stdlib.default.dependency=false"
            )
            checkTaskCompileClasspath(
                "compileKotlinJs",
                listOf(),
                checkModulesNotInClasspath = listOf("kotlin-stdlib" /*any of them*/),
                isBuildGradleKts = true
            )
        }
    }

    @AndroidGradlePluginTests
    @DisplayName("Android: kotlin-stdlib dependency is added by default")
    @GradleAndroidTest
    fun testStdlibDefaultAndroid(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidLibraryKotlinProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
            ),
            buildJdk = jdkVersion.location
        ) {
            removeDependencies(buildGradle)
            checkTaskCompileClasspath("compileDebugKotlin", listOf("kotlin-stdlib"))
        }
    }

    @AndroidGradlePluginTests
    @DisplayName("Android: kotlin-stdlib dependency is not added when disabled via properties")
    @GradleAndroidTest
    fun testStdlibDisabledAndroid(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidLibraryKotlinProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
            ),
            buildJdk = jdkVersion.location
        ) {
            removeDependencies(buildGradle)
            gradleProperties.appendText(
                "\nkotlin.stdlib.default.dependency=false"
            )
            checkTaskCompileClasspath(
                "compileDebugKotlin",
                emptyList(),
                checkModulesNotInClasspath = listOf("kotlin-stdlib" /*any of them*/),
            )
        }
    }

    @MppGradlePluginTests
    @DisplayName("MPP: kotlin-stdlib is added by default")
    @GradleTest
    fun kotlinStdlibDefaultMpp(gradleVersion: GradleVersion) {
        project("jvm-and-js-hmpp", gradleVersion) {
            removeDependencies(buildGradleKts)

            listOf(
                "compileKotlinJvm",
                "compileKotlinJs",
                "compileCommonMainKotlinMetadata",
                "compileJvmAndJsMainKotlinMetadata"
            ).forEach { task ->
                checkTaskCompileClasspath(task, listOf("kotlin-stdlib"), isBuildGradleKts = true)
            }
        }
    }

    @MppGradlePluginTests
    @DisplayName("MPP: kotlin-stdlib is not added when disabled in properties")
    @GradleTest
    fun kotlinStdlibDisabledMpp(gradleVersion: GradleVersion) {
        project("jvm-and-js-hmpp", gradleVersion) {
            removeDependencies(buildGradleKts)
            gradleProperties.appendText(
                "\nkotlin.stdlib.default.dependency=false"
            )

            listOf(
                "compileKotlinJvm",
                "compileKotlinJs",
                "compileCommonMainKotlinMetadata",
                "compileJvmAndJsMainKotlinMetadata"
            ).forEach { task ->
                checkTaskCompileClasspath(
                    task,
                    emptyList(),
                    checkModulesNotInClasspath = listOf("kotlin-stdlib"),
                    isBuildGradleKts = true
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Explicit kotlin-stdlib version overrides default one")
    @GradleTest
    fun testOverrideStdlib(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            removeDependencies(buildGradle)
            buildGradle.appendText(
                """
                
                dependencies { implementation("org.jetbrains.kotlin:kotlin-stdlib") }
                """.trimIndent()
            )

            // Check that the explicit stdlib overrides the plugin's choice of stdlib-jdk8
            checkTaskCompileClasspath(
                "compileKotlin",
                listOf("kotlin-stdlib-${defaultBuildOptions.kotlinVersion}"),
                listOf("kotlin-stdlib-jdk8")
            )
        }
    }

    @JvmGradlePluginTests
    @DisplayName("KT-41642: adding stdlib should not resolve dependencies eagerly")
    @GradleTest
    fun testStdlibEagerDependencies(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {

            // Disabling auto-adding kotlin.test dependencies
            gradleProperties.appendText(
                """
                
                kotlin.test.infer.jvm.variant=false
                """.trimIndent()
            )

            buildGradle.appendText(
                //language=Groovy
                """

                configurations.each { config ->
                	config.dependencies.addAllLater(
                        project.objects.listProperty(Dependency.class).value(
                            project.provider {
                		        throw new Throwable("Dependency resolved in ${'$'}{config.name}!")
                	        }
                        )
                    )
                }
                """.trimIndent()
            )

            build("help") {
                assertOutputDoesNotContain("Dependency resolved in")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("KT-41642: adding kotlin.test should not resolve dependencies eagerly")
    @GradleTest
    fun testKotlinTestEagerDependencies(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {

            // Disabling auto-adding kotlin stdlib dependencies
            gradleProperties.appendText(
                """
                
                kotlin.stdlib.default.dependency=false
                """.trimIndent()
            )

            buildGradle.appendText(
                //language=Groovy
                """

                configurations.each { config ->
                	config.dependencies.addAllLater(
                        project.objects.listProperty(Dependency.class).value(
                            project.provider {
                		        throw new Throwable("Dependency resolved in ${'$'}{config.name}!")
                	        }
                        )
                    )
                }
                """.trimIndent()
            )

            build("help") {
                assertOutputDoesNotContain("Dependency resolved in")
            }
        }
    }

    @AndroidGradlePluginTests
    @DisplayName("Android: Kotlin test single dependency in unit tests")
    @GradleAndroidTest
    fun kotlinTestSingleDependencyAndroidUnitTests(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidLibraryKotlinProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
            ),
            buildJdk = jdkVersion.location
        ) {
            assertKotlinTestDependency(
                listOf("testImplementation"),
                mapOf(
                    "compileDebugUnitTestKotlin" to listOf("kotlin-test-junit"),
                    "compileReleaseUnitTestKotlin" to listOf("kotlin-test-junit")
                )
            )
        }
    }

    @AndroidGradlePluginTests
    @DisplayName("Android: Kotlin test single dependency in ui tests")
    @GradleAndroidTest
    fun kotlinTestSingleDependencyAndroidUiTests(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidLibraryKotlinProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
            ),
            buildJdk = jdkVersion.location
        ) {
            assertKotlinTestDependency(
                listOf("androidTestImplementation"),
                mapOf("compileDebugAndroidTestKotlin" to listOf("kotlin-test-junit"))
            )
        }
    }

    @JvmGradlePluginTests
    @DisplayName("JVM: Kotlin test single dependency")
    @GradleTest
    fun kotlinTestSingleDependencyJvm(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            assertKotlinTestDependency(
                listOf("testImplementation"),
                mapOf("compileTestKotlin" to listOf("kotlin-test-testng"))
            )
        }
    }

    @JsGradlePluginTests
    @DisplayName("JS: Kotlin test single dependency")
    @GradleTest
    fun kotlinTestSingleDependencyJs(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-plugin-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Summary)
        ) {
            assertKotlinTestDependency(
                listOf("testImplementation"),
                mapOf("compileTestKotlinJs" to listOf("kotlin-test-js")),
                isBuildGradleKts = true
            )
        }
    }

    @MppGradlePluginTests
    @DisplayName("MPP: Kotlin test single dependency in common")
    @GradleTest
    fun kotlinTestSingleDependencyMppCommon(gradleVersion: GradleVersion) {
        project("jvm-and-js-hmpp", gradleVersion) {
            assertKotlinTestDependency(
                listOf("commonTestImplementation"),
                mapOf(
                    "compileTestKotlinJvm" to listOf("kotlin-test-junit"),
                    "compileTestKotlinJs" to listOf("kotlin-test-js")
                ),
                mapOf(
                    "commonTestImplementationDependenciesMetadata" to listOf("kotlin-test-common", "kotlin-test-annotations-common"),
                    "commonTestApiDependenciesMetadata" to listOf("!kotlin-test-common", "!kotlin-test-annotations-common"),
                ),
                isBuildGradleKts = true
            )
        }
    }

    @MppGradlePluginTests
    @DisplayName("MPP: Kotlin test single dependency in JVM and JS variants")
    @GradleTest
    fun kotlinTestSingleDependencyMppJvmJs(gradleVersion: GradleVersion) {
        project("jvm-and-js-hmpp", gradleVersion) {
            assertKotlinTestDependency(
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
                ),
                isBuildGradleKts = true
            )
        }
    }

    @MppGradlePluginTests
    @DisplayName("MPP (KTIJ-6098): in single platform project common metadata configurations resolve the framework-specific dependency ")
    @GradleTest
    fun kotlinTestSingleDependencyMppCommonSinglePlatform(gradleVersion: GradleVersion) {
        project("jvm-and-js-hmpp", gradleVersion) {
            assertKotlinTestDependency(
                listOf("commonTestImplementation"),
                mapOf(
                    "compileTestKotlinJvm" to listOf("kotlin-test-junit"),
                ),
                mapOf(
                    "commonTestImplementationDependenciesMetadata" to listOf("kotlin-test-common", "kotlin-test-annotations-common")
                ),
                isBuildGradleKts = true
            )
        }
    }

    @JvmGradlePluginTests
    @DisplayName("JVM: test framework variant proper selection")
    @GradleTestVersions
    @ParameterizedTest(name = "{1} with {0}: {displayName}")
    @ArgumentsSource(GradleAndTestFrameworksArgumentsProvider::class)
    fun testFrameworkSelectionJvm(
        gradleVersion: GradleVersion,
        testFramework: Pair<String, String>
    ) {
        project("simpleProject", gradleVersion) {
            removeDependencies(buildGradle)
            buildGradle.appendText("""${'\n'}dependencies { "testImplementation"("$kotlinTestMultiplatformDependency") }""")
            buildGradle.appendText("\n(tasks.getByName(\"test\") as Test).${testFramework.first}")

            val expectedModule = "kotlin-test-${testFramework.second}-"
            checkTaskCompileClasspath(
                "compileTestKotlin",
                listOf(expectedModule),
                testFrameworks.map { "kotlin-test-" + it.second + "-" } - expectedModule
            )
        }
    }

    @MppGradlePluginTests
    @DisplayName("MPP common: test framework variant proper selection")
    @GradleTestVersions
    @ParameterizedTest(name = "{1} with {0}: {displayName}")
    @ArgumentsSource(GradleAndTestFrameworksArgumentsProvider::class)
    fun testFrameworkSelectionMppJvm(
        gradleVersion: GradleVersion,
        testFramework: Pair<String, String>
    ) {
        project("jvm-and-js-hmpp", gradleVersion) {
            removeDependencies(buildGradleKts)
            buildGradleKts.appendText("""${'\n'}dependencies { "jvmAndJsTestImplementation"("$kotlinTestMultiplatformDependency") }""")
            buildGradleKts.appendText("\n(tasks.getByName(\"jvmTest\") as Test).${testFramework.first}")

            val expectedModule = "kotlin-test-${testFramework.second}-"
            checkTaskCompileClasspath(
                "compileTestKotlinJvm",
                listOf(expectedModule),
                testFrameworks.map { "kotlin-test-" + it.second + "-" } - expectedModule,
                isBuildGradleKts = true
            )
        }
    }

    @MppGradlePluginTests
    @DisplayName("MPP jvm: test framework variant proper selection")
    @GradleTestVersions
    @ParameterizedTest(name = "{1} with {0}: {displayName}")
    @ArgumentsSource(GradleAndTestFrameworksArgumentsProvider::class)
    fun testFrameworkSelectionMppCommon(
        gradleVersion: GradleVersion,
        testFramework: Pair<String, String>
    ) {
        project("jvm-and-js-hmpp", gradleVersion) {
            removeDependencies(buildGradleKts)
            buildGradleKts.appendText("""${'\n'}dependencies { "commonTestImplementation"("$kotlinTestMultiplatformDependency") }""")
            buildGradleKts.appendText("\n(tasks.getByName(\"jvmTest\") as Test).${testFramework.first}")

            val expectedModule = "kotlin-test-${testFramework.second}-"
            checkTaskCompileClasspath(
                "compileTestKotlinJvm",
                listOf(expectedModule),
                testFrameworks.map { "kotlin-test-" + it.second + "-" } - expectedModule,
                isBuildGradleKts = true
            )
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Possible to remove 'kotlin-test' dependency from configuration")
    @GradleTest
    fun testRemoveKotlinTestDependency(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            removeDependencies(buildGradle)

            buildGradle.appendText(
                """
                
                dependencies { testImplementation("$kotlinTestMultiplatformDependency") }
                configurations.getByName("testImplementation").dependencies.removeAll { it.name == "kotlin-test" }
                """.trimIndent()
            )
            checkTaskCompileClasspath("compileTestKotlin", checkModulesNotInClasspath = listOf("kotlin-test"))

            // Add it back after removal:
            buildGradle.appendText(
                """
                
                dependencies { testImplementation("$kotlinTestMultiplatformDependency") }
                """.trimIndent()
            )
            checkTaskCompileClasspath("compileTestKotlin", checkModulesInClasspath = listOf("kotlin-test"))
        }
    }

    @JvmGradlePluginTests
    @DisplayName("coreLibrariesVersion override default version")
    @GradleTest
    fun testCoreLibraryVersionsDsl(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            removeDependencies(buildGradle)
            val customVersion = TestVersions.Kotlin.STABLE_RELEASE
            buildGradle.appendText(
                """
                
                kotlin.coreLibrariesVersion = "$customVersion"
                dependencies {
                    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
                    testImplementation("org.jetbrains.kotlin:kotlin-test")
                }
                test.useJUnit()
                """.trimIndent()
            )

            checkTaskCompileClasspath(
                "compileTestKotlin",
                listOf("kotlin-stdlib-", "kotlin-reflect-", "kotlin-test-").map { it + customVersion }
            )
        }
    }

    @JvmGradlePluginTests
    @DisplayName("No failure if configuration is observed")
    @GradleTest
    fun testNoFailureIfConfigurationIsObserved(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            removeDependencies(buildGradle)
            buildGradle.modify {
                //language=Groovy
                """
                $it
                
                configurations {
                     apiTest
                     api.extendsFrom(apiTest)
                }
                
                dependencies {
                    apiTest("org.jetbrains.kotlin:kotlin-reflect")
                }
                println(configurations.apiTest.incoming.resolutionResult.allDependencies)
                println(configurations.apiTest.incoming.dependencies.toList())
                """.trimIndent()
            }

            checkTaskCompileClasspath("compileKotlin", listOf("kotlin-reflect"))
        }
    }

    private fun TestProject.assertKotlinTestDependency(
        configurationsToAddDependency: List<String>,
        classpathElementsExpectedByTask: Map<String, List<String>>,
        filesExpectedByConfiguration: Map<String, List<String>> = emptyMap(),
        isBuildGradleKts: Boolean = false
    ) {
        val buildFile = if (isBuildGradleKts) buildGradleKts else buildGradle
        removeDependencies(buildFile)
        buildFile.appendText(
            configurationsToAddDependency.joinToString("\n", "\n") { configuration ->
                "\ndependencies { \"$configuration\"(\"$kotlinTestMultiplatformDependency\") }"
            }
        )
        classpathElementsExpectedByTask.forEach { (task, expected) ->
            val (notInClasspath, inClasspath) = expected.partition { it.startsWith("!") }
            checkTaskCompileClasspath(
                task,
                inClasspath,
                notInClasspath.map { it.removePrefix("!") },
                isBuildGradleKts = isBuildGradleKts
            )
        }
        filesExpectedByConfiguration.forEach { (configuration, expected) ->
            val (notInItems, inItems) = expected.partition { it.startsWith("!") }
            checkConfigurationContent(
                configuration,
                inItems,
                notInItems.map { it.removePrefix("!") },
                isBuildGradleKts
            )
        }
    }

    private fun TestProject.checkConfigurationContent(
        configurationName: String,
        checkModulesInResolutionResult: List<String> = emptyList(),
        checkModulesNotInResolutionResult: List<String> = emptyList(),
        isBuildGradleKts: Boolean
    ) {
        val expression = """configurations["$configurationName"].toList()"""
        checkPrintedItems(
            null,
            expression,
            checkModulesInResolutionResult,
            checkModulesNotInResolutionResult,
            isBuildGradleKts
        )
    }

    private fun TestProject.checkTaskCompileClasspath(
        taskPath: String,
        checkModulesInClasspath: List<String> = emptyList(),
        checkModulesNotInClasspath: List<String> = emptyList(),
        isBuildGradleKts: Boolean = false
    ) {
        val subproject = taskPath.substringBeforeLast(":").takeIf { it.isNotEmpty() && it != taskPath }
        val taskName = taskPath.removePrefix(subproject.orEmpty())
        val taskClass = if (isBuildGradleKts) {
            "org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool<*>"
        } else {
            "org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool<?>"
        }
        val expression = """(tasks.getByName("$taskName") as $taskClass).libraries.toList()"""
        checkPrintedItems(subproject, expression, checkModulesInClasspath, checkModulesNotInClasspath, isBuildGradleKts)
    }

    private fun TestProject.checkPrintedItems(
        subproject: String?,
        itemsExpression: String,
        checkAnyItemsContains: List<String>,
        checkNoItemContains: List<String>,
        isBuildGradleKts: Boolean
    ) {
        val printingTaskName = "printItems${UUID.randomUUID()}"
        val buildFile = if (subproject != null) {
            subProject(subproject).run { if (isBuildGradleKts) buildGradleKts else buildGradle }
        } else {
            if (isBuildGradleKts) buildGradleKts else buildGradle
        }
        buildFile.appendText(
            """

            tasks.create("$printingTaskName") {
                doLast {
                    println("###$printingTaskName " + $itemsExpression)
                }
            }
            """.trimIndent()
        )

        build("${subproject?.prependIndent(":").orEmpty()}:$printingTaskName") {
            val itemsLine = output.lines().single { "###$printingTaskName" in it }.substringAfter(printingTaskName)
            val items = itemsLine.removeSurrounding("[", "]").split(", ").toSet()
            checkAnyItemsContains.forEach { pattern ->
                assertTrue("Dependencies does not contain $pattern") { items.any { pattern in it } }
            }
            checkNoItemContains.forEach { pattern ->
                assertFalse("Dependencies contain $pattern") { items.any { pattern in it } }
            }
        }
    }

    private fun removeDependencies(
        buildGradleFile: Path
    ) {
        buildGradleFile.modify {
            it.lines()
                .filter { line ->
                    "stdlib" !in line && "kotlin(\"test" !in line && "kotlin-test" !in line
                }
                .joinToString("\n")
        }
    }

    internal class GradleAndTestFrameworksArgumentsProvider : GradleArgumentsProvider() {
        override fun provideArguments(
            context: ExtensionContext
        ): Stream<out Arguments> {
            val gradleVersions = super.provideArguments(context).map { it.get().first() as GradleVersion }.toList()
            return testFrameworks
                .flatMap { testFramework ->
                    gradleVersions.map { it to testFramework }
                }
                .asSequence()
                .map {
                    Arguments.of(it.first, it.second)
                }
                .asStream()
        }
    }

    companion object {
        private const val kotlinTestMultiplatformDependency = "org.jetbrains.kotlin:kotlin-test"

        private val testFrameworks = listOf(
            "useJUnit()" to "junit",
            "useTestNG()" to "testng",
            "useJUnitPlatform()" to "junit5"
        )
    }
}