/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal enum class SwiftImportTestExecutionKind {
    SWIFT_RESOLVE,
    XCODEBUILD,
}

internal interface SwiftImportTestExecutionServiceParameters : BuildServiceParameters {
    val kind: Property<SwiftImportTestExecutionKind>
}

internal abstract class SwiftImportTestExecutionService : BuildService<SwiftImportTestExecutionServiceParameters> {
    private val ownerClaimed = CountDownLatch(1)
    private val swiftResolveGate = OwnerStartedBeforeAwaitWorkerGate()
    private val xcodebuildGate = OwnerStartedBeforeAwaitWorkerGate()
    private val executionKind: SwiftImportTestExecutionKind
        get() = parameters.kind.orNull ?: error("Swift import test execution kind is not configured")

    fun markOwnerClaimed() {
        ownerClaimed.countDown()
    }

    fun awaitOwnerClaimed() {
        check(ownerClaimed.await(MARKER_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            "Timed out waiting for owner to claim the shared SwiftPM import bucket"
        }
    }

    fun beforeSwiftResolveOwnerWorkerStarted() {
        if (executionKind != SwiftImportTestExecutionKind.SWIFT_RESOLVE) return
        swiftResolveGate.beforeOwnerWorkerStarted()
    }

    fun beforeSwiftResolveAwaitWorkerStarted() {
        if (executionKind != SwiftImportTestExecutionKind.SWIFT_RESOLVE) return
        swiftResolveGate.beforeAwaitWorkerStarted()
    }

    fun beforeXcodebuildOwnerWorkerStarted() {
        if (executionKind != SwiftImportTestExecutionKind.XCODEBUILD) return
        xcodebuildGate.beforeOwnerWorkerStarted()
    }

    fun beforeXcodebuildAwaitWorkerStarted() {
        if (executionKind != SwiftImportTestExecutionKind.XCODEBUILD) return
        xcodebuildGate.beforeAwaitWorkerStarted()
    }

    private class OwnerStartedBeforeAwaitWorkerGate {
        private val awaitWorkerStarted = CountDownLatch(1)
        private val ownerWorkerStartWaitTimedOut = AtomicBoolean(false)

        fun beforeOwnerWorkerStarted() {
            if (!awaitWorkerStarted.await(AWAIT_WORKER_START_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                ownerWorkerStartWaitTimedOut.set(true)
            }
        }

        fun beforeAwaitWorkerStarted() {
            awaitWorkerStarted.countDown()
            check(ownerWorkerStartWaitTimedOut.get()) {
                "Await worker started before the owner worker start hook timed out."
            }
        }
    }

    private companion object {
        private const val MARKER_WAIT_TIMEOUT_MS = 30_000L
        private const val AWAIT_WORKER_START_WAIT_TIMEOUT_MS = 2_000L
    }
}
