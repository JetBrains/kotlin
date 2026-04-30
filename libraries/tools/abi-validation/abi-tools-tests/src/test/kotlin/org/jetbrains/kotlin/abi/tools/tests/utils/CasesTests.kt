/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.tests.utils

import org.jetbrains.kotlin.abi.tools.AbiFilters
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.streams.toList
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Base class for cases-based tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class CasesTestBase(private val casesDir: String, private val binariesType: BinariesType) {
    lateinit var session: BuildSession

    @BeforeAll
    fun init() {
        session = BuildSession()
    }

    @TestFactory
    fun cases(): List<DynamicNode> {
        val generator = CasesTestGenerator(casesDir, session, binariesType)
        return generator.generateTests()
    }

    @AfterAll
    fun cleanup() {
        session.close()
    }
}

/**
 * Generates dynamic JUnit 5 tests by scanning a resource directory.
 *
 * For each subdirectory in the resource root, finds `.txt` files matching the pattern `<caseName><variant>.txt`.
 * For each such file, looks for a corresponding `filters<variant>.txt` file to parse into [AbiFilters].
 * Creates two subtests per file: one for Original and one for Embeddable ABI tools.
 *
 * @param casesDir the root directory in resources to scan (e.g. "cases")
 */
class CasesTestGenerator(private val casesDir: String, private val session: BuildSession, private val binariesType: BinariesType) {

    private val overwriteExpectedOutput = System.getProperty("overwrite.output")?.toBoolean() ?: false
    private val initDumps = System.getProperty("init.dumps")?.toBoolean() ?: false

    private val baseResourcePath = File("src/test/resources")

    private val tempDir = createTempDirectory("cases-tests")
    private val resourceDir = tempDir.resolve("resources").also {
        it.toFile().mkdirs()
        copyResourcesToDir(casesDir, it)
    }

    private val execDir = tempDir.resolve("exec").also { it.toFile().mkdirs() }

    fun generateTests(): List<DynamicNode> {
        return Files.list(resourceDir)
            .filter { Files.isDirectory(it) }
            .sorted()
            .flatMap { caseDir -> createContainersForCase(caseDir).stream() }
            .toList()
    }

    private fun createContainersForCase(caseDir: Path): List<DynamicNode> {
        val caseName = caseDir.name

        // Collect variants from dump files matching the pattern: <caseName><variant>.txt
        val variantsFromDumps = Files.list(caseDir)
            .filter { Files.isRegularFile(it) }
            .filter { file ->
                val fileName = file.name
                fileName.startsWith(caseName) && fileName.endsWith(".txt")
            }
            .map { it.name.removePrefix(caseName).removeSuffix(".txt") }
            .toList()
            .toSet()

        // Collect variants from filter files matching the pattern: filters<variant>.txt
        val variantsFromFilters = Files.list(caseDir)
            .filter { Files.isRegularFile(it) }
            .filter { file ->
                val fileName = file.name
                fileName.startsWith("filters") && fileName.endsWith(".txt")
            }
            .map { it.name.removePrefix("filters").removeSuffix(".txt") }
            .toList()
            .filter { it.isNotEmpty() } // exclude empty variant from filter-only discovery
            .toSet()

        val allVariants = (variantsFromDumps + variantsFromFilters).sorted()

        return allVariants.map { variant ->
            val filters = loadFilters(caseDir, variant)
            createContainerForVariant(caseDir, variant, filters)
        }
    }

    private fun loadFilters(subDir: Path, variant: String): AbiFilters {
        if (variant.isEmpty()) {
            // No variant — check for filters.txt (optional)
            val filtersFile = subDir.resolve("filters.txt")
            return if (filtersFile.exists()) parseFilters(filtersFile) else AbiFilters.EMPTY
        }

        // Non-empty variant — filters<variant>.txt must exist
        val filtersFile = subDir.resolve("filters$variant.txt")
        check(filtersFile.exists()) {
            "Filter file 'filters$variant.txt' not found for variant '$variant' in directory '${subDir.name}'"
        }
        return parseFilters(filtersFile)
    }

    private fun createContainerForVariant(
        caseDir: Path,
        variant: String,
        filters: AbiFilters,
    ): DynamicNode {
        val caseName = caseDir.name
        val fullName = "$caseName$variant"

        val kotlinOutputDir = when (binariesType) {
            BinariesType.JVM -> lazy {
                val outputDir = execDir.resolve(fullName).resolve("kotlin-output")
                val sources = caseDir.collectKtFiles()
                if (sources.isNotEmpty()) {
                    session.compileKotlinJvm(sources, outputDir, compileClasspath = listOf(caseDir))
                }
                outputDir
            }
            BinariesType.KLIB -> lazy {
                // while build tools API yet doesn't support klib compilation, we use precompiled klibs from resources
                caseDir.resolve("klib")
            }
        }

        val tests = listOf(
            "Original" to abiToolsOriginal,
            "Embeddable" to abiToolsEmbeddable,
        ).map { (variantLabel, abiTools) ->
            DynamicTest.dynamicTest("$fullName [$variantLabel]") {
                verifyDump(abiTools, kotlinOutputDir, caseDir, variant, filters)
            }
        }

        return DynamicContainer.dynamicContainer(fullName, tests)
    }

    private fun verifyDump(
        abiTools: AbiTools,
        kotlinOutputDir: Lazy<Path>,
        caseDir: Path,
        variant: String,
        filters: AbiFilters,
    ) {
        val caseName = caseDir.name
        val fullName = caseName + variant

        val actualDump = when (binariesType) {
            BinariesType.JVM -> {
                val inputFiles = kotlinOutputDir.value.walk().plus(caseDir.walk())
                    .filter { it.extension == "class" || it.extension == "jar" }
                    .map { it.toFile() }
                    .toList()
                check(inputFiles.isNotEmpty()) { "No class or jar files found for JVM test case: $caseName" }
                buildString {
                    abiTools.printJvmDump(this, inputFiles, filters)
                }
            }
            BinariesType.KLIB -> {
                val dump = abiTools.extractKlibAbi(kotlinOutputDir.value.toFile(), filters = filters)
                buildString {
                    dump.print(this)
                }
            }
        }

        val expectedDumpFile = caseDir.resolve("$fullName.txt")

        val fileToWrite = baseResourcePath.resolve("$casesDir/$caseName/$fullName.txt").toPath()

        if (!expectedDumpFile.exists()) {
            if (!initDumps) {
                fail("Expected dump file did not exist for $caseName$variant. To generate it automatically, specify the `init.dumps=true` property, or manually create a $fileToWrite file.")
            } else {
                fileToWrite.parent.createDirectories()
                fileToWrite.writeText(actualDump)
                fail("Expected dump file did not exist for $caseName$variant. Generating: $fileToWrite")
            }
        } else {
            assertEqualsToFile(expectedDumpFile, actualDump)
        }
    }

    private fun assertEqualsToFile(expectedFile: Path, actual: CharSequence) {
        val actualText = actual.trimTrailingWhitespacesAndAddNewlineAtEOF()
        val expectedText = expectedFile.readText().trimTrailingWhitespacesAndAddNewlineAtEOF()

        if (overwriteExpectedOutput && expectedText != actualText) {
            expectedFile.writeText(actualText)
            assertEquals(expectedText, actualText, "Actual data differs from file content: ${expectedFile.name}, rewriting")
        }

        assertEquals(
            expectedText,
            actualText,
            "Actual data differs from file content: ${expectedFile.name}\nTo overwrite the expected API rerun with -Doverwrite.output=true parameter\n"
        )
    }
}

/**
 * Parses a filters file into [AbiFilters].
 *
 * The file format uses sections prefixed with a keyword:
 * ```
 * includedClasses: pattern1, pattern2
 * excludedClasses: pattern1, pattern2
 * includedAnnotatedWith: pattern1, pattern2
 * excludedAnnotatedWith: pattern1, pattern2
 * ```
 *
 * Empty lines and lines starting with `#` are ignored.
 */
fun parseFilters(filtersFile: Path): AbiFilters {
    val includedClasses = mutableSetOf<String>()
    val excludedClasses = mutableSetOf<String>()
    val includedAnnotatedWith = mutableSetOf<String>()
    val excludedAnnotatedWith = mutableSetOf<String>()

    filtersFile.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .forEach { line ->
            val colonIndex = line.indexOf(':')
            check(colonIndex >= 0) { "Invalid filter line in ${filtersFile.name}: '$line'" }
            val key = line.substring(0, colonIndex).trim()
            val values = line.substring(colonIndex + 1).split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            when (key) {
                "includedClasses" -> includedClasses.addAll(values)
                "excludedClasses" -> excludedClasses.addAll(values)
                "includedAnnotatedWith" -> includedAnnotatedWith.addAll(values)
                "excludedAnnotatedWith" -> excludedAnnotatedWith.addAll(values)
                else -> error("Unknown filter key '$key' in ${filtersFile.name}")
            }
        }

    return AbiFilters(includedClasses, excludedClasses, includedAnnotatedWith, excludedAnnotatedWith)
}

private fun CharSequence.trimTrailingWhitespacesAndAddNewlineAtEOF(): String =
    this.lineSequence().joinToString(separator = "\n") { it.trimEnd() }.let {
        if (it.endsWith("\n")) it else it + "\n"
    }

enum class BinariesType {
    JVM,
    KLIB
}
