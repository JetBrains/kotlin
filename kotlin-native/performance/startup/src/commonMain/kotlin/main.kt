/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.startup.*

@State(Scope.Benchmark)
class Singleton {
    @Benchmark
    fun initialize() {
        singletonInitialize()
    }

    @Benchmark
    fun initializeNested() {
        singletonInitializeNested()
    }
}
