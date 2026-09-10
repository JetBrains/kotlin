/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

/**
 * Controls which tests Test Federation selects for a test task.
 * Other test filters, including nightly filters, still apply in both modes.
 *
 * [Full] selects all tests in the task. It is used when all tests in one of the task's domains are required for merging to master,
 * when Test Federation is disabled, when the task is configured to run all tests, or when this mode is explicitly configured.
 *
 * [Smoke] selects tests marked with `@MustRunAlways`, tests marked with `@MustRunOnChangesInXYZ` for a changed domain,
 * and any automatically selected sample. The task's configuration can disable the task in this mode.
 */
enum class TestFederationMode {
    Full, Smoke;
}
