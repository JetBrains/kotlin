/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.junit.jupiter.api.Tag

/**
 * See [Test Federation](repo/TEST-FEDERATION.md)
 *
 * The annotated tests run regardless of which domains contain changes.
 * Other test filters, such as [NightlyTest], still apply.
 *
 * These tests should be fast and stable because they run for unrelated changes too.
 */
@Tag("smoke")
annotation class MustRunAlways

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
