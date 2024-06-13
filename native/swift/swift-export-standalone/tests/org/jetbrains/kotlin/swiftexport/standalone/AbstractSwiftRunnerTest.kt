/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSwiftExportTest
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import kotlin.io.path.*
import kotlin.test.assertSame

abstract class AbstractKlibBasedSwiftRunnerTest : AbstractNativeSwiftExportTest() {

    private val tmpdir = FileUtil.createTempDirectory("SwiftExportIntegrationTests", null, false)

    override fun runCompiledTest(
        testPathFull: File,
        testCase: TestCase,
        swiftExportOutput: SwiftExportModule,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
    ) {
        val flattenModules = setOfNotNull(swiftExportOutput, swiftExportOutput.dependencies.firstOrNull())

        flattenModules.forEach {
            when (it) {
                is SwiftExportModule.BridgesToKotlin -> {
                    val files = it.files

                    val expectedFiles = testPathFull.toPath() / "golden_result/"
                    val expectedSwift = expectedFiles / it.name / "${it.name}.swift"
                    val expectedCHeader = expectedFiles / it.name / "${it.name}.h"
                    val expectedKotlinBridge = expectedFiles / it.name / "${it.name}.kt"

                    KotlinTestUtils.assertEqualsToFile(expectedSwift, files.swiftApi.readText())
                    KotlinTestUtils.assertEqualsToFile(expectedCHeader, files.cHeaderBridges.readText())
                    KotlinTestUtils.assertEqualsToFile(expectedKotlinBridge, files.kotlinBridges.readText())
                }
                is SwiftExportModule.SwiftOnly -> {
                    val expectedFiles = testPathFull.toPath() / "golden_result/"
                    val expectedSwift = expectedFiles / it.name / "${it.name}.swift"

                    KotlinTestUtils.assertEqualsToFile(expectedSwift, it.swiftApi.readText())
                }
            }
        }
    }

    override fun constructSwiftExportConfig(module: TestModule.Exclusive): SwiftExportConfig {
        val unsupportedTypeStrategy = ErrorTypeStrategy.Fail
        val errorTypeStrategy = ErrorTypeStrategy.Fail

        val defaultConfig: Map<String, String> = mapOf(
            SwiftExportConfig.STABLE_DECLARATIONS_ORDER to "true",
            SwiftExportConfig.RENDER_DOC_COMMENTS to "false",
            SwiftExportConfig.BRIDGE_MODULE_NAME to SwiftExportConfig.DEFAULT_BRIDGE_MODULE_NAME,
        )

        var unsupportedDeclarationReporterKind = UnsupportedDeclarationReporterKind.Silent
        var multipleModulesHandlingStrategy = MultipleModulesHandlingStrategy.OneToOneModuleMapping

        @Suppress("UNCHECKED_CAST") val discoveredConfig: Map<String, String> = (module
            .directives
            .firstOrNull { it.directive.name == TestDirectives.SWIFT_EXPORT_CONFIG.name }
            ?.values as? List<Pair<String, String>>)
            ?.toMap()
            ?.filter { (key, value) ->
                when (key) {
                    "unsupportedDeclarationsReporterKind" -> {
                        UnsupportedDeclarationReporterKind.entries
                            .singleOrNull { it.name.lowercase() == value.lowercase() }
                            ?.let { unsupportedDeclarationReporterKind = it }
                        false
                    }
                    "multipleModulesHandlingStrategy" -> {
                        MultipleModulesHandlingStrategy.entries
                            .singleOrNull { it.name.lowercase() == value.lowercase() }
                            ?.let { multipleModulesHandlingStrategy = it }
                        false
                    }
                    else -> true
                }
            }
            ?: emptyMap()

        val config = defaultConfig + discoveredConfig

        return SwiftExportConfig(
            settings = config,
            logger = createDummyLogger(),
            distribution = Distribution(KonanHome.konanHomePath),
            errorTypeStrategy = errorTypeStrategy,
            unsupportedTypeStrategy = unsupportedTypeStrategy,
            outputPath = tmpdir.toPath().resolve(module.name),
            unsupportedDeclarationReporterKind = unsupportedDeclarationReporterKind,
            multipleModulesHandlingStrategy = multipleModulesHandlingStrategy,
        )
    }

}

private object KonanHome {
    private const val KONAN_HOME_PROPERTY_KEY = "kotlin.internal.native.test.nativeHome"

    val konanHomePath: String
        get() = System.getProperty(KONAN_HOME_PROPERTY_KEY)
            ?: error("Missing System property: '$KONAN_HOME_PROPERTY_KEY'")
}
