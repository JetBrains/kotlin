/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests.integration

import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.jetbrains.kotlin.backend.konan.tests.integration.utils.*
import org.jetbrains.kotlin.backend.konan.tests.integration.utils.IntegrationTestReport.Issue
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.*
import org.jetbrains.kotlin.konan.test.blackbox.support.copyNativeHomeProperty
import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.IndexerResult
import org.jetbrains.kotlin.native.interop.tool.ToolConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Integration test that generates ObjC headers using K1 and K2 header generators,
 * then compares and validates the outputs.
 *
 * Implementation
 * 1. Load generator classes via reflection at runtime when the classes are available on the classpath
 * 2. Generate K1 and K2 headers for each test library
 * 3. Compile and index the headers using the [org.jetbrains.kotlin.native.interop.indexer.Indexer]
 * 4. Compare and build report
 *
 * Why reflection for creating instances of header generators?
 * We can avoid potentially reflection with 2 options:
 * 1. Using gradle testImplementation and using generator classes directory
 * Adding testImplementation(aa) and testImplementation(k2) triggers gradle circular dependency compile time error:
 * ```
 * objcexport-header-generator (tests) → objcexport-header-generator-k1 (tests) → objcexport-header-generator (tests) → ...
 * objcexport-header-generator (tests) → objcexport-header-generator-aa (tests) → objcexport-header-generator (tests) → ...
 * ```
 * 2. Extracting [Fe10HeaderGeneratorImpl] and [AnalysisApiHeaderGenerator] into shared module
 * This would require massive refactoring, both implementations depend on lots of K1 and AA generator classes and functions
 *
 * So solution is to add both dependencies in classpath and load them via reflection
 * ```
 * classpath += k1TestRuntimeClasspath
 * classpath += analysisApiRuntimeClasspath
 * ```
 */
class ObjCExportIntegrationTest {

    /**
     * Currently failing is disabled, since we have unresolved K1/K2 issues
     */
    private val failOnIssues = false
    private var files = IntegrationTempFiles(integrationModuleName)

    init {
        if (HostManager.host.family.isAppleFamily) {
            copyNativeHomeProperty()
            ToolConfig(
                userProvidedTargetName = HostManager.hostName,
                flavor = KotlinPlatform.NATIVE,
                propertyOverrides = emptyMap(),
                konanDataDir = null
            ).loadLibclang()
        }
    }

    @BeforeEach
    fun before() {
        assumeTrue(HostManager.host.family.isAppleFamily)
        initIndexerUtils()
        files = IntegrationTempFiles(integrationModuleName)
        assertTrue(File(appleSdkPath).exists(), "Apple SDK not found at `${appleSdkPath}`")
    }

    @AfterEach
    fun cleanup() {
        disposeIndexerUtils()
    }

    @Test
    fun `compare kotlinx-datetime`() {
        val report = generateHeadersAndBuildReport(testLibraryKotlinxDatetime)
        if (failOnIssues && report.hasIssues) {
            error(report.toString())
        }
    }

    @Test
    fun `compare kotlinx-coroutines`() {
        val report = generateHeadersAndBuildReport(testLibraryKotlinxCoroutines)
        if (failOnIssues && report.hasIssues) {
            error(report.toString())
        }
    }

    @Test
    fun `compare kotlinx-atomic-fu`() {
        val report = generateHeadersAndBuildReport(testLibraryAtomicFu)
        if (failOnIssues && report.hasIssues) {
            error(report.toString())
        }
    }

    @Test
    fun `compare kotlinx-serialization-core`() {
        val report = generateHeadersAndBuildReport(testLibraryKotlinxSerializationCore)
        if (failOnIssues && report.hasIssues) {
            error(report.toString())
        }
    }

    @Test
    fun `compare kotlinx-serialization-json`() {
        val report = generateHeadersAndBuildReport(testLibraryKotlinxSerializationJson)
        if (failOnIssues && report.hasIssues) {
            error(report.toString())
        }
    }

    @Test
    fun `compare combined dependencies`() {
        val report = generateHeadersAndBuildReport(
            "combined",
            listOf(
                testLibraryKotlinxDatetime,
                testLibraryKotlinxCoroutines,
                testLibraryAtomicFu,
                testLibraryKotlinxSerializationCore,
                testLibraryKotlinxSerializationJson
            )
        )
        if (failOnIssues && report.hasIssues) {
            error(report.toString())
        }
    }

    private fun generateHeadersAndBuildReport(library: Path): IntegrationTestReport {
        return generateHeadersAndBuildReport(library.name, listOf(library))
    }

    private fun generateHeadersAndBuildReport(name: String, libraries: List<Path>): IntegrationTestReport {
        val configuration = HeaderGenerator.Configuration(
            dependencies = libraries,
            exportedDependencies = libraries.toSet(),
            frameworkName = "",
            withObjCBaseDeclarationStubs = false
        )
        val k1Header = generateHeader(
            configuration = configuration,
            createK1HeaderGenerator()
        )

        val k2Header = generateHeader(
            configuration = configuration,
            createK2HeaderGenerator()
        )

        return compileIndexAndBuildReport(name, k1Header, k2Header)
    }

    private fun generateHeader(
        configuration: HeaderGenerator.Configuration = HeaderGenerator.Configuration(),
        generator: HeaderGenerator,
    ): String {
        val root = files.directory
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        return generator.generateHeaders(root, configuration).toString()
    }

    private fun compileIndexAndBuildReport(name: String, k1Header: String, k2Header: String): IntegrationTestReport {

        val baseHeader = files.file("Base.h", baseObjCTypes.trimIndent())

        val k1Index = try {
            compileAndIndex(k1Header, baseHeader)
        } catch (e: Throwable) {
            return IntegrationTestReport(name, listOf(Issue.FailedK1Compilation(e.message, k1Header, e)))
        }
        val k2Index = try {
            compileAndIndex(k2Header, baseHeader)
        } catch (e: Throwable) {
            return IntegrationTestReport(name, listOf(Issue.FailedK2CompilationK2(e.message, k2Header, e)))
        }

        return IntegrationTestReport(
            name, compareProtocolsOrClasses(k1Index, k2Index)
        )
    }

    private fun compileAndIndex(header: String, baseHeader: File): IndexerResult {
        val headerFile = files.file("$integrationModuleName.h", header)
        return compileAndIndex(
            listOf(baseHeader, headerFile), files, appleSdkPath, appleFrameworkPath
        )
    }
}