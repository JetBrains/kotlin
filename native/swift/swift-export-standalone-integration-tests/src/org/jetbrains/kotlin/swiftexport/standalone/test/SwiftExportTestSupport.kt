/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.getOrCreateTestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.NativeTestInstances
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler

class SwiftExportTestSupport : BeforeEachCallback {
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
}