/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.testFederation

import org.junit.jupiter.api.Tag

internal const val nightlyTagKey = "nightly"

/**
 * Marks tests which are only executed by dedicated nightly test tasks. These tests are not executed during merging to master checks.
 *
 * A dedicated nightly test task must explicitly exclude not nightly tests (`useJUnitPlatform { excludeTags("!nightly") }`).
 * Other test tasks *which are executed on CI* must exclude the tag (`excludeTags("nightly")`).
 * Local test tasks may omit the filtration for convenience.
 */
@Tag(nightlyTagKey)
annotation class NightlyTest
