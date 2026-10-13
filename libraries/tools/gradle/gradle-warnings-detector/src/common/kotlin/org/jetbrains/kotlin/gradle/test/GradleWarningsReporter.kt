/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.ConcurrentHashMap

internal abstract class GradleWarningsReporter : BuildService<BuildServiceParameters.None>, AutoCloseable {
    internal var executeAtBuildFinish: (() -> Unit)? = null

    private val warningReasons = ConcurrentHashMap.newKeySet<String>()
    private val ignoredWarningReasons = ConcurrentHashMap.newKeySet<String>()

    internal val warnings: Set<String> get() = warningReasons.toSet()
    internal val ignoredReasons: Set<String> get() = ignoredWarningReasons.toSet()

    internal fun report(description: String, stackTraceClassNames: List<String>) {
        val ignoredWarning = KNOWN_THIRD_PARTY_WARNINGS.firstMatching(description, stackTraceClassNames)
        if (ignoredWarning == null) {
            warningReasons.add(description)
        } else {
            ignoredWarningReasons.add(ignoredWarning.reason)
        }
    }

    override fun close() {
        executeAtBuildFinish?.invoke()
    }

    internal companion object {
        private val serviceName =
            "${GradleWarningsReporter::class.java.canonicalName}_${GradleWarningsReporter::class.java.classLoader.hashCode()}"

        fun registerIfAbsent(gradle: Gradle): Provider<GradleWarningsReporter> =
            gradle.sharedServices.registerIfAbsent(serviceName, GradleWarningsReporter::class.java) {}
    }
}
