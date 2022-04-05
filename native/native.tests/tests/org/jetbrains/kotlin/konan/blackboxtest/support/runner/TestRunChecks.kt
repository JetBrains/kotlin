/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Timeouts
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import kotlin.time.Duration

internal sealed interface TestRunCheck {
    sealed class ExecutionTimeout(val timeout: Duration) : TestRunCheck {
        class ShouldNotExceed(timeout: Duration) : ExecutionTimeout(timeout)
        class ShouldExceed(timeout: Duration) : ExecutionTimeout(timeout)
    }
}

internal class TestRunChecks(builderAction: MutableCollection<TestRunCheck>.() -> Unit) : Iterable<TestRunCheck> {
    constructor(vararg checks: TestRunCheck) : this({ addAll(checks) })

    private val allChecks = buildList {
        builderAction()
        addDefaults()
    }

    val executionTimeoutCheck: TestRunCheck.ExecutionTimeout get() = allChecks.firstIsInstance()

    override fun iterator() = allChecks.iterator()

    companion object {
        // The most frequently used case:
        @Suppress("TestFunctionName")
        fun Default(timeout: Duration) = TestRunChecks(TestRunCheck.ExecutionTimeout.ShouldNotExceed(timeout))

        private fun MutableCollection<TestRunCheck>.addDefaults() {
            if (isMissing<TestRunCheck.ExecutionTimeout>()) {
                add(TestRunCheck.ExecutionTimeout.ShouldNotExceed(Timeouts.DEFAULT_EXECUTION_TIMEOUT))
            }
        }

        private inline fun <reified T : TestRunCheck> Collection<TestRunCheck>.isMissing(): Boolean =
            firstIsInstanceOrNull<T>() == null
    }
}
