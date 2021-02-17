/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class MppHighlightingTestDataWithGradleIT : BaseGradleIT() {

    @Test
    fun runTestK2NativeCli() = doTest(CliCompiler.NATIVE)

    @Test
    fun runTestK2MetadataCli() = doTest(CliCompiler.K2METADATA)

    @Before
    fun cleanup() {
        project.setupWorkingDir()
        project.gradleSettingsScript().modify { it.lines().filter { !it.startsWith("include") }.joinToString("\n") }
        project.projectDir.resolve("src").deleteRecursively()
        project.gradleBuildScript().modify { line ->
            line.lines().dropLastWhile { it != buildScriptCustomizationMarker }.joinToString("\n")
        }
    }

    private val project by lazy { Project("mpp-source-set-hierarchy-analysis") }

    private fun doTest(cliCompiler: CliCompiler) = with(project) {
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
            appendln()
            appendln("kotlin {\n    sourceSets {")
            sourceRoots.forEach { sourceRoot ->
                if (sourceRoot.kotlinSourceSetName != "commonMain") {
                    appendln(
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
                    appendln("    // commonMain source set used for common module")
                }
            }

            // Add dependencies using dependsOn:
            sourceRoots.forEach { sourceRoot ->
                sourceRoot.dependencies.forEach { dependency ->
                    sourceRoots.find { it.qualifiedName == dependency }?.let { depSourceRoot ->
                        val depSourceSet = depSourceRoot.kotlinSourceSetName
                        appendln("""        getByName("${sourceRoot.kotlinSourceSetName}").dependsOn(getByName("$depSourceSet"))""")
                    }
                }
            }
            appendln("    }\n}")
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

    companion object {
        private val testDataRoot =
            File("../../../idea/testData/multiModuleHighlighting/multiplatform")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testData() = testDataRoot.listFiles()!!.filter { it.isDirectory }.mapNotNull { testDataDir ->
            val testDataSourceRoots = checkNotNull(testDataDir.listFiles())
            val sourceRoots = testDataSourceRoots.map { TestCaseSourceRoot.parse(it.name) }

            arrayOf(testDataDir.name, testDataDir, sourceRoots).takeIf { isTestSuiteValidForCommonCode(testDataDir, sourceRoots) }
        }

        private val bannedDependencies = setOf("fulljdk", "stdlib", "coroutines")

        const val testSourceRootSuffix = "tests"

        private const val buildScriptCustomizationMarker = "// customized content below"

        private fun isTestSuiteValidForCommonCode(testDataDir: File, sourceRoots: List<TestCaseSourceRoot>): Boolean {
            sourceRoots.forEach {
                val bannedDepsFound = bannedDependencies.intersect(it.dependencies)
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

    @Suppress("unused") // used for parameter string representation in test output
    @Parameterized.Parameter(0)
    lateinit var testCaseName: String

    @Parameterized.Parameter(1)
    lateinit var testDataDir: File

    @Parameterized.Parameter(2)
    lateinit var sourceRoots: List<TestCaseSourceRoot>
}
