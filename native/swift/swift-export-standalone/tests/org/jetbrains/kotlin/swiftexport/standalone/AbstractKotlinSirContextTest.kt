/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.test.KotlinTestUtils
import kotlin.io.path.*

open class AbstractSwiftRunnerTest : AbstractSwiftRunnerTestBase()

abstract class AbstractSwiftRunnerTestBase {

    private val tmpdir = FileUtil.createTempDirectory("SwiftExportIntegrationTests", null, false)

    fun runTest(testPathString: String) {
        val path = Path(testPathString)
        val expectedFiles = path / "golden_result/"
        val moduleRoot = path / "input_root/"
        assert(expectedFiles.isDirectory() && moduleRoot.isDirectory()) { "setup for $path is incorrect" }

        val expectedSwift = expectedFiles / "result.swift"
        val expectedCHeader = expectedFiles / "result.h"
        val expectedKotlinBridge = expectedFiles / "result.kt"

        val output = SwiftExportOutput(
            swiftApi = tmpdir.resolve("result.swift").toPath(),
            kotlinBridges = tmpdir.resolve("result.kt").toPath(),
            cHeaderBridges = tmpdir.resolve("result.c").toPath()
        )

        runSwiftExport(
            input = SwiftExportInput(
                sourceRoot = moduleRoot,
            ),
            output = output,
            config = SwiftExportConfig(
                settings = mapOf(
                    SwiftExportConfig.DEBUG_MODE_ENABLED to "true",
                    SwiftExportConfig.BRIDGE_MODULE_NAME to SwiftExportConfig.DEFAULT_BRIDGE_MODULE_NAME,
                ),
                logger = createDummyLogger(),
                distribution = Distribution(KonanHome.konanHomePath),
            )
        )

        KotlinTestUtils.assertEqualsToFile(expectedSwift, output.swiftApi.readText())
        KotlinTestUtils.assertEqualsToFile(expectedCHeader, output.cHeaderBridges.readText())
        KotlinTestUtils.assertEqualsToFile(expectedKotlinBridge, output.kotlinBridges.readText())
    }
}

private object KonanHome {
    private const val KONAN_HOME_PROPERTY_KEY = "kotlin.internal.native.test.nativeHome"

    val konanHomePath: String
        get() = System.getProperty(KONAN_HOME_PROPERTY_KEY)
            ?: error("Missing System property: '$KONAN_HOME_PROPERTY_KEY'")
}
