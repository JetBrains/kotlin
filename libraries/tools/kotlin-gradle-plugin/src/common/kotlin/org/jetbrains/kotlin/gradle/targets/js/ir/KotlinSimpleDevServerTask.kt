/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.deployment.internal.Deployment
import org.gradle.deployment.internal.DeploymentHandle
import org.gradle.deployment.internal.DeploymentRegistry
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.net.URL
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class KotlinSimpleDevServerTask
@Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contentDirectory: DirectoryProperty

    private val rootDirectory: File = project.rootDir

    @get:Input
    @get:Optional
    @get:Option(description = "Set a port for the dev server.")
    abstract val port: Property<Int>

    @get:Input
    @get:Option(description = "Set a HOST for the dev server.")
    val host: Property<String> = project.objects.property(String::class.java).convention("localhost")

    private val isContinuous = project.gradle.startParameter.isContinuous

    @TaskAction
    fun start() {
        val serverPort = port.getOrElse(findFreePort())

        val lockFile = temporaryDir.resolve("server.lock")

        if (isContinuous) {
            val deploymentRegistry = services.get(DeploymentRegistry::class.java)
            val deploymentHandle = deploymentRegistry.get("simpleDevServer", Handle::class.java)
            if (deploymentHandle == null) {
                println("HELLO 0")

                val workQueue = workerExecutor.processIsolation()

                workQueue.submit(DevServerWorkAction::class.java) { params ->
                    println("HELLO 1")
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
                    workerExecutor,
                    contentDirectory.get().asFile,
                    rootDirectory,
                    host.get(),
                    serverPort,
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

    fun findFreePort(startPort: Int = 8080): Int {
        var port = startPort
        while (true) {
            try {
                java.net.ServerSocket(port).use { return port }
            } catch (_: Exception) {
                port++
            }
        }
    }

    internal abstract class Handle @Inject constructor(
        private val workerExecutor: WorkerExecutor,
        private val contentDirectory: File,
        private val rootDirectory: File,
        private val host: String,
        private val serverPort: Int,
        private val lockFile: File,
    ) : DeploymentHandle {

        override fun isRunning(): Boolean =
            lockFile.exists()

        override fun start(deployment: Deployment) {
        }

        override fun stop() {
            URL(
                "http",
                host,
                serverPort,
                "__shutdown"
            )
                .openConnection()
                .getInputStream()
                .close()
        }
    }
}
