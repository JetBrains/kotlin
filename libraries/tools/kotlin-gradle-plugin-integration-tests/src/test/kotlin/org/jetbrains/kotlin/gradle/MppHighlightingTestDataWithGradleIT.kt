/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.capitalize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.nio.file.Files
import java.util.stream.Stream
import kotlin.io.path.appendText
import kotlin.streams.asStream
import kotlin.streams.toList

@DisplayName("Check Multiplatform IDE highlighting projects")
@MppGradlePluginTests
internal class MppHighlightingTestDataWithGradleIT : KGPBaseTest() {

    @GradleWithMppHighlightingTest
    fun runTest(
        gradleVersion: GradleVersion,
        cliCompiler: CliCompiler,
        testDataDir: File,
        sourceRoots: List<TestCaseSourceRoot>
    ) {
        project("mpp-source-set-hierarchy-analysis", gradleVersion) {
            val expectedErrorsPerSourceSetName = sourceRoots.associate { sourceRoot ->
                sourceRoot.kotlinSourceSetName to testDataDir.resolve(sourceRoot.directoryName).walkTopDown()
                    .filter { it.extension == "kt" }
                    .map { CodeWithErrorInfo.parse(it.readText()) }.toList()
                    .flatMap { it.errorInfo }
            }

            // put sources into project dir:
            sourceRoots.forEach { sourceRoot ->
                val sourceSetDir = projectPath.resolve(sourceRoot.gradleSrcDir).toFile()
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
                            """        
                            |            create("${sourceRoot.kotlinSourceSetName}") {
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

            buildGradleKts.appendText("\n" + scriptCustomization)

            val tasks = sourceRoots.map {
                "compile" + it.kotlinSourceSetName.capitalize() + "KotlinMetadata"
            }

            if (expectedErrorsPerSourceSetName.values.all { it.all(ErrorInfo::isAllowedInCli) }) {
                build(*tasks.toTypedArray())
            } else {
                buildAndFail(*tasks.toTypedArray())
            }
        }
    }

    data class TestCaseSourceRoot(
        val directoryName: String,
        val qualifiedNameParts: Iterable<String>,
        val dependencies: Iterable<String>,
    ) {
        companion object {
            private const val TEST_SOURCE_ROOT_SUFFIX = "tests"
            private const val COMMON_SOURCE_ROOT_NAME = "common"

            fun parse(directoryName: String): TestCaseSourceRoot {
                val parts = directoryName.split("_")

                val deps = parts.map { it.removeSurrounding("dep(", ")") }
                    .filterIndexed { index, it -> it != parts[index] }
                    .map { it.split("-").joinToString("") }

                val nameParts = parts.dropLast(deps.size)

                val platformIndex = when (nameParts.size) {
                    1 -> 0
                    else -> if (nameParts.last() == TEST_SOURCE_ROOT_SUFFIX) 0 else 1
                }

                val additionalDependencies = mutableListOf<String>().apply {
                    if (nameParts[platformIndex] != COMMON_SOURCE_ROOT_NAME)
                        add(partsToQualifiedName(nameParts.take(platformIndex) + COMMON_SOURCE_ROOT_NAME + nameParts.drop(platformIndex + 1)))
                    if (nameParts.last() == TEST_SOURCE_ROOT_SUFFIX)
                        add(partsToQualifiedName(nameParts.dropLast(1)))
                }

                return TestCaseSourceRoot(directoryName, nameParts, deps + additionalDependencies)
            }

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
                "NO_ACTUAL_FOR_EXPECT", null -> true
                else -> false
            }
    }

    internal enum class CliCompiler(
        val targets: List<String>
    ) {
        K2METADATA(listOf("jvm", "js")),
        NATIVE(listOf("linuxX64", "linuxArm64"));
    }

    class GradleAndMppHighlightingProvider : GradleArgumentsProvider() {
        private val testDataRoot = File("../../../idea/testData/multiModuleHighlighting/multiplatform")

        private val bannedDependencies = setOf("fulljdk", "stdlib", "coroutines")

        private fun isTestSuiteValidForCommonCode(
            testDataDir: File,
            sourceRoots: List<TestCaseSourceRoot>
        ): Boolean = when {
            sourceRoots.any { bannedDependencies.intersect(it.dependencies.toSet()).isNotEmpty() } -> false
            // Java sources can't be used in intermediate source sets
            testDataDir.walkTopDown().any { it.extension == "java" } -> false
            // Cannot test !CHECK_HIGHLIGHTING in CLI
            testDataDir.walkTopDown().filter { it.isFile }.any { "!CHECK_HIGHLIGHTING" in it.readText() } -> false
            else -> true
        }

        private val testData = testDataRoot
            .walkTopDown()
            .maxDepth(1)
            .filter { it.isDirectory && Files.newDirectoryStream(it.toPath()).use { stream -> stream.toList().isNotEmpty() } }
            .map { testDataDir ->
                Pair(
                    testDataDir,
                    Files.newDirectoryStream(testDataDir.toPath()).use { stream ->
                        stream.map { TestCaseSourceRoot.parse(it.fileName.toString()) }
                    }
                )
            }
            .filter {
                isTestSuiteValidForCommonCode(it.first, it.second)
            }
            .toList()

        override fun provideArguments(
            context: ExtensionContext
        ): Stream<out Arguments> {
            val gradleVersions = super.provideArguments(context).map { it.get().first() as GradleVersion }.toList()

            return gradleVersions
                .flatMap { gradleVersion ->
                    CliCompiler.entries.flatMap { cliCompiler ->
                        testData.map {
                            Arguments.of(gradleVersion, cliCompiler, it.first, it.second)
                        }
                    }
                }
                .asSequence()
                .asStream()
        }
    }

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    @GradleTestVersions
    @ParameterizedTest(name = "{0} - {1} - {2} - {3}: {displayName}")
    @ArgumentsSource(GradleAndMppHighlightingProvider::class)
    annotation class GradleWithMppHighlightingTest
}
