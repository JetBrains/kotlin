/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.withOSVersion
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.invokeSwiftC
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createModuleMap
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions
import java.io.File


abstract class SwiftTypeCheckBaseTest : AbstractNativeSimpleTest() {
    /**
    This test is the simplest possible solutions for task KT-65559

    One reading this test could find themselves confused, with following questions:
    1/ what does it do
    2/ why it is placed in native-test infra
    3/ what actually we do test

    This comment wil try to answer all those questions.

    What we are trying to achieve:
    There is a SwiftExport artefact, and we have some integration tests for it.
    But our integration tests check only one thing - that our current implementation generates expected source files.
    But we do not compile that resulted files, as that actions requires macOS agents with swift installed,
    and we want to keep our day2day tests as quick as possible.

    So, we have construct this test - it will take golden data that we are expecting to receive from SwiftExport,
    and verify with swift compiler that we expect valid code. This way we have separated tests that verify code generation
    from tests that verify code validity. That separation may not be desired, but that separation allows us to keep tests for
    code generation fast and TeamCity agent agnostic.

    We do plan to refactor this, and extract infra for running swiftc and xcode into separate module, that will be shared
    between SwiftExport, ObjectiveCExport, Kotlin/Native and KGP tests. But currently - the following solution is the simplest one.
     */
    fun runTest(
        swiftFilePath: String,
    ) {
        Assumptions.assumeTrue(targets.hostTarget.family.isAppleFamily && targets.testTarget.family.isAppleFamily)

        val swiftFile = File(swiftFilePath)
        val cHeader = swiftFile.withReplacedExtensionOrNull("swift", "h")
            ?: throw IllegalArgumentException("SwiftTypeCheckBaseTest input should have .h counterpart")

        val configs = testRunSettings.configurables as AppleConfigurables
        val swiftTarget = configs.targetTriple.withOSVersion(configs.osVersionMin).toString()

        val bridgeModuleFile = createModuleMap(buildDir, cHeader)

        val args = listOf(
            "-typecheck", swiftFile.absolutePath,
            "-Xcc", "-fmodule-map-file=${bridgeModuleFile.absolutePath}",
            "-sdk", configs.absoluteTargetSysRoot, "-target", swiftTarget
        )

        val (exitCode, swiftcOutput, _, _) =
            invokeSwiftC(testRunSettings, args)

        assertEquals(ExitCode.OK, exitCode, "swift type checked resulted in: $swiftcOutput")
    }
}
