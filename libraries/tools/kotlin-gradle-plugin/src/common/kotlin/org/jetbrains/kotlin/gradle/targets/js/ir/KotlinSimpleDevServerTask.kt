/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class KotlinSimpleDevServerTask
@Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contentDirectory: DirectoryProperty

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val port: Property<Int>

    @get:Input
    abstract val host: Property<String>

    @TaskAction
    fun start() {
        val serverPort = port.getOrElse(findFreePort())

        val workQueue = workerExecutor.processIsolation()

        workQueue.submit(DevServerWorkAction::class.java) { params ->
            params.contentDirectory.set(contentDirectory)
            params.rootDirectory.set(rootDirectory)
            params.host.set(host)
            params.port.set(serverPort)
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
}
