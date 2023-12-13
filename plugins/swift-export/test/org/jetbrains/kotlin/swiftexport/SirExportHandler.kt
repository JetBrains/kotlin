/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport

import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.io.File

internal class SirExportHandler(testServices: TestServices) : AnalysisHandler<SwiftExportArtifact>(
    testServices,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true
) {
    override val artifactKind: TestArtifactKind<SwiftExportArtifact>
        get() = SwiftExportArtifact.Kind

    override fun processModule(module: TestModule, info: SwiftExportArtifact) {
        val originalFile = module.files.first().originalFile
        val originalFileName = originalFile.absolutePath.removeSuffix(".kt")

        val expectedSwift = File("${originalFileName}.golden.swift")
        val expectedCHeader = File("${originalFileName}.golden.h")
        val expectedKotlinBridge = File("${originalFileName}.golden.kt")

        testServices.assertions.assertEqualsToFile(expectedSwift, info.swift.readText())
        testServices.assertions.assertEqualsToFile(expectedCHeader, info.cHeader.readText())
        testServices.assertions.assertEqualsToFile(expectedKotlinBridge, info.ktBridge.readText())
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}