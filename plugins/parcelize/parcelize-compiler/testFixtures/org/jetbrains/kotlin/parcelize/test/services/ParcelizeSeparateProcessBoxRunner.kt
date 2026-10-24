/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.services

import org.jetbrains.kotlin.parcelize.test.robolectric.ROBOLECTRIC_BOX_WORKER_CLASS_NAME
import org.jetbrains.kotlin.parcelize.test.robolectric.RobolectricBoxWorker
import org.jetbrains.kotlin.parcelize.test.robolectric.shared.RobolectricBoxSwitchboard
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeDirectives.ENABLE_PARCELIZE
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.jvm.JvmSeparateProcessBoxRunner
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.utils.PathUtil
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.net.URL
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Runs `box` of Parcelize tests in reusable Robolectric JVMs, see
 * [RobolectricBoxWorker][org.jetbrains.kotlin.parcelize.test.robolectric.RobolectricBoxWorker].
 *
 * A new JVM for each test would spend seconds on starting the JVM and setting up the Robolectric sandbox, under the load of parallel
 * tests even ~10 s, while `box` itself usually takes milliseconds.
 */
class ParcelizeSeparateProcessBoxRunner(testServices: TestServices) : JvmSeparateProcessBoxRunner(testServices) {
    override fun runBox(module: TestModule, javaExe: File, classPath: List<URL>): String? {
        if (ENABLE_PARCELIZE !in module.directives) return null
        val testDirectory = testServices.temporaryDirectoryManager.rootDir.toPath()
        val [testClassPath, sharedClassPath] = classPath.map { File(it.toURI()) }.distinct()
            .partition { it.toPath().startsWith(testDirectory) }
        val workerClassPath = sharedClassPath + PathUtil.getResourcePathForClass(RobolectricBoxSwitchboard::class.java)
        return RobolectricBoxWorkers.runBox(javaExe, workerClassPath, testClassPath)
    }
}

/**
 * Worker JVMs, reused by the tests that need the same JVM and class path.
 */
private object RobolectricBoxWorkers {
    private val ROBOLECTRIC_PROPERTIES = listOf("robolectric.classpath", "robolectric.offline", "robolectric.dependency.dir")
    private const val TIMEOUT_MINUTES = 3L

    /**
     * A test takes milliseconds in a worker that is set up, so a few workers serve all test threads.
     */
    private val maxWorkersPerPool = (Runtime.getRuntime().availableProcessors() / 4).coerceIn(1, 4)

    private val pools = ConcurrentHashMap<List<File>, Pool>()
    private val workerProcesses = ConcurrentLinkedQueue<Process>()
    private val responseReaders = Executors.newCachedThreadPool { runnable -> Thread(runnable).apply { isDaemon = true } }

    init {
        Runtime.getRuntime().addShutdownHook(Thread { workerProcesses.forEach(Process::destroyForcibly) })
    }

    fun runBox(javaExe: File, workerClassPath: List<File>, testClassPath: List<File>): String =
        pools.computeIfAbsent(listOf(javaExe) + workerClassPath) { Pool(javaExe, workerClassPath) }.runBox(testClassPath)

    private class Pool(private val javaExe: File, private val workerClassPath: List<File>) {
        private val permits = Semaphore(maxWorkersPerPool)
        private val idleWorkers = ConcurrentLinkedQueue<Worker>()

        fun runBox(testClassPath: List<File>): String {
            permits.acquire()
            try {
                val worker = idleWorkers.poll() ?: Worker.start(javaExe, workerClassPath)
                val result = worker.runBox(testClassPath)
                if (worker.isReusable) idleWorkers.add(worker)
                return result
            } finally {
                permits.release()
            }
        }
    }

    private class Worker(private val process: Process) {
        private val requests: BufferedWriter = process.outputStream.bufferedWriter()
        private val responses: BufferedReader = process.inputStream.bufferedReader()

        var isReusable: Boolean = true
            private set

        fun runBox(testClassPath: List<File>): String {
            requests.write(testClassPath.joinToString(File.pathSeparator))
            requests.newLine()
            requests.flush()
            val response = responseReaders.submit<String?> { responses.readLine() }
            val line = try {
                response.get(TIMEOUT_MINUTES, TimeUnit.MINUTES)
            } catch (_: TimeoutException) {
                retire()
                return "The Robolectric worker didn't finish the test in $TIMEOUT_MINUTES minutes"
            }
            if (line == null) {
                retire()
                return "The Robolectric worker exited before finishing the test"
            }
            val status = line.substringBefore(' ')
            if (status != RobolectricBoxWorker.KEEP_WORKER) retire()
            return String(Base64.getDecoder().decode(line.substringAfter(' ')))
        }

        private fun retire() {
            isReusable = false
            process.destroyForcibly()
        }

        companion object {
            fun start(javaExe: File, workerClassPath: List<File>): Worker {
                val command = listOf(javaExe.absolutePath, "-ea", "-classpath", workerClassPath.joinToString(File.pathSeparator)) +
                        ROBOLECTRIC_PROPERTIES.map { "-D$it=${System.getProperty(it)}" } +
                        ROBOLECTRIC_BOX_WORKER_CLASS_NAME
                val process = ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start()
                workerProcesses.add(process)
                return Worker(process)
            }
        }
    }
}
