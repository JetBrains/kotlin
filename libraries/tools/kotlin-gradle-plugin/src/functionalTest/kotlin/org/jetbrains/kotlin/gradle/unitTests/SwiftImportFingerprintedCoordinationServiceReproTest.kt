/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftResolveBucket
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftResolveBucketMapKey
import kotlin.test.Test
import kotlin.test.assertTrue
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch

class SwiftImportFingerprintedCoordinationServiceReproTest {

    @Test
    fun `joiner must wait for the owner to start before submitting the blocking worker`() {
        val taskPool = Executors.newFixedThreadPool(2)
        val workerPool = Executors.newSingleThreadExecutor()
        try {
            val tempRoot = Files.createTempDirectory("swift-import-repro").toFile()
            val bucket = SwiftResolveBucket(
                key = SwiftResolveBucketMapKey("repro"),
                ownerPackageResolvedFile = tempRoot.resolve("Package.resolved"),
                ownerWorkspaceStateFile = tempRoot.resolve("workspace-state.json"),
                ownerSwiftPMDependenciesCheckout = tempRoot.resolve("checkout"),
                ownerSyntheticImportProjectRoot = tempRoot.resolve("project"),
            )
            val joinerReachedWaitState = CountDownLatch(1)

            val joinerIsWaiting = taskPool.submit(Callable {
                joinerReachedWaitState.countDown()
                val waiter = workerPool.submit(Callable {
                    bucket.completion.await()
                    Unit
                })
                waiter.get(1, TimeUnit.SECONDS)
            })

            val owner = taskPool.submit(Callable {
                assertTrue(joinerReachedWaitState.await(1, TimeUnit.SECONDS), "joiner did not reach wait state")
                val ownerWorker = workerPool.submit(Callable {
                    bucket.completion.countDown()
                    Unit
                })
                ownerWorker.get(1, TimeUnit.SECONDS)
            })

            owner.get(1, TimeUnit.SECONDS)
            joinerIsWaiting.get(1, TimeUnit.SECONDS)
        } finally {
            workerPool.shutdownNow()
            taskPool.shutdownNow()
        }
    }
}
