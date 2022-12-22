/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.internal.tasks.testing.TestDescriptorInternal
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.internal.tasks.testing.TestStartEvent
import org.jetbrains.kotlin.gradle.testing.KotlinTestFailure

internal class MppTestReportHelperG74 : MppTestReportHelper {
    internal class MppTestReportHelperVariantFactoryG74 :
        MppTestReportHelper.MppTestReportHelperVariantFactory {
        override fun getInstance(): MppTestReportHelper = MppTestReportHelperG74()
    }

    override fun reportFailure(
        results: TestResultProcessor,
        id: Any,
        failure: KotlinTestFailure,
        isAssertionFailure: Boolean,
    ) {
        results.failure(
            id,
            failure,
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