/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.utils.registerSharedService
import java.io.File
import java.net.URLClassLoader

internal abstract class KotlinCompilerCacheService : BuildService<KotlinCompilerCacheService.Parameters>, AutoCloseable {
    interface Parameters : BuildServiceParameters {
        val cacheDaemonConnection: Property<Boolean>
    }

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
    private val cacheDaemonConnection = parameters.cacheDaemonConnection.get()

    @Synchronized
    override fun close() {
        daemonConnections.clear()
        for (classloader in classloaders.values) {
            try {
                classloader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        classloaders.clear()
    }

    @Synchronized
    fun getClassloader(
        compilerClasspath: List<File>,
        // currently it is not possible to inject or pass a Gradle/SLF4J logger
        // via constructor or build service parameters
        log: KotlinLogger
    ): ClassLoader {
        val compilerId = CompilerId.makeCompilerId(compilerClasspath)
        return classloaders.getOrPut(compilerId) {
            val urls = compilerClasspath.map { it.toURI().toURL() }.toTypedArray()
            URLClassLoader(urls).also {
                log.kotlinDebug {
                    "Creating a new classloader for Kotlin Compiler: " + compilerId.compilerClasspath.joinToString(", ")
                }
            }
        }
    }

    @Synchronized
    fun getDaemonConnection(
        compilerClasspath: List<File>,
        clientAliveFlagFile: File,
        sessionAliveFlagFile: File,
        messageCollector: MessageCollector,
        isDebugEnabled: Boolean,
        additionalJvmArgs: List<String>?,
        // currently it is not possible to inject or pass a Gradle/SLF4J logger
        // via constructor or build service parameters
        log: KotlinLogger
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

        fun newDaemonConnection(): CompileServiceSession? {
            log.kotlinDebug { "Creating a new connection to Kotlin Daemon" }
            return KotlinCompilerRunnerUtils.newDaemonConnection(
                compilerId = compilerId,
                clientAliveFlagFile = clientAliveFlagFile,
                sessionAliveFlagFile = sessionAliveFlagFile,
                isDebugEnabled = isDebugEnabled,
                daemonOptions = daemonOptions,
                daemonJVMOptions = daemonJvmOptions,
                messageCollector = if (isDebugEnabled) messageCollector else MessageCollector.NONE
            )
        }

        return if (cacheDaemonConnection) {
            val key = DaemonConnectionKey(
                compilerId = compilerId,
                clientAliveFlagFile = clientAliveFlagFile,
                sessionAliveFlagFile = sessionAliveFlagFile,
                isDebugEnabled = isDebugEnabled,
                daemonOptions = daemonOptions,
                daemonJVMOptions = daemonJvmOptions
            )
            val cachedConnection = daemonConnections[key]
            if (cachedConnection != null && cachedConnection.compileService.isAlive().isGood) {
                cachedConnection
            } else {
                newDaemonConnection().also { daemonConnections[key] = it }
            }
        } else newDaemonConnection()
    }

    companion object {
        fun registerIfAbsent(project: Project, properties: PropertiesProvider): Provider<KotlinCompilerCacheService> =
            project.gradle.registerSharedService {
                parameters.cacheDaemonConnection.set(properties.cacheDaemonConnection)
            }
    }
}