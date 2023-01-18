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

internal abstract class GradleWarningsReporter : BuildService<BuildServiceParameters.None>, AutoCloseable {
    internal var hasWarnings = false

    private val logger: Logger = Logging.getLogger(this.javaClass)
    internal var executeAtBuildFinish: (() -> Unit)? = null

    override fun close() {
        executeAtBuildFinish?.invoke()
        if (hasWarnings) {
            logger.warn("[${GradleWarningsDetectorPlugin::class.java.simpleName}] Some deprecation warnings were found during this build.")
        }
    }

    internal companion object {
        private val serviceName =
            "${GradleWarningsReporter::class.java.canonicalName}_${GradleWarningsReporter::class.java.classLoader.hashCode()}"

        fun registerIfAbsent(gradle: Gradle): Provider<GradleWarningsReporter> =
            gradle.sharedServices.registerIfAbsent(serviceName, GradleWarningsReporter::class.java) {}
    }
}