/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.testLibraryAtomicFu
import org.jetbrains.kotlin.konan.test.testLibraryKotlinxCoroutines
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

class SwiftExportWithCoroutinesTestSupport : BeforeTestExecutionCallback {
    override fun beforeTestExecution(context: ExtensionContext?) {
        (context?.requiredTestInstance as AbstractSwiftExportTest).givenModules += setOf(
            TestModule.Given(testLibraryKotlinxCoroutines.toFile()),
            TestModule.Given(testLibraryAtomicFu.toFile()),
        )
    }
}
