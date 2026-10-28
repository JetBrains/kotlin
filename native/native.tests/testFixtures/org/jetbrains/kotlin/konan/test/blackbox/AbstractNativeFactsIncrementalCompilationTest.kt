/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach

/**
 * The incremental compilation tests with the facts-based per-file caches (`-Xfacts-cache-enabled`):
 * the executable of each step must pass its tests, and the facts directives of `module.info` are checked.
 */
abstract class AbstractNativeFactsIncrementalCompilationTest : AbstractNativeIncrementalCompilationTest() {
    override val factsCacheEnabled: Boolean get() = true

    @BeforeEach
    fun assumeFactsCacheIsSupported() {
        Assumptions.assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
    }
}
