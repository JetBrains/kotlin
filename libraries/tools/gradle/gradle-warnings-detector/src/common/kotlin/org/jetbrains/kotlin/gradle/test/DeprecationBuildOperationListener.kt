/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test

import org.gradle.api.provider.Provider

internal class DeprecationBuildOperationListener(
    private val warningsReporter: Provider<GradleWarningsReporter>
) : org.gradle.internal.operations.BuildOperationListener {
    override fun started(
        buildOperation: org.gradle.internal.operations.BuildOperationDescriptor,
        startEvent: org.gradle.internal.operations.OperationStartEvent
    ) {
        // no-op
    }

    override fun progress(
        operationIdentifier: org.gradle.internal.operations.OperationIdentifier,
        progressEvent: org.gradle.internal.operations.OperationProgressEvent
    ) {
        val details = progressEvent.details
        if (details is org.gradle.internal.featurelifecycle.DefaultDeprecatedUsageProgressDetails) {
            warningsReporter.get().hasWarnings = true
        }
    }

    override fun finished(
        buildOperation: org.gradle.internal.operations.BuildOperationDescriptor,
        finishEvent: org.gradle.internal.operations.OperationFinishEvent
    ) {
        // no-op
    }
}