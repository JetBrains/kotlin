/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("benchmarking")
}

benchmark {
    applicationName = "Cinterop"
    commonSrcDirs = listOf("../../tools/benchmarks/shared/src", "src/main/kotlin", "../shared/src/main/kotlin", "../../tools/kliopt")
    jvmSrcDirs = listOf("src/main/kotlin-jvm", "../shared/src/main/kotlin-jvm")
    nativeSrcDirs = listOf("src/main/kotlin-native", "../shared/src/main/kotlin-native")
}

val native = kotlin.targets.getByName("native") as KotlinNativeTarget
native.compilations["main"].cinterops {
    create("macros")
    create("struct")
    create("types")
}
