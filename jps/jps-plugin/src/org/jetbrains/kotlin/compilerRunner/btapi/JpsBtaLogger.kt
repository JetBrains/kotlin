/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

/**
 * The logger the Build Tools API path runs on: [delegate] plus, when [verbose], a copy of everything into the *Build*
 * tool window.
 *
 * Without [verbose] this is the pre-existing behaviour — detail reaches the build process log only, and only for a
 * consumer who has put the `#org.jetbrains.kotlin.jps.build.KotlinBuilder` category at `FINER` in
 * `build-log-jul.properties`. That is not something the *Build* tool window ever shows, which leaves a consumer with
 * no equivalent of Gradle's `--info`/`--debug`. [verbose] is that equivalent, see
 * [JpsBuildToolsApiCompilerRunner.VERBOSE_PROPERTY].
 *
 * [isDebugEnabled] is what makes the switch reach past this class: the runner produces its own detail only when it is
 * `true`, and so does the incremental compilation reporter of the Build Tools API implementation, which is handed this
 * logger by `session.executeOperation`.
 *
 * Only `debug`, `info` and `lifecycle` are mirrored. Warnings and errors already reach JPS through
 * [JpsCompilerMessageRendererBridge] and the runner's own reporting, so mirroring them would double-report.
 */
internal class JpsBtaLogger(
    private val delegate: KotlinLogger,
    private val messageCollector: MessageCollector,
    private val verbose: Boolean,
) : KotlinLogger {
    override val isDebugEnabled: Boolean
        get() = verbose || delegate.isDebugEnabled

    override fun error(msg: String, throwable: Throwable?) {
        delegate.error(msg, throwable)
    }

    override fun warn(msg: String, throwable: Throwable?) {
        delegate.warn(msg, throwable)
    }

    override fun info(msg: String) {
        delegate.info(msg)
        reportIfVerbose(msg)
    }

    override fun debug(msg: String) {
        delegate.debug(msg)
        reportIfVerbose(msg)
    }

    override fun lifecycle(msg: String) {
        delegate.lifecycle(msg)
        reportIfVerbose(msg)
    }

    /**
     * `INFO` is the lowest severity [org.jetbrains.kotlin.jps.build.MessageCollectorAdapter] still maps to a
     * `BuildMessage.Kind`; `LOGGING` would be dropped and land back in the build process log this is trying to escape.
     * The tag matches the runner's progress lines, so that everything this path contributes is greppable as one.
     */
    private fun reportIfVerbose(msg: String) {
        if (!verbose) return
        messageCollector.report(CompilerMessageSeverity.INFO, "[Build Tools API] $msg")
    }
}
