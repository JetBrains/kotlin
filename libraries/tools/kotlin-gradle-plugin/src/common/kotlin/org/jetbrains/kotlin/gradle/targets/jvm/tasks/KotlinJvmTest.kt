/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm.tasks

import org.gradle.api.internal.tasks.testing.*
import org.gradle.api.internal.tasks.testing.detection.DefaultTestExecuter
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.testing.Test

@CacheableTask
abstract class KotlinJvmTest : Test() {
    @Input
    @Optional
    var targetName: String? = null

    override fun createTestExecuter(): TestExecuter<JvmTestExecutionSpec> {
        val originalExecutor = super.createTestExecuter()

        // If the executor is not an instance of DefaultTestExecutor it means that it was created before
        // and set to `Test.testExecuter` by Gradle plugins like test-distribution and test-retry.
        // We can return the executor "as is" in this case as it is already wrapped with our Executor.
        return if (targetName != null && originalExecutor is DefaultTestExecuter) {
            Executor(originalExecutor, targetName!!)
        } else {
            originalExecutor
        }
    }

    class Executor(
        private val delegate: TestExecuter<JvmTestExecutionSpec>,
        private val targetName: String,
    ) : TestExecuter<JvmTestExecutionSpec> by delegate {
        override fun execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor) {
            delegate.execute(
                testExecutionSpec,
                object : TestResultProcessor by testResultProcessor {
                    override fun started(test: TestDescriptorInternal, event: TestStartEvent) {
                        val myTest = object : TestDescriptorInternal by test {
                            override fun getDisplayName(): String = "${test.displayName}[$targetName]"
                            override fun getClassName(): String? = test.className?.replace('$', '.')
                            override fun getClassDisplayName(): String? = test.classDisplayName?.replace('$', '.')
                        }
                        testResultProcessor.started(myTest, event)
                    }
                }
            )
        }
    }
}
