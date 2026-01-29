/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.benchmarkingTargets

plugins {
    id("benchmarking")
}

kotlin {
    benchmarkingTargets()
}

benchmark {
    applicationName = "Logging"
    compilerOpts = listOf("-Xruntime-logs=logging=debug")
}
