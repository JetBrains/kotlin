/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.NativeTestInstances
import org.jetbrains.kotlin.konan.test.testLibraryAtomicFuKlibFile
import org.jetbrains.kotlin.konan.test.testLibraryAtomicFuCinteropInteropKlibFile
import org.jetbrains.kotlin.konan.test.testLibraryKotlinxCoroutinesKlibFile
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

class SwiftExportWithCoroutinesTestSupport : BeforeTestExecutionCallback {
    override fun beforeTestExecution(context: ExtensionContext?) {
        val atomicFuCinteropInterop = TestModule.Given(testLibraryAtomicFuCinteropInteropKlibFile.toFile())
        val atomicFuModule = TestModule.Given(
            testLibraryAtomicFuKlibFile.toFile(),
            dependencies = setOf(atomicFuCinteropInterop)
        )
        val kotlinxCoroutinesModule = TestModule.Given(
            testLibraryKotlinxCoroutinesKlibFile.toFile(),
            // It is not quite correct to pass atomicfu-cinterop-interop as a coroutines dependency,
            // but this fixes compilation of the corresponding static caches.
            dependencies = setOf(atomicFuModule, atomicFuCinteropInterop)
        )
        // NOTE: `requiredTestInstance` is the *innermost* instance. When a test data group is generated as an
        // inner test class (one nested class per group directory), that is the generated inner class, which does
        // not extend AbstractSwiftExportTest. The enclosing instance is the real test — and the one whose
        // `runTest` executes — so the modules have to be added there. Same reasoning as SwiftExportTestSupport.
        val instances = NativeTestInstances<AbstractSwiftExportTest>(context!!.requiredTestInstances.allInstances)
        instances.enclosingTestInstance.apply {
            givenModules += setOf(
                kotlinxCoroutinesModule,
                atomicFuModule,
            )
            minOSVersion = "15.0"
        }
    }
}
