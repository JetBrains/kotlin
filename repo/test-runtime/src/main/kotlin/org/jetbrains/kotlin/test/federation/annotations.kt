/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.junit.jupiter.api.Tag

/**
 * Requires the annotated tests to run and pass before merging to master, regardless of which domains contain changes.
 * Other test filters, such as [NightlyTest], still apply.
 *
 * These tests should be fast and stable because they run for unrelated changes too.
 * See [Test Federation](repo/TEST-FEDERATION.md).
 *
 * ### Extra: Smoke tests
 * Use this annotation for quick checks of core functionality that should run for unrelated changes too.
 * Not every smoke test needs to run for unrelated changes.
 */
@Tag("smoke")
annotation class MustRunAlways

/**
 * Excludes the annotated tests when nightly tests are disabled, including remote runs and safe-merge builds.
 * Nightly tests are enabled in nightly builds and by default in local Gradle runs.
 * These tests are not required for merging to master.
 *
 * This annotation does not select a test on its own. Test Federation selection and other test filters still apply.
 */
@Tag("nightly")
annotation class NightlyTest
