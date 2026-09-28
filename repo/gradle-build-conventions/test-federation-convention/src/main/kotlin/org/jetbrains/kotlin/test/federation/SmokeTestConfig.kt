/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import java.io.Serializable

sealed class SmokeTestConfig : Serializable {
    /**
     * Skips this task unless it is selected for a full test run.
     * Use this when running only a subset of the task's tests is not useful or is not supported.
     *
     * The task is skipped in [TestFederationMode.Smoke], not in [TestFederationMode.Full].
     */
    data object Disabled : SmokeTestConfig() {
        private fun readResolve(): Any = Disabled
    }

    /**
     * Selects tests marked with `@MustRunAlways` or `@MustRunOnChangesInXYZ` for a changed domain,
     * plus an optional automatic sample, when this task is not selected for a full test run.
     * Other test filters, including nightly filters, still apply.
     *
     * This selection applies in [TestFederationMode.Smoke]. A percentage of 100 makes the task use [TestFederationMode.Full].
     *
     * @param autoSmokeTestPercentage The approximate percentage of tests to select automatically using a hash of each test's identity.
     *                                A value of 0 selects no automatic sample; 100 selects all tests.
     *                                The sample is in addition to the annotated tests, so the total can exceed this percentage.
     */
    data class Enabled(val autoSmokeTestPercentage: Int) : SmokeTestConfig() {
        init {
            require(autoSmokeTestPercentage in 0..100) { "autoSmokeTestPercentage must be between 0 and 100, inclusive" }
        }
    }

    companion object {
        /**
         * Selects only tests marked with `@MustRunAlways` or `@MustRunOnChangesInXYZ` for a changed domain when no full run is selected.
         * No automatic sample is selected. Other test filters still apply.
         */
        val Default = Enabled(0)

        /**
         * Selects all tests in this task by using [TestFederationMode.Full], regardless of domain selection or an explicit mode override.
         * Other test filters, including nightly filters, still apply.
         */
        val RunAllTests = Enabled(100)
    }
}
