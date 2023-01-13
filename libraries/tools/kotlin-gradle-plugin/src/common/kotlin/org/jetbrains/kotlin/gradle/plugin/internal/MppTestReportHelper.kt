/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.internal.tasks.testing.TestDescriptorInternal
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.internal.tasks.testing.TestStartEvent
import org.gradle.api.tasks.testing.TestFailure
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.testing.KotlinTestFailure

/**
 * Handles differences in internal Gradle testing API before and since Gradle 7.6
 */
interface MppTestReportHelper {
    fun reportFailure(
        results: TestResultProcessor,
        id: Any,
        failure: KotlinTestFailure,
        isAssertionFailure: Boolean,
    )

    fun createDelegatingTestReportProcessor(origin: TestResultProcessor, targetName: String): TestResultProcessor

    interface MppTestReportHelperVariantFactory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(): MppTestReportHelper
    }
}

internal class DefaultMppTestReportHelperVariantFactory : MppTestReportHelper.MppTestReportHelperVariantFactory {
    override fun getInstance(): MppTestReportHelper = DefaultMppTestReportHelper()
}

internal class DefaultMppTestReportHelper : MppTestReportHelper {
    override fun reportFailure(
        results: TestResultProcessor,
        id: Any,
        failure: KotlinTestFailure,
        isAssertionFailure: Boolean,
    ) {
        results.failure(
            id,
            if (isAssertionFailure) {
                TestFailure.fromTestAssertionFailure(failure, failure.expected, failure.actual)
            } else {
                TestFailure.fromTestFrameworkFailure(failure)
            }
        )
    }

    @Suppress("DuplicatedCode") // the delegating processor implements different interfaces with the same code
    override fun createDelegatingTestReportProcessor(origin: TestResultProcessor, targetName: String) =
        object : TestResultProcessor by origin {
            override fun started(test: TestDescriptorInternal, event: TestStartEvent) {
                val myTest = object : TestDescriptorInternal by test {
                    override fun getDisplayName(): String = "${test.displayName}[$targetName]"
                    override fun getClassName(): String? = test.className?.replace('$', '.')
                    override fun getClassDisplayName(): String? = test.classDisplayName?.replace('$', '.')
                }
                origin.started(myTest, event)
            }
        }
}