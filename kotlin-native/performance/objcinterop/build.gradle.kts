/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("benchmarking")
}

kotlin {
    macosArm64()

    targets.filterIsInstance<KotlinNativeTarget>().forEach {
        it.compilations.getByName("main") {
            cinterops {
                val classes by creating {
                    headers("$projectDir/src/nativeInterop/cinterop/complexNumbers.h")
                    extraOpts("-Xccall-mode", "indirect") // Required for -Xcompile-source
                    extraOpts("-Xcompile-source", "$projectDir/src/nativeInterop/cinterop/complexNumbers.m")
                    extraOpts("-Xsource-compiler-option", "-lobjc", "-Xsource-compiler-option", "-fobjc-arc")
                }
            }
        }
    }
}

benchmark {
    applicationName = "ObjCInterop"
}
