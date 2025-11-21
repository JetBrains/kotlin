/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.testLibraryAtomicFu
import org.jetbrains.kotlin.konan.test.testLibraryAtomicFuCinteropInterop
import org.jetbrains.kotlin.konan.test.testLibraryKotlinxCoroutines
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

class SwiftExportWithCoroutinesTestSupport : BeforeTestExecutionCallback {
    override fun beforeTestExecution(context: ExtensionContext?) {
        val atomicFuCinteropInterop = TestModule.Given(testLibraryAtomicFuCinteropInterop.toFile())
        val atomicFuModule = TestModule.Given(
            testLibraryAtomicFu.toFile(),
            dependencies = setOf(atomicFuCinteropInterop)
        )
        val kotlinxCoroutinesModule = TestModule.Given(
            testLibraryKotlinxCoroutines.toFile(),
            // It is not quite correct to pass atomicfu-cinterop-interop as a coroutines dependency,
            // but this fixes compilation of the corresponding static caches.
            dependencies = setOf(atomicFuModule, atomicFuCinteropInterop)
        )
        (context?.requiredTestInstance as AbstractSwiftExportTest).givenModules += setOf(
            kotlinxCoroutinesModule,
            atomicFuModule,
        )
        (context.requiredTestInstance as AbstractSwiftExportTest).isCoroutineSupportEnabled = true
    }
}
