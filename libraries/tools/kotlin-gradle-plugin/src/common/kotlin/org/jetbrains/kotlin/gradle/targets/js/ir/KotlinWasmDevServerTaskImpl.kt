/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.deployment.internal.Deployment
import org.gradle.deployment.internal.DeploymentHandle
import org.gradle.deployment.internal.DeploymentRegistry
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmDevServer
import java.io.File
import java.net.ServerSocket
import javax.inject.Inject

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
            val deploymentRegistry = services.get(DeploymentRegistry::class.java)
            val deploymentHandle = deploymentRegistry.get("wasmDevServer", Handle::class.java)
            if (deploymentHandle == null) {
                val workQueue = workerExecutor.processIsolation()

                workQueue.submit(DevServerWorkAction::class.java) { params ->
                    params.contentDirectory.set(contentDirectory)
                    params.rootDirectory.set(rootDirectory)
                    params.host.set(host)
                    params.port.set(serverPort)
                    params.lockFile.set(lockFile)
                    params.continuous.set(true)
                }

                deploymentRegistry.start(
                    "simpleDevServer",
                    DeploymentRegistry.ChangeBehavior.BLOCK,
                    Handle::class.java,
                    lockFile,
                )
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
        while (true) {
            try {
                ServerSocket(port).use { return port }
            } catch (_: Exception) {
                port++
            }
        }
    }

    internal abstract class Handle @Inject constructor(
        private val lockFile: File,
    ) : DeploymentHandle {

        override fun isRunning(): Boolean =
            lockFile.exists()

        override fun start(deployment: Deployment) {
        }

        override fun stop() {
        }
    }
}
