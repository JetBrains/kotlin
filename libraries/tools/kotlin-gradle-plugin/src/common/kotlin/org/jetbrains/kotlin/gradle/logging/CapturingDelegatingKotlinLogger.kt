/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.gradle.report.BuildReportMode

/**
 * Delegates log messages to the origin logger.
 * Based on the [buildReportMode], it may capture some of them so that they could be used in the build reports or for other purposes via [capturedLines].
 */
internal class CapturingDelegatingKotlinLogger(private val origin: KotlinLogger, private val buildReportMode: BuildReportMode) : KotlinLogger {
    override val isDebugEnabled: Boolean
        get() = origin.isDebugEnabled || buildReportMode >= BuildReportMode.VERBOSE

    val capturedLines: List<String>
        field = mutableListOf<String>()

    override fun error(msg: String, throwable: Throwable?) {
        origin.error(msg, throwable)
        withBuildReportModeAtLeast(BuildReportMode.SIMPLE) {
            capturedLines += msg
        }
    }

    override fun warn(msg: String, throwable: Throwable?) {
        origin.warn(msg, throwable)
        withBuildReportModeAtLeast(BuildReportMode.SIMPLE) {
            capturedLines += msg
        }
    }

    override fun info(msg: String) {
        origin.info(msg)
        withBuildReportModeAtLeast(BuildReportMode.SIMPLE) {
            capturedLines += msg
        }
    }

    override fun debug(msg: String) {
        origin.debug(msg)
        // this message is used only for tests and is duplicated by a more user-friendly one
        if (msg.startsWith("[KOTLIN] compile iteration: ")) return
        withBuildReportModeAtLeast(BuildReportMode.VERBOSE) {
            capturedLines += msg
        }
    }

    override fun lifecycle(msg: String) {
        origin.lifecycle(msg)
        withBuildReportModeAtLeast(BuildReportMode.SIMPLE) {
            capturedLines += msg
        }
    }

    private fun withBuildReportModeAtLeast(requestedBuildReportMode: BuildReportMode, action: () -> Unit) {
        if (buildReportMode >= requestedBuildReportMode) action()
    }
}