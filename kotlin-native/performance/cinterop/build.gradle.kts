/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.benchmarkingTargets
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("benchmarking")
}

kotlin {
    benchmarkingTargets()

    targets.filterIsInstance<KotlinNativeTarget>().forEach {
        it.compilations.getByName("main") {
            cinterops {
                create("macros")
                create("struct")
                create("types")
            }
        }
    }

    compilerOptions {
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }
}

benchmark {
    applicationName = "Cinterop"
}
