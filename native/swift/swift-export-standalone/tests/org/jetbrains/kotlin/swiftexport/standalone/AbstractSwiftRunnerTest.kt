/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSwiftExportTest
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.callCompilerWithoutOutputInterceptor
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.*
import kotlin.streams.asSequence
import kotlin.test.assertSame

enum class InputModuleKind {
    Source, Binary
}

abstract class AbstractSourceBasedSwiftRunnerTest : AbstractSwiftRunnerTest(
    renderDocComments = true,
    inputModuleKind = InputModuleKind.Source,
)

abstract class AbstractKlibBasedSwiftRunnerTest : AbstractSwiftRunnerTest(
    renderDocComments = false,
    inputModuleKind = InputModuleKind.Binary,
)

abstract class AbstractSwiftRunnerTest(
    private val renderDocComments: Boolean,
    private val inputModuleKind: InputModuleKind,
) : AbstractNativeSwiftExportTest() {

    private val tmpdir = FileUtil.createTempDirectory("SwiftExportIntegrationTests", null, false)

    override fun runCompiledTest(
        testPathFull: File,
        testCase: TestCase,
        swiftExportOutput: SwiftExportModule,
        swiftModule: TestCompilationArtifact.Swift.Module,
        kotlinBinaryLibrary: TestCompilationArtifact.BinaryLibrary,
    ) {
        assertSame(0, swiftExportOutput.dependencies.count(), "should produce module without children")

        val files = swiftExportOutput.files

        val expectedFiles = testPathFull.toPath() / "golden_result/"
        val expectedSwift = if (!renderDocComments && (expectedFiles / "result.no_comments.swift").exists()) {
            expectedFiles / "result.no_comments.swift"
        } else {
            expectedFiles / "result.swift"
        }
        val expectedCHeader = expectedFiles / "result.h"
        val expectedKotlinBridge = expectedFiles / "result.kt"

        KotlinTestUtils.assertEqualsToFile(expectedSwift, files.swiftApi.readText())
        KotlinTestUtils.assertEqualsToFile(expectedCHeader, files.cHeaderBridges.readText())
        KotlinTestUtils.assertEqualsToFile(expectedKotlinBridge, files.kotlinBridges.readText())
    }

    override fun constructSwiftInput(testPathFull: File): InputModule {
        val moduleRoot = testPathFull.toPath() / "input_root/"

        return when (inputModuleKind) {
            InputModuleKind.Source -> {
                InputModule.Source(
                    path = moduleRoot,
                    name = "main"
                )
            }
            InputModuleKind.Binary -> {
                InputModule.Binary(
                    path = compileToNativeKLib(moduleRoot),
                    name = "main"
                )
            }
        }
    }

    override fun constructSwiftExportConfig(testPathFull: File): SwiftExportConfig {
        val unsupportedTypeStrategy = when (inputModuleKind) {
            InputModuleKind.Source -> ErrorTypeStrategy.SpecialType
            InputModuleKind.Binary -> ErrorTypeStrategy.Fail
        }

        val errorTypeStrategy = when (inputModuleKind) {
            InputModuleKind.Source -> ErrorTypeStrategy.SpecialType
            InputModuleKind.Binary -> ErrorTypeStrategy.Fail
        }

        val defaultConfig: Map<String, String> = mapOf(
            SwiftExportConfig.STABLE_DECLARATIONS_ORDER to "true",
            SwiftExportConfig.RENDER_DOC_COMMENTS to (if (renderDocComments) "true" else "false"),
            SwiftExportConfig.BRIDGE_MODULE_NAME to SwiftExportConfig.DEFAULT_BRIDGE_MODULE_NAME,
        )

        var unsupportedDeclarationReporterKind = UnsupportedDeclarationReporterKind.Silent
        val discoveredConfig = (testPathFull.toPath() / "config.properties").takeIf { it.exists() }?.let { configPath ->
            Properties().apply { load(configPath.toFile().inputStream()) }.let { properties ->
                properties.propertyNames().asSequence()
                    .filterIsInstance<String>()
                    .associateWith { properties.getProperty(it) }
                    .filter { (key, value) -> when {
                        key == "unsupportedDeclarationsReporterKind" -> {
                            UnsupportedDeclarationReporterKind.entries
                                .singleOrNull { it.name.lowercase() == value.lowercase() }
                                ?.let { unsupportedDeclarationReporterKind = it }
                            false
                        }
                        else -> true
                    } }
            }
        } ?: emptyMap()

        val config = defaultConfig + discoveredConfig

        return SwiftExportConfig(
            settings = config,
            logger = createDummyLogger(),
            distribution = Distribution(KonanHome.konanHomePath),
            errorTypeStrategy = errorTypeStrategy,
            unsupportedTypeStrategy = unsupportedTypeStrategy,
            outputPath = tmpdir.toPath(),
            unsupportedDeclarationReporterKind = unsupportedDeclarationReporterKind,
        )
    }

    override fun collectKotlinFiles(testPathFull: File): List<File> =
        (testPathFull.toPath() / "input_root").toFile().walk().filter { it.extension == "kt" }.map { testPathFull.resolve(it) }.toList()
}

internal fun AbstractNativeSimpleTest.compileToNativeKLib(kLibSourcesRoot: Path): Path {
    val ktFiles = Files.walk(kLibSourcesRoot).asSequence().filter { it.extension == "kt" }.toList()
    val testKlib = KtTestUtil.tmpDir("testLibrary").resolve("library.klib").toPath()

    val arguments = buildList {
        ktFiles.mapTo(this) { it.absolutePathString() }
        addAll(listOf("-produce", "library"))
        addAll(listOf("-output", testKlib.absolutePathString()))
    }

    // Avoid creating excessive number of classloaders
    val classLoader = testRunSettings.get<KotlinNativeClassLoader>().classLoader
    val compileResult = callCompilerWithoutOutputInterceptor(arguments.toTypedArray(), classLoader)

    check(compileResult.exitCode == ExitCode.OK) {
        "Compilation error: $compileResult"
    }

    return testKlib
}

private object KonanHome {
    private const val KONAN_HOME_PROPERTY_KEY = "kotlin.internal.native.test.nativeHome"

    val konanHomePath: String
        get() = System.getProperty(KONAN_HOME_PROPERTY_KEY)
            ?: error("Missing System property: '$KONAN_HOME_PROPERTY_KEY'")
}
