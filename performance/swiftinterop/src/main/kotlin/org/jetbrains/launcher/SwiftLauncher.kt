/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.benchmarksLauncher.*
import org.jetbrains.kliopt.*

class SwiftLauncher(numWarmIterations: Int, numberOfAttempts: Int, prefix: String): Launcher(numWarmIterations, numberOfAttempts, prefix) {
    override val benchmarks = BenchmarksCollection(
            mutableMapOf(
            )
    )
}