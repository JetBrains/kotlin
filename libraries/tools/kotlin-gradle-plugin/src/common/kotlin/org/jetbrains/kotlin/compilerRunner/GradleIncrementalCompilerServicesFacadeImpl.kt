/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.daemon.client.reportFromDaemon
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.ReportSeverity.*
import org.jetbrains.kotlin.gradle.logging.*
import java.io.Serializable
import java.rmi.Remote
import java.rmi.server.UnicastRemoteObject

internal open class GradleCompilerServicesFacadeImpl(
    private val log: KotlinLogger,
    // RMI messages are reported from RMI threads.
    // Messages reported from non-Gradle threads are not grouped and not shown in build scans.
    // To fix this, we store all messages in a buffer, then report them from a Gradle thread
    private val compilerMessageCollector: GradleBufferingMessageCollector,
    port: Int = SOCKET_ANY_FREE_PORT
) : UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory),
    CompilerServicesFacadeBase,
    Remote {

    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        when (ReportCategory.fromCode(category)) {
            ReportCategory.IC_MESSAGE -> {
                @Suppress("UNUSED_VARIABLE")
                val unusedValueForExhaustiveWhen = when (ReportSeverity.fromCode(severity)) {
                    ERROR -> log.kotlinError { "[IC] $message" }
                    WARNING -> log.kotlinWarn { "[IC] $message" }
                    INFO -> log.kotlinInfo { "[IC] $message" }
                    DEBUG -> log.kotlinDebug { "[IC] $message" }
                }
            }
            ReportCategory.DAEMON_MESSAGE -> {
                log.kotlinDebug { "[DAEMON] $message" }
            }
            else -> {
                compilerMessageCollector.reportFromDaemon(
                    outputsCollector = null,
                    category = category,
                    severity = severity,
                    message = message,
                    attachment = attachment
                )
            }
        }
    }
}

internal class GradleIncrementalCompilerServicesFacadeImpl(
    log: KotlinLogger,
    messageCollector: GradleBufferingMessageCollector,
    port: Int = SOCKET_ANY_FREE_PORT
) : GradleCompilerServicesFacadeImpl(log, messageCollector, port),
    IncrementalCompilerServicesFacade