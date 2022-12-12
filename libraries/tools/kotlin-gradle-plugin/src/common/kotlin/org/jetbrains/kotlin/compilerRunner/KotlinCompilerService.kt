/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.gradle.utils.registerSharedService
import java.io.File

internal abstract class KotlinDaemonService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private data class DaemonConnectionKey(
        val compilerId: CompilerId,
        val clientAliveFlagFile: File,
        val sessionAliveFlagFile: File,
        val isDebugEnabled: Boolean,
        val daemonOptions: DaemonOptions,
        val daemonJVMOptions: DaemonJVMOptions
    )

    private val connections = HashMap<DaemonConnectionKey, CompileServiceSession?>()

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
        val existingConnection = connections[key]
        if (existingConnection != null) {
            if (existingConnection.compileService.isAlive().isGood) {
                return existingConnection
            } else {
                connections.remove(existingConnection)
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
        connections[key] = newConnection
        return newConnection
    }

    @Synchronized
    override fun close() {
        connections.clear()
    }

    companion object {
        fun registerIfAbsent(gradle: Gradle): Provider<KotlinDaemonService> =
            gradle.registerSharedService()
    }
}