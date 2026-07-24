/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.BenchmarkRepeatingType
import org.jetbrains.kotlin.benchmarkingTargets

plugins {
    id("kotlinx-benchmarking")
}

kotlin {
    benchmarkingTargets()
}

kotlinxBenchmark {
    applicationName = "Startup"
    repeatingType = BenchmarkRepeatingType.EXTERNAL
}
