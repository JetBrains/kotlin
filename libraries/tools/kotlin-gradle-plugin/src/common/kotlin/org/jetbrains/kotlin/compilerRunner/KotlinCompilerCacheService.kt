/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.gradle.utils.registerSharedService
import java.io.File
import java.net.URLClassLoader

internal abstract class KotlinCompilerCacheService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private data class DaemonConnectionKey(
        val compilerId: CompilerId,
        val clientAliveFlagFile: File,
        val sessionAliveFlagFile: File,
        val isDebugEnabled: Boolean,
        val daemonOptions: DaemonOptions,
        val daemonJVMOptions: DaemonJVMOptions
    )

    private val daemonConnections = HashMap<DaemonConnectionKey, CompileServiceSession?>()
    private val classloaders = HashMap<CompilerId, URLClassLoader>()

    @Synchronized
    override fun close() {
        daemonConnections.clear()
        for (classloader in classloaders.values) {
            classloader.close()
        }
        classloaders.clear()
    }

    @Synchronized
    fun getClassloader(compilerClasspath: List<File>): ClassLoader {
        val compilerId = CompilerId.makeCompilerId(compilerClasspath)
        return classloaders.getOrPut(compilerId) {
            val urls = compilerClasspath.map { it.toURI().toURL() }.toTypedArray()
            URLClassLoader(urls)
        }
    }

    @Synchronized
    fun getDaemonConnection(
        compilerClasspath: List<File>,
        clientAliveFlagFile: File,
        sessionAliveFlagFile: File,
        messageCollector: MessageCollector,
        isDebugEnabled: Boolean,
        additionalJvmArgs: List<String>?
    ): CompileServiceSession? {
        val compilerId = CompilerId.makeCompilerId(compilerClasspath)
        val daemonOptions = configureDaemonOptions()
        val daemonJvmOptions = configureDaemonJVMOptions(
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = false,
            inheritAdditionalProperties = true
        ).also { opts ->
            if (!additionalJvmArgs.isNullOrEmpty()) {
                opts.jvmParams.addAll(
                    additionalJvmArgs.filterExtractProps(opts.mappers, "", opts.restMapper)
                )
            }
        }
        val key = DaemonConnectionKey(
            compilerId = compilerId,
            clientAliveFlagFile = clientAliveFlagFile,
            sessionAliveFlagFile = sessionAliveFlagFile,
            isDebugEnabled = isDebugEnabled,
            daemonOptions = daemonOptions,
            daemonJVMOptions = daemonJvmOptions
        )
        val existingConnection = daemonConnections[key]
        if (existingConnection != null) {
            if (existingConnection.compileService.isAlive().isGood) {
                return existingConnection
            } else {
                daemonConnections.remove(existingConnection)
            }
        }

        val newConnection = KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId = compilerId,
            clientAliveFlagFile = clientAliveFlagFile,
            sessionAliveFlagFile = sessionAliveFlagFile,
            isDebugEnabled = isDebugEnabled,
            daemonOptions = daemonOptions,
            daemonJVMOptions = daemonJvmOptions,
            messageCollector = if (isDebugEnabled) messageCollector else MessageCollector.NONE
        )
        daemonConnections[key] = newConnection
        return newConnection
    }

    companion object {
        fun registerIfAbsent(project: Project): Provider<KotlinCompilerCacheService> =
            project.gradle.registerSharedService()
    }
}