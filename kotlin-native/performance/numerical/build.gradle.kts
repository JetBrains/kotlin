/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("benchmarking")
}

kotlin {
    targets.filterIsInstance<KotlinNativeTarget>().forEach {
        it.compilations.getByName("main") {
            cinterops {
                val cinterop by creating {
                    headers("$projectDir/src/nativeInterop/cinterop/pi.h")
                    extraOpts("-Xccall-mode", "indirect") // Required for -Xcompile-source
                    extraOpts("-Xcompile-source", "$projectDir/src/nativeInterop/cinterop/pi.c")
                    extraOpts("-Xsource-compiler-option", "-O3")
                }
            }
        }
    }
}

benchmark {
    applicationName = "Numerical"
}
