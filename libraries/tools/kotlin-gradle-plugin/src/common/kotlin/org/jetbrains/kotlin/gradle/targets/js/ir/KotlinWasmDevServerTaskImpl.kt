/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinWasmDevServer
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import javax.inject.Inject

@OptIn(ExperimentalWasmDsl::class)
@DisableCachingByDefault
internal abstract class KotlinWasmDevServerTaskImpl
@Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask(), KotlinWasmDevServer {

    private val rootDirectory: File = project.rootDir

    override val host: Property<String> = project.objects.property(String::class.java).convention("localhost")

    private val isContinuous = project.gradle.startParameter.isContinuous

    @TaskAction
    fun start() {
        val serverPort = port.getOrElse(findFreePort())

        val lockFile = temporaryDir.resolve("server.lock")

        if (isContinuous) {
            if (!lockFile.exists()) {
                val workQueue = workerExecutor.processIsolation()

                workQueue.submit(DevServerWorkAction::class.java) { params ->
                    params.contentDirectory.set(contentDirectory)
                    params.rootDirectory.set(rootDirectory)
                    params.host.set(host)
                    params.port.set(serverPort)
                    params.lockFile.set(lockFile)
                    params.continuous.set(true)
                }
            }
        } else {
            val workQueue = workerExecutor.processIsolation()

            workQueue.submit(DevServerWorkAction::class.java) { params ->
                params.contentDirectory.set(contentDirectory)
                params.rootDirectory.set(rootDirectory)
                params.host.set(host)
                params.port.set(serverPort)
                params.lockFile.set(lockFile)
                params.continuous.set(false)
            }
        }
    }

    private fun findFreePort(startPort: Int = 8080): Int {
        var port = startPort
        val numberOfAttempts = minOf(MAX_PORT_NUMBER - startPort, MAX_NUMBER_ATTEMPTS)

        while (port < startPort + numberOfAttempts) {
            try {
                ServerSocket(port).use { return port }
            } catch (_: IOException) {
                port++
            }
        }

        error("Unable to find free port in a range from $startPort to ${startPort + numberOfAttempts}")
    }

    internal companion object {
        private const val MAX_NUMBER_ATTEMPTS = 1_000
        private const val MAX_PORT_NUMBER = 65_535
    }
}
