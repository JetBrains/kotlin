/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.computeBlackBoxTestInstances
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.getOrCreateTestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRunSettings
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

abstract class AbstractNativeCoreTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE) {
    private lateinit var extensionContext: ExtensionContext

    @RegisterExtension
    val extensionContextCaptor = BeforeEachCallback { context ->
        this.extensionContext = context
    }

    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        useAdditionalService { // Register TestRunSettings into TestServices
            extensionContext.createTestRunSettings(extensionContext.computeBlackBoxTestInstances())
        }
        useAdditionalService { // Register TestRunProvider into TestServices
            extensionContext.getOrCreateTestRunProvider()
        }
    }
}

val TestServices.testRunSettings: TestRunSettings by TestServices.testServiceAccessor()
val TestServices.testRunProvider: TestRunProvider by TestServices.testServiceAccessor()
