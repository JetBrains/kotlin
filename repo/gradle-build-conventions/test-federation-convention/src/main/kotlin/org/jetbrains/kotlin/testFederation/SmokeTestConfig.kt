/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import java.io.Serializable

sealed class SmokeTestConfig: Serializable {
    /**
     * Never execute this task in 'Smoke Test' mode. These tests shall be skipped instead.
     * This can be used if a task is known for not containing any useful smoke tests, or its execution might cause issues
     * when running in smoke test mode.
     */
    data object Disabled : SmokeTestConfig() {
        private fun readResolve(): Any = Disabled
    }

    /**
     * This test task is enabled in 'Smoke Test Mode'.
     * When executed in this mode, all tests, marked as `@SmokeTest` are guaranteed to be executed, alongside all tests
     * marked as `@AffectedBy{XYZ}` (where `XYZ` would be a domain affected by the current set of changes within the branch.)
     *
     * @param autoSmokeTestPercentage The percentage of tests to run automatically in smoke test mode.
     *                                A value of 0 means no automatic smoke tests, while 100 means all tests are run automatically.
     *                                A value of 5 would run 5% of the tests automatically, providing a balance between thoroughness and performance.
     *                                This param can be used for test tasks where no particular test clearly stands out, but a certain subset of
     *                                tests shall still be executed, providing necessary confidence.
     *
     */
    data class Enabled(val autoSmokeTestPercentage: Int) : SmokeTestConfig() {
        init {
            require(autoSmokeTestPercentage in 0..100) { "autoSmokeTestPercentage must be between 0 and 100, inclusive" }
        }
    }

    companion object {
        /**
         * Enabled by default, only tests marked up with `@SmokeTest` (and @AffectedBy{XYZ}) are executed ([Enabled.autoSmokeTestPercentage] is set to 0)
         */
        val Default = Enabled(0)

        /**
         * The entire test task is can be considered a valid 'SmokeTest' and all tests will be executed.
         */
        val RunAllTests = Enabled(100)
    }
}
