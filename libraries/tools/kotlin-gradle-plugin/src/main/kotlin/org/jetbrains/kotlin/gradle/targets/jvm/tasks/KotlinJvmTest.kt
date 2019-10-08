/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm.tasks

import org.gradle.api.internal.tasks.testing.*
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestFilter
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTestRun

open class KotlinJvmTest : Test() {
    @Input
    @Optional
    var targetName: String? = null

    override fun createTestExecuter(): TestExecuter<JvmTestExecutionSpec> =
        if (targetName != null) Executor(
            super.createTestExecuter(),
            targetName!!
        )
        else super.createTestExecuter()

    class Executor(
        private val delegate: TestExecuter<JvmTestExecutionSpec>,
        private val targetName: String
    ) : TestExecuter<JvmTestExecutionSpec> by delegate {
        override fun execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor) {
            delegate.execute(testExecutionSpec, object : TestResultProcessor by testResultProcessor {
                override fun started(test: TestDescriptorInternal, event: TestStartEvent) {
                    val myTest = object : TestDescriptorInternal by test {
                        override fun getDisplayName(): String = "${test.displayName}[$targetName]"
                        override fun getClassName(): String? = test.className?.replace('$', '.')
                        override fun getClassDisplayName(): String? = test.classDisplayName?.replace('$', '.')
                    }
                    testResultProcessor.started(myTest, event)
                }
            })
        }
    }
}