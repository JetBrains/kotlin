/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.getOrCreateTestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.NativeTestInstances
import org.jetbrains.kotlin.test.backend.handlers.tryUpdateTestData
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import org.opentest4j.AssertionFailedError

class SwiftExportTestSupport : BeforeEachCallback, TestExecutionExceptionHandler {
    /**
     * Note: [BeforeEachCallback.beforeEach] allows accessing test instances while [BeforeAllCallback.beforeAll] which may look
     * more preferable here does not allow it because it is called at the time when test instances are not created yet.
     * Also, [TestInstancePostProcessor.postProcessTestInstance] allows accessing only the currently created test instance and does
     * not allow accessing its parent test instance in case there are inner test classes in the generated test suite.
     */
    override fun beforeEach(extensionContext: ExtensionContext): Unit = with(extensionContext) {
        val settings = createTestRunSettings(NativeTestInstances<AbstractSwiftExportTest>(requiredTestInstances.allInstances))

        // Inject the required properties to test instance.
        with(settings.get<NativeTestInstances<AbstractSwiftExportTest>>().enclosingTestInstance) {
            testRunSettings = settings
            testRunProvider = getOrCreateTestRunProvider()
        }
    }

    private val overwriteGoldenData = System.getProperty("kotlin.test.update.test.data") == "true"

    override fun handleTestExecutionException(context: ExtensionContext, throwable: Throwable) {
        if (overwriteGoldenData) {
            throwable.tryUpdateTestData()
        }
        throw throwable
    }
}