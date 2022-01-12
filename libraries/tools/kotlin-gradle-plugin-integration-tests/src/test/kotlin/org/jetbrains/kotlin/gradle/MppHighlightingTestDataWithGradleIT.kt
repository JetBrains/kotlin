/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleArgumentsProvider
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.enableCacheRedirector
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.streams.toList

const val testSourceRootSuffix = "tests"

@MppGradlePluginTests
@EnabledOnOs(OS.LINUX)
class MppHighlightingTestDataWithGradleIT : BaseGradleIT() {

    private val buildScriptCustomizationMarker = "// customized content below"

    @BeforeEach
    fun before() {
        super.setUp()
    }

    @AfterEach
    fun after() {
        super.tearDown()
    }

    @GradleTestVersions
    @ParameterizedTest(name = "{3}: {0}")
    @ArgumentsSource(ArgumentsProvider::class)
    fun runTestK2NativeCli(
        @Suppress("UNUSED_PARAMETER") // used for parameter string representation in test output
        testCaseName: String,
        testDataDir: File,
        sourceRoots: List<TestCaseSourceRoot>,
        gradleVersion: GradleVersion
    ) = doTest(CliCompiler.NATIVE, prepareProject(gradleVersion), testDataDir, sourceRoots)

    @GradleTestVersions
    @ParameterizedTest(name = "{3}: {0}")
    @ArgumentsSource(ArgumentsProvider::class)
    fun runTestK2MetadataCli(
        @Suppress("UNUSED_PARAMETER") // used for parameter string representation in test output
        testCaseName: String,
        testDataDir: File,
        sourceRoots: List<TestCaseSourceRoot>,
        gradleVersion: GradleVersion
    ) = doTest(CliCompiler.K2METADATA, prepareProject(gradleVersion), testDataDir, sourceRoots)

    private fun prepareProject(gradleVersion: GradleVersion): Project {
        val project = Project("mpp-source-set-hierarchy-analysis", gradleVersion)
        project.setupWorkingDir(false)
        project.gradleSettingsScript().modify { it.lines().filter { !it.startsWith("include") }.joinToString("\n") }
        project.projectDir.resolve("src").deleteRecursively()
        project.gradleBuildScript().modify { line ->
            line.lines().dropLastWhile { it != buildScriptCustomizationMarker }.joinToString("\n")
        }

        project.projectDir.toPath().enableCacheRedirector()

        return project
    }

    private fun doTest(cliCompiler: CliCompiler, project: Project, testDataDir: File, sourceRoots: List<TestCaseSourceRoot>) =
        with(project) {
            val expectedErrorsPerSourceSetName = sourceRoots.associate { sourceRoot ->
                sourceRoot.kotlinSourceSetName to testDataDir.resolve(sourceRoot.directoryName).walkTopDown()
                    .filter { it.extension == "kt" }
                    .map { CodeWithErrorInfo.parse(it.readText()) }.toList()
                    .flatMap { it.errorInfo }
            }

            // put sources into project dir:
            sourceRoots.forEach { sourceRoot ->
                val sourceSetDir = projectDir.resolve(sourceRoot.gradleSrcDir)
                testDataDir.resolve(sourceRoot.directoryName).copyRecursively(sourceSetDir)
                sourceSetDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    file.modify { CodeWithErrorInfo.parse(file.readText()).code }
                }
            }

            // create Gradle Kotlin source sets for project roots:
            val scriptCustomization = buildString {
                appendLine()
                appendLine("kotlin {\n    sourceSets {")
                sourceRoots.forEach { sourceRoot ->
                    if (sourceRoot.kotlinSourceSetName != "commonMain") {
                        appendLine(
                            """        create("${sourceRoot.kotlinSourceSetName}") {
                          |            dependsOn(getByName("commonMain"))
                          |            listOf(${cliCompiler.targets.joinToString { "$it()" }}).forEach { 
                          |                it.compilations["main"].defaultSourceSet.dependsOn(this@create) 
                          |            }
                          |        }
                          |    
                        """.trimMargin()
                        )
                    } else {
                        appendLine("    // commonMain source set used for common module")
                    }
                }

                // Add dependencies using dependsOn:
                sourceRoots.forEach { sourceRoot ->
                    sourceRoot.dependencies.forEach { dependency ->
                        sourceRoots.find { it.qualifiedName == dependency }?.let { depSourceRoot ->
                            val depSourceSet = depSourceRoot.kotlinSourceSetName
                            appendLine("""        getByName("${sourceRoot.kotlinSourceSetName}").dependsOn(getByName("$depSourceSet"))""")
                        }
                    }
                }
                appendLine("    }\n}")
            }

            gradleBuildScript().appendText("\n" + scriptCustomization)

            val tasks = sourceRoots.map { "compile" + it.kotlinSourceSetName.capitalize() + "KotlinMetadata" }

            build(*tasks.toTypedArray()) {
                if (expectedErrorsPerSourceSetName.values.all { it.all(ErrorInfo::isAllowedInCli) }) {
                    assertSuccessful()
                } else {
                    assertFailed() // TODO: check the exact error message in the output, not just that the build failed
                }
            }
        }

    class ArgumentsProvider : GradleArgumentsProvider() {
        private val testDataRoot =
            File("../../../idea/testData/multiModuleHighlighting/multiplatform")

        override fun provideArguments(
            context: ExtensionContext
        ): Stream<out Arguments> {
            val gradleVersions = super.provideArguments(context).map { it.get().first() as GradleVersion }.toList()

            val result = gradleVersions
                .flatMap { gradleVersion ->
                    testDataRoot.listFiles()!!
                        .filter { it.isDirectory }
                        .mapNotNull { testDataDir ->
                            val testDataSourceRoots = checkNotNull(testDataDir.listFiles())
                            val sourceRoots = testDataSourceRoots.map { TestCaseSourceRoot.parse(it.name) }
                            arrayOf(testDataDir.name, testDataDir, sourceRoots)
                                .takeIf { isTestSuiteValidForCommonCode(testDataDir, sourceRoots) }
                        }.map { arrayOf(*it, gradleVersion) }
                }
                .asSequence()
                .map {
                    Arguments.of(*it)
                }
                .asStream()

            return result
        }

        private val bannedDependencies = setOf("fulljdk", "stdlib", "coroutines")

        private fun isTestSuiteValidForCommonCode(testDataDir: File, sourceRoots: List<TestCaseSourceRoot>): Boolean {
            sourceRoots.forEach {
                val bannedDepsFound = bannedDependencies.intersect(it.dependencies.toSet())
                if (bannedDepsFound.isNotEmpty())
                    return false
            }

            // Java sources can't be used in intermediate source sets
            if (testDataDir.walkTopDown().any { it.extension == "java" })
                return false

            // Cannot test !CHECK_HIGHLIGHTING in CLI
            if (testDataDir.walkTopDown().filter { it.isFile }.any { "!CHECK_HIGHLIGHTING" in it.readText() })
                return false

            return true
        }
    }

    data class TestCaseSourceRoot(
        val directoryName: String,
        val qualifiedNameParts: Iterable<String>,
        val dependencies: Iterable<String>,
    ) {
        companion object {
            fun parse(directoryName: String): TestCaseSourceRoot {
                val parts = directoryName.split("_")

                val deps = parts.map { it.removeSurrounding("dep(", ")") }
                    .filterIndexed { index, it -> it != parts[index] }
                    .map { it.split("-").joinToString("") }

                val nameParts = parts.dropLast(deps.size)

                val platformIndex = when (nameParts.size) {
                    1 -> 0
                    else -> if (nameParts.last() == testSourceRootSuffix) 0 else 1
                }

                val additionalDependencies = mutableListOf<String>().apply {
                    if (nameParts[platformIndex] != commonSourceRootName)
                        add(partsToQualifiedName(nameParts.take(platformIndex) + commonSourceRootName + nameParts.drop(platformIndex + 1)))
                    if (nameParts.last() == testSourceRootSuffix)
                        add(partsToQualifiedName(nameParts.dropLast(1)))
                }

                return TestCaseSourceRoot(directoryName, nameParts, deps + additionalDependencies)
            }

            private const val commonSourceRootName = "common"

            private fun partsToQualifiedName(parts: Iterable<String>) = parts.joinToString("")
        }

        val qualifiedName
            get() = partsToQualifiedName(qualifiedNameParts)

        val kotlinSourceSetName
            get() = "intermediate${qualifiedName.capitalize()}"

        val gradleSrcDir
            get() = "src/$kotlinSourceSetName/kotlin"
    }

    private class CodeWithErrorInfo(
        val code: String,
        val errorInfo: Iterable<ErrorInfo>
    ) {
        companion object {
            private val errorRegex = "<error(?: descr=\"\\[(.*?)] (.*?)\")?>".toRegex()
            private val errorTailRegex = "</error>".toRegex()

            fun parse(code: String): CodeWithErrorInfo {
                fun parseMatch(match: MatchResult): ErrorInfo {
                    val (_, errorKind, description) = match.groupValues
                    return ErrorInfo(errorKind.takeIf { it.isNotEmpty() }, description.takeIf { it.isNotEmpty() })
                }

                val matches = errorRegex.findAll(code).map(::parseMatch).toList()
                return CodeWithErrorInfo(code.replace(errorRegex, "").replace(errorTailRegex, ""), matches)
            }
        }
    }

    private data class ErrorInfo(
        val expectedErrorKind: String?,
        val expectedErrorMessage: String?
    ) {
        val isAllowedInCli
            get() = when (expectedErrorKind) {
                "NO_ACTUAL_FOR_EXPECT", "ACTUAL_WITHOUT_EXPECT", null /*TODO are some nulls better than others?*/ -> true
                else -> false
            }
    }

    private enum class CliCompiler(val targets: List<String>) {
        K2METADATA(listOf("jvm", "js")), NATIVE(listOf("linuxX64", "linuxArm64"))
    }
}
