/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test

import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class GradleWarningsReporter : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val logger: Logger = Logging.getLogger(this.javaClass)
    internal var executeAtBuildFinish: (() -> Unit)? = null

    private val hasWarnings = AtomicBoolean(false)
    private val ignoredWarningReasons = ConcurrentHashMap.newKeySet<String>()

    internal fun report(description: String, stackTraceClassNames: List<String>) {
        val ignoredWarning = KNOWN_THIRD_PARTY_WARNINGS.firstMatching(description, stackTraceClassNames)
        if (ignoredWarning == null) {
            hasWarnings.set(true)
        } else {
            ignoredWarningReasons.add(ignoredWarning.reason)
        }
    }

    override fun close() {
        executeAtBuildFinish?.invoke()
        if (ignoredWarningReasons.isNotEmpty()) {
            logger.info(
                "[$marker] Ignored deprecation warnings caused by third-party plugins: ${ignoredWarningReasons.sorted()}"
            )
        }
        if (hasWarnings.get()) {
            logger.warn("[$marker] Some deprecation warnings were found during this build.")
        }
    }

    internal companion object {
        private val marker = GradleWarningsDetectorPlugin::class.java.simpleName

        private val serviceName =
            "${GradleWarningsReporter::class.java.canonicalName}_${GradleWarningsReporter::class.java.classLoader.hashCode()}"

        fun registerIfAbsent(gradle: Gradle): Provider<GradleWarningsReporter> =
            gradle.sharedServices.registerIfAbsent(serviceName, GradleWarningsReporter::class.java) {}
    }
}
