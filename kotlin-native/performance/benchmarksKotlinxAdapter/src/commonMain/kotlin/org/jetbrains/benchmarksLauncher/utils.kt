package org.jetbrains.benchmarksLauncher

import kotlinx.benchmark.Param

abstract class SkipWhenBaseOnly {
    @Param("false")
    var baseOnly = false

    fun skipWhenBaseOnly() {
        check(!baseOnly) { "Skipping because baseOnly=true" }
    }
}
