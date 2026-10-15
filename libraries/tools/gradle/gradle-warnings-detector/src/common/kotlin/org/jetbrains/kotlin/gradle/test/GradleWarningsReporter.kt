/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test

import org.gradle.StartParameter
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.internal.operations.BuildOperationDescriptor
import org.gradle.internal.operations.BuildOperationListener
import org.gradle.internal.operations.BuildOperationListenerManager
import org.gradle.internal.operations.OperationFinishEvent
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.internal.operations.OperationProgressEvent
import org.gradle.internal.operations.OperationStartEvent
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal abstract class GradleWarningsReporter : BuildService<BuildServiceParameters.None>, BuildOperationListener,
    AutoCloseable {
    @get:Inject
    abstract val objects: ObjectFactory

    private val warningReasons = ConcurrentHashMap.newKeySet<String>()
    private val ignoredWarningReasons = ConcurrentHashMap.newKeySet<String>()

    private val gradleServices = objects.newInstance(GradleServices::class.java)
    private val buildOperationListenerManager = gradleServices.buildOperationListenerManager

    // Tests switch to the 'summary' warning mode to temporarily suppress deprecation warnings they can't fix.
    // Read in every build, as changing the warning mode doesn't invalidate the configuration cache entry.
    internal val failOnWarnings: Boolean = gradleServices.startParameter.warningMode != WarningMode.Summary
    private val deprecationBuildOperationListener = DeprecationBuildOperationListener(this)

    init {
        buildOperationListenerManager.addListener(deprecationBuildOperationListener)
    }

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

    override fun started(
        buildOperation: BuildOperationDescriptor,
        startEvent: OperationStartEvent
    ) {
        // no-op
    }

    override fun progress(
        operationIdentifier: OperationIdentifier,
        progressEvent: OperationProgressEvent
    ) {
        // no-op
    }

    override fun finished(
        buildOperation: BuildOperationDescriptor,
        finishEvent: OperationFinishEvent
    ) {
        // no-op
    }

    override fun close() {
        buildOperationListenerManager.removeListener(deprecationBuildOperationListener)
    }

    internal companion object {
        private val serviceName =
            "${GradleWarningsReporter::class.java.canonicalName}_${GradleWarningsReporter::class.java.classLoader.hashCode()}"

        fun registerIfAbsent(gradle: Gradle): Provider<GradleWarningsReporter> =
            gradle.sharedServices.registerIfAbsent(serviceName, GradleWarningsReporter::class.java) {}
    }
}

internal abstract class GradleServices {
    @get:Inject
    abstract val buildOperationListenerManager: BuildOperationListenerManager

    @get:Inject
    abstract val startParameter: StartParameter
}
