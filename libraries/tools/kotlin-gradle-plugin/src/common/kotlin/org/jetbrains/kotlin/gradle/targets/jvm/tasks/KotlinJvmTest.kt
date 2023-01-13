/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm.tasks

import org.gradle.api.internal.tasks.testing.*
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.plugin.internal.MppTestReportHelper
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

@CacheableTask
open class KotlinJvmTest : Test() {
    @Input
    @Optional
    var targetName: String? = null

    private val testReporter = project.gradle
        .variantImplementationFactory<MppTestReportHelper.MppTestReportHelperVariantFactory>()
        .getInstance()

    override fun createTestExecuter(): TestExecuter<JvmTestExecutionSpec> =
        if (targetName != null) Executor(
            super.createTestExecuter(),
            targetName!!,
            testReporter,
        )
        else super.createTestExecuter()

    class Executor(
        private val delegate: TestExecuter<JvmTestExecutionSpec>,
        private val targetName: String,
        private val testReporter: MppTestReportHelper,
    ) : TestExecuter<JvmTestExecutionSpec> by delegate {
        override fun execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor) {
            delegate.execute(testExecutionSpec, testReporter.createDelegatingTestReportProcessor(testResultProcessor, targetName))
        }
    }
}
