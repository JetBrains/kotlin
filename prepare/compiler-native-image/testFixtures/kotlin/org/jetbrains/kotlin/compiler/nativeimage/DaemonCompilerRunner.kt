/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.ReportCategory
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class DaemonCompilerRunner(private val compileService: () -> CompileService) : CompilerRunner {
    override fun run(workingDir: File, arguments: List<String>, classpath: List<File>): CompilerInvocationResult {
        val service = compileService()
        val sessionId = service.leaseCompileSession(null).get()
        try {
            val output = ByteArrayOutputStream()
            val exitCode = service.compile(
                sessionId,
                (classpath.asClasspath() + arguments).toTypedArray(),
                CompilationOptions(
                    CompilerMode.NON_INCREMENTAL_COMPILER,
                    CompileService.TargetPlatform.JVM,
                    arrayOf(ReportCategory.COMPILER_MESSAGE.code, ReportCategory.DAEMON_MESSAGE.code, ReportCategory.EXCEPTION.code),
                    ReportSeverity.INFO.code,
                    emptyArray(),
                ),
                BasicCompilerServicesWithResultsFacadeServer(
                    PrintingMessageCollector(PrintStream(output), MessageRenderer.PLAIN_FULL_PATHS, true),
                ),
                null,
            ).get()
            return CompilerInvocationResult(exitCode, "", output.toString())
        } finally {
            service.releaseCompileSession(sessionId)
        }
    }
}
