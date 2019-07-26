/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("benchmarking")
}

benchmark {
    applicationName = "Ring"
    commonSrcDirs = listOf("../../tools/benchmarks/shared/src", "src/main/kotlin", "../shared/src/main/kotlin", "../../endorsedLibraries/kliopt/src/main/kotlin")
    jvmSrcDirs = listOf("src/main/kotlin-jvm", "../shared/src/main/kotlin-jvm", "../../endorsedLibraries/kliopt/src/main/kotlin-jvm")
    nativeSrcDirs = listOf("src/main/kotlin-native", "../shared/src/main/kotlin-native/common", "../../endorsedLibraries/kliopt/src/main/kotlin-native")
    mingwSrcDirs = listOf("src/main/kotlin-native", "../shared/src/main/kotlin-native/mingw")
    posixSrcDirs = listOf("src/main/kotlin-native", "../shared/src/main/kotlin-native/posix")
}
