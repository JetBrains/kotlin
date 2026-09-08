/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.junit.jupiter.api.Tag

/**
 * See [Test Federation](repo/TEST-FEDERATION.md)
 *
 * Smoke tests are always executed on CI, regardless of the affected domains.
 * Any commit pushed to the master branch must pass all smoke tests.
 *
 * Smoke tests are intended to cover the core functionality of a domain to catch regressions
 * caused by changes in unrelated domains.
 *
 * Requirements:
 * - Smoke tests must be very stable.
 * - Smoke test (suites) must be fast.
 */
@Tag("smoke")
annotation class SmokeTest

/**
 * Will mark a given test as 'Nightly':
 * - This test will not run in remote runs (rr)
 * - This test will not run in safe-merge (and safe-merge dry runs)
 * - This test will only be executed nightly.
 *
 * Marking a test as 'Nightly' will still execute this test locally.
 */
@Tag("nightly")
annotation class NightlyTest
