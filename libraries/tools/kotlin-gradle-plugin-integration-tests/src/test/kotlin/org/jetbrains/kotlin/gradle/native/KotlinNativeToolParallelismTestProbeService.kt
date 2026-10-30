/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeToolParallelismProbe
import org.jetbrains.kotlin.internal.compilerRunner.native.UsesKotlinNativeToolParallelismProbe
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Records max concurrent Kotlin/Native tool workers.
 * Each worker waits for [Parameters.expectedRunning] workers to start, and fails if more than that run at once.
 */
internal abstract class KotlinNativeToolParallelismTestProbeService :
    BuildService<KotlinNativeToolParallelismTestProbeService.Parameters>, KotlinNativeToolParallelismProbe, AutoCloseable {

    internal interface Parameters : BuildServiceParameters {
        val expectedRunning: Property<Int>
        val parallelismReportFile: RegularFileProperty
    }

    private val running = AtomicInteger()
    private val maxRunning = AtomicInteger()
    private val expectedRunningReached by lazy { CountDownLatch(parameters.expectedRunning.get()) }

    override fun workerStarted() {
        val nowRunning = running.incrementAndGet()
        maxRunning.accumulateAndGet(nowRunning, ::maxOf)
        check(nowRunning <= parameters.expectedRunning.get()) {
            "$nowRunning parallel workers running, but the limit is ${parameters.expectedRunning.get()}"
        }
        expectedRunningReached.countDown()
        check(expectedRunningReached.await(EXPECTED_RUNNING_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            "Expected ${parameters.expectedRunning.get()} parallel workers, but only ${maxRunning.get()} overlapped " +
                    "within $EXPECTED_RUNNING_WAIT_TIMEOUT_SECONDS seconds"
        }
    }

    override fun workerFinished() {
        running.decrementAndGet()
    }

    override fun close() {
        val reportFile = parameters.parallelismReportFile.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(maxRunning.get().toString())
    }

    internal companion object {
        private const val EXPECTED_RUNNING_WAIT_TIMEOUT_SECONDS = 60L
        internal const val TEST_PROBE_NAME_PREFIX = "kotlinNativeToolParallelismTestProbe"

        internal inline fun <reified T : UsesKotlinNativeToolParallelismProbe> File.reportFile(): File =
            resolve("${T::class.java.simpleName}.txt")

        internal inline fun <reified T : UsesKotlinNativeToolParallelismProbe> File.observedMaxParallelWorkers(): Int {
            val reportFile = reportFile<T>()
            check(reportFile.exists()) { "No parallelism report at $reportFile: no ${T::class.java.simpleName} worker ran" }
            return reportFile.readText().toInt()
        }

        internal inline fun <reified T : UsesKotlinNativeToolParallelismProbe> Project.registerParallelismProbeServiceIfAbsent(
            reportDir: File,
            expectedRunning: Int,
        ): Provider<KotlinNativeToolParallelismTestProbeService> =
            gradle.sharedServices.registerIfAbsent(
                "$TEST_PROBE_NAME_PREFIX${T::class.java.simpleName}",
                KotlinNativeToolParallelismTestProbeService::class.java,
            ) {
                it.parameters.expectedRunning.value(expectedRunning).disallowChanges()
                it.parameters.parallelismReportFile.fileValue(reportDir.reportFile<T>()).disallowChanges()
            }.also { serviceProvider ->
                tasks.withType(T::class.java).configureEach { task ->
                    task.kotlinNativeToolParallelismProbe.value(serviceProvider).disallowChanges()
                    task.usesService(serviceProvider)
                }
            }
    }
}
